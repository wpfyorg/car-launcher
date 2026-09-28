package org.wpfy.carlauncher.design.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.rememberLazyListState
import org.wpfy.carlauncher.design.kit.KitDockActions
import org.wpfy.carlauncher.design.kit.KitDriveScale
import org.wpfy.carlauncher.design.kit.KitStateMessage
import org.wpfy.carlauncher.design.kit.KitNotificationsHint
import org.wpfy.carlauncher.design.kit.KitTripRow
import org.wpfy.carlauncher.design.kit.KitTripRoute
import org.wpfy.carlauncher.design.kit.KitTripSummaryRow
import org.wpfy.carlauncher.design.kit.KitTripsSummary
import org.wpfy.carlauncher.design.kit.KitTripsTotals
import org.wpfy.carlauncher.design.kit.PenIcons
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeM.AllTrips as AllTripsKit
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeM.AllTrips.Pane.Header.RangeTabs.All as AllRangeKit
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeM.AllTrips.Pane.Header.RangeTabs.Month as MonthRangeKit
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeM.AllTrips.Pane.Header.RangeTabs.Week as WeekRangeKit
import org.wpfy.carlauncher.design.kit.ScreenGroup
import org.wpfy.carlauncher.design.kit.PenText
import org.wpfy.carlauncher.design.kit.penShape
import org.wpfy.carlauncher.design.theme.OverlayTier

enum class TripHistoryRange { Week, Month, All }
enum class TripHistoryStatus { Ready, Loading, Empty, Error, Partial }

@Immutable
data class TripHistoryItem(val id: String, val row: KitTripSummaryRow, val referenceTag: String? = null)
@Immutable
data class TripHistoryGroup(val label: String, val trips: List<TripHistoryItem>)

/** Caller-owned filtered rows, totals, status, and event callbacks for the All Trips screen. */
@Immutable
data class TripHistoryScreenState(
    val title: String,
    val subtitle: String,
    val range: TripHistoryRange,
    val weekLabel: String,
    val monthLabel: String,
    val allLabel: String,
    val summary: KitTripsTotals,
    val groups: List<TripHistoryGroup> = emptyList(),
    val status: TripHistoryStatus = TripHistoryStatus.Ready,
    val loadingTitle: String = "Loading trips",
    val loadingMessage: String = "",
    val emptyTitle: String = "No trips yet",
    val emptyMessage: String = "Trips you save will appear here.",
    val errorTitle: String = "Trips unavailable",
    val errorMessage: String = "Try again later.",
    val partialTitle: String = "Some trips unavailable",
    val partialMessage: String = "Showing trips that could be read.",
    val retryLabel: String? = null,
    val backDescription: String = "Back",
    val currentLabel: String = "Current",
)

