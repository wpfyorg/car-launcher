package org.wpfy.carlauncher.design.kit

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import org.wpfy.carlauncher.design.kit.PenKit.DriveCard.Music as MusicKit
import org.wpfy.carlauncher.design.kit.PenKit.DriveCard.Music.Row.Controls as ControlsKit
import org.wpfy.carlauncher.design.kit.PenKit.DriveCard.Speed as SpeedKit
import org.wpfy.carlauncher.design.kit.PenKit.DriveCard.Status as StatusKit
import org.wpfy.carlauncher.design.kit.PenKit.DriveCard.Tiles as TilesKit
import org.wpfy.carlauncher.design.kit.PenKit.DriveCard.Music.Row.Artwork as ArtworkKit
import org.wpfy.carlauncher.design.kit.PenKit.DriveCard.Speed.Pill as LimitPillKit
import org.wpfy.carlauncher.design.kit.PenKit.DriveCard.Speed.Pill.Limit as SignKit
import org.wpfy.carlauncher.design.kit.PenKit.DriveCard.Tiles.Trip as TileKit
import org.wpfy.carlauncher.design.kit.PenKit.SpeedPillCamera as CameraKit
import org.wpfy.carlauncher.design.kit.PenKit.SpeedPillConfirm as ConfirmKit
import org.wpfy.carlauncher.design.kit.PenKit.SpeedPillCritical as CriticalKit
import org.wpfy.carlauncher.design.kit.PenKit.DriveCard.Status.Trailing as TrailingKit
import org.wpfy.carlauncher.design.kit.PenKit.DriveCard as ProKit
import org.wpfy.carlauncher.design.kit.PenKit.DriveCard.Glow as ProGlow
import org.wpfy.carlauncher.design.kit.PenKit.DriveCardFree as FreeKit
import org.wpfy.carlauncher.design.kit.PenKit.DriveCardFree.Glow as FreeGlow
import org.wpfy.carlauncher.design.kit.PenKit.DriveCardFree.Status as FreeStatus
import org.wpfy.carlauncher.design.kit.PenKit.DriveCardFreeWide as FreeWideKit
import org.wpfy.carlauncher.design.kit.PenKit.DriveCardFreeWide.Glow as FreeWideGlow
import org.wpfy.carlauncher.design.kit.PenKit.DriveCardWide.Glow as WideGlow
import org.wpfy.carlauncher.design.kit.PenKit.DriveCardWide as WideKit
import org.wpfy.carlauncher.design.kit.PenKit.SpeedPillOver as OverKit
import org.wpfy.carlauncher.design.kit.PenKit.SpeedPillSent as SentKit
import org.wpfy.carlauncher.design.theme.FixedScale

/** What the pill under the Drive Card's speed shows: the road's limit, a camera ahead, or the camera confirmation. */
@Immutable
sealed interface KitSpeedPill {
    /** The limit sign and the road's name (the default); a blank road leaves the sign alone in the pill. */
    data class Limit(val limit: String, val road: String) : KitSpeedPill
    /**
     * A camera ahead (Pen "Speed Pill Camera"), with the mute button at its end when [KitDriveActions.onMute] is
     * set: [muted] shows the muted glyph (its tap then dismisses, like the map's alert button). [critical] is the
     * Pen "Speed Pill Critical" look (the last 50 m over the limit); the Drive Card sets it itself from
     * [KitDriveState.critical]. A null [limit] (no road limit and none on the camera) draws no sign. [description] is what the pill announces, as a live region, in place of its texts.
     */
    data class Camera(
        val limit: String?, val label: String, val muted: Boolean = false, val critical: Boolean = false, val description: String? = null,
    ) : KitSpeedPill
    /** A camera ahead while over the limit. */
    data class Over(val limit: String, val label: String) : KitSpeedPill
    /** After passing a camera: is it still there? */
    data class Confirm(val title: String, val timer: String) : KitSpeedPill
    /** The answer was sent; it can be undone. */
    data class Sent(val label: String, val undo: String) : KitSpeedPill
}

/** One trip tile: its label, value and unit. */
@Immutable
data class KitDriveTile(val label: String, val number: String, val unit: String)

/** What the Drive Card's music pill shows; the card draws no glow and no pill without it. */
@Immutable
data class KitDriveMedia(val title: String, val artist: String, val art: ImageBitmap?, val progress: Float, val playing: Boolean, val canSkipPrevious: Boolean = true, val canPlayPause: Boolean = true, val canSkipNext: Boolean = true)

/** The media source dots in the Drive Card's status row: [count] sources, [selected] the shown one. */
@Immutable
data class KitDriveDots(val count: Int, val selected: Int)

/** What the Drive Card shows. Null parts are not drawn. */
@Immutable
data class KitDriveState(
    val speed: String,
    val unit: String,
    /** What the speed announces (reading, limit, over the limit) in place of the value and unit texts; null reads them as they are. */
    val speedDescription: String? = null,
    /** Over the limit: the speed turns the over-limit pill's red. */
    val over: Boolean = false,
    val pill: KitSpeedPill? = null,
    /** The speed source's label (GPS, OBD-II). */
    val source: String,
    /** Park or drive, when the car reports its gear; null leaves the gear out. */
    val drive: Boolean? = null,
    val trip: KitDriveTile? = null,
    val average: KitDriveTile? = null,
    val altitude: KitDriveTile? = null,
    val media: KitDriveMedia? = null,
    /**
     * The alert glow's intensity, 0 (off) to 1: over the limit with a speed camera ahead, ramped by the camera's
     * distance. The card eases to a new value; it never blinks.
     */
    val alert: Float = 0f,
    /** The last-50-metre camera treatment, independent of the continuous glow ramp. */
    val critical: Boolean = alert >= 1f,
    /** The media source dots; null, or fewer than two sources, draws none. */
    val dots: KitDriveDots? = null,
    /** Fresh speed is at or below a known applicable limit. Over-limit red takes precedence. */
    val withinLimit: Boolean = false,
)

