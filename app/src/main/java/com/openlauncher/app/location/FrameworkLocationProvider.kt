package com.openlauncher.app.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

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

    override fun lastKnownLocation(): LocationSample? {
        if (!hasLocationPermission()) return null

        return frameworkProviders(allowNetworkFallback = true)
            .mapNotNull { provider ->
                runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            }
            .maxByOrNull(Location::getTime)
            ?.toSample()
    }

    override fun updates(request: LocationRequest): Flow<LocationSample> = callbackFlow {
        if (!hasLocationPermission()) {
            close()
            return@callbackFlow
        }

        val providers = frameworkProviders(request.allowNetworkFallback)
        if (providers.isEmpty()) {
            close()
            return@callbackFlow
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location.toSample())
            }

            @Suppress("DEPRECATION")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) = Unit
        }

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
        } catch (_: SecurityException) {
            close()
            return@callbackFlow
        }

        awaitClose {
            runCatching { locationManager.removeUpdates(listener) }
        }
    }.conflate()

    private fun frameworkProviders(allowNetworkFallback: Boolean): List<String> {
        return buildList {
            if (
                hasFineLocationPermission() &&
                locationManager.isProviderEnabledSafely(LocationManager.GPS_PROVIDER)
            ) {
                add(LocationManager.GPS_PROVIDER)
            }
            if (
                allowNetworkFallback &&
                locationManager.isProviderEnabledSafely(LocationManager.NETWORK_PROVIDER)
            ) {
                add(LocationManager.NETWORK_PROVIDER)
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return hasFineLocationPermission() || ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }
}

private fun LocationManager.isProviderEnabledSafely(provider: String): Boolean {
    return runCatching { isProviderEnabled(provider) }.getOrDefault(false)
}

private fun Location.toSample(): LocationSample {
    return LocationSample(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracy.takeIf { hasAccuracy() },
        altitudeMeters = altitude.takeIf { hasAltitude() },
        speedMps = speed.takeIf { hasSpeed() },
        bearingDegrees = bearing.takeIf { hasBearing() },
        timestampMillis = time,
        provider = provider,
    )
}
