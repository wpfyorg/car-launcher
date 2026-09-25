package org.wpfy.carlauncher.design.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.wpfy.carlauncher.design.kit.*

/** A Pen node's box, relative to its parent. */
internal class MediaFrame(val x: Dp, val y: Dp, val w: Dp, val h: Dp)

internal class MediaSource(val frame: MediaFrame, val icon: MediaFrame, val iconFill: Color, val label: MediaFrame, val labelStyle: TextStyle, val labelFill: Color)

internal class MediaDisc(val frame: MediaFrame, val fill: Color, val icon: MediaFrame, val iconFill: Color)

/**
 * Measures of a Pen media pane (the gradient pane of the media page and the media screens): every
 * node's box relative to its parent, as the Pen group draws it. The block of nodes is centred in the
 * pane, vertical positions are the Pen's.
 */
internal class MediaPaneSpec(
    val radius: Dp,
    val gradient: List<Pair<Float, Color>>,
    val rotation: Float,
    val source: MediaSource?,
    val artwork: MediaFrame, val artworkRadius: Dp, val artworkGradient: List<Pair<Float, Color>>, val artworkRotation: Float,
    val artworkIcon: MediaFrame, val artworkIconFill: Color,
    val text: MediaFrame, val title: MediaFrame, val titleStyle: TextStyle, val titleFill: Color,
    val artist: MediaFrame, val artistStyle: TextStyle, val artistFill: Color,
    val progress: MediaFrame, val track: MediaFrame, val trackRadius: Dp, val trackFill: Color, val doneRadius: Dp, val doneFill: Color,
    val times: MediaFrame, val elapsed: MediaFrame, val total: MediaFrame, val timesStyle: TextStyle, val timesFill: Color,
    val transport: MediaFrame, val transportGap: Dp?,
    val previous: MediaDisc, val pause: MediaDisc, val next: MediaDisc,
    /** Portrait panes: every node but the source line is centred on its own (they differ in width), not as one block. */
    val centreEach: Boolean = false,
    /** The title and the artist fill their node's width with centred text (Portrait L). */
    val centreText: Boolean = false,
) {
    private val left get() = minOf(source?.frame?.x ?: artwork.x, artwork.x)
    private val right get() = maxOf(text.x + text.w, progress.x + progress.w, transport.x + transport.w)

    /** Offset that centres the block of nodes in a pane [paneWidth] wide. */
    fun shift(paneWidth: Dp): Dp = (paneWidth - (right - left)) / 2 - left

    /** Offset of [node] in a pane [paneWidth] wide: the block's, or the node's own centring. */
    fun shiftOf(node: MediaFrame, paneWidth: Dp): Dp = if (centreEach) (paneWidth - node.w) / 2 - node.x else shift(paneWidth)
}

private val MinSourceHit = 48.dp

private fun TextStyle.aligned(centre: Boolean): TextStyle = if (centre) copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center) else this

@Composable
private fun At(f: MediaFrame, tag: String, dx: Dp = 0.dp, content: @Composable BoxScope.() -> Unit = {}) {
    Box(Modifier.offset(f.x + dx, f.y).size(f.w, f.h).testTag(tag), content = content)
}

/**
 * The nodes of a media pane, tagged "<tag>/<Pen name>". [paneWidth] is the pane's width (the block
 * is centred in it). The pane's own fill is the caller's.
 */
