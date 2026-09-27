package org.wpfy.carlauncher.design.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.wpfy.carlauncher.design.kit.KitDestinationCard
import org.wpfy.carlauncher.design.kit.KitDestinationPin
import org.wpfy.carlauncher.design.kit.KitSearchFieldFocused
import org.wpfy.carlauncher.design.kit.KitSearchNotice
import org.wpfy.carlauncher.design.kit.KitSearchResultRow
import org.wpfy.carlauncher.design.kit.KitSearchRow
import org.wpfy.carlauncher.design.kit.PenDisc
import org.wpfy.carlauncher.design.kit.PenIcons
import org.wpfy.carlauncher.design.kit.PenKit
import org.wpfy.carlauncher.design.kit.PenScreens
import org.wpfy.carlauncher.design.kit.PenText
import org.wpfy.carlauncher.design.kit.penShape
import org.wpfy.carlauncher.design.kit.searchKitScale
import org.wpfy.carlauncher.design.theme.OverlayTier
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeL.SearchResults.MapPane.SearchPanel as L
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeM.SearchPreview.MapPane.FieldSelected as SelectedKit
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeM.SearchResults.MapPane.SearchPanel as M
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeS.SearchResults.MapPane.SearchPanel as S
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeXL.SearchResults.MapPane.SearchPanel as XL

/** What the destination search shows over the map: the result list, or the preview of the chosen place. */
@Immutable
sealed interface KitSearchState {
    val query: String
}

/**
 * The result list (Pen "Search · results"): the focused field with [query] and the [rows]; a [notice] (the list is
 * empty, routing is not ready yet) shows in the list's card, over or in place of the rows.
 */
@Immutable
data class KitSearchResults(
    override val query: String,
    val placeholder: String,
    val rows: List<KitSearchRow>,
    val notice: String? = null,
    val onQueryChange: (String) -> Unit = {},
    val onBack: () -> Unit = {},
    val onClear: () -> Unit = {},
    val autoFocus: Boolean = false,
) : KitSearchState

/**
 * The chosen place (Pen "Search · destination preview", Medium): the selected field with [query] and a back arrow,
 * the destination card and, at [pin], the destination pin. Trip facts the route does not give yet are null.
 */
@Immutable
data class KitSearchPreview(
    override val query: String,
    val name: String,
    val address: String?,
    val glyph: ImageVector,
    val travel: String? = null,
    val distance: String? = null,
    val arrival: String? = null,
    /** Where the pin stands in the pane (its top-start corner); null leaves it out. */
    val pin: PinPlace? = null,
    val onBack: () -> Unit = {},
    val onClose: () -> Unit = {},
    val onStart: () -> Unit = {},
) : KitSearchState {
    @Immutable data class PinPlace(val x: Dp, val y: Dp)
}

/** How the search panel and the HUD's Report button and map controls share the pane (what the Pen frames do per tier). */
enum class SearchControls {
    /** The panel is drawn over them and they stay (Small: the panel is narrower than the pane, they stand below it). */
    Overlay,
    /** The panel narrows to stop one row gap short of their column (Large, XL). */
    Clear,
    /** The panel keeps its width; where it would reach their column they are left out (Medium: it covers them). */
    HideWhenReached,
}

/**
 * The measures of a tier's search panel that are the Pen frame's own: the kit scale, the gap between the field and the
 * list, the list card's look, and how the HUD's Report button and map controls share the pane with it.
 */
