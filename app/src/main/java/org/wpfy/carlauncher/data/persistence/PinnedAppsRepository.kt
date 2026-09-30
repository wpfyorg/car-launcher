package org.wpfy.carlauncher.data.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.launcherPreferences by preferencesDataStore(name = "launcher_preferences")

class PinnedAppsRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
) {
    constructor(context: Context) : this(context.applicationContext.launcherPreferences)

    val orderedPinnedAppKeys: Flow<List<String>> = dataStore.data.map { preferences ->
        resolvePinnedOrder(
            encodedOrder = preferences[OrderedPinnedAppsKey],
            legacyKeys = preferences[PinnedAppsKey].orEmpty(),
        )
    }

    val pinnedAppKeys: Flow<Set<String>> = orderedPinnedAppKeys.map(List<String>::toSet)

    suspend fun setPinned(appKey: String, pinned: Boolean) {
        dataStore.edit { preferences ->
            val pinnedApps = preferences[PinnedAppsKey].orEmpty().toMutableSet()
            val orderedApps = resolvePinnedOrder(
                encodedOrder = preferences[OrderedPinnedAppsKey],
                legacyKeys = pinnedApps,
            ).toMutableList()
            if (pinned) {
                if (pinnedApps.add(appKey)) {
                    orderedApps.add(appKey)
                }
            } else {
                pinnedApps.remove(appKey)
                orderedApps.remove(appKey)
            }
            preferences[OrderedPinnedAppsKey] = encodePinnedOrder(orderedApps)
            preferences[PinnedAppsKey] = pinnedApps
        }
    }

    suspend fun movePinned(appKey: String, targetIndex: Int) {
        dataStore.edit { preferences ->
            val pinnedApps = preferences[PinnedAppsKey].orEmpty()
            val orderedApps = resolvePinnedOrder(
                encodedOrder = preferences[OrderedPinnedAppsKey],
                legacyKeys = pinnedApps,
            ).toMutableList()
            val sourceIndex = orderedApps.indexOf(appKey)
            if (sourceIndex == -1) return@edit

            val destinationIndex = targetIndex.coerceIn(0, orderedApps.lastIndex)
            orderedApps.removeAt(sourceIndex)
            orderedApps.add(destinationIndex, appKey)
            preferences[OrderedPinnedAppsKey] = encodePinnedOrder(orderedApps)
            preferences[PinnedAppsKey] = pinnedApps
        }
    }
}

private val PinnedAppsKey = stringSetPreferencesKey("pinned_apps")
private val OrderedPinnedAppsKey = stringPreferencesKey("pinned_apps_ordered")
