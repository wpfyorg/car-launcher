package com.openlauncher.app.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Settings
import com.openlauncher.app.design.component.CarGridItem
import com.openlauncher.app.design.component.CarHeader
import com.openlauncher.app.design.component.CarSearchField
import com.openlauncher.app.design.theme.CarColors

@Composable
fun AppsRoute(onOpenSettings: () -> Unit) {
    val stateHolder = rememberAppsStateHolder()
    AppsScreen(
        state = stateHolder.uiState,
        onQueryChange = stateHolder::updateQuery,
        onAppClick = stateHolder::launch,
        onTogglePinned = stateHolder::togglePinned,
        onOpenAppInfo = stateHolder::openAppInfo,
        onOpenSettings = onOpenSettings,
        iconLoader = stateHolder::loadIcon,
    )
}

@Composable
fun AppsScreen(
    state: AppsUiState,
    onQueryChange: (String) -> Unit,
    onAppClick: (LauncherApp) -> Unit,
    onTogglePinned: (LauncherApp) -> Unit,
    onOpenAppInfo: (LauncherApp) -> Unit,
    onOpenSettings: () -> Unit,
    iconLoader: suspend (LauncherApp) -> ImageBitmap?,
) {
    var contextMenuAppKey by remember { mutableStateOf<String?>(null) }
    val showsSettings = state.query.isBlank() || "settings".contains(state.query, ignoreCase = true)

    Column(modifier = Modifier.fillMaxSize()) {
        CarHeader(title = "Apps")
        CarSearchField(
            query = state.query,
            onQueryChange = onQueryChange,
            modifier = Modifier.padding(start = 16.dp, end = 24.dp, bottom = 12.dp),
        )

        when {
            state.isLoading && state.apps.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = CarColors.AccentMuted)
                }
            }

            state.visibleApps.isEmpty() && !showsSettings -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (state.query.isBlank()) "No launchable apps" else "No matching apps",
                        color = CarColors.TextSecondary,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 176.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 24.dp, bottom = 16.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp),
                ) {
                    if (showsSettings) {
                        item(key = "openlauncher-settings") {
                            CarGridItem(
                                label = "Settings",
                                onClick = onOpenSettings,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(CarColors.SurfaceElevated, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = CarColors.TextPrimary,
                                    )
                                }
                            }
                        }
                    }
                    items(
                        items = state.visibleApps,
                        key = LauncherApp::stableKey,
                    ) { app ->
                        Box {
                            CarGridItem(
                                label = app.label,
                                onClick = { onAppClick(app) },
                                onLongClick = { contextMenuAppKey = app.stableKey },
                            ) {
                                LauncherAppIcon(app = app, iconLoader = iconLoader)
                                if (state.isPinned(app)) {
                                    Icon(
                                        imageVector = Icons.Rounded.PushPin,
                                        contentDescription = "Pinned",
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(24.dp),
                                        tint = CarColors.AccentMuted,
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = contextMenuAppKey == app.stableKey,
                                onDismissRequest = { contextMenuAppKey = null },
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(if (state.isPinned(app)) "Unpin" else "Pin")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.PushPin,
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        contextMenuAppKey = null
                                        onTogglePinned(app)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("App info") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Info,
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        contextMenuAppKey = null
                                        onOpenAppInfo(app)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LauncherAppIcon(
    app: LauncherApp,
    iconLoader: suspend (LauncherApp) -> ImageBitmap?,
) {
    val image by produceState<ImageBitmap?>(initialValue = null, app.stableKey) {
        value = iconLoader(app)
    }

    if (image != null) {
        Image(
            bitmap = image!!,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            contentScale = ContentScale.Fit,
        )
    } else {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(FallbackIconColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = app.label.take(1).uppercase(),
                color = CarColors.Background,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

private val FallbackIconColor = Color(0xFFE3E2E2)
