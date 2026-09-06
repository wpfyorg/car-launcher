package com.openlauncher.app.debug.embedding

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.os.DropBoxManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.openlauncher.app.data.settings.SettingsRepository
import com.openlauncher.app.feature.navigation.external.EmbeddingHostState
import com.openlauncher.app.launcher.LauncherApp
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Activity-scoped host surface consumed by the process-scoped embedding engine. */
internal interface NavigationEmbeddingHostBridge {
    val navigationDisplayId: Int?

    fun startExternalActivity(intent: Intent): Result<Unit>

    fun performEmbeddedBack(): Result<Unit>

    fun onEngineStateChanged(state: EmbeddingHostState)
}

/**
 * Process-scoped owner of provider/task state. It deliberately owns no Activity or View; the
 * Activity-scoped host is held weakly and can be replaced without duplicating task state.
 */
internal class NavigationEmbeddingEngine private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager
    private val displayManager = appContext.getSystemService(DisplayManager::class.java)
    private val dropBoxManager = appContext.getSystemService(DropBoxManager::class.java)
    private val activityManager = ActivityManagerCompat(appContext)
    private val settingsRepository = SettingsRepository(appContext)
    private val stateStore = NavigationEmbeddingStateStore(appContext)
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var hostReference = WeakReference<NavigationEmbeddingHostBridge>(null)
    private var hostReady = false
    private var hostDestroying = false
    private var selectedProvider: ProviderSelection? = null
    private var currentOwnership: RuntimeOwnership? = null
    private var activeTaskId: Int? = null
    private var persistedTask: PersistedNavigationTask? = null
    private var settingsLoaded = false
    private var persistedTaskLoaded = false
    private var generation = 0
    private var stateSequence = 0L
    private var launchWallTimeMillis = 0L
    private var stoppedGeneration: Int? = null
    private var stoppedRuntimePackages: Set<String> = emptySet()
    private var stoppedLaunchWallTimeMillis = 0L
    private val seenOwnedTaskPackages = linkedSetOf<String>()

    // Declared before init because registerCrashReceiver() runs during construction.
    private val crashReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != DropBoxEntryAddedAction) return
            handler.post {
                val provider = selectedProvider ?: return@post
                val stoppedAtGeneration = stoppedGeneration ?: return@post
                if (currentState != EmbeddingHostState.Stopped || stoppedAtGeneration != generation) return@post
                if (
                    hasCrashEvidenceSince(
                        stoppedLaunchWallTimeMillis,
                        stoppedRuntimePackages,
                    )
                ) {
                    Log.w(Tag, "late crash evidence found for ${provider.app.packageName}; relaunching")
                    launchSelected("late crash recovery")
                }
            }
        }
    }

    var currentState: EmbeddingHostState = EmbeddingHostState.Idle
        private set

    init {
        scope.launch {
            persistedTask = runCatching { stateStore.current() }
                .onFailure { Log.w(Tag, "Unable to restore embedded task snapshot", it) }
                .getOrNull()
            persistedTaskLoaded = true
            maybeStartSelected()
        }
        scope.launch {
            settingsRepository.settings
                .map { it.navigationAppKey }
                .distinctUntilChanged()
                .collect { appKey ->
                    settingsLoaded = true
                    selectProvider(resolveLauncherApp(appKey), source = "settings")
                }
        }
        registerCrashReceiver()
    }

    fun attachHost(host: NavigationEmbeddingHostBridge) {
        hostReference = WeakReference(host)
        hostReady = false
        host.onEngineStateChanged(currentState)
    }

    fun onHostReady(host: NavigationEmbeddingHostBridge) {
        if (hostReference.get() !== host) return
        hostReady = true
        if (currentState == EmbeddingHostState.Idle) transition(EmbeddingHostState.SurfaceReady)
        maybeStartSelected()
    }

    fun destroyHost(host: NavigationEmbeddingHostBridge, onSafeToRelease: () -> Unit) {
        if (hostReference.get() !== host) {
            onSafeToRelease()
            return
        }
        hostDestroying = true
        hostReady = false
        val ownership = currentOwnership
        val appKey = ownership?.appKey ?: selectedProvider?.app?.stableKey
        val stopGeneration = ++generation
        if (ownership == null || ownership.runtimePackages.isEmpty()) {
            finishHostDestroy(host, appKey, onSafeToRelease)
            return
        }
        stopRuntime(
            ownership = ownership,
            generation = stopGeneration,
            reason = "host destroy",
        ) {
            finishHostDestroy(host, appKey, onSafeToRelease)
        }
    }

    fun onHostReleased() {
        handler.postDelayed(
            {
                val count = activityViewDisplayCount()
                val expectedCount = if (hostReference.get() == null) 0 else 1
                if (count != expectedCount) {
                    invariantFailure("host released with $count ActivityView displays; expected=$expectedCount")
                }
            },
            InvariantSettleMillis,
        )
    }

    fun performBack(): Boolean {
        if (currentState !is EmbeddingHostState.Running) return false
        return hostReference.get()
            ?.performEmbeddedBack()
            ?.onFailure { Log.w(Tag, "Unable to forward embedded Back", it) }
            ?.isSuccess == true
    }

    fun resumeStopped() {
        if (currentState != EmbeddingHostState.Stopped || !hostReady || hostDestroying) return
        launchSelected(reason = "pane tap resume")
    }

    fun stopSelected(): Boolean {
        if (hostDestroying || currentState !is EmbeddingHostState.Running) return false
        val provider = selectedProvider ?: return false
        val ownership = currentOwnership ?: resolveLaunchPlan(provider.app).ownership
        val stopGeneration = ++generation
        stoppedGeneration = null
        stoppedRuntimePackages = emptySet()
        stopRuntime(ownership, stopGeneration, reason = "explicit stop") {
            activeTaskId = null
            currentOwnership = null
            clearPersistedTask(provider.app.stableKey)
            transition(EmbeddingHostState.Stopped)
        }
        return true
    }

    fun restartSelected(): Boolean {
        if (
            hostDestroying ||
            currentState !is EmbeddingHostState.Running &&
            currentState != EmbeddingHostState.Stopped &&
            currentState !is EmbeddingHostState.Failed
        ) {
            return false
        }
        val provider = selectedProvider ?: return false
        val ownership = currentOwnership ?: resolveLaunchPlan(provider.app).ownership
        val restartGeneration = ++generation
        stoppedGeneration = null
        stoppedRuntimePackages = emptySet()
        stopRuntime(ownership, restartGeneration, reason = "explicit restart") {
            activeTaskId = null
            currentOwnership = null
            clearPersistedTask(provider.app.stableKey)
            if (selectedProvider?.app?.stableKey == provider.app.stableKey) {
                transition(EmbeddingHostState.SurfaceReady)
                launchSelected("explicit restart")
            }
        }
        return true
    }

    /** Debug POC entry point; production provider selection is driven by SettingsRepository. */
    fun selectProviderForDebug(app: LauncherApp?) {
        selectProvider(app, source = "debug")
    }

    private fun selectProvider(app: LauncherApp?, source: String) {
        val previous = selectedProvider
        if (previous?.app?.stableKey == app?.stableKey) {
            maybeStartSelected()
            return
        }

        val previousOwnership = currentOwnership ?: previous?.let { defaultOwnership(it.app) }
        selectedProvider = app?.let(::ProviderSelection)
        stoppedGeneration = null
        stoppedRuntimePackages = emptySet()
        val stopGeneration = ++generation
        Log.i(Tag, "provider selected source=$source previous=${previous?.app?.stableKey} next=${app?.stableKey}")

        if (previousOwnership != null) {
            stopRuntime(previousOwnership, stopGeneration, reason = "provider switch") {
                activeTaskId = null
                currentOwnership = null
                previous?.app?.stableKey?.let(::clearPersistedTask)
                assertNoTasks(ownedTaskPackages(previousOwnership), "provider switch completed")
                if (app == null) transition(EmbeddingHostState.Idle) else launchSelected("provider switch")
            }
        } else if (app == null) {
            stopMismatchedPersistedTaskIfNeeded()
            transition(EmbeddingHostState.Idle)
        } else {
            launchSelected("provider selection")
        }
    }

    private fun maybeStartSelected() {
        if (!settingsLoaded || !persistedTaskLoaded || !hostReady || hostDestroying) return
        if (currentState == EmbeddingHostState.Stopped) return
        if (currentState == EmbeddingHostState.Launching || currentState == EmbeddingHostState.Stopping) return
        if (currentState is EmbeddingHostState.Running) return
        if (selectedProvider == null) {
            stopMismatchedPersistedTaskIfNeeded()
            return
        }
        launchSelected(reason = "host ready")
    }

    private fun launchSelected(reason: String) {
        if (!hostReady || hostDestroying || !settingsLoaded || !persistedTaskLoaded) return
        val provider = selectedProvider ?: run {
            transition(EmbeddingHostState.Idle)
            return
        }
        val displayId = hostReference.get()?.navigationDisplayId ?: return

        val snapshot = persistedTask
        if (snapshot != null && snapshot.appKey != provider.app.stableKey) {
            val ownership = RuntimeOwnership(
                appKey = snapshot.appKey,
                runtimePackages = snapshot.runtimePackages,
                forceStopPackages = emptySet(),
            )
            val stopGeneration = ++generation
            stopRuntime(ownership, stopGeneration, reason = "stale persisted provider") {
                clearPersistedTask(snapshot.appKey)
                launchSelected(reason)
            }
            return
        }

        if (snapshot != null && snapshot.appKey == provider.app.stableKey) {
            val location = activityManager.findStackByTaskId(snapshot.taskId).getOrNull()
            if (location != null) {
                val ownership = RuntimeOwnership(
                    appKey = snapshot.appKey,
                    runtimePackages = snapshot.runtimePackages,
                    forceStopPackages = setOf(provider.app.packageName),
                )
                currentOwnership = ownership
                activeTaskId = snapshot.taskId
                seenOwnedTaskPackages += ownedTaskPackages(ownership)
                launchWallTimeMillis = System.currentTimeMillis()
                val launchGeneration = ++generation
                transition(EmbeddingHostState.Launching)
                if (location.displayId != displayId) {
                    activityManager.startTaskFromRecents(snapshot.taskId, displayId).fold(
                        onSuccess = {
                            Log.i(Tag, "restoring task=${snapshot.taskId} display=${location.displayId}->$displayId")
                            watchTask(provider, ownership, displayId, launchGeneration, trackedTaskId = snapshot.taskId)
                        },
                        onFailure = { fail("Unable to restore ${provider.app.label}", it) },
                    )
                } else {
                    watchTask(provider, ownership, displayId, launchGeneration, trackedTaskId = snapshot.taskId)
                }
                return
            }
            clearPersistedTask(snapshot.appKey)
        }

        val plan = resolveLaunchPlan(provider.app)
        currentOwnership = plan.ownership
        seenOwnedTaskPackages += ownedTaskPackages(plan.ownership)
        val launchGeneration = ++generation
        transition(EmbeddingHostState.Launching)
        Log.i(Tag, "launching ${provider.app.packageName} reason=$reason runtime=${plan.ownership.runtimePackages}")
        activityManager.purgeTasks(ownedTaskPackages(plan.ownership)).fold(
            onSuccess = { waitForLaunchPurge(provider, plan, displayId, launchGeneration, attempt = 0) },
            onFailure = { fail("Unable to purge old ${provider.app.label} tasks", it) },
        )
    }

    private fun waitForLaunchPurge(
        provider: ProviderSelection,
        plan: LaunchPlan,
        displayId: Int,
        launchGeneration: Int,
        attempt: Int,
    ) {
        handler.postDelayed(
            {
                if (!isCurrent(provider, launchGeneration)) return@postDelayed
                activityManager.findTasksByBasePackages(ownedTaskPackages(plan.ownership)).fold(
                    onSuccess = { tasks ->
                        if (tasks.isEmpty()) {
                            startLaunchIntent(provider, plan, displayId, launchGeneration)
                        } else if (attempt + 1 >= MaxPurgePolls) {
                            fail(
                                "Unable to purge old ${provider.app.label} tasks",
                                IllegalStateException("remaining tasks=${tasks.map { it.taskId }}"),
                            )
                        } else {
                            waitForLaunchPurge(provider, plan, displayId, launchGeneration, attempt + 1)
                        }
                    },
                    onFailure = { fail("Unable to verify ${provider.app.label} task purge", it) },
                )
            },
            TaskPollMillis,
        )
    }

    private fun startLaunchIntent(
        provider: ProviderSelection,
        plan: LaunchPlan,
        displayId: Int,
        launchGeneration: Int,
    ) {
        val host = hostReference.get() ?: return
        val intent = Intent(plan.intent).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        launchWallTimeMillis = System.currentTimeMillis()
        stoppedGeneration = null
        stoppedRuntimePackages = emptySet()
        host.startExternalActivity(intent).fold(
            onSuccess = {
                watchTask(provider, plan.ownership, displayId, launchGeneration)
            },
            onFailure = { fail("Unable to launch ${provider.app.label}", it) },
        )
    }

    private fun watchTask(
        provider: ProviderSelection,
        ownership: RuntimeOwnership,
        displayId: Int,
        launchGeneration: Int,
        trackedTaskId: Int? = null,
        attempt: Int = 0,
        stableTaskId: Int? = null,
        stableDisplayId: Int? = null,
        stablePolls: Int = 0,
    ) {
        val delay = if (currentState is EmbeddingHostState.Running) SteadyTaskPollMillis else TaskPollMillis
        handler.postDelayed(
            {
                if (!isCurrent(provider, launchGeneration) || hostDestroying) return@postDelayed
                val locationResult = if (trackedTaskId != null) {
                    activityManager.findStackByTaskId(trackedTaskId)
                } else {
                    activityManager.findTaskByBasePackages(ownership.runtimePackages, displayId)
                }
                locationResult.fold(
                    onSuccess = { location ->
                        if (location == null) {
                            if (trackedTaskId != null) {
                                findReplacementOrStop(provider, ownership, displayId, launchGeneration)
                            } else if (attempt + 1 >= MaxLaunchPolls) {
                                fail(
                                    "${provider.app.label} did not produce an embeddable task",
                                    IllegalStateException("no provider task observed"),
                                )
                            } else {
                                watchTask(
                                    provider,
                                    ownership,
                                    displayId,
                                    launchGeneration,
                                    attempt = attempt + 1,
                                )
                            }
                            return@fold
                        }

                        val basePackage = location.basePackageName
                        if (basePackage != null && basePackage !in ownership.runtimePackages) {
                            findReplacementOrStop(provider, ownership, displayId, launchGeneration)
                            return@fold
                        }

                        val nextStablePolls = if (
                            stableTaskId == location.taskId && stableDisplayId == location.displayId
                        ) {
                            stablePolls + 1
                        } else {
                            1
                        }

                        if (location.displayId == displayId) {
                            if (nextStablePolls >= TargetStablePolls) {
                                val wasRunning = currentState is EmbeddingHostState.Running &&
                                    activeTaskId == location.taskId
                                activeTaskId = location.taskId
                                currentOwnership = ownership
                                if (!wasRunning) {
                                    transition(EmbeddingHostState.Running(displayId))
                                    persistTask(provider, ownership, location.taskId)
                                    Log.i(Tag, "confirmed ${provider.app.packageName} task=${location.taskId} display=$displayId")
                                }
                                watchTask(
                                    provider,
                                    ownership,
                                    displayId,
                                    launchGeneration,
                                    trackedTaskId = location.taskId,
                                )
                            } else {
                                watchTask(
                                    provider,
                                    ownership,
                                    displayId,
                                    launchGeneration,
                                    trackedTaskId = location.taskId,
                                    attempt = attempt + 1,
                                    stableTaskId = location.taskId,
                                    stableDisplayId = location.displayId,
                                    stablePolls = nextStablePolls,
                                )
                            }
                            return@fold
                        }

                        if (
                            location.topActivity != null &&
                            location.topActivity.packageName !in ownership.runtimePackages
                        ) {
                            watchTask(
                                provider,
                                ownership,
                                displayId,
                                launchGeneration,
                                trackedTaskId = location.taskId,
                                attempt = attempt + 1,
                                stableTaskId = location.taskId,
                                stableDisplayId = location.displayId,
                                stablePolls = nextStablePolls,
                            )
                            return@fold
                        }

                        val requiredStablePolls = if (currentState is EmbeddingHostState.Running) {
                            EscapedTaskStablePollsSteady
                        } else {
                            EscapedTaskStablePollsLaunch
                        }
                        if (nextStablePolls < requiredStablePolls) {
                            watchTask(
                                provider,
                                ownership,
                                displayId,
                                launchGeneration,
                                trackedTaskId = location.taskId,
                                attempt = attempt + 1,
                                stableTaskId = location.taskId,
                                stableDisplayId = location.displayId,
                                stablePolls = nextStablePolls,
                            )
                            return@fold
                        }

                        rehomeTask(provider, ownership, location, displayId, launchGeneration)
                    },
                    onFailure = { fail("Unable to inspect ${provider.app.label} task", it) },
                )
            },
            delay,
        )
    }

    private fun findReplacementOrStop(
        provider: ProviderSelection,
        ownership: RuntimeOwnership,
        displayId: Int,
        launchGeneration: Int,
    ) {
        activityManager.findTaskByBasePackages(ownership.runtimePackages, displayId).fold(
            onSuccess = { replacement ->
                if (replacement != null) {
                    watchTask(
                        provider,
                        ownership,
                        displayId,
                        launchGeneration,
                        trackedTaskId = replacement.taskId,
                    )
                } else {
                    handleTaskGone(provider, ownership, launchGeneration)
                }
            },
            onFailure = { fail("Unable to discover replacement ${provider.app.label} task", it) },
        )
    }

    private fun handleTaskGone(
        provider: ProviderSelection,
        ownership: RuntimeOwnership,
        launchGeneration: Int,
    ) {
        if (!isCurrent(provider, launchGeneration)) return
        activeTaskId = null
        clearPersistedTask(provider.app.stableKey)
        if (hasCrashEvidenceSince(launchWallTimeMillis, ownership.runtimePackages)) {
            Log.w(Tag, "crash evidence found for ${provider.app.packageName}; relaunching")
            launchSelected("crash recovery")
            return
        }
        stoppedGeneration = launchGeneration
        stoppedRuntimePackages = ownership.runtimePackages
        stoppedLaunchWallTimeMillis = launchWallTimeMillis
        transition(EmbeddingHostState.Stopped)
        Log.i(Tag, "provider task exited ${provider.app.packageName}; waiting for pane tap or crash evidence")
    }

    private fun rehomeTask(
        provider: ProviderSelection,
        ownership: RuntimeOwnership,
        location: StackLocation,
        displayId: Int,
        launchGeneration: Int,
    ) {
        activityManager.startTaskFromRecents(location.taskId, displayId).fold(
            onSuccess = {
                Log.w(
                    Tag,
                    "rehome fired ${provider.app.packageName} task=${location.taskId} " +
                        "display=${location.displayId}->$displayId",
                )
                transition(EmbeddingHostState.Launching)
                watchTask(
                    provider,
                    ownership,
                    displayId,
                    launchGeneration,
                    trackedTaskId = location.taskId,
                )
            },
            onFailure = { fail("Unable to rehome ${provider.app.label}", it) },
        )
    }

    private fun stopRuntime(
        ownership: RuntimeOwnership,
        generation: Int,
        reason: String,
        onStopped: () -> Unit,
    ) {
        transition(EmbeddingHostState.Stopping)
        val trackedTaskId = activeTaskId.takeIf { currentOwnership?.appKey == ownership.appKey }
        val trackedDisplayId = hostReference.get()?.navigationDisplayId
        trackedTaskId?.let { taskId ->
            activityManager.removeTask(taskId)
                .onFailure { Log.w(Tag, "$reason tracked task removal failed task=$taskId", it) }
        }
        activityManager.purgeTasks(ownedTaskPackages(ownership))
            .onFailure { Log.w(Tag, "$reason initial task purge failed", it) }
        waitForRuntimeGone(
            ownership = ownership,
            trackedTaskId = trackedTaskId,
            trackedDisplayId = trackedDisplayId,
            generation = generation,
            reason = reason,
            attempt = 0,
            forceStopAttempted = false,
            onStopped = onStopped,
        )
    }

    private fun waitForRuntimeGone(
        ownership: RuntimeOwnership,
        trackedTaskId: Int?,
        trackedDisplayId: Int?,
        generation: Int,
        reason: String,
        attempt: Int,
        forceStopAttempted: Boolean,
        onStopped: () -> Unit,
    ) {
        handler.postDelayed(
            {
                if (generation != this.generation) return@postDelayed
                findTasksToStop(ownership, trackedTaskId, trackedDisplayId).fold(
                    onSuccess = { tasks ->
                        if (tasks.isEmpty()) {
                            Log.i(
                                Tag,
                                "$reason observed runtime gone owned=${ownedTaskPackages(ownership)} tracked=$trackedTaskId",
                            )
                            onStopped()
                            return@fold
                        }

                        val limit = if (forceStopAttempted) MaxForceStopPolls else MaxStopPolls
                        if (attempt + 1 >= limit) {
                            if (!forceStopAttempted && ownership.forceStopPackages.isNotEmpty()) {
                                ownership.forceStopPackages.forEach { packageName ->
                                    activityManager.forceStopPackage(packageName)
                                        .onFailure { Log.w(Tag, "$reason force-stop failed package=$packageName", it) }
                                }
                                activityManager.purgeTasks(ownedTaskPackages(ownership))
                                waitForRuntimeGone(
                                    ownership,
                                    trackedTaskId,
                                    trackedDisplayId,
                                    generation,
                                    reason,
                                    attempt = 0,
                                    forceStopAttempted = true,
                                    onStopped = onStopped,
                                )
                            } else {
                                fail(
                                    "Unable to stop embedded provider",
                                    IllegalStateException("$reason still has tasks=${tasks.map { it.taskId }}"),
                                )
                            }
                        } else {
                            if ((attempt + 1) % StopRemoveRetryPolls == 0) {
                                tasks.forEach { activityManager.removeTask(it.taskId) }
                            }
                            waitForRuntimeGone(
                                ownership,
                                trackedTaskId,
                                trackedDisplayId,
                                generation,
                                reason,
                                attempt + 1,
                                forceStopAttempted,
                                onStopped,
                            )
                        }
                    },
                    onFailure = { fail("Unable to verify provider stopped", it) },
                )
            },
            TaskPollMillis,
        )
    }

    private fun stopMismatchedPersistedTaskIfNeeded() {
        if (!persistedTaskLoaded) return
        val snapshot = persistedTask ?: return
        if (snapshot.appKey == selectedProvider?.app?.stableKey) return
        val ownership = RuntimeOwnership(
            appKey = snapshot.appKey,
            runtimePackages = snapshot.runtimePackages,
            forceStopPackages = emptySet(),
        )
        val stopGeneration = ++generation
        stopRuntime(ownership, stopGeneration, reason = "orphaned persisted provider") {
            clearPersistedTask(snapshot.appKey)
            if (selectedProvider == null) transition(EmbeddingHostState.Idle) else launchSelected("after orphan cleanup")
        }
    }

    private fun finishHostDestroy(
        host: NavigationEmbeddingHostBridge,
        appKey: String?,
        onSafeToRelease: () -> Unit,
    ) {
        appKey?.let(::clearPersistedTask)
        activeTaskId = null
        currentOwnership = null
        val replacementAttached = hostReference.get()?.let { it !== host } == true
        if (!replacementAttached && hostReference.get() === host) {
            hostReference.clear()
            hostReady = false
        }
        onSafeToRelease()
        hostDestroying = false
        if (replacementAttached && hostReady) {
            transition(EmbeddingHostState.SurfaceReady)
            maybeStartSelected()
        } else {
            transition(EmbeddingHostState.Idle)
        }
    }

    private fun ownedTaskPackages(ownership: RuntimeOwnership): Set<String> =
        ComponentName.unflattenFromString(ownership.appKey)
            ?.packageName
            ?.let(::setOf)
            ?: ownership.forceStopPackages.ifEmpty { ownership.runtimePackages }

    private fun findTasksToStop(
        ownership: RuntimeOwnership,
        trackedTaskId: Int?,
        trackedDisplayId: Int?,
    ): Result<List<StackLocation>> = runCatching {
        val tasks = linkedMapOf<Int, StackLocation>()
        activityManager.findTasksByBasePackages(ownedTaskPackages(ownership))
            .getOrThrow()
            .forEach { location -> tasks[location.taskId] = location }
        if (trackedDisplayId != null) {
            activityManager.findTasksByBasePackages(ownership.runtimePackages)
                .getOrThrow()
                .filter { location -> location.displayId == trackedDisplayId }
                .forEach { location -> tasks[location.taskId] = location }
        }
        trackedTaskId?.let { taskId ->
            activityManager.findStackByTaskId(taskId)
                .getOrThrow()
                ?.let { location -> tasks[location.taskId] = location }
        }
        tasks.values.toList()
    }

    private fun resolveLaunchPlan(app: LauncherApp): LaunchPlan {
        val defaultIntent = Intent.makeMainActivity(app.component)
        val startUrl = runCatching {
            packageManager
                .getApplicationInfo(app.packageName, PackageManager.GET_META_DATA)
                .metaData
                ?.getString(WebApkStartUrlMetadata)
        }.getOrNull()?.takeIf(String::isNotBlank)
            ?: return LaunchPlan(
                intent = defaultIntent,
                ownership = defaultOwnership(app),
            )

        val browserProbe = Intent(Intent.ACTION_VIEW, Uri.parse(WebBrowserProbeUrl)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        val runtime = packageManager
            .resolveActivity(browserProbe, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.takeIf { it.packageName != app.packageName }
            ?: return LaunchPlan(
                intent = defaultIntent,
                ownership = defaultOwnership(app),
            )

        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(startUrl)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            component = ComponentName(runtime.packageName, runtime.name)
        }
        return LaunchPlan(
            intent = webIntent,
            ownership = RuntimeOwnership(
                appKey = app.stableKey,
                runtimePackages = setOf(app.packageName, runtime.packageName),
                // The browser runtime can be shared; never force-stop it.
                forceStopPackages = setOf(app.packageName),
            ),
        )
    }

    private fun defaultOwnership(app: LauncherApp) = RuntimeOwnership(
        appKey = app.stableKey,
        runtimePackages = setOf(app.packageName),
        forceStopPackages = setOf(app.packageName),
    )

    private fun resolveLauncherApp(appKey: String?): LauncherApp? {
        val component = appKey?.let(ComponentName::unflattenFromString) ?: return null
        return runCatching {
            val info = packageManager.getActivityInfo(component, 0)
            LauncherApp(
                component = component,
                label = info.loadLabel(packageManager).toString().ifBlank { component.packageName },
            )
        }.onFailure {
            Log.w(Tag, "Configured navigation provider is unavailable: $appKey", it)
        }.getOrNull()
    }

    private fun persistTask(provider: ProviderSelection, ownership: RuntimeOwnership, taskId: Int) {
        val snapshot = PersistedNavigationTask(
            appKey = provider.app.stableKey,
            taskId = taskId,
            runtimePackages = ownership.runtimePackages,
        )
        persistedTask = snapshot
        scope.launch {
            runCatching { stateStore.save(snapshot) }
                .onFailure { Log.w(Tag, "Unable to persist embedded task=$taskId", it) }
        }
    }

    private fun clearPersistedTask(appKey: String) {
        if (persistedTask?.appKey == appKey) persistedTask = null
        scope.launch {
            runCatching { stateStore.clear(appKey) }
                .onFailure { Log.w(Tag, "Unable to clear persisted embedded task for $appKey", it) }
        }
    }

    private fun registerCrashReceiver() {
        val filter = IntentFilter(DropBoxEntryAddedAction)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(crashReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            appContext.registerReceiver(crashReceiver, filter)
        }
    }

    private fun hasCrashEvidenceSince(sinceMillis: Long, runtimePackages: Set<String>): Boolean {
        if (sinceMillis <= 0L || runtimePackages.isEmpty()) return false
        return runCatching {
            CrashTags.any { tag ->
                var cursor = (sinceMillis - 1L).coerceAtLeast(0L)
                var entry = dropBoxManager.getNextEntry(tag, cursor)
                while (entry != null) {
                    val nextCursor = entry.timeMillis + 1L
                    val text = entry.getText(MaxCrashEntryBytes).orEmpty()
                    entry.close()
                    if (runtimePackages.any { packageName -> crashTextMatchesPackage(text, packageName) }) {
                        return@any true
                    }
                    cursor = nextCursor
                    entry = dropBoxManager.getNextEntry(tag, cursor)
                }
                false
            }
        }.onFailure {
            Log.w(Tag, "DropBox crash evidence unavailable", it)
        }.getOrDefault(false)
    }

    private fun crashTextMatchesPackage(text: String, packageName: String): Boolean =
        text.lineSequence().any { line ->
            (line.startsWith("Package:") || line.startsWith("Process:")) &&
                line.substringAfter(':').trim().substringBefore(' ').substringBefore('/').substringBefore(':') == packageName
        }

    private fun transition(next: EmbeddingHostState) {
        currentState = next
        val sequence = ++stateSequence
        hostReference.get()?.onEngineStateChanged(next)
        checkInvariants()
        if (next == EmbeddingHostState.Launching || next == EmbeddingHostState.Stopping) {
            handler.postDelayed(
                {
                    if (stateSequence == sequence && currentState == next) {
                        invariantFailure("state stuck in ${next.javaClass.simpleName} for ${StateTimeoutMillis}ms")
                    }
                },
                StateTimeoutMillis,
            )
        }
    }

    private fun checkInvariants() {
        if (hostReady) {
            val displays = activityViewDisplayCount()
            if (displays != 1) invariantFailure("host ready with $displays ActivityView displays")
        }
        if (seenOwnedTaskPackages.isEmpty()) return
        val tasks = activityManager.findTasksByBasePackages(seenOwnedTaskPackages).getOrNull() ?: return
        if (tasks.size > 1) {
            invariantFailure("multiple provider tasks alive: ${tasks.map { "${it.taskId}@${it.displayId}" }}")
        }
        if (
            (currentState == EmbeddingHostState.Launching || currentState is EmbeddingHostState.Running) &&
            tasks.any { it.displayId == DefaultDisplayId }
        ) {
            invariantFailure("provider task escaped to display 0: ${tasks.map { it.taskId }}")
        }
        val running = currentState as? EmbeddingHostState.Running
        if (running != null && activeTaskId != null) {
            val location = activityManager.findStackByTaskId(activeTaskId!!).getOrNull()
            if (location != null && location.displayId != running.displayId) {
                invariantFailure(
                    "running task=$activeTaskId on display=${location.displayId}, expected=${running.displayId}",
                )
            }
        }
    }

    private fun assertNoTasks(runtimePackages: Set<String>, reason: String) {
        val tasks = activityManager.findTasksByBasePackages(runtimePackages).getOrNull().orEmpty()
        if (tasks.isNotEmpty()) {
            invariantFailure("$reason left tasks=${tasks.map { "${it.taskId}@${it.displayId}" }}")
        }
    }

    private fun activityViewDisplayCount(): Int =
        displayManager.displays.count { display ->
            display.name?.startsWith(ActivityViewDisplayPrefix) == true
        }

    private fun invariantFailure(message: String) {
        Log.wtf(Tag, "INVARIANT: $message")
        appContext.sendBroadcast(
            Intent(InvariantFailureAction)
                .setPackage(appContext.packageName)
                .putExtra(InvariantFailureExtra, message),
        )
    }

    private fun fail(message: String, error: Throwable) {
        Log.e(Tag, message, error)
        transition(EmbeddingHostState.Failed("$message: ${error.message ?: error.javaClass.simpleName}"))
    }

    private fun isCurrent(provider: ProviderSelection, launchGeneration: Int): Boolean =
        generation == launchGeneration && selectedProvider?.app?.stableKey == provider.app.stableKey

    private data class ProviderSelection(val app: LauncherApp)

    private data class RuntimeOwnership(
        val appKey: String,
        val runtimePackages: Set<String>,
        val forceStopPackages: Set<String>,
    )

    private data class LaunchPlan(
        val intent: Intent,
        val ownership: RuntimeOwnership,
    )

    companion object {
        private const val Tag = "NavigationEmbeddingEngine"
        private const val ActivityViewDisplayPrefix = "ActivityViewVirtualDisplay"
        private const val DefaultDisplayId = 0
        private const val WebApkStartUrlMetadata = "org.chromium.webapk.shell_apk.startUrl"
        private const val WebBrowserProbeUrl = "https://example.com"
        private const val DropBoxEntryAddedAction = "android.intent.action.DROPBOX_ENTRY_ADDED"
        private const val InvariantFailureAction = "com.openlauncher.app.debug.NAVIGATION_INVARIANT_FAILURE"
        private const val InvariantFailureExtra = "message"
        private const val TaskPollMillis = 100L
        private const val SteadyTaskPollMillis = 1_000L
        private const val TargetStablePolls = 2
        private const val EscapedTaskStablePollsLaunch = 30
        private const val EscapedTaskStablePollsSteady = 3
        private const val MaxLaunchPolls = 80
        private const val MaxPurgePolls = 30
        private const val MaxStopPolls = 50
        private const val MaxForceStopPolls = 20
        private const val StopRemoveRetryPolls = 5
        private const val StateTimeoutMillis = 8_000L
        private const val InvariantSettleMillis = 250L
        private const val MaxCrashEntryBytes = 64 * 1024

        private val CrashTags = listOf(
            "data_app_crash",
            "data_app_anr",
            "data_app_native_crash",
            "system_app_crash",
            "system_app_anr",
            "system_app_native_crash",
        )

        @Volatile
        private var instance: NavigationEmbeddingEngine? = null

        fun get(context: Context): NavigationEmbeddingEngine =
            instance ?: synchronized(this) {
                instance ?: NavigationEmbeddingEngine(context.applicationContext).also { instance = it }
            }
    }
}
