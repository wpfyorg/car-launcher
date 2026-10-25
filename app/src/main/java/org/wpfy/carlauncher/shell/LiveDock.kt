package org.wpfy.carlauncher.shell

import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import org.wpfy.carlauncher.design.adapters.kitDock
import org.wpfy.carlauncher.design.adapters.kitDockApps
import org.wpfy.carlauncher.design.adapters.ref
import org.wpfy.carlauncher.design.kit.KitDockActions
import org.wpfy.carlauncher.design.kit.KitDockLandscape
import org.wpfy.carlauncher.design.kit.KitDockLandscapeCompact
import org.wpfy.carlauncher.design.kit.KitDockPortrait
import org.wpfy.carlauncher.design.kit.PenIcons
import org.wpfy.carlauncher.design.kit.PenKit
import org.wpfy.carlauncher.design.kit.PenKit.DockLandscape as Landscape
import org.wpfy.carlauncher.design.kit.PenKit.DockPortrait as Portrait
import org.wpfy.carlauncher.design.theme.CarWindow
import org.wpfy.carlauncher.design.theme.OverlayTier
import org.wpfy.carlauncher.launcher.LauncherApp
import org.wpfy.carlauncher.launcher.rememberAppsStateHolder

/*
 * The live dock: the Pen kit dock of the window's screen group, wired to the shell. Side docks are the
 * landscape docks of the group's tier (compact on Small, the Large dock at the tier's scale
 * otherwise); the bottom dock is the portrait dock at the window's width.
 */

/** The landscape dock's scale for [tier] (the Small tier draws the compact dock instead). */
internal fun dockScaleFor(tier: OverlayTier): Float = when (tier) {
    OverlayTier.Small, OverlayTier.Medium -> PenKit.DockScale.Medium
    OverlayTier.Large -> PenKit.DockScale.Large
    OverlayTier.XLarge -> PenKit.DockScale.XLarge
}

/** How far the dock reaches into the window: its width on a side, its height at the bottom. */
internal fun dockThickness(side: RailSide, window: CarWindow): Dp = when {
    side == RailSide.Bottom -> PenKit.DockPortrait.H
    window.overlayTier == OverlayTier.Small -> PenKit.DockLandscapeCompact.W
    else -> PenKit.DockLandscape.W * dockScaleFor(window.overlayTier)
}

/** How many of [max] pinned apps fit in [availableDp] after [fixedDp] of other items, each [perAppDp] with its gap. */
internal fun dockAppCapacity(availableDp: Float, fixedDp: Float, perAppDp: Float, max: Int): Int =
    ((availableDp - fixedDp) / perAppDp).toInt().coerceIn(0, max)

/** Pinned apps that fit the side dock of [heightDp] at [scale] after the clock, bell, voice and all-apps items. */
internal fun sideDockCapacity(heightDp: Float, scale: Float, max: Int): Int {
    val gap = Landscape.PinnedApps.Gap.value
    val fixed = Landscape.PadTop.value + Landscape.Clock.H.value + Landscape.Notifications.W.value +
        Landscape.VoiceAssistant.W.value + Landscape.AllApps.W.value + Landscape.PadBottom.value - gap
    return dockAppCapacity(heightDp / scale, fixed, Landscape.PinnedApps.Calculator.W.value + gap, max)
}

/** Pinned apps that fit the bottom dock of [widthDp] after the all-apps, voice, bell and clock items. */
internal fun bottomDockCapacity(widthDp: Float, max: Int): Int {
    val gap = Portrait.Gap.value
    // Without apps the row is six children (the two spacers included), so five gaps; each app adds one.
    val fixed = Portrait.PadStart.value + Portrait.PadEnd.value + Portrait.AllApps.W.value + Portrait.Voice.W.value +
        Portrait.Notifications.W.value + Portrait.Clock.W.value + gap * 5
    return dockAppCapacity(widthDp, fixed, Portrait.Maps.W.value + gap, max)
}

