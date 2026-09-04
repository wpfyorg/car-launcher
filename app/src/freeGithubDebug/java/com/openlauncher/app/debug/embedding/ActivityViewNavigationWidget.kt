package com.openlauncher.app.debug.embedding

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import com.openlauncher.app.feature.navigation.external.EmbeddedNavigationView
import com.openlauncher.app.feature.navigation.external.EmbeddingHostState
import com.openlauncher.app.feature.navigation.external.NavigationEmbeddingSession
import com.openlauncher.app.launcher.LauncherApp

/**
 * Free+Debug-only privileged host that puts Android 9 ActivityView directly inside the Home map
 * widget. Release builds continue to use the public-SDK EmbeddedTaskView implementation.
 */
internal class ActivityViewNavigationWidget(context: Context) :
    FrameLayout(context),
    EmbeddedNavigationView {
    private val capabilities = EmbeddingCapabilities.detect(context)
    private val activityManager = runCatching { ActivityManagerCompat() }.getOrNull()
    private val activityView = ActivityViewCompat(context, capabilities)
    private val packageManager = context.packageManager

    private var targetApp: LauncherApp? = null
    private var stateListener: (EmbeddingHostState) -> Unit = {}
    private var confirmedAppKey: String? = null
    private var confirmedTaskId: Int? = null
    private var launchingAppKey: String? = null
    private var trackedPackageName: String? = null
    private var intentionalBackTaskId: Int? = null
    private var intentionalBackClear: Runnable? = null
    private var intentionallyStoppedAppKey: String? = null
    private var launchGeneration = 0
    private var readyPollCount = 0
    private var sessionHostRegistrationId: Long? = null
    private var sessionHostAppKey: String? = null

    override val supportsInteractiveEmbedding: Boolean
        get() = capabilities.canHostExternalActivity

    override val supportsEmbeddedBack: Boolean
        get() = capabilities.canHostExternalActivity

    init {
        addView(
            activityView,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        attachActivityView()
    }

    override fun bind(
        app: LauncherApp?,
        onStateChanged: (EmbeddingHostState) -> Unit,
    ) {
        stateListener = onStateChanged
        val previousApp = targetApp
        val changed = previousApp?.stableKey != app?.stableKey
        if (changed && previousApp != null) {
            removeEmbeddedTask(previousApp, reason = "provider switch")
        }
        targetApp = app
        if (changed) {
            confirmedAppKey = null
            confirmedTaskId = null
            launchingAppKey = null
            trackedPackageName = null
            clearIntentionalBackCandidate()
            intentionallyStoppedAppKey = null
            launchGeneration++
            if (app != null) {
                registerSessionHost(app)
                if (activityView.isAttached) {
                    startOrAdopt(app)
                } else {
                    attachActivityView()
                }
            }
        } else if (app != null) {
            registerSessionHost(app)
        }
    }

    override fun ensureRunning() {
        val app = targetApp ?: return
        if (intentionallyStoppedAppKey == app.stableKey) return
        if (confirmedAppKey != app.stableKey && launchingAppKey != app.stableKey) startOrAdopt(app)
    }

    override fun performBackPress(): Boolean {
        val taskId = confirmedTaskId ?: return false
        return activityView.performBackPress()
            .onSuccess { markIntentionalBackCandidate(taskId) }
            .onFailure { error -> Log.w(Tag, "Unable to forward embedded Back", error) }
            .isSuccess
    }

    override fun release() {
        // AndroidView.onRelease can run during ordinary Compose destination/recomposition churn.
        // Keep the ActivityView, task id, and display owned by NavigationEmbeddingSession so a
        // provider switch can tear them down synchronously, or a same-provider replacement can
        // adopt the task without a kill/relaunch cycle.
        stateListener = {}
        Log.i(
            Tag,
            "widget Compose host released; retaining session app=${targetApp?.packageName} " +
                "task=$confirmedTaskId display=${activityView.virtualDisplayId}",
        )
    }

    private fun releaseFromSession(mode: NavigationEmbeddingSession.HostReleaseMode) {
        sessionHostRegistrationId = null
        sessionHostAppKey = null
        if (mode == NavigationEmbeddingSession.HostReleaseMode.Teardown) {
            // Process-owned task removal runs before this callback. This fallback covers the short
            // launch window before watchTask has discovered a task id.
            targetApp?.let { app -> removeEmbeddedTask(app, reason = "session teardown fallback") }
            targetApp = null
        }
        releaseLocalHost()
    }

    private fun releaseLocalHost() {
        launchGeneration++
        confirmedAppKey = null
        confirmedTaskId = null
        launchingAppKey = null
        trackedPackageName = null
        clearIntentionalBackCandidate()
        intentionallyStoppedAppKey = null
        activityView.release()
        stateListener(EmbeddingHostState.Idle)
    }

    private fun registerSessionHost(app: LauncherApp) {
        if (sessionHostRegistrationId != null && sessionHostAppKey == app.stableKey) return
        sessionHostRegistrationId?.let(NavigationEmbeddingSession::unregisterHost)
        sessionHostRegistrationId = NavigationEmbeddingSession.registerHost(app.stableKey) { mode ->
            releaseFromSession(mode)
        }
        sessionHostAppKey = app.stableKey
    }

    private fun removeEmbeddedTask(app: LauncherApp, reason: String) {
        val manager = activityManager ?: return
        val displayId = activityView.virtualDisplayId ?: return
        val taskPackage = trackedPackageName ?: app.packageName
        val taskId = confirmedTaskId ?: manager
            .findTopActivityStack(taskPackage, preferredDisplayId = displayId)
                .getOrNull()
                ?.taskId
            ?: return
        manager.removeTask(taskId)
            .onSuccess { removed ->
                if (removed) {
                    NavigationEmbeddingSession.clearTrackedTask(app.stableKey, taskId)
                }
                Log.i(
                    Tag,
                    "widget $reason task=$taskId app=${app.packageName} " +
                        "display=$displayId removed=$removed",
                )
            }
            .onFailure { error ->
                Log.w(Tag, "Unable to remove embedded task $taskId during $reason", error)
            }
    }

    override fun onDetachedFromWindow() {
        // A Compose AndroidView can detach transiently while Home is still the active destination.
        // Releasing here destroys the ActivityView display and can kill/relaunch the same provider.
        // True composition disposal is handled by AndroidView.onRelease; provider changes are
        // synchronously gated by the process-owned NavigationEmbeddingSession.
        super.onDetachedFromWindow()
    }

    private fun attachActivityView() {
        activityView.attach().fold(
            onSuccess = {
                readyPollCount = 0
                waitForReady()
            },
            onFailure = { error -> fail("ActivityView unavailable", error) },
        )
    }

    private fun waitForReady() {
        val displayId = activityView.virtualDisplayId
        if (displayId != null) {
            stateListener(EmbeddingHostState.SurfaceReady)
            targetApp?.let(::startOrAdopt)
            return
        }
        if (readyPollCount++ >= MaxReadyPolls) {
            stateListener(EmbeddingHostState.Failed("ActivityView did not become ready"))
            return
        }
        postDelayed(::waitForReady, ReadyPollMillis)
    }

    private fun launch(app: LauncherApp) {
        val displayId = activityView.virtualDisplayId ?: return
        if (intentionallyStoppedAppKey == app.stableKey) return
        if (confirmedAppKey == app.stableKey) return
        if (launchingAppKey == app.stableKey) return
        if (!capabilities.canHostExternalActivity) {
            stateListener(
                EmbeddingHostState.Unavailable(
                    "Privileged ActivityView requirements are unavailable on this build",
                ),
            )
            return
        }

        val launchTarget = resolveLaunchTarget(app)
        trackedPackageName = launchTarget.packageName
        val launcherIntent = launchTarget.intent.apply {
            // Always create the launcher task on ActivityView's display instead of reusing an
            // existing full-screen task for the same app on the head unit's primary display.
            // FLAG_ACTIVITY_MULTIPLE_TASK only has task-creation semantics together with
            // FLAG_ACTIVITY_NEW_TASK. Without NEW_TASK, apps such as Organic Maps can join the
            // launcher's own task on display 0 before spawning their real map activity.
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION,
            )
        }
        val generation = ++launchGeneration
        launchingAppKey = app.stableKey
        activityView.startActivity(launcherIntent, external = true).fold(
            onSuccess = {
                Log.i(
                    Tag,
                    "widget started ${app.packageName} runtime=${launchTarget.packageName} display=$displayId",
                )
                watchTask(
                    app = app,
                    displayId = displayId,
                    generation = generation,
                )
            },
            onFailure = { error ->
                launchingAppKey = null
                fail("Unable to launch ${app.label}", error)
            },
        )
    }

    private fun startOrAdopt(app: LauncherApp) {
        val displayId = activityView.virtualDisplayId ?: return
        val existingTask = NavigationEmbeddingSession.trackedTask(app.stableKey)
        if (existingTask != null) {
            trackedPackageName = existingTask.runtimePackageName
            launchingAppKey = app.stableKey
            val generation = ++launchGeneration
            Log.i(
                Tag,
                "widget adopting ${app.packageName} task=${existingTask.taskId} " +
                    "display=${existingTask.displayId}->$displayId",
            )
            watchTask(
                app = app,
                displayId = displayId,
                generation = generation,
                trackedTaskId = existingTask.taskId,
            )
            return
        }
        launch(app)
    }

    private fun watchTask(
        app: LauncherApp,
        displayId: Int,
        generation: Int,
        attempt: Int = 0,
        trackedTaskId: Int? = null,
        missingTaskPolls: Int = 0,
        stableStackId: Int? = null,
        stableDisplayId: Int? = null,
        stablePolls: Int = 0,
        stableTopActivity: ComponentName? = null,
    ) {
        postDelayed(
            {
                if (generation != launchGeneration || targetApp?.stableKey != app.stableKey) {
                    return@postDelayed
                }
                val manager = activityManager ?: run {
                    stateListener(EmbeddingHostState.Failed("ActivityManager bridge unavailable"))
                    return@postDelayed
                }
                val taskPackage = trackedPackageName ?: app.packageName
                val locationResult = if (trackedTaskId != null) {
                    manager.findStackByTaskId(trackedTaskId)
                } else {
                    manager.findTopActivityStack(taskPackage, preferredDisplayId = displayId)
                }
                locationResult.fold(
                    onSuccess = { location ->
                        if (location == null) {
                            val missingGracePolls = if (trackedTaskId == intentionalBackTaskId) {
                                IntentionalBackMissingPolls
                            } else {
                                MissingTaskGracePolls
                            }
                            if (trackedTaskId != null && missingTaskPolls < missingGracePolls) {
                                watchTask(
                                    app = app,
                                    displayId = displayId,
                                    generation = generation,
                                    attempt = attempt + 1,
                                    trackedTaskId = trackedTaskId,
                                    missingTaskPolls = missingTaskPolls + 1,
                                )
                            } else if (trackedTaskId == intentionalBackTaskId) {
                                stopAfterIntentionalBack(app, generation)
                            } else if (trackedTaskId != null) {
                                // The original task is genuinely gone. Look for a replacement task
                                // from the same package before deciding that the app needs relaunching.
                                manager.findTopActivityStack(
                                    taskPackage,
                                    preferredDisplayId = displayId,
                                ).fold(
                                    onSuccess = { replacement ->
                                        if (replacement != null) {
                                            watchTask(
                                                app = app,
                                                displayId = displayId,
                                                generation = generation,
                                                attempt = 0,
                                                trackedTaskId = replacement.taskId,
                                                stableStackId = replacement.stackId,
                                                stableDisplayId = replacement.displayId,
                                                stablePolls = 1,
                                                stableTopActivity = replacement.topActivity,
                                            )
                                        } else {
                                            recoverMissingTask(app, generation)
                                        }
                                    },
                                    onFailure = { error ->
                                        fail("Unable to recover ${app.label} task", error)
                                    },
                                )
                            } else {
                                retryOrFail(app, displayId, generation, attempt)
                            }
                            return@fold
                        }

                        val activeTaskId = trackedTaskId ?: location.taskId
                        NavigationEmbeddingSession.trackTask(
                            appKey = app.stableKey,
                            runtimePackageName = taskPackage,
                            taskId = activeTaskId,
                            displayId = displayId,
                            removeTask = manager::removeTask,
                            isTaskPresent = { taskId ->
                                manager.findStackByTaskId(taskId).map { it != null }
                            },
                        )

                        val sameObservation =
                            location.stackId == stableStackId &&
                                location.displayId == stableDisplayId &&
                                location.topActivity == stableTopActivity
                        val nextStablePolls = if (sameObservation) stablePolls + 1 else 1

                        if (location.displayId == displayId) {
                            if (nextStablePolls >= TargetStablePolls) {
                                val wasConfirmed = confirmedAppKey == app.stableKey
                                confirmedAppKey = app.stableKey
                                confirmedTaskId = activeTaskId
                                launchingAppKey = null
                                stateListener(EmbeddingHostState.Running(displayId))
                                if (!wasConfirmed) {
                                    Log.i(
                                        Tag,
                                        "widget confirmed ${app.packageName} stable display=$displayId",
                                    )
                                }
                                // Keep monitoring after the first confirmation. Navigation apps can
                                // create a new task/activity later (onboarding, sign-in, permissions),
                                // and Android 9 may place that new task back on the default display.
                                watchTask(
                                    app,
                                    displayId,
                                    generation,
                                    attempt = 0,
                                    trackedTaskId = activeTaskId,
                                    stableStackId = location.stackId,
                                    stableDisplayId = location.displayId,
                                    stablePolls = 0,
                                    stableTopActivity = location.topActivity,
                                )
                            } else {
                                watchTask(
                                    app,
                                    displayId,
                                    generation,
                                    attempt + 1,
                                    activeTaskId,
                                    0,
                                    location.stackId,
                                    location.displayId,
                                    nextStablePolls,
                                    location.topActivity,
                                )
                            }
                            return@fold
                        }

                        // Android 9 can temporarily surface permission, chooser, sign-in, or other
                        // foreign-package activities in the tracked task on the default display.
                        // Moving the task while one of those activities owns the window token can
                        // crash the navigation app with BadTokenException. Keep tracking the stable
                        // task id and wait until the navigation app itself is top again before
                        // attempting a relocation. A null topActivity is likewise a transient stack
                        // state while ActivityManager tears an activity down.
                        if (location.topActivity?.packageName != taskPackage) {
                            watchTask(
                                app,
                                displayId,
                                generation,
                                attempt + 1,
                                activeTaskId,
                                0,
                                location.stackId,
                                location.displayId,
                                nextStablePolls,
                                location.topActivity,
                            )
                            return@fold
                        }

                        val escapedStablePolls = if (confirmedAppKey == app.stableKey) {
                            EscapedTaskStablePollsSteady
                        } else {
                            EscapedTaskStablePollsLaunch
                        }
                        if (nextStablePolls < escapedStablePolls) {
                            watchTask(
                                app,
                                displayId,
                                generation,
                                attempt + 1,
                                activeTaskId,
                                0,
                                location.stackId,
                                location.displayId,
                                nextStablePolls,
                                location.topActivity,
                            )
                            return@fold
                        }

                        rehomeEscapedTask(
                            manager = manager,
                            app = app,
                            location = location,
                            displayId = displayId,
                            generation = generation,
                            attempt = attempt,
                        )
                    },
                    onFailure = { error -> fail("Unable to inspect ${app.label} task", error) },
                )
            },
            if (confirmedAppKey == app.stableKey) SteadyTaskPollMillis else LaunchTaskPollMillis,
        )
    }

    private fun rehomeEscapedTask(
        manager: ActivityManagerCompat,
        app: LauncherApp,
        location: StackLocation,
        displayId: Int,
        generation: Int,
        attempt: Int,
    ) {
        if (capabilities.startTasksFromRecents) {
            manager.startTaskFromRecents(location.taskId, displayId).fold(
                onSuccess = {
                    Log.i(
                        Tag,
                        "widget attach ${app.packageName} task=${location.taskId} " +
                            "display=${location.displayId}->$displayId",
                    )
                    watchTask(app, displayId, generation, attempt + 1)
                },
                onFailure = { recentsError ->
                    rehomeByMovingStack(
                        manager,
                        app,
                        location,
                        displayId,
                        generation,
                        attempt,
                        recentsError,
                    )
                },
            )
            return
        }

        rehomeByMovingStack(
            manager,
            app,
            location,
            displayId,
            generation,
            attempt,
            IllegalStateException("START_TASKS_FROM_RECENTS unavailable"),
        )
    }

    private fun resolveLaunchTarget(app: LauncherApp): LaunchTarget {
        val defaultIntent = Intent.makeMainActivity(app.component)
        val startUrl = runCatching {
            packageManager
                .getApplicationInfo(app.packageName, PackageManager.GET_META_DATA)
                .metaData
                ?.getString(WebApkStartUrlMetadata)
        }.getOrNull()?.takeIf(String::isNotBlank) ?: return LaunchTarget(defaultIntent, app.packageName)

        val browserProbe = Intent(Intent.ACTION_VIEW, Uri.parse(WebBrowserProbeUrl)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        val runtime = packageManager
            .resolveActivity(browserProbe, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.takeIf { it.packageName != app.packageName }
            ?: return LaunchTarget(defaultIntent, app.packageName)

        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(startUrl)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            component = ComponentName(runtime.packageName, runtime.name)
        }
        Log.i(Tag, "widget WebAPK ${app.packageName} -> ${runtime.packageName}")
        return LaunchTarget(webIntent, runtime.packageName)
    }

    private fun rehomeByMovingStack(
        manager: ActivityManagerCompat,
        app: LauncherApp,
        location: StackLocation,
        displayId: Int,
        generation: Int,
        attempt: Int,
        recentsError: Throwable,
    ) {
        if (!capabilities.manageActivityStacks) {
            launchingAppKey = null
            fail("Unable to keep ${app.label} embedded", recentsError)
            return
        }

        manager.moveStackToDisplay(location.stackId, displayId).fold(
            onSuccess = {
                Log.i(
                    Tag,
                    "widget rehome ${app.packageName} stack=${location.stackId} " +
                        "display=${location.displayId}->$displayId",
                )
                watchTask(app, displayId, generation, attempt + 1)
            },
            onFailure = { error ->
                launchingAppKey = null
                fail("Unable to embed ${app.label}", error)
            },
        )
    }

    private fun retryOrFail(
        app: LauncherApp,
        displayId: Int,
        generation: Int,
        attempt: Int,
    ) {
        if (confirmedAppKey == app.stableKey) {
            watchTask(app, displayId, generation, attempt = 0)
            return
        }
        if (attempt >= MaxTaskPolls) {
            launchingAppKey = null
            stateListener(EmbeddingHostState.Failed("${app.label} did not produce an embeddable task"))
        } else {
            watchTask(app, displayId, generation, attempt + 1)
        }
    }

    private fun recoverMissingTask(app: LauncherApp, generation: Int) {
        if (generation != launchGeneration || targetApp?.stableKey != app.stableKey) return
        // Replacement discovery already failed, so clear durable identity even when the vanished
        // task disappeared before it was stable enough to become confirmedTaskId.
        NavigationEmbeddingSession.clearTrackedTask(app.stableKey)
        confirmedAppKey = null
        confirmedTaskId = null
        launchingAppKey = null
        clearIntentionalBackCandidate()
        stateListener(EmbeddingHostState.SurfaceReady)
        Log.i(Tag, "widget task disappeared; relaunching ${app.packageName}")
        launch(app)
    }

    private fun markIntentionalBackCandidate(taskId: Int) {
        clearIntentionalBackCandidate()
        intentionalBackTaskId = taskId
        intentionalBackClear = Runnable {
            intentionalBackClear = null
            intentionalBackTaskId = null
        }.also { clear ->
            postDelayed(clear, IntentionalBackWindowMillis)
        }
    }

    private fun clearIntentionalBackCandidate() {
        intentionalBackClear?.let(::removeCallbacks)
        intentionalBackClear = null
        intentionalBackTaskId = null
    }

    private fun stopAfterIntentionalBack(app: LauncherApp, generation: Int) {
        if (generation != launchGeneration || targetApp?.stableKey != app.stableKey) return
        confirmedTaskId?.let { taskId ->
            NavigationEmbeddingSession.clearTrackedTask(app.stableKey, taskId)
        }
        confirmedAppKey = null
        confirmedTaskId = null
        launchingAppKey = null
        clearIntentionalBackCandidate()
        intentionallyStoppedAppKey = app.stableKey
        stateListener(EmbeddingHostState.SurfaceReady)
        Log.i(Tag, "widget task closed by embedded Back; leaving ${app.packageName} stopped")
    }

    private fun fail(message: String, error: Throwable) {
        Log.e(Tag, message, error)
        stateListener(
            EmbeddingHostState.Failed(
                "$message: ${error.message ?: error.javaClass.simpleName}",
            ),
        )
    }

    private data class LaunchTarget(
        val intent: Intent,
        val packageName: String,
    )

    private companion object {
        const val Tag = "OpenLauncherMapWidget"
        const val WebApkStartUrlMetadata = "org.chromium.webapk.shell_apk.startUrl"
        const val WebBrowserProbeUrl = "https://example.com"
        const val ReadyPollMillis = 100L
        const val MaxReadyPolls = 50
        const val LaunchTaskPollMillis = 100L
        const val SteadyTaskPollMillis = 400L
        const val TargetStablePolls = 2
        // The accepted Phase 11 path waited about three seconds before relocating a task that
        // initially landed on the host display. Keeping the same dwell time avoids racing an
        // app's launcher/onboarding transition before startActivityFromRecents/moveStackToDisplay.
        const val EscapedTaskStablePollsLaunch = 30
        const val EscapedTaskStablePollsSteady = 8
        const val IntentionalBackMissingPolls = 1
        const val IntentionalBackWindowMillis = 3_000L
        const val MissingTaskGracePolls = 8
        const val MaxTaskPolls = 80
    }
}
