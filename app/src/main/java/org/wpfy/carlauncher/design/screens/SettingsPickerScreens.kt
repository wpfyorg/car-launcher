package org.wpfy.carlauncher.design.screens

import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.wpfy.carlauncher.design.kit.KitClearAllButton
import org.wpfy.carlauncher.design.kit.KitDockActions
import org.wpfy.carlauncher.design.kit.KitNotificationsHint
import org.wpfy.carlauncher.design.kit.KitScreenHeader
import org.wpfy.carlauncher.design.kit.KitSettingsSection
import org.wpfy.carlauncher.design.kit.PenDisc
import org.wpfy.carlauncher.design.kit.PenIcons
import org.wpfy.carlauncher.design.kit.PenKit
import org.wpfy.carlauncher.design.kit.PenScreens
import org.wpfy.carlauncher.design.kit.PenText
import org.wpfy.carlauncher.design.kit.penClick
import org.wpfy.carlauncher.design.kit.penShape
import org.wpfy.carlauncher.design.kit.penStyle
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeL.SettingsAppChoice.Pane.List.Automatic.AppIcon as AppIconTile
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeL.SettingsChoice.Pane as ChoicePane
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeL.SettingsChoice.Pane.Header.Back as BackButton
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeL.SettingsChoice.Pane.List.Small as OptionRow
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeL.SettingsChoice.Pane.List.Standard as ChosenRow
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeL.SettingsOfflineMaps.Pane.Header.Import as ImportButton
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeL.SettingsOfflineMaps.Pane.List.California as MapRow
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeL.SettingsOfflineMaps.Pane.List.Nevada.Text.Progress as ProgressTrack
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeL.SettingsOfflineMaps.Pane.List.Nevada.Text.Progress.Done as ProgressDone
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeL.SettingsOfflineMaps.Pane.List.Oregon as AvailableRow

/*
 * The settings pickers (Pen "SettingsChoice", "SettingsAppChoice" and "SettingsOfflineMaps"): the pane
 * of a settings screen shows one choice in place of the settings list: a header with a back button
 * and the title, then the options. The three frames are the same in every group; the measures are the
 * Landscape L frames' (the XL screens draw them scaled, as the other system screens do). Rows are as
 * tall as their text and parts (the Pen's rows are exactly that): a subtitle may wrap to [SubLines].
 */

/** One option of a choice: [node] is its Pen name; [selected] is the current one; a null [sub] leaves the description out. */
@Immutable
class ChoiceOption(val node: String, val title: String, val sub: String?, val selected: Boolean, val onClick: () -> Unit = {})

/**
 * One option of an app choice: the app's [icon] (a [glyph] on the neutral tile where it is not
 * loaded or there is no app), [sub] under the [title]. [loadIcon] runs once while the row is composed.
 */
@Immutable
class AppChoiceOption(
    val node: String, val title: String, val sub: String, val selected: Boolean, val icon: ImageBitmap?,
    val glyph: ImageVector? = null, val onClick: () -> Unit = {}, val loadIcon: (suspend () -> Unit)? = null,
    /** The option's identity across list changes (the app's key); [node] stays its Pen name and test tag. */
    val key: String = node,
)

/** What a map region's round button does; the button is accented where it asks for a download. */
enum class OfflineMapAction(val accent: Boolean) {
    Download(true), Cancel(false), Delete(false);

    val icon: ImageVector
        get() = when (this) {
            Download -> PenIcons.Download
            Cancel -> PenIcons.Close
            Delete -> PenIcons.Delete
        }
}

@Immutable
sealed interface OfflineMapEntry {
    class Section(val node: String, val title: String) : OfflineMapEntry

    /**
     * A region: [progress] (0 to 1) draws the bar under the [sub] while it downloads. The button is
     * [action], named [actionDescription] to accessibility services.
     */
    class Region(
        val node: String, val title: String, val sub: String, val progress: Float?,
        val action: OfflineMapAction, val actionDescription: String, val onAction: () -> Unit = {},
    ) : OfflineMapEntry

