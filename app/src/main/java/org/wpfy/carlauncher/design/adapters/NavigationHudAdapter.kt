package org.wpfy.carlauncher.design.adapters

import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Locale
import org.wpfy.carlauncher.data.settings.UnitSystem
import org.wpfy.carlauncher.design.kit.PenIcons
import org.wpfy.carlauncher.design.screens.KitHudState
import org.wpfy.carlauncher.design.screens.KitStatusAction
import org.wpfy.carlauncher.feature.home.NavigationStatusAction
import org.wpfy.carlauncher.feature.home.navigationStatusFor
import org.wpfy.carlauncher.feature.navigation.Maneuver
import org.wpfy.carlauncher.feature.navigation.ManeuverType
import org.wpfy.carlauncher.feature.navigation.NavigationError
import org.wpfy.carlauncher.feature.navigation.NavigationProgress
import org.wpfy.carlauncher.feature.navigation.NavigationState
import org.wpfy.carlauncher.feature.navigation.durationAndDistance
import org.wpfy.carlauncher.feature.navigation.formatDistance
import org.wpfy.carlauncher.feature.navigation.formatDuration
import org.wpfy.carlauncher.feature.navigation.formatEta

/*
 * Live navigation state -> the kit HUD's display-ready parts. The wording comes from the same
 * helpers the live HUD uses (formatDistance, navigationStatusFor, ...), so the kit says what the
 * live screen says; only the icons are the Pen's. Free builds have no speed, so the speed cluster
 * takes a plain value and defaults to the unavailable reading.
 */

/** Whether [locale]'s region posts road speeds in miles per hour (US, UK, Liberia, Myanmar, ...). */
internal fun usesMilesPerHour(locale: Locale): Boolean = locale.country.uppercase(Locale.ROOT) in MphRegions

/** The unit label of the unavailable speed reading in [locale]: "mph" where roads are posted so, else "km/h". */
fun speedUnitLabelFor(locale: Locale): String = speedUnitLabelFor(if (usesMilesPerHour(locale)) UnitSystem.Imperial else UnitSystem.Metric)

/** The unit label of the speed reading in [units]. */
fun speedUnitLabelFor(units: UnitSystem): String = if (units == UnitSystem.Imperial) "mph" else "km/h"

/** A single zero for no speed reading or while stationary. */
const val NoSpeedText = "0"

/** The speed cluster when nothing measures speed (Free), as the live Pro HUD shows no fix, in the locale's unit. */
fun kitNoSpeed(locale: Locale = Locale.getDefault()): KitHudState.Speed =
    KitHudState.Speed(value = NoSpeedText, unit = speedUnitLabelFor(locale), limit = null)

/** The unavailable speed reading in [units]. */
fun kitNoSpeed(units: UnitSystem): KitHudState.Speed =
    KitHudState.Speed(value = NoSpeedText, unit = speedUnitLabelFor(units), limit = null)

/** Regions that post road speeds in miles per hour. */
private val MphRegions = setOf(
    "US", "PR", "GU", "VI", "AS", "MP", // United States and territories
    "GB", "IM", "JE", "GG", // United Kingdom and Crown Dependencies
    "LR", "MM", "BS", "BZ", "KY", "VG", "TC", "FK", "AG", "DM", "GD", "KN", "LC", "VC",
)

/** The Pen arrow for a maneuver, one-to-one with the live HUD's arrows. */
fun ManeuverType.kitIcon(): ImageVector = when (this) {
    ManeuverType.TurnLeft -> PenIcons.TurnLeft
    ManeuverType.TurnRight -> PenIcons.TurnRight
    ManeuverType.SlightLeft -> PenIcons.TurnSlightLeft
    ManeuverType.SlightRight -> PenIcons.TurnSlightRight
    ManeuverType.SharpLeft -> PenIcons.TurnSharpLeft
    ManeuverType.SharpRight -> PenIcons.TurnSharpRight
    ManeuverType.UTurn -> PenIcons.UTurnLeft
    ManeuverType.Merge -> PenIcons.Merge
    ManeuverType.ExitLeft -> PenIcons.RampLeft
    ManeuverType.ExitRight -> PenIcons.RampRight
    ManeuverType.Roundabout -> PenIcons.RoundaboutRight
    ManeuverType.Continue -> PenIcons.Straight
    ManeuverType.Arrive -> PenIcons.Flag
    ManeuverType.Unknown -> PenIcons.Navigation
}

