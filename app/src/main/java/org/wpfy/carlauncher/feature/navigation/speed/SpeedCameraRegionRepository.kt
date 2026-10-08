package org.wpfy.carlauncher.feature.navigation.speed

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A country (ISO 3166-1 alpha-2 code) the user can download speed camera data for. */
data class SpeedCameraRegion(
    val id: String,
    val name: String,
    val state: SpeedCameraRegionState = SpeedCameraRegionState.NotDownloaded,
    val rows: Int = 0,
    val bytes: Long? = null,
    val version: String? = null,
    /** Why the download was refused for good, when it was; [SpeedCameraFailureIntegrity] means no genuine Play install. */
    val failureReason: String? = null,
)

const val SpeedCameraFailureIntegrity = "integrity"

enum class SpeedCameraRegionState {
    NotDownloaded,
    Queued,
    Downloading,
    Downloaded,
    Failed,
}

/** The regions the Pro build can download camera data for; the Free build has none. */
interface SpeedCameraRegionRepository {
    val regions: StateFlow<List<SpeedCameraRegion>>

    suspend fun refresh()

    suspend fun download(regionId: String)

    suspend fun delete(regionId: String)
}

/** The Free build: no camera data, so Settings has nothing to list. */
object UnavailableSpeedCameraRegionRepository : SpeedCameraRegionRepository {
    private val mutableRegions = MutableStateFlow<List<SpeedCameraRegion>>(emptyList())

    override val regions: StateFlow<List<SpeedCameraRegion>> = mutableRegions.asStateFlow()

    override suspend fun refresh() = Unit

    override suspend fun download(regionId: String) = Unit

    override suspend fun delete(regionId: String) = Unit
}
