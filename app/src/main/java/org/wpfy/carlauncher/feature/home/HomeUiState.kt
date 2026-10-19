package org.wpfy.carlauncher.feature.home

import org.wpfy.carlauncher.feature.media.MediaState
import org.wpfy.carlauncher.feature.navigation.NavigationState
import org.wpfy.carlauncher.feature.trip.TripState

data class HomeUiState(
    val navigation: NavigationState = NavigationState(),
    val media: MediaState = MediaState(),
    val tripState: TripState = TripState.Idle,
)
