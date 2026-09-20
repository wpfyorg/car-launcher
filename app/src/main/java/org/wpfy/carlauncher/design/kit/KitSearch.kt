package org.wpfy.carlauncher.design.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.wpfy.carlauncher.design.theme.FixedScale
import org.wpfy.carlauncher.design.theme.OverlayTier
import org.wpfy.carlauncher.design.kit.PenKit.DestinationCard as CardKit
import org.wpfy.carlauncher.design.kit.PenKit.DestinationPin as PinKit
import org.wpfy.carlauncher.design.kit.PenKit.SearchFieldFocused as FieldKit
import org.wpfy.carlauncher.design.kit.PenKit.SearchResultRow as RowKit

/*
 * The destination search kits (Pen "Search Field · Focused", "Search Result Row", "Destination Card",
 * "Destination Pin"). The field and the row are drawn at the Large tier's 1x tokens and scaled to their
 * tier by [searchKitScale] (the Pen tier's field height over the Large one's); the card and the pin
 * exist in the Medium tier only, so their tokens are Medium's own and they are drawn at 1x.
 */

/** The scale the search field and the result row draw at in [tier]. */
fun searchKitScale(tier: OverlayTier): Float = when (tier) {
    OverlayTier.Small -> PenKit.SearchScale.Small
    OverlayTier.Medium -> PenKit.SearchScale.Medium
    OverlayTier.Large -> PenKit.SearchScale.Large
    OverlayTier.XLarge -> PenKit.SearchScale.XLarge
}

/** The glyph colour of the Pen's place rows and actions ("$cl-accent-muted"); a recent search keeps the row's own glyph colour. */
private val AccentMuted = CardKit.Header.Glyph.Icon.Fill

/** The dim text colour of a row's address and of a field's hint. */
private val DimText = RowKit.Text.Address.Fill

/** One row of the destination search list. */
@Immutable
data class KitSearchRow(
    val name: String,
    /** The address line under the name; null leaves it out. */
    val detail: String?,
    /** The distance at the end of the row; null leaves it out. */
    val distance: String?,
    val glyph: ImageVector,
    /** The glyph in the accent colour (a place, an action); false draws a recent search in the row's own colour. */
    val accented: Boolean = true,
    val enabled: Boolean = true,
    /** Test-tag name of the row below its list ("Result<Name>" in the Pen frame); null leaves the row untagged. */
    val tag: String? = null,
    val onClick: (() -> Unit)? = null,
)

/**
 * The focused search field (Pen "Search Field · Focused"), [width] wide at [scale]: a back arrow, the live
 * [query] with its caret ([placeholder] while it is empty) and a clear button (while there is a query).
 * [autoFocus] puts the cursor in it and raises the keyboard when it first shows.
 */
