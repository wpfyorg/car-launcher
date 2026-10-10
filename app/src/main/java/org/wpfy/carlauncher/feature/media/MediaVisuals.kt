package org.wpfy.carlauncher.feature.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Tint of the tile that stands in for missing artwork, everywhere media is shown. */
internal val ArtworkFallbackTop = Color(0xFF3A2A5C)
internal val ArtworkFallbackBottom = Color(0xFF14161C)

/** Width [MediaSourceDots] occupies, so overlapping text can reserve room for it. */
internal fun mediaSourceDotsWidth(count: Int): Dp =
    if (count <= 1) 0.dp else ActiveDotSize + (InactiveDotSize + DotSpacing) * (count - 1)

private val ActiveDotSize = 8.dp
private val InactiveDotSize = 6.dp
private val DotSpacing = 6.dp

@Composable
internal fun MediaSourceDots(
    count: Int,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.White.copy(alpha = 0.38f),
) {
    if (count <= 1) return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DotSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(if (index == selectedIndex) ActiveDotSize else InactiveDotSize)
                    .background(
                        color = if (index == selectedIndex) activeColor else inactiveColor,
                        shape = CircleShape,
                    ),
            )
        }
    }
}
