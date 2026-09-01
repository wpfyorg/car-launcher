package com.openlauncher.app.feature.navigation

import com.openlauncher.app.location.LocationSample

interface RoutingEngine {
    suspend fun calculateRoute(
        origin: LocationSample,
        destination: NavigationDestination,
    ): Result<RouteSummary>
}
