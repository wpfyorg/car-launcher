package org.wpfy.carlauncher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.wpfy.carlauncher.data.settings.AppTheme
import org.wpfy.carlauncher.data.settings.StartupScreen
import org.wpfy.carlauncher.design.screens.KitSearchState
import org.wpfy.carlauncher.design.theme.CarColors
import org.wpfy.carlauncher.design.theme.CarTheme
import org.wpfy.carlauncher.distribution.DistributionFeatures
import org.wpfy.carlauncher.design.kit.KitMapControl
import org.wpfy.carlauncher.feature.home.HomeRoute
import org.wpfy.carlauncher.feature.home.MapAccessory
import org.wpfy.carlauncher.feature.media.AndroidMediaRepository
import org.wpfy.carlauncher.feature.media.packageOfAppKey
import org.wpfy.carlauncher.feature.media.MediaRoute
import org.wpfy.carlauncher.feature.navigation.NavigationError
import org.wpfy.carlauncher.feature.navigation.NavigationState
import org.wpfy.carlauncher.feature.navigation.external.DistributionNavigationHomeContent
import org.wpfy.carlauncher.feature.navigation.external.DistributionNavigationVoiceLifecycle
import org.wpfy.carlauncher.feature.onboarding.OnboardingRoute
import org.wpfy.carlauncher.feature.notifications.AndroidNotificationRepository
import org.wpfy.carlauncher.feature.notifications.NotificationRoute
import org.wpfy.carlauncher.feature.settings.SettingsRoute
import org.wpfy.carlauncher.feature.settings.rememberSettingsStateHolder
import org.wpfy.carlauncher.feature.trip.TripSessionStore
import org.wpfy.carlauncher.feature.trip.TripRoute
import org.wpfy.carlauncher.feature.trip.rememberTripNavigationState
import org.wpfy.carlauncher.launcher.AppsRoute
import org.wpfy.carlauncher.shell.LauncherShell
import org.wpfy.carlauncher.shell.PlaceholderDestination
import org.wpfy.carlauncher.shell.ShellBackAction
import org.wpfy.carlauncher.shell.ShellDestination
import org.wpfy.carlauncher.shell.shellBackAction
import org.wpfy.carlauncher.shell.rememberShellState

