package org.wpfy.carlauncher.design.kit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import org.wpfy.carlauncher.design.kit.PenKit.MediaCard.Controls.Pause as PlayControl
import org.wpfy.carlauncher.design.kit.PenKit.MediaCard.Controls.SkipNext as NextControl
import org.wpfy.carlauncher.design.kit.PenKit.MediaCard.Controls.SkipPrevious as PreviousControl
import org.wpfy.carlauncher.design.kit.PenKit.MediaCard.Progress as ProgressKit
import org.wpfy.carlauncher.design.theme.FixedScale

/**
 * A Pen linear gradient: [stops] (position to colour) along the Pen's [rotation] in degrees (0 runs
 * bottom to top, angles turn counter-clockwise: 90 runs right to left, 180 top to bottom, 135 top
 * right to bottom left); the gradient line crosses the box through its centre and spans the box's
 * projection on it, as the Pen draws it.
 */
internal fun Modifier.penGradient(stops: List<Pair<Float, Color>>, rotation: Float, shape: Shape): Modifier =
    clip(shape).drawWithCache {
        val brush = penBrush(stops, rotation, size)
        onDrawBehind { drawRect(brush) }
    }

/** The media card's artwork stand-in when there is no album art: the Pen artwork gradient. */
val KitArtworkGradient: List<Pair<Float, Color>> get() = PenKit.MediaCard.Artwork.Gradient

// --- Media card --------------------------------------------------------------------------------

/**
 * The media card (Pen "Media Card · Large"), fitted to [height]: the whole card is the 1x kit card
 * drawn at height / [PenKit.MediaCard.H], so every part keeps the Pen proportions; text keeps the
 * 11sp floor and follows the user's font scale, and the artwork takes what the text leaves.
 */