/** Taps from the Drive Card. */
class KitDriveActions(
    val onPrevious: () -> Unit = {},
    val onPlayPause: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onConfirmYes: () -> Unit = {},
    val onConfirmNo: () -> Unit = {},
    val onUndo: () -> Unit = {},
    /** The camera pill's mute button: mutes the alert, or dismisses it once muted; null draws no button. */
    val onMute: (() -> Unit)? = null,
    val onTrip: (() -> Unit)? = null,
)

/**
 * The Drive Card (Pen "Drive Card · Large", and its Small, Medium and XL sizes), fitted to [height] like the media
 * card: the whole card is the 1x kit card drawn at height / [PenKit.DriveCard.H], so every Pen size is this card at
 * its frame's height (text keeps the 11sp floor). The speed takes what the status row, tiles and music pill leave,
 * with the speed pill under it. With media playing, a glow of the album art's colour (the Pen glow without art) rises
 * from the bottom; without media the card is flat and has no music pill.
 *
 * [width] (real dp) widens the card past its Pen proportion (the speed, tiles and music pill fill it); null keeps it.
 * [corner] (real dp) fixes the card's outer corner whatever its height (beside the map: the map pane's corner); the
 * children at its corners keep nesting (child radius = card radius − their inset). Null scales the Pen's corner.
 *
 * [free] is the Free edition (Pen "Drive Card · Free"): the same card without the gear, the speed pill, the altitude
 * tile and the alert glow and critical stroke (those parts of [state] are ignored), on the Free tokens.
 */
@Composable
fun KitDriveCard(
    state: KitDriveState, height: Dp, modifier: Modifier = Modifier, actions: KitDriveActions = KitDriveActions(), corner: Dp? = null,
    width: Dp? = null, free: Boolean = false,
) {
    val scale = height / PenKit.DriveCard.H
    val realWidth = width ?: (PenKit.DriveCard.W * scale)
    Box(modifier.width(realWidth).height(height)) {
        FixedScale(scale) { DriveCardBody(state, actions, driveCardCorners(height, corner), realWidth / scale, if (free) FreeCard else ProCard) }
    }
}

/**
 * The wide Drive Card (Pen "Drive Card · Wide · Large" and "· Small"), under the map in portrait: the speed on the
 * left; the status row, tiles and music pill on the right. The whole card is the Pen card drawn at
 * width / [WideKit.W]; [height] (real dp) stretches it (large text), null keeps the Pen proportion. Its status row,
 * tiles, music pill and speed pill are the tall card's parts at the wide card's scale of them. [corner] and [free] as
 * [KitDriveCard] (Pen "Drive Card · Free · Wide").
 */
@Composable
fun KitDriveCardWide(
    state: KitDriveState, width: Dp, modifier: Modifier = Modifier, actions: KitDriveActions = KitDriveActions(), corner: Dp? = null,
    height: Dp? = null, free: Boolean = false,
) {
    val scale = width / WideKit.W
    val realHeight = height ?: (WideKit.H * scale)
    val spec = if (free) FreeWideCard else ProWideCard
    Box(modifier.width(width).height(realHeight)) {
        FixedScale(scale) { DriveCardWideBody(state, actions, scale, corner?.let { it / scale } ?: spec.radius, spec) }
    }
}

/**
 * The Drive Card's corner radii in its scope's dp: the card's and those of the children at its corners, each nested in
 * its parent (radius = parent radius − the child's inset), so they stay concentric at any corner; the album art keeps
 * at least the Pen's artwork radius, in the kit and on screen ([ArtworkKit.Radius] real dp), so it never reads square.
 */
@Immutable
internal data class DriveCardCorners(val card: Dp, val stroke: Dp, val tile: Dp, val music: Dp, val art: Dp)

/** The [DriveCardCorners] of a card [height] tall whose outer corner is [corner] real dp (null: the Pen's corner). */
internal fun driveCardCorners(height: Dp, corner: Dp?): DriveCardCorners {
    val scale = height / PenKit.DriveCard.H
    val card = if (corner == null) PenKit.DriveCard.Radius else corner / scale
    return nestedCorners(card, PenKit.DriveCard.CriticalStroke.X, PenKit.DriveCard.PadStart, PenKit.DriveCard.PadBottom, scale)
}

/**
 * Corners nested in a card of radius [card] whose critical stroke is inset [strokeInset], tiles [tileInset] and music
 * pill [musicInset], in the card's scope; [scale] is the scope's (real dp per unit), so the art's floor holds on screen.
 */
