package org.wpfy.carlauncher.distribution

import android.content.Context
import org.wpfy.carlauncher.feature.navigation.offline.OfflineMapRepository
import org.wpfy.carlauncher.feature.navigation.offline.UnavailableOfflineMapRepository

object DistributionOfflineMaps {
    @Suppress("UNUSED_PARAMETER")
    fun repository(context: Context): OfflineMapRepository = UnavailableOfflineMapRepository
}
