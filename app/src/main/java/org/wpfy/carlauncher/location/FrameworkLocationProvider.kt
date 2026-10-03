package org.wpfy.carlauncher.location

import android.annotation.SuppressLint
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

internal data class FrameworkLocationReading(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val hasAccuracy: Boolean,
    val altitudeMeters: Double,
    val hasAltitude: Boolean,
    val speedMps: Float,
    val hasSpeed: Boolean,
    val bearingDegrees: Float,
    val hasBearing: Boolean,
    val timestampMillis: Long,
    val provider: String?,
    val mslAltitudeMeters: Double? = null,
    val verticalAccuracyMeters: Float? = null,
    val elapsedRealtimeMillis: Long? = null,
)

internal fun FrameworkLocationReading.toLocationSample(): LocationSample {
    return LocationSample(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracyMeters.takeIf { hasAccuracy },
        altitudeMeters = altitudeMeters.takeIf { hasAltitude },
        speedMps = speedMps.takeIf { hasSpeed },
        bearingDegrees = bearingDegrees.takeIf { hasBearing },
        timestampMillis = timestampMillis,
        provider = provider,
        mslAltitudeMeters = mslAltitudeMeters,
        verticalAccuracyMeters = verticalAccuracyMeters,
        elapsedRealtimeMillis = elapsedRealtimeMillis,
    )
}

internal fun frameworkProviderNames(
    gpsEnabled: Boolean,
    networkEnabled: Boolean,
    allowNetworkFallback: Boolean,
): List<String> {
    return buildList {
        if (gpsEnabled) add(LocationManager.GPS_PROVIDER)
        if (allowNetworkFallback && networkEnabled) {
            add(LocationManager.NETWORK_PROVIDER)
        }
    }
}

class FrameworkLocationProvider(context: Context) : LocationProvider {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    override fun status(): LocationProviderStatus {
        return LocationProviderStatus(
            permissionGranted = hasLocationPermission(),
            gpsEnabled = locationManager.isProviderEnabledSafely(LocationManager.GPS_PROVIDER),
            networkEnabled = locationManager.isProviderEnabledSafely(LocationManager.NETWORK_PROVIDER),
        )
    }

    @SuppressLint("MissingPermission")
    override fun lastKnownLocation(): LocationSample? {
        if (!hasLocationPermission()) return null

        val samples = frameworkProviders(allowNetworkFallback = true).mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }
                .getOrNull()
                ?.toSample()
        }
        return selectLatestLocationSample(samples)
    }

    override fun updates(request: LocationRequest): Flow<LocationSample> = callbackFlow {
        if (!hasLocationPermission()) {
            close()
            return@callbackFlow
        }

        // Every raw fix is logged; only the arbitrated ones reach consumers (nav, trip, HUD).
        val arbiter = LocationArbiter(SystemClock::elapsedRealtime)
        val listener = LocationListener { location ->
            val sample = location.toSample()
            arbiter.accept(sample)?.let { trySend(it) }
        }
        var registeredProviders = emptySet<String>()

        fun refreshProviders() {
            if (!hasLocationPermission()) {
                close()
                return
            }

            val providers = frameworkProviders(request.allowNetworkFallback).toSet()
            if (providers == registeredProviders) return

            runCatching { locationManager.removeUpdates(listener) }
            registeredProviders = emptySet()

            try {
                providers.forEach { provider ->
                    locationManager.requestLocationUpdates(
                        provider,
                        request.minTimeMillis,
                        request.minDistanceMeters,
                        listener,
                        Looper.getMainLooper(),
                    )
                }
                registeredProviders = providers
            } catch (_: SecurityException) {
                close()
            }
        }

        val providerChanges = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                refreshProviders()
            }
        }
        val providerFilter = IntentFilter().apply {
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
            addAction(LocationManager.MODE_CHANGED_ACTION)
        }

        ContextCompat.registerReceiver(
            appContext,
            providerChanges,
            providerFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        refreshProviders()

        awaitClose {
            runCatching { appContext.unregisterReceiver(providerChanges) }
            runCatching { locationManager.removeUpdates(listener) }
        }
    }.conflate()

    private fun frameworkProviders(allowNetworkFallback: Boolean): List<String> {
        return frameworkProviderNames(
            gpsEnabled = locationManager.isProviderEnabledSafely(LocationManager.GPS_PROVIDER),
            networkEnabled = locationManager.isProviderEnabledSafely(LocationManager.NETWORK_PROVIDER),
            allowNetworkFallback = allowNetworkFallback,
        )
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }
}

private fun LocationManager.isProviderEnabledSafely(provider: String): Boolean {
    return runCatching { isProviderEnabled(provider) }.getOrDefault(false)
}

/** A fix's elapsed time in ms; 0 or negative nanos mean "not set" and give null, never a very old fix. */
internal fun elapsedRealtimeMillisOrNull(elapsedRealtimeNanos: Long): Long? =
    if (elapsedRealtimeNanos > 0L) elapsedRealtimeNanos / 1_000_000L else null

internal fun Location.toSample(): LocationSample {
    return FrameworkLocationReading(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracy,
        hasAccuracy = hasAccuracy(),
        altitudeMeters = altitude,
        hasAltitude = hasAltitude(),
        speedMps = speed,
        hasSpeed = hasSpeed(),
        bearingDegrees = bearing,
        hasBearing = hasBearing(),
        timestampMillis = time,
        provider = provider,
        mslAltitudeMeters = if (LocationCompat.hasMslAltitude(this)) {
            LocationCompat.getMslAltitudeMeters(this)
        } else {
            null
        },
        verticalAccuracyMeters = if (hasVerticalAccuracy()) verticalAccuracyMeters else null,
        elapsedRealtimeMillis = elapsedRealtimeMillisOrNull(elapsedRealtimeNanos),
    ).toLocationSample()
}