    /** A line of news in the row's look (an import that failed), without a button. */
    class Notice(val node: String, val title: String, val message: String) : OfflineMapEntry
}

/** A subtitle may take this many lines before it is cut; the Pen's rows hold one (a portrait pane wraps the longer ones). */
private const val SubLines = 2

/**
 * The least header width a labelled import button fits in: the back button, the title and the button, with their gaps
 * (the Pen's Portrait S header is narrower than that, its button runs out of the pane; there the glyph stands alone).
 */
private val ImportLabelledMinWidth = BackButton.W + ChoicePane.Header.Gap +
    PenScreens.LandscapeL.SettingsOfflineMaps.Pane.Header.Title.W + ChoicePane.Header.Gap + ImportButton.W

/**
 * What decides a row's height. A row is as tall as its parts and text but at least [min] (the Pen's row); the Pen's
 * rows hold one line of [sub], so they are exactly [min]. [reserved] is the width beside the text (the icon before it,
 * the mark or button after it, and their gaps); a row with a [progress] bar has the bar under the subtitle.
 * A [fixed] height is the row's own (a section's title line).
 */
private class RowFacts(val min: Dp, val sub: String? = null, val reserved: Dp = 0.dp, val progress: Boolean = false, val fixed: Dp? = null)

// --- Frame -------------------------------------------------------------------------------------

@Composable
private fun PickerBackButton(description: String, onBack: () -> Unit, p: String) {
    PenDisc(
        BackButton.W, BackButton.Fill, PenIcons.ArrowBack, BackButton.Icon.W, BackButton.Icon.Fill,
        Modifier.testTag("$p/Header/Back"), radius = BackButton.Radius,
        // The Pen's arrow points left; right-to-left layouts mirror it, as the start edge it sits at.
        iconModifier = Modifier.testTag("$p/Header/Back/Icon")
            .then(if (LocalLayoutDirection.current == LayoutDirection.Rtl) Modifier.scale(scaleX = -1f, scaleY = 1f) else Modifier),
        description = description, onClick = onBack,
    )
}

/**
 * The dock and pane of a picker, the header with the back button and [trailing], then [count] rows (a lazy list
 * when [lazy]) and the [hint] under them.
 */
