package com.openlauncher.app

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.openlauncher.app.data.settings.StartupScreen
import com.openlauncher.app.design.theme.CarColors
import com.openlauncher.app.design.theme.CarTheme
import com.openlauncher.app.distribution.DistributionFeatures
import com.openlauncher.app.feature.home.HomeRoute
import com.openlauncher.app.feature.media.AndroidMediaRepository
import com.openlauncher.app.feature.media.MediaRoute
import com.openlauncher.app.feature.navigation.NavigationError
import com.openlauncher.app.feature.navigation.NavigationState
import com.openlauncher.app.feature.navigation.external.DistributionNavigationHomeContent
import com.openlauncher.app.feature.notifications.AndroidNotificationRepository
import com.openlauncher.app.feature.notifications.NotificationRoute
import com.openlauncher.app.feature.settings.SettingsRoute
import com.openlauncher.app.feature.settings.rememberSettingsStateHolder
import com.openlauncher.app.launcher.AppsRoute
import com.openlauncher.app.shell.LauncherShell
import com.openlauncher.app.shell.PlaceholderDestination
import com.openlauncher.app.shell.ShellDestination
import com.openlauncher.app.shell.rememberShellState

@Composable
fun CarLauncherApp() {
    val context = LocalContext.current
    val isDebuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    val settingsStateHolder = rememberSettingsStateHolder()
    val settingsState = settingsStateHolder.uiState
    val mediaRepository = remember(context.applicationContext) {
        AndroidMediaRepository(context.applicationContext)
    }
    val mediaState by mediaRepository.state.collectAsState()
    val notificationRepository = remember(context.applicationContext) {
        AndroidNotificationRepository(context.applicationContext)
    }
    val notificationState by notificationRepository.state.collectAsState()

    DisposableEffect(mediaRepository, notificationRepository) {
        mediaRepository.start()
        notificationRepository.start()
        onDispose {
            notificationRepository.stop()
            mediaRepository.stop()
        }
    }

    CarTheme(textScale = settingsState.settings.textSizePreset.scale) {
        if (settingsState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CarColors.Background),
            )
        } else {
            val shellState = rememberShellState(
                initialDestination = when (settingsState.settings.startupScreen) {
                    StartupScreen.Home -> ShellDestination.Home
                    StartupScreen.Apps -> ShellDestination.Apps
                },
            )
            LauncherShell(
                showDebugOverlay = isDebuggable,
                notificationCount = notificationState.notifications.size,
                onAssistantClick = { launchAssistant(context) },
                state = shellState,
            ) { destination, navigateTo ->
                when (destination) {
                    ShellDestination.Home -> {
                        val navigationAppKey = settingsState.settings.navigationAppKey.takeIf {
                            DistributionFeatures.supportsExternalNavigation
                        }
                        val navigationApp = settingsState.apps.firstOrNull {
                            it.stableKey == navigationAppKey
                        }
                        HomeRoute(
                            navigationState = NavigationState(
                                error = if (navigationAppKey != null && navigationApp == null) {
                                    NavigationError.ProviderUnavailable
                                } else {
                                    null
                                },
                            ),
                            navigationContent = {
                                DistributionNavigationHomeContent(
                                    app = navigationApp,
                                    compatibilityMode =
                                        DistributionFeatures.supportsNavigationCompatibilityMode &&
                                            settingsState.settings.navigationCompatibilityMode,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            },
                            onNavigationClick = { navigateTo(ShellDestination.Settings) },
                            mediaState = mediaState,
                            onMediaClick = { navigateTo(ShellDestination.Media) },
                            onMediaSessionSelect = mediaRepository::selectSession,
                            onMediaPlayPause = mediaRepository.sessionController::playPause,
                            onMediaPrevious = mediaRepository.sessionController::previous,
                            onMediaNext = mediaRepository.sessionController::next,
                        )
                    }
                    ShellDestination.Apps -> AppsRoute(
                        onOpenSettings = { navigateTo(ShellDestination.Settings) },
                    )
                    ShellDestination.Media -> MediaRoute(
                        state = mediaState,
                        controller = mediaRepository.sessionController,
                        onSelectSession = mediaRepository::selectSession,
                        onClose = { navigateTo(ShellDestination.Home) },
                    )
                    ShellDestination.Notifications -> NotificationRoute(
                        state = notificationState,
                        repository = notificationRepository,
                    )
                    ShellDestination.Settings -> SettingsRoute(settingsStateHolder)
                    else -> PlaceholderDestination(destination)
                }
            }
        }
    }
}

private fun launchAssistant(context: Context) {
    val assistantIntent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(assistantIntent) }
}
