package org.wpfy.carlauncher.design.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.wpfy.carlauncher.design.kit.*
import org.wpfy.carlauncher.design.theme.OverlayScales
import org.wpfy.carlauncher.design.theme.OverlayTier
import org.wpfy.carlauncher.design.theme.ProvideOverlayScaling
import org.wpfy.carlauncher.design.theme.overlayScalesFor

/** What the media card shows. */
@Immutable
data class KitMediaState(
    val source: String,
    val title: String,
    val artist: String,
    val art: ImageBitmap?,
    val elapsed: String,
    val total: String,
    val progress: Float,
    val playing: Boolean,
    /** Transport controls the session offers; the others draw disabled. */
    val canSkipPrevious: Boolean = true,
    val canPlayPause: Boolean = true,
    val canSkipNext: Boolean = true,
    /** The source app's own icon, drawn in place of the Pen's glyph on the media screens' source line. */
    val sourceIcon: ImageBitmap? = null,
    /** The media screens' progress bar takes drag and tap to seek (with [KitMediaActions.onSeek]); otherwise it is inert. */
    val canSeek: Boolean = false,
    /** The elapsed text at a dragged fraction (0 to 1) while seeking; null keeps [elapsed]. */
    val elapsedAt: ((Float) -> String)? = null,
)

/**
 * What a landscape map screen takes from its Pen group: the overlay tier it draws at, the dock's scale,
 * the pane's place, look and inset, and the margin between the pane's edge and the HUD (real dp).
 */
@Immutable
class MapScreenSpec internal constructor(
    val tier: OverlayTier,
    val dockScale: Float,
    val paneX: Dp,
    val paneY: Dp,
    val paneRadius: Dp,
    val fill: Color,
    val margin: Dp,
    val scrim: Color,
) {
    companion object {
        val LandscapeM = MapScreenSpec(
            OverlayTier.Medium, PenKit.DockScale.Medium, PenScreens.LandscapeM.GuideMedia.MapPane.X, PenScreens.LandscapeM.GuideMedia.MapPane.Y,
            PenScreens.LandscapeM.GuideMedia.MapPane.Radius, PenScreens.LandscapeM.GuideMedia.Fill, PenScreens.LandscapeM.GuideMedia.MapPane.Y,
            PenScreens.LandscapeM.ReportSheet.MapPane.Scrim.Fill,
        )
        val LandscapeL = MapScreenSpec(
            OverlayTier.Large, PenKit.DockScale.Large, PenScreens.LandscapeL.GuideMedia.MapPane.X, PenScreens.LandscapeL.GuideMedia.MapPane.Y,
            PenScreens.LandscapeL.GuideMedia.MapPane.Radius, PenScreens.LandscapeL.GuideMedia.Fill, PenScreens.LandscapeL.GuideMap.MapPane.TurnCard.X,
            PenScreens.LandscapeL.ReportSheet.MapPane.Scrim.Fill,
        )
        val LandscapeXL = MapScreenSpec(
            OverlayTier.XLarge, PenKit.DockScale.XLarge, PenScreens.LandscapeXL.GuideMedia.MapPane.X, PenScreens.LandscapeXL.GuideMedia.MapPane.Y,
            PenScreens.LandscapeXL.GuideMedia.MapPane.Radius, PenScreens.LandscapeXL.GuideMedia.Fill, PenScreens.LandscapeXL.GuideMap.MapPane.TurnCard.X,
            PenScreens.LandscapeXL.ReportSheet.MapPane.Scrim.Fill,
        )
    }
}

/**
 * A Landscape M screen (Pen "Cluster · Landscape M", 805×480dp frames and their group): the dock along
 * the start edge, the map pane beside it with the HUD over [map], and, when [media] is set, the media
 * card at the end edge; with [drive] set the Drive Card takes the media card's place (it shows the speed, the limit and
 * the camera pill, so the HUD should leave its cluster and camera alert out).
 * Rules: dock at the group's dock scale, as tall as the screen; the pane starts after the dock gap and
 * is inset by the Pen's margin from the top, end and bottom edges; the media card is as tall as the
 * pane, one inset from the end edge, and the pane ends one inset before it.
 *
 * Test tags follow the Pen frame: "Screen/Dock", "Screen/MapPane", "Screen/MapPane/<element>",
 * "Screen/MediaCard".
 */
@Composable
fun LandscapeMScreen(
    dock: KitDockState,
    hud: KitHudState,
    modifier: Modifier = Modifier,
    media: KitMediaState? = null,
    dockActions: KitDockActions = KitDockActions(),
    hudActions: KitHudActions = KitHudActions(),
    mediaActions: KitMediaActions = KitMediaActions(),
    tag: String = "Screen",
    spec: MapScreenSpec = MapScreenSpec.LandscapeM,
    drive: KitDriveState? = null,
    driveActions: KitDriveActions = KitDriveActions(),
    map: @Composable () -> Unit = {},
) {
    BoxWithConstraints(modifier.fillMaxSize().background(spec.fill)) {
        val height = maxHeight
        val width = maxWidth
        val inset = spec.paneY
        Box(Modifier.testTag("$tag/Dock")) {
            KitDockLandscape(dock.clock, dock.notificationCount, dock.apps, height, spec.dockScale, actions = dockActions)
        }
        LandscapeMPaneArea(
            spec, hud, hudActions, tag, width - spec.paneX - inset, height - inset * 2, overlayScalesFor(spec.tier),
            card = if (drive != null) {
                { cardHeight -> KitDriveCard(drive, cardHeight, actions = driveActions, corner = spec.paneRadius) }
            } else if (media != null) {
                { cardHeight ->
                    KitMediaCard(
                        media.source, media.title, media.artist, media.art, media.elapsed, media.total, media.progress, media.playing,
                        height = cardHeight, onPrevious = mediaActions.onPrevious, onPlayPause = mediaActions.onPlayPause, onNext = mediaActions.onNext,
                        canSkipPrevious = media.canSkipPrevious, canPlayPause = media.canPlayPause, canSkipNext = media.canSkipNext,
                    )
                }
            } else {
                null
            },
            modifier = Modifier.offset(spec.paneX, spec.paneY),
            cardTag = if (drive != null) "DriveCard" else "MediaCard",
            map = map,
        )
    }
}

