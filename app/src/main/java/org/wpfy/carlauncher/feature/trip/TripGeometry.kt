package org.wpfy.carlauncher.feature.trip

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal object TripGeometry {
    private const val EarthRadiusMeters = 6_371_000.0
    private const val MinimumTurnDisplacementMeters = 5.0

    fun distanceM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val latitudeDelta = Math.toRadians(lat2 - lat1)
        val longitudeDelta = Math.toRadians(lon2 - lon1)
        val latitude1 = Math.toRadians(lat1)
        val latitude2 = Math.toRadians(lat2)
        val latitudeSin = sin(latitudeDelta / 2.0)
        val longitudeSin = sin(longitudeDelta / 2.0)
        val haversine = (latitudeSin * latitudeSin +
            cos(latitude1) * cos(latitude2) * longitudeSin * longitudeSin).coerceIn(0.0, 1.0)
        return 2.0 * EarthRadiusMeters * atan2(sqrt(haversine), sqrt(1.0 - haversine))
    }

    fun bearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val latitude1 = Math.toRadians(lat1)
        val latitude2 = Math.toRadians(lat2)
        val longitudeDelta = Math.toRadians(lon2 - lon1)
        val y = sin(longitudeDelta) * cos(latitude2)
        val x = cos(latitude1) * sin(latitude2) -
            sin(latitude1) * cos(latitude2) * cos(longitudeDelta)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    fun angleDifference(a: Double, b: Double): Double {
        val difference = (a - b) % 360.0
        return abs((difference + 540.0) % 360.0 - 180.0)
    }

    fun shouldKeepPoint(
        previousLatitude: Double,
        previousLongitude: Double,
        previousBearingDegrees: Double?,
        latitude: Double,
        longitude: Double,
        bearingDegrees: Double?,
        distanceThresholdM: Double = 25.0,
        angleThresholdDegrees: Double = 20.0,
    ): Boolean {
        val displacement = distanceM(previousLatitude, previousLongitude, latitude, longitude)
        if (displacement >= distanceThresholdM) return true
        if (displacement < MinimumTurnDisplacementMeters) return false
        if (previousBearingDegrees == null || bearingDegrees == null) return false
        return angleDifference(previousBearingDegrees, bearingDegrees) >= angleThresholdDegrees
    }
}
