package org.wpfy.carlauncher.design.kit

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import org.wpfy.carlauncher.design.kit.PenKit.AppTilePinned as Tile
import org.wpfy.carlauncher.design.kit.PenKit.NotificationCard as Card
import org.wpfy.carlauncher.design.kit.PenKit.ScreenHeaderClear as ClearHeader
import org.wpfy.carlauncher.design.kit.PenKit.ScreenHeaderSearch as SearchHeader
import org.wpfy.carlauncher.design.kit.PenKit.ScreenHeaderSearch.Search as SearchField
import org.wpfy.carlauncher.design.kit.PenKit.ScreenHeaderClear.ClearAll as ClearAll
import org.wpfy.carlauncher.design.kit.PenKit.SettingsRow as LinkRow
import org.wpfy.carlauncher.design.kit.PenKit.SettingsSwitchRowOff as SwitchOff
import org.wpfy.carlauncher.design.kit.PenKit.SettingsSwitchRowOn as SwitchOn

/*
 * The system screens' parts (Pen "System Parts" frame of the Kits cluster, 1x): screen header,
 * search field, clear-all button, app tile, settings section and rows, notification card and hint.
 * The app drawer, settings and notifications screens of every group are built from these. They
 * draw at 1x in every group (the Pen's S and M system screens use them at the same size).
 *
 * Parts repeat on a screen, so each takes [tag]: the test-tag path of its Pen node in the screen
 * ("Screen/Pane/List/HomeApp"); its insides are tagged below it by their Pen names.
 */

private fun Modifier.tagged(tag: String?, path: String = "."): Modifier =
    if (tag == null) this else testTag(if (path == ".") tag else "$tag/$path")

// --- Screen header -----------------------------------------------------------------------------

/**
 * A screen's header row: [leading] (a back button) if any, the title, a flexible gap, then [trailing] (search
 * field, clear all). The parts are [gap] apart: the Pen's header gap, or the narrower one of a picker's header.
 */
@Composable
fun KitScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    tag: String? = null,
    gap: Dp = SearchHeader.Gap,
    leading: (@Composable RowScope.() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.tagged(tag).fillMaxWidth().height(SearchHeader.H),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke(this)
        PenText(title, SearchHeader.Title.Style, SearchHeader.Title.Fill, Modifier.tagged(tag, "Title"))
        Spacer(Modifier.tagged(tag, "Spacer").weight(1f))
        trailing()
    }
}

/**
 * The search pill of a header, [width] wide (the screen's own measure). A set [onQueryChange] makes it a
 * text field holding [query]: the placeholder shows while it is empty and typing is in the title's colour.
 */
@Composable
fun KitSearchField(
    placeholder: String,
    width: Dp,
    modifier: Modifier = Modifier,
    tag: String? = null,
    onClick: () -> Unit = {},
    query: String = "",
    onQueryChange: ((String) -> Unit)? = null,
) {
    Row(
        modifier = modifier.tagged(tag).width(width).height(SearchField.H).clip(penShape(SearchField.Radius)).background(SearchField.Fill)
            .then(if (onQueryChange == null) Modifier.penClick(onClick) else Modifier).padding(start = SearchField.PadStart, end = SearchField.PadEnd),
        horizontalArrangement = Arrangement.spacedBy(SearchField.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(PenKit.ScreenHeaderSearch.Search.Icon.Vector, null, Modifier.tagged(tag, "Icon").size(SearchField.Icon.W), tint = SearchField.Icon.Fill)
        if (onQueryChange == null) {
            PenText(placeholder, SearchField.Placeholder.Style, SearchField.Placeholder.Fill, Modifier.tagged(tag, "Placeholder"))
        } else {
            val keyboard = LocalSoftwareKeyboardController.current
            val focus = LocalFocusManager.current
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.tagged(tag, "Input").fillMaxWidth(),
                    textStyle = penStyle(SearchField.Placeholder.Style).copy(color = SearchHeader.Title.Fill),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    // The filter is live, so Search only puts the keyboard away.
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); focus.clearFocus() }),
                    cursorBrush = SolidColor(SearchHeader.Title.Fill),
                )
                if (query.isEmpty()) PenText(placeholder, SearchField.Placeholder.Style, SearchField.Placeholder.Fill, Modifier.tagged(tag, "Placeholder"))
            }
        }
    }
}