@Composable
fun KitMediaCard(
    source: String,
    title: String,
    artist: String,
    art: ImageBitmap?,
    elapsed: String,
    total: String,
    progress: Float,
    playing: Boolean,
    height: Dp,
    modifier: Modifier = Modifier,
    sourceIcon: ImageVector = PenKit.MediaCard.Source.Icon.Vector,
    onPrevious: () -> Unit = {},
    onPlayPause: () -> Unit = {},
    onNext: () -> Unit = {},
    /** Transport controls the media session does not offer draw disabled, as on the live media screen. */
    canSkipPrevious: Boolean = true,
    canPlayPause: Boolean = true,
    canSkipNext: Boolean = true,
) {
    val c = "MediaCard"
    val scale = height / PenKit.MediaCard.H
    Box(modifier.width(PenKit.MediaCard.W * scale).height(height)) {
        FixedScale(scale) {
            Column(
                modifier = Modifier
                    .pen(c)
                    .width(PenKit.MediaCard.W)
                    .fillMaxHeight()
                    .mediaArtBackground(art, listOf(0f to PenKit.MediaCard.Fill, 1f to PenKit.MediaCard.Fill), 180f, penShape(PenKit.MediaCard.Radius), PenKit.MediaCard.Text.Artist.Fill)
                    .padding(
                        start = PenKit.MediaCard.PadStart, top = PenKit.MediaCard.PadTop,
                        end = PenKit.MediaCard.PadEnd, bottom = PenKit.MediaCard.PadBottom,
                    ),
                verticalArrangement = Arrangement.spacedBy(PenKit.MediaCard.Gap),
            ) {
                Row(
                    modifier = Modifier.pen(c, "Source").fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PenKit.MediaCard.Source.Gap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(sourceIcon, null, Modifier.pen(c, "Source/Icon").size(PenKit.MediaCard.Source.Icon.W), tint = PenKit.MediaCard.Source.Icon.Fill)
                    PenText(source, PenKit.MediaCard.Source.Label.Style, PenKit.MediaCard.Source.Label.Fill, Modifier.pen(c, "Source/Label"))
                }
                Box(
                    modifier = Modifier
                        .pen(c, "Artwork")
                        .fillMaxWidth()
                        .weight(1f)
                        .penGradient(PenKit.MediaCard.Artwork.Gradient, PenKit.MediaCard.Artwork.GradientRotation, penShape(PenKit.MediaCard.Artwork.Radius)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (art != null) {
                        Image(art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(PenKit.MediaCard.Artwork.Note.Vector, null, Modifier.pen(c, "Artwork/Note").size(PenKit.MediaCard.Artwork.Note.W), tint = PenKit.MediaCard.Artwork.Note.Fill)
                    }
                }
                Column(
                    modifier = Modifier.pen(c, "Text").fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(PenKit.MediaCard.Text.Gap),
                ) {
                    PenText(title, PenKit.MediaCard.Text.Title.Style, PenKit.MediaCard.Text.Title.Fill, Modifier.pen(c, "Text/Title").fillMaxWidth())
                    PenText(artist, PenKit.MediaCard.Text.Artist.Style, PenKit.MediaCard.Text.Artist.Fill, Modifier.pen(c, "Text/Artist").fillMaxWidth())
                }
                KitMediaProgress(c, "Progress", progress, elapsed, total)
                Row(
                    modifier = Modifier.pen(c, "Controls").fillMaxWidth().height(PenKit.MediaCard.Controls.H)
                        .padding(start = PenKit.MediaCard.Controls.PadStart, end = PenKit.MediaCard.Controls.PadEnd),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PenDisc(PreviousControl.W, PreviousControl.Fill, PreviousControl.Icon.Vector, PreviousControl.Icon.W, PreviousControl.Icon.Fill, Modifier.pen(c, "Controls/SkipPrevious"), iconModifier = Modifier.pen(c, "Controls/SkipPrevious/Icon"), description = "Previous", onClick = onPrevious, enabled = canSkipPrevious)
                    PenDisc(
                        PlayControl.W, PlayControl.Fill, if (playing) PlayControl.Icon.Vector else PenIcons.PlayArrow, PlayControl.Icon.W, PlayControl.Icon.Fill,
                        Modifier.pen(c, "Controls/Pause"), iconModifier = Modifier.pen(c, "Controls/Pause/Icon"), description = "Play or pause", onClick = onPlayPause, enabled = canPlayPause,
                    )
                    PenDisc(NextControl.W, NextControl.Fill, NextControl.Icon.Vector, NextControl.Icon.W, NextControl.Icon.Fill, Modifier.pen(c, "Controls/SkipNext"), iconModifier = Modifier.pen(c, "Controls/SkipNext/Icon"), description = "Next", onClick = onNext, enabled = canSkipNext)
                }
            }
        }
    }
}

/** The card's progress block: track with the played part, and the two times at its ends. */
@Composable
private fun KitMediaProgress(c: String, path: String, progress: Float, elapsed: String, total: String) {
    Column(Modifier.pen(c, path).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ProgressKit.Gap)) {
        Box(
            Modifier.pen(c, "$path/Track").fillMaxWidth().height(ProgressKit.Track.H)
                .clip(penShape(ProgressKit.Track.Radius)).background(ProgressKit.Track.Fill),
        ) {
            Box(
                Modifier.pen(c, "$path/Track/Done").fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight()
                    .clip(penShape(ProgressKit.Track.Done.Radius)).background(ProgressKit.Track.Done.Fill),
            )
        }
        Row(Modifier.pen(c, "$path/Times").fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PenText(elapsed, ProgressKit.Times.Elapsed.Style, ProgressKit.Times.Elapsed.Fill, Modifier.pen(c, "$path/Times/Elapsed"))
            PenText(total, ProgressKit.Times.Total.Style, ProgressKit.Times.Total.Fill, Modifier.pen(c, "$path/Times/Total"))
        }
    }
}

// --- Search button -----------------------------------------------------------------------------

@Composable
fun KitSearchButton(modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    PenDisc(
        PenKit.SearchButton.W, PenKit.SearchButton.Fill, PenKit.SearchButton.Icon.Vector, PenKit.SearchButton.Icon.W, PenKit.SearchButton.Icon.Fill,
        modifier.pen("SearchButton"), iconModifier = Modifier.pen("SearchButton", "Icon"), description = "Search destination", onClick = onClick,
    )
}

/** A Pen frame's padding, all four sides. */
internal fun Modifier.penPadding(top: Dp, end: Dp, bottom: Dp, start: Dp): Modifier =
    padding(start = start, top = top, end = end, bottom = bottom)

/** A tap, and a long press when [onLongClick] is set. */
@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.penLongClick(onClick: () -> Unit, onLongClick: (() -> Unit)?): Modifier =
    if (onLongClick == null) clickable(onClick = onClick)
    else combinedClickable(onClick = onClick, onLongClick = onLongClick)

/** A clickable that does nothing when [onClick] is null. */
internal fun Modifier.penClick(onClick: (() -> Unit)?): Modifier =
    if (onClick != null) clickable(onClick = onClick) else this
