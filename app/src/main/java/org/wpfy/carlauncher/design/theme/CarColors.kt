package org.wpfy.carlauncher.design.theme

import androidx.compose.ui.graphics.Color

object CarColors {
    val Background = Color(0xFF151616)
    val Surface = Color(0xFF1F2123)
    val SurfaceContainer = Color(0xFF252626)
    val SurfaceElevated = Color(0xFF393E45)
    val Outline = Color(0xFF444746)

    val Accent = Color(0xFF578CFF)
    val AccentStrong = Color(0xFF1A73E8)
    val AccentMuted = Color(0xFFB6D8FF)

    val TextPrimary = Color(0xFFF1F0F4)
    val TextSecondary = Color(0xFFDEE3EB)
    val TextDisabled = Color(0xFF8D9298)

    /** Content drawn on top of [AccentMuted] fills (primary actions, badges). */
    val OnAccentMuted = Color(0xFF002C6F)

    /** Selected navigation destination: tinted container + [AccentMuted] glyph. */
    val SelectedContainer = Color(0xFF1B2F57)

    val Success = Color(0xFF5BBF6B)
    val GuidanceTurn = Color(0xFF006A54)
    val GuidanceNext = Color(0xFF015141)

    /** Caution fills and glyphs (approaching hazards); content on it uses [OnWarning]. */
    val Warning = Color(0xFFFFC145)
    val OnWarning = Color(0xFF2B1D00)

    /** Urgent state (limit exceeded, imminent hazard): [Error] glyphs, [ErrorContainer] fills. */
    val Error = Color(0xFFFF8A80)
    val ErrorContainer = Color(0xFFB3261E)
    val OnErrorContainer = Color(0xFFFFFFFF)
}