@Composable
fun TripHistoryScreen(
    state: TripHistoryScreenState,
    modifier: Modifier = Modifier,
    dock: KitDockState = KitDockState("", 0),
    dockActions: KitDockActions = KitDockActions(),
    listState: LazyListState? = null,
    group: ScreenGroup = ScreenGroup.LandscapeM,
    tag: String = "Screen",
    showDock: Boolean = true,
    onBack: () -> Unit = {},
    onRangeSelected: (TripHistoryRange) -> Unit = {},
    onTripSelected: (String) -> Unit = {},
    thumbnailRoute: @Composable (TripHistoryItem) -> KitTripRoute = { it.row.route },
    onRetry: () -> Unit = {},
) {
    val screen = DriveScreenSpec(
        group, SystemScreenSpec.forGroup(group),
        AllTripsKit.Pane.PadStart, AllTripsKit.Pane.PadTop, AllTripsKit.Pane.PadEnd, AllTripsKit.Pane.PadBottom, AllTripsKit.Pane.Gap, AllTripsKit.Pane.Header.Gap, AllTripsKit.Pane.Header.Title.Gap,
        AllTripsKit.Pane.Header.Title.Title.Style, AllTripsKit.Pane.Header.Title.Title.Fill,
        AllTripsKit.Pane.Header.Title.Subtitle.Style, AllTripsKit.Pane.Header.Title.Subtitle.Fill, 0.dp,
    )
    val actualListState = listState ?: rememberLazyListState()
    val minSingleRowHeaderWidth = AllTripsKit.Pane.Header.Back.W + AllTripsKit.Pane.Header.Gap * 2 +
        maxOf(AllTripsKit.Pane.Header.Title.Title.W, AllTripsKit.Pane.Header.Title.Subtitle.W) +
        AllTripsKit.Pane.Header.RangeTabs.W
    DriveFrame(screen, dock, dockActions, tag, modifier, showDock) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(AllTripsKit.Pane.Gap)) {
            BoxWithConstraints(Modifier.fillMaxWidth().testTag("$tag/Pane/Header")) {
                if (maxWidth >= minSingleRowHeaderWidth) Row(
                Modifier.fillMaxWidth().heightIn(min = AllTripsKit.Pane.Header.H),
                horizontalArrangement = Arrangement.spacedBy(AllTripsKit.Pane.Header.Gap), verticalAlignment = Alignment.CenterVertically,
            ) {
                BackButton(state.backDescription, onBack, tag)
                DriveTitle(screen, state.title, state.subtitle, "$tag/Pane/Header/Title", Modifier.weight(1f))
                TripRangeTabs(state, "$tag/Pane/Header/RangeTabs", onRangeSelected)
                } else Column(verticalArrangement = Arrangement.spacedBy(AllTripsKit.Pane.Gap)) {
                    Row(Modifier.fillMaxWidth().heightIn(min = AllTripsKit.Pane.Header.H), horizontalArrangement = Arrangement.spacedBy(AllTripsKit.Pane.Header.Gap), verticalAlignment = Alignment.CenterVertically) {
                        BackButton(state.backDescription, onBack, tag)
                        DriveTitle(screen, state.title, state.subtitle, "$tag/Pane/Header/Title", Modifier.weight(1f))
                    }
                    TripRangeTabs(state, "$tag/Pane/Header/RangeTabs", onRangeSelected)
                }
            }
            Box(Modifier.fillMaxWidth().height(AllTripsKit.Pane.Summary.H).testTag("$tag/Pane/Summary")) {
                KitDriveScale(OverlayTier.Medium) {
                    KitTripsSummary(state.summary, Modifier.fillMaxSize(), width = AllTripsKit.Pane.Summary.W / org.wpfy.carlauncher.design.kit.kitDriveScaleFor(OverlayTier.Medium))
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f).testTag("$tag/Pane/List")) {
                when {
                    state.status == TripHistoryStatus.Loading -> KitStateMessage(state.loadingTitle, state.loadingMessage, Modifier.fillMaxSize(), tag = "$tag/Pane/List/State", loading = true)
                    state.status == TripHistoryStatus.Empty || (state.status == TripHistoryStatus.Ready && state.groups.none { it.trips.isNotEmpty() }) ->
                        KitStateMessage(state.emptyTitle, state.emptyMessage, Modifier.fillMaxSize(), tag = "$tag/Pane/List/State")
                    state.status == TripHistoryStatus.Error -> {
                        KitStateMessage(
                            state.errorTitle, state.errorMessage,
                            Modifier.fillMaxSize(), tag = "$tag/Pane/List/State",
                            actionLabel = state.retryLabel, onAction = onRetry,
                        )
                    }
                    state.status == TripHistoryStatus.Partial -> Column(Modifier.fillMaxSize()) {
                        KitNotificationsHint(state.partialMessage, Modifier.fillMaxWidth(), tag = "$tag/Pane/List/PartialHint")
                        TripHistoryRows(state, actualListState, tag, onTripSelected, thumbnailRoute, Modifier.weight(1f))
                    }
                    else -> TripHistoryRows(state, actualListState, tag, onTripSelected, thumbnailRoute, Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun BackButton(description: String, onBack: () -> Unit, tag: String) {
    Box(
        Modifier.size(AllTripsKit.Pane.Header.Back.W, AllTripsKit.Pane.Header.Back.H).clip(penShape(AllTripsKit.Pane.Header.Back.Radius))
            .background(AllTripsKit.Pane.Header.Back.Fill).border(AllTripsKit.Pane.Header.Back.Stroke, AllTripsKit.Pane.Header.Back.StrokeColor, penShape(AllTripsKit.Pane.Header.Back.Radius))
            .clickable(role = Role.Button, onClick = onBack)
            .semantics { contentDescription = description }
            .testTag("$tag/Pane/Header/Back"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(PenIcons.ArrowBack, null, Modifier.size(AllTripsKit.Pane.Header.Back.Icon.W).testTag("$tag/Pane/Header/Back/Icon"), tint = AllTripsKit.Pane.Header.Back.Icon.Fill)
    }
}

@Composable
private fun TripHistoryRows(state: TripHistoryScreenState, listState: LazyListState, tag: String, onTripSelected: (String) -> Unit, thumbnailRoute: @Composable (TripHistoryItem) -> KitTripRoute, modifier: Modifier) {
    LazyColumn(
                        state = listState, modifier = modifier.testTag("$tag/Pane/List/Rows"),
                        verticalArrangement = Arrangement.spacedBy(AllTripsKit.Pane.List.Gap),
                    ) {
                        state.groups.forEach { group ->
                            if (group.trips.isNotEmpty()) {
                                // Group labels omit the year; key by row identity to keep
                                // same weekday/month labels from different years distinct.
                                item(key = "group:${group.trips.first().id}:${group.label}") {
                                    PenText(group.label, groupStyle(group.label), groupFill(group.label),
                                        Modifier.height(groupHeight(group.label)).testTag("$tag/Pane/List/${groupTag(group.label)}"))
                                }
                                items(group.trips, key = { it.id }) { trip ->
                                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                                        KitDriveScale(OverlayTier.Medium) {
                                            KitTripRow(
                                                trip.row.copy(route = thumbnailRoute(trip)), Modifier.fillMaxWidth(),
                                                width = maxWidth / org.wpfy.carlauncher.design.kit.kitDriveScaleFor(OverlayTier.Medium),
                                                currentLabel = state.currentLabel,
                                                tag = "$tag/Pane/List/${trip.referenceTag ?: "Trip-${trip.id}"}",
                                                onClick = { onTripSelected(trip.id) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
}

@Composable
private fun TripRangeTabs(state: TripHistoryScreenState, tag: String, onSelected: (TripHistoryRange) -> Unit) {
    Row(
        Modifier.heightIn(min = AllTripsKit.Pane.Header.RangeTabs.H).clip(penShape(AllTripsKit.Pane.Header.RangeTabs.Radius)).background(AllTripsKit.Pane.Header.RangeTabs.Fill)
            .border(AllTripsKit.Pane.Header.RangeTabs.Stroke, AllTripsKit.Pane.Header.RangeTabs.StrokeColor, penShape(AllTripsKit.Pane.Header.RangeTabs.Radius))
            .testTag(tag)
            .padding(start = AllTripsKit.Pane.Header.RangeTabs.PadStart, top = AllTripsKit.Pane.Header.RangeTabs.PadTop, end = AllTripsKit.Pane.Header.RangeTabs.PadEnd, bottom = AllTripsKit.Pane.Header.RangeTabs.PadBottom),
        horizontalArrangement = Arrangement.spacedBy(AllTripsKit.Pane.Header.RangeTabs.Gap), verticalAlignment = Alignment.CenterVertically,
    ) {
        TripRangeTab(state.weekLabel, TripHistoryRange.Week, state.range, weekTokens(), monthTokens(), "$tag/Week", onSelected)
        TripRangeTab(state.monthLabel, TripHistoryRange.Month, state.range, monthInactiveTokens(), monthTokens(), "$tag/Month", onSelected)
        TripRangeTab(state.allLabel, TripHistoryRange.All, state.range, allTokens(), monthTokens(), "$tag/All", onSelected)
    }
}

@Composable
private fun TripRangeTab(label: String, range: TripHistoryRange, selectedRange: TripHistoryRange, tokens: TripRangeTokens, selectedTokens: TripRangeTokens, tag: String, onSelected: (TripHistoryRange) -> Unit) {
    val isActive = selectedRange == range
    Box(
        Modifier.widthIn(min = tokens.width).heightIn(min = tokens.height).clip(penShape(tokens.radius))
            .background(if (isActive) selectedTokens.fill else tokens.fill)
            .clickable(role = Role.Button) { onSelected(range) }.semantics { selected = isActive }.testTag(tag)
            .padding(start = tokens.padStart, top = tokens.padTop, end = tokens.padEnd, bottom = tokens.padBottom),
        contentAlignment = Alignment.Center,
    ) {
        PenText(label, if (isActive) selectedTokens.style else tokens.style, if (isActive) selectedTokens.textFill else tokens.textFill, Modifier.testTag("$tag/Label"))
    }
}

private fun weekTokens() = TripRangeTokens(WeekRangeKit.W, WeekRangeKit.H, WeekRangeKit.PadStart, WeekRangeKit.PadEnd, WeekRangeKit.PadTop, WeekRangeKit.PadBottom, WeekRangeKit.Radius, Color.Transparent, WeekRangeKit.Label.Style, WeekRangeKit.Label.Fill)
private fun monthTokens() = TripRangeTokens(MonthRangeKit.W, MonthRangeKit.H, MonthRangeKit.PadStart, MonthRangeKit.PadEnd, MonthRangeKit.PadTop, MonthRangeKit.PadBottom, MonthRangeKit.Radius, MonthRangeKit.Fill, MonthRangeKit.Label.Style, MonthRangeKit.Label.Fill)
private fun monthInactiveTokens() = TripRangeTokens(MonthRangeKit.W, MonthRangeKit.H, MonthRangeKit.PadStart, MonthRangeKit.PadEnd, MonthRangeKit.PadTop, MonthRangeKit.PadBottom, MonthRangeKit.Radius, Color.Transparent, WeekRangeKit.Label.Style, WeekRangeKit.Label.Fill)
private fun allTokens() = TripRangeTokens(AllRangeKit.W, AllRangeKit.H, AllRangeKit.PadStart, AllRangeKit.PadEnd, AllRangeKit.PadTop, AllRangeKit.PadBottom, AllRangeKit.Radius, Color.Transparent, AllRangeKit.Label.Style, AllRangeKit.Label.Fill)

private data class TripRangeTokens(
    val width: Dp, val height: Dp, val padStart: Dp, val padEnd: Dp, val padTop: Dp, val padBottom: Dp,
    val radius: Dp, val fill: Color, val style: TextStyle, val textFill: Color,
)

private fun groupTag(label: String): String = "Group" + label.filter(Char::isLetterOrDigit)
private fun groupHeight(label: String): Dp = if (label.equals("TODAY", true)) AllTripsKit.Pane.List.GroupTODAY.H else AllTripsKit.Pane.List.GroupYESTERDAY.H
private fun groupStyle(label: String): TextStyle = if (label.equals("TODAY", true)) AllTripsKit.Pane.List.GroupTODAY.Style else AllTripsKit.Pane.List.GroupYESTERDAY.Style
private fun groupFill(label: String): Color = if (label.equals("TODAY", true)) AllTripsKit.Pane.List.GroupTODAY.Fill else AllTripsKit.Pane.List.GroupYESTERDAY.Fill
