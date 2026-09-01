package com.openlauncher.app.feature.home

import com.openlauncher.app.feature.media.MediaState
import com.openlauncher.app.feature.navigation.NavigationState

data class HomeUiState(
    val navigation: NavigationState = NavigationState(),
    val media: MediaState = MediaState(),
)
