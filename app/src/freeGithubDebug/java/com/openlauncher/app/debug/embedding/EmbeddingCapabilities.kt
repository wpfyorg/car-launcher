package com.openlauncher.app.debug.embedding

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

data class EmbeddingCapabilities(
    val sdkInt: Int,
    val secondaryDisplayActivities: Boolean,
    val activityViewAvailable: Boolean,
    val taskViewClassName: String?,
    val injectEvents: Boolean,
    val internalSystemWindow: Boolean,
    val manageActivityStacks: Boolean,
    val startAnyActivity: Boolean,
    val startTasksFromRecents: Boolean,
) {
    val strategy: EmbeddingStrategy
        get() = when {
            sdkInt <= Build.VERSION_CODES.P && activityViewAvailable -> EmbeddingStrategy.ActivityView
            taskViewClassName != null -> EmbeddingStrategy.TaskView
            else -> EmbeddingStrategy.Unsupported
        }

    val canHostOwnedActivity: Boolean
        get() = strategy == EmbeddingStrategy.ActivityView &&
            secondaryDisplayActivities &&
            injectEvents

    val canHostExternalActivity: Boolean
        get() = canHostOwnedActivity && internalSystemWindow

    fun summary(): String = buildString {
        append("strategy=")
        append(strategy.label)
        append(" sdk=")
        append(sdkInt)
        append(" secondary=")
        append(yesNo(secondaryDisplayActivities))
        append(" activityView=")
        append(yesNo(activityViewAvailable))
        append(" taskView=")
        append(taskViewClassName ?: "no")
        append(" injectEvents=")
        append(yesNo(injectEvents))
        append(" internalWindow=")
        append(yesNo(internalSystemWindow))
        append(" manageStacks=")
        append(yesNo(manageActivityStacks))
        append(" startAny=")
        append(yesNo(startAnyActivity))
        append(" startRecents=")
        append(yesNo(startTasksFromRecents))
    }

    companion object {
        fun detect(context: Context): EmbeddingCapabilities {
            val packageManager = context.packageManager
            return EmbeddingCapabilities(
                sdkInt = Build.VERSION.SDK_INT,
                secondaryDisplayActivities = packageManager.hasSystemFeature(
                    PackageManager.FEATURE_ACTIVITIES_ON_SECONDARY_DISPLAYS,
                ),
                activityViewAvailable = classExists(ActivityViewClassName),
                taskViewClassName = TaskViewClassNames.firstOrNull(::classExists),
                injectEvents = context.hasPermission(InjectEventsPermission),
                internalSystemWindow = context.hasPermission(InternalSystemWindowPermission),
                manageActivityStacks = context.hasPermission(ManageActivityStacksPermission),
                startAnyActivity = context.hasPermission(StartAnyActivityPermission),
                startTasksFromRecents = context.hasPermission(StartTasksFromRecentsPermission),
            )
        }

        private fun classExists(className: String): Boolean =
            runCatching { Class.forName(className) }.isSuccess

        private fun Context.hasPermission(permission: String): Boolean =
            checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

        private fun yesNo(value: Boolean): String = if (value) "yes" else "no"

        private const val ActivityViewClassName = "android.app.ActivityView"
        private const val InjectEventsPermission = "android.permission.INJECT_EVENTS"
        private const val InternalSystemWindowPermission = "android.permission.INTERNAL_SYSTEM_WINDOW"
        private const val ManageActivityStacksPermission = "android.permission.MANAGE_ACTIVITY_STACKS"
        private const val StartAnyActivityPermission = "android.permission.START_ANY_ACTIVITY"
        private const val StartTasksFromRecentsPermission = "android.permission.START_TASKS_FROM_RECENTS"

        private val TaskViewClassNames = listOf(
            "android.app.TaskView",
            "com.android.wm.shell.TaskView",
            "com.android.wm.shell.taskview.TaskView",
            "com.android.systemui.shared.taskview.TaskView",
        )
    }
}

enum class EmbeddingStrategy(val label: String) {
    ActivityView("activity-view"),
    TaskView("task-view"),
    Unsupported("unsupported"),
}
