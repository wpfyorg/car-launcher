package org.wpfy.carlauncher.feature.home

import org.wpfy.carlauncher.data.settings.UnitSystem
import org.wpfy.carlauncher.design.adapters.kitGuidance
import org.wpfy.carlauncher.design.adapters.kitHudState
import org.wpfy.carlauncher.design.adapters.kitNoSpeed
import org.wpfy.carlauncher.design.screens.KitHudState
import org.wpfy.carlauncher.feature.navigation.NavigationState

internal const val PageDotsDescription = "Switch between map and media"

/** The map search tab's placeholder (Pen "Map Search": "Navigate"). */
internal const val MapSearchPlaceholder = "Navigate"

/**
 * The kit HUD of Home. Guidance, the ETA bar or the status strip come from [nav]; the report flow comes from the
 * [accessory] (an edition without one has no Report button). The map never shows speed or a camera alert: the Drive
 * Card does. [ownsControls]: the navigation content draws its own controls (the embedded app in Free), so Home adds
 * only the chip, the page dots and the media page. [dots]: the page dots, shown on the media page only. [driveCard]:
 * the Drive Card is on screen, so the report-sent pill is the card's. Idle navigation always shows the Pen's map search
 * tab at the top of the map instead of the "Ready to drive" strip (arrived, no GPS and errors keep their strip).
 * [searchOpen]: the destination search panel is open. It replaces the tab and sits over the top of the map, so the
 * HUD parts at that edge (tab, turn card, status strip, report button and sheet) are left out until it closes.
 */
internal fun homeHud(
    nav: NavigationState,
    accessory: MapAccessoryHud?,
    nowPlaying: KitHudState.NowPlaying?,
    mediaPage: Boolean,
    ownsControls: Boolean,
    dots: Boolean,
    units: UnitSystem = UnitSystem.Imperial,
    driveCard: Boolean = false,
    searchOpen: Boolean = false,
): KitHudState {
    val maneuver = nav.progress.nextManeuver.takeIf { showsGuidance(nav) }
    if (ownsControls) {
        val turn = maneuver?.let { kitGuidance(it, units = units) }
        return KitHudState(
            speed = kitNoSpeed(units), nowPlaying = nowPlaying, mediaPage = mediaPage,
            nextTurn = if (mediaPage) turn else null,
            showReport = false, showDots = dots,
            nextTurnDescription = turn?.let { nextTurnDescription(it) }, dotsDescription = PageDotsDescription,
        )
    }
    val hud = kitHudState(
        nav,
        speed = accessory?.speed ?: kitNoSpeed(units),
        nowPlaying = nowPlaying,
        mediaPage = mediaPage,
        reportSheet = accessory?.reportSheet,
        reportSent = accessory?.reportSent.takeUnless { driveCard },
        units = units,
    )
    // Idle never shows the "Ready to drive" strip, with or without the Drive Card (the Pro accessory registers late).
    val idle = navigationStatusFor(nav)?.collapsible == true
    if (searchOpen) {
        return hud.copy(
            guidance = null, nextTurn = null, status = null, search = null, reportSheet = null, reportSent = null,
            showReport = false, showDots = dots, dotsDescription = PageDotsDescription,
        )
    }
    return hud.copy(
        status = if (idle) null else hud.status,
        search = if (idle) MapSearchPlaceholder else null,
        showReport = accessory?.hasReport == true,
        showDots = dots,
        nextTurnDescription = hud.nextTurn?.let { nextTurnDescription(it) },
        dotsDescription = PageDotsDescription,
    )
}

/** What the next-turn chip announces: "Next turn <distance · road>. Show map". */
internal fun nextTurnDescription(turn: KitHudState.Guidance): String =
    "Next turn " + listOf(turn.distance, turn.road).filter { it.isNotBlank() }.joinToString(" · ") + ". Show map"

/** What the now-playing chip announces. */
internal fun nowPlayingDescription(title: String): String = "Now playing $title. Open media"
