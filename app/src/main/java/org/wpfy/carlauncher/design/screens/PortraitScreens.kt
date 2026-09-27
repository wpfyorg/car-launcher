package org.wpfy.carlauncher.design.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.wpfy.carlauncher.design.kit.*
import org.wpfy.carlauncher.design.theme.OverlayScales
import org.wpfy.carlauncher.design.theme.OverlayTier
import org.wpfy.carlauncher.design.theme.ProvideOverlayScaling
import org.wpfy.carlauncher.design.theme.overlayScalesFor
import org.wpfy.carlauncher.design.kit.PenScreens.PortraitL.GuideMap.MapPane as LMapPane
import org.wpfy.carlauncher.design.screens.PortraitLWideMediaCard as WideCard
import org.wpfy.carlauncher.design.screens.PortraitLWideMediaCard.Body.Transport.Pause as WidePause
import org.wpfy.carlauncher.design.screens.PortraitLWideMediaCard.Body.Transport.SkipNext as WideNext
import org.wpfy.carlauncher.design.screens.PortraitLWideMediaCard.Body.Transport.SkipPrevious as WidePrevious
import org.wpfy.carlauncher.design.kit.PenScreens.PortraitS.Idle.MapPane as SMapPane

/** The look of a portrait group's screens: its overlay tier, pane inset and radius, the HUD rules. */
@androidx.compose.runtime.Immutable
class PortraitScreenSpec internal constructor(
    val tier: OverlayTier,
    val fill: Color,
    val inset: Dp,
    val radius: Dp,
    val hud: PortraitHudSpec,
) {
    companion object {
        val PortraitS = PortraitScreenSpec(OverlayTier.Small, PenScreens.PortraitS.Idle.Fill, SMapPane.X, SMapPane.Radius, PortraitHudSpec.PortraitS)
        val PortraitL = PortraitScreenSpec(OverlayTier.Large, PenScreens.PortraitL.Idle.Fill, LMapPane.X, LMapPane.Radius, PortraitHudSpec.PortraitL)
    }
}

/**
 * The dock along the bottom edge and, above it, [content] in the area the dock leaves: the portrait
 * frame of every portrait screen. [content] gets the area's width and height; it is inset by the
 * Pen's margin from the start, end and top edges and, above the dock, by the same margin.
 * Tags: "Screen/Dock".
 */
@Composable
internal fun PortraitFrame(
    spec: PortraitScreenSpec,
    dock: KitDockState,
    dockActions: KitDockActions,
    tag: String,
    modifier: Modifier,
    content: @Composable (areaWidth: Dp, areaHeight: Dp) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize().background(spec.fill)) {
        val dockHeight = PenKit.DockPortrait.H
        val width = maxWidth
        Box(Modifier.offset(0.dp, maxHeight - dockHeight).testTag("$tag/Dock")) {
            KitDockPortrait(dock.clock, dock.notificationCount, dock.apps, width, actions = dockActions)
        }
        val areaWidth = maxWidth - spec.inset * 2
        val areaHeight = maxHeight - dockHeight - spec.inset * 2
        Box(Modifier.offset(spec.inset, spec.inset).size(areaWidth, areaHeight)) { content(areaWidth, areaHeight) }
    }
}

/**
 * A portrait map screen (Pen "Portrait S" 411×683dp and "Portrait L" 768×1024dp frames): the map pane
 * above the portrait dock, with the HUD over [map]. At Portrait S the pane has a second page, the
 * media page ([KitHudState.mediaPage] with [media]): the gradient pane with the media nodes. The
 * Drive Card ([driveCard]) sits under the map pane in both groups; the pane is as tall as what it leaves.
 * [cardHeight] must be positive whenever [driveCard] is set (the caller measures it, e.g. `driveWideHeight`);
 * [driveActions] are the card's callbacks.
 *
 * Test tags follow the Pen frame: "Screen/Dock", "Screen/MapPane" ("Screen/Pane" on the media page),
 * "Screen/MapPane/<element>", "Screen/DriveCard".
 */