@Composable
private fun PickerScreen(
    spec: SystemScreenSpec,
    dock: KitDockState,
    dockActions: KitDockActions,
    tag: String,
    modifier: Modifier,
    showDock: Boolean,
    title: String,
    backDescription: String,
    onBack: () -> Unit,
    trailing: @Composable RowScope.(paneWidth: Dp) -> Unit,
    count: Int,
    key: (Int) -> String,
    facts: (Int) -> RowFacts,
    hint: String?,
    lazy: Boolean,
    row: @Composable (Int, Dp) -> Unit,
) {
    SystemFrame(spec, dock, dockActions, tag, modifier, showDock) { paneWidth ->
        val p = "$tag/Pane"
        // Rows are the Pen's exact heights, or as tall as their wrapped subtitle: measured, so the list does not drift by the
        // rounding of each row (the position of a row is summed in dp and rounded once, as in the settings list).
        val lineCount = rememberLineCounter()
        val subStyle = penStyle(OptionRow.Text.Sub.Style)
        val rowWidth = paneWidth - spec.pad * 2
        val heightOf: (Int) -> Dp = { i ->
            val f = facts(i)
            f.fixed ?: run {
                val lines = f.sub?.let { lineCount(it, subStyle, rowWidth - OptionRow.PadStart - OptionRow.PadEnd - f.reserved, SubLines) } ?: 0
                val text = OptionRow.Text.Title.H + (if (f.sub != null) OptionRow.Text.Gap + OptionRow.Text.Sub.H * lines else 0.dp) +
                    (if (f.progress) OptionRow.Text.Gap + ProgressTrack.H else 0.dp)
                maxOf(f.min, OptionRow.PadTop + OptionRow.PadBottom + text)
            }
        }
        PaneColumn(
            spec,
            header = {
                KitScreenHeader(
                    title, tag = "$p/Header", gap = ChoicePane.Header.Gap,
                    leading = { PickerBackButton(backDescription, onBack, p) },
                    trailing = { trailing(paneWidth) },
                )
            },
            scrolls = !lazy,
            lazyBody = lazy,
            body = {
                if (lazy) {
                    // The same rows one gap apart, only those near the window composed; the hint follows the last.
                    LazyColumn(
                        Modifier.fillMaxSize().testTag("$p/List"),
                        contentPadding = PaddingValues(bottom = spec.pad),
                        verticalArrangement = Arrangement.spacedBy(ChoicePane.List.Gap),
                    ) {
                        items(count, key = key) { row(it, heightOf(it)) }
                        if (hint != null) item(key = "Hint") {
                            Column {
                                Spacer(Modifier.height((spec.contentGap - ChoicePane.List.Gap).coerceAtLeast(0.dp)))
                                KitNotificationsHint(hint, tag = "$p/Hint")
                            }
                        }
                    }
                    return@PaneColumn
                }
                PenStack((0 until count).map(heightOf), ChoicePane.List.Gap, Modifier.fillMaxWidth().testTag("$p/List")) {
                    repeat(count) { row(it, heightOf(it)) }
                }
                if (hint != null) {
                    Spacer(Modifier.height(spec.contentGap))
                    KitNotificationsHint(hint, tag = "$p/Hint")
                }
            },
        )
    }
}

// --- Rows --------------------------------------------------------------------------------------

/**
 * A row of a picker: [leading] part, the [title] with the [sub] under it ([extra] under that), then [trailing].
 * With [selected] set it is one option of a radio group: the chosen one has the Pen's blue fill and outline.
 */
