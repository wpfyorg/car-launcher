package com.openlauncher.app.feature.navigation.external

import android.content.Context
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
    expanded: Boolean = false,
    onExpandedChange: (Boolean) -> Unit = {},
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
    val canStop = hostState is EmbeddingHostState.Running
    val canRestart = canStop || hostState == EmbeddingHostState.Stopped || hostState is EmbeddingHostState.Failed

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

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            shape = RoundedCornerShape(14.dp),
            color = Color.Black.copy(alpha = 0.62f),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { onExpandedChange(!expanded) },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                        contentDescription = if (expanded) "Collapse navigation" else "Expand navigation",
                        tint = Color.White,
                    )
                }
                if (supportsInteractiveEmbedding) {
                    IconButton(
                        onClick = { embeddedView?.restart() },
                        enabled = canRestart,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Restart navigation app",
                            tint = Color.White,
                        )
                    }
                    IconButton(
                        onClick = { embeddedView?.stop() },
                        enabled = canStop,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close navigation app",
                            tint = Color.White,
                        )
                    }
                }
            }
        }

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
        val supported = clazz
            .getMethod("isSupported", Context::class.java)
            .invoke(null, context) as? Boolean == true
        if (!supported) return@runCatching null
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
