package org.wpfy.carlauncher.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal val Context.settingsPreferences by preferencesDataStore(name = "settings_preferences")

class SettingsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dataStore = appContext.settingsPreferences
    private val migrationPreferences = appContext.getSharedPreferences(
        MigrationPreferencesName,
        Context.MODE_PRIVATE,
    )

    val settings: Flow<LauncherSettings> = dataStore.data.map(::settingsFromPreferences)

    suspend fun current(): LauncherSettings = settings.first()

    suspend fun setStartOnBoot(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[StartOnBootKey] = enabled }
    }

    suspend fun setStartupScreen(screen: StartupScreen) {
        dataStore.edit { preferences -> preferences[StartupScreenKey] = screen.name }
    }

    suspend fun setTextSizePreset(preset: TextSizePreset) {
        dataStore.edit { preferences -> preferences[TextSizeKey] = preset.name }
    }

    suspend fun setPreferredMediaApp(appKey: String?) {
        dataStore.edit { preferences ->
            if (appKey == null) preferences.remove(PreferredMediaAppKey)
            else preferences[PreferredMediaAppKey] = appKey
        }
    }

    suspend fun setNavigationApp(appKey: String?) {
        dataStore.edit { preferences ->
            if (appKey == null) preferences.remove(NavigationAppKey)
            else preferences[NavigationAppKey] = appKey
        }
    }

    suspend fun setNavigationCompatibilityMode(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[NavigationCompatibilityModeKey] = enabled }
    }

    suspend fun setNavigationCardPosition(position: NavigationCardPosition) {
        dataStore.edit { preferences -> preferences[NavigationCardPositionKey] = position.name }
    }

    suspend fun setRailPosition(position: RailPosition) {
        dataStore.edit { preferences -> preferences[RailPositionKey] = position.name }
    }

    suspend fun setOverlaySize(size: OverlaySize) {
        dataStore.edit { preferences -> preferences[OverlaySizeKey] = size.name }
    }

    suspend fun setMediaCardCollapsed(collapsed: Boolean) {
        dataStore.edit { preferences -> preferences[MediaCardCollapsedKey] = collapsed }
    }

    suspend fun setAppTheme(theme: AppTheme) {
        dataStore.edit { preferences -> preferences[AppThemeKey] = theme.name }
    }

    suspend fun setVoiceGuidanceMuted(muted: Boolean) {
        dataStore.edit { preferences -> preferences[VoiceGuidanceMutedKey] = muted }
    }

    suspend fun setVoiceVolume(volume: VoiceVolume) {
        dataStore.edit { preferences -> preferences[VoiceVolumeKey] = volume.name }
    }

    suspend fun setVoiceName(name: String?) {
        dataStore.edit { preferences ->
            if (name == null) preferences.remove(VoiceNameKey)
            else preferences[VoiceNameKey] = name
        }
    }

    suspend fun setSpeedUnit(unit: SpeedUnitSetting) {
        dataStore.edit { preferences -> preferences[SpeedUnitKey] = unit.name }
    }

    suspend fun setCameraAlertDistance(distance: CameraAlertDistance) {
        dataStore.edit { preferences -> preferences[CameraAlertDistanceKey] = distance.name }
    }

    suspend fun setOverspeedWarning(warning: OverspeedWarning) {
        dataStore.edit { preferences -> preferences[OverspeedWarningKey] = warning.name }
    }

    suspend fun completeOnboarding(position: RailPosition) {
        dataStore.edit { preferences ->
            preferences[RailPositionKey] = position.name
            preferences[OnboardingCompletedKey] = true
            preferences[OnboardingInitializedKey] = true
        }
    }

    suspend fun markOnboardingInitialized() {
        dataStore.edit { preferences ->
            if (preferences[OnboardingInitializedKey] == null) {
                preferences[OnboardingInitializedKey] = true
            }
        }
    }

    suspend fun migrateExistingInstallOnUpgrade(defaultRailPosition: RailPosition) {
        runUpgradeMigrationWithPendingMarker(
            markPending = { setUpgradeMigrationPending(true) },
            migrate = {
                dataStore.edit { preferences ->
                    if (
                        shouldMigrateLegacyOnboardingState(
                            onboardingCompleted = preferences[OnboardingCompletedKey],
                            onboardingInitialized = preferences[OnboardingInitializedKey],
                        )
                    ) {
                        if (preferences[RailPositionKey] == null) {
                            preferences[RailPositionKey] = defaultRailPosition.name
                        }
                        preferences[OnboardingCompletedKey] = true
                    }
                    preferences[OnboardingInitializedKey] = true
                }
            },
            clearPending = { setUpgradeMigrationPending(false) },
        )
    }

    suspend fun retryPendingUpgradeMigration(defaultRailPosition: RailPosition) {
        if (upgradeMigrationPending()) {
            migrateExistingInstallOnUpgrade(defaultRailPosition)
        }
    }

    private suspend fun upgradeMigrationPending(): Boolean = withContext(Dispatchers.IO) {
        migrationPreferences.getBoolean(UpgradeMigrationPendingKey, false)
    }

    private suspend fun setUpgradeMigrationPending(pending: Boolean) = withContext(Dispatchers.IO) {
        check(
            migrationPreferences.edit()
                .putBoolean(UpgradeMigrationPendingKey, pending)
                .commit(),
        ) { "Unable to persist onboarding migration state" }
    }
}