@Composable
fun KitSearchFieldFocused(
    query: String,
    placeholder: String,
    width: Dp,
    scale: Float,
    modifier: Modifier = Modifier,
    onQueryChange: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onClear: () -> Unit = {},
    autoFocus: Boolean = false,
    backDescription: String = "Back",
    clearDescription: String = "Clear",
) {
    val c = "SearchFieldFocused"
    Box(modifier.pen(c).width(width).height(FieldKit.H * scale)) {
        FixedScale(scale) {
            val shape = penShape(FieldKit.Radius)
            val keyboard = LocalSoftwareKeyboardController.current
            val focus = LocalFocusManager.current
            val requester = remember { FocusRequester() }
            // Keep IME selection and composition when the measured field changes width.
            // A String-only field can place the cursor before the first typed character here.
            var fieldValue by remember { mutableStateOf(TextFieldValue(query, selection = TextRange(query.length))) }
            var lastParentQuery by remember { mutableStateOf(query) }
            val pendingEdits = remember { mutableListOf<String>() }
            LaunchedEffect(query) {
                if (lastParentQuery != query) {
                    lastParentQuery = query
                    val acknowledgedIndex = pendingEdits.indexOf(query)
                    if (acknowledgedIndex >= 0) {
                        // A parent echo may lag behind newer keystrokes. Keep the local text.
                        pendingEdits.subList(0, acknowledgedIndex + 1).clear()
                    } else {
                        pendingEdits.clear()
                        if (fieldValue.text != query) {
                            fieldValue = TextFieldValue(query, selection = TextRange(query.length))
                        }
                    }
                }
            }
            if (autoFocus) {
                LaunchedEffect(Unit) {
                    requester.requestFocus()
                    keyboard?.show()
                }
            }
            val style = penStyle(FieldKit.Query.Text.Style).copy(color = FieldKit.Query.Text.Fill)
            // The caret is as tall as the text's line where the line outgrows the Pen's caret (large text).
            // The query hugs its text (a text field alone is ten characters wide at least).
            val measurer = rememberTextMeasurer()
            val textWidth = with(LocalDensity.current) { measurer.measure(fieldValue.text, style, maxLines = 1).size.width.toDp() }
            val caretHeight = maxOf(FieldKit.Query.Caret.H, with(LocalDensity.current) { style.lineHeight.toDp() })
            Row(
                Modifier.fillMaxSize().clip(shape).background(FieldKit.Fill, shape).border(FieldKit.Stroke, FieldKit.StrokeColor, shape)
                    .padding(start = FieldKit.PadStart, end = FieldKit.PadEnd),
                horizontalArrangement = Arrangement.spacedBy(FieldKit.Gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PenDisc(
                    FieldKit.Back.W, Color.Transparent, FieldKit.Back.Icon.Vector, FieldKit.Back.Icon.W, FieldKit.Back.Icon.Fill,
                    modifier = Modifier.pen(c, "Back"), radius = FieldKit.Back.Radius, iconModifier = Modifier.pen(c, "Back/Icon"),
                    description = backDescription, onClick = onBack,
                )
                Row(
                    Modifier.weight(1f).heightIn(min = FieldKit.Query.H).pen(c, "Query")
                        .pointerInput(requester, keyboard) {
                            detectTapGestures {
                                requester.requestFocus()
                                keyboard?.show()
                            }
                        },
                    horizontalArrangement = Arrangement.spacedBy(FieldKit.Query.Gap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = fieldValue,
                        onValueChange = { value ->
                            val textChanged = value.text != fieldValue.text
                            fieldValue = value
                            if (textChanged) {
                                pendingEdits.add(value.text)
                                if (pendingEdits.size > 32) pendingEdits.removeAt(0)
                                onQueryChange(value.text)
                            }
                        },
                        modifier = Modifier.weight(1f, fill = false).width(textWidth.coerceAtLeast(1.dp) + 16.dp).focusRequester(requester).pen(c, "Query/Text"),
                        textStyle = style,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        // The list is live, so Search only puts the keyboard away.
                        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); focus.clearFocus() }),
                        cursorBrush = SolidColor(Color.Transparent),
                    )
                    Box(Modifier.pen(c, "Query/Caret").width(FieldKit.Query.Caret.W).height(caretHeight).background(FieldKit.Query.Caret.Fill))
                    if (fieldValue.text.isEmpty()) PenText(placeholder, FieldKit.Query.Text.Style, DimText, Modifier.pen(c, "Query/Hint"))
                }
                if (fieldValue.text.isNotEmpty()) {
                    PenDisc(
                        FieldKit.Clear.W, FieldKit.Clear.Fill, FieldKit.Clear.Icon.Vector, FieldKit.Clear.Icon.W, FieldKit.Clear.Icon.Fill,
                        modifier = Modifier.pen(c, "Clear"), radius = FieldKit.Clear.Radius, iconModifier = Modifier.pen(c, "Clear/Icon"),
                        description = clearDescription,
                        onClick = {
                            pendingEdits.add("")
                            fieldValue = TextFieldValue("")
                            onClear()
                        },
                    )
                }
            }
        }
    }
}

/**
 * A search result row (Pen "Search Result Row"), [width] wide at [scale]: the glyph disc, the name over the
 * address, and the distance. The row is the Pen's height, taller where large text needs it.
 */