@Immutable
class SearchPanelSpec internal constructor(
    val scale: Float,
    val gap: Dp,
    val radius: Dp,
    val padV: Dp,
    val stroke: Dp,
    val strokeColor: Color,
    val fill: Color,
    val dividerHeight: Dp,
    val dividerFill: Color,
    val controls: SearchControls,
) {
    /** The panel's natural width: the focused field's kit width at this tier. */
    val width: Dp get() = PenKit.SearchFieldFocused.W * scale

    companion object {
        val Small = SearchPanelSpec(
            searchKitScale(OverlayTier.Small), S.Gap, S.Results.Radius, S.Results.PadTop, S.Results.Stroke, S.Results.StrokeColor, S.Results.Fill,
            S.Results.Divider1.H, S.Results.Divider1.Fill, controls = SearchControls.Overlay,
        )
        val Medium = SearchPanelSpec(
            searchKitScale(OverlayTier.Medium), M.Gap, M.Results.Radius, M.Results.PadTop, M.Results.Stroke, M.Results.StrokeColor, M.Results.Fill,
            M.Results.Divider1.H, M.Results.Divider1.Fill, controls = SearchControls.HideWhenReached,
        )
        val Large = SearchPanelSpec(
            searchKitScale(OverlayTier.Large), L.Gap, L.Results.Radius, L.Results.PadTop, L.Results.Stroke, L.Results.StrokeColor, L.Results.Fill,
            L.Results.Divider1.H, L.Results.Divider1.Fill, controls = SearchControls.Clear,
        )
        val XLarge = SearchPanelSpec(
            searchKitScale(OverlayTier.XLarge), XL.Gap, XL.Results.Radius, XL.Results.PadTop, XL.Results.Stroke, XL.Results.StrokeColor, XL.Results.Fill,
            XL.Results.Divider1.H, XL.Results.Divider1.Fill, controls = SearchControls.Clear,
        )

        fun forTier(tier: OverlayTier): SearchPanelSpec = when (tier) {
            OverlayTier.Small -> Small
            OverlayTier.Medium -> Medium
            OverlayTier.Large -> Large
            OverlayTier.XLarge -> XLarge
        }
    }
}

/**
 * The result list panel: the focused field, then the list card (up to what [maxHeight] leaves, scrolling past it). It
 * is as wide as the constraints it is given. Tags: "<tag>/FieldFocused", "<tag>/Results" and below it each row by its
 * [KitSearchRow.tag] and "Divider<n>" between rows, as the Pen frame names them.
 */
@Composable
internal fun SearchResultsPanel(state: KitSearchResults, spec: SearchPanelSpec, tag: String, modifier: Modifier = Modifier) {
    // The results stay above the keyboard: the list is bounded by what is left.
    BoxWithConstraints(modifier.imePadding()) {
        val width = maxWidth
        val maxHeight = this.maxHeight
        Column(Modifier.width(width).heightIn(max = maxHeight).testTag(tag), verticalArrangement = Arrangement.spacedBy(spec.gap)) {
            Box(Modifier.testTag("$tag/FieldFocused")) {
                KitSearchFieldFocused(
                    state.query, state.placeholder, width, spec.scale,
                    onQueryChange = state.onQueryChange, onBack = state.onBack, onClear = state.onClear, autoFocus = state.autoFocus,
                )
            }
            if (state.rows.isNotEmpty() || state.notice != null) {
                val shape = penShape(spec.radius)
                Column(
                    Modifier.weight(1f, fill = false).width(width).testTag("$tag/Results").clip(shape).background(spec.fill, shape)
                        .border(spec.stroke, spec.strokeColor, shape).verticalScroll(rememberScrollState())
                        .padding(vertical = spec.padV),
                ) {
                    state.notice?.let { Box(Modifier.testTag("$tag/Results/Notice")) { KitSearchNotice(it, width, spec.scale) } }
                    state.rows.forEachIndexed { i, row ->
                        if (i > 0 || state.notice != null) {
                            Box(Modifier.testTag("$tag/Results/Divider$i").fillMaxWidth().height(spec.dividerHeight).background(spec.dividerFill))
                        }
                        Box(if (row.tag != null) Modifier.testTag("$tag/Results/${row.tag}") else Modifier) {
                            KitSearchResultRow(row, width, spec.scale)
                        }
                    }
                }
            }
        }
    }
}

