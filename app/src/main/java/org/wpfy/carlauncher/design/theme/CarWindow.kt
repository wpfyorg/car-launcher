package org.wpfy.carlauncher.design.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Usable window bounds in dp, plus the layout decisions derived from them.
 *
 * Layout choices must come from these density-independent bounds, never from raw pixel
 * resolution: pixels, density, font scale, insets and orientation are separate inputs, and a
 * 1920x720 panel at 240 dpi is a 1280x480dp window.
 */
@Immutable
class CarWindow(val widthDp: Float, val heightDp: Float) {
    val width: Dp get() = widthDp.dp
    val height: Dp get() = heightDp.dp

    val isPortrait: Boolean get() = heightDp > widthDp

    /** Too narrow to spare a side rail: phone-width portrait windows. */
    val isNarrow: Boolean get() = widthDp < NarrowWidthDp

    /** Short enough that vertical stacks need the tighter sizing (800x480-class landscape). */
    val isShort: Boolean get() = heightDp < ShortHeightDp

    /**
     * So short that every vertical dp counts (800x480 at 240 dpi is 533x320dp): headers tighten
     * and a bottom rail would take a fifth of the height.
     */
    val isVeryShort: Boolean get() = heightDp < VeryShortHeightDp

    val isUltrawide: Boolean
        get() = !isPortrait && widthDp >= UltrawideMinWidthDp && widthDp / heightDp >= UltrawideAspect

    /** Overlay and rail scale class of the window (see [overlayTierFor]). */
    val overlayTier: OverlayTier get() = overlayTierFor(widthDp, heightDp)

    /**
     * The rail position setting is honored in landscape only: portrait windows always take the
     * bottom dock (the Pen's portrait frames), narrow windows cannot spare a side rail, and very
     * short windows that are still wide enough for one cannot spare a bottom rail's height.
     */
    fun effectiveRailBottom(prefersBottom: Boolean): Boolean = when {
        isPortrait -> true
        isVeryShort && widthDp >= SideRailMinWidthDp -> false
        else -> prefersBottom || isNarrow
    }

    val headerHeight: Dp
        get() = when {
            isVeryShort -> CarDimensions.HeaderHeightTight
            isShort -> CarDimensions.HeaderHeightCompact
            else -> CarDimensions.HeaderHeight
        }

    companion object {
        val Default = CarWindow(widthDp = 1280f, heightDp = 720f)

        const val NarrowWidthDp = 600f
        const val ShortHeightDp = 520f
        const val VeryShortHeightDp = 400f
        const val SideRailMinWidthDp = 480f
        const val UltrawideMinWidthDp = 1100f
        const val UltrawideAspect = 2.0f
    }
}

val LocalCarWindow = compositionLocalOf { CarWindow.Default }
