package com.openlauncher.app.feature.navigation.offline

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface OfflineMapRepository {
    val regions: StateFlow<List<OfflineMapRegion>>

    suspend fun refresh()

    suspend fun download(regionId: String)

    suspend fun cancel(regionId: String)

    suspend fun delete(regionId: String)
}

/**
 * Production placeholder until the native navigation flavor supplies a map catalog/downloader.
 * Keeping it behind the repository contract lets Settings render the manager shell without
 * pretending downloads are functional in the current build.
 */
object UnavailableOfflineMapRepository : OfflineMapRepository {
    private val mutableRegions = MutableStateFlow<List<OfflineMapRegion>>(emptyList())

    override val regions: StateFlow<List<OfflineMapRegion>> = mutableRegions.asStateFlow()

    override suspend fun refresh() = Unit

    override suspend fun download(regionId: String) = Unit

    override suspend fun cancel(regionId: String) = Unit

    override suspend fun delete(regionId: String) = Unit
}
