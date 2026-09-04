package com.openlauncher.app.feature.navigation.external

import android.util.Log

/**
 * Process-owned record for the currently embedded navigation provider.
 *
 * The active task identity deliberately outlives any individual Compose/AndroidView instance. A
 * provider can therefore be removed synchronously from Settings even if the Home view that created
 * its ActivityView has already been retained, detached, or replaced by Compose.
 */
object NavigationEmbeddingSession {
    data class TrackedTask(
        val appKey: String,
        val runtimePackageName: String,
        val taskId: Int,
        val displayId: Int,
    )

    enum class HostReleaseMode {
        /** A new host for the same provider is taking over the tracked task. */
        PreserveTask,

        /** The provider is being torn down; the display must be released after task removal. */
        Teardown,
    }

    private data class ActiveTask(
        val snapshot: TrackedTask,
        val removeTask: (Int) -> Result<Boolean>,
        val isTaskPresent: (Int) -> Result<Boolean>,
    )

    private data class ActiveHost(
        val registrationId: Long,
        val appKey: String,
        val release: (HostReleaseMode) -> Unit,
    )

    private var activeTask: ActiveTask? = null
    private var activeHost: ActiveHost? = null
    private var nextRegistrationId = 1L

    /**
     * Registers the current display host. Replacing a host for the same provider preserves the
     * process-owned task; registering a different provider first tears the old provider down.
     */
    fun registerHost(
        appKey: String,
        release: (HostReleaseMode) -> Unit,
    ): Long {
        val currentTaskAppKey = activeTask?.snapshot?.appKey
        val currentHost = activeHost
        if (
            (currentTaskAppKey != null && currentTaskAppKey != appKey) ||
            (currentHost != null && currentHost.appKey != appKey)
        ) {
            releaseActive(reason = "register $appKey")
        } else if (currentHost != null) {
            // Same-provider AndroidView replacement: release only the old display. The tracked task
            // remains process-owned so the new ActivityView can adopt/rehome it instead of creating
            // a duplicate task.
            activeHost = null
            currentHost.release(HostReleaseMode.PreserveTask)
        }

        val registrationId = nextRegistrationId++
        activeHost = ActiveHost(registrationId, appKey, release)
        return registrationId
    }

    /**
     * Stops treating a host as the current display owner without touching the process-owned task.
     * Stale host callbacks are ignored so an old View cannot disturb a newer same-provider host.
     */
    fun unregisterHost(registrationId: Long): Boolean {
        val host = activeHost ?: return false
        if (host.registrationId != registrationId) return false
        activeHost = null
        return true
    }

    /** Updates process-owned task identity as soon as the watcher discovers the task. */
    fun trackTask(
        appKey: String,
        runtimePackageName: String,
        taskId: Int,
        displayId: Int,
        removeTask: (Int) -> Result<Boolean>,
        isTaskPresent: (Int) -> Result<Boolean>,
    ) {
        val previous = activeTask
        if (previous != null && previous.snapshot.appKey != appKey) {
            // A new provider host may already be active by the time its watcher discovers a task.
            // Remove only the stale task here; tearing down the host would destroy the new display.
            if (!removeTrackedTask(previous, reason = "replace tracked task with $appKey")) return
            activeTask = null
        }
        activeTask = ActiveTask(
            snapshot = TrackedTask(
                appKey = appKey,
                runtimePackageName = runtimePackageName,
                taskId = taskId,
                displayId = displayId,
            ),
            removeTask = removeTask,
            isTaskPresent = isTaskPresent,
        )
    }

    fun trackedTask(appKey: String): TrackedTask? =
        activeTask?.snapshot?.takeIf { it.appKey == appKey }

    fun clearTrackedTask(appKey: String, taskId: Int? = null) {
        val task = activeTask ?: return
        if (task.snapshot.appKey != appKey) return
        if (taskId != null && task.snapshot.taskId != taskId) return
        activeTask = null
    }

    /**
     * Provider-switch teardown. The task is removed first while its ActivityView display still
     * exists, then the host display is released. Both identities are cleared before callbacks run
     * so release callbacks cannot recursively tear down a newly registered provider.
     */
    fun releaseActive(reason: String = "provider switch"): Boolean {
        val task = activeTask
        val host = activeHost

        if (task != null && !removeTrackedTask(task, reason)) {
            // Keep the durable identity and host registration intact so a later attempt can retry
            // the exact same task instead of silently orphaning it.
            return false
        }
        activeTask = null
        activeHost = null
        host?.release(HostReleaseMode.Teardown)
        return true
    }

    private fun removeTrackedTask(task: ActiveTask, reason: String): Boolean {
        val snapshot = task.snapshot
        return task.removeTask(snapshot.taskId).fold(
            onSuccess = { removed ->
                val gone = removed || task.isTaskPresent(snapshot.taskId)
                    .fold(
                        onSuccess = { present -> !present },
                        onFailure = { false },
                    )
                Log.i(
                    Tag,
                    "$reason app=${snapshot.appKey} runtime=${snapshot.runtimePackageName} " +
                        "task=${snapshot.taskId} display=${snapshot.displayId} removed=$removed gone=$gone",
                )
                gone
            }
            ,
            onFailure = { error ->
                Log.w(
                    Tag,
                    "$reason unable to remove task=${snapshot.taskId} app=${snapshot.appKey}",
                    error,
                )
                false
            },
        )
    }

    private const val Tag = "NavigationEmbeddingSession"
}
