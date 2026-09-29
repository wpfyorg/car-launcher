package org.wpfy.carlauncher.design.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Corner radii. Equivalent surfaces share one radius: multi-line cards use [Card], tiles nested in
 * a panel use [Control], the panels themselves (map pane, media card, sheets) use [Panel]. One-line
 * floating elements (bars, chips, buttons) are full pills ([CarShape.Pill]).
 */
object CarRadius {
    val Small = 12.dp
    val Control = 16.dp
    val Card = 20.dp
    val Panel = 24.dp
}

object CarShape {
    val Small = RoundedCornerShape(CarRadius.Small)
    val Control = RoundedCornerShape(CarRadius.Control)
    val Card = RoundedCornerShape(CarRadius.Card)
    val Panel = RoundedCornerShape(CarRadius.Panel)
    val Pill = RoundedCornerShape(percent = 50)
}

/**
 * Outer radius of a panel (map pane, media page) drawn at the real density. The cards inside are
 * drawn at [tileScale], so the panel shrinks with them to stay concentric; never below [CarRadius.Small].
 */
fun CarRadius.paneFor(tileScale: Float): Dp = (Panel * tileScale).coerceIn(Small, Panel)

/** Shadow depth: everything floating over the map, and modal sheets above it. */
object CarElevation {
    val Floating = 4.dp
    val Modal = 12.dp
}

/**
 * Control heights and diameters. A HUD row is one control plus [CarInset.Pill] above and below,
 * so a bar, the speed cluster and a free-standing button beside them share one height.
 */
object CarControl {
    /** Visual cap of a control embedded in a slim, scaled-down tile (touch target stays 48 real dp). */
    val Small = 40.dp
    val Regular = 48.dp
    val Primary = 56.dp
    /** Chips: now playing, next turn. Art or a control of [Regular] plus the pill inset. */
    val Chip = 56.dp

    /** Height of a HUD row (bar, speed cluster) holding controls of [controlSize]. */
    fun rowHeight(controlSize: Dp): Dp = controlSize + CarInset.Pill * 2
}

/**
 * Icon sizes: inline with text, inside any control (whatever its diameter), the leading glyph of
 * a card, the turn arrow.
 */
object CarIcon {
    val Small = 20.dp
    val Regular = 24.dp
    val Prominent = 32.dp
    val Hero = 40.dp
}

/** Insets and gaps, all on the [CarSpacing] scale. */
object CarInset {
    /** Between a pill's edge and a control, art or button that touches it. */
    val Pill = CarSpacing.Xs

    /** Between a pill's edge and text (or a bare icon) that starts it. */
    val PillText = CarSpacing.Lg

    val CardHorizontal = CarSpacing.Md
    val CardVertical = CarSpacing.Sm

    /** Between an icon and its label. */
    val IconLabel = CarSpacing.Sm

    /** Between groups inside one row (artwork and text, text parts of a bar). */
    val Group = CarSpacing.Md

    /** Between cards stacked on the map, and between HUD rows. */
    val Stack = CarSpacing.Sm

    /** Between floating map controls in a column. */
    val ControlGap = CarSpacing.Sm
}

/**
 * Text roles. Five sizes (28, 24, 20, 16, 14) and one weight per role: values bold, titles and
 * labels medium, buttons semibold, body regular.
 */
object CarText {
    /** Turn distance, current speed. */
    val Value = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
    val ValueCompact = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)

    /** Road name, media title, sheet title. */
    val Title = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Medium)
    val TitleCompact = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium)

    /** Secondary instruction, alert detail, bar text, choice labels, status titles. */
    val Label = TextStyle(fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)

    /** Text on buttons (End, Report, Undo, status actions). */
    val Button = TextStyle(fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)

    /** Alert type, chip title, subtitles, status body. */
    val Body = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal)

    /** Units, artist in the chip, hints, saved notes. */
    val Caption = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
}

/** Surfaces floating over the map. */
object CarSurface {
    /** Dark translucent fill of every floating bar, chip and map control. */
    val Scrim: Color = CarColors.Background.copy(alpha = 0.9f)

    /** The hairline that separates a floating element from the map under it. */
    val Hairline = BorderStroke(1.dp, CarColors.Outline)
}

/**
 * The Map HUD kit (Map Layouts.pen, "Map HUD Components"), in tile units: these are the kit's 1x
 * sizes, and small screens draw the tiles at 0.5, so the HUD matches the kit's v2 frames exactly.
 * Text never drops below [OverlayTextFloor] on screen.
 */
