package com.openlauncher.app.debug.embedding

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class VirtualDisplayPocActivity : Activity(), SurfaceHolder.Callback {
    private lateinit var surfaceView: SurfaceView
    private lateinit var statusView: TextView
    private var virtualDisplay: VirtualDisplay? = null
    private var currentSurface: Surface? = null
    private var currentWidth = InitialWidth
    private var currentHeight = InitialHeight
    private var lastLaunch: LaunchTarget = LaunchTarget.Probe
    private val supportsSecondaryDisplayActivities: Boolean
        get() = packageManager.hasSystemFeature(
            PackageManager.FEATURE_ACTIVITIES_ON_SECONDARY_DISPLAYS,
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "OpenLauncher VirtualDisplay POC"

        surfaceView = SurfaceView(this).apply {
            setBackgroundColor(Color.BLACK)
            holder.addCallback(this@VirtualDisplayPocActivity)
        }
        statusView = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            setBackgroundColor(Color.argb(205, 0, 0, 0))
            setPadding(12, 8, 12, 8)
            text = "Waiting for SurfaceView…"
        }

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 8, 8, 8)
            setBackgroundColor(Color.rgb(32, 33, 35))
            addView(actionButton("Probe") { launch(LaunchTarget.Probe) })
            addView(actionButton("Maps") { launch(LaunchTarget.Maps) })
            addView(actionButton("Resize") { toggleResize() })
            addView(actionButton("Release") { releaseDisplay() })
            addView(actionButton("Recreate") { recreateDisplayAndLaunch() })
        }

        val frame = FrameLayout(this).apply {
            addView(
                surfaceView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
            addView(
                statusView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP or Gravity.END,
                ),
            )
        }

        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.BLACK)
                addView(
                    controls,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ),
                )
                addView(
                    frame,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1f,
                    ),
                )
            },
        )

        surfaceView.setOnTouchListener { _, event ->
            logStatus(
                "host touch ${event.actionMasked} @ ${event.x.toInt()},${event.y.toInt()} " +
                    "(not forwarded yet)",
            )
            true
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        currentSurface = holder.surface
        createDisplayIfNeeded()
        if (supportsSecondaryDisplayActivities) {
            logStatus("display ready; choose Probe or Maps")
        } else {
            logStatus("secondary-display activities unsupported on this device")
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        currentSurface = holder.surface
        logStatus("surface ${width}x$height display=${virtualDisplay?.display?.displayId}")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        currentSurface = null
        releaseDisplay()
    }

    override fun onDestroy() {
        releaseDisplay()
        super.onDestroy()
    }

    private fun createDisplayIfNeeded(): Boolean {
        if (virtualDisplay != null) return true
        val surface = currentSurface ?: return false
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        return runCatching {
            virtualDisplay = displayManager.createVirtualDisplay(
                "OpenLauncher-POC",
                currentWidth,
                currentHeight,
                resources.displayMetrics.densityDpi,
                surface,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION or
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY,
            )
            val displayId = virtualDisplay?.display?.displayId
                ?: error("Virtual display was not created")
            logStatus("created display=$displayId ${currentWidth}x$currentHeight")
            true
        }.getOrElse { error ->
            logStatus("create failed: ${error.javaClass.simpleName}: ${error.message}")
            false
        }
    }

    private fun launch(target: LaunchTarget) {
        lastLaunch = target
        if (!supportsSecondaryDisplayActivities) {
            logStatus("launch blocked: FEATURE_ACTIVITIES_ON_SECONDARY_DISPLAYS=false")
            return
        }
        if (!createDisplayIfNeeded()) return
        val displayId = virtualDisplay?.display?.displayId ?: return
        val intent = when (target) {
            LaunchTarget.Probe -> Intent(this, EmbeddedProbeActivity::class.java)
            LaunchTarget.Maps -> packageManager.getLaunchIntentForPackage(MapsPackage)
                ?: run {
                    logStatus("launch maps failed: package $MapsPackage is not installed")
                    return
                }
        }.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT,
        )

        val options = ActivityOptions.makeBasic().apply {
            launchDisplayId = displayId
        }
        runCatching {
            startActivity(intent, options.toBundle())
            logStatus("launched ${target.label} on display=$displayId")
        }.onFailure { error ->
            logStatus("launch ${target.label} failed: ${error.javaClass.simpleName}: ${error.message}")
        }
    }

    private fun toggleResize() {
        if (!createDisplayIfNeeded()) return
        if (currentWidth == InitialWidth) {
            currentWidth = ResizedWidth
            currentHeight = ResizedHeight
        } else {
            currentWidth = InitialWidth
            currentHeight = InitialHeight
        }
        runCatching {
            virtualDisplay?.resize(
                currentWidth,
                currentHeight,
                resources.displayMetrics.densityDpi,
            )
            logStatus("resized display=${virtualDisplay?.display?.displayId} to ${currentWidth}x$currentHeight")
        }.onFailure { error ->
            logStatus("resize failed: ${error.javaClass.simpleName}: ${error.message}")
        }
    }

    private fun releaseDisplay() {
        val displayId = virtualDisplay?.display?.displayId ?: return
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        logStatus("released display=$displayId")
    }

    private fun recreateDisplayAndLaunch() {
        releaseDisplay()
        if (createDisplayIfNeeded()) {
            surfaceView.postDelayed({ launch(lastLaunch) }, RelaunchDelayMillis)
        }
    }

    private fun actionButton(label: String, onClick: () -> Unit): View =
        Button(this).apply {
            text = label
            setOnClickListener { onClick() }
        }

    private fun logStatus(message: String) {
        Log.i(EmbeddedProbeActivity.Tag, message)
        runOnUiThread {
            statusView.text = buildString {
                append(message)
                append("\nVD=")
                append(virtualDisplay?.display?.displayId ?: "none")
                append(" target=")
                append(lastLaunch.label)
                append(" secondaryActivities=")
                append(if (supportsSecondaryDisplayActivities) "yes" else "no")
            }
        }
    }

    private enum class LaunchTarget(val label: String) {
        Probe("probe"),
        Maps("maps"),
    }

    companion object {
        private const val InitialWidth = 960
        private const val InitialHeight = 540
        private const val ResizedWidth = 720
        private const val ResizedHeight = 480
        private const val RelaunchDelayMillis = 250L
        private const val MapsPackage = "com.google.android.apps.maps"
    }
}
