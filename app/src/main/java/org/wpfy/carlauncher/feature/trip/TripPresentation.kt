package org.wpfy.carlauncher.feature.trip

import org.wpfy.carlauncher.data.settings.UnitSystem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max

/** Pure presentation transforms. Callers pass the Trip-clock reference time and current display zone explicitly. */
enum class TripHistoryFilter { Week, Month, All }

data class TripHistoryRows(val trips: List<Trip>, val totals: TripTotals)
data class TripDateGroup(val date: LocalDate, val trips: List<Trip>)
data class TripRoutePoint(val x: Float, val y: Float)
data class TripRouteSegments(val segments: List<List<TripRoutePoint>>, val truncated: Boolean)
data class TripSpeedBin(val level: Float?, val recordedSpeedMps: Double?)
data class TripSpeedProfile(val bins: List<TripSpeedBin>, val truncated: Boolean)
data class TripClimbEstimate(val meters: Double?, val partial: Boolean)

sealed interface TripDetailSelection {
    data class Found(val trip: Trip, val liveState: TripState?) : TripDetailSelection
    data object Missing : TripDetailSelection
    data object Empty : TripDetailSelection
}

object TripPresentation {
    private const val MaxRouteInputPoints = 50_000
    private const val MaxRenderedRoutePoints = 2_000
    private const val MaxProfileInputPoints = 100_000

    /** A supplied selected ID is authoritative; a deleted selection never falls through to another trip. */
    fun selectDetail(selectedId: String?, liveState: TripState?, saved: List<Trip>): TripDetailSelection {
        val live = when (liveState) {
            is TripState.Recording -> liveState.trip
            is TripState.Paused -> liveState.trip
            TripState.Idle, null -> null
        }
        if (selectedId != null) {
            val trip = live?.takeIf { it.id == selectedId } ?: saved.firstOrNull { it.id == selectedId }
                ?: return TripDetailSelection.Missing
            return TripDetailSelection.Found(trip, liveState?.takeIf { trip.id == live?.id })
        }
        live?.let { return TripDetailSelection.Found(it, liveState) }
        saved.maxByOrNull { it.startedAt }?.let { return TripDetailSelection.Found(it, null) }
        return TripDetailSelection.Empty
    }

    /** Week/month are rolling local-date windows; All retains every repository row. */
    fun history(
        saved: List<TripSummary>, currentState: TripState?, filter: TripHistoryFilter,
        referenceNowMillis: Long, zone: ZoneId,
    ): TripHistoryRows {
        val byId = LinkedHashMap<String, Trip>()
        saved.forEach { byId[it.trip.id] = it.trip }
        val current = when (currentState) {
            is TripState.Recording -> currentState.trip
            is TripState.Paused -> currentState.trip
            TripState.Idle, null -> null
        }
        current?.let { byId[it.id] = it }
        val today = Instant.ofEpochMilli(referenceNowMillis).atZone(zone).toLocalDate()
        val firstDate = when (filter) {
            TripHistoryFilter.Week -> today.minusDays(6)
            TripHistoryFilter.Month -> today.minusDays(29)
            TripHistoryFilter.All -> null
        }
        val selected = byId.values.asSequence()
            .filter { trip ->
                if (firstDate == null) true
                else Instant.ofEpochMilli(trip.startedAt).atZone(zone).toLocalDate().let { date -> date >= firstDate && date <= today }
            }
            .sortedByDescending { it.startedAt }
            .toList()
        val total = TripTotals(
            tripCount = selected.size,
            distanceM = selected.sumOf { it.distanceM.takeIf { value -> value.isFinite() && value >= 0.0 } ?: 0.0 },
            movingMs = selected.sumOf { it.movingMs.coerceAtLeast(0) },
            idleMs = selected.sumOf { it.idleMs.coerceAtLeast(0) },
            maxSpeedMps = selected.maxOfOrNull { it.maxSpeedMps.takeIf { value -> value.isFinite() && value >= 0.0 } ?: 0.0 } ?: 0.0,
        )
        return TripHistoryRows(selected, total)
    }

