package com.openlauncher.app.debug.embedding

import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Process
import android.util.Log

/** Minimal Android 9 hidden-API bridge used by the privileged ActivityView path. */
internal class ActivityManagerCompat(context: Context? = null) {
    private val activityManagerClass = Class.forName("android.app.ActivityManager")
    private val activityManagerInterface = Class.forName("android.app.IActivityManager")
    private val service = activityManagerClass
        .getDeclaredMethod("getService")
        .apply { isAccessible = true }
        .invoke(null)
    private val publicActivityManager = context?.applicationContext
        ?.getSystemService(ActivityManager::class.java)

    fun focusStackOnDisplay(displayId: Int): Result<Unit> = runCatching {
        val target = allStacks().firstOrNull { stack ->
            stack != null && stack.intField("displayId") == displayId
        } ?: error("No activity stack found on display $displayId")
        val targetStackId = target.intField("stackId")

        activityManagerInterface
            .getMethod("setFocusedStack", Int::class.javaPrimitiveType)
            .invoke(service, targetStackId)
        Unit
    }

    fun focusedStackDisplayId(): Result<Int?> = runCatching {
        activityManagerInterface
            .getMethod("getFocusedStackInfo")
            .invoke(service)
            ?.intField("displayId")
    }

    fun findTopActivityStack(
        packageName: String,
        activityClassName: String,
    ): Result<StackLocation?> = runCatching {
        val target = allStacks().firstOrNull { stack ->
            val topActivity = stack?.objectField("topActivity") as? ComponentName
            topActivity?.packageName == packageName && topActivity.className == activityClassName
        } ?: return@runCatching null

        val taskId = target.intArrayField("taskIds").lastOrNull()
            ?: error("ActivityManager.StackInfo has no task ids")
        StackLocation(
            stackId = target.intField("stackId"),
            taskId = taskId,
            displayId = target.intField("displayId"),
            topActivity = target.objectField("topActivity") as? ComponentName,
            baseActivity = target.taskNameForTask(taskId)?.toComponentName(),
        )
    }

    /**
     * Finds a task by its base package instead of StackInfo.topActivity. The base identity remains
     * stable while permission, resolver, sign-in, or other transient activities are on top.
     */
    fun findTaskByBasePackages(
        packageNames: Set<String>,
        preferredDisplayId: Int? = null,
    ): Result<StackLocation?> = runCatching {
        val matches = taskLocations(packageNames)
        matches.firstOrNull { preferredDisplayId != null && it.displayId == preferredDisplayId }
            ?: matches.firstOrNull()
    }

    fun findTasksByBasePackages(packageNames: Set<String>): Result<List<StackLocation>> = runCatching {
        taskLocations(packageNames)
    }

    /** Kept for the standalone POC; production discovery uses base-package matching above. */
    fun findTopActivityStack(
        packageName: String,
        preferredDisplayId: Int? = null,
    ): Result<StackLocation?> = runCatching {
        val matchingStacks = allStacks().filter { stack ->
            val topActivity = stack?.objectField("topActivity") as? ComponentName
            topActivity?.packageName == packageName
        }
        val target = matchingStacks.firstOrNull { stack ->
            preferredDisplayId != null && stack?.intField("displayId") == preferredDisplayId
        } ?: matchingStacks.firstOrNull() ?: return@runCatching null

        val taskId = target.intArrayField("taskIds").lastOrNull()
            ?: error("ActivityManager.StackInfo has no task ids")
        StackLocation(
            stackId = target.intField("stackId"),
            taskId = taskId,
            displayId = target.intField("displayId"),
            topActivity = target.objectField("topActivity") as? ComponentName,
            baseActivity = target.taskNameForTask(taskId)?.toComponentName(),
        )
    }

    fun findStackByTaskId(taskId: Int): Result<StackLocation?> = runCatching {
        val target = allStacks().firstOrNull { stack ->
            stack?.intArrayField("taskIds")?.contains(taskId) == true
        } ?: return@runCatching null
        StackLocation(
            stackId = target.intField("stackId"),
            taskId = taskId,
            displayId = target.intField("displayId"),
            topActivity = target.objectField("topActivity") as? ComponentName,
            baseActivity = target.taskNameForTask(taskId)?.toComponentName(),
        )
    }

    fun startTaskFromRecents(taskId: Int, displayId: Int): Result<Int> = runCatching {
        val options = ActivityOptions.makeBasic()
        ActivityOptions::class.java
            .getMethod("setLaunchDisplayId", Int::class.javaPrimitiveType)
            .invoke(options, displayId)
        activityManagerInterface
            .getMethod("startActivityFromRecents", Int::class.javaPrimitiveType, Bundle::class.java)
            .invoke(service, taskId, options.toBundle()) as Int
    }

