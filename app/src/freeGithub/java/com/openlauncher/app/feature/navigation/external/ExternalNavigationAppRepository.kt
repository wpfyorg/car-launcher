package com.openlauncher.app.feature.navigation.external

import com.openlauncher.app.launcher.AppCatalog
import com.openlauncher.app.launcher.LauncherApp

class ExternalNavigationAppRepository(
    private val appCatalog: AppCatalog,
) {
    suspend fun resolve(stableKey: String?): LauncherApp? {
        if (stableKey == null) return null
        return appCatalog.loadApps().firstOrNull { it.stableKey == stableKey }
    }
}
