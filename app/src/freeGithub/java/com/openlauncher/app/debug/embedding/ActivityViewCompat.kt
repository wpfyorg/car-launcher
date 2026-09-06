package com.openlauncher.app.debug.embedding

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import java.lang.reflect.InvocationTargetException

/**
 * Clean-room wrapper around Android 9's hidden AOSP android.app.ActivityView API.
 *
 * This file intentionally lives in the Free+Debug source set while YT5760D privilege testing is
 * incomplete. No Play Paid or normal production launcher code should depend on it.
 */
class ActivityViewCompat(
    private val hostContext: Context,
    private val capabilities: EmbeddingCapabilities = EmbeddingCapabilities.detect(hostContext),
) : FrameLayout(hostContext) {
    private val activityViewClass = runCatching { Class.forName(ActivityViewClassName) }.getOrNull()
    private val activityManagerCompat = runCatching { ActivityManagerCompat(hostContext) }.getOrNull()
    private var activityView: View? = null
    private var orientationGuard: View? = null
    private var orientationGuardWindowManager: WindowManager? = null
    private var hostOrientationGuard: View? = null
    private var hostOrientationGuardWindowManager: WindowManager? = null

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
        check(capabilities.internalSystemWindow) {
            "Host orientation guard requires INTERNAL_SYSTEM_WINDOW"
        }
        installHostOrientationGuardIfNeeded()
        if (activityView != null) return Result.success(Unit)

        val clazz = activityViewClass ?: error("android.app.ActivityView is unavailable")
        val view = clazz
            .getConstructor(Context::class.java)
            .newInstance(context) as View
        view.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            notifyLocationChanged(view, clazz)
        }
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
        installHostOrientationGuardIfNeeded()
        installOrientationGuardIfNeeded()
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
        activityManager.focusStackOnDisplay(displayId).getOrThrow()
        check(activityManager.focusedStackDisplayId().getOrThrow() == displayId) {
            "Navigation display $displayId did not become focused"
        }
        forwardDisplayKeyEvent(view, clazz, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)
        forwardDisplayKeyEvent(view, clazz, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK)
    }.mapFailure(::unwrap)

    fun forwardMotionEvent(event: MotionEvent): Result<Unit> = runCatching {
        val view = activityView ?: error("ActivityView is not attached")
        val clazz = activityViewClass ?: error("android.app.ActivityView is unavailable")
        forwardInputEvent(view, clazz, event)
    }.mapFailure(::unwrap)

    fun forwardKeyEvent(event: KeyEvent): Result<Unit> = runCatching {
        val view = activityView ?: error("ActivityView is not attached")
        val clazz = activityViewClass ?: error("android.app.ActivityView is unavailable")
        forwardInputEvent(view, clazz, event)
    }.mapFailure(::unwrap)

    fun release() {
        orientationGuard?.let { guard ->
            runCatching { orientationGuardWindowManager?.removeViewImmediate(guard) }
        }
        orientationGuard = null
        orientationGuardWindowManager = null
        hostOrientationGuard?.let { guard ->
            runCatching { hostOrientationGuardWindowManager?.removeViewImmediate(guard) }
        }
        hostOrientationGuard = null
        hostOrientationGuardWindowManager = null
        val view = activityView ?: return
        val virtualDisplay = runCatching {
            val clazz = activityViewClass ?: error("android.app.ActivityView is unavailable")
            val field = clazz.getDeclaredField("mVirtualDisplay").apply {
                isAccessible = true
            }
            field.get(view) as? VirtualDisplay
        }.getOrNull()
        val hiddenRelease = runCatching {
            val clazz = activityViewClass ?: error("android.app.ActivityView is unavailable")
            clazz.getMethod("release").invoke(view)
        }
        hiddenRelease.onFailure { error ->
            Log.w(Tag, "Hidden ActivityView.release() failed; releasing captured VirtualDisplay", unwrap(error))
            runCatching { virtualDisplay?.release() }
                .onFailure { fallbackError ->
                    Log.w(Tag, "Captured VirtualDisplay.release() fallback failed", fallbackError)
                }
        }
        removeView(view)
        activityView = null
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
        forwardInputEvent(view, clazz, event)
    }

    private fun forwardInputEvent(
        view: View,
        clazz: Class<*>,
        event: InputEvent,
    ) {
        val forwarderField = clazz.getDeclaredField("mInputForwarder").apply {
            isAccessible = true
        }
        val forwarder = forwarderField.get(view) ?: error("ActivityView input forwarder is unavailable")
        val forwardEvent = forwarder.javaClass.methods.firstOrNull { method ->
            method.name == "forwardEvent" && method.parameterTypes.size == 1
        } ?: error("ActivityView input forwarder has no forwardEvent(InputEvent)")
        check(forwardEvent.invoke(forwarder, event) == true) {
            "Input event was not forwarded into the ActivityView display"
        }
    }

    private fun notifyLocationChanged(view: View, clazz: Class<*>) {
        runCatching { clazz.getMethod("onLocationChanged").invoke(view) }
    }

    private fun installOrientationGuardIfNeeded() {
        if (orientationGuard != null) return
        check(capabilities.internalSystemWindow) {
            "ActivityView orientation guard requires INTERNAL_SYSTEM_WINDOW"
        }
        val displayId = virtualDisplayId ?: error("ActivityView virtual display is unavailable")
        val displayManager = hostContext.getSystemService(DisplayManager::class.java)
        val display = displayManager.getDisplay(displayId)
            ?: error("ActivityView display $displayId is unavailable")
        val displayContext = hostContext.createDisplayContext(display)
        val windowManager = displayContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val guard = View(displayContext)
        windowManager.addView(
            guard,
            WindowManager.LayoutParams(
                1,
                1,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT,
            ).apply {
                // Android 9 maps app-requested portrait/landscape through the primary display's
                // natural rotation even on secondary displays. ActivityView's virtual display is
                // created in the pane's natural orientation, so keeping the secondary display at
                // rotation 0 avoids orientation ping-pong in apps such as OsmAnd without changing
                // the host display or rewriting another app's requested orientation.
                screenOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR
                alpha = OrientationGuardAlpha
            },
        )
        orientationGuardWindowManager = windowManager
        orientationGuard = guard
    }

    private fun installHostOrientationGuardIfNeeded() {
        if (hostOrientationGuard != null) return
        check(capabilities.internalSystemWindow) {
            "Host orientation guard requires INTERNAL_SYSTEM_WINDOW"
        }
        val windowManager = hostContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val guard = View(hostContext)
        windowManager.addView(
            guard,
            WindowManager.LayoutParams(
                1,
                1,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT,
            ).apply {
                // API 28 has one globally focused stack. A focused task on an ActivityView display
                // can therefore temporarily drive display 0's rotation despite MainActivity's
                // landscape manifest request. Keep the car launcher display landscape while the
                // embedded session owns global focus.
                screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                alpha = OrientationGuardAlpha
            },
        )
        hostOrientationGuardWindowManager = windowManager
        hostOrientationGuard = guard
    }

    companion object {
        private const val ActivityViewClassName = "android.app.ActivityView"
        private const val OrientationGuardAlpha = 0.01f
        private const val Tag = "ActivityViewCompat"
    }
}
