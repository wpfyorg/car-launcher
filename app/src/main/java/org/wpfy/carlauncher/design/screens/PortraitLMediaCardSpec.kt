package org.wpfy.carlauncher.design.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The Portrait L "Media Card Wide" of the Pen's "with media" frame, kept by hand: the Drive Card Pen no longer carries it,
 * but Portrait L keeps this card while there is no Drive Card (Free without location permission or OBD). Values are the
 * Pen's, as generated before the Drive Card frames replaced it.
 */
internal object PortraitLWideMediaCard {
    val H = 232.dp
    val PadTop = 16.dp
    val PadEnd = 16.dp
    val PadBottom = 16.dp
    val PadStart = 16.dp
    val Gap = 20.dp
    val Radius = 20.dp
    val Fill = Color(0xFF1F2123)

    object Artwork {
        val W = 200.dp
        val H = 200.dp
        val Radius = 16.dp
        const val GradientRotation = 135.0f
        val Gradient = listOf(0.0f to Color(0xFFFF7A59), 0.55f to Color(0xFFC2185B), 1.0f to Color(0xFF3A1C71))

        object Icon {
            val W = 48.dp
            val Fill = Color(0x80FFFFFF)
        }
    }

    object Body {
        object Text {
            val Gap = 4.dp

            object Source {
                val Fill = Color(0xFF8D9298)
                val Style = TextStyle(fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight(500))
            }

            object Title {
                val Fill = Color(0xFFF1F0F4)
                val Style = TextStyle(fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight(700))
            }

            object Artist {
                val Fill = Color(0xFFDEE3EB)
                val Style = TextStyle(fontSize = 16.sp, lineHeight = 19.sp, fontWeight = FontWeight(400))
            }
        }

        object Progress {
            val H = 4.dp

            object Track {
                val H = 4.dp
                val Radius = 999.dp
                val Fill = Color(0xFF252626)

                object Done {
                    val H = 4.dp
                    val Radius = 999.dp
                    val Fill = Color(0xFF578CFF)
                }
            }
        }

        object Transport {
            val H = 64.dp

            object SkipPrevious {
                val W = 56.dp
                val Fill = Color(0xFF252626)

                object Icon {
                    val W = 28.dp
                    val Fill = Color(0xFFF1F0F4)
                }
            }

            object Pause {
                val W = 64.dp
                val Fill = Color(0xFF578CFF)

                object Icon {
                    val W = 28.dp
                    val Fill = Color(0xFFFFFFFF)
                }
            }

            object SkipNext {
                val W = 56.dp
                val Fill = Color(0xFF252626)

                object Icon {
                    val W = 28.dp
                    val Fill = Color(0xFFF1F0F4)
                }
            }
        }
    }
}
