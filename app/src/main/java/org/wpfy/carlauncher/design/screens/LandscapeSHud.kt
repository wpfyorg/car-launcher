package org.wpfy.carlauncher.design.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.wpfy.carlauncher.design.kit.*
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeS.Guide.MapPane as GuideFrame
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeS.GuideLargeText.MapPane as LargeFrame
import org.wpfy.carlauncher.design.theme.ControlScale
import org.wpfy.carlauncher.design.theme.LocalOverlayScaling
import org.wpfy.carlauncher.design.theme.TileScale
import kotlin.math.roundToInt

/** What the Landscape S HUD shows; null parts are not drawn. Strings are display-ready. */
@Immutable
data class KitHudState(
    val guidance: Guidance? = null,
    val speed: Speed,
    val eta: Eta? = null,
    val status: Status? = null,
    val nowPlaying: NowPlaying? = null,
    val mediaPage: Boolean = false,
    val nextTurn: Guidance? = null,
    val reportSheet: ReportSheet? = null,
    val reportSent: ReportSent? = null,
    /** The map's search field, when nothing is being guided (the Landscape M HUD beside the Drive Card); its placeholder. */
    val search: String? = null,
    /** The destination search over the map (Pen "Search" frames): the HUD leaves out what it would cover. */
    val searchPanel: KitSearchState? = null,
    /** Parts a host that draws its own controls leaves out, and the page dots where the media page does not exist. */
    val showReport: Boolean = true,
    val showDots: Boolean = true,
    /** What the next-turn chip and the page dots announce when they are tappable. */
    val nextTurnDescription: String? = null,
    val dotsDescription: String? = null,
) {
    @Immutable data class Guidance(val distance: String, val road: String, val arrow: ImageVector, val thenArrow: ImageVector? = null, val thenDescription: String? = null)
    @Immutable data class Alert(
        val title: String, val detail: String, val icon: ImageVector,
        val muteDescription: String = "Mute camera alerts", val description: String? = null,
    )
    @Immutable data class Speed(val value: String, val unit: String, val limit: String?, val description: String? = null, val withinLimit: Boolean = false)
    @Immutable data class Eta(val arrival: String, val remaining: String, val endDescription: String? = null)
    @Immutable data class Status(val title: String, val body: String, val icon: ImageVector = PenIcons.Navigation, val action: KitStatusAction = KitStatusAction.Search)
    @Immutable data class NowPlaying(val title: String, val artist: String, val art: ImageBitmap?, val artFallback: Color, val playing: Boolean, val canPlayPause: Boolean = true, val description: String? = null)
    @Immutable data class ReportSheet(val title: String, val subtitle: String, val choices: List<KitReportChoice>)
    @Immutable data class ReportSent(val title: String, val note: String?, val seconds: Int)
}

/** What the status strip's button does, with the label it announces; the live HUD's four actions. */
enum class KitStatusAction(val label: String) {
    Search("Search destination"),
    AppSettings("Open app settings"),
    Retry("Try again"),
    NavigationSettings("Navigation settings"),
}

/** Taps from the HUD. */
class KitHudActions(
    val onMuteAlert: () -> Unit = {},
    val onEnd: () -> Unit = {},
    val onSearch: () -> Unit = {},
    val onPlayPause: () -> Unit = {},
    val onReport: () -> Unit = {},
    val onCloseReport: () -> Unit = {},
    val onUndoReport: () -> Unit = {},
    val mapControls: List<KitMapControl> = emptyList(),
    val onOpenAppSettings: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onNavigationSettings: () -> Unit = {},
    /** A tap on the now-playing chip, the next-turn chip and the page dots; null leaves them inert, as in the Pen. */
    val onChipClick: (() -> Unit)? = null,
    val onNextTurnClick: (() -> Unit)? = null,
    val onDotsClick: (() -> Unit)? = null,
    /** Extra modifiers of the chip and the dots (the paged screens drag the page from them). */
    val chipModifier: Modifier = Modifier,
    val dotsModifier: Modifier = Modifier,
    /** The least touch area (real dp) of a map control; 0 keeps the Pen's column. */
    val minTouch: Dp = 0.dp,
) {
    /** The callback of a status strip's button. */
    fun statusAction(action: KitStatusAction): () -> Unit = when (action) {
        KitStatusAction.Search -> onSearch
        KitStatusAction.AppSettings -> onOpenAppSettings
        KitStatusAction.Retry -> onRetry
        KitStatusAction.NavigationSettings -> onNavigationSettings
    }
}

