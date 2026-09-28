package org.wpfy.carlauncher.design.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.key
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.wpfy.carlauncher.design.theme.OverlayTier
import org.wpfy.carlauncher.design.theme.ProvideOverlayScaling
import org.wpfy.carlauncher.design.theme.TileScale
import org.wpfy.carlauncher.design.theme.overlayScalesFor
import org.wpfy.carlauncher.design.kit.*
import org.wpfy.carlauncher.design.kit.PenKit.AppTilePinned as Tile
import org.wpfy.carlauncher.design.kit.PenKit.NotificationCard as Card
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeM.AppDrawer.Pane as MPane
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeM.Notifications.Pane.List as MNotificationsList
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeM.Settings.Pane.List as MSettingsList
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeL.AppDrawer.Pane as LPane
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeL.Notifications.Pane.List as LNotificationsList
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeL.Settings.Pane.Body as LSettingsBody
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeL.Settings.Pane.Body.Categories as LCategories
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeXL.AppDrawer.Pane as XLPane
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeS.AppDrawer.Pane as SPane
import org.wpfy.carlauncher.design.kit.PenScreens.PortraitL.AppDrawer.Pane as PLPane
import org.wpfy.carlauncher.design.kit.PenScreens.PortraitL.Notifications.Pane.List as PLNotificationsList
import org.wpfy.carlauncher.design.kit.PenScreens.PortraitL.Settings.Pane.Body as PLSettingsBody
import org.wpfy.carlauncher.design.kit.PenScreens.PortraitL.Settings.Pane.Body.Categories as PLCategories
import org.wpfy.carlauncher.design.kit.PenScreens.PortraitS.AppDrawer.Pane as PSPane
import org.wpfy.carlauncher.design.kit.PenScreens.PortraitS.Notifications.Pane.List as PSNotificationsList
import org.wpfy.carlauncher.design.kit.PenScreens.PortraitS.Settings.Pane.List as PSSettingsList
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeS.Notifications.Pane.List as SNotificationsList
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeS.Settings.Pane.List as SSettingsList

/**
 * What a landscape group's system screens (app drawer, settings, notifications) take from the Pen:
 * the pane's place and look, its padding, and the gaps of its content. Dock: the Small tier's
 * compact dock, or the landscape dock at [dockScale].
 *
 * The pane's place and look are real dp. The content measures are in the units of [tier]'s tile scale
 * (the XL screens draw the L screen's content, scaled): the Pen's own numbers for an unscaled group.
 * [tileMaxWidth] is the widest app tile, [searchWidth] the widest search field and [columnMax] the
 * widest notifications column; a wider pane centres them. [categories] is set where settings are two
 * panes (the sections beside the chosen section's rows).
 *
 * A [portrait] group has the portrait dock along the bottom edge and the pane above it; there the tiles
 * are as many as fit the Pen's tile width ([tileMinWidth]), and the rows of settings and notifications
 * are as tall as their text, which wraps to [wrapLines] lines ([bodyLine]: a notification body's line height
 * in the Pen frames; the S frames draw at 2 canvas px per dp, so it is 16.5).
 */
