package com.openlauncher.app.design.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.openlauncher.app.design.theme.CarTheme
import com.openlauncher.app.feature.home.HomeNavigationState
import com.openlauncher.app.feature.home.HomeScreen
import com.openlauncher.app.feature.home.HomeUiState
import com.openlauncher.app.feature.media.MediaControls
import com.openlauncher.app.feature.media.MediaState
import com.openlauncher.app.shell.LauncherShell

@Preview(name = "YT5760D 1600×720", widthDp = 1600, heightDp = 720)
@Preview(name = "BMW iX 1920×584", widthDp = 1920, heightDp = 584)
@Preview(name = "Widescreen 1280×480", widthDp = 1280, heightDp = 480)
@Preview(name = "Minimum widescreen 1120×480", widthDp = 1120, heightDp = 480)
@Preview(name = "Vertical rail 880×480", widthDp = 880, heightDp = 480)
@Preview(name = "Compact 800×480", widthDp = 800, heightDp = 480)
@Preview(name = "Compact 748×450", widthDp = 748, heightDp = 450)
@Preview(name = "Portrait 800×880", widthDp = 800, heightDp = 880)
@Preview(name = "Short portrait 790×686", widthDp = 790, heightDp = 686)
@Composable
private fun LauncherShellPreview() {
    CarTheme {
        LauncherShell()
    }
}

@Preview(name = "Home parked 1600×720", widthDp = 1600, heightDp = 720)
@Preview(name = "Home parked 1920×584", widthDp = 1920, heightDp = 584)
@Preview(name = "Home parked 1280×480", widthDp = 1280, heightDp = 480)
@Preview(name = "Home parked 1120×480", widthDp = 1120, heightDp = 480)
@Preview(name = "Home parked 880×480", widthDp = 880, heightDp = 480)
@Preview(name = "Home parked 800×480", widthDp = 800, heightDp = 480)
@Preview(name = "Home parked 748×450", widthDp = 748, heightDp = 450)
@Preview(name = "Home parked 800×880", widthDp = 800, heightDp = 880)
@Preview(name = "Home parked 790×686", widthDp = 790, heightDp = 686)
@Composable
private fun HomeParkedPreview() {
    CarTheme {
        LauncherShell { _, _ -> HomeScreen(state = HomeUiState()) }
    }
}

@Preview(name = "Home active navigation 1600×720", widthDp = 1600, heightDp = 720)
@Composable
private fun HomeActiveNavigationPreview() {
    CarTheme {
        LauncherShell { _, _ ->
            HomeScreen(
                state = HomeUiState(
                    navigation = HomeNavigationState.Active(
                        distanceToTurn = "350 ft",
                        street = "101 W Pacific Coast Hwy",
                        nextTurnLabel = "Turn right",
                        duration = "50 min",
                        distanceRemaining = "32.5 mi",
                        arrivalTime = "12:26 pm",
                    ),
                    media = MediaState(
                        hasSession = true,
                        title = "You got to listen",
                        artist = "Michael Evans",
                        positionMs = 72_000L,
                        durationMs = 225_000L,
                        isPlaying = true,
                        controls = MediaControls(
                            canPlayPause = true,
                            canSkipPrevious = true,
                            canSkipNext = true,
                        ),
                    ),
                ),
            )
        }
    }
}