/** The preview's field (Pen "Field · selected": the Medium frame's own frame, not a kit part): back arrow and the query. */
@Composable
internal fun SearchPreviewField(state: KitSearchPreview, tag: String, backDescription: String, modifier: Modifier = Modifier) {
    val shape = penShape(SelectedKit.Radius)
    Row(
        modifier.testTag(tag).fillMaxWidth().height(SelectedKit.H).clip(shape).background(SelectedKit.Fill, shape)
            .border(SelectedKit.Stroke, SelectedKit.StrokeColor, shape).padding(start = SelectedKit.PadStart, end = SelectedKit.PadEnd),
        horizontalArrangement = Arrangement.spacedBy(SelectedKit.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PenDisc(
            SelectedKit.Back.W, Color.Transparent, PenIcons.ArrowBack,
            SelectedKit.Back.Icon.W, SelectedKit.Back.Icon.Fill, modifier = Modifier.testTag("$tag/Back"), radius = SelectedKit.Back.Radius,
            iconModifier = Modifier.testTag("$tag/Back/Icon"), description = backDescription, onClick = state.onBack,
        )
        PenText(state.query, SelectedKit.Query.Style, SelectedKit.Query.Fill, Modifier.weight(1f).testTag("$tag/Query"))
    }
}

/** The preview's destination card, as wide as the constraints it is given, tagged as the Pen frame's node. */
@Composable
internal fun SearchPreviewCard(state: KitSearchPreview, tag: String, closeDescription: String, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.testTag(tag)) {
        KitDestinationCard(
            state.name, state.address, state.glyph, width = maxWidth, travel = state.travel, distance = state.distance, arrival = state.arrival,
            closeDescription = closeDescription, onClose = state.onClose, onStart = state.onStart,
        )
    }
}

/** The slots the search layer takes in a HUD's layout. */
internal enum class SearchSlot { Panel, PreviewField, PreviewPin, PreviewCard }

private const val BackDescription = "Back"
private const val CloseDescription = "Close"

/** The search layer's composables, each a direct child of the HUD's layout under its [SearchSlot]. Tags below "<tag>". */
@Composable
internal fun SearchLayerContent(state: KitSearchState?, spec: SearchPanelSpec, tag: String) {
    when (state) {
        null -> Unit
        is KitSearchResults -> Box(Modifier.layoutId(SearchSlot.Panel)) { SearchResultsPanel(state, spec, "$tag/SearchPanel") }
        is KitSearchPreview -> {
            Box(Modifier.layoutId(SearchSlot.PreviewField)) { SearchPreviewField(state, "$tag/FieldSelected", BackDescription) }
            if (state.pin != null) {
                Box(Modifier.layoutId(SearchSlot.PreviewPin).testTag("$tag/DestinationPin")) { KitDestinationPin(state.glyph) }
            }
            Box(Modifier.layoutId(SearchSlot.PreviewCard)) { SearchPreviewCard(state, "$tag/DestinationCard", CloseDescription) }
        }
    }
}

/** The search layer measured: what [measureSearch] made of the slots, for [placeSearch]. */
internal class SearchMeasured(
    val panel: Placeable?, val field: Placeable?, val pin: Placeable?, val card: Placeable?,
    val x: Int, val top: Int, val bottom: Int, val pinX: Int, val pinY: Int,
)

/**
 * Measures the search layer in a pane [pw] x [ph]: the results panel at most [panelWidth] wide and as tall as the pane
 * leaves between [top] and the [margin] under it; the preview's field and card across the pane inside the margin.
 */
internal fun Density.measureSearch(
    byId: Map<Any?, Measurable>, state: KitSearchState?, pw: Int, ph: Int, margin: Int, top: Int, panelWidth: Int,
): SearchMeasured {
    val inner = (pw - margin * 2).coerceAtLeast(0)
    val panel = byId[SearchSlot.Panel]?.measure(Constraints(minWidth = panelWidth, maxWidth = panelWidth, maxHeight = (ph - top - margin).coerceAtLeast(0)))
    val field = byId[SearchSlot.PreviewField]?.measure(Constraints.fixedWidth(inner))
    val card = byId[SearchSlot.PreviewCard]?.measure(Constraints(minWidth = inner, maxWidth = inner, maxHeight = ph))
    val pin = byId[SearchSlot.PreviewPin]?.measure(Constraints())
    val place = (state as? KitSearchPreview)?.pin
    return SearchMeasured(panel, field, pin, card, margin, top, margin, place?.x?.roundToPx() ?: 0, place?.y?.roundToPx() ?: 0)
}

internal fun Placeable.PlacementScope.placeSearch(m: SearchMeasured, ph: Int) {
    m.panel?.place(m.x, m.top)
    m.field?.place(m.x, m.top)
    m.pin?.place(m.pinX, m.pinY)
    m.card?.place(m.x, ph - m.bottom - m.card.height)
}
