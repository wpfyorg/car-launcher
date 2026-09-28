package org.wpfy.carlauncher.design.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import org.wpfy.carlauncher.design.kit.KitDockActions
import org.wpfy.carlauncher.design.kit.KitDriveScale
import org.wpfy.carlauncher.design.kit.KitSpeedBar
import org.wpfy.carlauncher.design.kit.KitSpeedProfile
import org.wpfy.carlauncher.design.kit.KitTripMapText
import org.wpfy.carlauncher.design.kit.KitTripRoute
import org.wpfy.carlauncher.design.kit.KitTripRouteMap
import org.wpfy.carlauncher.design.kit.KitTripStat
import org.wpfy.carlauncher.design.kit.KitTripStatTile
import org.wpfy.carlauncher.design.kit.PenIcons
import org.wpfy.carlauncher.design.kit.ScreenGroup
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeL.Trip as LandscapeLTrip
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeM.Trip as LandscapeMTrip
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeS.Trip as LandscapeSTrip
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeXL.Trip as LandscapeXLTrip
import org.wpfy.carlauncher.design.kit.PenScreens.PortraitL.Trip as PortraitLTrip
import org.wpfy.carlauncher.design.kit.PenScreens.PortraitS.Trip as PortraitSTrip
import org.wpfy.carlauncher.design.kit.PenScreens.UltrawideL.Trip as UltrawideLTrip
import org.wpfy.carlauncher.design.kit.PenScreens.UltrawideM.Trip as UltrawideMTrip
import org.wpfy.carlauncher.design.kit.PenKit.SpeedProfile as ProfileKit

/**
 * What a Trip screen's groups take from their Pen frames (Pen "Trip screen"): the shared frame and header ([screen]),
 * the "All trips" chip ([allTrips], with its [chevron] where the frame has one) and the "Reset" chip ([reset], in the
 * frames that draw it); then the body's: the [statsGap] between the stats column's rows, the [rowGap] between the tiles
 * of a row, the speed profile's [profileHeight], and the stats column's [statsShare] of the body (its width beside the
 * route map in landscape, its height under it in portrait). The route map takes the rest of the body, and the two tile
 * rows share what the stats column leaves the profile.
 */