@Immutable
class SystemScreenSpec internal constructor(
    val paneX: Dp, val paneY: Dp, val paneRadius: Dp, val paneFill: Color, val fill: Color,
    val pad: Dp, val contentGap: Dp, val headerGap: Dp, val gridGap: Dp, val columnGap: Dp, val settingsGap: Dp, val notificationGap: Dp,
    val gridRowHeight: Dp, val notificationHeight: Dp, val sectionHeight: Dp,
    val compactDock: Boolean,
    val tileMaxWidth: Dp, val searchWidth: Dp, val columnMax: Dp,
    val dockScale: Float = PenKit.DockScale.Medium,
    val tier: OverlayTier? = null,
    val categories: SettingsCategoryStyle? = null,
    val portrait: PortraitScreenSpec? = null,
    val tileMinWidth: Dp = Tile.W,
    val wrapLines: Int = 1,
    val bodyLine: Dp? = null,
) {
    companion object {
        val LandscapeS = SystemScreenSpec(
            SPane.X, SPane.Y, SPane.Radius, SPane.Fill, PenScreens.LandscapeS.AppDrawer.Fill, SPane.PadStart, SPane.Gap, SPane.Header.Gap, SPane.Grid.Gap,
            SPane.Grid.Row1.Gap, SSettingsList.Gap, SNotificationsList.Gap, SPane.Grid.Row1.H, SNotificationsList.Messages.H, SSettingsList.SectionLauncher.H, compactDock = true,
            tileMaxWidth = SPane.Grid.Row1.Maps.W, searchWidth = SPane.Header.Search.W, columnMax = SNotificationsList.W,
        )
        val LandscapeM = SystemScreenSpec(
            MPane.X, MPane.Y, MPane.Radius, MPane.Fill, PenScreens.LandscapeM.AppDrawer.Fill, MPane.PadStart, MPane.Gap, MPane.Header.Gap, MPane.Grid.Gap,
            MPane.Grid.Row1.Gap, MSettingsList.Gap, MNotificationsList.Gap, MPane.Grid.Row1.H, MNotificationsList.Messages.H, MSettingsList.SectionLauncher.H, compactDock = false,
            tileMaxWidth = MPane.Grid.Row1.Maps.W, searchWidth = MPane.Header.Search.W, columnMax = MNotificationsList.W,
        )
        val LandscapeL = SystemScreenSpec(
            LPane.X, LPane.Y, LPane.Radius, LPane.Fill, PenScreens.LandscapeL.AppDrawer.Fill, LPane.PadStart, LPane.Gap, LPane.Header.Gap, LPane.Grid.Gap,
            LPane.Grid.Row1.Gap, LSettingsBody.Details.Gap, LNotificationsList.Gap, LPane.Grid.Row1.H, LNotificationsList.Messages.H, LSettingsBody.Details.Section.H, compactDock = false,
            tileMaxWidth = LPane.Grid.Row1.Maps.W, searchWidth = LPane.Header.Search.W, columnMax = LNotificationsList.W,
            dockScale = PenKit.DockScale.Large, tier = OverlayTier.Large, categories = SettingsCategoryStyle.LandscapeL,
        )
        val LandscapeXL = SystemScreenSpec(
            XLPane.X, XLPane.Y, XLPane.Radius, XLPane.Fill, PenScreens.LandscapeXL.AppDrawer.Fill, LPane.PadStart, LPane.Gap, LPane.Header.Gap, LPane.Grid.Gap,
            LPane.Grid.Row1.Gap, LSettingsBody.Details.Gap, LNotificationsList.Gap, LPane.Grid.Row1.H, LNotificationsList.Messages.H, LSettingsBody.Details.Section.H, compactDock = false,
            tileMaxWidth = LPane.Grid.Row1.Maps.W, searchWidth = LPane.Header.Search.W, columnMax = LNotificationsList.W,
            dockScale = PenKit.DockScale.XLarge, tier = OverlayTier.XLarge, categories = SettingsCategoryStyle.LandscapeL,
        )
        val PortraitS = SystemScreenSpec(
            PSPane.X, PSPane.Y, PSPane.Radius, PSPane.Fill, PenScreens.PortraitS.AppDrawer.Fill, PSPane.PadStart, PSPane.Gap, PSPane.Header.Gap, PSPane.Grid.Gap,
            PSPane.Grid.Row1.Gap, PSSettingsList.Gap, PSNotificationsList.Gap, PSPane.Grid.Row1.H, PSNotificationsList.Messages.H, PSSettingsList.SectionLauncher.H, compactDock = false,
            tileMaxWidth = PSPane.Grid.Row1.Maps.W, searchWidth = PLPane.Header.Search.W, columnMax = PSNotificationsList.W,
            portrait = PortraitScreenSpec.PortraitS, tileMinWidth = PSPane.Grid.Row1.Maps.W, wrapLines = 2, bodyLine = PSNotificationsList.Calendar.Text.Body.H,
        )
        val PortraitL = SystemScreenSpec(
            PLPane.X, PLPane.Y, PLPane.Radius, PLPane.Fill, PenScreens.PortraitL.AppDrawer.Fill, PLPane.PadStart, PLPane.Gap, PLPane.Header.Gap, PLPane.Grid.Gap,
            PLPane.Grid.Row1.Gap, PLSettingsBody.Details.Gap, PLNotificationsList.Gap, PLPane.Grid.Row1.H, PLNotificationsList.Messages.H, PLSettingsBody.Details.SectionLauncher.H, compactDock = false,
            tileMaxWidth = PLPane.Grid.Row1.Maps.W, searchWidth = PLPane.Header.Search.W, columnMax = PLNotificationsList.W,
            categories = SettingsCategoryStyle.PortraitL,
            portrait = PortraitScreenSpec.PortraitL, tileMinWidth = PLPane.Grid.Row1.Maps.W, wrapLines = 2, bodyLine = PLNotificationsList.Messages.Text.Body.H,
        )

        /** The spec of the Pen frames of [group]; the Ultrawide groups have no system frames of their own and draw their tier's landscape ones. */
        fun forGroup(group: ScreenGroup): SystemScreenSpec = when (group) {
            ScreenGroup.LandscapeS -> LandscapeS
            ScreenGroup.LandscapeM, ScreenGroup.UltrawideM -> LandscapeM
            ScreenGroup.LandscapeL, ScreenGroup.UltrawideL -> LandscapeL
            ScreenGroup.LandscapeXL -> LandscapeXL
            ScreenGroup.PortraitS -> PortraitS
            ScreenGroup.PortraitL -> PortraitL
        }
    }
}

