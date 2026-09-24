package org.wpfy.carlauncher.design.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import org.wpfy.carlauncher.design.kit.*
import org.wpfy.carlauncher.design.theme.ControlScale
import org.wpfy.carlauncher.design.theme.LocalOverlayScaling
import org.wpfy.carlauncher.design.theme.TileScale
import kotlin.math.roundToInt

/**
 * The Landscape M map HUD (Pen "Cluster · Landscape M" frames), built from the kit composables only.
 * Rules (tile units are the tile scale's dp; the margin is the Pen's pane inset, in real dp):
 *  - margin on all four sides; no page dots: cards stack from the top-start corner, the now-playing
 *    chip sits top-end; Report is bottom-end.
 *  - the bar (ETA bar or status strip) hugs its content, centred on the pane along the bottom margin. Report
 *    stands on that row at the end margin; where the bar would reach it, Report stands one margin above the bar.
 *  - the map controls stand one margin above Report, in its column (so they move up with it); the sent pill
 *    one row gap above it, end-aligned.
 *  - the report sheet is the pane's inner width up to the Pen's sheet width (which the XL tier widens by its
 *    tile scale; the sheet itself is drawn at 1x); when narrower than the pane it is a corner
 *    sheet, end-aligned one row gap above Report, otherwise it spans the bottom of the pane.
 *  - cards are kitCardWidth wide, capped one row gap short of the controls column (and the margin).
 *  - the search field (when set, and nothing is guided) takes the card's place at the top-start corner, at the card's width.
 *  - the destination search panel (when set) takes the top-start corner, at the kit's field width but one row gap short of
 *    the controls column; the cards, chip, bar and sheets are left out while it is open, and so are Report and the map
 *    controls where the Pen frame leaves them out ([SearchPanelSpec.showControls]). The preview's field and card span the
 *    pane inside the margin, the card along the bottom.
 * Controls draw at the control scale and the report sheet at 1x, as in the Pen.
 */