/**
 * The Landscape S map HUD (Pen "v2 · Compact s533" frames), built from the kit composables only.
 * The pane is laid out in tile units (the Pen canvas: 2 canvas px per dp, tiles at 0.5): a 24 margin.
 * Guided, the turn card stands at the top-start corner and Report at the top-end corner, the bar centred
 * on the pane along the bottom; the map controls stand in the end column above where Report stands
 * unguided (bottom-end), which is also where the sent pill sits. Unguided, Report is at the bottom-end
 * and the search field is under the page-dots band. The turn card is the Pen's width, growing with the font
 * scale to what the Report corner leaves (the Pen's gap).
 * Controls draw at the control scale and the report sheet at 1x, as in the Pen.
 */
@Composable
fun LandscapeSHud(
    state: KitHudState,
    actions: KitHudActions = KitHudActions(),
    modifier: Modifier = Modifier,
    /** Test-tag path of the pane in its screen; each element is tagged below it by its Pen name. */
    tag: String = "MapPane",
) {
    val scales = LocalOverlayScaling.current?.scales
    val tile = scales?.tile ?: 1f
    val control = scales?.control ?: 1f
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    // The cards keep the kit's proportions under the text floor and the user's font scale.
    val cardWidth = kitCardWidth(tile, fontScale)
    val chipText = PenKit.NowPlayingChip.Text.W * fontScale

    BoxWithConstraints(modifier.fillMaxSize()) {
    // The guided turn card: the Pen's width at 1x, growing with the font scale (Compose's non-linear scaling eased from 1.0) to what the Report corner leaves.
    val margin = GuideFrame.TurnCard.X
    val largeGap = LargeFrame.Report.X - (LargeFrame.TurnCard.X + LargeFrame.TurnCard.W)
    val largeWidth = maxWidth - margin - GuideFrame.Report.W - largeGap - margin
    val growth = ((fontScale - 1f) / 0.3f).coerceIn(0f, 1f)
    val turnWidth = (GuideFrame.TurnCard.W + (largeWidth - GuideFrame.TurnCard.W) * growth) / tile

    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            val searching = state.searchPanel != null
            SearchLayerContent(state.searchPanel, SearchPanelSpec.Small, tag)
            if (state.showDots && !searching) Box(Modifier.layoutId(Slot.Dots).testTag("$tag/PageDots")) { HudDots(state, actions) }
            if (!state.mediaPage) {
                if (!searching) state.nowPlaying?.let { np ->
                    Box(Modifier.layoutId(Slot.Chip).testTag("$tag/NowPlayingChip")) {
                        HudChip(np, actions, chipText)
                    }
                }
                if (!searching) state.guidance?.let { g ->
                    Box(Modifier.layoutId(Slot.Card).testTag("$tag/TurnCard")) {
                        TileScale { KitTurnCard(g.distance, g.road, g.arrow, g.thenArrow, width = turnWidth, thenDescription = g.thenDescription, roadLines = 1) }
                    }
                }
                if (!searching && state.search != null && state.guidance == null) {
                    Box(Modifier.layoutId(Slot.Card).testTag("$tag/MapSearch")) {
                        TileScale { KitMapSearch(state.search, cardWidth, onClick = actions.onSearch) }
                    }
                }
                if (searching) Unit else if (state.eta != null) {
                    Box(Modifier.layoutId(Slot.Bar).testTag("$tag/ETABar")) {
                        TileScale { KitEtaBar(state.eta.arrival, state.eta.remaining, onEnd = actions.onEnd, endDescription = state.eta.endDescription) }
                    }
                } else if (state.status != null) {
                    Box(Modifier.layoutId(Slot.Bar).testTag("$tag/StatusStrip")) {
                        TileScale { KitStatusStrip(state.status.title, state.status.body, icon = state.status.icon, actionDescription = state.status.action.label, onSearch = actions.statusAction(state.status.action)) }
                    }
                }
                if (state.showReport) Box(Modifier.layoutId(Slot.Report).testTag("$tag/Report")) {
                    ControlScale { KitReportButton(onClick = actions.onReport) }
                }
                // The sent pill takes the end column: the map controls step aside while it is up.
                if (state.reportSent == null && actions.mapControls.isNotEmpty()) {
                    Box(Modifier.layoutId(Slot.Controls).testTag("$tag/MapControls")) { ControlScale { KitMapControls(actions.mapControls, minTouch = actions.minTouch) } }
                }
                if (!searching) state.reportSent?.let { sent ->
                    Box(Modifier.layoutId(Slot.Sent).testTag("$tag/ReportSentPill")) {
                        ControlScale {
                            KitReportSentPill(
                                sent.title, sent.note, sent.seconds,
                                textScale = tile / control, onUndo = actions.onUndoReport,
                            )
                        }
                    }
                }
                // The sheet itself is the screen's, not the pane's (see [LandscapeSReportSheet]); the scrim stays pane-sized.
                if (!searching) state.reportSheet?.let {
                    Box(Modifier.layoutId(Slot.Scrim).testTag("$tag/Scrim").fillMaxSize().background(ScrimColor))
                }
            } else {
                state.nextTurn?.let { g ->
                    Box(Modifier.layoutId(Slot.Chip).testTag("$tag/NextTurnChip")) { HudNextTurn(g, state, actions) }
                }
            }
        },
    ) { measurables, constraints ->
        // Tile units (the Pen canvas) to px; every rule below is a Pen frame measure in tile units.
        val u = 1.dp.toPx() * tile
        fun t(v: Float) = (v * u).roundToInt()
        val pw = constraints.maxWidth
        val ph = constraints.maxHeight
        val loose = Constraints(maxWidth = pw, maxHeight = ph)
        val m = t(Margin)
        val top = m + t(DotsBand)
        val byId = measurables.associateBy { it.layoutId }
        val dots = byId[Slot.Dots]?.measure(loose)
        val chip = byId[Slot.Chip]?.measure(loose)
        val card = byId[Slot.Card]?.measure(loose)
        val report = byId[Slot.Report]?.measure(loose)
        val controls = byId[Slot.Controls]?.measure(loose)
        val sent = byId[Slot.Sent]?.measure(loose.copy(maxWidth = pw - m * 2))
        val reportX = pw - m - (report?.width ?: 0)
        // Where Report stands unguided; the controls and the sent pill are anchored to it either way.
        val reportY = ph - m - (report?.height ?: 0)
        val bar = byId[Slot.Bar]?.measure(Constraints(maxWidth = (pw - m * 2).coerceAtLeast(0), maxHeight = ph))
        val scrim = byId[Slot.Scrim]?.measure(Constraints.fixed(pw, ph))
        // The controls' discs share the Report button's column: centred on it, or on where it would stand, so a wider touch box does not leave the pane.
        val discWidth = (PenKit.MapControls.ControlVolumeUp.W * control).roundToPx()
        val columnCentre = if (report != null) reportX + report.width / 2 else reportX - discWidth / 2
        fun controlsX(width: Int) = columnCentre - width / 2
        // The search panel stands where the map search tab does (under the page dots band), at the kit's field width.
        val searchLayer = measureSearch(byId, state.searchPanel, pw, ph, m, top, minOf(SearchPanelSpec.Small.width.roundToPx(), (pw - m * 2).coerceAtLeast(0)))
        layout(pw, ph) {
            dots?.place(pw - m - dots.width, m)
            chip?.place(pw - m - chip.width, top)
            card?.place(m, if (state.guidance != null) m else top)
            bar?.place((pw - bar.width) / 2, ph - m - bar.height)
            report?.place(reportX, if (state.guidance != null) m else reportY)
            controls?.place(controlsX(controls.width), reportY - t(ControlsGap) - controls.height)
            sent?.place(pw - m - sent.width, reportY - t(RowGap) - sent.height)
            scrim?.place(0, 0)
            placeSearch(searchLayer, ph)
        }
    }
    }
}

