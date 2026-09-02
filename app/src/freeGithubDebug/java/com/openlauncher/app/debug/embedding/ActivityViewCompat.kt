package com.openlauncher.app.debug.embedding

import android.content.Context
import android.content.Intent
import android.hardware.display.VirtualDisplay
import android.os.SystemClock
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import java.lang.reflect.InvocationTargetException

/**
 * Clean-room wrapper around Android 9's hidden AOSP android.app.ActivityView API.
 *
 * This file intentionally lives in the Free+Debug source set while YT5760D privilege testing is
 * incomplete. No Play Paid or normal production launcher code should depend on it.
 */
class ActivityViewCompat(
    context: Context,
    private val capabilities: EmbeddingCapabilities = EmbeddingCapabilities.detect(context),
) : FrameLayout(context) {
    private val activityViewClass = runCatching { Class.forName(ActivityViewClassName) }.getOrNull()
    private val activityManagerCompat = runCatching { ActivityManagerCompat() }.getOrNull()
    private var activityView: View? = null
    private var pendingFocusRestore: Runnable? = null

    val isAttached: Boolean
        get() = activityView != null

    val virtualDisplayId: Int?
        get() {
            val view = activityView ?: return null
            val clazz = activityViewClass ?: return null
            val methodResult = runCatching {
                (clazz.getMethod("getVirtualDisplayId").invoke(view) as Number).toInt()
            }.getOrNull()?.takeIf { it >= 0 }
            if (methodResult != null) return methodResult

            return runCatching {
                val field = clazz.getDeclaredField("mVirtualDisplay").apply {
                    isAccessible = true
                }
                (field.get(view) as? VirtualDisplay)?.display?.displayId
            }.getOrNull()?.takeIf { it >= 0 }
        }

    fun attach(): Result<Unit> = runCatching {
        check(capabilities.strategy == EmbeddingStrategy.ActivityView) {
            "ActivityView strategy unavailable: ${capabilities.summary()}"
        }
        check(capabilities.canHostOwnedActivity) {
            "ActivityView host requirements missing: ${capabilities.summary()}"
        }
        if (activityView != null) return Result.success(Unit)

        val clazz = activityViewClass ?: error("android.app.ActivityView is unavailable")
        val view = clazz
            .getConstructor(Context::class.java)
            .newInstance(context) as View
        activityView = view
        addView(
            view,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
    }.mapFailure(::unwrap)

    fun startActivity(intent: Intent, external: Boolean): Result<Unit> = runCatching {
        check(activityView != null) { "ActivityView is not attached" }
        if (external) {
            check(capabilities.canHostExternalActivity) {
                "External ActivityView launch requires INTERNAL_SYSTEM_WINDOW"
            }
        }
        val clazz = activityViewClass ?: error("android.app.ActivityView is unavailable")
        clazz.getMethod("startActivity", Intent::class.java).invoke(activityView, intent)
        Unit
    }.mapFailure(::unwrap)

    fun performBackPress(): Result<Unit> = runCatching {
        val view = activityView ?: error("ActivityView is not attached")
        val clazz = activityViewClass ?: error("android.app.ActivityView is unavailable")
        val directMethod = runCatching { clazz.getMethod("performBackPress") }.getOrNull()
        if (directMethod != null) {
            directMethod.invoke(view)
            return@runCatching
        }

        check(capabilities.manageActivityStacks) {
            "Android 9 embedded Back requires MANAGE_ACTIVITY_STACKS"
        }
        val displayId = virtualDisplayId ?: error("ActivityView virtual display is unavailable")
        val activityManager = activityManagerCompat
            ?: error("Android 9 IActivityManager compatibility bridge is unavailable")
        val previousStackId = activityManager.focusStackOnDisplay(displayId).getOrThrow()
        try {
            forwardDisplayKeyEvent(view, clazz, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)
            forwardDisplayKeyEvent(view, clazz, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK)
        } catch (error: Throwable) {
            activityManager.restoreFocusedStack(previousStackId).getOrThrow()
            throw error
        }
        pendingFocusRestore?.let(view::removeCallbacks)
        pendingFocusRestore = Runnable {
            pendingFocusRestore = null
            activityManager.restoreFocusedStack(previousStackId)
        }.also { restore ->
            view.postDelayed(restore, FocusRestoreDelayMillis)
        }
    }.mapFailure(::unwrap)

    fun release() {
        val view = activityView ?: return
        pendingFocusRestore?.let(view::removeCallbacks)
        pendingFocusRestore = null
        runCatching { activityViewClass?.getMethod("release")?.invoke(view) }
        removeView(view)
        activityView = null
    }

    override fun onDetachedFromWindow() {
        release()
        super.onDetachedFromWindow()
    }

    private fun <T> Result<T>.mapFailure(transform: (Throwable) -> Throwable): Result<T> =
        fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(transform(it)) },
        )

    private fun unwrap(error: Throwable): Throwable =
        if (error is InvocationTargetException) error.targetException ?: error else error

    private fun forwardDisplayKeyEvent(
        view: View,
        clazz: Class<*>,
        action: Int,
        keyCode: Int,
    ) {
        val forwarderField = clazz.getDeclaredField("mInputForwarder").apply {
            isAccessible = true
        }
        val forwarder = forwarderField.get(view) ?: error("ActivityView input forwarder is unavailable")
        val forwardEvent = forwarder.javaClass.methods.firstOrNull { method ->
            method.name == "forwardEvent" && method.parameterTypes.size == 1
        } ?: error("ActivityView input forwarder has no forwardEvent(InputEvent)")
        val whenMillis = SystemClock.uptimeMillis()
        val event = KeyEvent(
            whenMillis,
            whenMillis,
            action,
            keyCode,
            0,
            0,
            KeyCharacterMap.VIRTUAL_KEYBOARD,
            0,
            KeyEvent.FLAG_FROM_SYSTEM or KeyEvent.FLAG_VIRTUAL_HARD_KEY,
            InputDevice.SOURCE_KEYBOARD,
        )
        check(forwardEvent.invoke(forwarder, event) == true) {
            "Key event was not forwarded into the ActivityView display"
        }
    }

    companion object {
        private const val ActivityViewClassName = "android.app.ActivityView"
        private const val FocusRestoreDelayMillis = 100L
    }
}