/** The look of the section list beside the settings rows (Pen "Body/Categories"): the chosen row is a filled pill. */
@Immutable
class SettingsCategoryStyle internal constructor(
    val width: Dp, val gap: Dp, val bodyGap: Dp, val height: Dp, val gapInside: Dp, val pad: Dp, val radius: Dp, val iconSize: Dp,
    val selectedFill: Color, val selectedIconFill: Color, val selectedLabel: TextStyle, val selectedLabelFill: Color,
    val iconFill: Color, val label: TextStyle, val labelFill: Color,
) {
    companion object {
        val LandscapeL = SettingsCategoryStyle(
            LCategories.W, LCategories.Gap, LSettingsBody.Gap, LCategories.Launcher.H, LCategories.Launcher.Gap, LCategories.Launcher.PadStart, LCategories.Launcher.Radius, LCategories.Launcher.Icon.W,
            LCategories.Launcher.Fill, LCategories.Launcher.Icon.Fill, LCategories.Launcher.Label.Style, LCategories.Launcher.Label.Fill,
            LCategories.Appearance.Icon.Fill, LCategories.Appearance.Label.Style, LCategories.Appearance.Label.Fill,
        )
        val PortraitL = SettingsCategoryStyle(
            PLCategories.W, PLCategories.Gap, PLSettingsBody.Gap, PLCategories.Launcher.H, PLCategories.Launcher.Gap, PLCategories.Launcher.PadStart, PLCategories.Launcher.Radius, PLCategories.Launcher.Icon.W,
            PLCategories.Launcher.Fill, PLCategories.Launcher.Icon.Fill, PLCategories.Launcher.Label.Style, PLCategories.Launcher.Label.Fill,
            PLCategories.Appearance.Icon.Fill, PLCategories.Appearance.Label.Style, PLCategories.Appearance.Label.Fill,
        )
    }
}

/** One section of a two-pane settings screen; [node] is its Pen name. */
@Immutable
class SettingsCategory(val node: String, val label: String, val icon: ImageVector, val selected: Boolean, val onClick: () -> Unit = {})

/** The menu of a tile, open or not: [pinned] decides whether it offers Pin or Unpin. */
@Immutable
class AppDrawerMenu(
    val expanded: Boolean, val pinned: Boolean,
    val onDismiss: () -> Unit, val onTogglePinned: () -> Unit, val onAppInfo: () -> Unit,
)

/** A tile of the drawer; a [glyph] is drawn on the neutral tile where there is no [icon], and a [menu] is drawn by the tile. */
@Immutable
class AppDrawerApp(
    val node: String, val label: String, val icon: ImageBitmap?, val pinned: Boolean,
    val onClick: () -> Unit = {}, val onLongClick: (() -> Unit)? = null,
    val glyph: ImageVector? = null, val menu: AppDrawerMenu? = null,
    /** Run once while the tile is composed: a lazy grid loads only the icons of rows near the window. */
    val loadIcon: (suspend () -> Unit)? = null,
)

/** What a screen shows instead of its list (see [KitStateMessage]); the Pen has no frame for it. */
@Immutable
class ScreenState(
    val title: String, val message: String, val icon: ImageVector? = null, val loading: Boolean = false,
    val actionLabel: String? = null, val onAction: () -> Unit = {},
)

