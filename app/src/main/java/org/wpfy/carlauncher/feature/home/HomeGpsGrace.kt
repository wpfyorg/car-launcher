package org.wpfy.carlauncher.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import org.wpfy.carlauncher.feature.navigation.NavigationState

/**
 * How long a missing location fix is hidden before Home says "GPS unavailable". The first fix takes seconds after
 * start, and a fix can drop for a moment (tunnel, state flicker between idle and no-fix); neither is a warning.
 */
internal const val GpsGraceMillis = 10_000L

/**
 * Sets [set] to true once the fix has been missing for [graceMillis], false as soon as it is back. Run it again (a new
 * coroutine) whenever [available] changes: a flicker restarts the wait instead of adding up.
 */
internal suspend fun confirmGpsLoss(available: Boolean, graceMillis: Long, set: (Boolean) -> Unit) {
    set(false)
    if (available) return
    delay(graceMillis)
    set(true)
}

/** True while the fix has been missing for longer than the grace period. Starts false, so the first seconds show no warning. */
@Composable
internal fun rememberGpsLossConfirmed(gpsAvailable: Boolean, graceMillis: Long = GpsGraceMillis): State<Boolean> =
    produceState(false, gpsAvailable, graceMillis) { confirmGpsLoss(gpsAvailable, graceMillis) { value = it } }

/**
 * The navigation state Home shows: a missing fix counts as present until [gpsLossConfirmed]. A real error (permission
 * denied, no route) is never held back.
 */
internal fun homeNavigation(nav: NavigationState, gpsLossConfirmed: State<Boolean>): NavigationState =
    homeNavigation(nav, gpsLossConfirmed.value)

internal fun homeNavigation(nav: NavigationState, gpsLossConfirmed: Boolean): NavigationState =
    if (nav.gpsAvailable || gpsLossConfirmed || nav.error != null) nav else nav.copy(gpsAvailable = true)