@Composable
internal fun LiveDock(
    selected: ShellDestination,
    onDestinationSelected: (ShellDestination) -> Unit,
    side: RailSide,
    window: CarWindow,
    notificationCount: Int,
    onAssistantClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isDebuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    val appsStateHolder = rememberAppsStateHolder()
    val catalog = appsStateHolder.uiState.apps
    val appsByKey = catalog.associateBy(LauncherApp::stableKey)
    val tier = window.overlayTier
    val compact = side != RailSide.Bottom && tier == OverlayTier.Small
    val capacity = when {
        side == RailSide.Bottom -> bottomDockCapacity(window.widthDp, MaxDockApps)
        compact -> 0
        else -> sideDockCapacity(window.heightDp, dockScaleFor(tier), MaxDockApps)
    }
    val dockApps = kitDockApps(
        catalog = catalog.map(LauncherApp::ref),
        orderedPinnedKeys = appsStateHolder.uiState.orderedPinnedAppKeys,
        fallbackToCatalog = isDebuggable,
    ).take(capacity)

    val icons = remember { mutableStateMapOf<String, ImageBitmap>() }
    LaunchedEffect(dockApps) {
        dockApps.forEach { app ->
            if (app.key !in icons) {
                appsByKey[app.key]?.let { appsStateHolder.loadIcon(it) }?.let { icons[app.key] = it }
            }
        }
    }

    val dock = kitDock(
        clock = rememberClockText(),
        notificationCount = notificationCount,
        apps = dockApps,
        icon = { icons[it.key] },
        onOpen = { app -> appsByKey[app.key]?.let(appsStateHolder::launch) },
    )
    // The all-apps button is the way back to Home from every other screen, and so is the bell
    // while the notifications are open.
    val launcherDestination = appLauncherDestination(selected)
    val backHome = launcherDestination == ShellDestination.Home
    val actions = KitDockActions(
        onNotifications = {
            onDestinationSelected(
                if (selected == ShellDestination.Notifications) ShellDestination.Home else ShellDestination.Notifications,
            )
        },
        onVoice = onAssistantClick,
        onAllApps = { onDestinationSelected(launcherDestination) },
        allAppsIcon = if (backHome) PenIcons.Home else null,
        allAppsDescription = if (backHome) "Go home" else "Open app drawer",
        notificationsDescription = if (selected == ShellDestination.Notifications) "Go home" else null,
        onMoveApp = { from, to ->
            pinnedMove(
                shownKeys = dockApps.map { it.key },
                orderedPinnedKeys = appsStateHolder.uiState.orderedPinnedAppKeys,
                from = from,
                to = to,
            )?.let { (key, index) -> appsStateHolder.movePinned(key, index) }
        },
    )

    when {
        side == RailSide.Bottom ->
            KitDockPortrait(dock.clock, dock.notificationCount, dock.apps, window.width, modifier, actions)
        compact ->
            KitDockLandscapeCompact(dock.clock, dock.notificationCount, window.height, modifier, actions)
        else ->
            KitDockLandscape(dock.clock, dock.notificationCount, dock.apps, window.height, dockScaleFor(tier), modifier, actions)
    }
}

/** The pinned apps a dock may show; the same cap as before the kit dock. */
internal const val MaxDockApps = 3

/** The shell destination the all-apps button opens from [selected]: Apps from Home, Home from everywhere else. */
internal fun appLauncherDestination(selected: ShellDestination): ShellDestination {
    return if (selected == ShellDestination.Home) {
        ShellDestination.Apps
    } else {
        ShellDestination.Home
    }
}

/** Pinned app keys in pin order: only apps still installed, no repeats, at most [maxDockApps]. */
internal fun resolveDockAppKeys(
    catalogKeys: List<String>,
    orderedPinnedKeys: List<String>,
    maxDockApps: Int,
): List<String> {
    val catalogKeySet = catalogKeys.toSet()
    val seen = mutableSetOf<String>()
    return orderedPinnedKeys
        .asSequence()
        .filter { key -> key in catalogKeySet && seen.add(key) }
        .take(maxDockApps)
        .toList()
}

/**
 * The pin-order change for dragging the dock app at [from] onto the one at [to] ([shownKeys] are the
 * apps on the dock, a prefix of the pin order without uninstalled apps): the moved app's key and its
 * index in [orderedPinnedKeys], or null when nothing moves.
 */
internal fun pinnedMove(
    shownKeys: List<String>,
    orderedPinnedKeys: List<String>,
    from: Int,
    to: Int,
): Pair<String, Int>? {
    if (from == to) return null
    val key = shownKeys.getOrNull(from) ?: return null
    val targetKey = shownKeys.getOrNull(to) ?: return null
    val index = orderedPinnedKeys.indexOf(targetKey)
    if (index == -1 || key !in orderedPinnedKeys) return null
    return key to index
}