@Immutable
class TripScreenSpec internal constructor(
    val screen: DriveScreenSpec,
    val allTrips: DriveChip, val chevron: DriveGlyph?, val reset: DriveChip?,
    val statsGap: Dp, val rowGap: Dp, val profileHeight: Dp, val statsShare: Float,
) {
    /** Where the frame names the trip's places in its subtitle: the landscape frames wide enough for it. */
    val subtitleNamesPlaces: Boolean get() = !screen.portrait && screen.tier != org.wpfy.carlauncher.design.theme.OverlayTier.Small

    companion object {
        val LandscapeS = TripScreenSpec(
            DriveScreenSpec(
                ScreenGroup.LandscapeS, SystemScreenSpec.LandscapeS, LandscapeSTrip.Pane.PadStart, LandscapeSTrip.Pane.PadTop, LandscapeSTrip.Pane.PadEnd, LandscapeSTrip.Pane.PadBottom, LandscapeSTrip.Pane.Gap,
                LandscapeSTrip.Pane.Header.Gap, LandscapeSTrip.Pane.Header.Title.Gap, LandscapeSTrip.Pane.Header.Title.Title.Style, LandscapeSTrip.Pane.Header.Title.Title.Fill, LandscapeSTrip.Pane.Header.Title.Subtitle.Style, LandscapeSTrip.Pane.Header.Title.Subtitle.Fill,
                LandscapeSTrip.Pane.Body.Gap,
            ),
            allTrips = DriveChip(LandscapeSTrip.Pane.Header.AllTrips.H, LandscapeSTrip.Pane.Header.AllTrips.PadStart, LandscapeSTrip.Pane.Header.AllTrips.PadEnd, LandscapeSTrip.Pane.Header.AllTrips.Gap, LandscapeSTrip.Pane.Header.AllTrips.Radius, LandscapeSTrip.Pane.Header.AllTrips.Stroke, LandscapeSTrip.Pane.Header.AllTrips.StrokeColor, LandscapeSTrip.Pane.Header.AllTrips.Fill, LandscapeSTrip.Pane.Header.AllTrips.Icon.W, LandscapeSTrip.Pane.Header.AllTrips.Icon.Fill, LandscapeSTrip.Pane.Header.AllTrips.Label.Style, LandscapeSTrip.Pane.Header.AllTrips.Label.Fill), chevron = null, reset = null,
            statsGap = LandscapeSTrip.Pane.Body.Stats.Gap, rowGap = LandscapeSTrip.Pane.Body.Stats.Row1.Gap, profileHeight = LandscapeSTrip.Pane.Body.Stats.SpeedProfile.H,
            statsShare = LandscapeSTrip.Pane.Body.Stats.W / LandscapeSTrip.Pane.Body.W,
        )

        val LandscapeM = TripScreenSpec(
            DriveScreenSpec(
                ScreenGroup.LandscapeM, SystemScreenSpec.LandscapeM, LandscapeMTrip.Pane.PadStart, LandscapeMTrip.Pane.PadTop, LandscapeMTrip.Pane.PadEnd, LandscapeMTrip.Pane.PadBottom, LandscapeMTrip.Pane.Gap,
                LandscapeMTrip.Pane.Header.Gap, LandscapeMTrip.Pane.Header.Title.Gap, LandscapeMTrip.Pane.Header.Title.Title.Style, LandscapeMTrip.Pane.Header.Title.Title.Fill, LandscapeMTrip.Pane.Header.Title.Subtitle.Style, LandscapeMTrip.Pane.Header.Title.Subtitle.Fill,
                LandscapeMTrip.Pane.Body.Gap,
            ),
            allTrips = DriveChip(LandscapeMTrip.Pane.Header.AllTrips.H, LandscapeMTrip.Pane.Header.AllTrips.PadStart, LandscapeMTrip.Pane.Header.AllTrips.PadEnd, LandscapeMTrip.Pane.Header.AllTrips.Gap, LandscapeMTrip.Pane.Header.AllTrips.Radius, LandscapeMTrip.Pane.Header.AllTrips.Stroke, LandscapeMTrip.Pane.Header.AllTrips.StrokeColor, LandscapeMTrip.Pane.Header.AllTrips.Fill, LandscapeMTrip.Pane.Header.AllTrips.Icon.W, LandscapeMTrip.Pane.Header.AllTrips.Icon.Fill, LandscapeMTrip.Pane.Header.AllTrips.Label.Style, LandscapeMTrip.Pane.Header.AllTrips.Label.Fill), chevron = DriveGlyph(LandscapeMTrip.Pane.Header.AllTrips.Chevron.W, LandscapeMTrip.Pane.Header.AllTrips.Chevron.Fill), reset = DriveChip(LandscapeMTrip.Pane.Header.Reset.H, LandscapeMTrip.Pane.Header.Reset.PadStart, LandscapeMTrip.Pane.Header.Reset.PadEnd, LandscapeMTrip.Pane.Header.Reset.Gap, LandscapeMTrip.Pane.Header.Reset.Radius, LandscapeMTrip.Pane.Header.Reset.Stroke, LandscapeMTrip.Pane.Header.Reset.StrokeColor, LandscapeMTrip.Pane.Header.Reset.Fill, LandscapeMTrip.Pane.Header.Reset.Icon.W, LandscapeMTrip.Pane.Header.Reset.Icon.Fill, LandscapeMTrip.Pane.Header.Reset.Label.Style, LandscapeMTrip.Pane.Header.Reset.Label.Fill),
            statsGap = LandscapeMTrip.Pane.Body.Stats.Gap, rowGap = LandscapeMTrip.Pane.Body.Stats.Row1.Gap, profileHeight = LandscapeMTrip.Pane.Body.Stats.SpeedProfile.H,
            statsShare = LandscapeMTrip.Pane.Body.Stats.W / LandscapeMTrip.Pane.Body.W,
        )

        val UltrawideM = TripScreenSpec(
            DriveScreenSpec(
                ScreenGroup.UltrawideM, SystemScreenSpec.LandscapeM, UltrawideMTrip.Pane.PadStart, UltrawideMTrip.Pane.PadTop, UltrawideMTrip.Pane.PadEnd, UltrawideMTrip.Pane.PadBottom, UltrawideMTrip.Pane.Gap,
                UltrawideMTrip.Pane.Header.Gap, UltrawideMTrip.Pane.Header.Title.Gap, UltrawideMTrip.Pane.Header.Title.Title.Style, UltrawideMTrip.Pane.Header.Title.Title.Fill, UltrawideMTrip.Pane.Header.Title.Subtitle.Style, UltrawideMTrip.Pane.Header.Title.Subtitle.Fill,
                UltrawideMTrip.Pane.Body.Gap,
            ),
            allTrips = DriveChip(UltrawideMTrip.Pane.Header.AllTrips.H, UltrawideMTrip.Pane.Header.AllTrips.PadStart, UltrawideMTrip.Pane.Header.AllTrips.PadEnd, UltrawideMTrip.Pane.Header.AllTrips.Gap, UltrawideMTrip.Pane.Header.AllTrips.Radius, UltrawideMTrip.Pane.Header.AllTrips.Stroke, UltrawideMTrip.Pane.Header.AllTrips.StrokeColor, UltrawideMTrip.Pane.Header.AllTrips.Fill, UltrawideMTrip.Pane.Header.AllTrips.Icon.W, UltrawideMTrip.Pane.Header.AllTrips.Icon.Fill, UltrawideMTrip.Pane.Header.AllTrips.Label.Style, UltrawideMTrip.Pane.Header.AllTrips.Label.Fill), chevron = null, reset = null,
            statsGap = UltrawideMTrip.Pane.Body.Stats.Gap, rowGap = UltrawideMTrip.Pane.Body.Stats.Row1.Gap, profileHeight = UltrawideMTrip.Pane.Body.Stats.SpeedProfile.H,
            statsShare = UltrawideMTrip.Pane.Body.Stats.W / UltrawideMTrip.Pane.Body.W,
        )

        val LandscapeL = TripScreenSpec(
            DriveScreenSpec(
                ScreenGroup.LandscapeL, SystemScreenSpec.LandscapeL, LandscapeLTrip.Pane.PadStart, LandscapeLTrip.Pane.PadTop, LandscapeLTrip.Pane.PadEnd, LandscapeLTrip.Pane.PadBottom, LandscapeLTrip.Pane.Gap,
                LandscapeLTrip.Pane.Header.Gap, LandscapeLTrip.Pane.Header.Title.Gap, LandscapeLTrip.Pane.Header.Title.Title.Style, LandscapeLTrip.Pane.Header.Title.Title.Fill, LandscapeLTrip.Pane.Header.Title.Subtitle.Style, LandscapeLTrip.Pane.Header.Title.Subtitle.Fill,
                LandscapeLTrip.Pane.Body.Gap,
            ),
            allTrips = DriveChip(LandscapeLTrip.Pane.Header.AllTrips.H, LandscapeLTrip.Pane.Header.AllTrips.PadStart, LandscapeLTrip.Pane.Header.AllTrips.PadEnd, LandscapeLTrip.Pane.Header.AllTrips.Gap, LandscapeLTrip.Pane.Header.AllTrips.Radius, LandscapeLTrip.Pane.Header.AllTrips.Stroke, LandscapeLTrip.Pane.Header.AllTrips.StrokeColor, LandscapeLTrip.Pane.Header.AllTrips.Fill, LandscapeLTrip.Pane.Header.AllTrips.Icon.W, LandscapeLTrip.Pane.Header.AllTrips.Icon.Fill, LandscapeLTrip.Pane.Header.AllTrips.Label.Style, LandscapeLTrip.Pane.Header.AllTrips.Label.Fill), chevron = null, reset = null,
            statsGap = LandscapeLTrip.Pane.Body.Stats.Gap, rowGap = LandscapeLTrip.Pane.Body.Stats.Row1.Gap, profileHeight = LandscapeLTrip.Pane.Body.Stats.SpeedProfile.H,
            statsShare = LandscapeLTrip.Pane.Body.Stats.W / LandscapeLTrip.Pane.Body.W,
        )

        val UltrawideL = TripScreenSpec(
            DriveScreenSpec(
                ScreenGroup.UltrawideL, SystemScreenSpec.LandscapeL, UltrawideLTrip.Pane.PadStart, UltrawideLTrip.Pane.PadTop, UltrawideLTrip.Pane.PadEnd, UltrawideLTrip.Pane.PadBottom, UltrawideLTrip.Pane.Gap,
                UltrawideLTrip.Pane.Header.Gap, UltrawideLTrip.Pane.Header.Title.Gap, UltrawideLTrip.Pane.Header.Title.Title.Style, UltrawideLTrip.Pane.Header.Title.Title.Fill, UltrawideLTrip.Pane.Header.Title.Subtitle.Style, UltrawideLTrip.Pane.Header.Title.Subtitle.Fill,
                UltrawideLTrip.Pane.Body.Gap,
            ),
            allTrips = DriveChip(UltrawideLTrip.Pane.Header.AllTrips.H, UltrawideLTrip.Pane.Header.AllTrips.PadStart, UltrawideLTrip.Pane.Header.AllTrips.PadEnd, UltrawideLTrip.Pane.Header.AllTrips.Gap, UltrawideLTrip.Pane.Header.AllTrips.Radius, UltrawideLTrip.Pane.Header.AllTrips.Stroke, UltrawideLTrip.Pane.Header.AllTrips.StrokeColor, UltrawideLTrip.Pane.Header.AllTrips.Fill, UltrawideLTrip.Pane.Header.AllTrips.Icon.W, UltrawideLTrip.Pane.Header.AllTrips.Icon.Fill, UltrawideLTrip.Pane.Header.AllTrips.Label.Style, UltrawideLTrip.Pane.Header.AllTrips.Label.Fill), chevron = null, reset = null,
            statsGap = UltrawideLTrip.Pane.Body.Stats.Gap, rowGap = UltrawideLTrip.Pane.Body.Stats.Row1.Gap, profileHeight = UltrawideLTrip.Pane.Body.Stats.SpeedProfile.H,
            statsShare = UltrawideLTrip.Pane.Body.Stats.W / UltrawideLTrip.Pane.Body.W,
        )

        val LandscapeXL = TripScreenSpec(
            DriveScreenSpec(
                ScreenGroup.LandscapeXL, SystemScreenSpec.LandscapeXL, LandscapeXLTrip.Pane.PadStart, LandscapeXLTrip.Pane.PadTop, LandscapeXLTrip.Pane.PadEnd, LandscapeXLTrip.Pane.PadBottom, LandscapeXLTrip.Pane.Gap,
                LandscapeXLTrip.Pane.Header.Gap, LandscapeXLTrip.Pane.Header.Title.Gap, LandscapeXLTrip.Pane.Header.Title.Title.Style, LandscapeXLTrip.Pane.Header.Title.Title.Fill, LandscapeXLTrip.Pane.Header.Title.Subtitle.Style, LandscapeXLTrip.Pane.Header.Title.Subtitle.Fill,
                LandscapeXLTrip.Pane.Body.Gap,
            ),
            allTrips = DriveChip(LandscapeXLTrip.Pane.Header.AllTrips.H, LandscapeXLTrip.Pane.Header.AllTrips.PadStart, LandscapeXLTrip.Pane.Header.AllTrips.PadEnd, LandscapeXLTrip.Pane.Header.AllTrips.Gap, LandscapeXLTrip.Pane.Header.AllTrips.Radius, LandscapeXLTrip.Pane.Header.AllTrips.Stroke, LandscapeXLTrip.Pane.Header.AllTrips.StrokeColor, LandscapeXLTrip.Pane.Header.AllTrips.Fill, LandscapeXLTrip.Pane.Header.AllTrips.Icon.W, LandscapeXLTrip.Pane.Header.AllTrips.Icon.Fill, LandscapeXLTrip.Pane.Header.AllTrips.Label.Style, LandscapeXLTrip.Pane.Header.AllTrips.Label.Fill), chevron = null, reset = null,
            statsGap = LandscapeXLTrip.Pane.Body.Stats.Gap, rowGap = LandscapeXLTrip.Pane.Body.Stats.Row1.Gap, profileHeight = LandscapeXLTrip.Pane.Body.Stats.SpeedProfile.H,
            statsShare = LandscapeXLTrip.Pane.Body.Stats.W / LandscapeXLTrip.Pane.Body.W,
        )

        val PortraitS = TripScreenSpec(
            DriveScreenSpec(
                ScreenGroup.PortraitS, SystemScreenSpec.PortraitS, PortraitSTrip.Pane.PadStart, PortraitSTrip.Pane.PadTop, PortraitSTrip.Pane.PadEnd, PortraitSTrip.Pane.PadBottom, PortraitSTrip.Pane.Gap,
                PortraitSTrip.Pane.Header.Gap, PortraitSTrip.Pane.Header.Title.Gap, PortraitSTrip.Pane.Header.Title.Title.Style, PortraitSTrip.Pane.Header.Title.Title.Fill, PortraitSTrip.Pane.Header.Title.Subtitle.Style, PortraitSTrip.Pane.Header.Title.Subtitle.Fill,
                PortraitSTrip.Pane.Body.Gap,
            ),
            allTrips = DriveChip(PortraitSTrip.Pane.Header.AllTrips.H, PortraitSTrip.Pane.Header.AllTrips.PadStart, PortraitSTrip.Pane.Header.AllTrips.PadEnd, PortraitSTrip.Pane.Header.AllTrips.Gap, PortraitSTrip.Pane.Header.AllTrips.Radius, PortraitSTrip.Pane.Header.AllTrips.Stroke, PortraitSTrip.Pane.Header.AllTrips.StrokeColor, PortraitSTrip.Pane.Header.AllTrips.Fill, PortraitSTrip.Pane.Header.AllTrips.Icon.W, PortraitSTrip.Pane.Header.AllTrips.Icon.Fill, PortraitSTrip.Pane.Header.AllTrips.Label.Style, PortraitSTrip.Pane.Header.AllTrips.Label.Fill), chevron = null, reset = null,
            statsGap = PortraitSTrip.Pane.Body.Stats.Gap, rowGap = PortraitSTrip.Pane.Body.Stats.Row1.Gap, profileHeight = PortraitSTrip.Pane.Body.Stats.SpeedProfile.H,
            statsShare = PortraitSTrip.Pane.Body.Stats.H / PortraitSTrip.Pane.Body.H,
        )

        val PortraitL = TripScreenSpec(
            DriveScreenSpec(
                ScreenGroup.PortraitL, SystemScreenSpec.PortraitL, PortraitLTrip.Pane.PadStart, PortraitLTrip.Pane.PadTop, PortraitLTrip.Pane.PadEnd, PortraitLTrip.Pane.PadBottom, PortraitLTrip.Pane.Gap,
                PortraitLTrip.Pane.Header.Gap, PortraitLTrip.Pane.Header.Title.Gap, PortraitLTrip.Pane.Header.Title.Title.Style, PortraitLTrip.Pane.Header.Title.Title.Fill, PortraitLTrip.Pane.Header.Title.Subtitle.Style, PortraitLTrip.Pane.Header.Title.Subtitle.Fill,
                PortraitLTrip.Pane.Body.Gap,
            ),
            allTrips = DriveChip(PortraitLTrip.Pane.Header.AllTrips.H, PortraitLTrip.Pane.Header.AllTrips.PadStart, PortraitLTrip.Pane.Header.AllTrips.PadEnd, PortraitLTrip.Pane.Header.AllTrips.Gap, PortraitLTrip.Pane.Header.AllTrips.Radius, PortraitLTrip.Pane.Header.AllTrips.Stroke, PortraitLTrip.Pane.Header.AllTrips.StrokeColor, PortraitLTrip.Pane.Header.AllTrips.Fill, PortraitLTrip.Pane.Header.AllTrips.Icon.W, PortraitLTrip.Pane.Header.AllTrips.Icon.Fill, PortraitLTrip.Pane.Header.AllTrips.Label.Style, PortraitLTrip.Pane.Header.AllTrips.Label.Fill), chevron = null, reset = null,
            statsGap = PortraitLTrip.Pane.Body.Stats.Gap, rowGap = PortraitLTrip.Pane.Body.Stats.Row1.Gap, profileHeight = PortraitLTrip.Pane.Body.Stats.SpeedProfile.H,
            statsShare = PortraitLTrip.Pane.Body.Stats.H / PortraitLTrip.Pane.Body.H,
        )

        /** The spec of the Pen frames of [group]; the XL landscape group draws the Pen's Ultrawide XL frame (its only one). */
        fun forGroup(group: ScreenGroup): TripScreenSpec = when (group) {
            ScreenGroup.LandscapeS -> LandscapeS
            ScreenGroup.LandscapeM -> LandscapeM
            ScreenGroup.UltrawideM -> UltrawideM
            ScreenGroup.LandscapeL -> LandscapeL
            ScreenGroup.UltrawideL -> UltrawideL
            ScreenGroup.LandscapeXL -> LandscapeXL
            ScreenGroup.PortraitS -> PortraitS
            ScreenGroup.PortraitL -> PortraitL
        }
    }
}

