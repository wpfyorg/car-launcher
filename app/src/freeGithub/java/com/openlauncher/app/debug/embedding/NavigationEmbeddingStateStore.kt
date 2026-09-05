package com.openlauncher.app.debug.embedding

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.navigationEmbeddingPreferences by preferencesDataStore(
    name = "navigation_embedding_state",
)

internal data class PersistedNavigationTask(
    val appKey: String,
    val taskId: Int,
    val runtimePackages: Set<String>,
)

internal class NavigationEmbeddingStateStore(context: Context) {
    private val dataStore = context.applicationContext.navigationEmbeddingPreferences

    suspend fun current(): PersistedNavigationTask? {
        val preferences = dataStore.data.first()
        val appKey = preferences[AppKey] ?: return null
        val taskId = preferences[TaskId] ?: return null
        val runtimePackages = preferences[RuntimePackages].orEmpty()
        if (runtimePackages.isEmpty()) return null
        return PersistedNavigationTask(
            appKey = appKey,
            taskId = taskId,
            runtimePackages = runtimePackages,
        )
    }

    suspend fun save(task: PersistedNavigationTask) {
        dataStore.edit { preferences ->
            preferences[AppKey] = task.appKey
            preferences[TaskId] = task.taskId
            preferences[RuntimePackages] = task.runtimePackages
        }
    }

    suspend fun clear(appKey: String? = null) {
        dataStore.edit { preferences ->
            if (appKey != null && preferences[AppKey] != appKey) return@edit
            preferences.remove(AppKey)
            preferences.remove(TaskId)
            preferences.remove(RuntimePackages)
        }
    }

    private companion object {
        val AppKey = stringPreferencesKey("app_key")
        val TaskId = intPreferencesKey("task_id")
        val RuntimePackages = stringSetPreferencesKey("runtime_packages")
    }
}