@Immutable
sealed interface SettingsEntry {
    class Section(val node: String, val title: String) : SettingsEntry
    /** A row that opens a choice; a null [onClick] is a read-only value row (no chevron). */
    class Link(val node: String, val title: String, val value: String, val onClick: (() -> Unit)? = {}) : SettingsEntry
    class Switch(val node: String, val title: String, val body: String, val checked: Boolean, val onCheckedChange: (Boolean) -> Unit = {}) : SettingsEntry
}

@Immutable
class NotificationItem(
    val node: String, val meta: String, val title: String, val body: String, val appIcon: ImageBitmap?,
    val onClick: (() -> Unit)? = {}, val onDismiss: (() -> Unit)? = {},
    /** The dismiss button's spoken name; null is the card's own ("Dismiss"). */
    val dismissDescription: String? = null,
)

/**
 * The dock and the pane of a system screen; [content] fills the pane inside its padding, in the units of the
 * spec's tier (the pane's width in those units is its argument). Tags: "Screen/Dock", "Screen/Pane". Without
 * [showDock] only the pane is drawn, as large as [modifier] makes it.
 */
@Composable
internal fun SystemFrame(
    spec: SystemScreenSpec,
    dock: KitDockState,
    dockActions: KitDockActions,
    tag: String,
    modifier: Modifier,
    showDock: Boolean = true,
    content: @Composable (paneWidth: Dp) -> Unit,
) {
    if (!showDock) {
        // Only the pane, filling what the caller gives it (the live shell draws the dock and leaves the margins).
        BoxWithConstraints(modifier.fillMaxSize().testTag("$tag/Pane").clip(penShape(spec.paneRadius)).background(spec.paneFill)) {
            val scales = spec.tier?.let { overlayScalesFor(it) }
            if (scales == null) content(maxWidth)
            else ProvideOverlayScaling(scales) { TileScale { content(maxWidth / scales.tile) } }
        }
        return
    }
    if (spec.portrait != null) {
        // The portrait dock along the bottom edge, the pane in the area above it.
        PortraitFrame(spec.portrait, dock, dockActions, tag, modifier) { areaWidth, areaHeight ->
            Box(Modifier.size(areaWidth, areaHeight).testTag("$tag/Pane").clip(penShape(spec.paneRadius)).background(spec.paneFill)) { content(areaWidth) }
        }
        return
    }
    BoxWithConstraints(modifier.fillMaxSize().background(spec.fill)) {
        val height = maxHeight
        val width = maxWidth
        Box(Modifier.testTag("$tag/Dock")) {
            if (spec.compactDock) KitDockLandscapeCompact(dock.clock, dock.notificationCount, height, actions = dockActions)
            else KitDockLandscape(dock.clock, dock.notificationCount, dock.apps, height, spec.dockScale, actions = dockActions)
        }
        val paneWidth = width - spec.paneX - spec.paneY
        Box(
            Modifier.offset(spec.paneX, spec.paneY).size(paneWidth, height - spec.paneY * 2).testTag("$tag/Pane")
                .clip(penShape(spec.paneRadius)).background(spec.paneFill),
        ) {
            val scales = spec.tier?.let { overlayScalesFor(it) }
            if (scales == null) content(paneWidth)
            else ProvideOverlayScaling(scales) { TileScale { content(paneWidth / scales.tile) } }
        }
    }
}

/** The pane's column: the header, a gap, then [body] (scrolling unless [scrolls] is off, then it ends at the pane's inner edge); padded by the pane's padding. */
@Composable
internal fun PaneColumn(
    spec: SystemScreenSpec,
    header: @Composable () -> Unit,
    scrolls: Boolean = true,
    columnMax: Dp = Dp.Infinity,
    /** The body is a lazy list that scrolls itself and pads its own end, as the scrolling column does. */
    lazyBody: Boolean = false,
    body: @Composable () -> Unit,
) {
    // The list runs under the pane's bottom inset and is cut by the pane's edge, as in the Pen; the
    // inset is the scroll content's own end padding. A column wider than [columnMax] is centred.
    Box(Modifier.fillMaxSize().padding(start = spec.pad, top = spec.pad, end = spec.pad), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxHeight().widthIn(max = columnMax).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spec.contentGap)) {
            header()
            if (scrolls) Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = spec.pad)) { body() }
            else Box(Modifier.weight(1f).fillMaxWidth().then(if (lazyBody) Modifier else Modifier.padding(bottom = spec.pad))) { body() }
        }
    }
}

