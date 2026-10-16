package org.wpfy.carlauncher.feature.trip

import android.content.Context

object TripRepositoryFactory {
    const val persistsTrips = false

    private val repository = InMemoryTripRepository()

    @Suppress("UNUSED_PARAMETER")
    fun create(context: Context): TripRepository = repository

    @Suppress("UNUSED_PARAMETER")
    fun startRecordingService(context: Context) = Unit

    @Suppress("UNUSED_PARAMETER")
    fun stopRecordingService(context: Context) = Unit
}
