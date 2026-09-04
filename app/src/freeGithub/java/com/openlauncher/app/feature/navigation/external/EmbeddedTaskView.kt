package com.openlauncher.app.feature.navigation.external

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.openlauncher.app.launcher.LauncherApp

internal class EmbeddedTaskView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : SurfaceView(context, attrs), SurfaceHolder.Callback, EmbeddedNavigationView {
    private val controller = VirtualDisplayController(context, ::handleState)
    private var targetApp: LauncherApp? = null
    private var stateListener: (EmbeddingHostState) -> Unit = {}
    private var sessionHostRegistrationId: Long? = null
    private var sessionHostAppKey: String? = null

    override val supportsInteractiveEmbedding: Boolean
        get() = InputForwarder.isSupported && TaskManagerCompat.canControlEmbeddedTask

    init {
        holder.addCallback(this)
    }

    override fun bind(
        app: LauncherApp?,
        onStateChanged: (EmbeddingHostState) -> Unit,
    ) {
        stateListener = onStateChanged
        val changed = targetApp?.stableKey != app?.stableKey
        targetApp = app
        if (changed && app != null) {
            registerSessionHost(app)
            if (controller.displayId == null && holder.surface.isValid) {
                attachSurface(holder)
            } else {
                controller.launch(app, force = true)
            }
        } else if (app != null) {
            registerSessionHost(app)
        }
    }

    override fun ensureRunning() {
        targetApp?.let(controller::launch)
    }

    override fun release() {
        // Keep the process session registered across Compose/View disposal. The public fallback
        // cannot control task ids, but retaining the host lets an explicit provider switch release
        // its display before the next provider starts.
        controller.release()
    }

    private fun registerSessionHost(app: LauncherApp) {
        if (sessionHostRegistrationId != null && sessionHostAppKey == app.stableKey) return
        sessionHostRegistrationId?.let { registrationId ->
            NavigationEmbeddingSession.unregisterHost(registrationId)
        }
        sessionHostRegistrationId = NavigationEmbeddingSession.registerHost(app.stableKey) {
            sessionHostRegistrationId = null
            sessionHostAppKey = null
            controller.release()
        }
        sessionHostAppKey = app.stableKey
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
        release()
    }

    override fun onDetachedFromWindow() {
        release()
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
