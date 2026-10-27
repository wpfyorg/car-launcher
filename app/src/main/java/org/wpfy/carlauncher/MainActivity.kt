package org.wpfy.carlauncher

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.Manifest
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.wpfy.carlauncher.distribution.DistributionSpeedCameras
import org.wpfy.carlauncher.core.diagnostics.AppLog
import org.wpfy.carlauncher.core.performance.PerformanceMonitor
import org.wpfy.carlauncher.core.performance.PerformanceTimingState
import org.wpfy.carlauncher.feature.trip.TripRepositoryFactory
import org.wpfy.carlauncher.feature.trip.TripSessionStore

class MainActivity : ComponentActivity() {
    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var performanceTiming: PerformanceTimingState
    private var firstDrawListener: ViewTreeObserver.OnDrawListener? = null
    private var firstDrawRemoval: Runnable? = null
    private var homeRequests by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        val activityCreateElapsedMs = SystemClock.elapsedRealtime()
        super.onCreate(savedInstanceState)
        AppLog.initialize(applicationContext)
        AppLog.i(Tag, "onCreate restored=${savedInstanceState != null}")
        deferSpeedCameraWork()
        performanceMonitor = PerformanceMonitor.get(applicationContext)
        performanceTiming = performanceMonitor.beginActivityTiming(
            activityCreateElapsedMs = activityCreateElapsedMs,
            verifiedColdLaunchProven = isDebuggable() &&
                intent.getBooleanExtra(DebugVerifiedColdLaunchExtra, false),
        )
        val processRestarted = isProcessRestart(savedInstanceState != null, launchedInThisProcess)
        launchedInThisProcess = true
        if (shouldGoHomeOnCreate(processRestarted, intent.action, intent.categories, intent.flags)) {
            homeRequests++
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        setContent {
            CarLauncherApp(homeRequests = homeRequests)
        }
        registerFirstDrawMeasurement()
    }

    override fun onStart() {
        super.onStart()
        AppLog.i(Tag, "onStart")
        performanceTiming.onStart(SystemClock.elapsedRealtime())
    }

    override fun onResume() {
        super.onResume()
        AppLog.i(Tag, "onResume")
        performanceMonitor.onResume()
        startTripMonitoringIfPermitted()
    }

    override fun onPause() {
        performanceTiming.onPause()
        performanceMonitor.onPause()
        AppLog.i(Tag, "onPause")
        super.onPause()
    }

    override fun onStop() {
        performanceTiming.onStop()
        AppLog.i(Tag, "onStop")
        super.onStop()
    }

    override fun onDestroy() {
        cancelFirstDrawMeasurement()
        performanceTiming.onDestroy()
        AppLog.i(Tag, "onDestroy changingConfigurations=$isChangingConfigurations")
        if (!isChangingConfigurations) {
            TripRepositoryFactory.stopRecordingService(applicationContext)
            TripSessionStore.close()
        }
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        AppLog.i(Tag, "onNewIntent action=${intent.action.orEmpty()}")
        if (isHomeKeyIntent(intent.action, intent.categories, intent.flags)) {
            homeRequests++
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            performanceTiming.onWindowFocusChanged(
                hasFocus = true,
                focusElapsedMs = SystemClock.elapsedRealtime(),
            )?.let(performanceMonitor::recordWarmReturnToFocus)
            hideSystemBars()
            // A permission dialog is not guaranteed to resume this activity, but it always returns focus.
            startTripMonitoringIfPermitted()
        }
    }

    /**
     * Speed-camera sync and report upload start three WorkManager jobs that open a database and
     * a TLS client; on a slow head unit that competes with Home's first frames. They are not needed
     * to draw Home, so they wait until the first frame is long past. Runs once per process.
     */
    private fun deferSpeedCameraWork() {
        if (speedCameraWorkScheduled) return
        speedCameraWorkScheduled = true
        val appContext = applicationContext
        Handler(Looper.getMainLooper()).postDelayed(
            { DistributionSpeedCameras.start(appContext) },
            SpeedCameraWorkDelayMillis,
        )
    }

    private fun registerFirstDrawMeasurement() {
        if (!performanceTiming.registerFirstDrawListener()) return
        val decorView = window.decorView
        val listener = object : ViewTreeObserver.OnDrawListener {
            override fun onDraw() {
                val measurement = performanceTiming.markFirstDraw(SystemClock.elapsedRealtime()) ?: return
                performanceMonitor.recordFirstDraw(measurement)
                if (!performanceTiming.markFirstDrawRemovalPosted()) return

                val removal = Runnable {
                    firstDrawRemoval = null
                    removeFirstDrawListener()
                }
                firstDrawRemoval = removal
                decorView.post(removal)
            }
        }
        firstDrawListener = listener
        decorView.viewTreeObserver.addOnDrawListener(listener)
    }

    private fun cancelFirstDrawMeasurement() {
        firstDrawRemoval?.let(window.decorView::removeCallbacks)
        firstDrawRemoval = null
        removeFirstDrawListener()
    }

    private fun removeFirstDrawListener() {
        val listener = firstDrawListener ?: return
        val observer = window.decorView.viewTreeObserver
        if (observer.isAlive) {
            observer.removeOnDrawListener(listener)
        }
        performanceTiming.completeFirstDrawListenerRemoval(observer.isAlive)
        firstDrawListener = null
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun isDebuggable(): Boolean =
        applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    /** Idempotent. Trips need precise location: the recorder rejects fixes coarser than 30 m. */
    private fun startTripMonitoringIfPermitted() {
        if (TripRepositoryFactory.persistsTrips &&
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            // The service keeps location updates unthrottled in the background; monitoring also runs
            // directly so recording still works when the service cannot start.
            TripRepositoryFactory.startRecordingService(applicationContext)
            TripSessionStore.startMonitoring(applicationContext)
        }
    }

    private companion object {
        /** Set by the first create in this process; a later create with saved state is a recreation, not a restart. */
        var launchedInThisProcess = false
        var speedCameraWorkScheduled = false
        const val SpeedCameraWorkDelayMillis = 10_000L

        const val Tag = "MainActivity"
        const val DebugVerifiedColdLaunchExtra =
            "org.wpfy.carlauncher.debug.VERIFIED_COLD_LAUNCH"
    }
}