/** The turn card: distance, road (or the instruction), and the "Then" maneuver when there is one. */
fun kitGuidance(maneuver: Maneuver, then: Maneuver? = null, units: UnitSystem = UnitSystem.Imperial): KitHudState.Guidance = KitHudState.Guidance(
    distance = formatDistance(maneuver.distanceMeters, units),
    road = maneuver.roadName ?: maneuver.instruction,
    arrow = maneuver.type.kitIcon(),
    thenArrow = then?.type?.kitIcon(),
    thenDescription = then?.let { "Then ${it.instruction}" },
)

/** The ETA bar: arrival clock and "<duration> · <distance>"; unknown parts stay blank. */
fun kitEta(progress: NavigationProgress, units: UnitSystem = UnitSystem.Imperial): KitHudState.Eta = KitHudState.Eta(
    arrival = formatEta(progress.etaEpochMillis).orEmpty(),
    remaining = durationAndDistance(
        formatDuration(progress.remainingDurationSeconds),
        formatDistance(progress.remainingDistanceMeters, units),
    ),
    endDescription = "Stop navigation",
)

/** The status strip that replaces guidance (idle, arrived, no GPS, errors), or null while guiding. */
fun kitStatus(state: NavigationState): KitHudState.Status? {
    val status = navigationStatusFor(state) ?: return null
    val icon = when {
        state.progress.arrived -> PenIcons.CheckCircle
        state.error == NavigationError.PermissionDenied || !state.gpsAvailable -> PenIcons.LocationOff
        state.error != null -> PenIcons.AltRoute
        else -> PenIcons.Navigation
    }
    val action = when (status.action) {
        NavigationStatusAction.SearchDestination -> KitStatusAction.Search
        NavigationStatusAction.OpenAppSettings -> KitStatusAction.AppSettings
        NavigationStatusAction.Retry -> KitStatusAction.Retry
        NavigationStatusAction.OpenNavigationSettings -> KitStatusAction.NavigationSettings
    }
    return KitHudState.Status(title = status.title, body = status.body, icon = icon, action = action)
}

/**
 * The HUD for [state]. Guidance and the ETA bar show while guiding; otherwise the status strip does,
 * as on the live HUD. The remaining parts come from the caller: [speed] from the Pro speed
 * layer, [nowPlaying] and [mediaPage] from the media state, the report overlays from the report flow.
 * On the media page the next turn rides the chip.
 */
fun kitHudState(
    state: NavigationState,
    speed: KitHudState.Speed = kitNoSpeed(),
    nowPlaying: KitHudState.NowPlaying? = null,
    mediaPage: Boolean = false,
    reportSheet: KitHudState.ReportSheet? = null,
    reportSent: KitHudState.ReportSent? = null,
    units: UnitSystem = UnitSystem.Imperial,
): KitHudState {
    val status = kitStatus(state)
    val guidance = if (status == null) {
        state.progress.nextManeuver?.let { kitGuidance(it, state.progress.secondaryManeuver, units) }
    } else {
        null
    }
    return KitHudState(
        guidance = guidance,
        speed = speed,
        eta = if (status == null) kitEta(state.progress, units) else null,
        status = status,
        nowPlaying = nowPlaying,
        mediaPage = mediaPage,
        nextTurn = if (mediaPage) guidance else null,
        reportSheet = reportSheet,
        reportSent = reportSent,
    )
}
