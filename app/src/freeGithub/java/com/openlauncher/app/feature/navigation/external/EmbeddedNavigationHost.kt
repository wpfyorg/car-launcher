package com.openlauncher.app.feature.navigation.external

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.openlauncher.app.launcher.LauncherApp

@Composable
fun EmbeddedNavigationHost(
    app: LauncherApp,
    compatibilityMode: Boolean,
    modifier: Modifier = Modifier,
) {
    var hostState by remember(app.stableKey) {
        mutableStateOf<EmbeddingHostState>(EmbeddingHostState.Idle)
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            factory = { context -> EmbeddedTaskView(context) },
            update = { view ->
                view.bind(app, onStateChanged = { hostState = it })
                view.ensureRunning()
            },
            modifier = Modifier.fillMaxSize(),
        )

        val message = when (val state = hostState) {
            EmbeddingHostState.Idle,
            EmbeddingHostState.SurfaceReady,
            -> null

            is EmbeddingHostState.Running -> when {
                !InputForwarder.isSupported || !TaskManagerCompat.canControlEmbeddedTask ->
                    "Navigation preview only · touch awaits YT5760D privilege verification"

                compatibilityMode -> "Compatibility mode active"
                else -> null
            }

            is EmbeddingHostState.Unavailable -> state.reason
            is EmbeddingHostState.Failed -> state.reason
        }

        if (message != null) {
            Text(
                text = message,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp),
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
