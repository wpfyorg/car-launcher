package org.wpfy.carlauncher.launcher

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import org.wpfy.carlauncher.design.adapters.kitDrawerApps
import org.wpfy.carlauncher.design.adapters.ref
import org.wpfy.carlauncher.design.kit.PenIcons
import org.wpfy.carlauncher.design.kit.screenGroupFor
import org.wpfy.carlauncher.design.screens.AppDrawerApp
import org.wpfy.carlauncher.design.screens.AppDrawerMenu
import org.wpfy.carlauncher.design.screens.AppDrawerScreen
import org.wpfy.carlauncher.design.screens.ScreenState
import org.wpfy.carlauncher.design.screens.SystemScreenSpec
import org.wpfy.carlauncher.design.theme.LocalCarWindow

/*
 * The live app drawer: the Pen kit drawer of the window's screen group (the pane only: the shell
 * draws the dock), fed by the apps state. Search, the long-press menu (pin, app info), launch and the
 * settings entry are the live behaviour; loading and the empty states have no Pen frame.
 */

@Composable
fun AppsRoute(onOpenSettings: () -> Unit) {
    val stateHolder = rememberAppsStateHolder()
    AppDrawer(
        state = stateHolder.uiState,
        onQueryChange = stateHolder::updateQuery,
        onAppClick = stateHolder::launch,
        onTogglePinned = stateHolder::togglePinned,
        onOpenAppInfo = stateHolder::openAppInfo,
        onOpenSettings = onOpenSettings,
        iconLoader = stateHolder::loadIcon,
    )
}

/** Whether the Settings entry is listed for [query]: always, until the search no longer matches its name. */
internal fun showsSettingsEntry(query: String): Boolean = query.isBlank() || SettingsLabel.contains(query, ignoreCase = true)

@Composable
fun AppDrawer(
    state: AppsUiState,
    onQueryChange: (String) -> Unit,
    onAppClick: (LauncherApp) -> Unit,
    onTogglePinned: (LauncherApp) -> Unit,
    onOpenAppInfo: (LauncherApp) -> Unit,
    onOpenSettings: () -> Unit,
    iconLoader: suspend (LauncherApp) -> ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    val spec = SystemScreenSpec.forGroup(screenGroupFor(LocalCarWindow.current))
    var menuKey by remember { mutableStateOf<String?>(null) }
    val icons = remember { mutableStateMapOf<String, ImageBitmap>() }
    val visible = state.visibleApps
    val byKey = state.apps.associateBy(LauncherApp::stableKey)
    val showsSettings = showsSettingsEntry(state.query)

    val tiles = kitDrawerApps(
        apps = visible.map(LauncherApp::ref),
        pinnedKeys = state.orderedPinnedAppKeys.toSet(),
        icon = { icons[it.key] },
        onOpen = { ref -> byKey[ref.key]?.let(onAppClick) },
        onLongPress = { ref -> menuKey = ref.key },
        // The grid is lazy: a tile loads its icon when it is composed, so only rows near the window load.
        loadIcon = { ref ->
            suspend { if (ref.key !in icons) byKey[ref.key]?.let { app -> iconLoader(app)?.let { icons[ref.key] = it } } }
        },
        menu = { ref ->
            val app = byKey.getValue(ref.key)
            AppDrawerMenu(
                expanded = menuKey == ref.key,
                pinned = state.isPinned(app),
                onDismiss = { menuKey = null },
                onTogglePinned = { menuKey = null; onTogglePinned(app) },
                onAppInfo = { menuKey = null; onOpenAppInfo(app) },
            )
        },
    )
    val settings = AppDrawerApp("Settings", SettingsLabel, icon = null, pinned = false, onClick = onOpenSettings, glyph = PenIcons.Settings)

    val screenState = when {
        state.isLoading && state.apps.isEmpty() -> ScreenState("Loading apps", "", loading = true)
        tiles.isEmpty() && !showsSettings -> if (state.query.isBlank()) {
            ScreenState("No apps yet", "Apps you install will appear here.", PenIcons.Apps)
        } else {
            ScreenState("No matching apps", "Try a different app name.", PenIcons.SearchOff)
        }
        else -> null
    }

    AppDrawerScreen(
        spec = spec,
        title = "Apps",
        searchPlaceholder = "Search apps",
        apps = if (showsSettings) listOf(settings) + tiles else tiles,
        modifier = modifier.fillMaxSize().imePadding(),
        showDock = false,
        query = state.query,
        onQueryChange = onQueryChange,
        state = screenState,
        lazy = true,
    )
}

private const val SettingsLabel = "Settings"
