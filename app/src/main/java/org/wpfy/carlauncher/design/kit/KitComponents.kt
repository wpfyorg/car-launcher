package org.wpfy.carlauncher.design.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import org.wpfy.carlauncher.design.theme.CarColors
import org.wpfy.carlauncher.design.theme.LocalActiveOverlayScale
import org.wpfy.carlauncher.design.theme.OverlayTextFloor
import org.wpfy.carlauncher.design.theme.textGrowFor
import org.wpfy.carlauncher.design.theme.withFloor

/*
 * The HUD kit, 1:1 with the Pen components (see PenKit, generated from the Pen file). Every size,
 * inset, gap, radius, colour and type style comes from PenKit; nothing here is a design value of
 * its own. Each node carries a test tag "<Component>/<Pen node path>" so tests can compare its
 * bounds with the Pen geometry (PenKitReference). The components draw at kit units (1x); the
 * caller's TileScale / ControlScale scope scales them, and text is floored to 11sp real.
 */

/** Pen corner radius: 999 is the Pen's pill, anything else a fixed radius. */
internal fun penShape(radius: Dp): Shape =
    if (radius >= 999.dp) RoundedCornerShape(percent = 50) else RoundedCornerShape(radius)

/** Test tag of a Pen node: "<Component>/<path>", "<Component>" for the root. */
internal fun Modifier.pen(component: String, path: String = "."): Modifier =
    testTag(if (path == ".") component else "$component/$path")

/**
 * Pen text: the Pen's line box (its text frame height) as the line height, no font padding, the
 * glyphs centred in the line; raised to the 11sp real floor inside scaled scopes.
 */
@Composable
internal fun penStyle(style: TextStyle): TextStyle = style.withFloor(OverlayTextFloor.Secondary).penLine()

/**
 * Pen text at its exact Pen size whatever the user's font scale (the dock's clock and badge, which
 * the Pen keeps fixed in the large-text frames): the size in dp, converted through the font scale.
 */
@Composable
internal fun penFixedStyle(style: TextStyle): TextStyle {
    val density = LocalDensity.current
    return with(density) {
        style.copy(fontSize = style.fontSize.value.dp.toSp(), lineHeight = style.lineHeight.value.dp.toSp())
    }.penLine()
}

private fun TextStyle.penLine(): TextStyle = copy(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
)

@Composable
internal fun PenText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    /** Fixed: the Pen size whatever the font scale, no floor (see [penFixedStyle]). */
    fixed: Boolean = false,
    textAlign: TextAlign? = null,
    /** The text's frame height: larger text (a big font scale) shrinks until [maxLines] of it fit the frame. Null keeps the Pen's text as is. */
    fitHeight: Dp? = null,
) {
    val base = if (fixed) penFixedStyle(style) else penStyle(style)
    // Shrink factor that makes the laid-out text fit [fitHeight]: measured, so any font metrics are honoured.
    var factor by remember(text, base, fitHeight) { mutableStateOf(1f) }
    val haveHeight = fitHeight?.let { with(LocalDensity.current) { it.toPx() } }
    val fitted = if (fitHeight == null || base.lineHeight == TextUnit.Unspecified || factor >= 1f) base
    else base.copy(fontSize = base.fontSize * factor, lineHeight = base.lineHeight * factor)
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        style = fitted,
        color = color,
        maxLines = maxLines,
        softWrap = maxLines > 1,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = if (haveHeight == null) null else { layout ->
            val height = layout.size.height
            if (height > haveHeight + 0.5f && factor > MinFitFactor) factor = maxOf(MinFitFactor, factor * haveHeight / height * 0.98f)
        },
    )
}

private const val MinFitFactor = 0.2f

/** How much of a disc shows while its control is unavailable. */
private const val DisabledAlpha = 0.4f

