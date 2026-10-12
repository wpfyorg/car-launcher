package org.wpfy.carlauncher.feature.notifications

import java.util.concurrent.TimeUnit

/** Formats notification age as "now", minutes, hours, or days; future timestamps count as "now". */
internal fun relativeTime(postTimeMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val age = (nowMillis - postTimeMillis).coerceAtLeast(0L)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(age)
    return when {
        minutes < 1L -> "now"
        minutes < 60L -> "${minutes}m"
        minutes < 1_440L -> "${minutes / 60L}h"
        else -> "${minutes / 1_440L}d"
    }
}
