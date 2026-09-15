package org.wpfy.carlauncher.distribution

import android.content.Context
import org.wpfy.carlauncher.feature.navigation.speed.SpeedCameraRegionRepository
import org.wpfy.carlauncher.feature.navigation.speed.UnavailableSpeedCameraRegionRepository

object DistributionSpeedCameras {
    @Suppress("UNUSED_PARAMETER")
    fun repository(context: Context): SpeedCameraRegionRepository = UnavailableSpeedCameraRegionRepository

    @Suppress("UNUSED_PARAMETER")
    fun start(context: Context) = Unit
}