/** A round Pen button: [size] disc of [fill], [icon] of [iconSize] centred, clickable without padding. */
@Composable
internal fun PenDisc(
    size: Dp,
    fill: Color,
    icon: ImageVector,
    iconSize: Dp,
    iconTint: Color,
    modifier: Modifier = Modifier,
    radius: Dp = 999.dp,
    stroke: Dp = 0.dp,
    strokeColor: Color = Color.Transparent,
    iconModifier: Modifier = Modifier,
    description: String? = null,
    onClick: (() -> Unit)? = null,
    /** False greys the disc out and stops its clicks (a transport control the media session does not offer). */
    enabled: Boolean = true,
    /** The least touch area (real dp, on screen) of a clickable disc: an invisible box of that size takes the tap and the description; 0 is the Pen's disc alone. */
    minTouch: Dp = 0.dp,
) {
    val shape = penShape(radius)
    val hit = if (minTouch > 0.dp && onClick != null) minTouch / LocalActiveOverlayScale.current.coerceAtLeast(0.01f) else size
    val outer = hit > size
    val visual = modifier
        .size(size)
        .then(if (enabled) Modifier else Modifier.alpha(DisabledAlpha))
        .clip(shape)
        .background(fill, shape)
        .then(if (stroke > 0.dp) Modifier.border(stroke, strokeColor, shape) else Modifier)
    val body: @Composable (Modifier) -> Unit = { m ->
        Box(m, contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = iconModifier.size(iconSize), tint = iconTint)
        }
    }
    if (outer) {
        Box(
            Modifier.size(hit)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick!!)
                .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
            contentAlignment = Alignment.Center,
        ) { body(visual) }
    } else {
        body(
            visual
                .then(if (onClick != null) Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick) else Modifier)
                .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
        )
    }
}

// --- Turn card ---------------------------------------------------------------------------------

/**
 * Width of the turn card and the camera alert in a tile scope of [tileScale] at the user's
 * [fontScale]: the kit width grown with the text (the font scale, and the 11sp floor where the tile
 * scale would take the road name under it), so the Pen's line breaks hold.
 */
fun kitCardWidth(tileScale: Float, fontScale: Float): Dp =
    PenKit.TurnCard.W * fontScale.coerceAtLeast(1f) * textGrowFor(tileScale, styleSp = 20f)

