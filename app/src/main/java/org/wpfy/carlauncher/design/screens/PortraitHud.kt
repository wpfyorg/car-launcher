package org.wpfy.carlauncher.design.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import org.wpfy.carlauncher.design.kit.*
import org.wpfy.carlauncher.design.theme.ControlScale
import org.wpfy.carlauncher.design.theme.LocalOverlayScaling
import org.wpfy.carlauncher.design.theme.TileScale
import org.wpfy.carlauncher.design.kit.PenScreens.PortraitL as PL
import org.wpfy.carlauncher.design.kit.PenScreens.PortraitS as PS

/**
 * What a portrait group's HUD takes from its Pen frames (real dp): the margin around the pane's
 * content, the gaps between its rows, where the first card starts, and the report sheet's columns.
 * [pageDots]: the group has a media page, so the pane shows the page dots in a band above the cards.
 */
@Immutable
class PortraitHudSpec internal constructor(
    val margin: Dp,
    val rowGap: Dp,
    val stackGap: Dp,
    val controlsGap: Dp,
    val top: Dp,
    val sheetColumns: Int,
    val pageDots: Boolean,
    val scrim: Color,
    /** The Pen's measures of the destination search panel at this group's tier. */
    val search: SearchPanelSpec,
) {
    companion object {
        /** Portrait S: the margin is the cluster's inset, the gaps are what the Pen leaves between the stacked nodes. */
        val PortraitS = PortraitHudSpec(
            margin = PS.Guide.MapPane.TurnCard.X,
            rowGap = PS.Guide.MapPane.ETABar.Y - (PS.Guide.MapPane.Report.Y + PS.Guide.MapPane.Report.H),
            stackGap = PS.Guide.MapPane.ETABar.Y - (PS.Guide.MapPane.Report.Y + PS.Guide.MapPane.Report.H),
            controlsGap = PS.Idle.MapPane.Report.Y - (PS.Idle.MapPane.MapControls.Y + PS.Idle.MapPane.MapControls.H),
            top = PS.Guide.MapPane.TurnCard.Y,
            sheetColumns = 2,
            pageDots = true,
            scrim = PS.ReportSheet.MapPane.Scrim.Fill,
            search = SearchPanelSpec.Small,
        )
        val PortraitL = PortraitHudSpec(
            margin = PL.GuideMap.MapPane.TurnCard.X,
            rowGap = PL.GuideMap.MapPane.ETABar.Y - (PL.GuideMap.MapPane.Report.Y + PL.GuideMap.MapPane.Report.H),
            stackGap = PL.GuideMap.MapPane.ETABar.Y - (PL.GuideMap.MapPane.Report.Y + PL.GuideMap.MapPane.Report.H),
            controlsGap = PL.Idle.MapPane.Report.Y - (PL.Idle.MapPane.MapControls.Y + PL.Idle.MapPane.MapControls.H),
            top = PL.GuideMap.MapPane.TurnCard.Y,
            sheetColumns = 4,
            pageDots = false,
            scrim = PL.ReportSheet.MapPane.Scrim.Fill,
            search = SearchPanelSpec.Large,
        )
    }
}

/**
 * The portrait map HUD (Pen "Portrait S" and "Portrait L" frames), built from the kit composables only.
 * Rules (the spec's measures are real dp, the kit's sizes are tile units):
 *  - a margin on all four sides; the now-playing chip sits top-end, level with the first card, which
 *    starts at the spec's top (under a band of page dots where the group has them).
 *  - cards are kitCardWidth wide at the top-start corner; when a card and the chip no longer fit side
 *    by side (large text), the cards drop under the chip and span the pane.
 *  - the bar (ETA bar or status strip) spans the pane along the bottom; Report
 *    stands one row gap above it, at the end; the map controls one controls gap above Report;
 *    the sent pill one row gap above Report, end-aligned.
 *  - the report sheet spans the bottom of the pane (inside the margin) over a scrim.
 * Controls draw at the control scale and the report sheet at 1x, as in the Pen.
 */
