package com.openlauncher.app.feature.navigation.offline

data class OfflineMapRegion(
    val id: String,
    val name: String,
    val detail: String? = null,
    val estimatedSizeBytes: Long? = null,
    val downloadedBytes: Long = 0L,
    val progress: Float = 0f,
    val state: OfflineMapRegionState = OfflineMapRegionState.NotDownloaded,
    val errorMessage: String? = null,
)

enum class OfflineMapRegionState {
    NotDownloaded,
    Queued,
    Downloading,
    Downloaded,
    Failed,
}