@Composable
fun KitTurnCard(
    distance: String,
    road: String,
    arrow: ImageVector,
    thenArrow: ImageVector?,
    modifier: Modifier = Modifier,
    width: Dp = PenKit.TurnCard.W,
    thenDescription: String? = null,
    /** Lines the road may take before it ends in an ellipsis; the Pen's Landscape S card keeps it to one. */
    roadLines: Int = 2,
) {
    val c = "TurnCard"
    Column(
        modifier = modifier
            .pen(c)
            .width(width)
            .clip(penShape(PenKit.TurnCard.Radius))
            .background(PenKit.TurnCard.Fill),
    ) {
        Row(
            modifier = Modifier
                .pen(c, "Main")
                .fillMaxWidth()
                .padding(start = PenKit.TurnCard.Main.PadStart, top = PenKit.TurnCard.Main.PadTop, end = PenKit.TurnCard.Main.PadEnd, bottom = PenKit.TurnCard.Main.PadBottom),
            horizontalArrangement = Arrangement.spacedBy(PenKit.TurnCard.Main.Gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(arrow, contentDescription = null, modifier = Modifier.pen(c, "Main/Arrow").size(PenKit.TurnCard.Main.Arrow.W), tint = PenKit.TurnCard.Main.Arrow.Fill)
            Column(
                modifier = Modifier.pen(c, "Main/Text").weight(1f),
                verticalArrangement = Arrangement.spacedBy(PenKit.TurnCard.Main.Text.Gap),
            ) {
                PenText(distance, PenKit.TurnCard.Main.Text.Distance.Style, PenKit.TurnCard.Main.Text.Distance.Fill, Modifier.pen(c, "Main/Text/Distance"))
                PenText(road, PenKit.TurnCard.Main.Text.Road.Style, PenKit.TurnCard.Main.Text.Road.Fill, Modifier.pen(c, "Main/Text/Road").fillMaxWidth(), maxLines = roadLines)
            }
        }
        if (thenArrow != null) {
            Row(
                modifier = Modifier
                    .pen(c, "Then")
                    .fillMaxWidth()
                    .background(PenKit.TurnCard.Then.Fill)
                    .padding(start = PenKit.TurnCard.Then.PadStart, top = PenKit.TurnCard.Then.PadTop, end = PenKit.TurnCard.Then.PadEnd, bottom = PenKit.TurnCard.Then.PadBottom)
                    .then(if (thenDescription != null) Modifier.semantics(mergeDescendants = true) { contentDescription = thenDescription } else Modifier),
                horizontalArrangement = Arrangement.spacedBy(PenKit.TurnCard.Then.Gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PenText(PenKit.TurnCard.Then.ThenLabel.Reference, PenKit.TurnCard.Then.ThenLabel.Style, PenKit.TurnCard.Then.ThenLabel.Fill, Modifier.pen(c, "Then/ThenLabel"))
                Icon(thenArrow, contentDescription = null, modifier = Modifier.pen(c, "Then/ThenArrow").size(PenKit.TurnCard.Then.ThenArrow.W), tint = PenKit.TurnCard.Then.ThenArrow.Fill)
            }
        }
    }
}

// --- ETA bar -----------------------------------------------------------------------------------

@Composable
fun KitEtaBar(
    arrival: String,
    remaining: String,
    modifier: Modifier = Modifier,
    endIcon: ImageVector = PenKit.EtaBar.Stop.Icon.Vector,
    /** The bar spans the width its slot gives it (narrow panes): the remaining time takes the slack. */
    fill: Boolean = false,
    onEnd: () -> Unit = {},
    /** What the End button announces; null leaves its icon and label to be read, as in the Pen. */
    endDescription: String? = null,
) {
    val c = "EtaBar"
    Row(
        modifier = modifier
            .pen(c)
            .height(PenKit.EtaBar.H)
            .clip(penShape(PenKit.EtaBar.Radius))
            .background(PenKit.EtaBar.Fill)
            .padding(start = PenKit.EtaBar.PadStart, top = PenKit.EtaBar.PadTop, end = PenKit.EtaBar.PadEnd, bottom = PenKit.EtaBar.PadBottom),
        horizontalArrangement = Arrangement.spacedBy(PenKit.EtaBar.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PenText(arrival, PenKit.EtaBar.Arrive.Style, PenKit.EtaBar.Arrive.Fill, Modifier.pen(c, "Arrive"))
        PenText(remaining, PenKit.EtaBar.Remain.Style, PenKit.EtaBar.Remain.Fill, Modifier.pen(c, "Remain").weight(1f, fill = fill))
        Row(
            modifier = Modifier
                .pen(c, "Stop")
                .height(PenKit.EtaBar.Stop.H)
                .clip(penShape(PenKit.EtaBar.Stop.Radius))
                .background(PenKit.EtaBar.Stop.Fill)
                .clickable(onClick = onEnd)
                .then(if (endDescription != null) Modifier.semantics(mergeDescendants = true) { contentDescription = endDescription } else Modifier)
                .padding(start = PenKit.EtaBar.Stop.PadStart, end = PenKit.EtaBar.Stop.PadEnd),
            horizontalArrangement = Arrangement.spacedBy(PenKit.EtaBar.Stop.Gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(endIcon, contentDescription = null, modifier = Modifier.pen(c, "Stop/Icon").size(PenKit.EtaBar.Stop.Icon.W), tint = PenKit.EtaBar.Stop.Icon.Fill)
            PenText(PenKit.EtaBar.Stop.Label.Reference, PenKit.EtaBar.Stop.Label.Style, PenKit.EtaBar.Stop.Label.Fill, Modifier.pen(c, "Stop/Label"))
        }
    }
}

// --- Status strip ------------------------------------------------------------------------------

@Composable
fun KitStatusStrip(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = PenKit.StatusStrip.Badge.Icon.Vector,
    searchIcon: ImageVector = PenKit.StatusStrip.Search.Icon.Vector,
    /** The strip spans the width its slot gives it (portrait panes): the text takes the slack, the search button ends the row. */
    fill: Boolean = false,
    /** What the button announces; [onSearch] is whatever the strip's action does (not only a search). */
    actionDescription: String = "Search destination",
    onSearch: () -> Unit = {},
) {
    val c = "StatusStrip"
    Row(
        modifier = modifier
            .pen(c)
            .height(PenKit.StatusStrip.H)
            .clip(penShape(PenKit.StatusStrip.Radius))
            .background(PenKit.StatusStrip.Fill)
            .padding(start = PenKit.StatusStrip.PadStart, top = PenKit.StatusStrip.PadTop, end = PenKit.StatusStrip.PadEnd, bottom = PenKit.StatusStrip.PadBottom),
        horizontalArrangement = Arrangement.spacedBy(PenKit.StatusStrip.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PenDisc(PenKit.StatusStrip.Badge.W, PenKit.StatusStrip.Badge.Fill, icon, PenKit.StatusStrip.Badge.Icon.W, PenKit.StatusStrip.Badge.Icon.Fill, Modifier.pen(c, "Badge"), iconModifier = Modifier.pen(c, "Badge/Icon"))
        Column(modifier = Modifier.pen(c, "Text").weight(1f, fill = fill)) {
            PenText(title, PenKit.StatusStrip.Text.Title.Style, PenKit.StatusStrip.Text.Title.Fill, Modifier.pen(c, "Text/Title"))
            PenText(body, PenKit.StatusStrip.Text.Body.Style, PenKit.StatusStrip.Text.Body.Fill, Modifier.pen(c, "Text/Body"))
        }
        PenDisc(
            PenKit.StatusStrip.Search.W, PenKit.StatusStrip.Search.Fill, searchIcon, PenKit.StatusStrip.Search.Icon.W, PenKit.StatusStrip.Search.Icon.Fill, Modifier.pen(c, "Search"),
            iconModifier = Modifier.pen(c, "Search/Icon"), description = actionDescription, onClick = onSearch,
        )
    }
}

// --- Now-playing and next-turn chips -----------------------------------------------------------

@Composable
fun KitNowPlayingChip(
    title: String,
    artist: String,
    art: ImageBitmap?,
    artFallback: Color,
    playing: Boolean,
    modifier: Modifier = Modifier,
    textWidth: Dp = PenKit.NowPlayingChip.Text.W,
    canPlayPause: Boolean = true,
    onPlayPause: () -> Unit = {},
    /** A tap on the chip itself (outside its play/pause button); null leaves it inert, as in the Pen. */
    onClick: (() -> Unit)? = null,
    /** What the chip announces when it is tappable. */
    description: String? = null,
) {
    val c = "NowPlayingChip"
    Row(
        modifier = modifier
            .pen(c)
            .height(PenKit.NowPlayingChip.H)
            .clip(penShape(PenKit.NowPlayingChip.Radius))
            .background(PenKit.NowPlayingChip.Fill)
            .penClick(onClick)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier)
            .padding(start = PenKit.NowPlayingChip.PadStart, top = PenKit.NowPlayingChip.PadTop, end = PenKit.NowPlayingChip.PadEnd, bottom = PenKit.NowPlayingChip.PadBottom),
        horizontalArrangement = Arrangement.spacedBy(PenKit.NowPlayingChip.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .pen(c, "Art")
                .size(PenKit.NowPlayingChip.Art.W)
                .clip(penShape(PenKit.NowPlayingChip.Art.Radius))
                .background(artFallback),
        ) {
            if (art != null) {
                Image(art, contentDescription = null, modifier = Modifier.size(PenKit.NowPlayingChip.Art.W), contentScale = ContentScale.Crop)
            }
        }
        Column(modifier = Modifier.pen(c, "Text").width(textWidth)) {
            PenText(title, PenKit.NowPlayingChip.Text.Title.Style, PenKit.NowPlayingChip.Text.Title.Fill, Modifier.pen(c, "Text/Title"))
            PenText(artist, PenKit.NowPlayingChip.Text.Artist.Style, PenKit.NowPlayingChip.Text.Artist.Fill, Modifier.pen(c, "Text/Artist"))
        }
        PenDisc(
            PenKit.NowPlayingChip.PlayPause.W, PenKit.NowPlayingChip.PlayPause.Fill, if (playing) PenKit.NowPlayingChip.PlayPause.Icon.Vector else PenIcons.PlayArrow, PenKit.NowPlayingChip.PlayPause.Icon.W, PenKit.NowPlayingChip.PlayPause.Icon.Fill,
            Modifier.pen(c, "PlayPause").width(PenKit.NowPlayingChip.PlayPause.W).height(PenKit.NowPlayingChip.PlayPause.H),
            iconModifier = Modifier.pen(c, "PlayPause/Icon"), description = "Play or pause", onClick = onPlayPause, enabled = canPlayPause,
        )
    }
}

@Composable
fun KitNextTurnChip(
    distance: String,
    road: String,
    arrow: ImageVector,
    modifier: Modifier = Modifier,
    /** A tap on the chip (brings the map back); null leaves it inert, as in the Pen. */
    onClick: (() -> Unit)? = null,
    /** What the chip announces when it is tappable. */
    description: String? = null,
) {
    val c = "NextTurnChip"
    Row(
        modifier = modifier
            .pen(c)
            .height(PenKit.NextTurnChip.H)
            .clip(penShape(PenKit.NextTurnChip.Radius))
            .background(PenKit.NextTurnChip.Fill)
            .penClick(onClick)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier)
            .padding(start = PenKit.NextTurnChip.PadStart, top = PenKit.NextTurnChip.PadTop, end = PenKit.NextTurnChip.PadEnd, bottom = PenKit.NextTurnChip.PadBottom),
        horizontalArrangement = Arrangement.spacedBy(PenKit.NextTurnChip.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(arrow, contentDescription = null, modifier = Modifier.pen(c, "Icon").size(PenKit.NextTurnChip.Icon.W), tint = PenKit.NextTurnChip.Icon.Fill)
        Column(modifier = Modifier.pen(c, "Text").weight(1f, fill = false)) {
            PenText(distance, PenKit.NextTurnChip.Text.Distance.Style, PenKit.NextTurnChip.Text.Distance.Fill, Modifier.pen(c, "Text/Distance"))
            PenText(road, PenKit.NextTurnChip.Text.Road.Style, PenKit.NextTurnChip.Text.Road.Fill, Modifier.pen(c, "Text/Road"))
        }
    }
}

// --- Report button, map controls ---------------------------------------------------------------

@Composable
fun KitReportButton(modifier: Modifier = Modifier, icon: ImageVector = PenKit.ReportButton.Icon.Vector, onClick: () -> Unit = {}) {
    PenDisc(
        PenKit.ReportButton.W, PenKit.ReportButton.Fill, icon, PenKit.ReportButton.Icon.W, PenKit.ReportButton.Icon.Fill, modifier.pen("ReportButton"),
        stroke = PenKit.ReportButton.Stroke, strokeColor = PenKit.ReportButton.StrokeColor, iconModifier = Modifier.pen("ReportButton", "Icon"),
        description = "Report speed camera", onClick = onClick,
    )
}

/** One map control of the column, by its Pen node name ("ControlVolumeUp", ...). */
class KitMapControl(val node: String, val icon: ImageVector, val description: String, val onClick: () -> Unit)

@Composable
fun KitMapControls(
    controls: List<KitMapControl>,
    modifier: Modifier = Modifier,
    /** The least touch area (real dp, on screen) of a control; 0 draws the Pen's column. */
    minTouch: Dp = 0.dp,
) {
    val c = "MapControls"
    Column(modifier = modifier.pen(c), verticalArrangement = Arrangement.spacedBy(PenKit.MapControls.Gap)) {
        controls.forEach { control ->
            PenDisc(
                PenKit.MapControls.ControlVolumeUp.W, PenKit.MapControls.ControlVolumeUp.Fill, control.icon, PenKit.MapControls.ControlVolumeUp.Icon.W, PenKit.MapControls.ControlVolumeUp.Icon.Fill, Modifier.pen(c, control.node),
                iconModifier = Modifier.pen(c, "${control.node}/Icon"), description = control.description, onClick = control.onClick, minTouch = minTouch,
            )
        }
    }
}

// --- Report sent pill --------------------------------------------------------------------------

@Composable
fun KitReportSentPill(
    title: String,
    note: String?,
    seconds: Int,
    modifier: Modifier = Modifier,
    checkIcon: ImageVector = PenKit.ReportSentPill.Check.Vector,
    undoIcon: ImageVector = PenKit.ReportSentPill.Undo.Icon.Vector,
    /** The pill draws at the control scale; its text follows the tile scale (tile / control). */
    textScale: Float = 1f,
    onUndo: () -> Unit = {},
) {
    fun TextStyle.scaled() = if (textScale == 1f) this else copy(fontSize = fontSize * textScale, lineHeight = lineHeight * textScale)
    val c = "ReportSentPill"
    val shape = penShape(PenKit.ReportSentPill.Radius)
    Row(
        modifier = modifier
            .pen(c)
            .height(PenKit.ReportSentPill.H)
            .clip(shape)
            .background(PenKit.ReportSentPill.Fill)
            .border(PenKit.ReportSentPill.Stroke, PenKit.ReportSentPill.StrokeColor, shape)
            .padding(start = PenKit.ReportSentPill.PadStart, top = PenKit.ReportSentPill.PadTop, end = PenKit.ReportSentPill.PadEnd, bottom = PenKit.ReportSentPill.PadBottom),
        horizontalArrangement = Arrangement.spacedBy(PenKit.ReportSentPill.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(checkIcon, contentDescription = null, modifier = Modifier.pen(c, "Check").size(PenKit.ReportSentPill.Check.W), tint = PenKit.ReportSentPill.Check.Fill)
        Column(modifier = Modifier.pen(c, "Text").weight(1f, fill = false)) {
            PenText(title, PenKit.ReportSentPill.Text.Title.Style.scaled(), PenKit.ReportSentPill.Text.Title.Fill, Modifier.pen(c, "Text/Title"))
            if (note != null) PenText(note, PenKit.ReportSentPill.Text.Sub.Style.scaled(), PenKit.ReportSentPill.Text.Sub.Fill, Modifier.pen(c, "Text/Sub"))
        }
        Row(
            modifier = Modifier
                .pen(c, "Undo")
                .height(PenKit.ReportSentPill.Undo.H)
                .clip(penShape(PenKit.ReportSentPill.Undo.Radius))
                .background(PenKit.ReportSentPill.Undo.Fill)
                .clickable(onClick = onUndo)
                .padding(start = PenKit.ReportSentPill.Undo.PadStart, end = PenKit.ReportSentPill.Undo.PadEnd),
            horizontalArrangement = Arrangement.spacedBy(PenKit.ReportSentPill.Undo.Gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(undoIcon, contentDescription = null, modifier = Modifier.pen(c, "Undo/Icon").size(PenKit.ReportSentPill.Undo.Icon.W), tint = PenKit.ReportSentPill.Undo.Icon.Fill)
            PenText(PenKit.ReportSentPill.Undo.Label.Reference, PenKit.ReportSentPill.Undo.Label.Style.scaled(), PenKit.ReportSentPill.Undo.Label.Fill, Modifier.pen(c, "Undo/Label"))
            if (seconds > 0) PenText("$seconds", PenKit.ReportSentPill.Undo.Seconds.Style.scaled(), PenKit.ReportSentPill.Undo.Seconds.Fill, Modifier.pen(c, "Undo/Seconds"))
        }
    }
}

// --- Report sheet ------------------------------------------------------------------------------

/**
 * One report choice: its label, its badge icon, and the Pen node name it maps to ("SpeedCamera", ...).
 * A disabled choice (no GPS fix to pin it to) is not clickable and draws the live sheet's greyed badge and label.
 */
class KitReportChoice(
    val node: String,
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
)

@Composable
fun KitReportSheet(
    title: String,
    subtitle: String,
    choices: List<KitReportChoice>,
    columns: Int,
    modifier: Modifier = Modifier,
    closeIcon: ImageVector = PenKit.ReportSheet.Header.Close.Icon.Vector,
    onClose: () -> Unit = {},
) {
    val c = "ReportSheet"
    val shape = penShape(PenKit.ReportSheet.Radius)
    Column(
        modifier = modifier
            .pen(c)
            .clip(shape)
            .background(PenKit.ReportSheet.Fill)
            .border(PenKit.ReportSheet.Stroke, PenKit.ReportSheet.StrokeColor, shape)
            .padding(start = PenKit.ReportSheet.PadStart, top = PenKit.ReportSheet.PadTop, end = PenKit.ReportSheet.PadEnd, bottom = PenKit.ReportSheet.PadBottom),
        verticalArrangement = Arrangement.spacedBy(PenKit.ReportSheet.Gap),
    ) {
        Row(
            modifier = Modifier.pen(c, "Header").fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PenKit.ReportSheet.Header.Gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.pen(c, "Header/Titles").weight(1f),
                verticalArrangement = Arrangement.spacedBy(PenKit.ReportSheet.Header.Titles.Gap),
            ) {
                PenText(title, PenKit.ReportSheet.Header.Titles.Title.Style, PenKit.ReportSheet.Header.Titles.Title.Fill, Modifier.pen(c, "Header/Titles/Title"))
                PenText(subtitle, PenKit.ReportSheet.Header.Titles.Sub.Style, PenKit.ReportSheet.Header.Titles.Sub.Fill, Modifier.pen(c, "Header/Titles/Sub").fillMaxWidth())
            }
            PenDisc(
                PenKit.ReportSheet.Header.Close.W, PenKit.ReportSheet.Header.Close.Fill, closeIcon, PenKit.ReportSheet.Header.Close.Icon.W, PenKit.ReportSheet.Header.Close.Icon.Fill,
                Modifier.pen(c, "Header/Close"), iconModifier = Modifier.pen(c, "Header/Close/Icon"),
                description = "Close report sheet", onClick = onClose,
            )
        }
        val rows = choices.chunked(columns)
        rows.forEachIndexed { index, rowChoices ->
            // One row is the Pen's wide sheet ("Choices"); two are its "Row 1" and "Row 2".
            val rowNode = if (rows.size == 1) "Choices" else "Row${index + 1}"
            Row(
                modifier = Modifier.pen(c, rowNode).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PenKit.ReportSheet.Row1.Gap),
            ) {
                rowChoices.forEach { item ->
                    val path = "$rowNode/${item.node}"
                    Row(
                        modifier = Modifier
                            .pen(c, path)
                            .weight(1f)
                            .height(PenKit.ReportSheet.Row1.SpeedCamera.H)
                            .clip(penShape(PenKit.ReportSheet.Row1.SpeedCamera.Radius))
                            .background(PenKit.ReportSheet.Row1.SpeedCamera.Fill)
                            .clickable(enabled = item.enabled, onClick = item.onClick)
                            .padding(start = PenKit.ReportSheet.Row1.SpeedCamera.PadStart, end = PenKit.ReportSheet.Row1.SpeedCamera.PadEnd),
                        horizontalArrangement = Arrangement.spacedBy(PenKit.ReportSheet.Row1.SpeedCamera.Gap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PenDisc(
                            PenKit.ReportSheet.Row1.SpeedCamera.Badge.W,
                            if (item.enabled) PenKit.ReportSheet.Row1.SpeedCamera.Badge.Fill else CarColors.SurfaceElevated,
                            item.icon, PenKit.ReportSheet.Row1.SpeedCamera.Badge.Icon.W,
                            if (item.enabled) PenKit.ReportSheet.Row1.SpeedCamera.Badge.Icon.Fill else CarColors.TextDisabled,
                            Modifier.pen(c, "$path/Badge"), iconModifier = Modifier.pen(c, "$path/Badge/Icon"),
                        )
                        PenText(item.label, PenKit.ReportSheet.Row1.SpeedCamera.Label.Style, if (item.enabled) PenKit.ReportSheet.Row1.SpeedCamera.Label.Fill else CarColors.TextDisabled, Modifier.pen(c, "$path/Label").weight(1f), maxLines = 2)
                    }
                }
            }
        }
    }
}

// --- Page dots ---------------------------------------------------------------------------------

/** The two page dots (map, media) in their scrim pill; [mediaShown] lights the second. */
@Composable
fun KitPageDots(
    mediaShown: Boolean,
    modifier: Modifier = Modifier,
    /** A tap on the dots switches page; null leaves them inert, as in the Pen. */
    onClick: (() -> Unit)? = null,
    /** What the dots announce when they are tappable. */
    description: String? = null,
) {
    val c = "PageDots"
    Row(
        modifier = modifier
            .pen(c)
            .clip(penShape(PenKit.PageDots.Radius))
            .background(PenKit.PageDots.Fill)
            .penClick(onClick)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier)
            .padding(
                start = PenKit.PageDots.PadStart, top = PenKit.PageDots.PadTop,
                end = PenKit.PageDots.PadEnd, bottom = PenKit.PageDots.PadBottom,
            ),
        horizontalArrangement = Arrangement.spacedBy(PenKit.PageDots.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.pen(c, "Map").size(PenKit.PageDots.Map.W)
                .background(if (mediaShown) PenKit.PageDots.Media.Fill else PenKit.PageDots.Map.Fill, penShape(999.dp)),
        )
        Box(
            Modifier.pen(c, "Media").size(PenKit.PageDots.Media.W)
                .background(if (mediaShown) PenKit.PageDots.Map.Fill else PenKit.PageDots.Media.Fill, penShape(999.dp)),
        )
    }
}