/**
 * The Landscape S report sheet (Pen: a direct child of the screen frame): its natural width, drawn by the caller
 * centred on the screen with its bottom [LandscapeSReportSheetBottomGap] above the screen's bottom edge, over the dock edge.
 * Draws nothing when [state] has no sheet. Tag: "<tag>/ReportSheet".
 */
@Composable
fun LandscapeSReportSheet(state: KitHudState, actions: KitHudActions, tag: String = "Screen", modifier: Modifier = Modifier) {
    val sheet = state.reportSheet ?: return
    KitReportSheet(
        sheet.title, sheet.subtitle, sheet.choices, columns = 2,
        modifier = modifier.width(LandscapeSReportSheetWidth).testTag("$tag/ReportSheet"), onClose = actions.onCloseReport,
    )
}

/** The sheet's natural width and the gap under it, from the Pen's Landscape S ReportSheet frame. */
val LandscapeSReportSheetWidth = PenScreens.LandscapeS.ReportSheet.ReportSheet.W
val LandscapeSReportSheetBottomGap =
    PenScreens.LandscapeS.ReportSheet.H - (PenScreens.LandscapeS.ReportSheet.ReportSheet.Y + PenScreens.LandscapeS.ReportSheet.ReportSheet.H)

private enum class Slot { Dots, Chip, Card, Bar, Report, Controls, Sent, Scrim }

// Pen "v2 · Compact s533" frame measures, in tile units (Pen canvas px).
private const val Margin = 24f
private const val DotsBand = 60f
private const val RowGap = 16f
private const val ControlsGap = 24f
private val ScrimColor = PenScreens.LandscapeS.ReportSheet.MapPane.Scrim.Fill