    fun groupByLocalDate(trips: List<Trip>, zone: ZoneId): List<TripDateGroup> = trips
        .groupBy { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
        .toSortedMap(compareByDescending { it })
        .map { (date, values) -> TripDateGroup(date, values.sortedByDescending { it.startedAt }) }

    /** Compact stable labels; caller chooses localized strings for these semantic keys. */
    fun dateGroupKey(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "today"
        today.minusDays(1) -> "yesterday"
        else -> "date"
    }

    fun formatDistance(meters: Double, units: UnitSystem, locale: Locale = Locale.getDefault()): String {
        if (!meters.isFinite() || meters < 0.0) return "—"
        val value = if (units == UnitSystem.Imperial) meters / 1_609.344 else meters / 1_000.0
        return String.format(locale, "%.1f", value)
    }

    fun distanceUnit(units: UnitSystem): String = if (units == UnitSystem.Imperial) "mi" else "km"

    /** A blank/unavailable address uses a caller-localized neutral label, never raw coordinates. */
    fun endpointLabel(address: String?, neutralLabel: String): String = address?.trim()?.takeIf(String::isNotEmpty) ?: neutralLabel

    fun formatSpeed(metersPerSecond: Double, units: UnitSystem, locale: Locale = Locale.getDefault()): String {
        if (!metersPerSecond.isFinite() || metersPerSecond < 0.0) return "—"
        val value = metersPerSecond * if (units == UnitSystem.Imperial) 2.2369362921 else 3.6
        if (!value.isFinite()) return "—"
        return String.format(locale, "%.0f", value)
    }

    fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs.coerceAtLeast(0) / 1_000)
        val hours = seconds / 3_600
        val minutes = seconds % 3_600 / 60
        if (hours > 0) return "${hours}h ${minutes}m"
        if (minutes == 0L && seconds > 0L) return "$seconds sec"
        return "$minutes min"
    }

    fun formatTripDate(startedAtMillis: Long, zone: ZoneId, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofPattern("MMM d, yyyy", locale).withZone(zone).format(Instant.ofEpochMilli(startedAtMillis))

    /** Project valid contiguous track runs into a padded unit square; invalid fixes and segment changes split paths. */
    fun route(
        points: List<TripPoint>, viewportAspect: Double = 1.0, markerPaddingFraction: Double = 0.08,
    ): TripRouteSegments {
        require(viewportAspect.isFinite() && viewportAspect > 0.0)
        require(markerPaddingFraction.isFinite() && markerPaddingFraction in 0.0..0.49)
        val truncated = points.size > MaxRouteInputPoints
        val runs = ArrayList<MutableList<TripPoint>>()
        var run = mutableListOf<TripPoint>()
        var previousSegment: Int? = null
        for (point in points.take(MaxRouteInputPoints)) {
            val valid = point.latitude.isFinite() && point.latitude in -90.0..90.0 &&
                point.longitude.isFinite() && point.longitude in -180.0..180.0
            if (!valid || (previousSegment != null && point.segment != previousSegment)) {
                if (run.isNotEmpty()) runs += run
                run = mutableListOf()
            }
            if (valid) {
                run += point
                previousSegment = point.segment
            } else previousSegment = null
        }
        if (run.isNotEmpty()) runs += run
        if (truncated) {
            points.lastOrNull()?.takeIf { it.latitude.isFinite() && it.latitude in -90.0..90.0 &&
                it.longitude.isFinite() && it.longitude in -180.0..180.0 }?.let { finalPoint ->
                val retainedLast = runs.lastOrNull()?.lastOrNull()
                if (retainedLast != finalPoint) runs += mutableListOf(finalPoint)
            }
        }
        // Keep each path separate, but place all paths in one unwrapped longitude domain for framing.
        var previousRunEnd: Double? = null
        val coordinates = runs.map { values ->
            var previousLon: Double? = null
            val runCoordinates = values.map { point ->
                var lon = point.longitude
                previousLon?.let { prior ->
                    while (lon - prior > 180.0) lon -= 360.0
                    while (lon - prior < -180.0) lon += 360.0
                } ?: previousRunEnd?.let { prior ->
                    while (lon - prior > 180.0) lon -= 360.0
                    while (lon - prior < -180.0) lon += 360.0
                }
                previousLon = lon
                lon to point.latitude
            }
            previousRunEnd = runCoordinates.lastOrNull()?.first ?: previousRunEnd
            runCoordinates
        }
        val all = coordinates.flatten()
        if (all.isEmpty()) return TripRouteSegments(emptyList(), truncated)
        val meanLat = all.map { it.second }.average().coerceIn(-85.0, 85.0)
        val cosine = cos(Math.toRadians(meanLat)).coerceAtLeast(0.01)
        val projected = coordinates.map { runPoints -> runPoints.map { (lon, lat) -> lon * cosine to lat } }
        val minX = projected.flatten().minOf { it.first }; val maxX = projected.flatten().maxOf { it.first }
        val minY = projected.flatten().minOf { it.second }; val maxY = projected.flatten().maxOf { it.second }
        val rawWidth = maxX - minX; val rawHeight = maxY - minY
        if (rawWidth <= 1e-12 && rawHeight <= 1e-12) {
            val centeredRuns = downsampleRuns(runs.map { values -> List(values.size) { 0.0 to 0.0 } }, MaxRenderedRoutePoints)
            return TripRouteSegments(centeredRuns.map { values -> values.map { TripRoutePoint(0.5f, 0.5f) } },
                truncated || centeredRuns.sumOf { it.size } < runs.sumOf { it.size })
        }
        val width = max(rawWidth, 1e-12); val height = max(rawHeight, 1e-12)
        val available = 1.0 - 2.0 * markerPaddingFraction
        val scale = minOf(available / width, available / (height * viewportAspect))
        val renderedRuns = downsampleRuns(projected, MaxRenderedRoutePoints)
        val scaledWidth = width * scale
        val scaledHeight = height * scale * viewportAspect
        val left = (1.0 - scaledWidth) / 2.0
        val top = (1.0 - scaledHeight) / 2.0
        val output = renderedRuns.map { runPoints -> runPoints.map { (x, y) ->
            TripRoutePoint((left + (x - minX) * scale).toFloat(), (top + (maxY - y) * scale * viewportAspect).toFloat())
        } }
        return TripRouteSegments(output, truncated || output.sumOf { it.size } < projected.sumOf { it.size })
    }

    /** One observed speed per timestamp bin; no value is interpolated across an empty bin or segment gap. */
    fun speedProfile(
        points: List<TripPoint>, startMillis: Long, endMillis: Long, binCount: Int,
    ): TripSpeedProfile {
        require(binCount > 0)
        val count = binCount.coerceAtMost(512)
        val bins = arrayOfNulls<Double>(count)
        val span = (endMillis - startMillis).coerceAtLeast(1L).toDouble()
        val truncated = points.size > MaxProfileInputPoints
        for (point in points.take(MaxProfileInputPoints)) {
            if (point.timestampMillis !in startMillis..endMillis || !point.speedMps.isFinite() || point.speedMps < 0.0) continue
            val index = floor((point.timestampMillis - startMillis) / span * count).toInt().coerceIn(0, count - 1)
            bins[index] = max(bins[index] ?: 0.0, point.speedMps)
        }
        val peak = bins.filterNotNull().maxOrNull() ?: 0.0
        return TripSpeedProfile(bins.map { value ->
            TripSpeedBin(value?.let { if (peak > 0.0) (it / peak).toFloat().coerceIn(0f, 1f) else 0f }, value)
        }, truncated)
    }

    /** Positive altitude gain from consecutive valid observations; any missing altitude makes it explicitly partial. */
    fun climb(points: List<TripPoint>): TripClimbEstimate {
        var previous: TripPoint? = null
        var gain = 0.0
        var hasPair = false
        var partial = false
        for (point in points) {
            val altitude = point.altitudeM
            val coordinateValid = point.latitude.isFinite() && point.latitude in -90.0..90.0 &&
                point.longitude.isFinite() && point.longitude in -180.0..180.0
            if (altitude == null || !altitude.isFinite() || !coordinateValid) {
                partial = true
                previous = null
                continue
            }
            val prior = previous
            if (prior != null && prior.segment == point.segment) {
                hasPair = true
                val delta = altitude - prior.altitudeM!!
                if (delta > 0) gain += delta
            } else if (prior != null && prior.segment != point.segment) partial = true
            previous = point
        }
        return TripClimbEstimate(gain.takeIf { hasPair }, partial)
    }

    private fun downsampleRuns(runs: List<List<Pair<Double, Double>>>, limit: Int): List<List<Pair<Double, Double>>> {
        val total = runs.sumOf { it.size }
        if (total <= limit) return runs
        // Keep complete endpoints for each retained run. If there are too many runs, never join/drop into one path.
        if (runs.size * 2 > limit) {
            val sampleCount = limit / 2
            return List(sampleCount) { index ->
                val run = runs[index * (runs.lastIndex) / (sampleCount - 1)]
                if (run.size <= 1) run else listOf(run.first(), run.last())
            }
        }
        val budget = limit - runs.size * 2
        val interior = runs.sumOf { (it.size - 2).coerceAtLeast(0) }
        return runs.map { points ->
            if (points.size <= 2) points else {
                val allowance = if (interior == 0) 0 else ((points.size - 2).toDouble() / interior * budget).toInt()
                val count = (allowance + 2).coerceAtMost(points.size)
                List(count) { index -> points[index * (points.lastIndex) / (count - 1)] }
            }
        }
    }
}
