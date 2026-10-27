package org.wpfy.carlauncher

import android.app.Application
import org.wpfy.carlauncher.distribution.DistributionStartup
import org.wpfy.carlauncher.feature.home.HomeObdDriveStateHolder

class CarLauncherApplication : Application() {
    lateinit var homeObdDriveStateHolder: HomeObdDriveStateHolder
        private set

    override fun onCreate() {
        super.onCreate()
        DistributionStartup.preloadNativeLibraries()
        homeObdDriveStateHolder = HomeObdDriveStateHolder.createForProcess(this)
    }

    override fun onTerminate() {
        homeObdDriveStateHolder.close()
        super.onTerminate()
    }
}
