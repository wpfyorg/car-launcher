package org.wpfy.carlauncher.feature.home

import androidx.compose.runtime.Composable
import org.wpfy.carlauncher.design.kit.KitMapControl
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.BoxScope

@Composable
internal fun DistributionPaidHomeScreen(
    state: HomeUiState,
    navigationContent: @Composable BoxScope.() -> Unit,
    navigationContentOwnsControls: Boolean,
    navigationExpanded: Boolean,
    onSearchClick: () -> Unit,
    onLocationPermissionClick: () -> Unit,
    onStopNavigation: () -> Unit,
    onMediaClick: () -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaPrevious: () -> Unit,
    onMediaNext: () -> Unit,
    onAppsClick: () -> Unit,
    /** The map's round controls (recenter, theme, guidance mute); the map no longer draws its own. */
    mapControls: List<KitMapControl> = emptyList(),
    onTripClick: (() -> Unit)? = null,
) {
    HomeContent(
        state = state,
        navigationContent = navigationContent,
        navigationContentOwnsControls = navigationContentOwnsControls,
        navigationExpanded = navigationExpanded,
        onSearchClick = onSearchClick,
        onLocationPermissionClick = onLocationPermissionClick,
        onStopNavigation = onStopNavigation,
        onMediaClick = onMediaClick,
        onMediaPlayPause = onMediaPlayPause,
        onMediaPrevious = onMediaPrevious,
        onMediaNext = onMediaNext,
        mapControls = mapControls,
        onTripClick = onTripClick,
    )
}
