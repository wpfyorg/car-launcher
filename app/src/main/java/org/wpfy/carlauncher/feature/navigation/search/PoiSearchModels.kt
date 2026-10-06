package org.wpfy.carlauncher.feature.navigation.search

import org.wpfy.carlauncher.feature.navigation.GeoPoint
import org.wpfy.carlauncher.feature.navigation.offline.OfflineRegionBounds

/** The installed-region set to search for a request. */
enum class PoiRegionScope {
    Current,
    Nearby,
    Remaining,
    AllInstalled,
}

/** A search database that belongs to one installed offline region. */
data class PoiSearchRegion(
    val id: String,
    val name: String,
    val databasePath: String,
    val installationIdentity: String? = null,
    val bounds: OfflineRegionBounds? = null,
)

data class PoiSearchRequest(
    val text: String = "",
    val category: String? = null,
    val regionScope: PoiRegionScope = PoiRegionScope.Current,
    val currentLocation: GeoPoint? = null,
    val cameraCenter: GeoPoint? = null,
    val limit: Int = 25,
    val nearbyRegionLimit: Int = 3,
) {
    val distanceOrigin: GeoPoint?
        get() = currentLocation ?: cameraCenter
}

enum class PoiSearchMatchType {
    Exact,
    Prefix,
    Token,
}

data class PoiSearchResult(
    val id: Long,
    val name: String,
    val normalizedName: String = normalizePoiName(name),
    val category: String? = null,
    val subcategory: String? = null,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val locality: String? = null,
    val region: String? = null,
    val sourceId: String? = null,
    val regionId: String? = null,
    val distanceMeters: Double? = null,
    val matchType: PoiSearchMatchType = PoiSearchMatchType.Token,
)

data class FixedCameraQuery(
    val center: GeoPoint? = null,
    val radiusMeters: Double = 5_000.0,
    val regionScope: PoiRegionScope = PoiRegionScope.Current,
    val limit: Int = 100,
    val nearbyRegionLimit: Int = 3,
)

data class FixedCamera(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    val type: String? = null,
    val speedLimitKph: Int? = null,
    val directionDegrees: Int? = null,
    val enforcementType: String? = null,
    val sourceId: String? = null,
    val sourceVersion: String? = null,
    val region: String? = null,
    val regionId: String? = null,
    val distanceMeters: Double? = null,
) {
    /** Alias matching the SQLite column name used by the region builder. */
    val directionDeg: Int?
        get() = directionDegrees
}

enum class PoiSavedPlace {
    Home,
    Work,
}

/** The Paid implementation also exposes this data source for fixed-camera alerts. */
interface FixedCameraDataSource {
    suspend fun query(request: FixedCameraQuery): List<FixedCamera>
}
