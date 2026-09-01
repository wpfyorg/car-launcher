package com.openlauncher.app.feature.navigation.external

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.view.Surface
import com.openlauncher.app.launcher.LauncherApp

internal class VirtualDisplayController(
    context: Context,
    private val onStateChanged: (EmbeddingHostState) -> Unit,
) {
    private val appContext = context.applicationContext
    private val displayManager = appContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private var virtualDisplay: VirtualDisplay? = null
    private var launchedAppKey: String? = null

    val displayId: Int?
        get() = virtualDisplay?.display?.displayId

    val supportsSecondaryActivities: Boolean
        get() = appContext.packageManager.hasSystemFeature(
            PackageManager.FEATURE_ACTIVITIES_ON_SECONDARY_DISPLAYS,
        )

    fun attach(surface: Surface, width: Int, height: Int, densityDpi: Int): Boolean {
        if (!supportsSecondaryActivities) {
            onStateChanged(
                EmbeddingHostState.Unavailable(
                    "This device does not support activities on secondary displays",
                ),
            )
            return false
        }
        if (width <= 0 || height <= 0 || !surface.isValid) return false

        if (virtualDisplay != null) {
            virtualDisplay?.setSurface(surface)
            resize(width, height, densityDpi)
            return true
        }

        return runCatching {
            virtualDisplay = displayManager.createVirtualDisplay(
                DisplayName,
                width,
                height,
                densityDpi,
                surface,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION or
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY,
            ) ?: error("Virtual display was not created")
            launchedAppKey = null
            onStateChanged(EmbeddingHostState.SurfaceReady)
            true
        }.getOrElse { error ->
            onStateChanged(EmbeddingHostState.Failed(error.message ?: "Unable to create display"))
            false
        }
    }

    fun resize(width: Int, height: Int, densityDpi: Int) {
        if (width <= 0 || height <= 0) return
        runCatching { virtualDisplay?.resize(width, height, densityDpi) }
    }

    fun launch(app: LauncherApp, force: Boolean = false): Boolean {
        val displayId = displayId ?: return false
        if (!force && launchedAppKey == app.stableKey) return true

        return runCatching {
            val intent = Intent.makeMainActivity(app.component).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK,
                )
            }
            val options = ActivityOptions.makeBasic().apply {
                launchDisplayId = displayId
            }
            appContext.startActivity(intent, options.toBundle())
            launchedAppKey = app.stableKey
            onStateChanged(EmbeddingHostState.Running(displayId))
            true
        }.getOrElse { error ->
            launchedAppKey = null
            onStateChanged(EmbeddingHostState.Failed(error.message ?: "Unable to launch navigation app"))
            false
        }
    }

    fun release() {
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        launchedAppKey = null
        onStateChanged(EmbeddingHostState.Idle)
    }

    private companion object {
        const val DisplayName = "CarLauncher-Navigation"
    }
}