@Composable
fun LandscapeMHud(
    state: KitHudState,
    actions: KitHudActions = KitHudActions(),
    modifier: Modifier = Modifier,
    /** Test-tag path of the pane in its screen; each element is tagged below it by its Pen name. */
    tag: String = "MapPane",
    /** The Pen's margin between the pane's edge and the HUD (real dp), and the scrim's colour. */
    margin: Dp = Margin,
    scrim: Color = ScrimColor,
    /** The Pen's measures of the destination search panel at this tier. */
    searchSpec: SearchPanelSpec = SearchPanelSpec.Medium,
) {
    val scales = LocalOverlayScaling.current?.scales
    val tile = scales?.tile ?: 1f
    val control = scales?.control ?: 1f
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val cardWidth = kitCardWidth(tile, fontScale)
    val chipText = PenKit.NowPlayingChip.Text.W * fontScale

    Layout(
        modifier = modifier.fillMaxSize(),
        content = {
            val searching = state.searchPanel != null
            SearchLayerContent(state.searchPanel, searchSpec, tag)
            if (!searching) state.nowPlaying?.let { np ->
                Box(Modifier.layoutId(MSlot.Chip).testTag("$tag/NowPlayingChip")) {
                    HudChip(np, actions, chipText)
                }
            }
            // Cards are measured against the cap in the layout, so they are composed at their kit width
            // and re-sized there: the kit takes the width as a parameter, so the cap is applied below.
            if (!searching) state.guidance?.let { g ->
                Box(Modifier.layoutId(MSlot.Card).testTag("$tag/TurnCard")) {
                    TileScale { KitTurnCard(g.distance, g.road, g.arrow, g.thenArrow, width = cardWidth, thenDescription = g.thenDescription) }
                }
            }
            if (!searching && state.search != null && state.guidance == null) {
                Box(Modifier.layoutId(MSlot.Search).testTag("$tag/MapSearch")) {
                    TileScale { KitMapSearch(state.search, cardWidth, onClick = actions.onSearch) }
                }
            }
            if (searching) Unit else if (state.eta != null) {
                Box(Modifier.layoutId(MSlot.Bar).testTag("$tag/ETABar")) {
                    TileScale { KitEtaBar(state.eta.arrival, state.eta.remaining, fill = true, onEnd = actions.onEnd, endDescription = state.eta.endDescription) }
                }
            } else if (state.status != null) {
                Box(Modifier.layoutId(MSlot.Bar).testTag("$tag/StatusStrip")) {
                    TileScale { KitStatusStrip(state.status.title, state.status.body, icon = state.status.icon, actionDescription = state.status.action.label, onSearch = actions.statusAction(state.status.action)) }
                }
            }
            if (state.showReport) Box(Modifier.layoutId(MSlot.Report).testTag("$tag/Report")) { ControlScale { KitReportButton(onClick = actions.onReport) } }
            if (state.reportSent == null && actions.mapControls.isNotEmpty()) {
                Box(Modifier.layoutId(MSlot.Controls).testTag("$tag/MapControls")) { ControlScale { KitMapControls(actions.mapControls, minTouch = actions.minTouch) } }
            }
            if (!searching) state.reportSent?.let { sent ->
                Box(Modifier.layoutId(MSlot.Sent).testTag("$tag/ReportSentPill")) {
                    ControlScale { KitReportSentPill(sent.title, sent.note, sent.seconds, textScale = tile / control, onUndo = actions.onUndoReport) }
                }
            }
            if (!searching) state.reportSheet?.let { sheet ->
                Box(Modifier.layoutId(MSlot.Scrim).testTag("$tag/Scrim").fillMaxSize().background(scrim))
                Box(Modifier.layoutId(MSlot.Sheet).testTag("$tag/ReportSheet")) {
                    KitReportSheet(
                        sheet.title, sheet.subtitle, sheet.choices, columns = 2,
                        modifier = Modifier.fillMaxWidth(), onClose = actions.onCloseReport,
                    )
                }
            }
        },
    ) { measurables, constraints ->
        val u = 1.dp.toPx() * tile
        fun t(v: Float) = (v * u).roundToInt()
        val pw = constraints.maxWidth
        val ph = constraints.maxHeight
        val loose = Constraints(maxWidth = pw, maxHeight = ph)
        val m = margin.roundToPx()
        val rowGap = t(RowGap)
        val byId = measurables.associateBy { it.layoutId }
        val chip = byId[MSlot.Chip]?.measure(loose)
        val report = byId[MSlot.Report]?.measure(loose)
        val controls = byId[MSlot.Controls]?.measure(loose)
        val sent = byId[MSlot.Sent]?.measure(loose.copy(maxWidth = pw - m * 2))
        val reportX = pw - m - (report?.width ?: 0)
        // Cards stop one row gap short of the controls column (Report's column) and keep the margin.
        val cap = (reportX - rowGap - m).coerceAtLeast(0)
        val card = byId[MSlot.Card]?.let { it.measure(Constraints(maxWidth = minOf(it.maxIntrinsicWidth(ph), cap), maxHeight = ph)) }
        val search = byId[MSlot.Search]?.let { it.measure(Constraints(maxWidth = minOf(it.maxIntrinsicWidth(ph), cap), maxHeight = ph)) }

        // The bar hugs its content (within the pane's inner width), centred on the pane along the bottom margin.
        val barMeasurable = byId[MSlot.Bar]
        val barWidth = minOf(barMeasurable?.maxIntrinsicWidth(ph) ?: 0, pw - m * 2)
        val bar = barMeasurable?.measure(Constraints.fixedWidth(barWidth).copy(maxHeight = ph))
        val barX = (pw - barWidth) / 2
        val barTop = ph - m - (bar?.height ?: 0)
        // Report stands on the bar's row; where the centred bar would reach it, one margin above the bar.
        val reachesReport = bar != null && report != null && barX + barWidth > reportX
        val reportY = (if (reachesReport) barTop - m else ph - m) - (report?.height ?: 0)
        val scrim = byId[MSlot.Scrim]?.measure(Constraints.fixed(pw, ph))
        // The sheet is as wide as the pane's inner width allows, up to the Pen's sheet width: narrower than the
        // pane it is a corner sheet above Report, otherwise the bottom sheet across the pane.
        val sheetWidth = minOf(pw - m * 2, (SheetMaxWidth * maxOf(1f, tile)).roundToPx())
        val corner = sheetWidth < pw - m * 2
        val sheet = byId[MSlot.Sheet]?.measure(Constraints(minWidth = sheetWidth, maxWidth = sheetWidth, maxHeight = ph - m * 2))
        // The controls' discs share the Report button's column: centred on it, or on where it would stand, so a wider touch box does not leave the pane.
        val discWidth = (PenKit.MapControls.ControlVolumeUp.W * control).roundToPx()
        val columnCentre = if (report != null) reportX + report.width / 2 else reportX - discWidth / 2
        fun controlsX(width: Int) = columnCentre - width / 2
        // The search panel: the kit's field width, one row gap short of the controls column where that is on screen.
        val natural = searchSpec.width.roundToPx()
        val clears = searchSpec.controls == SearchControls.Clear && report != null
        val searchLayer = measureSearch(byId, state.searchPanel, pw, ph, m, m, minOf(natural, if (clears) cap else (pw - m * 2).coerceAtLeast(0)))
        // Medium: the panel keeps its width and covers the Report button and the controls where it reaches them; the preview does too.
        val covered = state.searchPanel != null && (
            state.searchPanel is KitSearchPreview ||
                (searchSpec.controls == SearchControls.HideWhenReached && m + natural > (if (report != null) reportX else controlsX(controls?.width ?: 0)))
            )
        layout(pw, ph) {
            chip?.place(pw - m - chip.width, m)
            card?.place(m, m)
            search?.place(m, m)
            bar?.place(barX, barTop)
            if (!covered) report?.place(reportX, reportY)
            if (!covered) controls?.place(controlsX(controls.width), reportY - m - controls.height)
            sent?.place(pw - m - sent.width, reportY - rowGap - sent.height)
            scrim?.place(0, 0)
            placeSearch(searchLayer, ph)
            if (sheet != null) {
                if (corner) sheet.place(pw - m - sheet.width, reportY - rowGap - sheet.height)
                else sheet.place(m, ph - m - sheet.height)
            }
        }
    }
}

private enum class MSlot { Chip, Card, Search, Bar, Report, Controls, Sent, Scrim, Sheet }

// The pane inset of the Pen frames (real dp), and the tile-unit gaps of the Pen canvas.
private val Margin = PenScreens.LandscapeM.GuideMedia.MapPane.Y
private val SheetMaxWidth = PenScreens.UltrawideM.ReportSheet.MapPane.ReportSheet.W
private const val RowGap = 16f
private val ScrimColor = PenScreens.LandscapeM.ReportSheet.MapPane.Scrim.Fill