/** The outlined "Clear all" pill of the notifications header (also the offline maps' Import: any header action in the Pen's pill). */
@Composable
fun KitClearAllButton(
    label: String,
    modifier: Modifier = Modifier,
    tag: String? = null,
    onClick: () -> Unit = {},
    /** The glyph before the label; null leaves it out (the pill as a plain action button). */
    icon: ImageVector? = PenKit.ScreenHeaderClear.ClearAll.Icon.Vector,
    /** False draws the glyph alone (a narrow header), the label then being its spoken description. */
    showLabel: Boolean = true,
    /** The glyph's size; the Pen's other header pills (Import) draw it smaller. */
    iconSize: Dp = ClearAll.Icon.W,
) {
    val shape = penShape(ClearAll.Radius)
    Row(
        modifier = modifier.tagged(tag).height(ClearAll.H).clip(shape).border(ClearAll.Stroke, ClearAll.StrokeColor, shape)
            .penClick(onClick).padding(start = ClearAll.PadStart, end = ClearAll.PadEnd),
        horizontalArrangement = Arrangement.spacedBy(ClearAll.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, if (showLabel) null else label, Modifier.tagged(tag, "Icon").size(iconSize), tint = ClearAll.Icon.Fill)
        if (showLabel || icon == null) PenText(label, ClearAll.Label.Style, ClearAll.Label.Fill, Modifier.tagged(tag, "Label"))
    }
}

// --- App tile ----------------------------------------------------------------------------------

/**
 * One app of the drawer grid, [width] wide (the grid's column): the app's system launcher icon in
 * the Pen icon tile, the pinned dot when [pinned], the label under it.
 */
@Composable
fun KitAppTile(
    label: String,
    icon: ImageBitmap?,
    pinned: Boolean,
    width: Dp,
    modifier: Modifier = Modifier,
    tag: String? = null,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    /** A glyph on the neutral tile while there is no [icon] (a launcher entry that is not an installed app). */
    glyph: ImageVector? = null,
) {
    Column(
        modifier = modifier.tagged(tag).width(width).height(Tile.H).clip(penShape(Tile.Radius))
            .penLongClick(onClick, onLongClick)
            .padding(top = Tile.PadTop, bottom = Tile.PadBottom),
        verticalArrangement = Arrangement.spacedBy(Tile.Gap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.tagged(tag, "IconTile").size(Tile.IconTile.W)) {
            Box(Modifier.size(Tile.IconTile.W).clip(penShape(Tile.IconTile.Radius))) {
                if (icon != null) {
                    Image(icon, null, Modifier.size(Tile.IconTile.W), contentScale = ContentScale.Fit)
                } else {
                    // Loading: the Pen's neutral tile.
                    Box(Modifier.size(Tile.IconTile.W).background(PenKit.DockPortrait.Calculator.Fill), contentAlignment = Alignment.Center) {
                        if (glyph != null) Icon(glyph, null, Modifier.size(Tile.IconTile.W / 2), tint = Tile.Label.Fill)
                    }
                }
            }
            if (pinned) {
                val dot = penShape(Tile.IconTile.Pinned.Radius)
                Box(
                    Modifier.offset(Tile.IconTile.Pinned.X, Tile.IconTile.Pinned.Y).tagged(tag, "IconTile/Pinned")
                        .size(Tile.IconTile.Pinned.W).background(Tile.IconTile.Pinned.Fill, dot)
                        .border(Tile.IconTile.Pinned.Stroke, Tile.IconTile.Pinned.StrokeColor, dot),
                )
            }
        }
        PenText(label, Tile.Label.Style, Tile.Label.Fill, Modifier.tagged(tag, "Label"))
    }
}

/**
 * The menu of an app tile (long press): pin or unpin it, and open its app info. Drawn at the tile's
 * bottom edge, with the settings rows' fill and text. Put it in the same box as the tile.
 */
@Composable
fun KitAppMenu(
    expanded: Boolean,
    pinned: Boolean,
    onDismiss: () -> Unit,
    onTogglePinned: () -> Unit,
    onOpenAppInfo: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String? = null,
) {
    DropdownMenu(expanded, onDismiss, modifier.tagged(tag), containerColor = LinkRow.Fill) {
        KitAppMenuItem(if (pinned) "Unpin" else "Pin", PenIcons.Keep, onTogglePinned)
        KitAppMenuItem("App info", PenIcons.Info, onOpenAppInfo)
    }
}

@Composable
private fun KitAppMenuItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { PenText(label, LinkRow.Text.Title.Style, LinkRow.Text.Title.Fill) },
        onClick = onClick,
        leadingIcon = { Icon(icon, null, Modifier.size(LinkRow.Icon.W), tint = LinkRow.Icon.Fill) },
    )
}

// --- Empty and waiting states --------------------------------------------------------------------