private const val MaxColumns = 5

/** A [ScreenState] centred in the pane's body. */
@Composable
internal fun StateBody(state: ScreenState, tag: String) {
    KitStateMessage(state.title, state.message, tag = tag, icon = state.icon, loading = state.loading, actionLabel = state.actionLabel, onAction = state.onAction)
}

/**
 * The app drawer (Pen "AppDrawer"): header with the title and a search field half the pane wide, then
 * the grid of app tiles in as many columns as fit (tile width plus the column gap), at most five,
 * the tiles sharing the width up to the Pen's widest tile, the grid centred. Tags: "Screen/Pane/Header", "Screen/Pane/Grid/Row<n>/<node>".
 * A set [onQueryChange] makes the search field a text field of [query]; a [state] (loading, no apps) replaces the grid.
 * With [lazy] the grid is a lazy list, scrolling itself: the rows are the same, but only those near the window exist.
 */
@Composable
fun AppDrawerScreen(
    spec: SystemScreenSpec,
    dock: KitDockState = KitDockState("", 0),
    title: String,
    searchPlaceholder: String,
    apps: List<AppDrawerApp>,
    modifier: Modifier = Modifier,
    dockActions: KitDockActions = KitDockActions(),
    onSearch: () -> Unit = {},
    tag: String = "Screen",
    showDock: Boolean = true,
    query: String = "",
    onQueryChange: ((String) -> Unit)? = null,
    state: ScreenState? = null,
    lazy: Boolean = false,
) {
    SystemFrame(spec, dock, dockActions, tag, modifier, showDock) { paneWidth ->
        val p = "$tag/Pane"
        PaneColumn(
            spec,
            header = {
                KitScreenHeader(title, tag = "$p/Header") {
                    KitSearchField(
                        searchPlaceholder, minOf(paneWidth / 2, spec.searchWidth), tag = "$p/Header/Search",
                        onClick = onSearch, query = query, onQueryChange = onQueryChange,
                    )
                }
            },
            scrolls = state == null && !lazy,
            lazyBody = state == null && lazy,
            body = {
                if (state != null) {
                    StateBody(state, "$p/State")
                    return@PaneColumn
                }
                val inner = paneWidth - spec.pad * 2
                val columns = ((inner + spec.columnGap) / (spec.tileMinWidth + spec.columnGap) + COLUMN_EPSILON).toInt().coerceIn(1, MaxColumns)
                val tileWidth = minOf((inner - spec.columnGap * (columns - 1)) / columns, spec.tileMaxWidth)
                val rows = apps.chunked(columns)
                val gridWidth = tileWidth * columns + spec.columnGap * (columns - 1)
                val rowContent: @Composable (Int, List<AppDrawerApp>) -> Unit = { r, row ->
                    Row(Modifier.fillMaxWidth().testTag("$p/Grid/Row${r + 1}"), horizontalArrangement = Arrangement.spacedBy(spec.columnGap)) {
                        row.forEach { app ->
                            val t = "$p/Grid/Row${r + 1}/${app.node}"
                            app.loadIcon?.let { load -> LaunchedEffect(app.node) { load() } }
                            val tile: @Composable () -> Unit = {
                                KitAppTile(app.label, app.icon, app.pinned, tileWidth, tag = t, onClick = app.onClick, onLongClick = app.onLongClick, glyph = app.glyph)
                            }
                            val menu = app.menu
                            if (menu == null) tile()
                            else Box {
                                tile()
                                KitAppMenu(menu.expanded, menu.pinned, menu.onDismiss, menu.onTogglePinned, menu.onAppInfo, tag = "$t/Menu")
                            }
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().then(if (lazy) Modifier.fillMaxHeight() else Modifier), contentAlignment = Alignment.TopCenter) {
                    if (lazy) {
                        // The same rows at the same places, only those near the window composed.
                        LazyColumn(
                            Modifier.width(gridWidth).fillMaxHeight().testTag("$p/Grid"),
                            contentPadding = PaddingValues(bottom = spec.pad),
                            verticalArrangement = Arrangement.spacedBy(spec.gridGap),
                        ) {
                            itemsIndexed(rows) { r, row -> Box(Modifier.height(spec.gridRowHeight)) { rowContent(r, row) } }
                        }
                    } else {
                        PenStack(rows.map { spec.gridRowHeight }, spec.gridGap, Modifier.width(gridWidth).testTag("$p/Grid")) {
                            rows.forEachIndexed { r, row -> rowContent(r, row) }
                        }
                    }
                }
            },
        )
    }
}

private const val COLUMN_EPSILON = 0.001f

/**
 * The settings list (Pen "Settings"): the title (no trailing part, so the header is as tall as its
 * title), then sections, link rows and switch rows one gap apart. Where the spec has [categories]
 * the screen is two panes: the sections of [categoryList] beside the rows of the chosen one.
 * Tags: "Screen/Pane/Header", "Screen/Pane/List/Section", "Screen/Pane/List/<node>"; two panes:
 * "Screen/Pane/Body/Categories/<node>" and "Screen/Pane/Body/Details/<node>".
 * Without [showDock] only the pane is drawn; with [lazy] the rows are a lazy list that scrolls itself:
 * the same rows at the same places (each as tall as its text), only those near the window composed.
 */
@Composable
fun SettingsListScreen(
    spec: SystemScreenSpec,
    dock: KitDockState = KitDockState("", 0),
    title: String,
    entries: List<SettingsEntry>,
    modifier: Modifier = Modifier,
    dockActions: KitDockActions = KitDockActions(),
    tag: String = "Screen",
    categoryList: List<SettingsCategory> = emptyList(),
    showDock: Boolean = true,
    lazy: Boolean = false,
) {
    SystemFrame(spec, dock, dockActions, tag, modifier, showDock) {
        val p = "$tag/Pane"
        val style = spec.categories
        // Rows are the kit's fixed height, or as tall as their wrapped text (portrait); a section is the group's section line.
        val lineCount = rememberLineCounter()
        val subStyle = penStyle(PenKit.SettingsRow.Text.Sub.Style)
        val row: @Composable (SettingsEntry, String) -> Unit = { e, list ->
            when (e) {
                is SettingsEntry.Section -> KitSettingsSection(e.title, tag = "$list/${e.node}")
                is SettingsEntry.Link -> KitSettingsRow(e.title, e.value, tag = "$list/${e.node}", valueLines = spec.wrapLines, onClick = e.onClick)
                is SettingsEntry.Switch -> KitSettingsSwitchRow(e.title, e.body, e.checked, tag = "$list/${e.node}", bodyLines = spec.wrapLines, onCheckedChange = e.onCheckedChange)
            }
        }
        val rows: @Composable (String) -> Unit = { list ->
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val textWidth = { trailing: Dp -> maxWidth - PenKit.SettingsRow.PadStart - PenKit.SettingsRow.PadEnd - PenKit.SettingsRow.Gap - trailing }
                val heights = entries.map { e ->
                    when {
                        e is SettingsEntry.Section -> spec.sectionHeight
                        spec.wrapLines == 1 -> PenKit.SettingsRow.H
                        e is SettingsEntry.Link -> settingsRowHeight(lineCount(e.value, subStyle, textWidth(if (e.onClick != null) PenKit.SettingsRow.Icon.W else 0.dp), spec.wrapLines))
                        e is SettingsEntry.Switch -> settingsRowHeight(lineCount(e.body, subStyle, textWidth(PenKit.SettingsSwitchRowOn.Switch.W), spec.wrapLines))
                        else -> PenKit.SettingsRow.H
                    }
                }
                PenStack(heights, spec.settingsGap, Modifier.fillMaxWidth().testTag(list)) {
                    entries.forEach { e -> row(e, list) }
                }
            }
        }
        // The same rows in a lazy list; the list ends under the pane's inset, as the scrolling column does.
        val lazyRows: @Composable (String) -> Unit = { list ->
            val listState = rememberLazyListState()
            LazyColumn(
                Modifier.fillMaxSize().testTag(list), state = listState,
                contentPadding = PaddingValues(bottom = spec.pad),
                verticalArrangement = Arrangement.spacedBy(spec.settingsGap),
            ) {
                items(entries, key = { e -> when (e) { is SettingsEntry.Section -> "S-${e.node}"; is SettingsEntry.Link -> "L-${e.node}"; is SettingsEntry.Switch -> "W-${e.node}" } }) { e -> row(e, list) }
            }
        }
        PaneColumn(
            spec,
            header = { KitScreenHeader(title, Modifier.height(PenKit.ScreenHeaderSearch.Title.H), tag = "$p/Header") },
            scrolls = style == null && !lazy,
            lazyBody = lazy,
            body = {
                if (style == null) {
                    if (lazy) lazyRows("$p/List") else rows("$p/List")
                } else {
                    Row(Modifier.fillMaxSize().testTag("$p/Body"), horizontalArrangement = Arrangement.spacedBy(style.bodyGap)) {
                        PenStack(categoryList.map { style.height }, style.gap, Modifier.width(style.width).testTag("$p/Body/Categories")) {
                            categoryList.forEach { SettingsCategoryRow(style, it, "$p/Body/Categories/${it.node}") }
                        }
                        if (lazy) {
                            // Another section starts at the top of its rows.
                            key(categoryList.firstOrNull { it.selected }?.node) { Box(Modifier.weight(1f).fillMaxHeight()) { lazyRows("$p/Body/Details") } }
                        } else {
                            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = spec.pad)) { rows("$p/Body/Details") }
                        }
                    }
                }
            },
        )
    }
}

