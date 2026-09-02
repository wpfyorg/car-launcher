package com.openlauncher.app.debug.embedding

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

    private fun Any.intField(name: String): Int =
        javaClass.getField(name).getInt(this)
}
