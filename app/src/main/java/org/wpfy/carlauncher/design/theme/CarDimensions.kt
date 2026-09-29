package org.wpfy.carlauncher.design.theme

import androidx.compose.ui.unit.dp

object CarDimensions {
    val HeaderHeight = 80.dp
    val HeaderHeightCompact = 64.dp
    val HeaderHeightTight = 56.dp
    val MinimumTouchTarget = 56.dp

    /** Content margin around the main surface; keeps cards off the screen edge and the rail. */
    val ContentMargin = 12.dp

    /** Cards that hold readable status text never grow past this, however wide the window. */
    val StatusCardMaxWidth = 400.dp

    val IconSm = 24.dp
    val IconMd = 32.dp
    val IconLg = 40.dp
}