private val OnboardingCompletedKey = booleanPreferencesKey("onboarding_completed")
private val OnboardingInitializedKey = booleanPreferencesKey("onboarding_initialized")
private val StartOnBootKey = booleanPreferencesKey("start_on_boot")
private val StartupScreenKey = stringPreferencesKey("startup_screen")
private val TextSizeKey = stringPreferencesKey("text_size")
private val PreferredMediaAppKey = stringPreferencesKey("preferred_media_app")
private val NavigationAppKey = stringPreferencesKey("navigation_app")
private val NavigationCompatibilityModeKey = booleanPreferencesKey("navigation_compatibility_mode")
private val NavigationCardPositionKey = stringPreferencesKey("navigation_card_position")
private val RailPositionKey = stringPreferencesKey("rail_position")
private val OverlaySizeKey = stringPreferencesKey("overlay_size")
private val MediaCardCollapsedKey = booleanPreferencesKey("media_card_collapsed")
private val AppThemeKey = stringPreferencesKey("app_theme")
internal val VoiceGuidanceMutedKey = booleanPreferencesKey("voice_guidance_muted")
private val VoiceVolumeKey = stringPreferencesKey("voice_guidance_volume")
private val VoiceNameKey = stringPreferencesKey("voice_guidance_voice")
private val SpeedUnitKey = stringPreferencesKey("speed_unit")
private val CameraAlertDistanceKey = stringPreferencesKey("camera_alert_distance")
private val OverspeedWarningKey = stringPreferencesKey("overspeed_warning")
private const val MigrationPreferencesName = "settings_migrations"
private const val UpgradeMigrationPendingKey = "onboarding_upgrade_migration_pending"

internal fun settingsFromPreferences(preferences: Preferences): LauncherSettings = LauncherSettings(
    onboardingCompleted = preferences[OnboardingCompletedKey] ?: false,
    startOnBoot = preferences[StartOnBootKey] ?: false,
    startupScreen = preferences[StartupScreenKey]
        ?.let { value -> runCatching { StartupScreen.valueOf(value) }.getOrNull() }
        ?: StartupScreen.Home,
    textSizePreset = preferences[TextSizeKey]
        ?.let { value -> runCatching { TextSizePreset.valueOf(value) }.getOrNull() }
        ?: TextSizePreset.Standard,
    preferredMediaAppKey = preferences[PreferredMediaAppKey],
    navigationAppKey = preferences[NavigationAppKey],
    navigationCompatibilityMode = preferences[NavigationCompatibilityModeKey] ?: false,
    navigationCardPosition = preferences[NavigationCardPositionKey]
        ?.let { value -> runCatching { NavigationCardPosition.valueOf(value) }.getOrNull() }
        ?: NavigationCardPosition.BottomLeft,
    railPosition = preferences[RailPositionKey]
        ?.let { value -> runCatching { RailPosition.valueOf(value) }.getOrNull() }
        ?: RailPosition.Left,
    overlaySize = preferences[OverlaySizeKey]
        ?.let { value -> runCatching { OverlaySize.valueOf(value) }.getOrNull() }
        ?: OverlaySize.Auto,
    mediaCardCollapsed = preferences[MediaCardCollapsedKey] ?: false,
    appTheme = preferences[AppThemeKey]
        ?.let { value -> runCatching { AppTheme.valueOf(value) }.getOrNull() }
        ?: AppTheme.Default,
    voiceGuidanceMuted = preferences[VoiceGuidanceMutedKey] ?: false,
    voiceVolume = preferences[VoiceVolumeKey]
        ?.let { value -> runCatching { VoiceVolume.valueOf(value) }.getOrNull() }
        ?: VoiceVolume.Max,
    voiceName = preferences[VoiceNameKey],
    speedUnit = preferences[SpeedUnitKey]
        ?.let { value -> runCatching { SpeedUnitSetting.valueOf(value) }.getOrNull() }
        ?: SpeedUnitSetting.Auto,
    cameraAlertDistance = preferences[CameraAlertDistanceKey]
        ?.let { value -> runCatching { CameraAlertDistance.valueOf(value) }.getOrNull() }
        ?: CameraAlertDistance.Auto,
    overspeedWarning = preferences[OverspeedWarningKey]
        ?.let { value -> runCatching { OverspeedWarning.valueOf(value) }.getOrNull() }
        ?: OverspeedWarning.AtLimit,
)

internal fun shouldMigrateLegacyOnboardingState(
    onboardingCompleted: Boolean?,
    onboardingInitialized: Boolean?,
): Boolean = onboardingCompleted == null && onboardingInitialized == null

internal suspend fun runUpgradeMigrationWithPendingMarker(
    markPending: suspend () -> Unit,
    migrate: suspend () -> Unit,
    clearPending: suspend () -> Unit,
) {
    markPending()
    migrate()
    clearPending()
}
