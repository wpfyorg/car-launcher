package com.openlauncher.app.debug.embedding

import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import java.lang.reflect.InvocationTargetException

/**
 * Clean-room wrapper around Android 9's hidden AOSP android.app.ActivityView API.
 *
 * This file intentionally lives in the debug source set until a dedicated system/free
 * distribution source set exists. No production launcher code should depend on it.
 */
class ActivityViewCompat(
    context: Context,
    private val capabilities: EmbeddingCapabilities = EmbeddingCapabilities.detect(context),
) : FrameLayout(context) {
    private val activityViewClass = runCatching { Class.forName(ActivityViewClassName) }.getOrNull()
    private var activityView: View? = null

    val isAttached: Boolean
        get() = activityView != null

    val virtualDisplayId: Int?
        get() {
            val view = activityView ?: return null
            val clazz = activityViewClass ?: return null
            return runCatching {
                (clazz.getMethod("getVirtualDisplayId").invoke(view) as Number).toInt()
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
        clazz.getMethod("performBackPress").invoke(view)
        Unit
    }.mapFailure(::unwrap)

    fun release() {
        val view = activityView ?: return
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

    companion object {
        private const val ActivityViewClassName = "android.app.ActivityView"
    }
}
