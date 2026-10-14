package org.wpfy.carlauncher.feature.trip

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable

enum class TripNavigationPage { Detail, History }

/** Saveable navigation only. Trip content, storage, and commands remain owned by their callers. */
@Stable
class TripNavigationState private constructor(
    initialPage: TripNavigationPage,
    initialSelectedTripId: String?,
    initialDetailFromHistory: Boolean,
    initialHistoryFilter: TripHistoryFilter,
    initialHistoryIndex: Int,
    initialHistoryOffset: Int,
    initialRequestedResetId: String?,
) {
    var page by mutableStateOf(initialPage)
        private set
    var selectedTripId by mutableStateOf(initialSelectedTripId)
        private set
    var detailFromHistory by mutableStateOf(initialDetailFromHistory)
        private set
    var historyFilter by mutableStateOf(initialHistoryFilter)
        private set
    var historyIndex by mutableStateOf(initialHistoryIndex.coerceAtLeast(0))
        private set
    var historyOffset by mutableStateOf(initialHistoryOffset.coerceAtLeast(0))
        private set
    var requestedResetId by mutableStateOf(initialRequestedResetId)
        private set

    fun openHistory() {
        requestedResetId = null
        page = TripNavigationPage.History
        selectedTripId = null
        detailFromHistory = false
    }

    fun openTrip(id: String) {
        require(id.isNotBlank()) { "Trip ID cannot be blank" }
        requestedResetId = null
        detailFromHistory = page == TripNavigationPage.History
        selectedTripId = id
        page = TripNavigationPage.Detail
    }

    fun changeFilter(filter: TripHistoryFilter) {
        historyFilter = filter
        historyIndex = 0
        historyOffset = 0
    }

    fun updateScroll(index: Int, offset: Int) {
        historyIndex = index.coerceAtLeast(0)
        historyOffset = offset.coerceAtLeast(0)
    }

    /** Stores an owner Reset ID for later status observation; it never dispatches a command. */
    fun observeReset(id: String?) {
        requestedResetId = id
    }

    /** Follows this screen's replacement only while the originally reset detail remains selected. */
    fun applyResetReplacement(expectedId: String, replacementId: String) {
        if (replacementId.isNotBlank() && requestedResetId == expectedId &&
            page == TripNavigationPage.Detail && selectedTripId == expectedId) {
            selectedTripId = replacementId
        }
    }

    /** Returns false only when the caller should continue Back handling to Home. */
    fun back(): Boolean {
        requestedResetId = null
        return when {
            page == TripNavigationPage.Detail && detailFromHistory -> {
                page = TripNavigationPage.History
                selectedTripId = null
                detailFromHistory = false
                true
            }
            page == TripNavigationPage.History -> {
                page = TripNavigationPage.Detail
                selectedTripId = null
                detailFromHistory = false
                true
            }
            else -> false
        }
    }

    private fun saveableValues(): ArrayList<Any?> = arrayListOf(
        page.name,
        selectedTripId,
        detailFromHistory,
        historyFilter.name,
        historyIndex,
        historyOffset,
        requestedResetId,
    )

    companion object {
        private fun create(
            page: TripNavigationPage = TripNavigationPage.Detail,
            selectedTripId: String? = null,
            detailFromHistory: Boolean = false,
            historyFilter: TripHistoryFilter = TripHistoryFilter.Month,
            historyIndex: Int = 0,
            historyOffset: Int = 0,
            requestedResetId: String? = null,
        ) = TripNavigationState(
            page,
            selectedTripId?.takeIf { it.isNotBlank() }?.takeIf { page == TripNavigationPage.Detail },
            detailFromHistory && page == TripNavigationPage.Detail && !selectedTripId.isNullOrBlank(),
            historyFilter, historyIndex, historyOffset, requestedResetId,
        )

        private fun restore(values: List<*>): TripNavigationState {
            fun enumName(index: Int) = values.getOrNull(index) as? String
            val page = enumName(0)?.let { name ->
                runCatching { TripNavigationPage.valueOf(name) }.getOrNull()
            } ?: TripNavigationPage.Detail
            val filter = enumName(3)?.let { name ->
                runCatching { TripHistoryFilter.valueOf(name) }.getOrNull()
            } ?: TripHistoryFilter.Month
            return create(
                page = page,
                selectedTripId = values.getOrNull(1) as? String,
                detailFromHistory = values.getOrNull(2) as? Boolean ?: false,
                historyFilter = filter,
                historyIndex = values.getOrNull(4) as? Int ?: 0,
                historyOffset = values.getOrNull(5) as? Int ?: 0,
                requestedResetId = values.getOrNull(6) as? String,
            )
        }

        val saver: Saver<TripNavigationState, Any> = Saver(
            save = { it.saveableValues() },
            restore = { saved -> (saved as? List<*>)?.let(::restore) ?: create() },
        )

        internal fun createDefault(): TripNavigationState = create()
    }
}

@Composable
fun rememberTripNavigationState(): TripNavigationState = rememberSaveable(
    saver = TripNavigationState.saver,
) {
    TripNavigationState.createDefault()
}
