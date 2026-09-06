package com.openlauncher.app.feature.navigation.external

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.openlauncher.app.feature.navigation.integrated.MapLibreNavigationSurface
import com.openlauncher.app.feature.navigation.integrated.MapRenderState
import com.openlauncher.app.launcher.LauncherApp

@Composable
fun DistributionNavigationHomeContent(
    app: LauncherApp?,
    compatibilityMode: Boolean,
    expanded: Boolean = false,
    onExpandedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var renderState by remember { mutableStateOf(MapRenderState()) }
    MapLibreNavigationSurface(
        state = renderState,
        onStateChange = { renderState = it },
        modifier = modifier,
    )
}
