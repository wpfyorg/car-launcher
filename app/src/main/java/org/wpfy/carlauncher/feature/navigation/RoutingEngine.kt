package org.wpfy.carlauncher.feature.navigation

import org.wpfy.carlauncher.location.LocationSample

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)

enum class RoutingProfile {
    Driving,
}

data class RouteRequest(
    val origin: LocationSample?,
    val destination: NavigationDestination,
    val profile: RoutingProfile = RoutingProfile.Driving,
    val waypoints: List<GeoPoint> = emptyList(),
)

data class RoutePlan(
    val id: String,
    val destination: NavigationDestination,
    val summary: RouteSummary,
    val geometry: List<GeoPoint>,
    val maneuvers: List<Maneuver>,
)

sealed class RoutingError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    data object NoGps : RoutingError("Current GPS location is unavailable")
    class NetworkUnavailable(cause: Throwable? = null) :
        RoutingError("Routing network is unavailable", cause)
    class ServerUnavailable(
        val statusCode: Int? = null,
        cause: Throwable? = null,
    ) : RoutingError("Routing server is unavailable", cause)
    data object NoRouteFound : RoutingError("No route was found")
    class MalformedResponse(cause: Throwable? = null) :
        RoutingError("Routing response could not be read", cause)
}

interface RoutingEngine {
    suspend fun calculateRoute(request: RouteRequest): Result<RoutePlan>
}
