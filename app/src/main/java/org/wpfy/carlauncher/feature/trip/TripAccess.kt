package org.wpfy.carlauncher.feature.trip

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/** Strict repository selection used by the process owner and by reads when no recorder owner exists. */
internal suspend fun TripRepository.readSelection(id: String? = null): TripReadResult {
    return try {
        val selectedId = id ?: summaries.first().firstOrNull()?.trip?.id
        if (selectedId == null) TripReadResult.Empty
        else getTrip(selectedId)?.let { TripReadResult.Complete(it) } ?: TripReadResult.Empty
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        TripReadResult.Unavailable
    }
}
