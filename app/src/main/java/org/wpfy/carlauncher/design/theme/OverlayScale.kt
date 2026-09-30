package org.wpfy.carlauncher.design.theme

import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import org.wpfy.carlauncher.data.settings.OverlaySize

/**
 * How much of a window the map overlays may use. Small windows (533x320dp head units, phones) draw
 * the information tiles at half size so the map stays visible; controls shrink less because they
 * are touched while driving.
 */
enum class OverlayTier { Small, Medium, Large, XLarge }

/**
 * Small: under 400dp tall in landscape, or narrower than 600dp in portrait (533x320, 360x640,
 * 411x731). Medium: under 520dp tall. XLarge: landscape 800dp tall or more (2400x900 and
 * 1920x1080 panels at 160dpi). Large: everything else. Decided on dp bounds only.
 */
fun overlayTierFor(widthDp: Float, heightDp: Float): OverlayTier {
    val portrait = heightDp > widthDp
    return when {
        !portrait && heightDp < SmallLandscapeMaxHeightDp -> OverlayTier.Small
        portrait && widthDp < SmallPortraitMaxWidthDp -> OverlayTier.Small
        heightDp < MediumMaxHeightDp -> OverlayTier.Medium
        !portrait && heightDp >= XLargeMinHeightDp -> OverlayTier.XLarge
        else -> OverlayTier.Large
    }
}

private const val SmallLandscapeMaxHeightDp = 400f
private const val SmallPortraitMaxWidthDp = 600f
private const val MediumMaxHeightDp = 520f
private const val XLargeMinHeightDp = 800f

/** Scale of the information tiles and of the controls, relative to the regular (1x) sizes. */
@Immutable
data class OverlayScales(val tile: Float, val control: Float) {
    companion object {
        val Unscaled = OverlayScales(1f, 1f)
    }
}

private fun tileScaleForTier(tier: OverlayTier): Float = when (tier) {
    OverlayTier.Small -> 0.5f
    OverlayTier.Medium -> 0.8f
    OverlayTier.Large -> 1f
    OverlayTier.XLarge -> 1.25f
}

private fun controlScaleForTier(tier: OverlayTier): Float = when (tier) {
    OverlayTier.Small -> 0.8f
    OverlayTier.Medium -> 0.9f
    OverlayTier.Large -> 1f
    OverlayTier.XLarge -> 1.25f
}

/**
 * Tiles follow the tier (Auto) or the user's override; controls always follow the tier. Neither
 * ever drops a touch target below [MinRealTouchTarget] (see [hitSizeFor]).
 */
fun overlayScalesFor(tier: OverlayTier, size: OverlaySize = OverlaySize.Auto): OverlayScales =
    OverlayScales(
        tile = size.tileScale ?: tileScaleForTier(tier),
        control = controlScaleForTier(tier),
    )

/** Smallest touch target in real dp, whatever the overlay scale. */
val MinRealTouchTarget = 48.dp

/**
 * Size of a control's hit area in the units of a scope drawn at [scale]: the visual size, grown to
 * the minimum real touch target. The visual may shrink with the scale, the hit area may not.
 */
fun hitSizeFor(visual: Dp, scale: Float, minReal: Dp = MinRealTouchTarget): Dp =
    maxOf(visual, minReal / scale.coerceAtLeast(0.01f))

/** Text size (sp as the user sees it, after scaling) given a style size in the scaled scope. */
fun floorSp(styleSp: Float, scale: Float, minRealSp: Float): Float =
    maxOf(styleSp, minRealSp / scale.coerceAtLeast(0.01f))

/**
 * How much the 12sp floor enlarges tile text of [styleSp] beyond the tile scale, so layout made in
 * virtual dp accounts for it.
 */
fun textGrowFor(tileScale: Float, styleSp: Float = 16f): Float =
    maxOf(1f, OverlayTextFloor.Secondary / (styleSp * tileScale.coerceAtLeast(0.01f)))

/** Floors for the scaled overlay text: turn distance and speed, bar headings, then smaller labels. */
object OverlayTextFloor {
    /**
     * One legibility floor for every HUD text role (11sp real): tiles keep the kit's proportions
     * at the 0.5 tile scale, and only the kit's smallest roles (the "Then" label, alert detail,
     * units, captions) are raised to it.
     */
    const val Primary = 11f
    const val Title = 11f
    const val Secondary = 11f
}

