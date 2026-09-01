package com.openlauncher.app.feature.home

import com.openlauncher.app.feature.media.MediaState

sealed interface HomeNavigationState {
    data object Parked : HomeNavigationState

    data class Active(
        val distanceToTurn: String,
        val street: String,
        val nextTurnLabel: String,
        val duration: String,
        val distanceRemaining: String,
        val arrivalTime: String,
    ) : HomeNavigationState

    data object GpsUnavailable : HomeNavigationState
    data object NavigationUnavailable : HomeNavigationState
    data object Arrived : HomeNavigationState
}

data class HomeUiState(
    val navigation: HomeNavigationState = HomeNavigationState.Parked,
    val media: MediaState = MediaState(),
)