@Composable
internal fun MediaPaneBody(
    spec: MediaPaneSpec, media: KitMediaState, paneWidth: Dp, tag: String, actions: KitMediaActions = KitMediaActions(),
    /** The pane is scaled to fit its window: text honours large font sizes until it would leave its frame, then shrinks; the source tap stays 48dp on screen. */
    fit: Boolean = false,
    /** The scale the pane is drawn at ([FitPane]). */
    scale: Float = 1f,
) {
    Box(Modifier.fillMaxSize()) {
        spec.source?.let { s ->
            At(s.frame, "$tag/Source", if (spec.centreEach) 0.dp else spec.shift(paneWidth)) {
                actions.onSource?.let { onSource ->
                    // The Pen frame is far smaller than a finger: an invisible hit box of at least 48dp on screen, centred on it.
                    val hit = MinSourceHit / scale
                    Box(
                        Modifier.align(Alignment.Center).requiredSize(maxOf(s.frame.w, hit), maxOf(s.frame.h, hit)).testTag("$tag/Source/Hit")
                            .semantics { contentDescription = media.source }.clickable(role = Role.Button, onClick = onSource),
                    )
                }
                At(s.icon, "$tag/Source/Icon") {
                    // The live source app's own icon replaces the Pen's glyph.
                    if (media.sourceIcon != null) Image(media.sourceIcon, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    else Icon(PenIcons.Bluetooth, null, Modifier.fillMaxSize(), tint = s.iconFill)
                }
                At(s.label, "$tag/Source/Label") { PenText(media.source, s.labelStyle, s.labelFill, fitHeight = if (fit) s.label.h else null) }
            }
        }
        At(spec.artwork, "$tag/Artwork", spec.shiftOf(spec.artwork, paneWidth)) {
            Box(Modifier.fillMaxSize().penGradient(spec.artworkGradient, spec.artworkRotation, penShape(spec.artworkRadius))) {
                if (media.art != null) Image(media.art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            if (media.art == null) {
                At(spec.artworkIcon, "$tag/Artwork/Icon") { Icon(PenIcons.MusicNote, null, Modifier.fillMaxSize(), tint = spec.artworkIconFill) }
            }
        }
        At(spec.text, "$tag/Text", spec.shiftOf(spec.text, paneWidth)) {
            At(spec.title, "$tag/Text/Title") { PenText(media.title, spec.titleStyle.aligned(spec.centreText), spec.titleFill, Modifier.fillMaxWidth(), fitHeight = if (fit) spec.title.h else null) }
            At(spec.artist, "$tag/Text/Artist") {
                // The Pen sets a wrapped line's box height as the text's line height: n lines of the title's ratio.
                val line = spec.artistStyle.fontSize * (spec.titleStyle.lineHeight.value / spec.titleStyle.fontSize.value)
                val lines = (spec.artistStyle.lineHeight.value / line.value).let { Math.round(it) }.coerceAtLeast(1)
                PenText(media.artist, spec.artistStyle.copy(lineHeight = line).aligned(spec.centreText), spec.artistFill, Modifier.fillMaxWidth().wrapContentHeight(Alignment.Top, unbounded = true), maxLines = lines, fitHeight = if (fit) spec.artist.h else null)
            }
        }
        // While a finger is on the bar the dragged fraction is drawn; the seek is made once, on release.
        var dragged by remember { mutableStateOf<Float?>(null) }
        val onSeek = actions.onSeek.takeIf { media.canSeek }
        val shown = (dragged ?: media.progress).coerceIn(0f, 1f)
        At(spec.progress, "$tag/Progress", spec.shiftOf(spec.progress, paneWidth)) {
            At(spec.track, "$tag/Progress/Track") {
                Box(Modifier.fillMaxSize().clip(penShape(spec.trackRadius)).background(spec.trackFill)) {
                    Box(
                        Modifier.testTag("$tag/Progress/Track/Done").size(spec.track.w * shown, spec.track.h)
                            .clip(penShape(spec.doneRadius)).background(spec.doneFill),
                    )
                }
                if (onSeek != null) {
                    // The track is far thinner than a finger: an invisible touch area of at least 48dp on screen, centred on it.
                    val hit = MinSourceHit / scale
                    SeekArea(
                        Modifier.align(Alignment.Center).requiredSize(spec.track.w, maxOf(spec.track.h, hit)).testTag("$tag/Progress/Seek")
                            .semantics { contentDescription = "Seek"; progressBarRangeInfo = ProgressBarRangeInfo(shown, 0f..1f); setProgress { value -> onSeek(value.coerceIn(0f, 1f)); true } },
                        onDrag = { dragged = it },
                        onSeek = { dragged = null; onSeek(it) },
                        onCancel = { dragged = null },
                    )
                }
            }
            At(spec.times, "$tag/Progress/Times") {
                At(spec.elapsed, "$tag/Progress/Times/Elapsed") { PenText(dragged?.let { media.elapsedAt?.invoke(it) } ?: media.elapsed, spec.timesStyle, spec.timesFill, fitHeight = if (fit) spec.elapsed.h else null) }
                At(spec.total, "$tag/Progress/Times/Total") { PenText(media.total, spec.timesStyle, spec.timesFill, fitHeight = if (fit) spec.total.h else null) }
            }
        }
        At(spec.transport, "$tag/Transport", spec.shiftOf(spec.transport, paneWidth)) {
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = spec.transportGap?.let { Arrangement.spacedBy(it, Alignment.CenterHorizontally) } ?: Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MediaDiscView(spec.previous, PenIcons.SkipPrevious, "$tag/Transport/SkipPrevious", "Previous", actions.onPrevious, media.canSkipPrevious)
                MediaDiscView(spec.pause, if (media.playing) PenIcons.Pause else PenIcons.PlayArrow, "$tag/Transport/Pause", "Play or pause", actions.onPlayPause, media.canPlayPause)
                MediaDiscView(spec.next, PenIcons.SkipNext, "$tag/Transport/SkipNext", "Next", actions.onNext, media.canSkipNext)
            }
        }
    }
}

@Composable
private fun MediaDiscView(d: MediaDisc, icon: androidx.compose.ui.graphics.vector.ImageVector, tag: String, description: String, onClick: () -> Unit, enabled: Boolean = true) {
    PenDisc(d.frame.w, d.fill, icon, d.icon.w, d.iconFill, Modifier.testTag(tag), iconModifier = Modifier.testTag("$tag/Icon"), description = description, onClick = onClick, enabled = enabled)
}

/**
 * [content] laid out at the Pen pane's size [pen] and scaled uniformly, centred, to fit the pane it is in
 * (a window between the Pen's frame sizes). The scale is a layer's, so touch targets follow it.
 */
@Composable
internal fun FitPane(pen: DpSize, content: @Composable (scale: Float) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // A zero-sized pane (mid-layout) must not make the scale 0: the hit target divides by it.
        val scale = minOf(maxWidth / pen.width, maxHeight / pen.height).coerceAtLeast(MinFitScale)
        Box(Modifier.requiredSize(pen.width, pen.height).graphicsLayer { scaleX = scale; scaleY = scale }) { content(scale) }
    }
}

/**
 * A media screen (Pen "MediaScreen" of a landscape group): the dock along the start edge and the
 * gradient pane (the album art background when there is art) beside it, inset by the Pen's margin from the top, end and bottom edges, with the
 * media pane's nodes centred in it. [compactDock] is the Small tier's dock, otherwise the landscape dock at [dockScale].
 * Tags: "Screen/Dock", "Screen/Pane", "Screen/Pane/<Pen name>". Without [showDock] only the pane is drawn, as large as [modifier] makes it; with [fitPane] its nodes keep the Pen's layout at [penPane] (the Pen pane's size) and are scaled uniformly, centred, to fit the pane.
 */
@Composable
internal fun MediaScreenLayout(
    dock: KitDockState,
    media: KitMediaState,
    spec: MediaPaneSpec,
    paneX: Dp,
    paneY: Dp,
    paneRadius: Dp,
    fill: Color,
    compactDock: Boolean,
    modifier: Modifier = Modifier,
    dockActions: KitDockActions = KitDockActions(),
    mediaActions: KitMediaActions = KitMediaActions(),
    tag: String = "Screen",
    dockScale: Float = PenKit.DockScale.Medium,
    showDock: Boolean = true,
    fitPane: Boolean = false,
    penPane: DpSize = DpSize.Unspecified,
) {
    if (!showDock) {
        // Only the pane, filling what the caller gives it (the live shell draws the dock and leaves the margins).
        BoxWithConstraints(modifier.fillMaxSize().testTag("$tag/Pane").mediaArtBackground(media.art, spec.gradient, spec.rotation, penShape(paneRadius), spec.artistFill)) {
            if (fitPane && penPane != DpSize.Unspecified) FitPane(penPane) { scale -> MediaPaneBody(spec, media, penPane.width, "$tag/Pane", mediaActions, fit = true, scale = scale) }
            else MediaPaneBody(spec, media, maxWidth, "$tag/Pane", mediaActions)
        }
        return
    }
    BoxWithConstraints(modifier.fillMaxSize().background(fill)) {
        val height = maxHeight
        val width = maxWidth
        Box(Modifier.testTag("$tag/Dock")) {
            if (compactDock) KitDockLandscapeCompact(dock.clock, dock.notificationCount, height, actions = dockActions)
            else KitDockLandscape(dock.clock, dock.notificationCount, dock.apps, height, dockScale, actions = dockActions)
        }
        val paneWidth = width - paneX - paneY
        Box(
            Modifier.offset(paneX, paneY).size(paneWidth, height - paneY * 2).testTag("$tag/Pane")
                .mediaArtBackground(media.art, spec.gradient, spec.rotation, penShape(paneRadius), spec.artistFill),
        ) {
            MediaPaneBody(spec, media, paneWidth, "$tag/Pane", mediaActions)
        }
    }
}

/** The smallest fit scale, so a zero-sized pane keeps every size derived from the scale finite. */
private const val MinFitScale = 0.01f
