package org.wpfy.carlauncher.design.kit

import org.wpfy.carlauncher.design.theme.CarWindow
import org.wpfy.carlauncher.design.theme.OverlayTier
import org.wpfy.carlauncher.design.theme.overlayTierFor

/**
 * The Pen "Screen Groups": every panel is laid out by the group of its window in dp (never its
 * pixels). Each group has its own Pen frames; panels inside a group share one arrangement and extra
 * width only widens the map. [tier] is the overlay scale class the group draws at. The Ultrawide
 * groups are the M and L landscape tiers at 2:1 or wider.
 */
enum class ScreenGroup(val tier: OverlayTier, val portrait: Boolean) {
    LandscapeS(OverlayTier.Small, portrait = false),
    LandscapeM(OverlayTier.Medium, portrait = false),
    UltrawideM(OverlayTier.Medium, portrait = false),
    LandscapeL(OverlayTier.Large, portrait = false),
    UltrawideL(OverlayTier.Large, portrait = false),
    LandscapeXL(OverlayTier.XLarge, portrait = false),
    PortraitS(OverlayTier.Small, portrait = true),
    PortraitL(OverlayTier.Large, portrait = true),
}

/** Narrowest Ultrawide frame in the Pen (1067x480dp); [CarWindow.UltrawideMinWidthDp] would miss it. */
private const val UltrawideGroupMinWidthDp = 1000f

/** The group of a window [widthDp] x [heightDp] (dp, app bounds). */
fun screenGroupFor(widthDp: Float, heightDp: Float): ScreenGroup {
    val tier = overlayTierFor(widthDp, heightDp)
    val ultrawide = widthDp >= UltrawideGroupMinWidthDp && widthDp / heightDp >= CarWindow.UltrawideAspect
    return if (heightDp > widthDp) {
        if (tier == OverlayTier.Small) ScreenGroup.PortraitS else ScreenGroup.PortraitL
    } else {
        when (tier) {
            OverlayTier.Small -> ScreenGroup.LandscapeS
            OverlayTier.Medium -> if (ultrawide) ScreenGroup.UltrawideM else ScreenGroup.LandscapeM
            OverlayTier.Large -> if (ultrawide) ScreenGroup.UltrawideL else ScreenGroup.LandscapeL
            OverlayTier.XLarge -> ScreenGroup.LandscapeXL
        }
    }
}

/** The group of [window]. */
fun screenGroupFor(window: CarWindow): ScreenGroup = screenGroupFor(window.widthDp, window.heightDp)
