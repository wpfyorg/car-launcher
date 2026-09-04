package com.openlauncher.app.debug.embedding

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.openlauncher.app.feature.navigation.external.EmbeddingHostState
import com.openlauncher.app.launcher.LauncherApp

/**
 * Debug-only harness around the exact ActivityView host used by the Home navigation surface.
 *
 * Pass a launcher package with --es package <name>. This avoids app-specific POC code and lets
 * Phase 11 exercise the production-shaped privileged widget against arbitrary installed apps.
 */
class ActivityViewWidgetPocActivity : Activity() {
    private lateinit var statusView: TextView
    private lateinit var widget: ActivityViewNavigationWidget

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        statusView = TextView(this).apply {
            setTextColor(Color.WHITE)
            setBackgroundColor(0x99000000.toInt())
            textSize = 14f
            setPadding(16, 10, 16, 10)
            text = "Waiting for target"
        }
        widget = ActivityViewNavigationWidget.acquire(this) as ActivityViewNavigationWidget

        setContentView(
            FrameLayout(this).apply {
                setBackgroundColor(Color.BLACK)
                addView(
                    widget,
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
            },
        )

        bindTarget(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        bindTarget(intent)
    }

    private fun bindTarget(intent: Intent?) {
        val packageName = intent?.getStringExtra(ExtraPackage)?.trim().orEmpty()
        if (packageName.isBlank()) {
            statusView.text = "Missing --es $ExtraPackage <package>"
            return
        }
        val launcherIntent = packageManager.getLaunchIntentForPackage(packageName)
        val component = launcherIntent?.component
        if (component == null) {
            statusView.text = "No launcher activity for $packageName"
            return
        }
        val label = runCatching {
            packageManager.getActivityInfo(component, 0).loadLabel(packageManager).toString()
        }.getOrDefault(packageName)
        val app = LauncherApp(component = component, label = label)
        statusView.text = "Starting $label"
        widget.bind(app) { state -> renderState(label, state) }
        NavigationEmbeddingEngine.get(this).selectProviderForDebug(app)
    }

    private fun renderState(label: String, state: EmbeddingHostState) {
        statusView.text = when (state) {
            EmbeddingHostState.Idle -> "$label · idle"
            EmbeddingHostState.SurfaceReady -> "$label · surface ready"
            EmbeddingHostState.Launching -> "$label · launching"
            EmbeddingHostState.Stopping -> "$label · stopping"
            EmbeddingHostState.Stopped -> "$label · stopped · tap pane to resume"
            is EmbeddingHostState.Running -> "$label · running on display ${state.displayId}"
            is EmbeddingHostState.Unavailable -> "$label · unavailable: ${state.reason}"
            is EmbeddingHostState.Failed -> "$label · failed: ${state.reason}"
        }
    }

    private companion object {
        const val ExtraPackage = "package"
    }
}