private fun nestedCorners(card: Dp, strokeInset: Dp, tileInset: Dp, musicInset: Dp, scale: Float): DriveCardCorners {
    fun nest(parent: Dp, inset: Dp) = (parent - inset).coerceAtLeast(0.dp)
    val music = nest(card, musicInset)
    return DriveCardCorners(
        card = card,
        stroke = nest(card, strokeInset),
        tile = nest(card, tileInset),
        music = music,
        // Concentric with the music panel, but never squarer than the Pen's artwork: beside the map the card's corner
        // stays the pane's while the insets grow with the card, and the nested radius would round to nothing; a small
        // card keeps the artwork's radius on screen.
        art = maxOf(nest(music, MusicKit.PadStart), ArtworkKit.Radius, ArtworkKit.Radius / scale),
    )
}

private const val C = "DriveCard"
private const val CW = "DriveCardWide"
private const val CF = "DriveCardFree"
private const val CFW = "DriveCardFreeWide"

/** The Pen glow box of a card variant. */
private class GlowBox(val x: Dp, val y: Dp, val w: Dp, val h: Dp, val gradient: List<Pair<Float, Color>>, val rotation: Float)

/**
 * What differs between the Pro Drive Card and the Free one at the card level (the parts inside are shared): the tag
 * root, the card's own tokens, the status row's height and, for the wide card, the speed column's. [alerts] draws the
 * alert glow, the critical stroke, the gear, the speed pill and the altitude tile.
 */
private class CardSpec(
    val root: String, val alerts: Boolean, val radius: Dp, val fill: Color, val padStart: Dp, val padTop: Dp, val padEnd: Dp, val padBottom: Dp,
    val gap: Dp, val glow: GlowBox, val statusH: Dp, val speedW: Dp = 0.dp, val speedGap: Dp = 0.dp,
)

private fun glowBox(x: Dp, y: Dp, w: Dp, h: Dp, gradient: List<Pair<Float, Color>>, rotation: Float) = GlowBox(x, y, w, h, gradient, rotation)

private val ProCard get() = CardSpec(
    C, true, ProKit.Radius, ProKit.Fill, ProKit.PadStart, ProKit.PadTop, ProKit.PadEnd, ProKit.PadBottom, ProKit.Gap,
    glowBox(ProGlow.X, ProGlow.Y, ProGlow.W, ProGlow.H, ProGlow.Gradient, ProGlow.GradientRotation), StatusKit.H,
)
private val FreeCard get() = CardSpec(
    CF, false, FreeKit.Radius, FreeKit.Fill, FreeKit.PadStart, FreeKit.PadTop, FreeKit.PadEnd, FreeKit.PadBottom, FreeKit.Gap,
    glowBox(FreeGlow.X, FreeGlow.Y, FreeGlow.W, FreeGlow.H, FreeGlow.Gradient, FreeGlow.GradientRotation), FreeStatus.H,
)
private val ProWideCard get() = CardSpec(
    CW, true, WideKit.Radius, WideKit.Fill, WideKit.PadStart, WideKit.PadTop, WideKit.PadEnd, WideKit.PadBottom, WideKit.Gap,
    glowBox(WideGlow.X, WideGlow.Y, WideGlow.W, WideGlow.H, WideGlow.Gradient, WideGlow.GradientRotation), StatusKit.H, WideKit.Speed.W, WideKit.Speed.Gap,
)
private val FreeWideCard get() = CardSpec(
    CFW, false, FreeWideKit.Radius, FreeWideKit.Fill, FreeWideKit.PadStart, FreeWideKit.PadTop, FreeWideKit.PadEnd, FreeWideKit.PadBottom, FreeWideKit.Gap,
    glowBox(FreeWideGlow.X, FreeWideGlow.Y, FreeWideGlow.W, FreeWideGlow.H, FreeWideGlow.Gradient, FreeWideGlow.GradientRotation), FreeStatus.H,
    FreeWideKit.Speed.W, FreeWideKit.Speed.Gap,
)

/** The Free card shows none of the Pro-only parts, whatever the state carries. */
private fun KitDriveState.forFree() = copy(drive = null, pill = null, altitude = null, alert = 0f, over = false)