/** A section of the two-pane settings: icon and label in a row; the chosen one is a filled pill with bold accent text. */
@Composable
private fun SettingsCategoryRow(style: SettingsCategoryStyle, c: SettingsCategory, tag: String) {
    val shape = penShape(style.radius)
    Row(
        Modifier.testTag(tag).fillMaxWidth().height(style.height).clip(shape).background(if (c.selected) style.selectedFill else Color.Transparent, shape)
            .penClick(c.onClick).padding(horizontal = style.pad),
        horizontalArrangement = Arrangement.spacedBy(style.gapInside),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(c.icon, null, Modifier.testTag("$tag/Icon").size(style.iconSize), tint = if (c.selected) style.selectedIconFill else style.iconFill)
        PenText(c.label, if (c.selected) style.selectedLabel else style.label, if (c.selected) style.selectedLabelFill else style.labelFill, Modifier.testTag("$tag/Label"))
    }
}

/**
 * The notifications list (Pen "Notifications"): the title and the Clear all pill at the end, the cards
 * one gap apart, then the hint centred under them, all in a column up to the Pen's width, centred. Tags: "Screen/Pane/Header/ClearAll",
 * "Screen/Pane/List/<node>", "Screen/Pane/Hint". Without [showClearAll] the header is the title alone; a [state]
 * (access needed, nothing to show) replaces the list and the hint.
 */
