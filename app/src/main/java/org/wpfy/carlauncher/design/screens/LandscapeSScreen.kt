package org.wpfy.carlauncher.design.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import org.wpfy.carlauncher.design.kit.KitDockActions
import org.wpfy.carlauncher.design.kit.KitDockApp
import org.wpfy.carlauncher.design.kit.KitDockLandscapeCompact
import org.wpfy.carlauncher.design.kit.KitDriveActions
import org.wpfy.carlauncher.design.kit.KitDriveCard
import org.wpfy.carlauncher.design.kit.KitDriveState
import org.wpfy.carlauncher.design.kit.PenScreens
import org.wpfy.carlauncher.design.kit.mediaArtBackground
import org.wpfy.carlauncher.design.kit.penShape
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeS.Idle.MapPane as Pane
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeS.MediaPage.Pane as MediaPagePane

/** What every dock shows: the clock, the notification count, the pinned apps (not on compact docks). */
@Immutable
data class KitDockState(val clock: String, val notificationCount: Int, val apps: List<KitDockApp> = emptyList())

/**
 * A Landscape S screen (Pen "Cluster · Landscape S", 533×320dp frames and their group): the compact
 * dock along the start edge, the map pane beside it with the HUD over [map]. Dock and pane keep the
 * Pen's measures (pane inset by the frame's margin, corner radius); the HUD lays itself out in the
 * pane ([LandscapeSHud]).
 *
 * Test tags follow the Pen frame: "Screen/Dock", "Screen/MapPane", "Screen/MapPane/<element>".
 */
@Composable
fun LandscapeSScreen(
    dock: KitDockState,
    hud: KitHudState,
    modifier: Modifier = Modifier,
    /** The media page's content (the HUD's mediaPage is set): drawn on the gradient pane under the page dots and next-turn chip. */
    media: KitMediaState? = null,
    dockActions: KitDockActions = KitDockActions(),
    hudActions: KitHudActions = KitHudActions(),
    tag: String = "Screen",
    /** The Drive Card at the pane's end edge (the Pen's tall card: its fixed width, the pane's height); null leaves the pane the whole area. */
    drive: KitDriveState? = null,
    driveActions: KitDriveActions = KitDriveActions(),
    map: @Composable () -> Unit = {},
) {
    BoxWithConstraints(modifier.fillMaxSize().background(PenScreens.LandscapeS.Idle.Fill)) {
        val height = maxHeight
        Box(Modifier.testTag("$tag/Dock")) { KitDockLandscapeCompact(dock.clock, dock.notificationCount, height, actions = dockActions) }
        // The pane's inset from the frame's top, end and bottom edges is the gap between dock and pane.
        val inset = Pane.Y
        val areaWidth = maxWidth - Pane.X - inset
        val paneHeight = maxHeight - inset * 2
        val card = PenScreens.LandscapeS.Idle.DriveCard
        val cardWidth = if (drive != null) card.W else 0.dp
        val paneWidth = if (drive != null) areaWidth - cardWidth - inset else areaWidth
        LandscapeSPane(
            hud, media, hudActions, tag = tag, map = map,
            modifier = Modifier.offset(Pane.X, Pane.Y).size(paneWidth, paneHeight),
        )
        if (drive != null) {
            Box(Modifier.offset(Pane.X + areaWidth - cardWidth, Pane.Y).testTag("$tag/DriveCard")) {
                KitDriveCard(drive, paneHeight, actions = driveActions, corner = Pane.Radius, width = cardWidth)
            }
        }
        // The report sheet is a child of the screen: centred, over the dock edge, its bottom at the Pen's gap.
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = LandscapeSReportSheetBottomGap)) { LandscapeSReportSheet(hud, hudActions, tag) }
    }
}

/**
 * The Landscape S pane alone (the dock is the shell's): the map pane with the HUD over [map], or, when
 * [hud] is on the media page and there is [media], the gradient pane with the media nodes. It fills the
 * space it is given ([modifier] sizes it); The media nodes scale down when the pane is smaller than the Pen's. Tags: "<tag>/MapPane" or "<tag>/Pane".
 */
@Composable
internal fun LandscapeSPane(
    hud: KitHudState,
    media: KitMediaState?,
    hudActions: KitHudActions = KitHudActions(),
    mediaActions: KitMediaActions = KitMediaActions(),
    tag: String = "Screen",
    modifier: Modifier = Modifier,
    map: @Composable () -> Unit = {},
) {
    // The media page is the Pen's "Pane": the gradient pane with the media nodes; the map pane is "MapPane".
    val mediaPage = hud.mediaPage && media != null
    val paneTag = if (mediaPage) "$tag/Pane" else "$tag/MapPane"
    val paneShape = penShape(Pane.Radius)
    BoxWithConstraints(
        modifier.testTag(paneTag)
            .then(if (mediaPage) Modifier.mediaArtBackground(media!!.art, MediaPagePane.Gradient, MediaPagePane.GradientRotation, paneShape, LandscapeSMediaPageSpec.artistFill) else Modifier.clip(paneShape)),
    ) {
        if (mediaPage) FitPane(DpSize(MediaPagePane.W, MediaPagePane.H)) { scale -> MediaPaneBody(LandscapeSMediaPageSpec, media!!, MediaPagePane.W, paneTag, mediaActions, fit = true, scale = scale) } else map()
        LandscapeSHud(hud, hudActions, tag = paneTag)
    }
}
