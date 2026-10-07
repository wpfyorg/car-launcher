package org.wpfy.carlauncher.feature.navigation.search

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

interface PoiSearchRepository : FixedCameraDataSource {
    suspend fun search(request: PoiSearchRequest): List<PoiSearchResult>

    /** Debounces typing and cancels the previous in-flight search when a new query arrives. */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    fun searchFlow(requests: Flow<PoiSearchRequest>): Flow<List<PoiSearchResult>> {
        return requests
            .debounce(QueryDebounceMillis)
            .mapLatest(::search)
    }

    suspend fun recordRecent(result: PoiSearchResult) = Unit

    suspend fun recentSearches(): List<PoiSearchResult> = emptyList()

    suspend fun setFavorite(result: PoiSearchResult, favorite: Boolean) = Unit

    suspend fun favorites(): List<PoiSearchResult> = emptyList()

    suspend fun setSavedPlace(slot: PoiSavedPlace, result: PoiSearchResult?) = Unit

    suspend fun savedPlace(slot: PoiSavedPlace): PoiSearchResult? = null

    /** Closes cached region handles. Implementations may be closed more than once. */
    fun close() = Unit

    companion object {
        const val QueryDebounceMillis = 150L
        const val DefaultResultLimit = 25
        const val DefaultNearbyRegionLimit = 3
    }
}
