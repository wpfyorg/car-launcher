package org.wpfy.carlauncher.design.adapters

import androidx.compose.ui.graphics.ImageBitmap
import org.wpfy.carlauncher.design.kit.KitDockApp
import org.wpfy.carlauncher.design.screens.AppDrawerApp
import org.wpfy.carlauncher.design.screens.AppDrawerMenu
import org.wpfy.carlauncher.design.screens.KitDockState
import org.wpfy.carlauncher.launcher.LauncherApp
import org.wpfy.carlauncher.shell.MaxDockApps
import org.wpfy.carlauncher.shell.resolveDockAppKeys

/*
 * Live apps -> the kit's dock and app drawer. Apps come in as [AppRef] (key and label) so the
 * adapters never touch a ComponentName; icons are looked up by the caller, null while loading.
 * Pen node names are positional ("App1", "App2", ...), as in the reference frames.
 */

/** What the adapters need of an installed app; [packageName] is what a picker lists under the label. */
data class AppRef(val key: String, val label: String, val packageName: String = "")

/** Returns the stable key and display label used by the dock and drawer adapters. */
fun LauncherApp.ref(): AppRef = AppRef(stableKey, label, packageName)

/**
 * The pinned apps the dock shows, in pin order: only apps still installed, no repeats, at most
 * [MaxDockApps]. [fallbackToCatalog] is the debuggable-build fallback to the first catalog apps
 * when nothing is pinned.
 */
fun kitDockApps(
    catalog: List<AppRef>,
    orderedPinnedKeys: List<String>,
    fallbackToCatalog: Boolean = false,
): List<AppRef> {
    val byKey = catalog.associateBy(AppRef::key)
    val pinned = resolveDockAppKeys(
        catalogKeys = catalog.map(AppRef::key),
        orderedPinnedKeys = orderedPinnedKeys,
        maxDockApps = MaxDockApps,
    ).mapNotNull(byKey::get)
    return if (pinned.isNotEmpty() || !fallbackToCatalog) pinned else catalog.take(MaxDockApps)
}

/** The dock: the clock text, the notification badge count and the pinned apps. */
fun kitDock(
    clock: String,
    notificationCount: Int,
    apps: List<AppRef> = emptyList(),
    icon: (AppRef) -> ImageBitmap? = { null },
    onOpen: (AppRef) -> Unit = {},
): KitDockState = KitDockState(
    clock = clock,
    notificationCount = notificationCount,
    apps = apps.mapIndexed { index, app ->
        KitDockApp("App${index + 1}", app.label, icon(app)) { onOpen(app) }
    },
)

/** The drawer's tiles, in the order given; pinned ones carry the pin mark, a tile has the [menu] given for its app and runs [loadIcon] while it is composed. */
fun kitDrawerApps(
    apps: List<AppRef>,
    pinnedKeys: Set<String>,
    icon: (AppRef) -> ImageBitmap? = { null },
    onOpen: (AppRef) -> Unit = {},
    onLongPress: ((AppRef) -> Unit)? = null,
    menu: (AppRef) -> AppDrawerMenu? = { null },
    loadIcon: ((AppRef) -> suspend () -> Unit)? = null,
): List<AppDrawerApp> = apps.mapIndexed { index, app ->
    AppDrawerApp(
        node = "App${index + 1}",
        label = app.label,
        icon = icon(app),
        pinned = app.key in pinnedKeys,
        onClick = { onOpen(app) },
        onLongClick = onLongPress?.let { handler -> { handler(app) } },
        menu = menu(app),
        loadIcon = loadIcon?.invoke(app),
    )
}