@Composable
fun PortraitScreen(
    spec: PortraitScreenSpec,
    dock: KitDockState,
    hud: KitHudState,
    modifier: Modifier = Modifier,
    media: KitMediaState? = null,
    dockActions: KitDockActions = KitDockActions(),
    hudActions: KitHudActions = KitHudActions(),
    mediaActions: KitMediaActions = KitMediaActions(),
    tag: String = "Screen",
    /** The wide Drive Card under the map pane, [cardHeight] high (Portrait S and L). */
    driveCard: KitDriveState? = null,
    cardHeight: Dp = 0.dp,
    driveActions: KitDriveActions = KitDriveActions(),
    map: @Composable () -> Unit = {},
) {
    require(driveCard == null || cardHeight > 0.dp) { "driveCard needs a positive cardHeight, got $cardHeight" }
    PortraitFrame(spec, dock, dockActions, tag, modifier) { areaWidth, areaHeight ->
        PortraitPaneArea(
            spec, hud, media, hudActions, mediaActions, tag, areaWidth, areaHeight, overlayScalesFor(spec.tier),
            card = when {
                driveCard != null -> ({ KitDriveCardWide(driveCard, areaWidth, actions = driveActions, corner = spec.radius, height = cardHeight) })
                spec === PortraitScreenSpec.PortraitL && media != null -> ({ PortraitLMediaCard(media, "$tag/MediaCardWide", mediaActions) })
                else -> null
            },
            cardHeight = if (driveCard == null) WideCard.H else cardHeight,
            cardTag = if (driveCard == null) "MediaCardWide" else "DriveCard",
            map = map,
        )
    }
}

/**
 * The portrait map pane and, at Portrait L, the media card under it, in an area of [areaWidth] x [areaHeight]
 * (the dock is the shell's): the part of [PortraitScreen] above the dock. The card draws inside [card]'s box. [scales] are the overlay scales it
 * draws at; the Pen's are the tier's.
 */
@Composable
internal fun PortraitPaneArea(
    spec: PortraitScreenSpec,
    hud: KitHudState,
    media: KitMediaState?,
    hudActions: KitHudActions,
    mediaActions: KitMediaActions,
    tag: String,
    areaWidth: Dp,
    areaHeight: Dp,
    scales: OverlayScales = overlayScalesFor(spec.tier),
    /** The Drive Card under the pane, [cardHeight] high and as wide as the area; null leaves the pane the whole area. */
    card: (@Composable () -> Unit)? = null,
    cardHeight: Dp = 0.dp,
    /** The tag of the box holding [card]: the Drive Card's, or "MediaCardWide" for the Portrait L media card that stays without one. */
    cardTag: String = "DriveCard",
    map: @Composable () -> Unit = {},
) {
    val page = spec === PortraitScreenSpec.PortraitS && hud.mediaPage && media != null
    ProvideOverlayScaling(scales) {
        val paneHeight = areaHeight - if (card != null) cardHeight + spec.inset else 0.dp
        val paneTag = if (page) "$tag/Pane" else "$tag/MapPane"
        val shape = penShape(spec.radius)
        Box(
            Modifier.size(areaWidth, paneHeight).testTag(paneTag)
                .then(
                    if (page) Modifier.mediaArtBackground(media!!.art, PenScreens.PortraitS.MediaPage.Pane.Gradient, PenScreens.PortraitS.MediaPage.Pane.GradientRotation, shape, PortraitSMediaPageSpec.artistFill)
                    else Modifier.clip(shape),
                ),
        ) {
            if (page) FitPane(DpSize(PenScreens.PortraitS.MediaPage.Pane.W, PenScreens.PortraitS.MediaPage.Pane.H)) { scale -> MediaPaneBody(PortraitSMediaPageSpec, media!!, PenScreens.PortraitS.MediaPage.Pane.W, paneTag, mediaActions, fit = true, scale = scale) } else map()
            PortraitHud(hud, spec.hud, hudActions, tag = paneTag)
        }
        if (card != null) {
            Box(Modifier.offset(0.dp, paneHeight + spec.inset).testTag("$tag/$cardTag")) { card() }
        }
    }
}

