package com.openlauncher.app.feature.navigation.external

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.openlauncher.app.launcher.LauncherApp

@Composable
fun DistributionNavigationHomeContent(
    app: LauncherApp?,
    compatibilityMode: Boolean,
    expanded: Boolean = false,
    onExpandedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (app != null) {
        EmbeddedNavigationHost(
            app = app,
            compatibilityMode = compatibilityMode,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            modifier = modifier,
        )
    }
}
