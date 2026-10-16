package org.wpfy.carlauncher.feature.trip

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wpfy.carlauncher.data.settings.SpeedUnitSetting
import org.wpfy.carlauncher.data.settings.rememberUnitRegionLocale
import org.wpfy.carlauncher.data.settings.resolveUnitSystem
import org.wpfy.carlauncher.design.kit.KitDockActions
import org.wpfy.carlauncher.design.kit.KitStateMessage
import org.wpfy.carlauncher.design.kit.PenKit
import org.wpfy.carlauncher.design.kit.screenGroupFor
import org.wpfy.carlauncher.design.screens.TripHistoryRange
import org.wpfy.carlauncher.design.screens.TripHistoryScreen
import org.wpfy.carlauncher.design.screens.TripHistoryScreenState
import org.wpfy.carlauncher.design.screens.TripHistoryStatus
import org.wpfy.carlauncher.design.screens.KitDockState
import org.wpfy.carlauncher.design.screens.TripScreen
import org.wpfy.carlauncher.design.screens.TripScreenSpec
import org.wpfy.carlauncher.design.theme.LocalCarWindow
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** Minimal seam so route behavior can be exercised without mutating the process recorder. */
internal data class TripRouteSource(
    val summaries: Flow<List<TripSummary>>,
    val liveState: StateFlow<TripState>,
    val resetStatus: StateFlow<TripResetStatus?>,
    val readTrip: suspend (String?) -> TripReadResult,
    val reset: (String) -> Deferred<TripResetResult>,
    val referenceNow: suspend (List<TripSummary>) -> Long,
    val persistsTrips: Boolean,
    val canReset: (String) -> Boolean,
)

/** Runtime Trip destination. It reads existing history/live owner state and never starts location recording. */
@Composable
fun TripRoute(
    navigationState: TripNavigationState,
    speedUnit: SpeedUnitSetting = SpeedUnitSetting.Auto,
    modifier: Modifier = Modifier,
    onBackToHome: () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val source = remember(context) {
        TripRouteSource(
            summaries = TripRepositoryFactory.create(context).summaries,
            liveState = TripSessionStore.state,
            resetStatus = TripSessionStore.resetStatus,
            readTrip = { id -> TripSessionStore.readTrip(context, id) },
            reset = TripSessionStore::resetExistingTrip,
            referenceNow = { saved -> TripSessionStore.referenceNow(saved) },
            persistsTrips = TripRepositoryFactory.persistsTrips,
            canReset = { id ->
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    TripSessionStore.canResetExistingTrip(id)
            },
        )
    }
    TripRouteContent(navigationState, source, speedUnit, modifier, onBackToHome)
}