/**
 * The Portrait S and L media screens (Pen "MediaScreen" of the portrait groups): the portrait dock
 * and the gradient pane above it, with the media pane's nodes centred in it.
 * Tags: "Screen/Dock", "Screen/Pane", "Screen/Pane/<Pen name>".
 */
@Composable
fun PortraitMediaScreen(
    spec: PortraitScreenSpec,
    dock: KitDockState,
    media: KitMediaState,
    modifier: Modifier = Modifier,
    dockActions: KitDockActions = KitDockActions(),
    mediaActions: KitMediaActions = KitMediaActions(),
    tag: String = "Screen",
    showDock: Boolean = true,
    fitPane: Boolean = false,
) {
    val small = spec === PortraitScreenSpec.PortraitS
    val gradient = if (small) PenScreens.PortraitS.MediaScreen.Pane.Gradient else PenScreens.PortraitL.MediaScreen.Pane.Gradient
    val rotation = if (small) PenScreens.PortraitS.MediaScreen.Pane.GradientRotation else PenScreens.PortraitL.MediaScreen.Pane.GradientRotation
    val paneSpec = if (small) PortraitSMediaSpec else PortraitLMediaSpec
    if (!showDock) {
        // Only the pane, filling what the caller gives it (the live shell draws the dock and leaves the margins).
        BoxWithConstraints(modifier.fillMaxSize().testTag("$tag/Pane").mediaArtBackground(media.art, gradient, rotation, penShape(spec.radius), paneSpec.artistFill)) {
            // The Pen pane: the frame less the margin on both sides and above and below, and the dock.
            val pen = if (small) DpSize(PenScreens.PortraitS.MediaScreen.W - spec.inset * 2, PenScreens.PortraitS.MediaScreen.H - PenKit.DockPortrait.H - spec.inset * 2)
            else DpSize(PenScreens.PortraitL.MediaScreen.W - spec.inset * 2, PenScreens.PortraitL.MediaScreen.H - PenKit.DockPortrait.H - spec.inset * 2)
            if (fitPane) FitPane(pen) { scale -> MediaPaneBody(paneSpec, media, pen.width, "$tag/Pane", mediaActions, fit = true, scale = scale) }
            else MediaPaneBody(paneSpec, media, maxWidth, "$tag/Pane", mediaActions)
        }
        return
    }
    PortraitFrame(spec, dock, dockActions, tag, modifier) { areaWidth, areaHeight ->
        Box(
            Modifier.size(areaWidth, areaHeight).testTag("$tag/Pane")
                .mediaArtBackground(media.art, gradient, rotation, penShape(spec.radius), paneSpec.artistFill),
        ) {
            MediaPaneBody(paneSpec, media, areaWidth, "$tag/Pane", mediaActions)
        }
    }
}

/**
 * The media card under the Portrait L map pane (Pen "Media Card Wide"): the artwork at the start and,
 * beside it, the source, title and artist at the top, the progress line, and the transport at the
 * bottom, spread over the artwork's height.
 */
