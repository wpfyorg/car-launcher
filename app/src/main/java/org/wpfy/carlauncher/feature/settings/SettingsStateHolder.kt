package org.wpfy.carlauncher.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import org.wpfy.carlauncher.core.platform.defaultRailPositionForDisplay
import org.wpfy.carlauncher.data.persistence.PinnedAppsRepository
import org.wpfy.carlauncher.data.settings.LauncherSettings
import org.wpfy.carlauncher.data.settings.AppTheme
import org.wpfy.carlauncher.data.settings.NavigationCardPosition
import org.wpfy.carlauncher.data.settings.OverlaySize
import org.wpfy.carlauncher.data.settings.RailPosition
import org.wpfy.carlauncher.data.settings.SettingsRepository
import org.wpfy.carlauncher.data.settings.SpeedUnitSetting
import org.wpfy.carlauncher.data.settings.CameraAlertDistance
import org.wpfy.carlauncher.data.settings.OverspeedWarning
import org.wpfy.carlauncher.data.settings.StartupScreen
import org.wpfy.carlauncher.data.settings.TextSizePreset
import org.wpfy.carlauncher.data.settings.VoiceVolume
import org.wpfy.carlauncher.feature.navigation.offline.OfflineMapRegion
import org.wpfy.carlauncher.feature.navigation.offline.OfflineMapRepository
import org.wpfy.carlauncher.distribution.DistributionOfflineMaps
import org.wpfy.carlauncher.distribution.DistributionSpeedCameras
import org.wpfy.carlauncher.feature.navigation.speed.SpeedCameraRegion
import org.wpfy.carlauncher.feature.navigation.speed.SpeedCameraRegionRepository
import org.wpfy.carlauncher.feature.navigation.speed.UnavailableSpeedCameraRegionRepository
import org.wpfy.carlauncher.launcher.AppCatalog
import org.wpfy.carlauncher.launcher.LauncherApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val settings: LauncherSettings = LauncherSettings(),
    val pinnedAppCount: Int = 0,
    val apps: List<LauncherApp> = emptyList(),
    val navigationApps: List<LauncherApp> = emptyList(),
    val appsLoaded: Boolean = false,
    val offlineMapRegions: List<OfflineMapRegion> = emptyList(),
    val offlineMapImportError: String? = null,
    val speedCameraRegions: List<SpeedCameraRegion> = emptyList(),
)