@Composable
fun NotificationsListScreen(
    spec: SystemScreenSpec,
    dock: KitDockState = KitDockState("", 0),
    title: String,
    clearAll: String,
    items: List<NotificationItem>,
    hint: String,
    modifier: Modifier = Modifier,
    dockActions: KitDockActions = KitDockActions(),
    onClearAll: () -> Unit = {},
    tag: String = "Screen",
    showDock: Boolean = true,
    showClearAll: Boolean = true,
    state: ScreenState? = null,
    /** Clear all as its glyph alone (a header too narrow for the title and the labelled pill). */
    compactClearAll: Boolean = false,
    /** The list as a lazy list, scrolling itself: the cards are the same, but only those near the window exist. */
    lazy: Boolean = false,
) {
    SystemFrame(spec, dock, dockActions, tag, modifier, showDock) { paneWidth ->
        val p = "$tag/Pane"
        PaneColumn(
            spec,
            header = {
                KitScreenHeader(title, tag = "$p/Header") {
                    if (showClearAll) KitClearAllButton(clearAll, tag = "$p/Header/ClearAll", onClick = onClearAll, showLabel = !compactClearAll)
                }
            },
            scrolls = state == null && !lazy,
            lazyBody = state == null && lazy,
            columnMax = spec.columnMax,
            body = {
                if (state != null) {
                    StateBody(state, "$p/State")
                    return@PaneColumn
                }
                val lineCount = rememberLineCounter()
                val bodyLine = spec.bodyLine
                val bodyStyle = if (bodyLine != null) Card.Text.Body.Style.copy(lineHeight = bodyLine.value.sp) else Card.Text.Body.Style
                val measured = penStyle(bodyStyle)
                // A wrapping body takes what the card leaves beside the icon and the dismiss button.
                val bodyWidth = paneWidth - spec.pad * 2 - Card.PadStart - Card.PadEnd - Card.AppIcon.W - Card.Gap * 2
                val height: (NotificationItem) -> Dp = {
                    if (bodyLine == null || spec.wrapLines == 1) spec.notificationHeight
                    else notificationHeight(lineCount(it.body, measured, if (it.onDismiss != null) bodyWidth - Card.Dismiss.W - Card.Gap else bodyWidth, spec.wrapLines), bodyLine)
                }
                val card: @Composable (NotificationItem, Modifier) -> Unit = { n, m ->
                    KitNotificationCard(
                        n.meta, n.title, n.body, n.appIcon, m, tag = "$p/List/${n.node}", bodyLines = spec.wrapLines, bodyStyle = bodyStyle,
                        onClick = n.onClick, onDismiss = n.onDismiss, dismissDescription = n.dismissDescription ?: "Dismiss",
                    )
                }
                // The Pen's hint is one line; in a portrait pane it runs wider than the padded list and stays centred.
                val hintWidth = if (spec.portrait != null) Modifier.wrapContentWidth(Alignment.CenterHorizontally, unbounded = true) else Modifier
                val hintRow: @Composable () -> Unit = {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Box(hintWidth) { KitNotificationsHint(hint, tag = "$p/Hint") } }
                }
                if (lazy) {
                    // The same cards one gap apart, only those near the window composed; the hint follows the last.
                    LazyColumn(
                        Modifier.fillMaxSize().testTag("$p/List"),
                        contentPadding = PaddingValues(bottom = spec.pad),
                        verticalArrangement = Arrangement.spacedBy(spec.notificationGap),
                    ) {
                        items(items, key = NotificationItem::node) { n -> card(n, Modifier.height(height(n))) }
                        item(key = "Hint") { Column { Spacer(Modifier.height((spec.contentGap - spec.notificationGap).coerceAtLeast(0.dp))); hintRow() } }
                    }
                    return@PaneColumn
                }
                PenStack(items.map(height), spec.notificationGap, Modifier.fillMaxWidth().testTag("$p/List")) {
                    items.forEach { n -> card(n, Modifier) }
                }
                Spacer(Modifier.height(spec.contentGap))
                hintRow()
            },
        )
    }
}