@Composable
fun KitSearchResultRow(row: KitSearchRow, width: Dp, scale: Float, modifier: Modifier = Modifier) {
    val c = "SearchResultRow"
    Box(modifier.width(width)) {
        FixedScale(scale) {
            // The row is the Pen's height at least; its content is centred in it (large text does not grow it).
            Row(
                Modifier.pen(c).fillMaxWidth().heightIn(min = RowKit.H)
                    .then(if (row.onClick != null) Modifier.clickableRow(row.enabled, row.name, row.onClick) else Modifier)
                    .padding(start = RowKit.PadStart, end = RowKit.PadEnd),
                horizontalArrangement = Arrangement.spacedBy(RowKit.Gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PenDisc(
                    RowKit.Glyph.W, RowKit.Glyph.Fill, row.glyph, RowKit.Glyph.Icon.W,
                    if (row.accented) AccentMuted else RowKit.Glyph.Icon.Fill,
                    modifier = Modifier.pen(c, "Glyph"), radius = RowKit.Glyph.Radius, iconModifier = Modifier.pen(c, "Glyph/Icon"),
                )
                Column(Modifier.weight(1f).pen(c, "Text"), verticalArrangement = Arrangement.spacedBy(RowKit.Text.Gap)) {
                    PenText(row.name, RowKit.Text.Name.Style, RowKit.Text.Name.Fill, Modifier.fillMaxWidth().pen(c, "Text/Name"))
                    if (row.detail != null) {
                        PenText(row.detail, RowKit.Text.Address.Style, RowKit.Text.Address.Fill, Modifier.fillMaxWidth().pen(c, "Text/Address"))
                    }
                }
                if (row.distance != null) {
                    PenText(row.distance, RowKit.Distance.Style, RowKit.Distance.Fill, Modifier.pen(c, "Distance"))
                }
            }
        }
    }
}

/**
 * A line of status in the result list's card (the list is empty, routing is not ready yet): the Pen has no frame for
 * it, so it is the row's address text, inset like the row, at least a row tall.
 */
@Composable
fun KitSearchNotice(text: String, width: Dp, scale: Float, modifier: Modifier = Modifier) {
    Box(modifier.width(width)) {
        FixedScale(scale) {
            Box(Modifier.fillMaxWidth().heightIn(min = RowKit.H).padding(horizontal = RowKit.PadStart), contentAlignment = Alignment.CenterStart) {
                PenText(text, RowKit.Text.Address.Style, DimText, Modifier.fillMaxWidth(), maxLines = 2)
            }
        }
    }
}

private fun Modifier.clickableRow(enabled: Boolean, description: String, onClick: () -> Unit): Modifier =
    this.semantics { contentDescription = description; role = Role.Button }
        .then(if (enabled) Modifier.penClick(onClick) else Modifier)

/**
 * The destination card of the preview (Pen "Destination Card · Medium", drawn at 1x, [width] wide): the place's
 * [name] and [address] with a close button, the trip facts the route gives ([travel] time, [distance], [arrival];
 * any left null is left out) and the Start button.
 */
@Composable
fun KitDestinationCard(
    name: String,
    address: String?,
    glyph: ImageVector,
    modifier: Modifier = Modifier,
    width: Dp = CardKit.W,
    travel: String? = null,
    distance: String? = null,
    arrival: String? = null,
    startLabel: String = CardKit.Start.Label.Reference,
    closeDescription: String = "Close",
    onClose: () -> Unit = {},
    onStart: () -> Unit = {},
) {
    val c = "DestinationCard"
    val shape = penShape(CardKit.Radius)
    Column(
        modifier.pen(c).width(width).clip(shape).background(CardKit.Fill, shape).border(CardKit.Stroke, CardKit.StrokeColor, shape)
            .padding(start = CardKit.PadStart, end = CardKit.PadEnd, top = CardKit.PadTop, bottom = CardKit.PadBottom),
        verticalArrangement = Arrangement.spacedBy(CardKit.Gap),
    ) {
        Row(Modifier.pen(c, "Header").fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CardKit.Header.Gap)) {
            PenDisc(
                CardKit.Header.Glyph.W, CardKit.Header.Glyph.Fill, glyph, CardKit.Header.Glyph.Icon.W, CardKit.Header.Glyph.Icon.Fill,
                modifier = Modifier.pen(c, "Header/Glyph"), radius = CardKit.Header.Glyph.Radius, iconModifier = Modifier.pen(c, "Header/Glyph/Icon"),
            )
            Column(Modifier.weight(1f).pen(c, "Header/Text"), verticalArrangement = Arrangement.spacedBy(CardKit.Header.Text.Gap)) {
                PenText(name, CardKit.Header.Text.Name.Style, CardKit.Header.Text.Name.Fill, Modifier.fillMaxWidth().pen(c, "Header/Text/Name"))
                if (address != null) {
                    PenText(address, CardKit.Header.Text.Address.Style, CardKit.Header.Text.Address.Fill, Modifier.fillMaxWidth().pen(c, "Header/Text/Address"))
                }
            }
            PenDisc(
                CardKit.Header.Close.W, CardKit.Header.Close.Fill, CardKit.Header.Close.Icon.Vector, CardKit.Header.Close.Icon.W, CardKit.Header.Close.Icon.Fill,
                modifier = Modifier.pen(c, "Header/Close"), radius = CardKit.Header.Close.Radius, stroke = CardKit.Header.Close.Stroke,
                strokeColor = CardKit.Header.Close.StrokeColor, iconModifier = Modifier.pen(c, "Header/Close/Icon"),
                description = closeDescription, onClick = onClose,
            )
        }
        if (travel != null || distance != null || arrival != null) {
            Row(
                Modifier.pen(c, "TripFacts").fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CardKit.TripFacts.Gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (travel != null) {
                    val pill = penShape(CardKit.TripFacts.TravelTime.Radius)
                    Row(
                        Modifier.pen(c, "TripFacts/TravelTime").clip(pill).background(CardKit.TripFacts.TravelTime.Fill, pill)
                            .padding(
                                start = CardKit.TripFacts.TravelTime.PadStart, end = CardKit.TripFacts.TravelTime.PadEnd,
                                top = CardKit.TripFacts.TravelTime.PadTop, bottom = CardKit.TripFacts.TravelTime.PadBottom,
                            ),
                        horizontalArrangement = Arrangement.spacedBy(CardKit.TripFacts.TravelTime.Gap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            CardKit.TripFacts.TravelTime.Icon.Vector, null,
                            Modifier.pen(c, "TripFacts/TravelTime/Icon").size(CardKit.TripFacts.TravelTime.Icon.W), tint = CardKit.TripFacts.TravelTime.Icon.Fill,
                        )
                        PenText(travel, CardKit.TripFacts.TravelTime.Value.Style, CardKit.TripFacts.TravelTime.Value.Fill, Modifier.pen(c, "TripFacts/TravelTime/Value"))
                    }
                }
                if (distance != null) {
                    Row(
                        Modifier.pen(c, "TripFacts/Distance"),
                        horizontalArrangement = Arrangement.spacedBy(CardKit.TripFacts.Distance.Gap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            CardKit.TripFacts.Distance.Icon.Vector, null,
                            Modifier.pen(c, "TripFacts/Distance/Icon").size(CardKit.TripFacts.Distance.Icon.W), tint = CardKit.TripFacts.Distance.Icon.Fill,
                        )
                        PenText(distance, CardKit.TripFacts.Distance.Value.Style, CardKit.TripFacts.Distance.Value.Fill, Modifier.pen(c, "TripFacts/Distance/Value"))
                    }
                }
                if (arrival != null) {
                    if (travel != null || distance != null) {
                        Box(
                            Modifier.pen(c, "TripFacts/Dot").size(CardKit.TripFacts.Dot.W)
                                .clip(penShape(999.dp)).background(CardKit.TripFacts.Dot.Fill, penShape(999.dp)),
                        )
                    }
                    PenText(arrival, CardKit.TripFacts.Arrival.Style, CardKit.TripFacts.Arrival.Fill, Modifier.pen(c, "TripFacts/Arrival"))
                }
            }
        }
        val start = penShape(CardKit.Start.Radius)
        Row(
            Modifier.pen(c, "Start").fillMaxWidth().height(CardKit.Start.H).clip(start).background(CardKit.Start.Fill, start).penClick(onStart),
            horizontalArrangement = Arrangement.spacedBy(CardKit.Start.Gap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(CardKit.Start.Icon.Vector, null, Modifier.pen(c, "Start/Icon").size(CardKit.Start.Icon.W), tint = CardKit.Start.Icon.Fill)
            PenText(startLabel, CardKit.Start.Label.Style, CardKit.Start.Label.Fill, Modifier.pen(c, "Start/Label"))
        }
    }
}

/** The destination marker (Pen "Destination Pin · Medium", drawn at 1x): a badge with [glyph] over its anchor dot. */
@Composable
fun KitDestinationPin(glyph: ImageVector, modifier: Modifier = Modifier) {
    val c = "DestinationPin"
    val round = penShape(PinKit.Badge.Radius)
    Column(
        modifier.pen(c).width(PinKit.W),
        verticalArrangement = Arrangement.spacedBy(PinKit.Gap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.pen(c, "Badge").size(PinKit.Badge.W).clip(round).background(PinKit.Badge.Fill, round).border(PinKit.Badge.Stroke, PinKit.Badge.StrokeColor, round),
            contentAlignment = Alignment.Center,
        ) {
            Icon(glyph, null, Modifier.pen(c, "Badge/Icon").size(PinKit.Badge.Icon.W), tint = PinKit.Badge.Icon.Fill)
        }
        Box(
            Modifier.pen(c, "Anchor").size(PinKit.Anchor.W).clip(round).background(PinKit.Anchor.Fill, round).border(PinKit.Anchor.Stroke, PinKit.Anchor.StrokeColor, round),
        )
    }
}
