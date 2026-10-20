package org.wpfy.carlauncher.feature.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AltRoute
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.ui.graphics.vector.ImageVector
import org.wpfy.carlauncher.feature.navigation.NavigationError
import org.wpfy.carlauncher.feature.navigation.NavigationState

/**
 * True when the turn guidance (banner or compact strip) is on screen; every other navigation state
 * shows a status strip instead.
 */
internal fun showsGuidance(state: NavigationState): Boolean =
    !state.progress.arrived &&
        state.error == null &&
        state.gpsAvailable &&
        state.active &&
        state.progress.nextManeuver != null

/** What a navigation status strip's action does; the surface maps each kind to its callback. */
internal enum class NavigationStatusAction { SearchDestination, OpenAppSettings, Retry, OpenNavigationSettings }

internal class NavigationStatus(
    val icon: ImageVector,
    val title: String,
    val body: String,
    val actionLabel: String,
    val action: NavigationStatusAction,
    /**
     * Nothing is wrong, so the strip may fold to its search button alone when its icon, heading
     * and caption do not all fit on one line each. Warnings and errors always keep their text.
     */
    val collapsible: Boolean = false,
)

/** The status strip shown instead of turn guidance, or null while guidance is on screen. */
internal fun navigationStatusFor(state: NavigationState): NavigationStatus? = when {
    state.progress.arrived -> NavigationStatus(
        icon = Icons.Rounded.CheckCircle,
        title = "You’ve arrived",
        body = "Route guidance has ended",
        actionLabel = "Search destination",
        action = NavigationStatusAction.SearchDestination,
    )

    state.error == NavigationError.PermissionDenied -> NavigationStatus(
        icon = Icons.Rounded.LocationOff,
        title = "Location permission required",
        body = "Allow location access to use navigation",
        actionLabel = "Open app settings",
        action = NavigationStatusAction.OpenAppSettings,
    )

    !state.gpsAvailable -> NavigationStatus(
        icon = Icons.Rounded.LocationOff,
        title = "GPS unavailable",
        body = "Waiting for a location fix",
        actionLabel = "Try again",
        action = NavigationStatusAction.Retry,
    )

    state.error == NavigationError.ProviderUnavailable -> NavigationStatus(
        icon = Icons.Rounded.AltRoute,
        title = "Navigation app unavailable",
        body = "Choose or install a compatible navigation app",
        actionLabel = "Navigation settings",
        action = NavigationStatusAction.OpenNavigationSettings,
    )

    state.error != null -> NavigationStatus(
        icon = Icons.Rounded.AltRoute,
        title = when (state.error) {
            NavigationError.RouteUnavailable -> "No route found"
            else -> "Navigation couldn’t start"
        },
        body = when (state.error) {
            NavigationError.RouteUnavailable -> "Choose another destination"
            else -> "Check the destination and try again"
        },
        actionLabel = "Try again",
        action = NavigationStatusAction.Retry,
    )

    state.active && state.progress.nextManeuver != null -> null

    else -> NavigationStatus(
        icon = Icons.Rounded.Navigation,
        title = "Ready to drive",
        body = "Choose a destination",
        actionLabel = "Search destination",
        action = NavigationStatusAction.SearchDestination,
        collapsible = true,
    )
}
