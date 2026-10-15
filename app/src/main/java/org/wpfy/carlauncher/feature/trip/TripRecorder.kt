package org.wpfy.carlauncher.feature.trip

import android.os.SystemClock
import java.io.Writer
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.wpfy.carlauncher.core.diagnostics.AppLog
import org.wpfy.carlauncher.location.LocationProvider
import org.wpfy.carlauncher.location.LocationRequest
import org.wpfy.carlauncher.location.LocationSample

private const val Tag = "TripRecorder"
private const val CheckpointIntervalMs = 15_000L

data class TripRecorderConfig(
    val autoStartSpeedMps: Double = 8.0 / 3.6,
    val autoStartDurationMs: Long = 20_000,
    val movingSpeedMps: Double = 2.0 / 3.6,
    val idleTimeoutMs: Long = 300_000,
    val maxAccuracyM: Float = 30f,
    val maxSpeedMps: Double = 250.0 / 3.6,
    val maxSampleGapMs: Long = 15_000,
    val trackDistanceM: Double = 25.0,
    val trackAngleDegrees: Double = 20.0,
) {
    init {
        require(autoStartSpeedMps.isFinite() && autoStartSpeedMps > 0)
        require(movingSpeedMps.isFinite() && movingSpeedMps > 0 && movingSpeedMps <= autoStartSpeedMps)
        require(autoStartDurationMs > 0 && idleTimeoutMs > 0 && maxSampleGapMs > 0)
        require(maxAccuracyM.isFinite() && maxAccuracyM > 0)
        require(maxSpeedMps.isFinite() && maxSpeedMps > autoStartSpeedMps)
        require(trackDistanceM.isFinite() && trackDistanceM >= 5)
        require(trackAngleDegrees.isFinite() && trackAngleDegrees in 0.0..180.0)
    }
}

/**
 * Backend only. Caller starts monitoring after location permission is available, and owns its scope.
 * No foreground service: recording lasts while the launcher process runs. Each accepted fix is
 * checkpointed every 15 s and at lifecycle changes; restart resumes the trip without bridging unobserved
 * time/distance, and the next fix or the idle timer closes it at its last observation when stale. Manual pauses exclude unobserved time from moving/idle stats.
 */