    /** Kept only for the standalone ActivityView POC. */
    fun moveStackToDisplay(stackId: Int, displayId: Int): Result<Unit> = runCatching {
        activityManagerInterface
            .getMethod(
                "moveStackToDisplay",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
            )
            .invoke(service, stackId, displayId)
        Unit
    }

    fun removeTask(taskId: Int): Result<Boolean> = runCatching {
        activityManagerInterface
            .getMethod("removeTask", Int::class.javaPrimitiveType)
            .invoke(service, taskId) as Boolean
    }

    /** Removes active and recent tasks whose base package belongs to the provider runtime set. */
    fun purgeTasks(packageNames: Set<String>): Result<Set<Int>> = runCatching {
        val taskIds = linkedSetOf<Int>()
        taskLocations(packageNames).forEach { taskIds += it.taskId }

        val recentTasks = runCatching {
            @Suppress("DEPRECATION")
            publicActivityManager
                ?.getRecentTasks(MaxRecentTasks, ActivityManager.RECENT_IGNORE_UNAVAILABLE)
                .orEmpty()
        }.getOrElse { emptyList() }
        recentTasks.forEach { task ->
            val basePackage = task.baseIntent?.component?.packageName
                ?: task.origActivity?.packageName
            if (basePackage in packageNames) {
                val taskId = task.id.takeIf { it >= 0 } ?: task.persistentId
                if (taskId >= 0) taskIds += taskId
            }
        }

        val removedTaskIds = linkedSetOf<Int>()
        taskIds.forEach { taskId ->
            removeTask(taskId).fold(
                onSuccess = { removed ->
                    if (removed) {
                        removedTaskIds += taskId
                    } else {
                        Log.w(Tag, "removeTask($taskId) returned false")
                    }
                },
                onFailure = { error -> Log.w(Tag, "removeTask($taskId) failed", error) },
            )
        }
        removedTaskIds
    }

    fun forceStopPackage(packageName: String): Result<Unit> = runCatching {
        val userId = Process.myUid() / PerUserRange
        activityManagerInterface
            .getMethod("forceStopPackage", String::class.java, Int::class.javaPrimitiveType)
            .invoke(service, packageName, userId)
        Unit
    }

    private fun allStacks(): List<*> =
        activityManagerInterface
            .getMethod("getAllStackInfos")
            .invoke(service) as? List<*>
            ?: error("IActivityManager.getAllStackInfos() returned no stack list")

    private fun taskLocations(packageNames: Set<String>): List<StackLocation> = buildList {
        allStacks().forEach { stack ->
            if (stack == null) return@forEach
            val taskIds = stack.intArrayField("taskIds")
            val taskNames = stack.stringArrayField("taskNames")
            taskIds.indices.reversed().forEach taskLoop@{ index ->
                val taskId = taskIds[index]
                val taskName = taskNames.getOrNull(index)
                val baseActivity = taskName?.toComponentName()
                val basePackage = baseActivity?.packageName
                    ?: taskName?.substringBefore('/')?.takeIf(String::isNotBlank)
                if (basePackage !in packageNames) return@taskLoop
                add(
                    StackLocation(
                        stackId = stack.intField("stackId"),
                        taskId = taskId,
                        displayId = stack.intField("displayId"),
                        topActivity = stack.objectField("topActivity") as? ComponentName,
                        baseActivity = baseActivity,
                    ),
                )
            }
        }
    }

    private fun Any.taskNameForTask(taskId: Int): String? {
        val taskIds = intArrayField("taskIds")
        val index = taskIds.indexOf(taskId)
        return if (index >= 0) stringArrayField("taskNames").getOrNull(index) else null
    }

    private fun String.toComponentName(): ComponentName? =
        ComponentName.unflattenFromString(this)

    private fun Any.intField(name: String): Int =
        javaClass.getField(name).getInt(this)

    private fun Any.intArrayField(name: String): IntArray =
        javaClass.getField(name).get(this) as IntArray

    private fun Any.stringArrayField(name: String): Array<String> =
        @Suppress("UNCHECKED_CAST")
        (javaClass.getField(name).get(this) as Array<String>)

    private fun Any.objectField(name: String): Any? =
        javaClass.getField(name).get(this)

    private companion object {
        const val Tag = "ActivityManagerCompat"
        const val MaxRecentTasks = 200
        const val PerUserRange = 100_000
    }
}

internal data class StackLocation(
    val stackId: Int,
    val taskId: Int,
    val displayId: Int,
    val topActivity: ComponentName?,
    val baseActivity: ComponentName? = null,
) {
    val basePackageName: String?
        get() = baseActivity?.packageName
}