/**
 * Floors for icons drawn inside scaled tiles, in real dp: a glyph never shrinks below the controls'
 * icons around it (the Report button, map controls), whatever the tile scale.
 */
object OverlayIconFloor {
    /** Inline glyphs (End's close mark, the "Then" arrow). */
    val Small = 10.dp
    /** Icons in buttons and leading icons of bars. */
    val Regular = 11.dp
    /** The turn arrow and the alert's type icon. */
    val Prominent = 20.dp
}

/** [regular] in the current scope's units, raised so it is at least [minReal] on screen. */
@Composable
fun iconSizeIn(regular: Dp, minReal: Dp): Dp {
    val scale = LocalActiveOverlayScale.current
    if (scale >= 1f) return regular
    return maxOf(regular, minReal / scale.coerceAtLeast(0.01f))
}

/** The unscaled density and the scales a Home pane applies to its overlay subtrees. */
@Immutable
class OverlayScaling(val base: Density, val scales: OverlayScales) {
    override fun equals(other: Any?): Boolean =
        other is OverlayScaling && other.scales == scales &&
            other.base.density == base.density && other.base.fontScale == base.fontScale

    override fun hashCode(): Int = scales.hashCode() * 31 + base.density.hashCode()
}

val LocalOverlayScaling = compositionLocalOf<OverlayScaling?> { null }

/** Scale of the scope being composed (1 outside any scaled scope). */
val LocalActiveOverlayScale = compositionLocalOf { 1f }

/** Provides the unscaled density and the scales to every overlay below; call once per Home pane. */
@Composable
fun ProvideOverlayScaling(scales: OverlayScales, content: @Composable () -> Unit) {
    val base = LocalDensity.current
    val scaling = remember(base.density, base.fontScale, scales) { OverlayScaling(base, scales) }
    CompositionLocalProvider(LocalOverlayScaling provides scaling) { content() }
}

/** Draws [content] at the information-tile scale: layout in virtual dp, rendered at [OverlayScales.tile]. */
@Composable
fun TileScale(content: @Composable () -> Unit) {
    val scaling = LocalOverlayScaling.current
    ScaledScope(scaling, scaling?.scales?.tile ?: 1f, content)
}

/** Draws [content] at the control scale; its touch targets stay at least 48 real dp. */
@Composable
fun ControlScale(content: @Composable () -> Unit) {
    val scaling = LocalOverlayScaling.current
    ScaledScope(scaling, scaling?.scales?.control ?: 1f, content)
}

/**
 * Draws [content] at [scale] of the base density: a component the layout sizes as a whole (the media
 * card fitted to its pane, the dock to its tier), with the overlay text floor and real touch targets.
 */
@Composable
fun FixedScale(scale: Float, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val scaling = LocalOverlayScaling.current ?: remember(density) { OverlayScaling(density, OverlayScales(1f, 1f)) }
    ScaledScope(scaling, scale, content)
}

@Composable
private fun ScaledScope(scaling: OverlayScaling?, scale: Float, content: @Composable () -> Unit) {
    if (scaling == null || scale == 1f) {
        // Absolute, not relative: a 1x scope nested in a scaled one restores the base density.
        if (scaling != null && LocalActiveOverlayScale.current != 1f) {
            CompositionLocalProvider(
                LocalDensity provides scaling.base,
                LocalActiveOverlayScale provides 1f,
                LocalMinimumInteractiveComponentSize provides MinRealTouchTarget,
            ) { content() }
        } else {
            content()
        }
        return
    }
    val density = remember(scaling, scale) {
        Density(scaling.base.density * scale, scaling.base.fontScale)
    }
    val parent = LocalViewConfiguration.current
    val viewConfiguration = remember(parent, scale) { RealTouchTargetViewConfiguration(parent, scale) }
    CompositionLocalProvider(
        LocalDensity provides density,
        LocalViewConfiguration provides viewConfiguration,
        LocalActiveOverlayScale provides scale,
        // Material controls reserve at least 48 *real* dp (their layout footprint), not 48 virtual.
        LocalMinimumInteractiveComponentSize provides hitSizeFor(0.dp, scale),
    ) { content() }
}