/** The names of the six stat tiles in the Pen, in reading order (two rows of three). */
private val TripStatNodes = listOf("StatDURATION", "StatMOVING", "StatAVG", "StatMAX", "StatFUEL", "StatCLIMB")

/**
 * What the Trip screen shows. [whenLabel] is the subtitle's day and time span ("Today · 11:04 – 12:16"), [places] the
 * trip's ends ("Santa Monica → Malibu", null: not known); [stats] are the six tiles in reading order; [map] and
 * [profile] the route and the speed over it.
 */
@Immutable
class TripScreenState(
    val title: String,
    val whenLabel: String,
    val places: String?,
    val stats: List<KitTripStat>,
    val route: KitTripRoute,
    val mapText: KitTripMapText,
    val speedBars: List<KitSpeedBar>,
    val speedRange: String,
    val allTripsLabel: String,
    val resetLabel: String,
    val resetEnabled: Boolean = false,
    val resetDescription: String = "Reset unavailable for this trip",
) {
    init {
        require(stats.size == TripStatNodes.size) { "A trip shows ${TripStatNodes.size} stats" }
    }
}

/**
 * The Trip screen (Pen "Trip screen"): the header (the title over the trip's day, time and places, the "All trips" chip,
 * the "Reset" chip where the frame has it and [onReset] is set), then the body: the route map beside the stats in
 * landscape, over them in portrait. The stats are three tiles to a row, two rows, then the speed profile.
 * Tags follow the Pen frame: "Screen/Dock", "Screen/Pane", "Screen/Pane/Header/...", "Screen/Pane/Body/RouteMap",
 * "Screen/Pane/Body/Stats/Row1/StatDURATION", "Screen/Pane/Body/Stats/SpeedProfile".
 */