@Composable
private fun DriveCardBody(card: KitDriveState, actions: KitDriveActions, corners: DriveCardCorners, width: Dp, spec: CardSpec) {
    val state = if (spec.alerts) card else card.forFree()
    val C = spec.root
    val shape = penShape(corners.card)
    Box(Modifier.pen(C).width(width).fillMaxHeight().clip(shape).background(spec.fill, shape)) {
        state.media?.let {
            val k = spec.glow
            DriveGlow(it.art, C, k.x, k.y, k.w, k.h, k.gradient, k.rotation)
        }
        if (spec.alerts) {
            val a = PenKit.DriveCard.AlertGlow
            DriveAlertGlow(state.alert, C, AlertGlowSpec(a.X, a.Y, a.W, a.H, a.Gradient, a.GradientCenterX, a.GradientCenterY, a.GradientSizeW, a.GradientSizeH))
            CriticalStroke(state, C, PenKit.DriveCard.CriticalStroke.Stroke, PenKit.DriveCard.CriticalStroke.StrokeColor, corners.stroke)
        }
        Column(
            Modifier.fillMaxSize().padding(start = spec.padStart, top = spec.padTop, end = spec.padEnd, bottom = spec.padBottom),
            verticalArrangement = Arrangement.spacedBy(spec.gap),
        ) {
            DriveStatus(state, C, spec.statusH)
            Column(
                Modifier.pen(C, "Speed").fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(SpeedKit.Gap, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DriveSpeed(state, actions, C, SpeedKit.Value.Style, SpeedKit.Unit.Style, PillSize.Tall)
            }
            DriveTiles(state, actions, C, corners)
            state.media?.let { DriveMusic(it, actions, corners, C) }
        }
    }
}

/**
 * The wide card's body in its Pen units ([scale] real dp each). Its status row, tiles, music pill and speed pill are the
 * tall card's parts at the wide card's scale of them (the ratio of the two cards' insets); the speed column's own
 * sizes (width, speed, pill height and sign) are the wide card's.
 */
@Composable
private fun DriveCardWideBody(wide: KitDriveState, actions: KitDriveActions, scale: Float, card: Dp, spec: CardSpec) {
    val state = if (spec.alerts) wide else wide.forFree()
    val CW = spec.root
    val shape = penShape(card)
    // One tall-card unit is this many wide-card units.
    val parts = WideKit.PadStart / PenKit.DriveCard.PadStart
    fun Dp.inParts() = this / parts
    val inner = nestedCorners(card.inParts(), WideKit.CriticalStroke.X.inParts(), WideKit.PadStart.inParts(), WideKit.PadBottom.inParts(), scale * parts)
    Box(Modifier.pen(CW).fillMaxSize().clip(shape).background(spec.fill, shape)) {
        state.media?.let {
            val k = spec.glow
            DriveGlow(it.art, CW, k.x, k.y, k.w, k.h, k.gradient, k.rotation)
        }
        if (spec.alerts) {
            val a = WideKit.AlertGlow
            DriveAlertGlow(state.alert, CW, AlertGlowSpec(a.X, a.Y, a.W, a.H, a.Gradient, a.GradientCenterX, a.GradientCenterY, a.GradientSizeW, a.GradientSizeH))
            CriticalStroke(state, CW, WideKit.CriticalStroke.Stroke, WideKit.CriticalStroke.StrokeColor, (card - WideKit.CriticalStroke.X).coerceAtLeast(0.dp))
        }
        FixedScale(scale * parts) {
            Row(
                Modifier.fillMaxSize().padding(
                    start = spec.padStart.inParts(), top = spec.padTop.inParts(), end = spec.padEnd.inParts(), bottom = spec.padBottom.inParts(),
                ),
                horizontalArrangement = Arrangement.spacedBy(spec.gap.inParts()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    Modifier.pen(CW, "Speed").width(spec.speedW.inParts()).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(spec.speedGap.inParts(), Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    DriveSpeed(
                        state, actions, CW, WideKit.Speed.Value.Style.over(parts), WideKit.Speed.Unit.Style.over(parts),
                        PillSize(WideKit.Speed.Pill.H.inParts(), WideKit.Speed.Pill.Limit.W.inParts(), WideKit.Speed.Pill.Limit.Value.Style.over(parts)),
                    )
                }
                val side = "$CW/Side"
                // The Pen spreads the side's rows over the card's height: status at the top, music pill at the bottom.
                Column(Modifier.pen(side).weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    DriveStatus(state, side, spec.statusH)
                    DriveTiles(state, actions, side, inner)
                    state.media?.let { DriveMusic(it, actions, inner, side) }
                }
            }
        }
    }
}

/** This style [k] times smaller: a wide-card size in tall-card units. */
private fun TextStyle.over(k: Float): TextStyle = copy(fontSize = fontSize / k, lineHeight = lineHeight / k)

/** The speed, its unit and the speed pill, in the caller's column; [root] is the card's tag root. */
@Composable
private fun DriveSpeed(state: KitDriveState, actions: KitDriveActions, root: String, value: TextStyle, unit: TextStyle, pillSize: PillSize) {
    PenText(
        state.speed, value, when {
            state.over -> PenKit.SpeedPillOver.StrokeColor
            state.withinLimit -> PenKit.EtaBar.Arrive.Fill
            else -> SpeedKit.Value.Fill
        },
        Modifier.pen(root, "Speed/Value").then(state.speedDescription?.let { d -> Modifier.clearAndSetSemantics { contentDescription = d } } ?: Modifier),
    )
    PenText(
        state.unit, unit, SpeedKit.Unit.Fill,
        Modifier.pen(root, "Speed/Unit").then(if (state.speedDescription != null) Modifier.clearAndSetSemantics {} else Modifier),
    )
    state.pill?.let { pill ->
        val shown = if (state.critical && pill is KitSpeedPill.Camera) pill.copy(critical = true) else pill
        SpeedPill(shown, actions, "$root/Speed/Pill", pillSize)
    }
}

/** The error stroke the card takes at full alert (Pen "Critical Stroke"). */
@Composable
private fun BoxScope.CriticalStroke(state: KitDriveState, root: String, stroke: Dp, color: Color, radius: Dp) {
    val shape = penShape(radius)
    Box(Modifier.pen(root, "CriticalStroke").matchParentSize().then(if (state.critical) Modifier.border(stroke, color, shape) else Modifier))
}

/** The trip tiles, when there are any; [root] is their parent's tag root. */
@Composable
private fun DriveTiles(state: KitDriveState, actions: KitDriveActions, root: String, corners: DriveCardCorners) {
    if (state.trip == null && state.average == null && state.altitude == null) return
    Row(Modifier.pen(root, "Tiles").fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TilesKit.Gap)) {
        state.trip?.let { DriveTile(it, "Trip", PenIcons.Route, corners, root, actions.onTrip) }
        state.average?.let { DriveTile(it, "Avg", PenIcons.Speed, corners, root) }
        state.altitude?.let { DriveTile(it, "Alt", PenIcons.Landscape, corners, root) }
    }
}

/** The Pen glow, in the album art's colour when there is art: the Pen's stops, alpha and place. */
@Composable
private fun DriveGlow(art: ImageBitmap?, root: String, x: Dp, y: Dp, w: Dp, h: Dp, gradient: List<Pair<Float, Color>>, rotation: Float) {
    val dominant = rememberMediaArtBackdrop(art)?.dominant?.let { Color(it) }
    val stops = gradient.map { (at, c) -> at to (dominant?.copy(alpha = c.alpha) ?: c) }
    Box(Modifier.offset(x, y).pen(root, "Glow").size(w, h).drawBehind { drawRect(penBrush(stops, rotation, size)) })
}

/** A card's Pen "Alert Glow": its box and radial gradient (centre and ellipse, fractions of the box). */
private class AlertGlowSpec(
    val x: Dp, val y: Dp, val w: Dp, val h: Dp, val gradient: List<Pair<Float, Color>>,
    val centerX: Float, val centerY: Float, val sizeW: Float, val sizeH: Float,
)

/** How long the alert glow takes to reach a new intensity. */
private const val AlertGlowMillis = 600

/**
 * The Pen "Alert Glow": its radial gradient (centre and ellipse from the Pen) over the album glow and under the content,
 * at [alert] intensity, eased so a step in the camera distance never blinks.
 */
@Composable
private fun DriveAlertGlow(alert: Float, root: String, k: AlertGlowSpec) {
    val intensity by animateFloatAsState(alert.coerceIn(0f, 1f), tween(AlertGlowMillis, easing = FastOutSlowInEasing), label = "alertGlow")
    Box(
        Modifier.offset(k.x, k.y).pen(root, "AlertGlow").size(k.w, k.h).drawBehind {
            if (intensity <= 0f) return@drawBehind
            val center = Offset(size.width * k.centerX, size.height * k.centerY)
            val rx = size.width * k.sizeW / 2f
            val ry = size.height * k.sizeH / 2f
            val brush = Brush.radialGradient(*k.gradient.toTypedArray(), center = center, radius = rx)
            // A circle of radius rx squeezed to the Pen's ellipse; the rect is the box before the squeeze.
            val sy = ry / rx
            scale(1f, sy, pivot = center) {
                drawRect(brush, topLeft = Offset(0f, center.y - center.y / sy), size = Size(size.width, size.height / sy), alpha = intensity)
            }
        },
    )
}

/**
 * The status row: the gear at the start; at the end the speed source and the media source dots. The row keeps the
 * Pen height without the gear, so the speed does not move. [root] is its parent's tag root.
 */
@Composable
private fun DriveStatus(state: KitDriveState, root: String, minHeight: Dp = StatusKit.H) {
    val C = root
    Row(
        Modifier.pen(C, "Status").fillMaxWidth().heightIn(min = minHeight),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
    ) {
        val drive = state.drive
        if (drive != null) {
            Row(Modifier.pen(C, "Status/Gear"), horizontalArrangement = Arrangement.spacedBy(StatusKit.Gear.Gap), verticalAlignment = Alignment.CenterVertically) {
                // The engaged gear takes the bright fill, the other the dim one.
                PenText("P", StatusKit.Gear.Park.Style, if (drive) StatusKit.Gear.Park.Fill else StatusKit.Gear.Drive.Fill, Modifier.pen(C, "Status/Gear/Park"))
                PenText("D", StatusKit.Gear.Drive.Style, if (drive) StatusKit.Gear.Drive.Fill else StatusKit.Gear.Park.Fill, Modifier.pen(C, "Status/Gear/Drive"))
            }
        } else {
            Box(Modifier)
        }
        Row(Modifier.pen(C, "Status/Trailing"), horizontalArrangement = Arrangement.spacedBy(TrailingKit.Gap), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.pen(C, "Status/Trailing/Source"), horizontalArrangement = Arrangement.spacedBy(TrailingKit.Source.Gap), verticalAlignment = Alignment.CenterVertically) {
                Icon(TrailingKit.Source.Icon.Vector, null, Modifier.pen(C, "Status/Trailing/Source/Icon").size(TrailingKit.Source.Icon.W), tint = TrailingKit.Source.Icon.Fill)
                PenText(state.source, TrailingKit.Source.Label.Style, TrailingKit.Source.Label.Fill, Modifier.pen(C, "Status/Trailing/Source/Label"))
            }
            val dots = state.dots
            if (dots != null && dots.count > 1) {
                Row(Modifier.pen(C, "Status/Trailing/Dots"), horizontalArrangement = Arrangement.spacedBy(TrailingKit.Dots.Gap), verticalAlignment = Alignment.CenterVertically) {
                    repeat(dots.count) { i ->
                        val selected = i == dots.selected
                        // The Pen draws the selected dot first; its measures stand for every dot.
                        val name = if (i == 0) "Selected" else if (i == 1) "Other" else "Other$i"
                        Box(
                            Modifier.pen(C, "Status/Trailing/Dots/$name").size(TrailingKit.Dots.Selected.W, TrailingKit.Dots.Selected.H)
                                .background(if (selected) TrailingKit.Dots.Selected.Fill else TrailingKit.Dots.Other.Fill, penShape(TrailingKit.Dots.Selected.W)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.DriveTile(tile: KitDriveTile, name: String, icon: ImageVector, corners: DriveCardCorners, root: String, onClick: (() -> Unit)? = null) {
    val C = root
    // The three tiles are built alike in the Pen; the Trip tile carries their measures.
    val shape = penShape(corners.tile)
    Column(
        Modifier.pen(C, "Tiles/$name").weight(1f).clip(shape).background(TileKit.Fill, shape).border(TileKit.Stroke, TileKit.StrokeColor, shape)
            .then(onClick?.let { Modifier.clickable(role = Role.Button, onClick = it) } ?: Modifier)
            .padding(start = TileKit.PadStart, top = TileKit.PadTop, end = TileKit.PadEnd, bottom = TileKit.PadBottom),
        verticalArrangement = Arrangement.spacedBy(TileKit.Gap),
    ) {
        Row(Modifier.pen(C, "Tiles/$name/Label"), horizontalArrangement = Arrangement.spacedBy(TileKit.Label.Gap), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.pen(C, "Tiles/$name/Label/Icon").size(TileKit.Label.Icon.W), tint = TileKit.Label.Icon.Fill)
            PenText(tile.label, TileKit.Label.Text.Style, TileKit.Label.Text.Fill, Modifier.pen(C, "Tiles/$name/Label/Text"))
        }
        Row(Modifier.pen(C, "Tiles/$name/Value"), horizontalArrangement = Arrangement.spacedBy(TileKit.Value.Gap), verticalAlignment = Alignment.Bottom) {
            PenText(tile.number, TileKit.Value.Number.Style, TileKit.Value.Number.Fill, Modifier.pen(C, "Tiles/$name/Value/Number"))
            PenText(tile.unit, TileKit.Value.Unit.Style, TileKit.Value.Unit.Fill, Modifier.pen(C, "Tiles/$name/Value/Unit"))
        }
    }
}

@Composable
private fun DriveMusic(media: KitDriveMedia, actions: KitDriveActions, corners: DriveCardCorners, root: String) {
    val C = root
    val shape = penShape(corners.music)
    Column(
        Modifier.pen(C, "Music").fillMaxWidth().clip(shape).background(MusicKit.Fill, shape).border(MusicKit.Stroke, MusicKit.StrokeColor, shape)
            .padding(start = MusicKit.PadStart, top = MusicKit.PadTop, end = MusicKit.PadEnd, bottom = MusicKit.PadBottom),
        verticalArrangement = Arrangement.spacedBy(MusicKit.Gap),
    ) {
        Row(Modifier.pen(C, "Music/Row").fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MusicKit.Row.Gap), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.pen(C, "Music/Row/Artwork").size(ArtworkKit.W, ArtworkKit.H).penGradient(ArtworkKit.Gradient, ArtworkKit.GradientRotation, penShape(corners.art)),
                contentAlignment = Alignment.Center,
            ) {
                if (media.art != null) Image(media.art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Icon(ArtworkKit.Note.Vector, null, Modifier.pen(C, "Music/Row/Artwork/Note").size(ArtworkKit.Note.W), tint = ArtworkKit.Note.Fill)
            }
            Column(Modifier.pen(C, "Music/Row/Text").weight(1f), verticalArrangement = Arrangement.spacedBy(MusicKit.Row.Text.Gap)) {
                PenText(media.title, MusicKit.Row.Text.Title.Style, MusicKit.Row.Text.Title.Fill, Modifier.pen(C, "Music/Row/Text/Title").fillMaxWidth())
                PenText(media.artist, MusicKit.Row.Text.Artist.Style, MusicKit.Row.Text.Artist.Fill, Modifier.pen(C, "Music/Row/Text/Artist").fillMaxWidth())
            }
            Row(Modifier.pen(C, "Music/Row/Controls"), verticalAlignment = Alignment.CenterVertically) {
                PenDisc(
                    ControlsKit.SkipPrevious.W, Color.Transparent, ControlsKit.SkipPrevious.Icon.Vector, ControlsKit.SkipPrevious.Icon.W, ControlsKit.SkipPrevious.Icon.Fill,
                    Modifier.pen(C, "Music/Row/Controls/SkipPrevious"), iconModifier = Modifier.pen(C, "Music/Row/Controls/SkipPrevious/Icon"),
                    description = "Previous", onClick = actions.onPrevious, enabled = media.canSkipPrevious,
                )
                PenDisc(
                    ControlsKit.Pause.W, ControlsKit.Pause.Fill, if (media.playing) ControlsKit.Pause.Icon.Vector else PenIcons.PlayArrow, ControlsKit.Pause.Icon.W, ControlsKit.Pause.Icon.Fill,
                    Modifier.pen(C, "Music/Row/Controls/Pause"), iconModifier = Modifier.pen(C, "Music/Row/Controls/Pause/Icon"),
                    description = "Play or pause", onClick = actions.onPlayPause, enabled = media.canPlayPause,
                )
                PenDisc(
                    ControlsKit.SkipNext.W, Color.Transparent, ControlsKit.SkipNext.Icon.Vector, ControlsKit.SkipNext.Icon.W, ControlsKit.SkipNext.Icon.Fill,
                    Modifier.pen(C, "Music/Row/Controls/SkipNext"), iconModifier = Modifier.pen(C, "Music/Row/Controls/SkipNext/Icon"),
                    description = "Next", onClick = actions.onNext, enabled = media.canSkipNext,
                )
            }
        }
        Box(Modifier.pen(C, "Music/Track").fillMaxWidth().height(MusicKit.Track.H).clip(penShape(MusicKit.Track.Radius)).background(MusicKit.Track.Fill)) {
            Box(Modifier.pen(C, "Music/Track/Done").fillMaxWidth(media.progress.coerceIn(0f, 1f)).fillMaxHeight().clip(penShape(MusicKit.Track.Done.Radius)).background(MusicKit.Track.Done.Fill))
        }
    }
}

// --- Speed pill -----------------------------------------------------------------------------------

/**
 * The look of a limit-sign pill (the default, camera, critical and over-limit pills share their build). [muteFill]
 * is the mute glyph's tint of a pill that has the mute button.
 */
private class SignPillLook(
    val fill: Color, val stroke: Color, val padStart: Dp, val padEnd: Dp, val gap: Dp,
    val text: TextStyle, val textFill: Color, val iconFill: Color?, val muteFill: Color? = null,
)

private val LimitLook get() = SignPillLook(LimitPillKit.Fill, LimitPillKit.StrokeColor, LimitPillKit.PadStart, LimitPillKit.PadEnd, LimitPillKit.Gap, LimitPillKit.Road.Style, LimitPillKit.Road.Fill, null)
private val CameraLook get() = SignPillLook(CameraKit.Fill, CameraKit.StrokeColor, CameraKit.PadStart, CameraKit.PadEnd, CameraKit.Gap, CameraKit.Label.Style, CameraKit.Label.Fill, CameraKit.Icon.Fill, CameraKit.Mute.Icon.Fill)
private val CriticalLook get() = SignPillLook(CriticalKit.Fill, CriticalKit.StrokeColor, CriticalKit.PadStart, CriticalKit.PadEnd, CriticalKit.Gap, CriticalKit.Label.Style, CriticalKit.Label.Fill, CriticalKit.Icon.Fill, CriticalKit.Mute.Icon.Fill)
private val OverLook get() = SignPillLook(OverKit.Fill, OverKit.StrokeColor, OverKit.PadStart, OverKit.PadEnd, OverKit.Gap, OverKit.Label.Style, OverKit.Label.Fill, OverKit.Icon.Fill)

/** The mute button's state on a camera pill. */
private class PillMute(val muted: Boolean, val onClick: () -> Unit)

/**
 * The speed pill's height and limit sign: the tall card's ([Tall]) or the wide card's, whose pill is lower with a
 * smaller sign. Everything else in the pill is the same.
 */
@Immutable
private class PillSize(val height: Dp, val sign: Dp, val signValue: TextStyle) {
    companion object {
        val Tall get() = PillSize(SpeedKit.Pill.H, SignKit.W, SignKit.Value.Style)
    }
}

/**
 * The speed pill (Pen "Pill" of the Drive Card and "Speed Pill <state> · Large"): every state is a pill of the Pen's
 * height, so the speed above it does not move when the state changes. [tag] is its test-tag root.
 */
@Composable
fun KitSpeedPill(pill: KitSpeedPill, actions: KitDriveActions = KitDriveActions(), tag: String = "SpeedPill") {
    SpeedPill(pill, actions, tag, PillSize.Tall)
}

@Composable
private fun SpeedPill(pill: KitSpeedPill, actions: KitDriveActions, tag: String, size: PillSize) {
    when (pill) {
        is KitSpeedPill.Limit -> SignPill(LimitLook, pill.limit, pill.road, "Road", tag, size)
        is KitSpeedPill.Camera -> SignPill(
            if (pill.critical) CriticalLook else CameraLook, pill.limit, pill.label, "Label", tag, size,
            actions.onMute?.let { PillMute(pill.muted, it) }, pill.description,
        )
        is KitSpeedPill.Over -> SignPill(OverLook, pill.limit, pill.label, "Label", tag, size)
        is KitSpeedPill.Confirm -> ConfirmPill(pill, actions, tag, size)
        is KitSpeedPill.Sent -> SentPill(pill, actions, tag, size)
    }
}

@Composable
private fun PillFrame(tag: String, size: PillSize, fill: Color, stroke: Color, padStart: Dp, padEnd: Dp, gap: Dp, content: @Composable RowScope.() -> Unit) {
    val shape = penShape(SpeedKit.Pill.Radius)
    Row(
        Modifier.testTagPath(tag).height(size.height).clip(shape).background(fill, shape).border(SpeedKit.Pill.Stroke, stroke, shape)
            .padding(start = padStart, end = padEnd),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

private fun Modifier.testTagPath(tag: String): Modifier = pen(tag)

@Composable
private fun SignPill(look: SignPillLook, limit: String?, text: String, textName: String, tag: String, size: PillSize, mute: PillMute? = null, liveDescription: String? = null) {
    // The mute button ends a camera pill at the Pen's end inset; without it the pill ends like the limit pill. A sign
    // alone is a circle: the pill ends at the sign's inset (Pen "Speed Pill Limit Sign").
    val padEnd = when {
        text.isBlank() -> PenKit.SpeedPillLimitSign.PadEnd
        look.muteFill != null && mute == null -> LimitPillKit.PadEnd
        else -> look.padEnd
    }
    PillFrame(tag, size, look.fill, look.stroke, look.padStart, padEnd, look.gap) {
        val shape = penShape(SignKit.Radius)
        if (limit != null) {
            Box(Modifier.pen(tag, "Limit").size(size.sign).background(SignKit.Fill, shape).border(SignKit.Stroke, SignKit.StrokeColor, shape), contentAlignment = Alignment.Center) {
                PenText(limit, size.signValue, SignKit.Value.Fill, Modifier.pen(tag, "Limit/Value"))
            }
        }
        if (look.iconFill != null) Icon(PenKit.SpeedPillCamera.Icon.Vector, null, Modifier.pen(tag, "Icon").size(PenKit.SpeedPillCamera.Icon.W), tint = look.iconFill)
        // A blank road leaves the sign alone: the pill hugs it (Pen "Speed Pill Limit Sign"). The text takes what is
        // left of a full-width pill (large text) and ellipsizes, so the sign and the mute button keep their size.
        if (text.isNotBlank()) PenText(
            text, look.text, look.textFill,
            Modifier.pen(tag, textName).weight(1f, fill = false).then(
                liveDescription?.let { d -> Modifier.clearAndSetSemantics { contentDescription = d; liveRegion = LiveRegionMode.Polite } } ?: Modifier,
            ),
        )
        val muteFill = look.muteFill
        if (mute != null && muteFill != null) {
            PenDisc(
                CameraKit.Mute.W, Color.Transparent, if (mute.muted) PenIcons.VolumeOff else CameraKit.Mute.Icon.Vector, CameraKit.Mute.Icon.W, muteFill,
                Modifier.pen(tag, "Mute"), iconModifier = Modifier.pen(tag, "Mute/Icon"),
                description = if (mute.muted) "Dismiss camera alert" else "Mute camera alert", onClick = mute.onClick,
            )
        }
    }
}

@Composable
private fun ConfirmPill(pill: KitSpeedPill.Confirm, actions: KitDriveActions, tag: String, size: PillSize) {
    PillFrame(tag, size, ConfirmKit.Fill, ConfirmKit.StrokeColor, ConfirmKit.PadStart, ConfirmKit.PadEnd, ConfirmKit.Gap) {
        Column(Modifier.pen(tag, "Text"), verticalArrangement = Arrangement.spacedBy(ConfirmKit.Text.Gap)) {
            PenText(pill.title, ConfirmKit.Text.Title.Style, ConfirmKit.Text.Title.Fill, Modifier.pen(tag, "Text/Title"))
            PenText(pill.timer, ConfirmKit.Text.Timer.Style, ConfirmKit.Text.Timer.Fill, Modifier.pen(tag, "Text/Timer"))
        }
        PenDisc(ConfirmKit.No.W, ConfirmKit.No.Fill, ConfirmKit.No.Icon.Vector, ConfirmKit.No.Icon.W, ConfirmKit.No.Icon.Fill, Modifier.pen(tag, "No"), iconModifier = Modifier.pen(tag, "No/Icon"), description = "Camera is gone", onClick = actions.onConfirmNo)
        PenDisc(ConfirmKit.Yes.W, ConfirmKit.Yes.Fill, ConfirmKit.Yes.Icon.Vector, ConfirmKit.Yes.Icon.W, ConfirmKit.Yes.Icon.Fill, Modifier.pen(tag, "Yes"), iconModifier = Modifier.pen(tag, "Yes/Icon"), description = "Camera is still there", onClick = actions.onConfirmYes)
    }
}

@Composable
private fun SentPill(pill: KitSpeedPill.Sent, actions: KitDriveActions, tag: String, size: PillSize) {
    PillFrame(tag, size, SentKit.Fill, SentKit.StrokeColor, SentKit.PadStart, SentKit.PadEnd, SentKit.Gap) {
        Icon(SentKit.Icon.Vector, null, Modifier.pen(tag, "Icon").size(SentKit.Icon.W), tint = SentKit.Icon.Fill)
        PenText(pill.label, SentKit.Label.Style, SentKit.Label.Fill, Modifier.pen(tag, "Label"))
        val shape = penShape(SentKit.Undo.Radius)
        Box(
            Modifier.pen(tag, "Undo").clip(shape).background(SentKit.Undo.Fill, shape).clickable(role = Role.Button, onClick = actions.onUndo)
                .padding(start = SentKit.Undo.PadStart, top = SentKit.Undo.PadTop, end = SentKit.Undo.PadEnd, bottom = SentKit.Undo.PadBottom),
        ) {
            PenText(pill.undo, SentKit.Undo.Label.Style, SentKit.Undo.Label.Fill, Modifier.pen(tag, "Undo/Label"))
        }
    }
}

// --- Map search ---------------------------------------------------------------------------------------

/**
 * The map's search field (Pen "Map Search · Large"): a scrim pill with the search glyph and [placeholder], [width]
 * wide (the HUD gives it the turn card's width); a tap opens destination search.
 */
@Composable
fun KitMapSearch(placeholder: String, width: Dp, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val c = "MapSearch"
    val shape = penShape(PenKit.MapSearch.Radius)
    Row(
        modifier.pen(c).width(width).height(PenKit.MapSearch.H).clip(shape).background(PenKit.MapSearch.Fill, shape)
            .penClick(onClick).padding(start = PenKit.MapSearch.PadStart, end = PenKit.MapSearch.PadEnd),
        horizontalArrangement = Arrangement.spacedBy(PenKit.MapSearch.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(PenKit.MapSearch.Icon.Vector, null, Modifier.pen(c, "Icon").size(PenKit.MapSearch.Icon.W), tint = PenKit.MapSearch.Icon.Fill)
        PenText(placeholder, PenKit.MapSearch.Placeholder.Style, PenKit.MapSearch.Placeholder.Fill, Modifier.pen(c, "Placeholder"))
    }
}
