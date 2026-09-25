package org.wpfy.carlauncher.design.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import org.wpfy.carlauncher.design.kit.KitDockActions
import org.wpfy.carlauncher.design.kit.PenKit
import org.wpfy.carlauncher.design.kit.PenScreens

/** The Landscape S media screen (Pen "MediaScreen" of "Cluster · Landscape S"): compact dock, gradient media pane. */
@Composable
fun LandscapeSMediaScreen(
    dock: KitDockState,
    media: KitMediaState,
    modifier: Modifier = Modifier,
    dockActions: KitDockActions = KitDockActions(),
    mediaActions: KitMediaActions = KitMediaActions(),
    tag: String = "Screen",
    showDock: Boolean = true,
    fitPane: Boolean = false,
) {
    MediaScreenLayout(dock, media, LandscapeSMediaSpec, PenScreens.LandscapeS.MediaScreen.Pane.X, PenScreens.LandscapeS.MediaScreen.Pane.Y, PenScreens.LandscapeS.MediaScreen.Pane.Radius, PenScreens.LandscapeS.MediaScreen.Fill, compactDock = true, modifier, dockActions, mediaActions, tag, showDock = showDock, fitPane = fitPane, penPane = DpSize(PenScreens.LandscapeS.MediaScreen.W - PenScreens.LandscapeS.MediaScreen.Pane.X - PenScreens.LandscapeS.MediaScreen.Pane.Y, PenScreens.LandscapeS.MediaScreen.H - PenScreens.LandscapeS.MediaScreen.Pane.Y * 2))
}

/** The Landscape M media screen (Pen "MediaScreen" of "Cluster · Landscape M"): Medium dock, gradient media pane. */
@Composable
fun LandscapeMMediaScreen(
    dock: KitDockState,
    media: KitMediaState,
    modifier: Modifier = Modifier,
    dockActions: KitDockActions = KitDockActions(),
    mediaActions: KitMediaActions = KitMediaActions(),
    tag: String = "Screen",
    showDock: Boolean = true,
    fitPane: Boolean = false,
) {
    MediaScreenLayout(dock, media, LandscapeMMediaSpec, PenScreens.LandscapeM.MediaScreen.Pane.X, PenScreens.LandscapeM.MediaScreen.Pane.Y, PenScreens.LandscapeM.MediaScreen.Pane.Radius, PenScreens.LandscapeM.MediaScreen.Fill, compactDock = false, modifier, dockActions, mediaActions, tag, showDock = showDock, fitPane = fitPane, penPane = DpSize(PenScreens.LandscapeM.MediaScreen.W - PenScreens.LandscapeM.MediaScreen.Pane.X - PenScreens.LandscapeM.MediaScreen.Pane.Y, PenScreens.LandscapeM.MediaScreen.H - PenScreens.LandscapeM.MediaScreen.Pane.Y * 2))
}

/** The Ultrawide M media screen (Pen "MediaScreen" of the ultrawide M group). */
@Composable
fun UltrawideMMediaScreen(
    dock: KitDockState,
    media: KitMediaState,
    modifier: Modifier = Modifier,
    dockActions: KitDockActions = KitDockActions(),
    mediaActions: KitMediaActions = KitMediaActions(),
    tag: String = "Screen",
    showDock: Boolean = true,
    fitPane: Boolean = false,
) {
    MediaScreenLayout(dock, media, UltrawideMMediaSpec, PenScreens.UltrawideM.MediaScreen.Pane.X, PenScreens.UltrawideM.MediaScreen.Pane.Y, PenScreens.UltrawideM.MediaScreen.Pane.Radius, PenScreens.UltrawideM.MediaScreen.Fill, compactDock = false, modifier, dockActions, mediaActions, tag, showDock = showDock, fitPane = fitPane, penPane = DpSize(PenScreens.UltrawideM.MediaScreen.W - PenScreens.UltrawideM.MediaScreen.Pane.X - PenScreens.UltrawideM.MediaScreen.Pane.Y, PenScreens.UltrawideM.MediaScreen.H - PenScreens.UltrawideM.MediaScreen.Pane.Y * 2))
}

