package com.openlauncher.app.debug.embedding

import android.app.ActivityOptions
import android.content.ComponentName
import android.os.Bundle

/**
 * Minimal Android 9 hidden-API bridge used by the ActivityView lab only.
 */
internal class ActivityManagerCompat {
    private val activityManagerClass = Class.forName("android.app.ActivityManager")
    private val activityManagerInterface = Class.forName("android.app.IActivityManager")
    private val service = activityManagerClass
        .getDeclaredMethod("getService")
        .apply { isAccessible = true }
        .invoke(null)

    fun focusStackOnDisplay(displayId: Int): Result<Int?> = runCatching {
        val previousStackId = activityManagerInterface
            .getMethod("getFocusedStackInfo")
            .invoke(service)
            ?.intField("stackId")

        val stacks = activityManagerInterface
            .getMethod("getAllStackInfos")
            .invoke(service) as? List<*>
            ?: error("IActivityManager.getAllStackInfos() returned no stack list")

        val target = stacks.firstOrNull { stack ->
            stack != null && stack.intField("displayId") == displayId
        } ?: error("No activity stack found on display $displayId")
        val targetStackId = target.intField("stackId")

        activityManagerInterface
            .getMethod("setFocusedStack", Int::class.javaPrimitiveType)
            .invoke(service, targetStackId)

        previousStackId
    }

    fun restoreFocusedStack(stackId: Int?): Result<Unit> = runCatching {
        if (stackId == null) return@runCatching
        activityManagerInterface
            .getMethod("setFocusedStack", Int::class.javaPrimitiveType)
            .invoke(service, stackId)
        Unit
    }

    fun findTopActivityStack(
        packageName: String,
        activityClassName: String,
    ): Result<StackLocation?> = runCatching {
        val stacks = activityManagerInterface
            .getMethod("getAllStackInfos")
            .invoke(service) as? List<*>
            ?: error("IActivityManager.getAllStackInfos() returned no stack list")

        val target = stacks.firstOrNull { stack ->
            val topActivity = stack?.objectField("topActivity") as? ComponentName
            topActivity?.packageName == packageName && topActivity.className == activityClassName
        } ?: return@runCatching null

        val stackId = target.intField("stackId")
        val taskId = target.intArrayField("taskIds").lastOrNull()
            ?: error("ActivityManager.StackInfo has no task ids")
        val topActivity = target.objectField("topActivity") as? ComponentName
            ?: error("ActivityManager.StackInfo has no top activity")
        StackLocation(stackId, taskId, target.intField("displayId"), topActivity)
    }

    fun findTopActivityStack(
        packageName: String,
        preferredDisplayId: Int? = null,
    ): Result<StackLocation?> = runCatching {
        val stacks = activityManagerInterface
            .getMethod("getAllStackInfos")
            .invoke(service) as? List<*>
            ?: error("IActivityManager.getAllStackInfos() returned no stack list")

        val matchingStacks = stacks.filter { stack ->
            val topActivity = stack?.objectField("topActivity") as? ComponentName
            topActivity?.packageName == packageName
        }
        val target = matchingStacks.firstOrNull { stack ->
            preferredDisplayId != null && stack?.intField("displayId") == preferredDisplayId
        } ?: matchingStacks.firstOrNull() ?: return@runCatching null

        val stackId = target.intField("stackId")
        val taskId = target.intArrayField("taskIds").lastOrNull()
            ?: error("ActivityManager.StackInfo has no task ids")
        val topActivity = target.objectField("topActivity") as? ComponentName
            ?: error("ActivityManager.StackInfo has no top activity")
        StackLocation(stackId, taskId, target.intField("displayId"), topActivity)
    }

    fun findStackByTaskId(taskId: Int): Result<StackLocation?> = runCatching {
        val stacks = activityManagerInterface
            .getMethod("getAllStackInfos")
            .invoke(service) as? List<*>
            ?: error("IActivityManager.getAllStackInfos() returned no stack list")

        val target = stacks.firstOrNull { stack ->
            stack?.intArrayField("taskIds")?.contains(taskId) == true
        } ?: return@runCatching null
        val topActivity = target.objectField("topActivity") as? ComponentName
        StackLocation(
            stackId = target.intField("stackId"),
            taskId = taskId,
            displayId = target.intField("displayId"),
            topActivity = topActivity,
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

    private fun Any.intField(name: String): Int =
        javaClass.getField(name).getInt(this)

    private fun Any.intArrayField(name: String): IntArray =
        javaClass.getField(name).get(this) as IntArray

    private fun Any.objectField(name: String): Any? =
        javaClass.getField(name).get(this)
}

internal data class StackLocation(
    val stackId: Int,
    val taskId: Int,
    val displayId: Int,
    val topActivity: ComponentName?,
)