@Stable
class SettingsStateHolder(
    private val settingsRepository: SettingsRepository,
    private val pinnedAppsRepository: PinnedAppsRepository,
    private val appCatalog: AppCatalog,
    private val offlineMapRepository: OfflineMapRepository,
    private val defaultRailPosition: RailPosition,
    private val scope: CoroutineScope,
    private val speedCameraRepository: SpeedCameraRegionRepository = UnavailableSpeedCameraRegionRepository,
) {
    var uiState by mutableStateOf(SettingsUiState())
        private set

    init {
        scope.launch {
            runCatching {
                settingsRepository.retryPendingUpgradeMigration(defaultRailPosition)
                settingsRepository.markOnboardingInitialized()
            }
        }
        scope.launch {
            settingsRepository.settings.collectLatest { settings ->
                uiState = uiState.copy(settings = settings, isLoading = false)
            }
        }
        scope.launch {
            pinnedAppsRepository.pinnedAppKeys.collectLatest { keys ->
                uiState = uiState.copy(pinnedAppCount = keys.size)
            }
        }
        scope.launch {
            refreshApps()
            appCatalog.packageChanges().collectLatest { refreshApps() }
        }
        scope.launch {
            offlineMapRepository.regions.collectLatest { regions ->
                uiState = uiState.copy(offlineMapRegions = regions)
            }
        }
        scope.launch { offlineMapRepository.refresh() }
        scope.launch {
            speedCameraRepository.regions.collectLatest { regions ->
                uiState = uiState.copy(speedCameraRegions = regions)
            }
        }
        scope.launch { speedCameraRepository.refresh() }
    }

    fun setStartOnBoot(enabled: Boolean) = update { settingsRepository.setStartOnBoot(enabled) }

    fun setStartupScreen(screen: StartupScreen) = update { settingsRepository.setStartupScreen(screen) }

    fun setTextSizePreset(preset: TextSizePreset) = update { settingsRepository.setTextSizePreset(preset) }

    fun setPreferredMediaApp(app: LauncherApp?) = update {
        settingsRepository.setPreferredMediaApp(app?.stableKey)
    }

    fun setNavigationApp(app: LauncherApp?) = update {
        settingsRepository.setNavigationApp(app?.stableKey)
    }

    fun setNavigationCompatibilityMode(enabled: Boolean) = update {
        settingsRepository.setNavigationCompatibilityMode(enabled)
    }

    fun setNavigationCardPosition(position: NavigationCardPosition) = update {
        settingsRepository.setNavigationCardPosition(position)
    }

    fun setRailPosition(position: RailPosition) = update {
        settingsRepository.setRailPosition(position)
    }

    fun setOverlaySize(size: OverlaySize) = update { settingsRepository.setOverlaySize(size) }

    fun setMediaCardCollapsed(collapsed: Boolean) = update {
        settingsRepository.setMediaCardCollapsed(collapsed)
    }

    fun setAppTheme(theme: AppTheme) = update {
        settingsRepository.setAppTheme(theme)
    }

    fun setVoiceGuidanceMuted(muted: Boolean) = update { settingsRepository.setVoiceGuidanceMuted(muted) }

    fun setVoiceVolume(volume: VoiceVolume) = update { settingsRepository.setVoiceVolume(volume) }

    fun setVoiceName(name: String?) = update { settingsRepository.setVoiceName(name) }

    fun setSpeedUnit(unit: SpeedUnitSetting) = update { settingsRepository.setSpeedUnit(unit) }

    fun setCameraAlertDistance(distance: CameraAlertDistance) = update { settingsRepository.setCameraAlertDistance(distance) }

    fun setOverspeedWarning(warning: OverspeedWarning) = update { settingsRepository.setOverspeedWarning(warning) }

    fun completeOnboarding(position: RailPosition) = update {
        settingsRepository.completeOnboarding(position)
    }

    fun downloadOfflineMap(regionId: String) = update {
        offlineMapRepository.download(regionId)
    }

    fun cancelOfflineMapDownload(regionId: String) = update {
        offlineMapRepository.cancel(regionId)
    }

    fun deleteOfflineMap(regionId: String) = update {
        offlineMapRepository.delete(regionId)
    }

    fun downloadSpeedCameraRegion(regionId: String) = update {
        speedCameraRepository.download(regionId)
    }

    /** Fetches the camera region list again (the one at start may have run offline). */
    fun refreshSpeedCameraRegions() {
        scope.launch { speedCameraRepository.refresh() }
    }

    fun deleteSpeedCameraRegion(regionId: String) = update {
        speedCameraRepository.delete(regionId)
    }

    fun importOfflineMap(uri: Uri) {
        scope.launch {
            uiState = uiState.copy(offlineMapImportError = null)
            val error = runOfflineMapImport { offlineMapRepository.importFile(uri) }
            if (error != null) {
                uiState = uiState.copy(offlineMapImportError = error)
            }
        }
    }

    /** The launcher icon of an app listed in a picker. */
    suspend fun loadIcon(app: LauncherApp): ImageBitmap? = appCatalog.loadIcon(app)

    private fun update(block: suspend () -> Unit) {
        scope.launch { block() }
    }

    private suspend fun refreshApps() {
        val apps = appCatalog.loadApps()
        val navigationApps = appCatalog.loadNavigationApps()
        uiState = uiState.copy(
            apps = apps,
            navigationApps = navigationApps,
            appsLoaded = true,
        )
    }
}

internal suspend fun runOfflineMapImport(block: suspend () -> Unit): String? {
    return try {
        block()
        null
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Throwable) {
        error.message
            ?.takeIf { it.isNotBlank() }
            ?.take(160)
            ?: "Offline map import failed"
    }
}

@Composable
fun rememberSettingsStateHolder(): SettingsStateHolder {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    // Density changes are handled in place (no activity restart), so the display-derived default is
    // read once per holder: re-keying on it would start a second holder in the same scope.
    return remember(context, scope) {
        val configuration = context.resources.configuration
        val defaultRailPosition = defaultRailPositionForDisplay(
            screenWidthDp = configuration.screenWidthDp,
            screenHeightDp = configuration.screenHeightDp,
        )
        SettingsStateHolder(
            settingsRepository = SettingsRepository(context),
            pinnedAppsRepository = PinnedAppsRepository(context),
            appCatalog = AppCatalog(context),
            offlineMapRepository = DistributionOfflineMaps.repository(context),
            defaultRailPosition = defaultRailPosition,
            scope = scope,
            speedCameraRepository = DistributionSpeedCameras.repository(context),
        )
    }
}
