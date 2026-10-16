package org.wpfy.carlauncher.feature.trip

import androidx.compose.ui.geometry.Offset
import org.wpfy.carlauncher.data.settings.UnitSystem
import org.wpfy.carlauncher.design.kit.KitSpeedBar
import org.wpfy.carlauncher.design.kit.KitSummaryFigure
import org.wpfy.carlauncher.design.kit.KitTripMapText
import org.wpfy.carlauncher.design.kit.KitTripRoute
import org.wpfy.carlauncher.design.kit.KitTripRouteSegment
import org.wpfy.carlauncher.design.kit.KitTripStat
import org.wpfy.carlauncher.design.kit.KitTripSummaryRow
import org.wpfy.carlauncher.design.kit.KitTripsTotals
import org.wpfy.carlauncher.design.kit.PenIcons
import org.wpfy.carlauncher.design.screens.TripHistoryGroup
import org.wpfy.carlauncher.design.screens.TripHistoryItem
import org.wpfy.carlauncher.design.screens.TripHistoryRange
import org.wpfy.carlauncher.design.screens.TripHistoryScreenState
import org.wpfy.carlauncher.design.screens.TripHistoryStatus
import org.wpfy.carlauncher.design.screens.TripScreenState
import org.wpfy.carlauncher.design.kit.ScreenGroup
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** UI data for one detail render. The caller supplies the track viewport before projecting [routePoints]. */
internal data class TripDetailUi(
    val state: TripScreenState,
    val routePoints: List<TripPoint>,
    val partial: Boolean,
)

/** Convert strict repository data to Pen screen inputs without inventing unavailable measurements. */
internal fun tripDetailUi(
    details: TripDetails,
    liveState: TripState?,
    referenceNowMillis: Long,
    zone: ZoneId,
    units: UnitSystem,
    locale: Locale,
    group: ScreenGroup,
    partial: Boolean,
    resetEnabled: Boolean,
    resetDescription: String,
    statusMessage: String? = null,
): TripDetailUi {
    val trip = details.trip
    val live = liveState?.takeIf { stateTripId(it) == trip.id }
    val recordedEnd = trip.endedAt ?: details.points.lastOrNull()?.timestampMillis?.coerceAtLeast(trip.startedAt)
    val endTime = recordedEnd ?: trip.startedAt
    val nowDate = Instant.ofEpochMilli(referenceNowMillis).atZone(zone).toLocalDate()
    val start = Instant.ofEpochMilli(trip.startedAt).atZone(zone)
    val end = Instant.ofEpochMilli(endTime).atZone(zone)
    val dateLabel = when (start.toLocalDate()) {
        nowDate -> "Today"
        nowDate.minusDays(1) -> "Yesterday"
        else -> DateTimeFormatter.ofPattern("MMM d, yyyy", locale).format(start)
    }
    val timeFormat = DateTimeFormatter.ofPattern("HH:mm", locale)
    val endLabel = when {
        live is TripState.Recording -> "Now"
        live is TripState.Paused -> "Paused"
        trip.endedAt != null -> timeFormat.format(end)
        else -> "Last recorded · incomplete"
    }
    val statusPrefix = statusMessage?.let { "$it · " }.orEmpty()
    val statusSuffix = if (partial) " · Incomplete data" else ""
    val whenLabel = "$statusPrefix$dateLabel · ${timeFormat.format(start)} – $endLabel$statusSuffix"
    val distance = TripPresentation.formatDistance(trip.distanceM, units, locale)
    val distanceUnit = TripPresentation.distanceUnit(units)
    val averageSpeed = if (trip.movingMs > 0) TripPresentation.formatSpeed(trip.avgMovingSpeedMps, units, locale) else "—"
    val maxSpeed = TripPresentation.formatSpeed(trip.maxSpeedMps, units, locale)
    val climb = TripPresentation.climb(details.points)
    val climbUnit = if (units == UnitSystem.Imperial) "ft" else "m"
    val climbValue = climb.meters?.let { meters ->
        val converted = if (units == UnitSystem.Imperial) meters * 3.280839895 else meters
        if (converted.isFinite()) converted.roundToInt().toString() else "—"
    } ?: "—"
    val climbLabel = "CLIMB"
    val endAddress = TripPresentation.endpointLabel(trip.endAddress, "Destination")
    val startAddress = TripPresentation.endpointLabel(trip.startAddress, "Start")
    val profile = TripPresentation.speedProfile(
        details.points,
        trip.startedAt,
        endTime,
        binCount = RuntimeProfileBinCount,
    )
    val peakMps = profile.bins.mapNotNull { it.recordedSpeedMps }.maxOrNull()
    val speedRange = peakMps?.let { "0–${TripPresentation.formatSpeed(it, units, locale)} ${speedUnit(units)}" }
        ?: "— ${speedUnit(units)}"
    return TripDetailUi(
        state = TripScreenState(
            title = "Trip",
            whenLabel = whenLabel,
            places = if (group == ScreenGroup.LandscapeS || group == ScreenGroup.PortraitS) null else "$startAddress → $endAddress",
            stats = listOf(
                KitTripStat(PenIcons.Timer, if (group == ScreenGroup.LandscapeS || group == ScreenGroup.PortraitS) "TIME" else "DURATION", TripPresentation.formatDuration(trip.durationMs), null),
                KitTripStat(PenIcons.DirectionsCar, if (group == ScreenGroup.LandscapeS || group == ScreenGroup.PortraitS) "DRIVE" else "MOVING", TripPresentation.formatDuration(trip.movingMs), null),
                KitTripStat(PenIcons.Speed, "AVG", averageSpeed, speedUnit(units)),
                KitTripStat(PenIcons.Speed, "MAX", maxSpeed, speedUnit(units)),
                KitTripStat(PenIcons.LocalGasStation, "FUEL", "—", null),
                KitTripStat(
                    PenIcons.Landscape,
                    climbLabel,
                    climbValue,
                    climb.meters?.let { climbUnit },
                    description = climb.meters?.let {
                        "Climb, ${if (partial || climb.partial) "partial estimate" else "estimated"}, $climbValue $climbUnit"
                    } ?: "Climb unavailable",
                ),
            ),
            route = KitTripRoute(),
            mapText = KitTripMapText(
                legend = null,
                number = distance,
                unit = distanceUnit,
                from = startAddress,
                to = endAddress,
            ),
            speedBars = profile.bins.map { KitSpeedBar(it.level) },
            speedRange = speedRange,
            allTripsLabel = "All trips",
            resetLabel = "Reset",
            resetEnabled = resetEnabled,
            resetDescription = resetDescription,
        ),
        routePoints = details.points,
        partial = partial,
    )
}

