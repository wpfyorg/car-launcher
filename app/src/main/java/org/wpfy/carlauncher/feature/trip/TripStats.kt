package org.wpfy.carlauncher.feature.trip

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object TripStats {
    /** Local calendar periods, Monday-based week. Whole trips belong to their start date. */
    fun totals(trips: List<TripSummary>, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): TripPeriodTotals {
        val date = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val today = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val week = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay(zone).toInstant().toEpochMilli()
        val month = date.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        fun sum(from: Long): TripTotals {
            val selected = trips.map { it.trip }.filter { it.startedAt in from..nowMillis }
            return TripTotals(selected.size, selected.sumOf { it.distanceM }, selected.sumOf { it.movingMs },
                selected.sumOf { it.idleMs }, selected.maxOfOrNull { it.maxSpeedMps } ?: 0.0)
        }
        return TripPeriodTotals(sum(today), sum(week), sum(month))
    }
}
