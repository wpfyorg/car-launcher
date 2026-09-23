package org.wpfy.carlauncher.design.preview

import android.content.ComponentName
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.wpfy.carlauncher.design.theme.CarTheme
import org.wpfy.carlauncher.feature.home.HomeContent
import org.wpfy.carlauncher.feature.home.HomeUiState
import org.wpfy.carlauncher.feature.media.MediaAccessStatus
import org.wpfy.carlauncher.feature.media.MediaControls
import org.wpfy.carlauncher.feature.media.Media
import org.wpfy.carlauncher.feature.media.MediaSource
import org.wpfy.carlauncher.feature.media.MediaSourceSession
import org.wpfy.carlauncher.feature.media.MediaState
import org.wpfy.carlauncher.feature.navigation.Maneuver
import org.wpfy.carlauncher.feature.navigation.ManeuverType
import org.wpfy.carlauncher.feature.navigation.NavigationProgress
import org.wpfy.carlauncher.feature.navigation.NavigationState
import org.wpfy.carlauncher.launcher.AppDrawer
import org.wpfy.carlauncher.launcher.AppsUiState
import org.wpfy.carlauncher.launcher.LauncherApp
import org.wpfy.carlauncher.shell.LauncherShell

// Reference panels as usable dp (pixels / (dpi / 160)). Layout follows dp bounds, never raw pixels.
@Preview(name = "800×480 compact", widthDp = 800, heightDp = 480)
@Preview(name = "1024×600", widthDp = 1024, heightDp = 600)
@Preview(name = "1280×720", widthDp = 1280, heightDp = 720)
@Preview(name = "1280×800", widthDp = 1280, heightDp = 800)
@Preview(name = "1920×720 @160", widthDp = 1920, heightDp = 720)
@Preview(name = "1280×480 ultrawide (1920×720 @240)", widthDp = 1280, heightDp = 480)
@Preview(name = "1920×1080", widthDp = 1920, heightDp = 1080)
@Preview(name = "1000×600 (2000×1200 @320)", widthDp = 1000, heightDp = 600)
@Preview(name = "360×640 portrait (720×1280 @320)", widthDp = 360, heightDp = 640)
@Preview(name = "540×960 portrait (1080×1920 @320)", widthDp = 540, heightDp = 960)
@Preview(name = "600×1000 tall (1200×2000 @320)", widthDp = 600, heightDp = 1000)
@Preview(name = "1080×1920 portrait @160", widthDp = 1080, heightDp = 1920)
@Preview(name = "Compact 748×450", widthDp = 748, heightDp = 450)
@Preview(name = "Extreme small 533×320", widthDp = 533, heightDp = 320)
@Composable
private fun LauncherShellPreview() {
    CarTheme {
        LauncherShell()
    }
}

@Preview(name = "800×480 compact", widthDp = 800, heightDp = 480)
@Preview(name = "1024×600", widthDp = 1024, heightDp = 600)
@Preview(name = "1280×720", widthDp = 1280, heightDp = 720)
@Preview(name = "1280×800", widthDp = 1280, heightDp = 800)
@Preview(name = "1920×720 @160", widthDp = 1920, heightDp = 720)
@Preview(name = "1280×480 ultrawide (1920×720 @240)", widthDp = 1280, heightDp = 480)
@Preview(name = "1920×1080", widthDp = 1920, heightDp = 1080)
@Preview(name = "1000×600 (2000×1200 @320)", widthDp = 1000, heightDp = 600)
@Preview(name = "360×640 portrait (720×1280 @320)", widthDp = 360, heightDp = 640)
@Preview(name = "540×960 portrait (1080×1920 @320)", widthDp = 540, heightDp = 960)
@Preview(name = "600×1000 tall (1200×2000 @320)", widthDp = 600, heightDp = 1000)
@Preview(name = "1080×1920 portrait @160", widthDp = 1080, heightDp = 1920)
@Preview(name = "Compact 748×450", widthDp = 748, heightDp = 450)
@Preview(name = "Extreme small 533×320", widthDp = 533, heightDp = 320)
@Composable
private fun HomeParkedPreview() {
    CarTheme {
        LauncherShell { _, _ -> HomeContent(state = HomeUiState()) }
    }
}