class TripRecorder(
    private val locationProvider: LocationProvider,
    private val repository: TripRepository,
    private val scope: CoroutineScope,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val config: TripRecorderConfig = TripRecorderConfig(),
    /** Monotonic clock for arrival deadlines; immune to runtime RTC corrections. */
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
    /** Closed recorder this one replaces: its in-flight write must land before recovery reads the checkpoint. */
    private var predecessor: TripRecorder? = null,
) {
    private val mutex = Mutex()
    private val shutdownLock = Any()
    private val predecessorLock = Any()
    private val mutableState = MutableStateFlow<TripState>(TripState.Idle)
    val state: StateFlow<TripState> = mutableState.asStateFlow()
    val summaries: Flow<List<TripSummary>> = repository.summaries
    private var initialized = false
    private var active: TripCheckpoint? = null
    private var candidate: TripCheckpoint? = null
    private val candidatePoints = mutableListOf<TripPoint>()
    private val pendingPoints = mutableListOf<TripPoint>()
    private var lastCheckpointAt: Long? = null
    private var lastAccepted: LocationSample? = null
    /** Monotonic arrival time of the last recorded fix; shares the clock with tick(), independent of RTC and GPS epoch. */
    private var lastArrivalAt: Long? = null
    /** Offset of GPS fix time over the system clock, from the latest accepted fix; 0 until one arrives. */
    @Volatile private var gpsOffsetMs = 0L
    /** False after process creation until the first accepted fix: the RTC may not share the stored trips' domain. */
    @Volatile private var gpsOffsetKnown = false
    /** Paired GPS epoch and monotonic arrival anchor for display dates; refreshed only by an accepted fix. */
    private var displayGpsEpochMs: Long? = null
    private var displayGpsElapsedMs: Long? = null
    private var updatesJob: Job? = null
    private var timerJob: Job? = null
    @Volatile private var isClosing = false
    @Volatile private var shutdownJob: Job? = null
    @Volatile var isClosed: Boolean = false
        private set
    internal val isClosingOrClosed: Boolean get() = isClosing || isClosed

    suspend fun startMonitoring() = mutex.withLock {
        check(!isClosingOrClosed)
        recoverLocked()
        // close() may run while recovery is suspended; never launch jobs for a closed recorder.
        if (isClosingOrClosed || updatesJob?.isActive == true) return@withLock
        updatesJob = scope.launch {
            try {
                locationProvider.updates(LocationRequest()).collect { sample ->
                    try {
                        accept(sample)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        AppLog.w(Tag, "Trip location update failed", error)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                AppLog.w(Tag, "Trip location stream failed", error)
            } finally {
                mutex.withLock {
                    active?.takeIf { it.pausedAt == null }?.let {
                        mutableState.value = TripState.Paused(it.trip, PauseReason.LocationUnavailable)
                    }
                    resetCandidate()
                }
            }
        }
        if (timerJob?.isActive != true) timerJob = scope.launch {
            while (isActive) {
                delay(1_000)
                try {
                    tick()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    AppLog.w(Tag, "Trip idle timer failed", error)
                }
            }
        }
        if (isClosingOrClosed) { updatesJob?.cancel(); timerJob?.cancel() }
    }

    suspend fun start() = mutex.withLock {
        check(!isClosingOrClosed)
        recoverLocked()
        if (isClosed) return@withLock
        if (active != null) return@withLock
        resetCandidate()
        lastAccepted = null
        lastArrivalAt = elapsedRealtime()
        val value = TripCheckpoint(Trip(newId(), nowMillis()), null, null, null, 0, null, null, lastObservedAt = 0,
            awaitingFix = true)
        checkpointLocked(value, durable = true)
        if (isClosed) return@withLock
        active = value
        lastCheckpointAt = null
        pendingPoints.clear()
        publish(value, 0.0)
    }

    suspend fun stop() = mutex.withLock {
        check(!isClosingOrClosed)
        recoverLocked()
        if (isClosed) return@withLock
        active?.let { finishLocked(stopTime(it)) }
        resetCandidate()
    }

    suspend fun pause() = mutex.withLock {
        check(!isClosingOrClosed)
        recoverLocked()
        if (isClosed) return@withLock
        val value = active ?: return@withLock
        if (value.pausedAt != null) return@withLock
        val settled = settleHeldInterval(value)
        val endpoint = endpoint(settled)
        val paused = settled.copy(pausedAt = nowMillis(), lastTrackPoint = endpoint ?: value.lastTrackPoint)
        checkpointLocked(paused, listOfNotNull(endpoint), durable = true)
        if (isClosed) return@withLock
        active = paused
        publish(paused, 0.0)
    }

    suspend fun resume() = mutex.withLock {
        check(!isClosingOrClosed)
        recoverLocked()
        if (isClosed) return@withLock
        val value = active ?: return@withLock
        if (value.pausedAt == null) return@withLock
        // Keep the prior observation: it is the finalization fallback until the new segment's first fix.
        // The terminal-idle run ends here: its booked idle is already in the trip total and must not be trimmed later.
        val resumed = value.copy(lastSample = null, idleSince = null, idleBookedMs = 0, pausedAt = null,
            segment = value.segment + 1, lastTrackPoint = null, lastTrackBearing = null, awaitingFix = true)
        checkpointLocked(resumed, durable = true)
        if (isClosed) return@withLock
        active = resumed
        lastAccepted = null
        lastArrivalAt = elapsedRealtime()
        publish(resumed, 0.0)
    }

    /** Include the live endpoint even when it has not crossed the decimation threshold. */
    suspend fun getTrip(id: String): TripDetails? = mutex.withLock {
        val details = try {
            repository.getTrip(id)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            AppLog.w(Tag, "Trip details unavailable; returning buffered recording", error)
            null
        }
        withLiveTrack(id, details)
    }

    /** Strict selected/live read. Unlike [getTrip], it reports storage failure and keeps the live fallback explicit. */
    internal suspend fun readTripResult(id: String? = null): TripReadResult {
        val selected = mutex.withLock {
            if (isClosingOrClosed) return@withLock null
            val live = active
            val selectedId = id ?: live?.trip?.id
            if (selectedId == null || live?.trip?.id != selectedId) return@withLock repository.readSelection(selectedId)

            val liveState = (mutableState.value as? TripState.Recording)?.takeIf { it.trip.id == selectedId }
                ?: (mutableState.value as? TripState.Paused)?.takeIf { it.trip.id == selectedId }
                ?: if (live.pausedAt != null) TripState.Paused(live.trip, PauseReason.Manual) else TripState.Recording(live.trip, 0.0)
            val buffered = TripDetails(live.trip,
                (listOfNotNull(live.lastTrackPoint) + pendingPoints + listOfNotNull(endpoint(live)))
                    .distinctBy { it.timestampMillis }.sortedBy { it.timestampMillis })
            val stored = try {
                repository.getTrip(selectedId)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                AppLog.w(Tag, "Trip read failed; returning buffered live track", error)
                null
            }
            if (isClosingOrClosed) return@withLock null
            if (stored == null) return@withLock TripReadResult.Partial(buffered, liveState)
            val details = TripDetails(live.trip,
                (stored.points + pendingPoints + listOfNotNull(endpoint(live))).distinctBy { it.timestampMillis }.sortedBy { it.timestampMillis })
            TripReadResult.Complete(details, liveState)
        }
        if (selected != null) return selected
        if (isClosing) awaitClosed()
        return repository.readSelection(id)
    }

    /**
     * Archives and replaces the expected active trip under the recorder mutex. The checkpoint of the new trip is
     * durable before it is published; an archive failure leaves all original state and buffered fixes untouched.
     */
    internal suspend fun resetTrip(expectedTripId: String): TripResetResult = mutex.withLock {
        if (isClosingOrClosed) return@withLock TripResetResult.Unavailable
        try {
            recoverLocked()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            AppLog.w(Tag, "Trip reset could not recover recorder state", error)
            return@withLock TripResetResult.Unavailable
        }
        // close() can set its flag while recovery is waiting on storage.
        if (isClosingOrClosed) return@withLock TripResetResult.Unavailable
        val current = active ?: return@withLock TripResetResult.NoActiveTrip
        if (current.trip.id != expectedTripId) return@withLock TripResetResult.StaleRequest(current.trip.id)

        val preserveManualPause = current.pausedAt != null
        val archivedId = current.trip.id
        try {
            // pausedAt is RTC-domain UI state; stopTime projects the request into the GPS observation domain.
            finishLocked(stopTime(current))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            AppLog.w(Tag, "Trip reset could not archive the current trip", error)
            return@withLock TripResetResult.ArchiveFailed(archivedId)
        }
        if (isClosingOrClosed) {
            return@withLock TripResetResult.ArchivedWithoutReplacement(archivedId, TripResetResult.ReplacementFailure.RecorderClosing)
        }

        val startedAt = when {
            current.lastObservedAt > 0L -> observedNow(current)
            gpsOffsetKnown -> nowMillis() + gpsOffsetMs
            else -> nowMillis()
        }
        val replacement = TripCheckpoint(
            trip = Trip(newId(), startedAt), lastSample = null, idleSince = null,
            pausedAt = if (preserveManualPause) startedAt else null, segment = 0,
            lastTrackPoint = null, lastTrackBearing = null, lastObservedAt = 0, awaitingFix = true,
        )
        try {
            repository.checkpoint(replacement)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            AppLog.w(Tag, "Trip reset archived the current trip but could not checkpoint its replacement", error)
            active = null
            pendingPoints.clear()
            lastCheckpointAt = null
            lastAccepted = null
            lastArrivalAt = null
            resetCandidate()
            mutableState.value = TripState.Idle
            return@withLock TripResetResult.ArchivedWithoutReplacement(archivedId, TripResetResult.ReplacementFailure.Storage)
        }
        if (isClosingOrClosed) {
            return@withLock TripResetResult.ArchivedWithoutReplacement(archivedId, TripResetResult.ReplacementFailure.RecorderClosing)
        }
        active = replacement
        pendingPoints.clear()
        lastCheckpointAt = null
        lastAccepted = null
        lastArrivalAt = elapsedRealtime()
        resetCandidate()
        publish(replacement, 0.0)
        TripResetResult.Reset(archivedId, replacement.trip.id)
    }

    private fun withLiveTrack(id: String, details: TripDetails?): TripDetails? {
        val live = active?.takeIf { it.trip.id == id }
        return if (live == null) details else TripDetails(live.trip,
            (details?.points.orEmpty() + pendingPoints + listOfNotNull(endpoint(live)))
                .distinctBy { it.timestampMillis }.sortedBy { it.timestampMillis })
    }

    suspend fun deleteTrip(id: String) = mutex.withLock {
        check(!isClosingOrClosed)
        recoverLocked()
        if (isClosed) return@withLock
        repository.deleteTrip(id)
        if (active?.trip?.id == id) {
            active = null; pendingPoints.clear(); lastCheckpointAt = null
            mutableState.value = TripState.Idle
        }
        if (candidate?.trip?.id == id) resetCandidate()
    }

    suspend fun clearAll() = mutex.withLock {
        check(!isClosingOrClosed)
        recoverLocked()
        if (isClosed) return@withLock
        repository.clearAll()
        active = null
        pendingPoints.clear()
        lastCheckpointAt = null
        resetCandidate()
        mutableState.value = TripState.Idle
    }

    /** Strict read: a storage failure propagates instead of exporting a partial (buffered-only) track. */
    suspend fun exportGpx(id: String, writer: Writer): Boolean {
        val details = mutex.withLock { withLiveTrack(id, repository.getTrip(id)) } ?: return false
        TripGpx.write(details, writer)
        return true
    }

    /** Recomputes at midnight and during live recording, even when no trip list write occurs. */
    fun totals(zone: ZoneId = ZoneId.systemDefault()): Flow<TripPeriodTotals> = combine(
        summaries,
        flow { while (true) { emit(nowMillis()); delay(30_000) } },
    ) { values, _ -> TripStats.totals(values, periodNow(values), zone) }

    /**
     * Now in the stored trips' (GPS) domain. Until a fix reveals the RTC offset (e.g. after process recreation)
     * the RTC projection is trusted, unless it is behind the newest stored trip (a trip would lie in the future);
     * then the newest trip time is used. An RTC running ahead before the first fix is accepted: head units with
     * a wrong RTC usually reset to a past date, and the offset corrects it on the first fix.
     */
    private fun periodNow(values: List<TripSummary>): Long {
        val projected = nowMillis() + gpsOffsetMs
        if (gpsOffsetKnown) return projected
        val newest = values.maxOfOrNull { it.trip.endedAt ?: it.trip.startedAt } ?: return projected
        return maxOf(projected, newest)
    }

    /** A read-only reference date in the GPS domain, without recovery or storage I/O. */
    internal suspend fun referenceNow(
        savedSummaries: List<TripSummary>,
        fallbackNowMillis: Long = nowMillis(),
    ): Long = mutex.withLock {
        val gpsEpoch = displayGpsEpochMs
        val gpsElapsed = displayGpsElapsedMs
        if (gpsEpoch != null && gpsElapsed != null) {
            val elapsedSinceFix = (elapsedRealtime() - gpsElapsed).coerceAtLeast(0L)
            return@withLock if (elapsedSinceFix > 0L && gpsEpoch > Long.MAX_VALUE - elapsedSinceFix) {
                Long.MAX_VALUE
            } else {
                gpsEpoch + elapsedSinceFix
            }
        }
        maxOf(fallbackNowMillis, savedSummaries.maxOfOrNull { it.trip.endedAt ?: it.trip.startedAt } ?: Long.MIN_VALUE)
    }

    /** Keeps the durable checkpoint; a later owner may recover it. Does not cancel caller's scope. */
    fun close() {
        synchronized(shutdownLock) {
            if (isClosingOrClosed) return
            isClosed = true
            updatesJob?.cancel()
            timerJob?.cancel()
        }
    }

    /** Begin one final durable checkpoint. The registered barrier precedes any possible successor. */
    fun closeAndCheckpoint() {
        synchronized(shutdownLock) {
            if (isClosingOrClosed) return
            isClosing = true

            shutdownJob = scope.launch(start = CoroutineStart.ATOMIC) {
                withContext(NonCancellable) {
                    try {
                        mutex.withLock {
                            isClosed = true
                            updatesJob?.cancel()
                            timerJob?.cancel()
                        }
                        awaitPredecessorSettled()
                        mutex.withLock { checkpointForShutdownLocked() }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        AppLog.e(Tag, "Final trip checkpoint failed during shutdown", error)
                    }
                }
            }
        }
    }

    /** Wait until registered shutdown writes and every predecessor write have settled. */
    internal suspend fun awaitClosed() {
        check(isClosingOrClosed)
        awaitSettled()
    }

    /**
     * Returns once this recorder's registered final shutdown write and all predecessor writes have
     * settled. The shutdown lock pairs registration with this snapshot; joining happens outside it.
     * Settled predecessor links are dropped.
     */
    private suspend fun awaitSettled() {
        awaitPredecessorSettled()
        val registeredShutdown = synchronized(shutdownLock) { shutdownJob }
        registeredShutdown?.join()
        mutex.withLock {}
    }

    private suspend fun awaitPredecessorSettled() {
        val previous = synchronized(predecessorLock) { predecessor } ?: return
        previous.awaitSettled()
        synchronized(predecessorLock) {
            if (predecessor === previous) predecessor = null
        }
    }

    private suspend fun checkpointForShutdownLocked() {
        val value = active ?: return
        val terminal = endpoint(value)?.takeUnless { point ->
            pendingPoints.any { it.timestampMillis == point.timestampMillis }
        }
        checkpointLocked(value, listOfNotNull(terminal), durable = true, allowClosedForShutdown = true)
    }

    // TODO: integrate vehicle/ignition data in a later PR; no shared OBD contract here.

    internal suspend fun accept(sample: LocationSample) = mutex.withLock {
        if (isClosingOrClosed) return@withLock
        recoverLocked()
        if (isClosed) return@withLock
        val previous = lastAccepted
        val observed = active?.lastObservedAt ?: 0L
        // Fix times must be newer than the durable observation, even when recovery or resume cleared the anchor.
        if (!valid(sample, previous) || (observed > 0L && sample.timestampMillis <= observed)) {
            resetCandidate(); return@withLock
        }
        gpsOffsetMs = sample.timestampMillis - nowMillis()
        gpsOffsetKnown = true
        displayGpsEpochMs = sample.timestampMillis
        displayGpsElapsedMs = elapsedRealtime()
        val gap = previous?.let { sample.timestampMillis - it.timestampMillis }
        val motionAnchor = active?.lastSample ?: candidate?.lastSample ?: previous
        val displacement = motionAnchor?.let { distance(it, sample) } ?: 0.0
        val speed = speed(sample, motionAnchor, displacement)
        lastAccepted = sample

        var current = active
        // Paused fixes only refresh the clock offset; they are not observations of the trip.
        if (current?.pausedAt != null) return@withLock
        val resumedWithoutFix = current?.awaitingFix == true
        if (resumedWithoutFix) current = current!!.copy(awaitingFix = false)
        if (current != null && current.lastObservedAt > 0L && !resumedWithoutFix) {
            val lastTime = current.lastObservedAt
            val idleDeadline = current.idleSince?.plus(config.idleTimeoutMs)
            if ((idleDeadline != null && sample.timestampMillis >= idleDeadline) ||
                sample.timestampMillis - lastTime >= config.idleTimeoutMs) {
                finishLocked(if (idleDeadline != null) current.idleSince!! else lastTime,
                    trimTerminalIdle = idleDeadline != null)
                // Preserve the active trip if a failed database finish needs retrying.
                if (active != null) return@withLock
                current = null
            }
        }
        // Moved only after a finish succeeded: a failed finish keeps the old arrival baseline, so tick()
        // retries it promptly instead of waiting another full idle timeout.
        lastArrivalAt = elapsedRealtime()
        // Manual start may precede the first GPS fix while the head unit RTC is wrong.
        // lastObservedAt stays 0 until the first fix, whatever pauses or resumes came before it.
        if (current != null && current.lastObservedAt == 0L) {
            current = current.copy(trip = current.trip.copy(startedAt = sample.timestampMillis))
        }
        if (current == null) {
            if (speed <= config.autoStartSpeedMps) { resetCandidate(); return@withLock }
            if (gap != null && gap > config.maxSampleGapMs) resetCandidate()
            val draft = candidate
            val initial = draft ?: TripCheckpoint(Trip(newId(), sample.timestampMillis), null, null, null, 0, null, null)
            val advanced = advance(initial, sample, speed)
            candidate = advanced.first
            candidatePoints += advanced.second
            if (advanced.first.trip.movingMs >= config.autoStartDurationMs) {
                pendingPoints += candidatePoints
                checkpointLocked(advanced.first)
                if (isClosed) return@withLock
                active = advanced.first
                publish(advanced.first, speed)
                resetCandidate()
            }
            return@withLock
        }
        val advanced = advance(current, sample, speed)
        pendingPoints += advanced.second
        // Kept points stay buffered; they reach storage with the next interval or lifecycle checkpoint.
        if (lastCheckpointAt == null || sample.timestampMillis - lastCheckpointAt!! >= CheckpointIntervalMs) {
            checkpointLocked(advanced.first)
        }
        if (isClosed) return@withLock
        active = advanced.first
        publish(advanced.first, speed)
    }

    internal suspend fun tick() = mutex.withLock {
        if (isClosingOrClosed) return@withLock
        recoverLocked()
        if (isClosed) return@withLock
        val now = elapsedRealtime()
        // Candidates exist only while idle (no active trip), so expire them before the no-trip return.
        if (candidate != null && lastArrivalAt?.let { now - it > config.maxSampleGapMs } == true) resetCandidate()
        val value = active ?: return@withLock
        if (value.pausedAt != null) return@withLock
        val last = value.lastObservedAt
        val idleDeadline = value.idleSince?.plus(config.idleTimeoutMs)
        val elapsed = lastArrivalAt?.let { now - it } ?: 0L
        if (last == 0L || value.awaitingFix) {
            // Manual start/resume with no fix yet: wait for GPS instead of closing an empty trip.
            if (elapsed > config.maxSampleGapMs) mutableState.value = TripState.Paused(value.trip, PauseReason.LocationUnavailable)
            return@withLock
        }
        // While this process is alive use arrival elapsed time, not GPS-vs-RTC absolute age.
        if (idleDeadline != null && elapsed >= idleDeadline - last) finishLocked(value.idleSince!!, trimTerminalIdle = true)
        else if (elapsed >= config.idleTimeoutMs) finishLocked(last)
        else if (elapsed > config.maxSampleGapMs) {
            mutableState.value = TripState.Paused(value.trip, PauseReason.LocationUnavailable)
        }
    }

    /**
     * Retried on every call until the checkpoint read succeeds, so recording never starts over an unread trip.
     * Recovery never judges staleness: the system clock and GPS fix times may be in different domains. The
     * resumed trip is closed by the live path instead, by the next fix (GPS time) or the arrival timer.
     */
    private suspend fun recoverLocked() {
        if (initialized) return
        awaitPredecessorSettled()
        val saved = repository.activeCheckpoint()
        if (isClosed) return
        if (saved != null) {
            // Always break the track across process death; the gap has no observed movement.
            val keep = saved.pausedAt != null || saved.lastSample == null
            val settled = if (keep) saved else settleHeldInterval(saved)
            val resumed = if (keep) saved else settled.copy(
                lastSample = null, segment = saved.segment + 1, lastTrackPoint = null, lastTrackBearing = null)
            repository.checkpoint(resumed, listOfNotNull(endpoint(settled)))
            // close() may run while the write is suspended; a closed recorder never mutates or publishes.
            if (isClosed) return
            active = resumed
            lastCheckpointAt = resumed.lastObservedAt.takeIf { it > 0 }
            lastArrivalAt = elapsedRealtime()
            publish(resumed, 0.0)
        }
        initialized = true
    }

    /**
     * Fixes held inside the jitter radius advance lastObservedAt but keep the anchor, so that interval is only
     * booked by the next moving fix. Book it as idle before the anchor is dropped (pause, restart).
     */
    private fun settleHeldInterval(value: TripCheckpoint): TripCheckpoint {
        val anchor = value.lastSample ?: return value
        val held = value.lastObservedAt - anchor.timestampMillis
        if (value.idleSince == null || held <= 0) return value
        return value.copy(trip = value.trip.copy(idleMs = value.trip.idleMs + held),
            idleBookedMs = value.idleBookedMs + held,
            lastSample = anchor.copy(timestampMillis = value.lastObservedAt))
    }

    private fun advance(value: TripCheckpoint, sample: LocationSample, speed: Double): Pair<TripCheckpoint, List<TripPoint>> {
        val previous = value.lastSample
        val dt = previous?.let { sample.timestampMillis - it.timestampMillis } ?: 0
        val continuous = previous != null && sample.timestampMillis - value.lastObservedAt <= config.maxSampleGapMs
        val moving = speed > config.movingSpeedMps
        val displacement = previous?.let { distance(it, sample) } ?: 0.0
        val jitterM = maxOf(3.0, 3.0 * maxOf(previous?.accuracyMeters ?: 0f, sample.accuracyMeters ?: 0f))
        if (continuous && displacement < jitterM && (sample.speedMps == null || moving)) {
            // Keep the position/time anchor until movement clears its uncertainty radius. A real
            // slow drive then commits the full interval; oscillating fixes never accumulate it.
            return value.copy(lastObservedAt = sample.timestampMillis,
                idleSince = value.idleSince ?: previous!!.timestampMillis) to emptyList()
        }
        val meters = if (continuous && moving) displacement else 0.0
        // Time the anchor was held inside the jitter radius counts as moving only when uniform motion at the
        // interval's average speed would itself have stayed inside that radius for the held time (a slow real
        // drive). Otherwise the held fixes never established motion and only the tail since the last held
        // observation is moving, so a parked spell with a false reported speed ending in one real move cannot
        // book the whole interval (or start a trip early).
        val heldMs = if (continuous && moving) (value.lastObservedAt - previous!!.timestampMillis).coerceIn(0, dt) else 0
        val sustained = dt > 0 && displacement * 1_000 / dt > config.movingSpeedMps &&
            heldMs * displacement <= jitterM * dt
        val movingMs = if (continuous && moving) (if (sustained) dt else dt - heldMs) else 0
        val trip = value.trip.copy(
            distanceM = value.trip.distanceM + meters,
            movingMs = value.trip.movingMs + movingMs,
            idleMs = value.trip.idleMs + if (continuous) dt - movingMs else 0,
            maxSpeedMps = maxOf(value.trip.maxSpeedMps, speed),
            startLatitude = value.trip.startLatitude ?: sample.latitude,
            startLongitude = value.trip.startLongitude ?: sample.longitude,
            endLatitude = sample.latitude, endLongitude = sample.longitude,
        )
        val segment = value.segment + if (previous != null && !continuous) 1 else 0
        val bearing = sample.bearingDegrees?.toDouble() ?: previous?.takeIf { meters >= 3 }?.let {
            TripGeometry.bearingDegrees(it.latitude, it.longitude, sample.latitude, sample.longitude)
        }
        val lastPoint = value.lastTrackPoint
        val keep = lastPoint == null || !continuous || moving != (value.idleSince == null) || (moving && TripGeometry.shouldKeepPoint(
            lastPoint.latitude, lastPoint.longitude, value.lastTrackBearing, sample.latitude,
            sample.longitude, bearing, config.trackDistanceM, config.trackAngleDegrees,
        ))
        val point = if (keep) TripPoint(trip.id, sample.timestampMillis, sample.latitude,
            sample.longitude, speed, sample.altitudeMeters?.takeIf { it.isFinite() }, segment) else null
        // Keep the derived speed so synthesized endpoints never carry a false zero.
        val stored = if (sample.speedMps == null) sample.copy(speedMps = speed.toFloat()) else sample
        val updated = value.copy(trip = trip, lastSample = stored,
            idleSince = if (moving) null else value.idleSince ?:
                if (continuous) previous!!.timestampMillis else sample.timestampMillis,
            segment = segment, lastTrackPoint = point ?: lastPoint,
            lastTrackBearing = if (point != null) bearing else value.lastTrackBearing,
            idleBookedMs = if (moving) 0 else value.idleBookedMs + if (continuous) dt - movingMs else 0,
            lastObservedAt = sample.timestampMillis)
        val beforeGap = if (previous != null && !continuous) endpoint(value) else null
        return updated to listOfNotNull(beforeGap, point)
    }

    private suspend fun finishLocked(at: Long, trimTerminalIdle: Boolean = false) {
        val value = active ?: return
        val completed = completedTrip(value, at, trimTerminalIdle)
        repository.finish(completed, pendingPoints + listOfNotNull(endpoint(value)))
        active = null
        pendingPoints.clear()
        lastCheckpointAt = null
        mutableState.value = TripState.Idle
    }

    /**
     * A legacy-closed recorder never writes. Its sole exception is the registered final shutdown checkpoint,
     * which runs after predecessor settlement under the shutdown mutex. Interval checkpoints are lenient
     * (keep recording, buffer points); durable manual transitions propagate failures without applying state.
     */
    private suspend fun checkpointLocked(value: TripCheckpoint, extraPoints: List<TripPoint> = emptyList(),
        durable: Boolean = false, allowClosedForShutdown: Boolean = false) {
        if (isClosed && !allowClosedForShutdown) return
        try {
            repository.checkpoint(value, pendingPoints + extraPoints)
            pendingPoints.clear()
            lastCheckpointAt = value.lastObservedAt.takeIf { it > 0 }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (durable) throw error
            pendingPoints += extraPoints
            // Count the attempt: otherwise every later fix retries the failing transaction under the mutex.
            lastCheckpointAt = value.lastObservedAt.takeIf { it > 0 }
            AppLog.w(Tag, "Trip checkpoint failed; retaining buffered fixes", error)
        }
    }

    private fun completedTrip(value: TripCheckpoint, at: Long, trimTerminalIdle: Boolean): Trip {
        val end = at.coerceAtLeast(value.trip.startedAt)
        val extraIdle = if (!trimTerminalIdle && value.idleSince != null && value.pausedAt == null) {
            (end - (value.lastSample?.timestampMillis ?: end)).coerceIn(0, config.idleTimeoutMs)
        } else 0
        val trailingIdle = if (trimTerminalIdle && value.idleSince != null) {
            // Only the booked part: a held anchor's interval was never added to idleMs, and the anchor
            // is gone after recovery, so the booked amount is tracked on its own.
            value.idleBookedMs
        } else 0
        return value.trip.copy(endedAt = end,
            idleMs = (value.trip.idleMs + extraIdle - trailingIdle).coerceAtLeast(0))
    }

    private fun endpoint(value: TripCheckpoint): TripPoint? = value.lastSample?.let { sample ->
        if (sample.timestampMillis == value.lastTrackPoint?.timestampMillis) null
        else TripPoint(value.trip.id, sample.timestampMillis, sample.latitude, sample.longitude,
            sample.speedMps?.toDouble() ?: 0.0, sample.altitudeMeters?.takeIf { it.isFinite() }, value.segment)
    }

    private fun valid(sample: LocationSample, previous: LocationSample?): Boolean {
        if (!sample.latitude.isFinite() || sample.latitude !in -90.0..90.0 ||
            !sample.longitude.isFinite() || sample.longitude !in -180.0..180.0) return false
        val accuracy = sample.accuracyMeters ?: return false
        if (!accuracy.isFinite() || accuracy < 0 || accuracy > config.maxAccuracyM) return false
        if (sample.speedMps?.let { !it.isFinite() || it < 0 || it > config.maxSpeedMps } == true) return false
        if (sample.bearingDegrees?.let { !it.isFinite() || it < 0 || it >= 360 } == true) return false
        if (sample.timestampMillis < 0) return false
        if (previous != null) {
            val dt = sample.timestampMillis - previous.timestampMillis
            if (dt <= 0 || distance(previous, sample) * 1_000 / dt > config.maxSpeedMps) return false
        }
        return true
    }

    private fun speed(sample: LocationSample, previous: LocationSample?, meters: Double): Double {
        sample.speedMps?.let { return it.toDouble() }
        previous ?: return 0.0
        val jitterM = maxOf(3.0, sample.accuracyMeters?.toDouble() ?: 0.0,
            previous.accuracyMeters?.toDouble() ?: 0.0)
        return if (meters > jitterM) meters * 1_000 / (sample.timestampMillis - previous.timestampMillis) else 0.0
    }

    /** Unobserved time never counts: with location unavailable (no fix within the gap limit) stop at the last observation. */
    private fun stopTime(value: TripCheckpoint): Long {
        val unavailable = value.pausedAt == null && lastArrivalAt?.let { elapsedRealtime() - it > config.maxSampleGapMs } == true
        return if (unavailable) value.lastObservedAt.takeIf { it > 0 } ?: value.trip.startedAt else observedNow(value)
    }

    /** Now in the GPS fix domain: the last observation plus arrival time elapsed since it. */
    private fun observedNow(value: TripCheckpoint): Long {
        val since = (elapsedRealtime() - (lastArrivalAt ?: elapsedRealtime())).coerceAtLeast(0)
        return (value.lastObservedAt.takeIf { it > 0 } ?: value.trip.startedAt) + since
    }

    private fun distance(a: LocationSample, b: LocationSample) = TripGeometry.distanceM(a.latitude, a.longitude, b.latitude, b.longitude)
    private fun resetCandidate() { candidate = null; candidatePoints.clear() }
    private fun publish(value: TripCheckpoint, speed: Double) {
        mutableState.value = if (value.pausedAt != null) TripState.Paused(value.trip, PauseReason.Manual)
        else TripState.Recording(value.trip, speed)
    }
}
