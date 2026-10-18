package org.wpfy.carlauncher.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.location.LocationManager
import android.os.SystemClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.transformLatest
import org.wpfy.carlauncher.data.settings.UnitSystem
import org.wpfy.carlauncher.design.adapters.NoSpeedText
import org.wpfy.carlauncher.design.adapters.speedUnitLabelFor
import org.wpfy.carlauncher.design.screens.KitHudState
import org.wpfy.carlauncher.location.FrameworkLocationProvider
import org.wpfy.carlauncher.location.LocationProvider
import org.wpfy.carlauncher.location.LocationRequest
import org.wpfy.carlauncher.location.LocationSample

/** A GPS fix shows its speed on the Free Drive Card for this long after it was taken; then the speed is hidden. */
internal const val DriveGpsSpeedFreshMillis = 3_000L

private const val KphPerMps = 3.6
private const val MilesPerKilometre = 0.621371

/** The Free card's speed reading: [kph] rounded in [units], no limit (Free has none). */
internal fun freeDriveSpeed(kph: Double, units: UnitSystem): KitHudState.Speed {
    val inUnits = if (units == UnitSystem.Imperial) kph * MilesPerKilometre else kph
    val value = if (inUnits < 1.0) NoSpeedText else Math.round(inUnits).toString()
    val unit = speedUnitLabelFor(units)
    return KitHudState.Speed(value = value, unit = unit, limit = null, description = "$value $unit")
}

/**
 * The fix's speed in km/h, or null when it has none, is negative or not finite, is older than [freshnessMillis], or is from the future (a negative age).
 * Age is measured on the monotonic clock ([nowElapsedMillis] against the fix's elapsed time), never the wall clock, which a
 * head unit may have wrong. A fix without an elapsed time counts as fresh when it arrives.
 */
internal fun gpsSpeedKph(sample: LocationSample?, nowElapsedMillis: Long, freshnessMillis: Long = DriveGpsSpeedFreshMillis): Double? {
    val speed = sample?.speedMps?.takeIf { it.isFinite() && it >= 0f } ?: return null
    return (speed * KphPerMps).takeIf { fixAgeMillis(sample, nowElapsedMillis) in 0L..freshnessMillis }
}

private fun fixAgeMillis(sample: LocationSample, nowElapsedMillis: Long): Long =
    sample.elapsedRealtimeMillis?.let { nowElapsedMillis - it } ?: 0L

/** Where the Drive Card's speed comes from, in priority order: OBD, fresh GPS, a bridged GPS gap, nothing. */
internal enum class SpeedSource(val logName: String) { Obd("obd"), Gps("gps"), Bridge("bridge"), None("none") }

internal data class DriveSpeed(val kph: Double?, val source: SpeedSource) {
    companion object { val None = DriveSpeed(null, SpeedSource.None) }
}

/**
 * Past [DriveGpsSpeedFreshMillis] a GPS speed is held, falling in a straight line to zero at this age, then dropped.
 * No accelerometer: a phone or head unit in a mount has an unknown orientation and horizontal acceleration has no sign,
 * so integrating it cannot tell speeding up from braking and drifts within seconds. A decaying hold is never wrong by more
 * than the speed itself, needs no sensor (many head units have none) and costs no battery.
 */
internal const val DriveSpeedBridgeMillis = 8_000L

/** The speed to show for [sample]: exact while fresh, a decaying hold up to [DriveSpeedBridgeMillis], then none. */
internal fun bridgedDriveSpeed(sample: LocationSample?, nowElapsedMillis: Long): DriveSpeed {
    val speed = sample?.speedMps?.takeIf { it.isFinite() && it >= 0f } ?: return DriveSpeed.None
    val age = fixAgeMillis(sample, nowElapsedMillis)
    val kph = speed * KphPerMps
    return when {
        age < 0L || age > DriveSpeedBridgeMillis -> DriveSpeed.None
        age <= DriveGpsSpeedFreshMillis -> DriveSpeed(kph, SpeedSource.Gps)
        else -> {
            val left = (DriveSpeedBridgeMillis - age).toDouble() / (DriveSpeedBridgeMillis - DriveGpsSpeedFreshMillis)
            DriveSpeed(kph * left, SpeedSource.Bridge)
        }
    }
}

/** OBD speed wins whenever it is fresh (the caller passes null when stale or disconnected); otherwise the GPS reading. */
internal fun resolveDriveSpeed(obdKph: Double?, gps: DriveSpeed): DriveSpeed =
    if (obdKph != null) DriveSpeed(obdKph, SpeedSource.Obd) else gps

/** Milliseconds between re-emissions while a held speed decays. */
private const val BridgeStepMillis = 1_000L

/**
 * The Free Drive Card's GPS-derived speed: exact while a fix is fresh, then a decaying hold across a short GPS gap, then
 * [DriveSpeed.None]. A fix without a speed never replaces the last speed. A cached fix is as old as its elapsed time says,
 * so it is never shown as fresh. Never prompts: without location permission it emits none and does not subscribe.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun LocationProvider.driveGpsSpeed(now: () -> Long = SystemClock::elapsedRealtime): Flow<DriveSpeed> {
    if (!status().permissionGranted) return flowOf(DriveSpeed.None)
    return flow {
        var newestGpsAt: Long? = null
        updates(LocationRequest())
            .filter {
                it.provider == LocationManager.GPS_PROVIDER &&
                    it.speedMps?.let { s -> s.isFinite() && s >= 0f } == true
            }
            .collect { arrived ->
                val arrivalAt = now()
                // A fix without elapsed time is aged from arrival, or it would never decay.
                val sample = if (arrived.elapsedRealtimeMillis == null) arrived.copy(elapsedRealtimeMillis = arrivalAt) else arrived
                val fixAt = (sample.elapsedRealtimeMillis ?: arrivalAt).coerceAtMost(arrivalAt)
                val latestAt = newestGpsAt
                if (latestAt == null || fixAt >= latestAt) {
                    newestGpsAt = fixAt
                    emit(sample)
                }
            }
    }.transformLatest { sample ->
        while (true) {
            val speed = bridgedDriveSpeed(sample, now())
            emit(speed)
            if (speed.source == SpeedSource.None) break
            val age = fixAgeMillis(sample, now())
            val next = if (age < DriveGpsSpeedFreshMillis) DriveGpsSpeedFreshMillis - age + 1L else BridgeStepMillis
            delay(next.coerceAtMost((DriveSpeedBridgeMillis - age).coerceAtLeast(0L) + 1L))
        }
    }
}

/**
 * Free Home's GPS-derived speed, collected only while [enabled] and the screen is started (Home shown); none otherwise. Location
 * permission is only checked, never asked for here.
 */
@Composable
internal fun rememberDriveGpsSpeed(enabled: Boolean): DriveSpeed {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var started by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> started = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    var speed by remember { mutableStateOf(DriveSpeed.None) }
    LaunchedEffect(enabled, started) {
        speed = DriveSpeed.None
        if (enabled && started) FrameworkLocationProvider(context).driveGpsSpeed().collect { speed = it }
    }
    return speed
}

/**
 * Whether location permission is granted, read from the provider's status (never asked for) and re-read every time the
 * screen resumes, so a grant made in Settings is picked up. False when not [enabled].
 */
@Composable
internal fun rememberLocationPermissionGranted(enabled: Boolean): Boolean {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var granted by remember(enabled) { mutableStateOf(enabled && FrameworkLocationProvider(context).status().permissionGranted) }
    DisposableEffect(lifecycle, enabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = enabled && FrameworkLocationProvider(context).status().permissionGranted
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return granted
}