internal fun tripPlaceholderState(group: ScreenGroup, message: String, resetDescription: String): TripScreenState {
    val small = group == ScreenGroup.LandscapeS || group == ScreenGroup.PortraitS
    return TripScreenState(
        title = "Trip",
        whenLabel = message,
        places = null,
        stats = listOf(
            KitTripStat(PenIcons.Timer, if (small) "TIME" else "DURATION", "—", null),
            KitTripStat(PenIcons.DirectionsCar, if (small) "DRIVE" else "MOVING", "—", null),
            KitTripStat(PenIcons.Speed, "AVG", "—", null),
            KitTripStat(PenIcons.Speed, "MAX", "—", null),
            KitTripStat(PenIcons.LocalGasStation, "FUEL", "—", null),
            KitTripStat(PenIcons.Landscape, "CLIMB", "—", null, description = "Climb unavailable"),
        ),
        route = KitTripRoute(),
        mapText = KitTripMapText(null, "—", "", "Start", "Destination"),
        speedBars = List(RuntimeProfileBinCount) { KitSpeedBar(null) },
        speedRange = "—",
        allTripsLabel = "All trips",
        resetLabel = "Reset",
        resetEnabled = false,
        resetDescription = resetDescription,
    )
}

/** Map one filtered repository result to the existing All Trips Pen screen inputs. */
internal fun tripHistoryUi(
    rows: TripHistoryRows,
    range: TripHistoryFilter,
    referenceNowMillis: Long,
    zone: ZoneId,
    units: UnitSystem,
    locale: Locale,
    currentState: TripState?,
    status: TripHistoryStatus,
    persistsTrips: Boolean = TripRepositoryFactory.persistsTrips,
): TripHistoryScreenState {
    val today = Instant.ofEpochMilli(referenceNowMillis).atZone(zone).toLocalDate()
    val currentTripId = currentState?.tripOrNull()?.id
    val groups = TripPresentation.groupByLocalDate(rows.trips, zone).map { group ->
        val label = when (TripPresentation.dateGroupKey(group.date, today)) {
            "today" -> "TODAY"
            "yesterday" -> "YESTERDAY"
            else -> DateTimeFormatter.ofPattern("EEE, MMM d", locale).format(group.date).uppercase(locale)
        }
        TripHistoryGroup(label, group.trips.map { trip ->
            val time = tripRowTime(trip, zone, locale, currentState?.takeIf { currentTripId == trip.id })
            val average = if (trip.movingMs > 0) {
                "avg ${TripPresentation.formatSpeed(trip.avgMovingSpeedMps, units, locale)} ${speedUnit(units)}"
            } else "avg — ${speedUnit(units)}"
            TripHistoryItem(
                id = trip.id,
                row = KitTripSummaryRow(
                    title = "${TripPresentation.endpointLabel(trip.startAddress, "Start")} → ${TripPresentation.endpointLabel(trip.endAddress, "Destination")}",
                    time = time,
                    distance = "${TripPresentation.formatDistance(trip.distanceM, units, locale)} ${TripPresentation.distanceUnit(units)}",
                    average = average,
                    route = KitTripRoute(),
                    current = currentTripId == trip.id,
                ),
            )
        })
    }
    val total = rows.totals
    val average = if (total.movingMs > 0) TripPresentation.formatSpeed(total.avgMovingSpeedMps, units, locale) else "—"
    return TripHistoryScreenState(
        title = "All trips",
        subtitle = if (persistsTrips) "Saved on this device" else "Session trips · not saved",
        range = when (range) {
            TripHistoryFilter.Week -> TripHistoryRange.Week
            TripHistoryFilter.Month -> TripHistoryRange.Month
            TripHistoryFilter.All -> TripHistoryRange.All
        },
        weekLabel = "Week",
        monthLabel = "Month",
        allLabel = "All",
        summary = KitTripsTotals(
            trips = KitSummaryFigure(total.tripCount.toString()),
            distance = KitSummaryFigure(TripPresentation.formatDistance(total.distanceM, units, locale), TripPresentation.distanceUnit(units)),
            driving = KitSummaryFigure(TripPresentation.formatDuration(total.movingMs)),
            avgSpeed = KitSummaryFigure(average, speedUnit(units)),
        ),
        groups = groups,
        status = status,
        loadingTitle = "Loading trips",
        emptyTitle = "No trips yet",
        emptyMessage = "Recorded trips will appear here.",
        errorTitle = "Trips unavailable",
        errorMessage = "Trip history could not be read.",
        partialTitle = "Some trips unavailable",
        partialMessage = "Showing trips that could be read.",
        retryLabel = if (status == TripHistoryStatus.Error) "Retry" else null,
        currentLabel = "Current",
    )
}

internal fun TripRouteSegments.toKitTripRoute(): KitTripRoute = KitTripRoute(
    segments.map { segment -> KitTripRouteSegment(segment.map { Offset(it.x, it.y) }) },
)

private fun tripRowTime(trip: Trip, zone: ZoneId, locale: Locale, current: TripState?): String {
    val start = Instant.ofEpochMilli(trip.startedAt).atZone(zone)
    val clock = DateTimeFormatter.ofPattern("HH:mm", locale)
    val endLabel = when {
        current is TripState.Recording -> "Now"
        current is TripState.Paused -> "Paused"
        trip.endedAt == null -> "Incomplete"
        else -> clock.format(Instant.ofEpochMilli(trip.endedAt).atZone(zone))
    }
    return "${clock.format(start)} – $endLabel · ${TripPresentation.formatDuration(trip.durationMs)}"
}

private fun stateTripId(state: TripState): String? = when (state) {
    is TripState.Recording -> state.trip.id
    is TripState.Paused -> state.trip.id
    TripState.Idle -> null
}

private fun speedUnit(units: UnitSystem): String = if (units == UnitSystem.Imperial) "mph" else "km/h"

private const val RuntimeProfileBinCount = 24
