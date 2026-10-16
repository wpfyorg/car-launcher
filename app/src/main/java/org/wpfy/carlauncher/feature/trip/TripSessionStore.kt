package org.wpfy.carlauncher.feature.trip

import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wpfy.carlauncher.core.diagnostics.AppLog
import org.wpfy.carlauncher.location.FrameworkLocationProvider

/** Process owner. MainActivity owns permission checks and lifecycle entry points; no shell or service. */
object TripSessionStore {
    private var recorder: TripRecorder? = null
    private var scope: CoroutineScope? = null
    private val mutableState = MutableStateFlow<TripState>(TripState.Idle)
    private val mutableResetStatus = MutableStateFlow<TripResetStatus?>(null)
    private var pendingReset: PendingReset? = null

    /** Stable process-level state. Collecting it never creates a recorder or requests location. */
    val state: StateFlow<TripState> = mutableState.asStateFlow()

    /** Latest Reset request, retained for screens recreated while the process-owned command is running. */
    val resetStatus: StateFlow<TripResetStatus?> = mutableResetStatus.asStateFlow()

    /** Reads saved history or the live owner without creating a recorder, location provider or subscription. */
    suspend fun readTrip(context: Context, id: String? = null): TripReadResult {
        val existing = synchronized(this) { recorder }
        if (existing != null) return existing.readTripResult(id)
        return try {
            TripRepositoryFactory.create(context.applicationContext).readSelection(id)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            AppLog.w(Tag, "Trip read unavailable", error)
            TripReadResult.Unavailable
        }
    }

    /** Read-only display clock; this never creates an owner, provider, subscription, or checkpoint. */
    internal suspend fun referenceNow(
        savedSummaries: List<TripSummary>,
        fallbackNowMillis: Long = System.currentTimeMillis(),
    ): Long {
        val existing = synchronized(this) { recorder }
        if (existing != null) return existing.referenceNow(savedSummaries, fallbackNowMillis)
        return maxOf(fallbackNowMillis, savedSummaries.maxOfOrNull { it.trip.endedAt ?: it.trip.startedAt } ?: Long.MIN_VALUE)
    }

    /** Capability query only; it never creates or starts an owner. */
    @Synchronized
    internal fun canResetExistingTrip(id: String): Boolean {
        val current = recorder ?: return false
        val activeId = when (val state = mutableState.value) {
            is TripState.Recording -> state.trip.id
            is TripState.Paused -> state.trip.id
            TripState.Idle -> null
        }
        return !current.isClosingOrClosed && scope != null && activeId == id
    }

    /** Runs in the process owner's scope; cancelling a screen awaiting the result cannot interrupt or replay Reset. */
    fun resetExistingTrip(expectedTripId: String): Deferred<TripResetResult> = synchronized(this) {
        pendingReset?.let { pending ->
            if (pending.expectedTripId == expectedTripId) return@synchronized pending.result
            return@synchronized CompletableDeferred(TripResetResult.Unavailable)
        }
        val current = recorder?.takeUnless { it.isClosingOrClosed }
        val ownerScope = if (current == null) null else scope
        if (current == null || ownerScope == null) {
            return@synchronized completedReset(expectedTripId, TripResetResult.Unavailable)
        }
        enqueueReset(current, ownerScope, expectedTripId)
    }

    /** Internal seam for the process-owner queue; the returned result is independent of the awaiting UI job. */
    internal fun scheduleReset(
        current: TripRecorder,
        ownerScope: CoroutineScope,
        expectedTripId: String,
    ): Deferred<TripResetResult> = synchronized(this) {
        pendingReset?.let { pending ->
            if (pending.expectedTripId == expectedTripId) return@synchronized pending.result
            return@synchronized CompletableDeferred(TripResetResult.Unavailable)
        }
        enqueueReset(current, ownerScope, expectedTripId)
    }

    private fun completedReset(expectedTripId: String, outcome: TripResetResult): Deferred<TripResetResult> {
        mutableResetStatus.value = TripResetStatus(expectedTripId, outcome)
        return CompletableDeferred(outcome)
    }

    /** Called with this process owner locked, so status and command registration are atomic. */
    private fun enqueueReset(
        current: TripRecorder,
        ownerScope: CoroutineScope,
        expectedTripId: String,
    ): Deferred<TripResetResult> {
        val result = CompletableDeferred<TripResetResult>()
        val pending = PendingReset(expectedTripId, result)
        pendingReset = pending
        mutableResetStatus.value = TripResetStatus(expectedTripId)
        ownerScope.launch(start = CoroutineStart.ATOMIC) {
            var outcome: TripResetResult = TripResetResult.Unavailable
            try {
                withContext(NonCancellable) {
                    outcome = try {
                        current.resetTrip(expectedTripId)
                    } catch (error: CancellationException) {
                        TripResetResult.Unavailable
                    } catch (error: Exception) {
                        AppLog.w(Tag, "Trip reset failed", error)
                        TripResetResult.Unavailable
                    }
                }
            } finally {
                synchronized(this@TripSessionStore) {
                    if (pendingReset === pending) {
                        pendingReset = null
                        mutableResetStatus.value = TripResetStatus(expectedTripId, outcome)
                    }
                }
                result.complete(outcome)
            }
        }
        return result
    }

    private data class PendingReset(
        val expectedTripId: String,
        val result: Deferred<TripResetResult>,
    )

    /** Idempotent. Caller checks product edition and location permission before starting. */
    fun startMonitoring(context: Context) {
        try {
            val current = getOrCreate(context)
            val ownerScope = synchronized(this) { scope } ?: return
            ownerScope.launch {
                try {
                    current.startMonitoring()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    AppLog.w(Tag, "Trip monitoring startup failed", error)
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            AppLog.w(Tag, "Trip monitoring startup failed", error)
        }
    }

    /** Close only when the owning activity is really finishing, not during configuration change. */
    @Synchronized
    fun close() {
        val current = recorder ?: return
        if (current.isClosingOrClosed) return
        current.closeAndCheckpoint()
        val ownerScope = scope ?: return
        mutableState.value = TripState.Idle
        ownerScope.launch {
            try {
                current.awaitClosed()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                AppLog.w(Tag, "Trip shutdown settling failed", error)
            } finally {
                synchronized(this@TripSessionStore) {
                    if (scope === ownerScope) scope = null
                }
                ownerScope.cancel()
            }
        }
    }

    @Synchronized
    fun getOrCreate(context: Context): TripRecorder {
        val previous = recorder
        if (previous != null && !previous.isClosingOrClosed) return previous
        val appContext = context.applicationContext
        val provider = FrameworkLocationProvider(appContext)
        val repository = TripRepositoryFactory.create(appContext)
        val ownerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default +
            CoroutineExceptionHandler { _, error ->
                AppLog.e(Tag, "Uncaught trip coroutine error", error)
            })
        val current = try {
            TripRecorder(provider, repository, ownerScope, predecessor = previous)
        } catch (error: Exception) {
            ownerScope.cancel()
            throw error
        }
        recorder = current
        scope = ownerScope
        mutableState.value = TripState.Idle
        ownerScope.launch {
            current.state.collect { value ->
                synchronized(this@TripSessionStore) {
                    if (recorder === current && !current.isClosingOrClosed) mutableState.value = value
                }
            }
        }
        return current
    }

    private const val Tag = "TripSessionStore"
}