/**
 * What a screen shows in place of its content: a spinner when [loading], or an [icon], the [title], the
 * [message] under it and, when [actionLabel] is set, a pill button for [onAction]; all centred in the
 * space it is given. Drawn with the settings rows' type and the clear-all pill: there is no Pen frame for it.
 */
@Composable
fun KitStateMessage(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    tag: String? = null,
    icon: ImageVector? = null,
    loading: Boolean = false,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Box(modifier.tagged(tag).fillMaxSize(), contentAlignment = Alignment.Center) {
        if (loading) {
            CircularProgressIndicator(Modifier.tagged(tag, "Progress"), color = ClearAll.Icon.Fill)
            return@Box
        }
        Column(
            Modifier.widthIn(max = Tile.W * 4),
            verticalArrangement = Arrangement.spacedBy(Card.Gap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (icon != null) Icon(icon, null, Modifier.tagged(tag, "Icon").size(Tile.IconTile.W / 2), tint = LinkRow.Icon.Fill)
            PenText(title, LinkRow.Text.Title.Style, LinkRow.Text.Title.Fill, Modifier.tagged(tag, "Title"), maxLines = 2, textAlign = TextAlign.Center)
            PenText(message, LinkRow.Text.Sub.Style, LinkRow.Text.Sub.Fill, Modifier.tagged(tag, "Message"), maxLines = 3, textAlign = TextAlign.Center)
            if (actionLabel != null) {
                KitClearAllButton(actionLabel, Modifier.padding(top = Card.Gap), tag = tag?.let { "$it/Action" }, onClick = onAction, icon = null)
            }
        }
    }
}

// --- Settings ----------------------------------------------------------------------------------

@Composable
fun KitSettingsSection(title: String, modifier: Modifier = Modifier, tag: String? = null) {
    PenText(title, PenKit.SettingsSection.Style, PenKit.SettingsSection.Fill, modifier.tagged(tag))
}

/**
 * A settings row that opens a choice: title, current value under it, a chevron. [valueLines] above
 * one lets the value wrap: the row is then as tall as its text (its vertical padding around it).
 * A null [onClick] makes it a read-only value row: no chevron, not clickable.
 */
@Composable
fun KitSettingsRow(title: String, value: String, modifier: Modifier = Modifier, tag: String? = null, valueLines: Int = 1, onClick: (() -> Unit)? = {}) {
    SettingsRowFrame(modifier, tag, onClick, valueLines) {
        SettingsRowText(title, value, tag, valueLines)
        if (onClick != null) Icon(PenKit.SettingsRow.Icon.Vector, null, Modifier.tagged(tag, "Icon").size(LinkRow.Icon.W), tint = LinkRow.Icon.Fill)
    }
}

/** A settings row with a switch: title, its explanation under it, the switch. [bodyLines] as [KitSettingsRow]'s valueLines. */
@Composable
fun KitSettingsSwitchRow(
    title: String,
    body: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    tag: String? = null,
    bodyLines: Int = 1,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    val semantics = Modifier.semantics(mergeDescendants = true) {
        role = Role.Switch
        stateDescription = if (checked) "On" else "Off"
    }
    SettingsRowFrame(modifier.then(semantics), tag, onClick = { onCheckedChange(!checked) }, lines = bodyLines) {
        SettingsRowText(title, body, tag, bodyLines)
        val track = if (checked) SwitchOn.Switch.Fill else SwitchOff.Switch.Fill
        val thumb = if (checked) SwitchOn.Switch.Thumb else null
        Box(
            Modifier.tagged(tag, "Switch").width(SwitchOn.Switch.W).height(SwitchOn.Switch.H)
                .background(track, penShape(SwitchOn.Switch.Radius)),
        ) {
            Box(
                Modifier.offset(if (thumb != null) SwitchOn.Switch.Thumb.X else SwitchOff.Switch.Thumb.X, SwitchOn.Switch.Thumb.Y)
                    .tagged(tag, "Switch/Thumb").size(SwitchOn.Switch.Thumb.W)
                    .background(if (checked) SwitchOn.Switch.Thumb.Fill else SwitchOff.Switch.Thumb.Fill, penShape(999f.asDp())),
            )
        }
    }
}

@Composable
private fun SettingsRowFrame(modifier: Modifier, tag: String?, onClick: (() -> Unit)?, lines: Int = 1, content: @Composable RowScope.() -> Unit) {
    // One line: the Pen's fixed row height. More: as tall as the text, the row's vertical padding around it.
    val size = if (lines > 1) Modifier else Modifier.height(LinkRow.H)
    val padding = if (lines > 1) Modifier.padding(top = LinkRow.PadTop, bottom = LinkRow.PadBottom) else Modifier
    Row(
        modifier = modifier.tagged(tag).fillMaxWidth().then(size).clip(penShape(LinkRow.Radius)).background(LinkRow.Fill)
            .penClick(onClick).then(padding).padding(start = LinkRow.PadStart, end = LinkRow.PadEnd),
        horizontalArrangement = Arrangement.spacedBy(LinkRow.Gap),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun RowScope.SettingsRowText(title: String, sub: String, tag: String?, lines: Int = 1) {
    Column(Modifier.tagged(tag, "Text").weight(1f), verticalArrangement = Arrangement.spacedBy(LinkRow.Text.Gap)) {
        PenText(title, LinkRow.Text.Title.Style, LinkRow.Text.Title.Fill, Modifier.tagged(tag, "Text/Title"))
        PenText(sub, LinkRow.Text.Sub.Style, LinkRow.Text.Sub.Fill, Modifier.tagged(tag, "Text/Sub").fillMaxWidth().wrapLines(lines), maxLines = lines)
    }
}

// --- Notifications -----------------------------------------------------------------------------

/**
 * One notification: the posting app's system icon in the Pen's round slot, meta line (app and
 * age), title and body, and the dismiss button. [bodyLines] above one lets the body wrap: the card is
 * then as tall as its text (its vertical padding around it); [bodyStyle] is the body's text style.
 * A null [onDismiss] leaves the dismiss button out (a notification Android will not clear).
 */
@Composable
fun KitNotificationCard(
    meta: String,
    title: String,
    body: String,
    appIcon: ImageBitmap?,
    modifier: Modifier = Modifier,
    tag: String? = null,
    bodyLines: Int = 1,
    bodyStyle: TextStyle = Card.Text.Body.Style,
    onClick: (() -> Unit)? = {},
    onDismiss: (() -> Unit)? = {},
    /** What the dismiss button is called to accessibility services. */
    dismissDescription: String = "Dismiss",
) {
    val size = if (bodyLines > 1) Modifier else Modifier.height(Card.H)
    val padding = if (bodyLines > 1) Modifier.padding(top = Card.PadTop, bottom = Card.PadBottom) else Modifier
    Row(
        modifier = modifier.tagged(tag).fillMaxWidth().then(size).clip(penShape(Card.Radius)).background(Card.Fill)
            .then(if (onClick != null) Modifier.penClick(onClick) else Modifier).then(padding).padding(start = Card.PadStart, end = Card.PadEnd),
        horizontalArrangement = Arrangement.spacedBy(Card.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.tagged(tag, "AppIcon").size(Card.AppIcon.W).clip(penShape(Card.AppIcon.Radius))) {
            if (appIcon != null) {
                Image(appIcon, null, Modifier.size(Card.AppIcon.W), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.size(Card.AppIcon.W).background(PenKit.DockPortrait.Calculator.Fill))
            }
        }
        Column(Modifier.tagged(tag, "Text").weight(1f), verticalArrangement = Arrangement.spacedBy(Card.Text.Gap)) {
            PenText(meta, Card.Text.Meta.Style, Card.Text.Meta.Fill, Modifier.tagged(tag, "Text/Meta"))
            PenText(title, Card.Text.Title.Style, Card.Text.Title.Fill, Modifier.tagged(tag, "Text/Title"))
            PenText(body, bodyStyle, Card.Text.Body.Fill, Modifier.tagged(tag, "Text/Body").fillMaxWidth().wrapLines(bodyLines), maxLines = bodyLines)
        }
        if (onDismiss != null) PenDisc(
            Card.Dismiss.W, androidx.compose.ui.graphics.Color.Transparent, PenKit.NotificationCard.Dismiss.Icon.Vector, Card.Dismiss.Icon.W, Card.Dismiss.Icon.Fill,
            Modifier.tagged(tag, "Dismiss"), iconModifier = Modifier.tagged(tag, "Dismiss/Icon"), description = dismissDescription, onClick = onDismiss,
        )
    }
}

/** The line under the notification list: why some notifications are not shown while driving. */
@Composable
fun KitNotificationsHint(text: String, modifier: Modifier = Modifier, tag: String? = null) {
    PenText(text, PenKit.NotificationsHint.Style, PenKit.NotificationsHint.Fill, modifier.tagged(tag), maxLines = 2)
}

private fun Float.asDp(): Dp = Dp(this)

/** Wrapping text is not clipped by the height its parent gives it. */
private fun Modifier.wrapLines(lines: Int): Modifier =
    if (lines > 1) wrapContentHeight(Alignment.Top, unbounded = true) else this
