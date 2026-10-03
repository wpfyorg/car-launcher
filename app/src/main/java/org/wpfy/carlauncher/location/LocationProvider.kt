package org.wpfy.carlauncher.location

import android.location.Location
import androidx.core.location.LocationCompat
import kotlinx.coroutines.flow.Flow

data class LocationSample(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float? = null,
    val altitudeMeters: Double? = null,
    val speedMps: Float? = null,
    val bearingDegrees: Float? = null,
    val timestampMillis: Long,
    val provider: String? = null,
    val mslAltitudeMeters: Double? = null,
    val verticalAccuracyMeters: Float? = null,
    /** When the fix was taken on the monotonic clock ([android.os.SystemClock.elapsedRealtime]), immune to a wrong wall clock; null when unknown. */
    val elapsedRealtimeMillis: Long? = null,
)

data class LocationRequest(
    val minTimeMillis: Long = 1_000L,
    val minDistanceMeters: Float = 0f,
    val allowNetworkFallback: Boolean = true,
)

data class LocationProviderStatus(
    val permissionGranted: Boolean,
    val gpsEnabled: Boolean,
    val networkEnabled: Boolean,
) {
    val anyProviderEnabled: Boolean
        get() = gpsEnabled || networkEnabled
}

interface LocationProvider {
    fun status(): LocationProviderStatus

    fun lastKnownLocation(): LocationSample?

    /**
     * Emits location fixes while at least one requested framework provider is available.
     * Call [status] first when the caller needs to distinguish permission/provider failures.
     */
    fun updates(request: LocationRequest = LocationRequest()): Flow<LocationSample>
}

internal fun selectLatestLocationSample(samples: Iterable<LocationSample?>): LocationSample? {
    return samples.filterNotNull().maxByOrNull { it.timestampMillis }
}

fun LocationSample.toAndroidLocation(): Location {
    return Location(provider ?: "carlauncher").apply {
        latitude = this@toAndroidLocation.latitude
        longitude = this@toAndroidLocation.longitude
        time = timestampMillis
        accuracyMeters?.let { accuracy = it }
        altitudeMeters?.let { altitude = it }
        speedMps?.let { speed = it }
        bearingDegrees?.let { bearing = it }
        this@toAndroidLocation.mslAltitudeMeters?.let { LocationCompat.setMslAltitudeMeters(this, it) }
        this@toAndroidLocation.verticalAccuracyMeters?.let { setVerticalAccuracyMeters(it) }
    }
}
