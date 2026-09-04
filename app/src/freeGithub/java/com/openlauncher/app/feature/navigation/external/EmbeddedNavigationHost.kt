package com.openlauncher.app.feature.navigation.external

import android.content.Context
import android.view.View
import androidx.activity.compose.BackHandler
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
    var supportsInteractiveEmbedding by remember(app.stableKey) {
        mutableStateOf(false)
    }
    var embeddedView by remember(app.stableKey) {
        mutableStateOf<EmbeddedNavigationView?>(null)
    }

    BackHandler(
        enabled = embeddedView?.supportsEmbeddedBack == true && hostState is EmbeddingHostState.Running,
    ) {
        embeddedView?.performBackPress()
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            factory = ::createEmbeddedNavigationView,
            onRelease = { view ->
                (view as? EmbeddedNavigationView)?.release()
            },
            update = { view ->
                (view as? EmbeddedNavigationView)?.let { embedded ->
                    embeddedView = embedded
                    supportsInteractiveEmbedding = embedded.supportsInteractiveEmbedding
                    embedded.bind(app, onStateChanged = { hostState = it })
                    embedded.ensureRunning()
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        val message = when (val state = hostState) {
            EmbeddingHostState.Idle,
            EmbeddingHostState.SurfaceReady,
            EmbeddingHostState.Launching,
            EmbeddingHostState.Stopping,
            -> null

            EmbeddingHostState.Stopped -> "Navigation stopped · tap the pane to resume"

            is EmbeddingHostState.Running -> when {
                !supportsInteractiveEmbedding ->
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

private fun createEmbeddedNavigationView(context: Context): View {
    val privilegedDebugView = runCatching {
        val clazz = Class.forName(PrivilegedDebugViewClassName)
        runCatching {
            clazz.getMethod("acquire", Context::class.java)
                .invoke(null, context) as? View
        }.getOrNull() ?: clazz
            .getConstructor(Context::class.java)
            .newInstance(context) as? View
    }.getOrNull()

    return privilegedDebugView
        ?.takeIf { it is EmbeddedNavigationView }
        ?: EmbeddedTaskView(context)
}

private const val PrivilegedDebugViewClassName =
    "com.openlauncher.app.debug.embedding.ActivityViewNavigationWidget"
