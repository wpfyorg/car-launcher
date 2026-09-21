package org.wpfy.carlauncher.design.kit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt
import org.wpfy.carlauncher.design.theme.FixedScale
import org.wpfy.carlauncher.design.theme.OverlayTier
import org.wpfy.carlauncher.design.kit.PenKit.SpeedProfile as ProfileKit
import org.wpfy.carlauncher.design.kit.PenKit.SpeedProfile.Head as ProfileHeadKit
import org.wpfy.carlauncher.design.kit.PenKit.SpeedProfile.SpeedChart as ChartKit
import org.wpfy.carlauncher.design.kit.PenKit.TripRouteMap as MapKit
import org.wpfy.carlauncher.design.kit.PenKit.TripRouteMap.DistanceOverlay as OverlayKit
import org.wpfy.carlauncher.design.kit.PenKit.TripRouteMap.DistanceOverlay.Distance as OverlayDistanceKit
import org.wpfy.carlauncher.design.kit.PenKit.TripRouteMap.Legend as LegendKit
import org.wpfy.carlauncher.design.kit.PenKit.TripRow as TripRowKit
import org.wpfy.carlauncher.design.kit.PenKit.TripRow.Text as TripRowTextKit
import org.wpfy.carlauncher.design.kit.PenKit.TripStatTile as StatKit
import org.wpfy.carlauncher.design.kit.PenKit.TripsSummary as SummaryKit

/*
 * The trip kit (Pen "Trip Route Map", "Trip Stat Tile", "Speed Profile", "Trip Row", "Trips Summary"): drawn at the
 * Large tier's 1x sizes inside [KitDriveScale]. Trip Row and Trips Summary are drawn only at Medium in the Pen; their
 * 1x sizes are the Medium ones over the drive scale.
 */

/** Scale for reusable drive-screen kits, derived from the exported Pen frame widths. */
fun kitDriveScaleFor(tier: OverlayTier): Float = when (tier) {
    OverlayTier.Small -> PenKit.DriveScale.Small
    OverlayTier.Medium -> PenKit.DriveScale.Medium
    OverlayTier.Large -> PenKit.DriveScale.Large
    OverlayTier.XLarge -> PenKit.DriveScale.XLarge
}

@Composable
fun KitDriveScale(tier: OverlayTier, content: @Composable () -> Unit) = FixedScale(kitDriveScaleFor(tier), content)

// --- Shared label and value runs ---------------------------------------------------------------

/** A Pen stat label run: [icon] then the caps [text], centred on each other. */
@Composable
internal fun PenStatLabel(
    icon: ImageVector,
    iconSize: Dp,
    iconTint: Color,
    gap: Dp,
    text: String,
    style: TextStyle,
    color: Color,
    tag: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier.pen(tag), horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.pen(tag, "Icon").size(iconSize), tint = iconTint)
        PenLineText(text, style, color, Modifier.pen(tag, "Text"))
    }
}

/** A Pen value run: the [number] then its [unit] (none when null), on one baseline-bottom. */
@Composable
internal fun PenStatValue(
    number: String,
    numberStyle: TextStyle,
    numberColor: Color,
    unit: String?,
    unitStyle: TextStyle?,
    unitColor: Color,
    gap: Dp,
    tag: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier.pen(tag), horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.Bottom) {
        PenLineText(number, numberStyle, numberColor, Modifier.pen(tag, "Number"))
        if (unit != null && unitStyle != null) PenLineText(unit, unitStyle, unitColor, Modifier.pen(tag, "Unit"))
    }
}

/** Filled Pen shape with its exported border. */
internal fun Modifier.penCard(fill: Color, radius: Dp, stroke: Dp? = null, strokeColor: Color = Color.Transparent): Modifier {
    val shape = penShape(radius)
    return clip(shape).background(fill, shape).then(if (stroke != null) Modifier.border(stroke, strokeColor, shape) else Modifier)
}

internal fun Modifier.penEllipse(fill: Color, stroke: Dp? = null, strokeColor: Color = Color.Transparent): Modifier =
    clip(CircleShape).background(fill, CircleShape).then(if (stroke != null) Modifier.border(stroke, strokeColor, CircleShape) else Modifier)

/** Single-line Pen text in its exported line box. */
@Composable
internal fun PenLineText(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier, textAlign: TextAlign? = null) {
    val line = with(androidx.compose.ui.platform.LocalDensity.current) { penStyle(style).lineHeight.toDp() }
    Box(modifier.height(line), contentAlignment = Alignment.CenterStart, propagateMinConstraints = true) {
        PenText(text, style, color, Modifier.wrapContentHeight(unbounded = true), textAlign = textAlign)
    }
}