/** A settings row as tall as its text: its vertical padding, the title, the gap and [subLines] lines of the explanation. */
private fun settingsRowHeight(subLines: Int): Dp =
    PenKit.SettingsRow.PadTop + PenKit.SettingsRow.PadBottom + PenKit.SettingsRow.Text.Title.H + PenKit.SettingsRow.Text.Gap + PenKit.SettingsRow.Text.Sub.H * subLines

/** A notification card as tall as its text: its vertical padding, meta, title and [bodyLines] lines of [bodyLine]. */
private fun notificationHeight(bodyLines: Int, bodyLine: Dp): Dp =
    Card.PadTop + Card.PadBottom + Card.Text.Meta.H + Card.Text.Gap + Card.Text.Title.H + Card.Text.Gap + bodyLine * bodyLines

/** How many lines (at most the given maximum) a text takes in a resolved style within a width: what makes a row or card taller. */
@Composable
internal fun rememberLineCounter(): (String, TextStyle, Dp, Int) -> Int {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return { text, style, width, maxLines ->
        measurer.measure(text, style, constraints = Constraints(maxWidth = with(density) { width.roundToPx() }), maxLines = maxLines).lineCount
    }
}

/**
 * A column whose items have the Pen's heights ([heights], null = as tall as the item's content) and
 * [gap] between them. Positions are summed in dp and rounded once to device pixels, so a long list of
 * fixed-height rows does not drift from its design by the rounding of each row.
 */
@Composable
internal fun PenStack(heights: List<Dp?>, gap: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val width = constraints.maxWidth
        var y = 0f
        val tops = IntArray(measurables.size)
        val placeables = measurables.mapIndexed { i, m ->
            val top = y.roundToInt()
            tops[i] = top
            val height = heights.getOrNull(i)
            if (height != null) {
                val bottom = (y + height.toPx()).roundToInt()
                y += height.toPx() + gap.toPx()
                m.measure(Constraints(maxWidth = width, minHeight = bottom - top, maxHeight = bottom - top))
            } else {
                m.measure(Constraints(maxWidth = width)).also { y += it.height + gap.toPx() }
            }
        }
        val total = if (placeables.isEmpty()) 0 else (y - gap.toPx()).roundToInt()
        layout(width, total) { placeables.forEachIndexed { i, p -> p.place(0, tops[i]) } }
    }
}
