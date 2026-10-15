package org.wpfy.carlauncher.feature.trip

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface TripRepository {
    /** Newest first; includes the active trip. Does not load track points. */
    val summaries: Flow<List<TripSummary>>
    suspend fun getTrip(id: String): TripDetails?
    suspend fun activeCheckpoint(): TripCheckpoint?
    /** Trip, appended track points and active checkpoint must be committed atomically. */
    suspend fun checkpoint(value: TripCheckpoint, points: List<TripPoint> = emptyList())
    /** Final trip and endpoint are committed atomically with checkpoint removal; points after endedAt are dropped. */
    suspend fun finish(trip: Trip, points: List<TripPoint> = emptyList())
    suspend fun deleteTrip(id: String)
    suspend fun clearAll()
}

/** Free flavors deliberately retain trips for this process only. No Room dependency. */
class InMemoryTripRepository : TripRepository {
    private val mutex = Mutex()
    private val trips = MutableStateFlow<Map<String, TripDetails>>(emptyMap())
    private var active: TripCheckpoint? = null
    override val summaries: Flow<List<TripSummary>> = trips.map { values ->
        values.values.map { TripSummary(it.trip) }
            .sortedWith(compareByDescending<TripSummary> { it.trip.startedAt }.thenBy { it.trip.id })
    }

    override suspend fun getTrip(id: String): TripDetails? = mutex.withLock { trips.value[id] }
    override suspend fun activeCheckpoint(): TripCheckpoint? = mutex.withLock { active }
    override suspend fun checkpoint(value: TripCheckpoint, points: List<TripPoint>) = mutex.withLock {
        save(value.trip, points)
        active = value
    }

    override suspend fun finish(trip: Trip, points: List<TripPoint>) = mutex.withLock {
        save(trip, points)
        trips.value[trip.id]?.let { details ->
            trips.value = trips.value + (trip.id to details.copy(points = details.points.filter { trip.endedAt == null || it.timestampMillis <= trip.endedAt }))
        }
        if (active?.trip?.id == trip.id) active = null
    }

    override suspend fun deleteTrip(id: String) = mutex.withLock {
        trips.value = trips.value - id
        if (active?.trip?.id == id) active = null
    }

    override suspend fun clearAll() = mutex.withLock {
        trips.value = emptyMap()
        active = null
    }

    private fun save(trip: Trip, points: List<TripPoint>) {
        require(points.all { it.tripId == trip.id })
        val track = (trips.value[trip.id]?.points.orEmpty() + points)
            .distinctBy { it.timestampMillis }.sortedBy { it.timestampMillis }
        trips.value = trips.value + (trip.id to TripDetails(trip, track))
    }
}
