package org.wpfy.carlauncher.feature.navigation.offline

data class OfflineRegionManifest(
    val formatVersion: Int,
    val regionId: String,
    val name: String,
    val country: String,
    val bounds: OfflineRegionBounds,
    val kind: OfflineRegionKind,
    val buildTimestamp: String,
    val dataVersion: String,
    val minimumAppVersionCode: Long,
    val compressedSize: Long,
    val installedSize: Long,
    val files: List<OfflineRegionFile>,
    val safetyAlertsAllowed: Boolean,
)

data class OfflineRegionBounds(
    val minLat: Double,
    val minLon: Double,
    val maxLat: Double,
    val maxLon: Double,
) {
    fun contains(latitude: Double, longitude: Double, marginDegrees: Double = 0.0): Boolean {
        return latitude >= minLat - marginDegrees &&
            latitude <= maxLat + marginDegrees &&
            longitude >= minLon - marginDegrees &&
            longitude <= maxLon + marginDegrees
    }
}

enum class OfflineRegionKind(val wireValue: String) {
    Region("region"),
    Base("base");

    companion object {
        fun fromWireValue(value: String): OfflineRegionKind? = entries.firstOrNull { it.wireValue == value }
    }
}

data class OfflineRegionFile(
    val path: String,
    val size: Long,
    val sha256: String,
)
