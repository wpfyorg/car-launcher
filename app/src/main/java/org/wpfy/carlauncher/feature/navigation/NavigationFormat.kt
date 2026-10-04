package org.wpfy.carlauncher.feature.navigation

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.wpfy.carlauncher.data.settings.UnitSystem

/** Joins nonblank duration and distance labels with a middle dot, returning empty text if both are blank. */
internal fun durationAndDistance(duration: String, distance: String): String =
    listOf(duration, distance).filter { it.isNotBlank() }.joinToString(" · ")

/**
 * Formats meters in [units]: whole feet below 1,000 feet or miles to one decimal (Imperial), whole metres below
 * 1 km or kilometres to one decimal (Metric). Null or negative input yields empty text.
 */
internal fun formatDistance(meters: Double?, units: UnitSystem = UnitSystem.Imperial): String {
    if (meters == null || meters < 0.0) return ""
    if (units == UnitSystem.Metric) {
        return if (meters < 1_000.0) "${meters.toInt()} m" else String.format(Locale.US, "%.1f km", meters / 1_000.0)
    }
    val feet = meters * 3.28084
    return if (feet < 1_000.0) {
        "${feet.toInt()} ft"
    } else {
        String.format(Locale.US, "%.1f mi", meters / 1_609.344)
    }
}

/** Rounds seconds to the nearest minute and formats minutes or hours; null or negative input yields empty text. */
internal fun formatDuration(seconds: Long?): String {
    if (seconds == null || seconds < 0L) return ""
    val minutes = (seconds + 30L) / 60L
    return if (minutes < 60L) {
        "$minutes min"
    } else {
        val hours = minutes / 60L
        val remainder = minutes % 60L
        if (remainder == 0L) "$hours hr" else "$hours hr $remainder min"
    }
}

private val EtaFormatter = DateTimeFormatter.ofPattern("h:mm a")

/** Formats a positive epoch timestamp as a lowercase 12-hour clock in [zone]; null or nonpositive input yields null. */
internal fun formatEta(epochMillis: Long?, zone: ZoneId = ZoneId.systemDefault()): String? {
    if (epochMillis == null || epochMillis <= 0L) return null
    return Instant.ofEpochMilli(epochMillis)
        .atZone(zone)
        .format(EtaFormatter)
        .lowercase(Locale.getDefault())
}