@Preview(name = "Home active navigation 1280×720", widthDp = 1280, heightDp = 720)
@Preview(name = "Home active navigation 360×640", widthDp = 360, heightDp = 640)
@Preview(name = "Home active navigation 800×480", widthDp = 800, heightDp = 480)
@Composable
private fun HomeActiveNavigationPreview() {
    CarTheme {
        LauncherShell { _, _ ->
            HomeContent(
                state = HomeUiState(
                    navigation = NavigationState(
                        active = true,
                        progress = NavigationProgress(
                            nextManeuver = Maneuver(
                                type = ManeuverType.TurnRight,
                                instruction = "Turn right",
                                roadName = "101 W Pacific Coast Hwy",
                                distanceMeters = 106.68,
                            ),
                            secondaryManeuver = Maneuver(
                                type = ManeuverType.TurnRight,
                                instruction = "Turn right",
                            ),
                            remainingDistanceMeters = 52_303.7,
                            remainingDurationSeconds = 3_000L,
                            etaEpochMillis = 1_788_226_760_000L,
                        ),
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

@Preview(name = "Media 800×480", widthDp = 800, heightDp = 480)
@Preview(name = "Media 1280×720", widthDp = 1280, heightDp = 720)
@Preview(name = "Media 1280×480 ultrawide", widthDp = 1280, heightDp = 480)
@Preview(name = "Media 360×640 portrait", widthDp = 360, heightDp = 640)
@Preview(name = "Media 600×1000 tall", widthDp = 600, heightDp = 1000)
@Composable
private fun MediaPreview() {
    val session = MediaSourceSession(
        id = "preview",
        title = "A Long Track Title That Should Wrap Cleanly Instead Of Clipping",
        artist = "Michael Evans",
        source = MediaSource(packageName = "preview", label = "Music"),
        durationMs = 225_000L,
        positionMs = 72_000L,
        isPlaying = true,
        isShuffleEnabled = false,
        isRepeatEnabled = false,
        isFavorite = false,
        controls = MediaControls(true, true, true, true, true, true, true),
    )
    CarTheme {
        LauncherShell { _, _ ->
            Media(
                state = MediaState(
                    accessStatus = MediaAccessStatus.Available,
                    hasSession = true,
                    availableSessions = listOf(session),
                    selectedSessionId = session.id,
                ),
                onSelectSession = {},
                onRequestAccess = {},
                onOpenSource = {},
                onPlayPause = {},
                onPrevious = {},
                onNext = {},
            )
        }
    }
}

@Preview(name = "Apps 800×480", widthDp = 800, heightDp = 480)
@Preview(name = "Apps 1280×720", widthDp = 1280, heightDp = 720)
@Preview(name = "Apps 1280×480 ultrawide", widthDp = 1280, heightDp = 480)
@Preview(name = "Apps 360×640 portrait", widthDp = 360, heightDp = 640)
@Preview(name = "Apps 600×1000 tall", widthDp = 600, heightDp = 1000)
@Composable
private fun AppsPreview() {
    val apps = listOf(
        "Calculator", "Calendar", "Camera", "Chrome", "Clock", "Contacts", "Files", "Gmail",
        "Maps", "Messages", "Music", "Phone", "Photos", "A Rather Long App Label",
    ).map { label -> LauncherApp(ComponentName("preview.${label.take(4)}", ".Main"), label) }
    CarTheme {
        LauncherShell { _, _ ->
            AppDrawer(
                state = AppsUiState(apps = apps, isLoading = false),
                onQueryChange = {},
                onAppClick = {},
                onTogglePinned = {},
                onOpenAppInfo = {},
                onOpenSettings = {},
                iconLoader = { null },
            )
        }
    }
}
