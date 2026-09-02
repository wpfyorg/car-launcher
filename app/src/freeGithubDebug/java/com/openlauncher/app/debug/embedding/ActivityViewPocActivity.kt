package com.openlauncher.app.debug.embedding

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Android 9 system-build experiment based on AOSP's hidden android.app.ActivityView API.
 *
 * This stays in the Free+Debug source set because ActivityView is not an SDK API and requires
 * platform/signature privileges for input forwarding and arbitrary third-party embedding.
 */
class ActivityViewPocActivity : Activity() {
    private lateinit var host: FrameLayout
    private lateinit var statusView: TextView
    private lateinit var capabilities: EmbeddingCapabilities
    private var activityView: ActivityViewCompat? = null
    private var pendingTarget: LaunchTarget? = null
    private var readyPollCount = 0
    private var compactSize = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        statusView = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(12, 8, 12, 8)
        }

        host = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(
                statusView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP or Gravity.END,
                ),
            )
        }

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.rgb(32, 33, 35))
            setPadding(8, 8, 8, 8)
            addView(actionButton("Probe") { launch(LaunchTarget.Probe) })
            addView(actionButton("Organic") { launch(LaunchTarget.OrganicMaps) })
            addView(actionButton("Maps") { launch(LaunchTarget.Maps) })
            addView(actionButton("Back") { performEmbeddedBack() })
            addView(actionButton("Resize") { toggleActivityViewSize() })
            addView(actionButton("Release") { releaseActivityView() })
            addView(actionButton("Recreate") { recreateActivityView() })
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
                    host,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1f,
                    ),
                )
            },
        )

        capabilities = EmbeddingCapabilities.detect(this)
        logStatus(capabilities.summary())
        if (capabilities.canHostOwnedActivity) {
            createActivityView()
        }
    }

    override fun onDestroy() {
        releaseActivityView()
        super.onDestroy()
    }

    private fun createActivityView() {
        if (activityView != null) return
        if (!capabilities.canHostOwnedActivity) {
            logStatus("ActivityView blocked\n${capabilities.summary()}")
            return
        }

        val view = ActivityViewCompat(this, capabilities)
        view.attach().fold(
            onSuccess = {
                activityView = view
                host.addView(
                    view,
                    0,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
                readyPollCount = 0
                waitForReady()
                logStatus("ActivityView attached; waiting for virtual display")
            },
            onFailure = { error ->
                logStatus("ActivityView create failed: ${error.javaClass.simpleName}: ${error.message}")
            },
        )
    }

    private fun waitForReady() {
        val view = activityView ?: return
        val displayId = view.virtualDisplayId

        if (displayId != null && displayId >= 0) {
            logStatus("ActivityView ready display=$displayId")
            pendingTarget?.also {
                pendingTarget = null
                launch(it)
            }
            return
        }

        if (readyPollCount++ < MaxReadyPolls) {
            host.postDelayed(::waitForReady, ReadyPollMillis)
        } else {
            logStatus("ActivityView did not become ready")
        }
    }

    private fun launch(target: LaunchTarget) {
        if (!capabilities.canHostOwnedActivity) {
            logStatus("launch blocked\n${capabilities.summary()}")
            return
        }
        if (target != LaunchTarget.Probe && !capabilities.canHostExternalActivity) {
            logStatus("${target.label} blocked: INTERNAL_SYSTEM_WINDOW not granted")
            return
        }
        if (activityView == null) createActivityView()

        val view = activityView ?: return
        val displayId = view.virtualDisplayId
        if (displayId == null || displayId < 0) {
            pendingTarget = target
            logStatus("queued ${target.label}; ActivityView not ready")
            return
        }

        val intent = targetIntent(target) ?: run {
            logStatus("No launcher intent for ${target.label}")
            return
        }
        view.startActivity(intent, external = target != LaunchTarget.Probe).fold(
            onSuccess = { logStatus("started ${target.label} in ActivityView display=$displayId") },
            onFailure = { error ->
                logStatus("start ${target.label} failed: ${error.javaClass.simpleName}: ${error.message}")
            },
        )
    }

    private fun targetIntent(target: LaunchTarget): Intent? = when (target) {
        LaunchTarget.Probe -> Intent(this, EmbeddedProbeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }
        LaunchTarget.OrganicMaps -> OrganicMapsPackages.firstNotNullOfOrNull(
            packageManager::getLaunchIntentForPackage,
        )
        LaunchTarget.Maps -> packageManager.getLaunchIntentForPackage(GoogleMapsPackage)
    }

    private fun performEmbeddedBack() {
        val view = activityView ?: return
        view.performBackPress().fold(
            onSuccess = { logStatus("embedded back sent") },
            onFailure = { error ->
                logStatus("embedded back failed: ${error.javaClass.simpleName}: ${error.message}")
            },
        )
    }

    private fun releaseActivityView() {
        val view = activityView ?: return
        view.release()
        host.removeView(view)
        activityView = null
        pendingTarget = null
        logStatus("ActivityView released")
    }

    private fun recreateActivityView() {
        releaseActivityView()
        createActivityView()
    }

    private fun toggleActivityViewSize() {
        val view = activityView ?: return
        val width = host.width
        val height = host.height
        if (width == 0 || height == 0) {
            logStatus("resize blocked: host has not been measured")
            return
        }

        compactSize = !compactSize
        val params = FrameLayout.LayoutParams(
            if (compactSize) (width * CompactScale).toInt() else ViewGroup.LayoutParams.MATCH_PARENT,
            if (compactSize) (height * CompactScale).toInt() else ViewGroup.LayoutParams.MATCH_PARENT,
        ).apply {
            gravity = if (compactSize) Gravity.CENTER else Gravity.NO_GRAVITY
        }
        view.layoutParams = params
        view.requestLayout()
        val label = if (compactSize) "compact" else "full"
        logStatus("ActivityView resize requested: $label display=${view.virtualDisplayId}")
    }

    private fun actionButton(label: String, onClick: () -> Unit): View =
        Button(this).apply {
            text = label
            setOnClickListener { onClick() }
        }

    private fun logStatus(message: String) {
        Log.i(Tag, message.replace('\n', ' '))
        statusView.text = message
    }

    private enum class LaunchTarget(val label: String) {
        Probe("probe"),
        OrganicMaps("Organic Maps"),
        Maps("maps"),
    }

    companion object {
        private const val Tag = "OpenLauncherActivityView"
        private const val GoogleMapsPackage = "com.google.android.apps.maps"
        private val OrganicMapsPackages = listOf("app.organicmaps", "app.organicmaps.web")
        private const val CompactScale = 0.67f
        private const val ReadyPollMillis = 100L
        private const val MaxReadyPolls = 50
    }
}
