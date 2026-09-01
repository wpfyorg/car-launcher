package com.openlauncher.app.feature.navigation.external

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.openlauncher.app.launcher.LauncherApp

internal class EmbeddedTaskView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : SurfaceView(context, attrs), SurfaceHolder.Callback {
    private val controller = VirtualDisplayController(context, ::handleState)
    private var targetApp: LauncherApp? = null
    private var stateListener: (EmbeddingHostState) -> Unit = {}

    init {
        holder.addCallback(this)
    }

    fun bind(
        app: LauncherApp?,
        onStateChanged: (EmbeddingHostState) -> Unit,
    ) {
        stateListener = onStateChanged
        val changed = targetApp?.stableKey != app?.stableKey
        targetApp = app
        if (changed && app != null) {
            controller.launch(app, force = true)
        }
    }

    fun ensureRunning() {
        targetApp?.let(controller::launch)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        attachSurface(holder)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (controller.displayId == null) {
            attachSurface(holder)
        } else {
            controller.resize(width, height, resources.displayMetrics.densityDpi)
            ensureRunning()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        controller.release()
    }

    override fun onDetachedFromWindow() {
        controller.release()
        super.onDetachedFromWindow()
    }

    private fun attachSurface(holder: SurfaceHolder) {
        val frame = holder.surfaceFrame
        val surfaceWidth = frame.width().takeIf { it > 0 } ?: this.width
        val surfaceHeight = frame.height().takeIf { it > 0 } ?: this.height
        if (
            controller.attach(
                surface = holder.surface,
                width = surfaceWidth,
                height = surfaceHeight,
                densityDpi = resources.displayMetrics.densityDpi,
            )
        ) {
            ensureRunning()
        }
    }

    private fun handleState(state: EmbeddingHostState) {
        post { stateListener(state) }
    }
}