object HudKit {
    /** Turn card: radius, main row insets and gap, arrow, then-row insets. */
    val CardWidth = 340.dp
    val CardRadius = 20.dp
    val CardPaddingH = 16.dp
    val CardPaddingV = 14.dp
    val CardGap = 14.dp
    val TurnArrow = 44.dp
    val ThenPaddingV = 8.dp
    val ThenGap = 8.dp
    val ThenArrow = 20.dp
    val Distance = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
    val Road = TextStyle(fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal)
    val ThenLabel = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal)

    /** Camera alert: radius, insets, type badge, mute button. */
    val AlertRadius = 16.dp
    val AlertPaddingStart = 14.dp
    val AlertPaddingEnd = 8.dp
    val AlertPaddingV = 10.dp
    val AlertGap = 12.dp
    val AlertBadge = 40.dp
    val AlertIcon = 22.dp
    val AlertMute = 48.dp
    val AlertTitle = TextStyle(fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold)
    val AlertDetail = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal)

    /** Speed cluster: scrim pill around the speed bubble and the posted-limit sign. */
    val ClusterPadding = 8.dp
    val ClusterGap = 10.dp
    val SpeedBubble = 84.dp
    val LimitSign = 64.dp
    val LimitRing = 7.dp
    val SpeedValue = TextStyle(fontSize = 32.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
    val SpeedUnit = TextStyle(fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.Normal)
    val LimitValue = TextStyle(fontSize = 24.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold)

    /** ETA bar: one pill, arrival, remaining time and distance, End. */
    val EtaHeight = 64.dp
    val EtaPaddingStart = 22.dp
    val EtaPaddingEnd = 8.dp
    val EtaGap = 18.dp
    val EndHeight = 48.dp
    val EndPaddingH = 18.dp
    val EndGap = 6.dp
    val EndIcon = 20.dp
    val Arrival = TextStyle(fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold)
    val Remaining = TextStyle(fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal)
    val EndLabel = TextStyle(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
    val ArrivalColor = Color(0xFF5BBF6B)

    /** Round map controls and the Report button (control units). */
    val Control = 56.dp
    val ControlIcon = 24.dp
    val ControlGap = 12.dp

    /** Now-playing and next-turn chips. */
    val ChipHeight = 72.dp
    val ChipPadding = 8.dp
    val ChipGap = 12.dp
    val ChipArt = 56.dp
    val ChipPlayWidth = 90.dp
    val ChipPlayIcon = 36.dp
    val ChipTextMax = 150.dp
    val ChipTitle = TextStyle(fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
    val ChipCaption = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal)

    /** Between stacked cards, and between the bottom row's elements. */
    val StackGap = 16.dp

    /**
     * Report-sent pill (Pen "Report Sent Pill"): one Report-button high, green ring, check, title
     * over the saved note, then an Undo pill. Control units; its text follows the tile scale.
     */
    val SentHeight = 56.dp
    val SentRing = 2.dp
    val SentPaddingStart = 10.dp
    val SentPaddingEnd = 8.dp
    val SentGap = 9.dp
    val SentCheck = 35.dp
    val SentUndoHeight = 40.dp
    val SentUndoPaddingStart = 11.dp
    val SentUndoPaddingEnd = 14.dp
    val SentUndoGap = 5.dp
    val SentUndoIcon = 20.dp
    val SentTitle = TextStyle(fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold)
    val SentNote = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal)
    val SentUndo = TextStyle(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
    val SentSeconds = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal)
    val SentRingColor = Color(0xFF5BBF6B)

    /**
     * Status strip (Pen "Status Strip"): the ETA bar's pill with a 48 badge at the start (arrow 34),
     * title over caption, and a 48 round search button at the end (icon 30). Tile units.
     */
    val StripPadding = 8.dp
    val StripGap = 12.dp
    val StripBadge = 48.dp
    val StripBadgeIcon = 34.dp
    val StripAction = 48.dp
    val StripActionIcon = 30.dp
    val StripTitle = TextStyle(fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
    val StripBody = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal)

    /** Report sheet (Pen "Report Sheet"): control units. */
    val SheetRadius = 24.dp
    val SheetPadding = 16.dp
    val SheetGap = 12.dp
    val SheetClose = 44.dp
    val SheetCloseIcon = 20.dp
    val SheetTitle = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
    val SheetSubtitle = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal)
    val ChoiceGap = 8.dp
    val ChoiceHeight = 56.dp
    val ChoiceRadius = 16.dp
    val ChoicePaddingH = 10.dp
    val ChoiceInnerGap = 10.dp
    val ChoiceBadge = 36.dp
    val ChoiceIcon = 20.dp
    val ChoiceLabel = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
}