@Composable
fun TripScreen(
    spec: TripScreenSpec,
    state: TripScreenState,
    modifier: Modifier = Modifier,
    dock: KitDockState = KitDockState("", 0),
    dockActions: KitDockActions = KitDockActions(),
    tag: String = "Screen",
    showDock: Boolean = true,
    onAllTrips: () -> Unit = {},
    onReset: (() -> Unit)? = null,
    runtimeRoute: (@Composable (width: Dp, height: Dp, markerPadding: Dp) -> KitTripRoute)? = null,
) {
    val s = spec.screen
    DriveFrame(s, dock, dockActions, tag, modifier, showDock) {
        val p = "$tag/Pane"
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(s.paneGap)) {
            Row(Modifier.fillMaxWidth().testTag("$p/Header"), horizontalArrangement = Arrangement.spacedBy(s.headerGap), verticalAlignment = Alignment.CenterVertically) {
                val subtitle = state.whenLabel + if (spec.subtitleNamesPlaces && state.places != null) " · ${state.places}" else ""
                DriveTitle(
                    s, state.title, subtitle, "$p/Header/Title", Modifier.weight(1f),
                    fillSubtitleWidth = s.group != ScreenGroup.LandscapeM,
                )
                DriveChipView(spec.allTrips, PenIcons.History, state.allTripsLabel, "$p/Header/AllTrips", onClick = onAllTrips, enabled = true) {
                    spec.chevron?.let { Icon(PenIcons.ChevronRight, null, Modifier.testTag("$p/Header/AllTrips/Chevron").size(it.size), tint = it.fill) }
                }
                val reset = spec.reset
                if (reset != null) DriveChipView(
                    reset, PenIcons.RestartAlt, state.resetLabel, "$p/Header/Reset",
                    onClick = onReset.takeIf { state.resetEnabled },
                    description = state.resetDescription,
                    enabled = state.resetEnabled,
                )
            }
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).testTag("$p/Body")) {
                val bodyWidth = maxWidth
                val bodyHeight = maxHeight
                val stats: @Composable (Modifier) -> Unit = { m -> TripStats(spec, state, "$p/Body/Stats", m) }
                val route: @Composable (Modifier) -> Unit = { m ->
                    Box(m.testTag("$p/Body/RouteMap")) {
                        KitDriveScale(s.tier) {
                            BoxWithConstraints(Modifier.fillMaxSize()) {
                        KitTripRouteMap(state.route, state.mapText, Modifier.fillMaxSize(), width = maxWidth, height = maxHeight, tag = "$p/Body/RouteMap/Kit", runtimeRoute = runtimeRoute)
                            }
                        }
                    }
                }
                if (s.portrait) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(s.bodyGap)) {
                        route(Modifier.fillMaxWidth().weight(1f))
                        stats(Modifier.fillMaxWidth().height(bodyHeight * spec.statsShare))
                    }
                } else {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(s.bodyGap)) {
                        route(Modifier.fillMaxHeight().weight(1f))
                        stats(Modifier.fillMaxHeight().width(bodyWidth * spec.statsShare))
                    }
                }
            }
        }
    }
}

