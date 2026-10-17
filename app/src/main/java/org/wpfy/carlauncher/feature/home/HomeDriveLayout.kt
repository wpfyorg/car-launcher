package org.wpfy.carlauncher.feature.home

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.wpfy.carlauncher.design.kit.PenScreens
import org.wpfy.carlauncher.design.kit.ScreenGroup

/**
 * Whether Home draws the Drive Card in the card slot: with a speed source (Pro accessory, OBD or, on Free, location
 * permission, whose fix may be briefly stale: the card stays and only its speed goes). With none the media card stays.
 */
internal fun homeShowsDriveCard(navigationExpanded: Boolean, hasAccessory: Boolean, hasObdSpeed: Boolean, hasGpsSource: Boolean = false): Boolean =
    !navigationExpanded && (hasAccessory || hasObdSpeed || hasGpsSource)

/**
 * Whether the Drive Card is actually on screen: on a paged Small screen the full-pane media page covers it while it shows
 * (even part-way in). The Pro accessory is told this, so a camera confirmation waits for a card that can be seen.
 */
internal fun homeDriveCardOnScreen(driveCard: Boolean, paged: Boolean, mediaPageShown: Boolean): Boolean =
    driveCard && !(paged && mediaPageShown)

internal const val LANDSCAPE_L_CARD_RATIO = 0.614f

/** The Pen's tall Drive Card of a landscape group: its width over its height (Idle frame), so the card is [driveCardWidth] wide at any height. */
private fun tallRatio(group: ScreenGroup): Float = when (group) {
    ScreenGroup.LandscapeS -> PenScreens.LandscapeS.Idle.DriveCard.W / PenScreens.LandscapeS.Idle.DriveCard.H
    // Owner rule: in every L frame the card is 0.614 x its height (353.7x576, 427.4x696).
    ScreenGroup.LandscapeL, ScreenGroup.UltrawideL -> LANDSCAPE_L_CARD_RATIO
    ScreenGroup.LandscapeXL -> PenScreens.LandscapeXL.Idle.DriveCard.W / PenScreens.LandscapeXL.Idle.DriveCard.H
    else -> PenScreens.LandscapeM.Idle.DriveCard.W / PenScreens.LandscapeM.Idle.DriveCard.H
}

/** The tall Drive Card's width at [height]: the group's Pen proportion (Landscape S: the Pen's fixed width). */
internal fun driveCardWidth(group: ScreenGroup, height: Dp): Dp =
    // Landscape S: the Pen's card is the same width at both of its heights (296 and 300dp), so it is not proportional.
    if (group == ScreenGroup.LandscapeS) PenScreens.LandscapeS.Idle.DriveCard.W else height * tallRatio(group)

/**
 * The wide Drive Card's height at [width] (Portrait S and L): the Pen's proportion, and with large text the Pen's taller
 * "large text" frame, reached at font scale 1.3 and eased from 1.0 (Compose scales the text non-linearly in between).
 */
internal fun driveWideHeight(group: ScreenGroup, width: Dp, fontScale: Float): Dp {
    val portraitS = group == ScreenGroup.PortraitS
    val frameWidth = if (portraitS) PenScreens.PortraitS.Idle.DriveCard.W else PenScreens.PortraitL.Idle.DriveCard.W
    val natural = if (portraitS) PenScreens.PortraitS.Idle.DriveCard.H else PenScreens.PortraitL.Idle.DriveCard.H
    val large = if (portraitS) PenScreens.PortraitS.GuideLargeText.DriveCard.H else PenScreens.PortraitL.GuideLargeText.DriveCard.H
    val t = ((fontScale - 1f) / 0.3f).coerceIn(0f, 1f)
    return (natural + (large - natural) * t) * (width / frameWidth)
}