/**
 * The map pane with the HUD over [map] and, when there is a [card], the media card at the end edge, in an area
 * of [areaWidth] x [areaHeight] (the dock is the shell's): the part of [LandscapeMScreen] beside the dock. The
 * card is [areaHeight] high ([card] gets that height) and [PenKit.MediaCard.W] by that height's scale wide; the pane
 * takes the rest less one inset. [scales] are the overlay scales it draws at; the Pen's are the tier's.
 * [cardWidthOverride] sets the width where the card is not the media card's proportion (the Drive Card). Tags: "<tag>/MapPane", "<tag>/<cardTag>".
 */
@Composable
internal fun LandscapeMPaneArea(
    spec: MapScreenSpec,
    hud: KitHudState,
    hudActions: KitHudActions,
    tag: String,
    areaWidth: Dp,
    areaHeight: Dp,
    scales: OverlayScales = overlayScalesFor(spec.tier),
    card: (@Composable (cardHeight: Dp) -> Unit)? = null,
    modifier: Modifier = Modifier,
    cardTag: String = "MediaCard",
    /** The card's width when it is not the media card's proportion (the Drive Card's, per group). */
    cardWidthOverride: Dp? = null,
    map: @Composable () -> Unit = {},
) {
    val inset = spec.paneY
    val cardWidth: Dp = if (card != null) cardWidthOverride ?: (PenKit.MediaCard.W * (areaHeight / PenKit.MediaCard.H)) else 0.dp
    val cardSpace = if (card != null) cardWidth + inset else 0.dp
    val paneWidth = areaWidth - cardSpace
    Box(modifier.size(areaWidth, areaHeight)) {
        ProvideOverlayScaling(scales) {
            Box(Modifier.size(paneWidth, areaHeight).testTag("$tag/MapPane").clip(penShape(spec.paneRadius))) {
                map()
                LandscapeMHud(hud, hudActions, tag = "$tag/MapPane", margin = spec.margin, scrim = spec.scrim, searchSpec = SearchPanelSpec.forTier(spec.tier))
            }
        }
        if (card != null) {
            Box(Modifier.offset(areaWidth - cardWidth, 0.dp).testTag("$tag/$cardTag")) { card(areaHeight) }
        }
    }
}

/** A Landscape L screen (Pen "Cluster · Landscape L", Standard and Ultrawide rows): the Landscape M arrangement at the Large tier. */
@Composable
fun LandscapeLScreen(
    dock: KitDockState,
    hud: KitHudState,
    modifier: Modifier = Modifier,
    media: KitMediaState? = null,
    dockActions: KitDockActions = KitDockActions(),
    hudActions: KitHudActions = KitHudActions(),
    mediaActions: KitMediaActions = KitMediaActions(),
    tag: String = "Screen",
    drive: KitDriveState? = null,
    driveActions: KitDriveActions = KitDriveActions(),
    map: @Composable () -> Unit = {},
) = LandscapeMScreen(dock, hud, modifier, media, dockActions, hudActions, mediaActions, tag, MapScreenSpec.LandscapeL, drive, driveActions, map)

/** A Landscape XL screen (Pen "Cluster · Landscape L", Ultrawide XL row): the Landscape M arrangement at the XLarge tier. */
@Composable
fun LandscapeXLScreen(
    dock: KitDockState,
    hud: KitHudState,
    modifier: Modifier = Modifier,
    media: KitMediaState? = null,
    dockActions: KitDockActions = KitDockActions(),
    hudActions: KitHudActions = KitHudActions(),
    mediaActions: KitMediaActions = KitMediaActions(),
    tag: String = "Screen",
    drive: KitDriveState? = null,
    driveActions: KitDriveActions = KitDriveActions(),
    map: @Composable () -> Unit = {},
) = LandscapeMScreen(dock, hud, modifier, media, dockActions, hudActions, mediaActions, tag, MapScreenSpec.LandscapeXL, drive, driveActions, map)

/** Taps from the media card. */
class KitMediaActions(
    val onPrevious: () -> Unit = {}, val onPlayPause: () -> Unit = {}, val onNext: () -> Unit = {},
    /** Taps on the media screens' source line (opens the source app); null leaves the line inert. */
    val onSource: (() -> Unit)? = null,
    /** Called once with the fraction (0 to 1) where a drag or tap on the media screens' progress bar ends; null leaves the bar inert. */
    val onSeek: ((Float) -> Unit)? = null,
)
