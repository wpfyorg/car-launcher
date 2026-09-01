package com.openlauncher.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsPreferences by preferencesDataStore(name = "settings_preferences")

class SettingsRepository(context: Context) {
    private val dataStore = context.applicationContext.settingsPreferences

    val settings: Flow<LauncherSettings> = dataStore.data.map { preferences ->
        LauncherSettings(
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
        )
    }

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
}

private val StartOnBootKey = booleanPreferencesKey("start_on_boot")
private val StartupScreenKey = stringPreferencesKey("startup_screen")
private val TextSizeKey = stringPreferencesKey("text_size")
private val PreferredMediaAppKey = stringPreferencesKey("preferred_media_app")
private val NavigationAppKey = stringPreferencesKey("navigation_app")
private val NavigationCompatibilityModeKey = booleanPreferencesKey("navigation_compatibility_mode")
