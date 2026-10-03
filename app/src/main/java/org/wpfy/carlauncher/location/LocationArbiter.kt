package org.wpfy.carlauncher.location

import android.location.LocationManager

/** While a GPS fix is this young, fixes from any other provider (network, fused) are dropped. */
internal const val GpsFreshForArbitrationMillis = 10_000L

/** A fallback fix is compared with the last accepted fix only while that one is at most this old. */
internal const val AccuracyCompareWindowMillis = 30_000L

/** A fallback fix this many times less accurate than the last accepted one (and worse than [WorseAccuracyFloorMeters]) is dropped. */
internal const val WorseAccuracyFactor = 3f
internal const val WorseAccuracyFloorMeters = 100f

/**
 * Picks which fixes of a mixed GPS + network stream reach the app. One instance per subscription; not thread safe
 * (callbacks arrive on one looper). [nowElapsedMillis] is the monotonic clock ([android.os.SystemClock.elapsedRealtime]).
 *
 * Speeds are never rewritten: a fix without one stays without one, and the speed consumer holds the last real speed itself
 * (see driveGpsSpeed), so a held speed keeps its original age.
 *
 * Rules:
 * 1. A GPS fix is always accepted.
 * 2. Any other provider's fix is dropped while the last GPS fix is at most [GpsFreshForArbitrationMillis] old.
 * 3. Once GPS is stale a fallback fix is accepted, unless it is much less accurate than a recent accepted fix.
 */
internal class LocationArbiter(private val nowElapsedMillis: () -> Long) {
    private var lastGpsAt: Long? = null
    private var lastAcceptedAt: Long? = null
    private var lastAcceptedAccuracy: Float? = null

    /** The sample to deliver, or null when it must be dropped. */
    fun accept(sample: LocationSample): LocationSample? {
        val now = nowElapsedMillis()
        val fixAt = sample.elapsedRealtimeMillis ?: now
        val isGps = sample.provider == LocationManager.GPS_PROVIDER
        if (!isGps) {
            val gpsAt = lastGpsAt
            if (gpsAt != null && now - gpsAt in 0..GpsFreshForArbitrationMillis) return null
            if (isMuchWorseThanRecent(sample, now)) return null
        }
        val acceptedAt = fixAt.coerceAtMost(now)
        val gpsAt = lastGpsAt
        if (isGps && (gpsAt == null || acceptedAt >= gpsAt)) {
            lastGpsAt = acceptedAt
        }
        val lastAt = lastAcceptedAt
        if (lastAt == null || acceptedAt >= lastAt) {
            lastAcceptedAt = acceptedAt
            lastAcceptedAccuracy = sample.accuracyMeters
        }
        return sample
    }

    private fun isMuchWorseThanRecent(sample: LocationSample, now: Long): Boolean {
        val acc = sample.accuracyMeters ?: return false
        val lastAt = lastAcceptedAt ?: return false
        val lastAcc = lastAcceptedAccuracy ?: return false
        if (now - lastAt !in 0..AccuracyCompareWindowMillis) return false
        return acc > WorseAccuracyFloorMeters && acc > lastAcc * WorseAccuracyFactor
    }
}