@Composable
internal fun PortraitLMediaCard(media: KitMediaState, tag: String, actions: KitMediaActions) {
    Row(
        Modifier.fillMaxWidth()
            .mediaArtBackground(media.art, listOf(0f to WideCard.Fill, 1f to WideCard.Fill), 180f, penShape(WideCard.Radius), WideCard.Body.Text.Source.Fill)
            .padding(start = WideCard.PadStart, top = WideCard.PadTop, end = WideCard.PadEnd, bottom = WideCard.PadBottom),
        horizontalArrangement = Arrangement.spacedBy(WideCard.Gap),
    ) {
        Box(
            Modifier.testTag("$tag/Artwork").size(WideCard.Artwork.W, WideCard.Artwork.H)
                .penGradient(WideCard.Artwork.Gradient, WideCard.Artwork.GradientRotation, penShape(WideCard.Artwork.Radius)),
            contentAlignment = Alignment.Center,
        ) {
            if (media.art != null) {
                androidx.compose.foundation.Image(media.art, null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            } else {
                Icon(PenIcons.GraphicEq, null, Modifier.testTag("$tag/Artwork/Icon").size(WideCard.Artwork.Icon.W), tint = WideCard.Artwork.Icon.Fill)
            }
        }
        androidx.compose.foundation.layout.Column(
            Modifier.testTag("$tag/Body").weight(1f).height(WideCard.Artwork.H),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            androidx.compose.foundation.layout.Column(
                Modifier.testTag("$tag/Body/Text").fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(WideCard.Body.Text.Gap),
            ) {
                PenText(media.source, WideCard.Body.Text.Source.Style, WideCard.Body.Text.Source.Fill, Modifier.testTag("$tag/Body/Text/Source"))
                PenText(media.title, WideCard.Body.Text.Title.Style, WideCard.Body.Text.Title.Fill, Modifier.testTag("$tag/Body/Text/Title"))
                PenText(media.artist, WideCard.Body.Text.Artist.Style, WideCard.Body.Text.Artist.Fill, Modifier.testTag("$tag/Body/Text/Artist"))
            }
            Box(
                Modifier.testTag("$tag/Body/Progress").fillMaxWidth().height(WideCard.Body.Progress.H),
            ) {
                Box(
                    Modifier.testTag("$tag/Body/Progress/Track").fillMaxWidth().height(WideCard.Body.Progress.Track.H)
                        .clip(penShape(WideCard.Body.Progress.Track.Radius)).background(WideCard.Body.Progress.Track.Fill),
                ) {
                    Box(
                        Modifier.testTag("$tag/Body/Progress/Track/Done")
                            .fillMaxWidth(media.progress.coerceIn(0f, 1f)).height(WideCard.Body.Progress.Track.Done.H)
                            .clip(penShape(WideCard.Body.Progress.Track.Done.Radius)).background(WideCard.Body.Progress.Track.Done.Fill),
                    )
                }
            }
            Row(
                Modifier.testTag("$tag/Body/Transport").fillMaxWidth().height(WideCard.Body.Transport.H),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PenDisc(WidePrevious.W, WidePrevious.Fill, PenIcons.SkipPrevious, WidePrevious.Icon.W, WidePrevious.Icon.Fill, Modifier.testTag("$tag/Body/Transport/SkipPrevious"), iconModifier = Modifier.testTag("$tag/Body/Transport/SkipPrevious/Icon"), description = "Previous", onClick = actions.onPrevious, enabled = media.canSkipPrevious)
                PenDisc(WidePause.W, WidePause.Fill, if (media.playing) PenIcons.Pause else PenIcons.PlayArrow, WidePause.Icon.W, WidePause.Icon.Fill, Modifier.testTag("$tag/Body/Transport/Pause"), iconModifier = Modifier.testTag("$tag/Body/Transport/Pause/Icon"), description = "Play or pause", onClick = actions.onPlayPause, enabled = media.canPlayPause)
                PenDisc(WideNext.W, WideNext.Fill, PenIcons.SkipNext, WideNext.Icon.W, WideNext.Icon.Fill, Modifier.testTag("$tag/Body/Transport/SkipNext"), iconModifier = Modifier.testTag("$tag/Body/Transport/SkipNext/Icon"), description = "Next", onClick = actions.onNext, enabled = media.canSkipNext)
            }
        }
    }
}
