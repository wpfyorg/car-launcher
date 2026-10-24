package org.wpfy.carlauncher.launcher

data class AppsUiState(
    val apps: List<LauncherApp> = emptyList(),
    val orderedPinnedAppKeys: List<String> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
) {
    fun isPinned(app: LauncherApp): Boolean = app.stableKey in orderedPinnedAppKeys

    val visibleApps: List<LauncherApp>
        get() {
            val normalizedQuery = query.trim()
            if (normalizedQuery.isEmpty()) {
                val appsByKey = apps.associateBy(LauncherApp::stableKey)
                return resolveVisibleAppOrder(
                    catalogKeys = apps.map(LauncherApp::stableKey),
                    orderedPinnedKeys = orderedPinnedAppKeys,
                ).mapNotNull(appsByKey::get)
            }

            return apps.filter { app ->
                app.label.contains(normalizedQuery, ignoreCase = true) ||
                    app.packageName.contains(normalizedQuery, ignoreCase = true)
            }
        }
}

internal fun resolveVisibleAppOrder(
    catalogKeys: List<String>,
    orderedPinnedKeys: List<String>,
): List<String> {
    val catalogKeySet = catalogKeys.toSet()
    val seen = mutableSetOf<String>()
    val pinned = orderedPinnedKeys.filter { key -> key in catalogKeySet && seen.add(key) }
    return pinned + catalogKeys.filter(seen::add)
}