@Composable
fun PortraitHud(
    state: KitHudState,
    spec: PortraitHudSpec,
    actions: KitHudActions = KitHudActions(),
    modifier: Modifier = Modifier,
    /** Test-tag path of the pane in its screen; each element is tagged below it by its Pen name. */
    tag: String = "MapPane",
) {
    val scales = LocalOverlayScaling.current?.scales
    val tile = scales?.tile ?: 1f
    val control = scales?.control ?: 1f
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val cardWidth = kitCardWidth(tile, fontScale)
    val chipText = PenKit.NowPlayingChip.Text.W * fontScale

    BoxWithConstraints(modifier.fillMaxSize()) {
        // Cards go beside the chip when both fit between the margins (a row gap apart), else under it.
        val chipWidth = (PenKit.NowPlayingChip.W + (chipText - PenKit.NowPlayingChip.Text.W)) * tile
        val used = spec.margin + cardWidth * tile + spec.rowGap + chipWidth
        // Within one device pixel: the Pen's sums are exact where the frame is tight.
        val beside = state.nowPlaying == null || with(LocalDensity.current) { used.toPx() <= (maxWidth - spec.margin).toPx() + 1f }
        val stackedWidth = (maxWidth - spec.margin * 2) / tile
        val width = if (beside) cardWidth else stackedWidth

        Layout(
            modifier = Modifier.fillMaxSize(),
            content = {
                val searching = state.searchPanel != null
                SearchLayerContent(state.searchPanel, spec.search, tag)
                if (spec.pageDots && state.showDots && !searching) {
                    Box(Modifier.layoutId(PSlot.Dots).testTag("$tag/PageDots")) { HudDots(state, actions) }
                }
                if (!state.mediaPage) {
                    if (!searching) state.nowPlaying?.let { np ->
                        Box(Modifier.layoutId(PSlot.Chip).testTag("$tag/NowPlayingChip")) {
                            HudChip(np, actions, chipText)
                        }
                    }
                    if (!searching) state.guidance?.let { g ->
                        Box(Modifier.layoutId(PSlot.Card).testTag("$tag/TurnCard")) {
                            TileScale { KitTurnCard(g.distance, g.road, g.arrow, g.thenArrow, width = width, thenDescription = g.thenDescription) }
                        }
                    }
                    // Nothing guided: the map's search field takes the turn card's place.
                    if (!searching && state.search != null && state.guidance == null) {
                        Box(Modifier.layoutId(PSlot.Card).testTag("$tag/MapSearch")) {
                            TileScale { KitMapSearch(state.search, width, onClick = actions.onSearch) }
                        }
                    }
                    if (searching) Unit else if (state.eta != null) {
                        Box(Modifier.layoutId(PSlot.Bar).testTag("$tag/ETABar")) {
                            TileScale { KitEtaBar(state.eta.arrival, state.eta.remaining, fill = true, onEnd = actions.onEnd, endDescription = state.eta.endDescription) }
                        }
                    } else if (state.status != null) {
                        Box(Modifier.layoutId(PSlot.Bar).testTag("$tag/StatusStrip")) {
                            TileScale { KitStatusStrip(state.status.title, state.status.body, icon = state.status.icon, fill = true, actionDescription = state.status.action.label, onSearch = actions.statusAction(state.status.action)) }
                        }
                    }
                    if (state.showReport) Box(Modifier.layoutId(PSlot.Report).testTag("$tag/Report")) { ControlScale { KitReportButton(onClick = actions.onReport) } }
                    // The sent pill takes the end column: the map controls step aside while it is up.
                    if (state.reportSent == null && actions.mapControls.isNotEmpty()) {
                        Box(Modifier.layoutId(PSlot.Controls).testTag("$tag/MapControls")) { ControlScale { KitMapControls(actions.mapControls, minTouch = actions.minTouch) } }
                    }
                    if (!searching) state.reportSent?.let { sent ->
                        Box(Modifier.layoutId(PSlot.Sent).testTag("$tag/ReportSentPill")) {
                            ControlScale { KitReportSentPill(sent.title, sent.note, sent.seconds, textScale = tile / control, onUndo = actions.onUndoReport) }
                        }
                    }
                    if (!searching) state.reportSheet?.let { sheet ->
                        Box(Modifier.layoutId(PSlot.Scrim).testTag("$tag/Scrim").fillMaxSize().background(spec.scrim))
                        Box(Modifier.layoutId(PSlot.Sheet).testTag("$tag/ReportSheet")) {
                            KitReportSheet(
                                sheet.title, sheet.subtitle, sheet.choices, columns = spec.sheetColumns,
                                modifier = Modifier.fillMaxWidth(), onClose = actions.onCloseReport,
                            )
                        }
                    }
                } else {
                    state.nextTurn?.let { g ->
                        Box(Modifier.layoutId(PSlot.Chip).testTag("$tag/NextTurnChip")) { HudNextTurn(g, state, actions) }
                    }
                }
            },
        ) { measurables, constraints ->
            val pw = constraints.maxWidth
            val ph = constraints.maxHeight
            val loose = Constraints(maxWidth = pw, maxHeight = ph)
            val m = spec.margin.roundToPx()
            val rowGap = spec.rowGap.roundToPx()
            val stackGap = spec.stackGap.roundToPx()
            val controlsGap = spec.controlsGap.roundToPx()
            val top = spec.top.roundToPx()
            val inner = pw - m * 2
            val byId = measurables.associateBy { it.layoutId }
            val dots = byId[PSlot.Dots]?.measure(loose)
            val chip = byId[PSlot.Chip]?.measure(loose)
            val card = byId[PSlot.Card]?.measure(Constraints(maxWidth = inner, maxHeight = ph))
            val report = byId[PSlot.Report]?.measure(loose)
            val controls = byId[PSlot.Controls]?.measure(loose)
            val sent = byId[PSlot.Sent]?.measure(loose.copy(maxWidth = inner))
            val bar = byId[PSlot.Bar]?.measure(Constraints.fixedWidth(inner).copy(maxHeight = ph))
            val scrim = byId[PSlot.Scrim]?.measure(Constraints.fixed(pw, ph))
            val sheet = byId[PSlot.Sheet]?.measure(Constraints(minWidth = inner, maxWidth = inner, maxHeight = ph - m * 2))
            // Without a bar (nothing guided) Report keeps the place it has above one.
            val barTop = ph - m - (bar?.height ?: (PenKit.EtaBar.H * tile).roundToPx())
            val rowBottom = barTop - rowGap
            val reportX = pw - m - (report?.width ?: 0)
            val reportY = rowBottom - (report?.height ?: 0)
            val cardsTop = if (beside || chip == null) top else top + chip.height + stackGap
        // The controls' discs share the Report button's column: centred on it, or on where it would stand, so a wider touch box does not leave the pane.
        val discWidth = (PenKit.MapControls.ControlVolumeUp.W * control).roundToPx()
        val columnCentre = if (report != null) reportX + report.width / 2 else reportX - discWidth / 2
        fun controlsX(width: Int) = columnCentre - width / 2
            // The search panel stands at the margin (the page dots band is not drawn while it is open), at the kit's field width.
            val searchLayer = measureSearch(byId, state.searchPanel, pw, ph, m, m, minOf(spec.search.width.roundToPx(), inner.coerceAtLeast(0)))
            layout(pw, ph) {
                dots?.place(pw - m - dots.width, m)
                chip?.place(if (dots != null && state.mediaPage) pw - m - dots.width - m - chip.width else pw - m - chip.width, top)
                card?.place(m, cardsTop)
                bar?.place(m, barTop)
                report?.place(reportX, reportY)
                controls?.place(controlsX(controls.width), reportY - controlsGap - controls.height)
                sent?.place(pw - m - sent.width, reportY - rowGap - sent.height)
                scrim?.place(0, 0)
                sheet?.place(m, ph - m - sheet.height)
                placeSearch(searchLayer, ph)
            }
        }
    }
}

private enum class PSlot { Dots, Chip, Card, Bar, Report, Controls, Sent, Scrim, Sheet }