@Composable
internal fun TripRouteContent(
    navigationState: TripNavigationState,
    source: TripRouteSource,
    speedUnit: SpeedUnitSetting = SpeedUnitSetting.Auto,
    modifier: Modifier = Modifier,
    onBackToHome: () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val group = screenGroupFor(LocalCarWindow.current)
    val locale = rememberUnitRegionLocale()
    val units = resolveUnitSystem(speedUnit, locale)
    val localeNow = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val liveState by source.liveState.collectAsState()
    val resetStatus by source.resetStatus.collectAsState()
    var summaries by remember { mutableStateOf(emptyList<TripSummary>()) }
    var zone by remember { mutableStateOf(ZoneId.systemDefault()) }
    var clockRevision by remember { mutableIntStateOf(0) }
    var referenceNow by remember { mutableStateOf<Long?>(null) }
    var summaryReadFailed by remember { mutableStateOf(false) }
    var summariesLoaded by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    val detailId = navigationState.selectedTripId ?: liveState.tripOrNull()?.id
        ?: summaries.firstOrNull()?.trip?.id
    var detailRead by remember(detailId) { mutableStateOf<TripReadResult?>(null) }
    var liveRefresh by remember { mutableIntStateOf(0) }
    val spec = remember(group) { TripScreenSpec.forGroup(group) }
    val routeScope = rememberCoroutineScope()

    BackHandler {
        if (!navigationState.back()) onBackToHome()
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) { clockRevision++ }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }
    LaunchedEffect(source, summaries, clockRevision) {
        while (true) {
            referenceNow = source.referenceNow(summaries)
            zone = ZoneId.systemDefault()
            delay(TimeUnit.MINUTES.toMillis(1))
        }
    }
    LaunchedEffect(source.summaries, retry) {
        source.summaries
            .flowOn(Dispatchers.IO)
            .catch { summaryReadFailed = true }
            .collect {
                val now = source.referenceNow(it)
                referenceNow = now
                zone = ZoneId.systemDefault()
                summaries = it
                summaryReadFailed = false
                summariesLoaded = true
            }
    }
    val selectedSummary = summaries.firstOrNull { it.trip.id == detailId }
    val liveStateKey = when (val state = liveState) {
        is TripState.Recording -> state.trip.id to "recording"
        is TripState.Paused -> state.trip.id to "paused:${state.reason.name}"
        TripState.Idle -> null
    }
    val liveReadKey = liveStateKey?.takeIf { it.first == detailId }
    val selectedSummaryReadKey = selectedSummary.takeUnless { liveReadKey != null }
    val selectedLiveRefresh = liveRefresh.takeIf { liveReadKey != null }
    LaunchedEffect(navigationState.page, detailId, liveReadKey, selectedSummaryReadKey, selectedLiveRefresh, retry, summariesLoaded, summaryReadFailed) {
        if (navigationState.page == TripNavigationPage.Detail && detailId != null) {
            detailRead = withContext(Dispatchers.IO) {
                try { source.readTrip(detailId) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { TripReadResult.Unavailable }
            }
        } else if (navigationState.page == TripNavigationPage.Detail && summaryReadFailed) {
            detailRead = TripReadResult.Unavailable
        } else if (navigationState.page == TripNavigationPage.Detail && summariesLoaded) {
            detailRead = TripReadResult.Empty
        }
    }
    LaunchedEffect(navigationState.page, liveStateKey) {
        if (liveStateKey != null) {
            while (true) {
                delay(TimeUnit.SECONDS.toMillis(15))
                liveRefresh++
            }
        }
    }
    LaunchedEffect(resetStatus, navigationState.requestedResetId, navigationState.selectedTripId, navigationState.page) {
        val requested = navigationState.requestedResetId ?: return@LaunchedEffect
        val status = resetStatus?.takeIf { it.expectedTripId == requested && !it.isPending } ?: return@LaunchedEffect
        val result = status.outcome
        if (navigationState.page == TripNavigationPage.Detail && navigationState.selectedTripId == requested && result is TripResetResult.Reset) {
            navigationState.applyResetReplacement(requested, result.replacementTripId)
        }
    }
    if (navigationState.page == TripNavigationPage.History) {
        val status = when {
            referenceNow == null -> TripHistoryStatus.Loading
            !summariesLoaded && !summaryReadFailed -> TripHistoryStatus.Loading
            summaryReadFailed && (summaries.isNotEmpty() || liveState != TripState.Idle) -> TripHistoryStatus.Partial
            summaryReadFailed -> TripHistoryStatus.Error
            summaries.isEmpty() && liveState == TripState.Idle -> TripHistoryStatus.Empty
            else -> TripHistoryStatus.Ready
        }
        val now = referenceNow ?: 0L
        val historyState by produceState<TripHistoryScreenState?>(null, summaries, liveState, navigationState.historyFilter, now, zone, units, localeNow, status) {
            value = withContext(Dispatchers.Default) {
                val rows = if (referenceNow == null) TripHistoryRows(emptyList(), TripTotals())
                else TripPresentation.history(summaries, liveState, navigationState.historyFilter, now, zone)
                tripHistoryUi(rows, navigationState.historyFilter, now, zone, units, localeNow, liveState, status, persistsTrips = source.persistsTrips)
            }
        }
        val state = historyState ?: tripHistoryUi(
            TripHistoryRows(emptyList(), TripTotals()), navigationState.historyFilter, now, zone,
            units, localeNow, liveState, TripHistoryStatus.Loading, persistsTrips = source.persistsTrips,
        )
        val listState = rememberLazyListState(navigationState.historyIndex, navigationState.historyOffset)
        LaunchedEffect(listState) {
            snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
                .distinctUntilChanged()
                .collect { (index, offset) -> navigationState.updateScroll(index, offset) }
        }
        TripHistoryScreen(
            state = state,
            modifier = modifier,
            listState = listState,
            group = group,
            showDock = false,
            onBack = { navigationState.back() },
            onRangeSelected = { range ->
                navigationState.changeFilter(when (range) {
                    TripHistoryRange.Week -> TripHistoryFilter.Week
                    TripHistoryRange.Month -> TripHistoryFilter.Month
                    TripHistoryRange.All -> TripHistoryFilter.All
                })
                routeScope.launch { listState.scrollToItem(0) }
            },
            onTripSelected = navigationState::openTrip,
            onRetry = { retry++ },
            thumbnailRoute = { item ->
                val route by produceState(org.wpfy.carlauncher.design.kit.KitTripRoute(), item.id, if (item.row.current) liveRefresh to liveStateKey else null, retry) {
                    val details = withContext(Dispatchers.IO) {
                        try {
                            when (val read = source.readTrip(item.id)) {
                                is TripReadResult.Complete -> read.details
                                is TripReadResult.Partial -> read.details
                                else -> null
                            }
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { null }
                    }
                    if (details != null) {
                        val aspect = PenKit.TripRow.Thumb.W.value / PenKit.TripRow.Thumb.H.value
                        value = withContext(Dispatchers.Default) {
                            TripPresentation.route(details.points, aspect.toDouble(), 0.0).toKitTripRoute()
                        }
                    }
                }
                route
            },
        )
        return
    }

    when (val result = detailRead) {
        null -> TripRouteMessage(spec, group, modifier, "Loading trip", "", true, onAllTrips = navigationState::openHistory)
        TripReadResult.Empty -> {
            val selected = navigationState.selectedTripId != null
            TripRouteMessage(
                spec, group, modifier,
                if (selected) "Trip unavailable" else "No trips yet",
                if (selected) "This trip is no longer available." else "Recorded trips will appear here.",
                false,
                onAllTrips = navigationState::openHistory,
            )
        }
        TripReadResult.Unavailable -> TripRouteMessage(
            spec, group, modifier, "Trip unavailable", "Trip details could not be read.", false,
            actionLabel = "Retry", onAction = { retry++ }, onAllTrips = navigationState::openHistory,
        )
        is TripReadResult.Complete, is TripReadResult.Partial -> {
            val details = when (result) {
                is TripReadResult.Complete -> result.details
                is TripReadResult.Partial -> result.details
                else -> error("Exhaustive Trip read")
            }
            val live = when (result) {
                is TripReadResult.Complete -> result.liveState
                is TripReadResult.Partial -> result.liveState
                else -> null
            }
            val id = details.trip.id
            val now = referenceNow ?: run {
                TripRouteMessage(spec, group, modifier, "Loading trip", "", true, onAllTrips = navigationState::openHistory)
                return
            }
            val permitted = source.canReset(id)
            val resetEligible = source.persistsTrips && permitted && live?.tripOrNull()?.id == id
            val resetPending = resetStatus?.isPending == true
            val resetDescription = when {
                resetPending -> "Reset in progress"
                !source.persistsTrips -> "Reset unavailable for session-only trips"
                !permitted -> "Reset needs an active recording and precise location access"
                live?.tripOrNull()?.id != id -> "Only the current trip can be reset"
                else -> "Reset this trip and start a new recording"
            }
            val observedStatus = resetStatus?.takeIf {
                it.expectedTripId == navigationState.requestedResetId &&
                    (it.expectedTripId == id || (it.outcome as? TripResetResult.Reset)?.replacementTripId == id)
            }
            val resetOutcome = observedStatus?.outcome
            val resetMessage = when (resetOutcome) {
                is TripResetResult.ArchiveFailed -> "Reset failed; trip was kept"
                is TripResetResult.ArchivedWithoutReplacement -> "Trip saved; new recording did not start"
                is TripResetResult.StaleRequest -> "Reset ignored; trip changed"
                TripResetResult.NoActiveTrip -> "No active trip to reset"
                TripResetResult.Unavailable -> "Reset unavailable"
                is TripResetResult.Reset, null -> null
            }
            val ui by produceState<TripDetailUi?>(null, details, live, now, zone, units, localeNow, group, result, resetEligible, resetPending, resetDescription, resetMessage) {
                value = withContext(Dispatchers.Default) {
                    tripDetailUi(
                        details, live, now, zone, units, localeNow, group,
                        partial = result is TripReadResult.Partial,
                        resetEnabled = resetEligible && !resetPending,
                        resetDescription = resetMessage ?: resetDescription,
                        statusMessage = resetMessage,
                    )
                }
            }
            val detailUi = ui ?: run {
                TripRouteMessage(spec, group, modifier, "Loading trip", "", true, onAllTrips = navigationState::openHistory)
                return
            }
            val runtimeRoute: @Composable (Dp, Dp, Dp) -> org.wpfy.carlauncher.design.kit.KitTripRoute = { width, height, markerPadding ->
                var projected by remember(details.trip.id, width, height, markerPadding) {
                    mutableStateOf(org.wpfy.carlauncher.design.kit.KitTripRoute())
                }
                LaunchedEffect(details.trip.id, details.points, width, height, markerPadding) {
                    if (width > 0.dp && height > 0.dp) {
                        val aspect = width.value / height.value
                        val padding = (markerPadding.value / minOf(width.value, height.value)).toDouble().coerceIn(0.0, 0.49)
                        projected = withContext(Dispatchers.Default) {
                            TripPresentation.route(details.points, aspect.toDouble(), padding).toKitTripRoute()
                        }
                    }
                }
                projected
            }
            TripScreen(
                spec, detailUi.state, modifier, dock = KitDockState("", 0), dockActions = KitDockActions(),
                showDock = false, onAllTrips = navigationState::openHistory,
                onReset = {
                    if (resetEligible && source.canReset(id) && source.resetStatus.value?.isPending != true) {
                        navigationState.observeReset(id)
                        source.reset(id)
                    }
                },
                runtimeRoute = runtimeRoute,
            )
        }
    }
}

@Composable
private fun TripRouteMessage(
    spec: TripScreenSpec,
    group: org.wpfy.carlauncher.design.kit.ScreenGroup,
    modifier: Modifier,
    title: String,
    message: String,
    loading: Boolean,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    onAllTrips: () -> Unit,
) {
    val screenState = tripPlaceholderState(group, title, "Reset unavailable until an active trip is loaded")
    val route: @Composable (Dp, Dp, Dp) -> org.wpfy.carlauncher.design.kit.KitTripRoute = { _, _, _ ->
        KitStateMessage(title, message, Modifier.fillMaxSize(), loading = loading, actionLabel = actionLabel, onAction = onAction)
        org.wpfy.carlauncher.design.kit.KitTripRoute()
    }
    TripScreen(
        spec = spec,
        state = screenState,
        modifier = modifier,
        dock = KitDockState("", 0),
        dockActions = KitDockActions(),
        showDock = false,
        onAllTrips = onAllTrips,
        runtimeRoute = route,
    )
}

/** Narrow content seam for tests and shell integration: it contains no storage or recording lifecycle. */
internal fun TripState.tripOrNull(): Trip? = when (this) {
    is TripState.Recording -> trip
    is TripState.Paused -> trip
    TripState.Idle -> null
}