/**
 * Compose expands pointer hit areas to the view configuration's minimum touch target, measured in
 * the density of the node. Inside a scaled scope that would be 48 *virtual* dp, so it is widened
 * to keep 48 real dp.
 */
private class RealTouchTargetViewConfiguration(
    private val delegate: ViewConfiguration,
    scale: Float,
) : ViewConfiguration by delegate {
    private val target = hitSizeFor(0.dp, scale)
    override val minimumTouchTargetSize: DpSize get() = DpSize(target, target)
}

/**
 * One height for the whole bottom HUD row (speed pill, ETA bar or status strip, Report button), in
 * real dp: the 1x row (a primary control plus its pill inset) scaled by the tile scale, never below
 * [MinRealTouchTarget] so the round Report button in it stays a full touch target.
 */
fun hudRowRealHeight(tileScale: Float): Dp =
    maxOf(MinRealTouchTarget, CarControl.rowHeight(CarControl.Primary) * tileScale)

/** [hudRowRealHeight] in the units of a scope drawn at [scale]. */
fun hudRowHeightIn(scale: Float, tileScale: Float): Dp = hudRowRealHeight(tileScale) / scale.coerceAtLeast(0.01f)

/** Smallest inset (real dp) between a pill's edge and the button, bubble or artwork touching it. */
val MinRealPillInset = 3.dp

/**
 * [CarInset.Pill] in the units of a scope drawn at [scale], never under [MinRealPillInset] on screen,
 * so a pill's own fill stays visible around what sits in it (speed cluster, chips, bars).
 */
fun pillInsetFor(scale: Float): Dp = maxOf(CarInset.Pill, MinRealPillInset / scale.coerceAtLeast(0.01f))

/** Diameter of the buttons inside a HUD row of [rowHeight] drawn at [scale]: the row minus the pill inset on both sides. */
fun hudRowButtonSize(rowHeight: Dp, scale: Float = 1f): Dp = rowHeight - pillInsetFor(scale) * 2

/** Largest visual size of a control embedded in a slim tile (ETA bar, strips) inside a shrunk scope. */
val EmbeddedControlVisual = 40.dp

/**
 * Visual size of a control that sits inside a slim tile: the regular size at 1x, at most
 * [EmbeddedControlVisual] when the scope is shrunk, so the tile stays slim while the touch target
 * is still held at 48 real dp by the scope's view configuration.
 */
@Composable
fun controlVisualSize(regular: Dp): Dp =
    if (LocalActiveOverlayScale.current >= 1f) regular else minOf(regular, EmbeddedControlVisual)

/**
 * [style] with its font size raised so the text is at least [minRealSp] after the current scope's
 * scale (line height follows). Without scaling this only enforces the same minimum.
 *
 * A Pen line box is the font's natural line ([PenLineRatio] of its size) rounded to a whole Pen pixel, at the size
 * the Pen draws it: a scaled tier's line is its own rounded line, not the Large line scaled (12.3sp text: a 14 line,
 * not 14.6, which the device would round up a pixel and push everything below it down). Lines the Pen set apart
 * from the natural one keep their proportion.
 */
@Composable
fun TextStyle.withFloor(minRealSp: Float): TextStyle {
    val scale = LocalActiveOverlayScale.current
    // Unscaled text is left alone: the floors only guard against shrinking.
    if (scale >= 1f || fontSize.isUnspecified) return this
    val size = floorSp(fontSize.value, scale, minRealSp)
    val line = when {
        lineHeight.isUnspecified -> lineHeight
        Math.round(fontSize.value * PenLineRatio).toFloat() == lineHeight.value ->
            (Math.round(size * scale * PenLineRatio) / scale).sp
        else -> (lineHeight.value * size / fontSize.value).sp
    }
    if (size <= fontSize.value && line == lineHeight) return this
    return copy(fontSize = size.sp, lineHeight = line)
}

/** Roboto's natural line height over its size: the Pen's line box before it rounds to a whole pixel. */
const val PenLineRatio = 1.172f