// --- Route --------------------------------------------------------------------------------------

/**
 * A trip route contains independent continuous runs. Each segment's points are normalized fractions (0..1) of the
 * drawing width and height; missing or invalid fixes belong between segments, never as a connecting line. Overspeed
 * ranges are segment-local point-index ranges.
 */
@Immutable
data class KitTripRoute(val segments: List<KitTripRouteSegment> = emptyList())

/** One continuous GPS run. Gaps and invalid fixes start a new segment. */
@Immutable
data class KitTripRouteSegment(val points: List<Offset>, val overLimit: List<IntRange> = emptyList())

private fun DrawScope.routePath(points: List<Offset>): Path = Path().apply {
    points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x * size.width, p.y * size.height) else lineTo(p.x * size.width, p.y * size.height) }
}

private fun DrawScope.drawRoute(points: List<Offset>, width: Dp, color: Color) {
    if (points.size < 2) return
    drawPath(routePath(points), color, style = Stroke(width.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
}

// --- Trip route map -----------------------------------------------------------------------------

/** The trip map's texts: the over-limit [legend] (null hides it), the distance [number] and [unit], and the [from] / [to] places. */
@Immutable
data class KitTripMapText(
    val legend: String?,
    val number: String,
    val unit: String,
    val from: String,
    val to: String,
    val label: String = OverlayDistanceKit.Label.Text.Reference,
)

/**
 * The trip's route over the map (Pen "Trip Route Map"), [width] × [height]: [map] (the map picture) under the tint, the
 * route with its casing and over-limit stretches, the start dot and end flag, the legend, and the distance overlay.
 */
@Composable
fun KitTripRouteMap(
    route: KitTripRoute,
    text: KitTripMapText,
    modifier: Modifier = Modifier,
    width: Dp = MapKit.W,
    height: Dp = MapKit.H,
    tag: String = "TripRouteMap",
    runtimeRoute: (@Composable (width: Dp, height: Dp, markerPadding: Dp) -> KitTripRoute)? = null,
    map: (@Composable BoxScope.() -> Unit)? = null,
) {
    val c = tag
    var overlayHeightPx by remember { mutableIntStateOf(0) }
    val overlayHeight = with(LocalDensity.current) { overlayHeightPx.toDp() }
    Box(
        modifier.pen(c).size(width, height).clip(penShape(MapKit.Radius))
            .penCard(Color.Transparent, MapKit.Radius, MapKit.Stroke, MapKit.StrokeColor),
    ) {
        map?.invoke(this)
        Box(Modifier.pen(c, "Tint").fillMaxSize().background(MapKit.Tint.Fill))
        if (runtimeRoute == null) {
            TripRouteLayers(route, width, height, c)
        } else if (overlayHeightPx > 0) {
            BoxWithConstraints(
                Modifier.fillMaxSize().padding(
                    start = MapKit.PadStart, end = MapKit.PadEnd, top = MapKit.PadTop,
                    bottom = MapKit.PadBottom + overlayHeight + MapKit.PadTop,
                ),
            ) {
                if (maxWidth.value > 0f && maxHeight.value > 0f) {
                    val markerPadding = maxOf(MapKit.Start.W, MapKit.End.W) / 2 + MapKit.RouteCasing.Stroke / 2
                    TripRouteLayers(runtimeRoute(maxWidth, maxHeight, markerPadding), maxWidth, maxHeight, c)
                }
            }
        }
        Column(Modifier.fillMaxSize().penPadding(MapKit.PadTop, MapKit.PadEnd, MapKit.PadBottom, MapKit.PadStart)) {
            if (text.legend != null) {
                Row(
                    Modifier.pen(c, "Legend").penCard(LegendKit.Fill, LegendKit.Radius)
                        .penPadding(LegendKit.PadTop, LegendKit.PadEnd, LegendKit.PadBottom, LegendKit.PadStart),
                    horizontalArrangement = Arrangement.spacedBy(LegendKit.Gap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.pen(c, "Legend/Swatch").size(LegendKit.Swatch.W, LegendKit.Swatch.H).penCard(LegendKit.Swatch.Fill, LegendKit.Swatch.Radius))
                    PenLineText(text.legend, LegendKit.Text.Style, LegendKit.Text.Fill, Modifier.pen(c, "Legend/Text"))
                }
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.pen(c, "DistanceOverlay").fillMaxWidth().onSizeChanged { overlayHeightPx = it.height }
                    .penCard(OverlayKit.Fill, OverlayKit.Radius, OverlayKit.Stroke, OverlayKit.StrokeColor)
                    .penPadding(OverlayKit.PadTop, OverlayKit.PadEnd, OverlayKit.PadBottom, OverlayKit.PadStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(Modifier.pen(c, "DistanceOverlay/Distance"), verticalArrangement = Arrangement.spacedBy(OverlayDistanceKit.Gap)) {
                    PenStatLabel(
                        OverlayDistanceKit.Label.Icon.Vector, OverlayDistanceKit.Label.Icon.W, OverlayDistanceKit.Label.Icon.Fill, OverlayDistanceKit.Label.Gap,
                        text.label, OverlayDistanceKit.Label.Text.Style, OverlayDistanceKit.Label.Text.Fill, "$c/DistanceOverlay/Distance/Label",
                    )
                    PenStatValue(
                        text.number, OverlayDistanceKit.Value.Number.Style, OverlayDistanceKit.Value.Number.Fill,
                        text.unit, OverlayDistanceKit.Value.Unit.Style, OverlayDistanceKit.Value.Unit.Fill, OverlayDistanceKit.Value.Gap,
                        "$c/DistanceOverlay/Distance/Value",
                    )
                }
                Column(
                    Modifier.pen(c, "DistanceOverlay/Ends"),
                    verticalArrangement = Arrangement.spacedBy(OverlayKit.Ends.Gap),
                    horizontalAlignment = Alignment.End,
                ) {
                    PenLineText(text.from, OverlayKit.Ends.From.Style, OverlayKit.Ends.From.Fill, Modifier.pen(c, "DistanceOverlay/Ends/From"), textAlign = TextAlign.End)
                    PenLineText(text.to, OverlayKit.Ends.To.Style, OverlayKit.Ends.To.Fill, Modifier.pen(c, "DistanceOverlay/Ends/To"), textAlign = TextAlign.End)
                }
            }
        }
    }
}

/** Draws fractions in the caller's actual track viewport; reference fixtures retain the full map. */
@Composable
private fun TripRouteLayers(route: KitTripRoute, width: Dp, height: Dp, c: String) {
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.pen(c, "RouteCasing").fillMaxSize()) { route.segments.forEach { drawRoute(it.points, MapKit.RouteCasing.Stroke, MapKit.RouteCasing.StrokeColor) } }
        Canvas(Modifier.pen(c, "Route").fillMaxSize()) { route.segments.forEach { drawRoute(it.points, MapKit.Route.Stroke, MapKit.Route.StrokeColor) } }
        Canvas(Modifier.pen(c, "RouteOverLimit").fillMaxSize()) {
            for (segment in route.segments) for (range in segment.overLimit) {
                val start = range.first.coerceAtLeast(0)
                val end = (range.last + 1).coerceAtMost(segment.points.size)
                if (end - start >= 2) drawRoute(segment.points.subList(start, end), MapKit.RouteOverLimit.Stroke, MapKit.RouteOverLimit.StrokeColor)
            }
        }
        route.segments.firstNotNullOfOrNull { it.points.firstOrNull() }?.let { p ->
            Box(
                Modifier.offset(width * p.x - MapKit.Start.W / 2, height * p.y - MapKit.Start.H / 2).pen(c, "Start").size(MapKit.Start.W)
                    .penEllipse(MapKit.Start.Fill, MapKit.Start.Stroke, MapKit.Start.StrokeColor),
            )
        }
        route.segments.asReversed().firstNotNullOfOrNull { it.points.lastOrNull() }?.takeIf { route.segments.sumOf { it.points.size } > 1 }?.let { p ->
            PenDisc(
                MapKit.End.W, MapKit.End.Fill, MapKit.End.Icon.Vector, MapKit.End.Icon.W, MapKit.End.Icon.Fill,
                Modifier.offset(width * p.x - MapKit.End.W / 2, height * p.y - MapKit.End.H / 2).pen(c, "End"),
                radius = MapKit.End.Radius, stroke = MapKit.End.Stroke, strokeColor = MapKit.End.StrokeColor, iconModifier = Modifier.pen(c, "End/Icon"),
            )
        }
    }
}

// --- Trip stat tile -----------------------------------------------------------------------------

/** One trip figure: [label] (caps) with its [icon], the [number] and its [unit] (none when null). */
@Immutable
data class KitTripStat(val icon: ImageVector, val label: String, val number: String, val unit: String?, val description: String? = null)

/** A trip figure tile (Pen "Trip Stat Tile"), [width] wide: label at the top, value at the bottom. */
@Composable
fun KitTripStatTile(stat: KitTripStat, modifier: Modifier = Modifier, width: Dp = StatKit.W, height: Dp = StatKit.H, tag: String = "TripStatTile") {
    val c = tag
    Column(
        modifier.pen(c).size(width, height)
            .then(stat.description?.let { description -> Modifier.clearAndSetSemantics { contentDescription = description } } ?: Modifier)
            .penCard(StatKit.Fill, StatKit.Radius, StatKit.Stroke, StatKit.StrokeColor)
            .penPadding(StatKit.PadTop, StatKit.PadEnd, StatKit.PadBottom, StatKit.PadStart),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        PenStatLabel(stat.icon, StatKit.Label.Icon.W, StatKit.Label.Icon.Fill, StatKit.Label.Gap, stat.label, StatKit.Label.Text.Style, StatKit.Label.Text.Fill, "$c/Label",
            // The Pen's Small label (floored to 11sp) is wider than the tile's inner width: it runs on, as drawn.
            Modifier.wrapContentWidth(Alignment.Start, unbounded = true))
        PenStatValue(
            stat.number, StatKit.Value.Number.Style, StatKit.Value.Number.Fill, stat.unit, StatKit.Value.Unit.Style, StatKit.Value.Unit.Fill,
            StatKit.Value.Gap, "$c/Value",
        )
    }
}

// --- Speed profile ------------------------------------------------------------------------------

/** One speed bin: [level] is bounded 0..1, while null means no recorded speed; [over] marks overspeed. */
@Immutable
data class KitSpeedBar(val level: Float?, val over: Boolean = false)

/** The trip's speed over time (Pen "Speed Profile"), [width] wide: label and time [range], then one bar per stretch. */
@Composable
fun KitSpeedProfile(
    bars: List<KitSpeedBar>,
    range: String,
    modifier: Modifier = Modifier,
    width: Dp = ProfileKit.W,
    label: String = ProfileHeadKit.Label.Text.Reference,
    tag: String = "SpeedProfile",
) {
    val c = tag
    val barShape = RoundedCornerShape(ChartKit.Bar1.RadiusTopStart, ChartKit.Bar1.RadiusTopEnd, ChartKit.Bar1.RadiusBottomEnd, ChartKit.Bar1.RadiusBottomStart)
    Column(
        modifier.pen(c).width(width)
            // The Pen's height is the 1x one scaled; the floored Small head overruns it into the bottom padding.
            .height(ProfileKit.H)
            .penCard(ProfileKit.Fill, ProfileKit.Radius, ProfileKit.Stroke, ProfileKit.StrokeColor)
            .penPadding(ProfileKit.PadTop, ProfileKit.PadEnd, ProfileKit.PadBottom, ProfileKit.PadStart),
        verticalArrangement = Arrangement.spacedBy(ProfileKit.Gap),
    ) {
        Row(Modifier.pen(c, "Head").fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            PenStatLabel(
                ProfileHeadKit.Label.Icon.Vector, ProfileHeadKit.Label.Icon.W, ProfileHeadKit.Label.Icon.Fill, ProfileHeadKit.Label.Gap,
                label, ProfileHeadKit.Label.Text.Style, ProfileHeadKit.Label.Text.Fill, "$c/Head/Label",
            )
            PenLineText(range, ProfileHeadKit.Range.Style, ProfileHeadKit.Range.Fill, Modifier.pen(c, "Head/Range"))
        }
        // Bars at the Pen's exact pitch (equal widths, the chart's gap between), bottom-aligned at their levels.
        Layout(
            content = {
                bars.forEachIndexed { i, bar ->
                    Box(Modifier.pen(c, "SpeedChart/Bar${i + 1}").clip(barShape).background(if (bar.over) ChartKit.Bar17.Fill else ChartKit.Bar1.Fill, barShape))
                }
            },
            // Keep the chart inside the card's remaining height. The Small tier's 11sp text floor makes the
            // header taller than its scaled Pen size; unbounding this child let its fixed height overflow padding.
            modifier = Modifier.pen(c, "SpeedChart").fillMaxWidth().height(ChartKit.H),
        ) { measurables, constraints ->
            val count = measurables.size
            val width = constraints.maxWidth
            val gap = if (count > 1) minOf(ChartKit.Gap.toPx(), width.toFloat() / (count - 1)) else 0f
            val pitch = (width - gap * (count - 1)) / count.coerceAtLeast(1)
            val h = constraints.maxHeight
            layout(width, h) {
                measurables.forEachIndexed { i, m ->
                    val x0 = (i * (pitch + gap)).roundToInt().coerceIn(0, width)
                    val x1 = (i * (pitch + gap) + pitch).roundToInt().coerceIn(x0, width)
                    val level = bars[i].level?.takeIf { it.isFinite() }?.coerceIn(0f, 1f)
                    val zeroMark = ProfileKit.Stroke.toPx().coerceAtMost(h.toFloat())
                    val bh = if (level == null) 0 else (h * level).coerceAtLeast(zeroMark).roundToInt().coerceIn(0, h)
                    m.measure(Constraints.fixed(x1 - x0, bh)).place(x0, h - bh)
                }
            }
        }
    }
}

// --- Trip row -----------------------------------------------------------------------------------

/** One past trip: [title] (from → to), its [time] line, [distance], [average] speed line, its [route], and whether it is the [current] one. */
@Immutable
data class KitTripSummaryRow(
    val title: String,
    val time: String,
    val distance: String,
    val average: String,
    val route: KitTripRoute,
    val current: Boolean = false,
)

/**
 * A trip in the trips list (Pen "Trip Row", Medium only), [width] wide: route thumbnail ([map] under its tint), title
 * with the Current badge, time, distance and average, chevron. The current row is highlighted; the others are surface cards.
 */
@Composable
fun KitTripRow(
    trip: KitTripSummaryRow,
    modifier: Modifier = Modifier,
    width: Dp = TripRowKit.W,
    currentLabel: String = TripRowTextKit.TitleRow.Current.Label.Reference,
    tag: String = "TripRow",
    onClick: (() -> Unit)? = null,
    map: (@Composable BoxScope.() -> Unit)? = null,
) {
    val c = tag
    Row(
        modifier.pen(c).width(width).height(TripRowKit.H)
            .then(
                if (trip.current) Modifier.penCard(TripRowKit.Fill, TripRowKit.Radius, TripRowKit.Stroke, TripRowKit.StrokeColor)
                // The Pen's other rows (All trips frame) override the row to the surface card with its hairline: the Trips Summary's.
                else Modifier.penCard(SummaryKit.Fill, TripRowKit.Radius, SummaryKit.Stroke, SummaryKit.StrokeColor),
            )
            .penClick(onClick)
            .penPadding(TripRowKit.PadTop, TripRowKit.PadEnd, TripRowKit.PadBottom, TripRowKit.PadStart),
        horizontalArrangement = Arrangement.spacedBy(TripRowKit.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.pen(c, "Thumb").size(TripRowKit.Thumb.W, TripRowKit.Thumb.H).clip(penShape(TripRowKit.Thumb.Radius))) {
            map?.invoke(this)
            Box(Modifier.pen(c, "Thumb/Tint").fillMaxSize().background(TripRowKit.Thumb.Tint.Fill))
            Canvas(Modifier.pen(c, "Thumb/Route").fillMaxSize()) { trip.route.segments.forEach { drawRoute(it.points, TripRowKit.Thumb.Route.Stroke, TripRowKit.Thumb.Route.StrokeColor) } }
        }
        Column(Modifier.pen(c, "Text").weight(1f), verticalArrangement = Arrangement.spacedBy(TripRowTextKit.Gap)) {
            Row(
                Modifier.pen(c, "Text/TitleRow"),
                horizontalArrangement = Arrangement.spacedBy(TripRowTextKit.TitleRow.Gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PenLineText(
                    trip.title, TripRowTextKit.TitleRow.Title.Style, TripRowTextKit.TitleRow.Title.Fill,
                    Modifier.pen(c, "Text/TitleRow/Title").weight(1f, fill = false),
                )
                if (trip.current) {
                    Box(
                        Modifier.pen(c, "Text/TitleRow/Current").penCard(TripRowTextKit.TitleRow.Current.Fill, TripRowTextKit.TitleRow.Current.Radius)
                            .penPadding(
                                TripRowTextKit.TitleRow.Current.PadTop, TripRowTextKit.TitleRow.Current.PadEnd,
                                TripRowTextKit.TitleRow.Current.PadBottom, TripRowTextKit.TitleRow.Current.PadStart,
                            ),
                    ) {
                        PenLineText(currentLabel, TripRowTextKit.TitleRow.Current.Label.Style, TripRowTextKit.TitleRow.Current.Label.Fill, Modifier.pen(c, "Text/TitleRow/Current/Label"))
                    }
                }
            }
            PenLineText(trip.time, TripRowTextKit.Time.Style, TripRowTextKit.Time.Fill, Modifier.pen(c, "Text/Time"))
        }
        Column(Modifier.pen(c, "Metrics"), verticalArrangement = Arrangement.spacedBy(TripRowKit.Metrics.Gap), horizontalAlignment = Alignment.End) {
            PenLineText(trip.distance, TripRowKit.Metrics.Distance.Style, TripRowKit.Metrics.Distance.Fill, Modifier.pen(c, "Metrics/Distance"))
            PenLineText(trip.average, TripRowKit.Metrics.Avg.Style, TripRowKit.Metrics.Avg.Fill, Modifier.pen(c, "Metrics/Avg"))
        }
        Icon(TripRowKit.Chevron.Vector, null, Modifier.pen(c, "Chevron").size(TripRowKit.Chevron.W), tint = TripRowKit.Chevron.Fill)
    }
}

// --- Trips summary ------------------------------------------------------------------------------

/** One summary figure: [number] and its [unit] (none when null). */
@Immutable
data class KitSummaryFigure(val number: String, val unit: String? = null)

/** The trips list's totals: trip count, distance, driving time and average speed, with their labels (the Pen's by default). */
@Immutable
data class KitTripsTotals(
    val trips: KitSummaryFigure,
    val distance: KitSummaryFigure,
    val driving: KitSummaryFigure,
    val avgSpeed: KitSummaryFigure,
    val tripsLabel: String = SummaryKit.TRIPS.Label.Reference,
    val distanceLabel: String = SummaryKit.DISTANCE.Label.Reference,
    val drivingLabel: String = SummaryKit.DRIVING.Label.Reference,
    val avgSpeedLabel: String = SummaryKit.AVGSPEED.Label.Reference,
)

/** A summary cell: caps [label] over the figure, its share of the strip; [inset] is the start padding after a divider (none for the first). */
@Composable
private fun RowScope.SummaryCell(label: String, figure: KitSummaryFigure, tag: String, inset: Dp? = null) {
    Column(Modifier.pen(tag).weight(1f).then(if (inset != null) Modifier.padding(start = inset) else Modifier), verticalArrangement = Arrangement.spacedBy(SummaryKit.DISTANCE.Gap)) {
        PenLineText(label, SummaryKit.DISTANCE.Label.Style, SummaryKit.DISTANCE.Label.Fill, Modifier.pen(tag, "Label"))
        PenStatValue(
            figure.number, SummaryKit.DISTANCE.Value.Number.Style, SummaryKit.DISTANCE.Value.Number.Fill,
            figure.unit, SummaryKit.DISTANCE.Value.Unit.Style, SummaryKit.DISTANCE.Value.Unit.Fill, SummaryKit.DISTANCE.Value.Gap, "$tag/Value",
        )
    }
}

/** The trips list's totals strip (Pen "Trips Summary", Medium only), [width] wide: four figures between hairline dividers. */
@Composable
fun KitTripsSummary(totals: KitTripsTotals, modifier: Modifier = Modifier, width: Dp = SummaryKit.W) {
    val c = "TripsSummary"
    Row(
        modifier.pen(c).width(width).height(SummaryKit.H)
            .penCard(SummaryKit.Fill, SummaryKit.Radius, SummaryKit.Stroke, SummaryKit.StrokeColor)
            .penPadding(SummaryKit.PadTop, SummaryKit.PadEnd, SummaryKit.PadBottom, SummaryKit.PadStart),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val divider = @Composable { n: Int ->
            Box(Modifier.pen(c, "Divider$n").width(SummaryKit.Divider1.W).fillMaxHeight().background(SummaryKit.Divider1.Fill))
        }
        SummaryCell(totals.tripsLabel, totals.trips, "$c/TRIPS")
        divider(1)
        SummaryCell(totals.distanceLabel, totals.distance, "$c/DISTANCE", SummaryKit.DISTANCE.PadStart)
        divider(2)
        SummaryCell(totals.drivingLabel, totals.driving, "$c/DRIVING", SummaryKit.DRIVING.PadStart)
        divider(3)
        SummaryCell(totals.avgSpeedLabel, totals.avgSpeed, "$c/AVGSPEED", SummaryKit.AVGSPEED.PadStart)
    }
}