@Composable
private fun PickerRow(
    tag: String,
    title: String,
    sub: String?,
    height: Dp,
    modifier: Modifier = Modifier,
    selected: Boolean? = null,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    extra: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val chosen = selected == true
    val shape = penShape(OptionRow.Radius)
    val press = if (selected != null && onClick != null) Modifier.selectable(chosen, onClick = onClick, role = Role.RadioButton) else Modifier.penClick(onClick)
    Row(
        modifier.testTag(tag).fillMaxWidth().height(height).clip(shape).background(if (chosen) ChosenRow.Fill else OptionRow.Fill, shape)
            .then(if (chosen) Modifier.border(ChosenRow.Stroke, ChosenRow.StrokeColor, shape) else Modifier)
            .then(press)
            .padding(start = OptionRow.PadStart, top = OptionRow.PadTop, end = OptionRow.PadEnd, bottom = OptionRow.PadBottom),
        horizontalArrangement = Arrangement.spacedBy(OptionRow.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Column(Modifier.testTag("$tag/Text").weight(1f), verticalArrangement = Arrangement.spacedBy(OptionRow.Text.Gap)) {
            PenText(title, OptionRow.Text.Title.Style, OptionRow.Text.Title.Fill, Modifier.testTag("$tag/Text/Title"))
            if (sub != null) PenText(sub, OptionRow.Text.Sub.Style, OptionRow.Text.Sub.Fill, Modifier.testTag("$tag/Text/Sub").fillMaxWidth(), maxLines = SubLines)
            extra?.invoke()
        }
        trailing?.invoke(this)
        if (selected != null) {
            Icon(
                if (chosen) PenIcons.CheckCircle else PenIcons.RadioButtonUnchecked, null,
                Modifier.testTag("$tag/Mark").size(OptionRow.Mark.W), tint = if (chosen) ChosenRow.Mark.Fill else OptionRow.Mark.Fill,
            )
        }
    }
}

/** The icon slot of an app row: the app's icon, or the neutral tile with a [glyph] where there is none. */
@Composable
private fun AppIconSlot(icon: ImageBitmap?, glyph: ImageVector?, tag: String) {
    Box(Modifier.testTag(tag).size(AppIconTile.W).clip(penShape(AppIconTile.Radius)), contentAlignment = Alignment.Center) {
        if (icon != null) {
            Image(icon, null, Modifier.size(AppIconTile.W), contentScale = ContentScale.Crop)
        } else {
            Box(Modifier.size(AppIconTile.W).background(AppIconTile.Fill), contentAlignment = Alignment.Center) {
                if (glyph != null) Icon(glyph, null, Modifier.size(BackButton.Icon.W), tint = Color.White)
            }
        }
    }
}

// --- Screens -----------------------------------------------------------------------------------

/**
 * The option picker (Pen "SettingsChoice"): a back button and the title, the options one gap apart with a
 * check on the chosen one, then the [hint]. Tags: "Screen/Pane/Header/Back", "Screen/Pane/List/<node>", "Screen/Pane/Hint".
 * Without [showDock] only the pane is drawn; [lazy] makes the list a lazy list that scrolls itself.
 */
@Composable
fun SettingsChoiceScreen(
    spec: SystemScreenSpec,
    dock: KitDockState = KitDockState("", 0),
    title: String,
    options: List<ChoiceOption>,
    modifier: Modifier = Modifier,
    dockActions: KitDockActions = KitDockActions(),
    onBack: () -> Unit = {},
    backDescription: String = "Back",
    hint: String? = null,
    tag: String = "Screen",
    showDock: Boolean = true,
    lazy: Boolean = false,
) {
    PickerScreen(
        spec, dock, dockActions, tag, modifier, showDock, title, backDescription, onBack, trailing = {},
        count = options.size, key = { options[it].node },
        facts = { RowFacts(OptionRow.H, options[it].sub, reserved = OptionRow.Gap + OptionRow.Mark.W) },
        hint = hint, lazy = lazy,
    ) { i, height ->
        val o = options[i]
        PickerRow("$tag/Pane/List/${o.node}", o.title, o.sub, height, selected = o.selected, onClick = o.onClick)
    }
}

/**
 * The app picker (Pen "SettingsAppChoice"): as [SettingsChoiceScreen], each option with its app's icon
 * before the text.
 */
@Composable
fun SettingsAppChoiceScreen(
    spec: SystemScreenSpec,
    dock: KitDockState = KitDockState("", 0),
    title: String,
    options: List<AppChoiceOption>,
    modifier: Modifier = Modifier,
    dockActions: KitDockActions = KitDockActions(),
    onBack: () -> Unit = {},
    backDescription: String = "Back",
    tag: String = "Screen",
    showDock: Boolean = true,
    lazy: Boolean = false,
) {
    PickerScreen(
        spec, dock, dockActions, tag, modifier, showDock, title, backDescription, onBack, trailing = {},
        count = options.size, key = { options[it].key },
        facts = { RowFacts(AppIconTile.W + OptionRow.PadTop + OptionRow.PadBottom, options[it].sub, reserved = AppIconTile.W + OptionRow.Gap * 2 + OptionRow.Mark.W) },
        hint = null, lazy = lazy,
    ) { i, height ->
        val o = options[i]
        o.loadIcon?.let { load -> LaunchedEffect(o.key) { load() } }
        val t = "$tag/Pane/List/${o.node}"
        PickerRow(t, o.title, o.sub, height, selected = o.selected, onClick = o.onClick, leading = { AppIconSlot(o.icon, o.glyph, "$t/AppIcon") })
    }
}

/**
 * The offline maps manager (Pen "SettingsOfflineMaps"; the speed camera regions use it too, without the import button): the title with an Import button, then sections and
 * region rows, each with its round button (download, cancel, delete). A header too narrow for the labelled Import draws its glyph alone. Tags: "Screen/Pane/Header/Import",
 * "Screen/Pane/List/<node>", rows: ".../Text/Progress/Done", ".../Action".
 */
@Composable
fun OfflineMapsScreen(
    spec: SystemScreenSpec,
    dock: KitDockState = KitDockState("", 0),
    title: String,
    importLabel: String?,
    entries: List<OfflineMapEntry>,
    modifier: Modifier = Modifier,
    dockActions: KitDockActions = KitDockActions(),
    onBack: () -> Unit = {},
    onImport: () -> Unit = {},
    backDescription: String = "Back",
    tag: String = "Screen",
    showDock: Boolean = true,
    lazy: Boolean = false,
    /** A line under the list (the speed camera data attribution). */
    hint: String? = null,
) {
    val fontScale = LocalDensity.current.fontScale
    PickerScreen(
        spec, dock, dockActions, tag, modifier, showDock, title, backDescription, onBack,
        trailing = trailing@{ paneWidth ->
            if (importLabel == null) return@trailing
            val labelled = paneWidth - spec.pad * 2 >= ImportLabelledMinWidth * maxOf(1f, fontScale)
            KitClearAllButton(
                importLabel, tag = "$tag/Pane/Header/Import", onClick = onImport, icon = PenIcons.UploadFile,
                showLabel = labelled, iconSize = ImportButton.Icon.W,
            )
        },
        count = entries.size,
        key = {
            when (val e = entries[it]) {
                is OfflineMapEntry.Section -> e.node
                is OfflineMapEntry.Region -> e.node
                is OfflineMapEntry.Notice -> e.node
            }
        },
        facts = {
            when (val e = entries[it]) {
                is OfflineMapEntry.Section -> RowFacts(OptionRow.H, fixed = PenKit.SettingsSection.H)
                is OfflineMapEntry.Notice -> RowFacts(OptionRow.H, e.message)
                is OfflineMapEntry.Region -> RowFacts(MapRow.H, e.sub, reserved = OptionRow.Gap + MapRow.Action.W, progress = e.progress != null)
            }
        },
        hint = hint, lazy = lazy,
    ) { i, height ->
        val list = "$tag/Pane/List"
        when (val e = entries[i]) {
            is OfflineMapEntry.Section -> KitSettingsSection(e.title, Modifier.height(height), tag = "$list/${e.node}")
            is OfflineMapEntry.Notice -> PickerRow("$list/${e.node}", e.title, e.message, height)
            is OfflineMapEntry.Region -> {
                val t = "$list/${e.node}"
                PickerRow(
                    t, e.title, e.sub, height,
                    extra = e.progress?.let { done -> { DownloadBar(done, "$t/Text/Progress") } },
                    trailing = {
                        val accent = e.action.accent
                        PenDisc(
                            MapRow.Action.W, if (accent) AvailableRow.Action.Fill else MapRow.Action.Fill, e.action.icon, MapRow.Action.Icon.W,
                            if (accent) AvailableRow.Action.Icon.Fill else MapRow.Action.Icon.Fill,
                            Modifier.testTag("$t/Action"), iconModifier = Modifier.testTag("$t/Action/Icon"),
                            description = e.actionDescription, onClick = e.onAction,
                        )
                    },
                )
            }
        }
    }
}

/** The bar of a region that is downloading: the track, and the share [done] of it filled. */
@Composable
private fun DownloadBar(done: Float, tag: String) {
    val share = done.coerceIn(0f, 1f)
    val shape = penShape(ProgressTrack.Radius)
    Box(
        Modifier.testTag(tag).fillMaxWidth().height(ProgressTrack.H).clip(shape).background(ProgressTrack.Fill, shape)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(share, 0f..1f) },
    ) {
        Box(Modifier.testTag("$tag/Done").fillMaxHeight().fillMaxWidth(share).background(ProgressDone.Fill, shape))
    }
}
