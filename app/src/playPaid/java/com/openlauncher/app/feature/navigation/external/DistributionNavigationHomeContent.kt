package com.openlauncher.app.feature.navigation.external

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.openlauncher.app.launcher.LauncherApp

@Composable
fun DistributionNavigationHomeContent(
    app: LauncherApp?,
    compatibilityMode: Boolean,
    modifier: Modifier = Modifier,
) {
    // Paid navigation will render the native MapLibre/Ferrostar surface in later phases.
}