/** The stats column: two rows of three tiles sharing the height the speed profile leaves, then the profile. */
@Composable
private fun TripStats(spec: TripScreenSpec, state: TripScreenState, tag: String, modifier: Modifier) {
    Column(modifier.testTag(tag), verticalArrangement = Arrangement.spacedBy(spec.statsGap)) {
        for (row in 0 until 2) {
            Row(Modifier.fillMaxWidth().weight(1f).testTag("$tag/Row${row + 1}"), horizontalArrangement = Arrangement.spacedBy(spec.rowGap)) {
                for (col in 0 until 3) {
                    val i = row * 3 + col
                    Box(Modifier.weight(1f).fillMaxHeight().testTag("$tag/Row${row + 1}/${TripStatNodes[i]}")) {
                        KitDriveScale(spec.screen.tier) {
                            KitTripStatTile(state.stats[i], Modifier.fillMaxSize(), tag = "$tag/Row${row + 1}/${TripStatNodes[i]}/Kit")
                        }
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(spec.profileHeight).testTag("$tag/SpeedProfile")) {
            KitDriveScale(spec.screen.tier) {
                KitSpeedProfile(state.speedBars, state.speedRange, Modifier.fillMaxSize(), width = ProfileKit.W, tag = "$tag/SpeedProfile/Kit")
            }
        }
    }
}