/** The Landscape L media screen (Pen "MediaScreen" of "Cluster · Landscape L", Standard row): Large dock, gradient media pane. */
@Composable
fun LandscapeLMediaScreen(
    dock: KitDockState,
    media: KitMediaState,
    modifier: Modifier = Modifier,
    dockActions: KitDockActions = KitDockActions(),
    mediaActions: KitMediaActions = KitMediaActions(),
    tag: String = "Screen",
    showDock: Boolean = true,
    fitPane: Boolean = false,
) {
    MediaScreenLayout(dock, media, LandscapeLMediaSpec, PenScreens.LandscapeL.MediaScreen.Pane.X, PenScreens.LandscapeL.MediaScreen.Pane.Y, PenScreens.LandscapeL.MediaScreen.Pane.Radius, PenScreens.LandscapeL.MediaScreen.Fill, compactDock = false, modifier, dockActions, mediaActions, tag, PenKit.DockScale.Large, showDock = showDock, fitPane = fitPane, penPane = DpSize(PenScreens.LandscapeL.MediaScreen.W - PenScreens.LandscapeL.MediaScreen.Pane.X - PenScreens.LandscapeL.MediaScreen.Pane.Y, PenScreens.LandscapeL.MediaScreen.H - PenScreens.LandscapeL.MediaScreen.Pane.Y * 2))
}

/** The Ultrawide L media screen (Pen "MediaScreen" of the Ultrawide row). */
@Composable
fun UltrawideLMediaScreen(
    dock: KitDockState,
    media: KitMediaState,
    modifier: Modifier = Modifier,
    dockActions: KitDockActions = KitDockActions(),
    mediaActions: KitMediaActions = KitMediaActions(),
    tag: String = "Screen",
    showDock: Boolean = true,
    fitPane: Boolean = false,
) {
    MediaScreenLayout(dock, media, UltrawideLMediaSpec, PenScreens.UltrawideL.MediaScreen.Pane.X, PenScreens.UltrawideL.MediaScreen.Pane.Y, PenScreens.UltrawideL.MediaScreen.Pane.Radius, PenScreens.UltrawideL.MediaScreen.Fill, compactDock = false, modifier, dockActions, mediaActions, tag, PenKit.DockScale.Large, showDock = showDock, fitPane = fitPane, penPane = DpSize(PenScreens.UltrawideL.MediaScreen.W - PenScreens.UltrawideL.MediaScreen.Pane.X - PenScreens.UltrawideL.MediaScreen.Pane.Y, PenScreens.UltrawideL.MediaScreen.H - PenScreens.UltrawideL.MediaScreen.Pane.Y * 2))
}

/** The Landscape XL media screen (Pen "MediaScreen" of the Ultrawide XL row): XLarge dock, gradient media pane. */
@Composable
fun LandscapeXLMediaScreen(
    dock: KitDockState,
    media: KitMediaState,
    modifier: Modifier = Modifier,
    dockActions: KitDockActions = KitDockActions(),
    mediaActions: KitMediaActions = KitMediaActions(),
    tag: String = "Screen",
    showDock: Boolean = true,
    fitPane: Boolean = false,
) {
    MediaScreenLayout(dock, media, LandscapeXLMediaSpec, PenScreens.LandscapeXL.MediaScreen.Pane.X, PenScreens.LandscapeXL.MediaScreen.Pane.Y, PenScreens.LandscapeXL.MediaScreen.Pane.Radius, PenScreens.LandscapeXL.MediaScreen.Fill, compactDock = false, modifier, dockActions, mediaActions, tag, PenKit.DockScale.XLarge, showDock = showDock, fitPane = fitPane, penPane = DpSize(PenScreens.LandscapeXL.MediaScreen.W - PenScreens.LandscapeXL.MediaScreen.Pane.X - PenScreens.LandscapeXL.MediaScreen.Pane.Y, PenScreens.LandscapeXL.MediaScreen.H - PenScreens.LandscapeXL.MediaScreen.Pane.Y * 2))
}
