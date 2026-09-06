package com.openlauncher.app.feature.navigation.integrated

import com.openlauncher.app.location.LocationSample

data class MapCoordinate(
    val latitude: Double,
    val longitude: Double,
)

enum class MapCameraMode {
    Free,
    Follow,
    FollowHeading,
}

data class MapRenderState(
    val cameraMode: MapCameraMode = MapCameraMode.FollowHeading,
    val night: Boolean = true,
    val lastFix: LocationSample? = null,
    val routeGeometry: List<MapCoordinate> = emptyList(),
)
