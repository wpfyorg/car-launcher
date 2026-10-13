package org.wpfy.carlauncher.feature.trip

import org.wpfy.carlauncher.location.LocationSample

/** Epoch milliseconds, meters and meters/second throughout this API. */
data class Trip(
    val id: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val distanceM: Double = 0.0,
    val movingMs: Long = 0,
    val idleMs: Long = 0,
    val maxSpeedMps: Double = 0.0,
    val startLatitude: Double? = null,
    val startLongitude: Double? = null,
    val endLatitude: Double? = null,
    val endLongitude: Double? = null,
    val startAddress: String? = null,
    val endAddress: String? = null,
) {
    val avgMovingSpeedMps: Double get() = if (movingMs > 0) distanceM * 1_000 / movingMs else 0.0
    /** Accounted time only: unobserved gaps (signal loss, pauses, restarts) are excluded. */
    val durationMs: Long get() = movingMs + idleMs
}

/** Decimated track, also supplies the future speed profile. Segment changes mark fix gaps. */
data class TripPoint(
    val tripId: String,
    val timestampMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val speedMps: Double,
    val altitudeM: Double? = null,
    val segment: Int = 0,
)

data class TripSummary(val trip: Trip)
data class TripDetails(val trip: Trip, val points: List<TripPoint>)

/** A strict Trip read distinguishes an empty history from unavailable storage and a buffered-only live fallback. */
sealed interface TripReadResult {
    data class Complete(val details: TripDetails, val liveState: TripState? = null) : TripReadResult
    data class Partial(val details: TripDetails, val liveState: TripState) : TripReadResult
    data object Empty : TripReadResult
    data object Unavailable : TripReadResult
}

/** Reset records the old trip before checkpointing a fresh trip; partial completion keeps the archived history. */
sealed interface TripResetResult {
    data class Reset(val archivedTripId: String, val replacementTripId: String) : TripResetResult
    data class StaleRequest(val currentTripId: String?) : TripResetResult
    data object NoActiveTrip : TripResetResult
    data object Unavailable : TripResetResult
    data class ArchiveFailed(val tripId: String) : TripResetResult
    data class ArchivedWithoutReplacement(val archivedTripId: String, val reason: ReplacementFailure) : TripResetResult

    enum class ReplacementFailure { RecorderClosing, Storage }
}

/** Latest process-owned Reset request; a null [outcome] means the command is still pending. */
data class TripResetStatus(val expectedTripId: String, val outcome: TripResetResult? = null) {
    val isPending: Boolean get() = outcome == null
}

sealed interface TripState {
    data object Idle : TripState
    data class Recording(val trip: Trip, val speedMps: Double) : TripState
    data class Paused(val trip: Trip, val reason: PauseReason) : TripState
}

enum class PauseReason { Manual, LocationUnavailable }

/** Everything needed for recovery; never infer unobserved distance or moving time after restart. */
data class TripCheckpoint(
    val trip: Trip,
    val lastSample: LocationSample?,
    val idleSince: Long?,
    val pausedAt: Long?,
    val segment: Int,
    val lastTrackPoint: TripPoint?,
    val lastTrackBearing: Double?,
    val lastObservedAt: Long = lastSample?.timestampMillis ?: trip.startedAt,
    /** Idle time booked since [idleSince]; survives anchor loss so a terminal stop trims exactly that. */
    val idleBookedMs: Long = 0,
    /** Manual start/resume with no fix yet: the segment has no observation of its own, so recovery must not judge it stale. */
    val awaitingFix: Boolean = false,
)

data class TripTotals(
    val tripCount: Int = 0,
    val distanceM: Double = 0.0,
    val movingMs: Long = 0,
    val idleMs: Long = 0,
    val maxSpeedMps: Double = 0.0,
) {
    val avgMovingSpeedMps: Double get() = if (movingMs > 0) distanceM * 1_000 / movingMs else 0.0
}

data class TripPeriodTotals(val today: TripTotals, val week: TripTotals, val month: TripTotals)