/** [homeRequests] counts Home key presses (and a Home start that restored old state); each one returns to Home. */
@Composable
fun CarLauncherApp(homeRequests: Int = 0) {
    val context = LocalContext.current
    val settingsStateHolder = rememberSettingsStateHolder()
    val settingsState = settingsStateHolder.uiState
    val mediaRepository = remember(context.applicationContext) {
        AndroidMediaRepository(context.applicationContext, publishSelection = true)
    }
    val mediaState by mediaRepository.state.collectAsState()
    val tripState by TripSessionStore.state.collectAsState()
    val preferredMediaPackage = packageOfAppKey(settingsState.settings.preferredMediaAppKey)
    LaunchedEffect(mediaRepository, preferredMediaPackage) {
        mediaRepository.setPreferredPackage(preferredMediaPackage)
    }
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
        } else if (!settingsState.settings.onboardingCompleted) {
            OnboardingRoute(settingsStateHolder)
        } else {
            val shellState = rememberShellState(
                initialDestination = when (settingsState.settings.startupScreen) {
                    StartupScreen.Home -> ShellDestination.Home
                    StartupScreen.Apps -> ShellDestination.Apps
                },
            )
            var handledHomeRequests by remember { mutableIntStateOf(0) }
            LaunchedEffect(homeRequests) {
                if (homeRequests != handledHomeRequests) {
                    handledHomeRequests = homeRequests
                    shellState.navigateTo(ShellDestination.Home)
                }
            }
            // Registered before any screen, so inner handlers (media page, search overlay, embedded
            // navigation) win. This is the Home activity: Back never leaves it.
            BackHandler {
                if (shellBackAction(shellState.destination, navigationExpanded = false) == ShellBackAction.GoHome) {
                    shellState.navigateTo(ShellDestination.Home)
                }
            }
            val tripNavigationState = rememberTripNavigationState()
            DistributionNavigationVoiceLifecycle(shellState.destination)
            LauncherShell(
                notificationCount = notificationState.notifications.size,
                onAssistantClick = { launchAssistant(context) },
                railPosition = settingsState.settings.railPosition,
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
                        var navigationExpanded by remember(navigationApp?.stableKey) {
                            mutableStateOf(false)
                        }
                        BackHandler(
                            enabled = shellBackAction(ShellDestination.Home, navigationExpanded) ==
                                ShellBackAction.CollapseNavigation,
                        ) { navigationExpanded = false }
                        var navigationState by remember(navigationAppKey, navigationApp?.stableKey) {
                            mutableStateOf(
                                NavigationState(
                                    error = if (navigationAppKey != null && navigationApp == null) {
                                        NavigationError.ProviderUnavailable
                                    } else {
                                        null
                                    },
                                ),
                            )
                        }
                        var stopNavigationHandler by remember(navigationAppKey, navigationApp?.stableKey) {
                            mutableStateOf<(() -> Unit)?>(null)
                        }
                        var searchState by remember(navigationApp?.stableKey) { mutableStateOf<KitSearchState?>(null) }
                        var mapAccessory by remember { mutableStateOf<MapAccessory?>(null) }
                        var mapControls by remember { mutableStateOf<List<KitMapControl>>(emptyList()) }
                        HomeRoute(
                            navigationState = navigationState,
                            navigationContent = {
                                DistributionNavigationHomeContent(
                                    app = navigationApp,
                                    compatibilityMode =
                                        DistributionFeatures.supportsNavigationCompatibilityMode &&
                                            settingsState.settings.navigationCompatibilityMode,
                                    expanded = navigationExpanded,
                                    onExpandedChange = { navigationExpanded = it },
                                    onNavigationStateChange = { navigationState = it },
                                    onStopNavigationHandlerChange = { stopNavigationHandler = it },
                                    onMapAccessoryChange = { mapAccessory = it },
                                    onMapControlsChange = { mapControls = it },
                                    onSearchChange = { searchState = it },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            },
                            navigationContentOwnsControls = navigationApp != null,
                            // The Pro destination search is a panel over the normal Home (Pen "Search" frames), not the map expanded
                            // to the whole area: only an embedded navigation app expands.
                            navigationExpanded = navigationExpanded && !DistributionFeatures.supportsOfflineMaps,
                            searchOpen = navigationExpanded,
                            search = searchState,
                            navigationCardPosition = settingsState.settings.navigationCardPosition,
                            onNavigationClick = {
                                if (
                                    shouldOpenNavigationSettings(
                                        supportsOfflineMaps = DistributionFeatures.supportsOfflineMaps,
                                        navigationAppAvailable = navigationApp != null,
                                        navigationError = navigationState.error,
                                    )
                                ) {
                                    navigateTo(ShellDestination.Settings)
                                } else {
                                    navigationExpanded = true
                                }
                            },
                            onLocationPermissionClick = { launchAppSettings(context) },
                            onNavigationSettingsClick = { navigateTo(ShellDestination.Settings) },
                            onStopNavigation = { stopNavigationHandler?.invoke() },
                            mediaState = mediaState,
                            onMediaClick = { navigateTo(ShellDestination.Media) },
                            onMediaSessionSelect = mediaRepository::selectSession,
                            onMediaPlayPause = mediaRepository.sessionController::playPause,
                            onMediaPrevious = mediaRepository.sessionController::previous,
                            onMediaNext = mediaRepository.sessionController::next,
                            onMediaSeek = mediaRepository.sessionController::seekTo,
                            mapAccessory = mapAccessory.takeIf {
                                DistributionFeatures.supportsSpeedCameraAlerts
                            },
                            mapControls = mapControls,
                            overlaySize = settingsState.settings.overlaySize,
                            paidUi = settingsState.settings.appTheme == AppTheme.PaidUi &&
                                DistributionFeatures.supportsThemeSelection,
                            onAppsClick = { navigateTo(ShellDestination.Apps) },
                            speedUnit = settingsState.settings.speedUnit,
                            tripState = tripState,
                            onTripClick = { navigateTo(ShellDestination.Trip) },
                        )
                    }
                    ShellDestination.Trip -> TripRoute(
                        navigationState = tripNavigationState,
                        speedUnit = settingsState.settings.speedUnit,
                        onBackToHome = { navigateTo(ShellDestination.Home) },
                    )
                    ShellDestination.Apps -> AppsRoute(
                        onOpenSettings = { navigateTo(ShellDestination.Settings) },
                    )
                    ShellDestination.Media -> MediaRoute(
                        state = mediaState,
                        controller = mediaRepository.sessionController,
                        onSelectSession = mediaRepository::selectSession,
                    )
                    ShellDestination.Notifications -> NotificationRoute(
                        state = notificationState,
                        repository = notificationRepository,
                    )
                    ShellDestination.Settings -> SettingsRoute(
                        stateHolder = settingsStateHolder,
                        bluetoothLink = mediaState.bluetooth,
                        onBluetoothPermissionChanged = mediaRepository::refreshBluetoothPermission,
                    )
                    else -> PlaceholderDestination(destination)
                }
            }
        }
    }
}

internal fun shouldOpenNavigationSettings(
    supportsOfflineMaps: Boolean,
    navigationAppAvailable: Boolean,
    navigationError: NavigationError?,
): Boolean {
    return !supportsOfflineMaps &&
        (!navigationAppAvailable || navigationError == NavigationError.ProviderUnavailable)
}

private fun launchAssistant(context: Context) {
    val assistantIntent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(assistantIntent) }
}

private fun launchAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}
