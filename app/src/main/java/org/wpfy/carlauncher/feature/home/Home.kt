package org.wpfy.carlauncher.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import org.wpfy.carlauncher.data.settings.LocalUnitSystem
import org.wpfy.carlauncher.data.settings.SpeedUnitSetting
import org.wpfy.carlauncher.data.settings.rememberUnitRegionLocale
import org.wpfy.carlauncher.data.settings.resolveUnitSystem
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.wpfy.carlauncher.CarLauncherApplication
import org.wpfy.carlauncher.data.settings.NavigationCardPosition
import org.wpfy.carlauncher.data.settings.OverlaySize
import org.wpfy.carlauncher.design.adapters.kitMedia
import org.wpfy.carlauncher.design.adapters.kitNowPlaying
import org.wpfy.carlauncher.design.adapters.kitNowPlayingIdle
import org.wpfy.carlauncher.design.kit.KitDriveActions
import org.wpfy.carlauncher.design.kit.KitDriveCard
import org.wpfy.carlauncher.design.kit.KitDriveCardWide
import androidx.compose.foundation.layout.fillMaxHeight
import org.wpfy.carlauncher.design.kit.KitDriveDots
import org.wpfy.carlauncher.design.kit.KitMapControl
import org.wpfy.carlauncher.design.kit.KitMediaCard
import org.wpfy.carlauncher.design.kit.KitStateMessage
import org.wpfy.carlauncher.design.kit.PenIcons
import org.wpfy.carlauncher.design.kit.PenKit
import org.wpfy.carlauncher.design.kit.PenDisc
import org.wpfy.carlauncher.design.kit.PenScreens
import org.wpfy.carlauncher.design.kit.ScreenGroup
import org.wpfy.carlauncher.design.kit.penShape
import org.wpfy.carlauncher.design.kit.screenGroupFor
import org.wpfy.carlauncher.design.screens.KitHudActions
import org.wpfy.carlauncher.design.screens.KitHudState
import org.wpfy.carlauncher.design.screens.KitMediaActions
import org.wpfy.carlauncher.design.screens.KitSearchState
import org.wpfy.carlauncher.design.screens.KitMediaState
import org.wpfy.carlauncher.design.screens.LandscapeMPaneArea
import org.wpfy.carlauncher.design.screens.LandscapeSPane
import org.wpfy.carlauncher.design.screens.LandscapeSReportSheet
import org.wpfy.carlauncher.design.screens.LandscapeSReportSheetBottomGap
import org.wpfy.carlauncher.design.screens.LandscapeSReportSheetWidth
import org.wpfy.carlauncher.design.screens.PortraitLMediaCard
import org.wpfy.carlauncher.design.screens.PortraitPaneArea
import org.wpfy.carlauncher.design.screens.PortraitLWideMediaCard
import org.wpfy.carlauncher.design.screens.PortraitScreenSpec
import org.wpfy.carlauncher.design.theme.LocalCarWindow
import org.wpfy.carlauncher.design.theme.overlayScalesFor
import org.wpfy.carlauncher.feature.media.MediaSourceDots
import org.wpfy.carlauncher.feature.media.MediaSourceSession
import org.wpfy.carlauncher.feature.media.MediaState
import org.wpfy.carlauncher.feature.media.seekPositionMs
import org.wpfy.carlauncher.feature.navigation.NavigationState
import org.wpfy.carlauncher.feature.trip.TripState

/*
 * The live Home screen: the Pen kit HUD screens of the window's screen group (the pane only: the shell
 * draws the dock). What is drawn comes from the kit; what it does is Home's: the navigation content is the
 * map, the media mode picks card, chip or pages, small screens page between the map and a media page.
 */

@Composable
fun HomeRoute(
    navigationState: NavigationState = NavigationState(),
    navigationContent: @Composable BoxScope.() -> Unit = {},
    navigationContentOwnsControls: Boolean = false,
    navigationExpanded: Boolean = false,
    /** The destination search is open (it hides the map's Navigate tab); [search] is the panel the HUD draws for it. */
    searchOpen: Boolean = navigationExpanded,
    search: KitSearchState? = null,
    // The kit HUD has no mirrored layout: the cards stay at the start edge (a Pen drift, see the plan).
    @Suppress("UNUSED_PARAMETER") navigationCardPosition: NavigationCardPosition = NavigationCardPosition.BottomLeft,
    onNavigationClick: () -> Unit = {},
    onLocationPermissionClick: () -> Unit = {},
    onNavigationSettingsClick: () -> Unit = {},
    onStopNavigation: () -> Unit = {},
    mediaState: MediaState,
    onMediaClick: () -> Unit,
    onMediaSessionSelect: (String) -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaPrevious: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaSeek: ((Long) -> Unit)? = null,
    mapAccessory: MapAccessory? = null,
    /** The map's round controls (recenter, theme, guidance mute), drawn in the HUD's control column. */
    mapControls: List<KitMapControl> = emptyList(),
    overlaySize: OverlaySize = OverlaySize.Auto,
    paidUi: Boolean = false,
    onAppsClick: () -> Unit = {},
    speedUnit: SpeedUnitSetting = SpeedUnitSetting.Auto,
    tripState: TripState = TripState.Idle,
    onTripClick: (() -> Unit)? = null,
) {
    val obdOwner = (LocalContext.current.applicationContext as CarLauncherApplication).homeObdDriveStateHolder
    val obdState by obdOwner.state.collectAsState()
    val obdSpeedKph = freshObdSpeedKph(obdState, obdState.nowElapsedRealtimeMillis)
    val state = HomeUiState(navigation = navigationState, media = mediaState, tripState = tripState)
    val units = resolveUnitSystem(speedUnit, rememberUnitRegionLocale())
    // The map's speed cluster and the HUD's distances follow the driver's unit, in either skin.
    CompositionLocalProvider(LocalUnitSystem provides units) {
        if (paidUi) {
            DistributionPaidHomeScreen(
                state = state,
                navigationContent = navigationContent,
                navigationContentOwnsControls = navigationContentOwnsControls,
                navigationExpanded = navigationExpanded,
                onSearchClick = onNavigationClick,
                onLocationPermissionClick = onLocationPermissionClick,
                onStopNavigation = onStopNavigation,
                onMediaClick = onMediaClick,
                onMediaPlayPause = onMediaPlayPause,
                onMediaPrevious = onMediaPrevious,
                onMediaNext = onMediaNext,
                onAppsClick = onAppsClick,
                mapControls = mapControls,
                onTripClick = onTripClick,
            )
        } else {
        HomeContent(
            state = state,
            navigationContent = navigationContent,
            navigationContentOwnsControls = navigationContentOwnsControls,
            navigationExpanded = navigationExpanded,
            searchOpen = searchOpen,
            search = search,
            onSearchClick = onNavigationClick,
            onLocationPermissionClick = onLocationPermissionClick,
            onNavigationSettingsClick = onNavigationSettingsClick,
            onStopNavigation = onStopNavigation,
            onMediaClick = onMediaClick,
            onMediaSessionSelect = onMediaSessionSelect,
            onMediaPlayPause = onMediaPlayPause,
            onMediaPrevious = onMediaPrevious,
            onMediaNext = onMediaNext,
            onMediaSeek = onMediaSeek,
            onTripClick = onTripClick,
            mapAccessory = mapAccessory,
            obdSpeedKph = obdSpeedKph,
            mapControls = mapControls,
            overlaySize = overlaySize,
        )
        }
    }
}

private const val Tag = "Home"

@Composable
internal fun HomeContent(
    state: HomeUiState,
    navigationContent: @Composable BoxScope.() -> Unit = {},
    navigationContentOwnsControls: Boolean = false,
    navigationExpanded: Boolean = false,
    searchOpen: Boolean = navigationExpanded,
    /** The destination search panel the HUD draws over the map; with it the HUD keeps the Pen's search frames, without it an open search only clears the top edge. */
    search: KitSearchState? = null,
    onSearchClick: () -> Unit = {},
    onLocationPermissionClick: () -> Unit = {},
    onNavigationSettingsClick: () -> Unit = {},
    onStopNavigation: () -> Unit = {},
    onMediaClick: () -> Unit = {},
    onMediaSessionSelect: (String) -> Unit = {},
    onMediaPlayPause: () -> Unit = {},
    onMediaPrevious: () -> Unit = {},
    onMediaNext: () -> Unit = {},
    onMediaSeek: ((Long) -> Unit)? = null,
    onTripClick: (() -> Unit)? = null,
    mapAccessory: MapAccessory? = null,
    obdSpeedKph: Double? = null,
    /** The Free edition shows the Free Drive Card; set by the build flavour, never inferred from the Pro accessory (which registers late). */
    freeEdition: Boolean = IsFreeEdition,
    /** Location permission as the Free card sees it; null reads the system's (re-read on resume). Tests set it. */
    locationGranted: Boolean? = null,
    mapControls: List<KitMapControl> = emptyList(),
    overlaySize: OverlaySize = OverlaySize.Auto,
) {
    val group = screenGroupFor(LocalCarWindow.current)
    val tier = group.tier
    val scales = overlayScalesFor(tier, overlaySize)
    val units = LocalUnitSystem.current
    val nav = homeNavigation(state.navigation, rememberGpsLossConfirmed(state.navigation.gpsAvailable))
    val media = state.media

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val areaWidth = maxWidth
        val areaHeight = maxHeight
        val guiding = showsGuidance(nav)
        val cardFits = homeCardFits(group, areaWidth, areaHeight, guiding)
        val accessoryHud = mapAccessory?.hud()
        // FREE SEAM: the one place that decides whether the card slot holds the Drive Card. Pro has its accessory and OBD;
        // the Free edition (by build flavour), whose card (speedo + trip) shows with OBD speed or location permission (its speed reads "--" while the fix is stale).
        // Free with neither keeps the media card. GPS is collected only while Home is shown, and never asks for permission.
        val free = freeEdition
        val systemGranted = rememberLocationPermissionGranted(enabled = free && locationGranted == null)
        val gpsSource = free && (locationGranted ?: systemGranted)
        val gpsSpeed = rememberDriveGpsSpeed(enabled = gpsSource)
        val driveSpeed = resolveDriveSpeed(obdSpeedKph, gpsSpeed)
        val freeSpeedKph = driveSpeed.kph
        val driveCard = homeShowsDriveCard(navigationExpanded, accessoryHud != null, obdSpeedKph != null, hasGpsSource = gpsSource)
        val mode = if (navigationExpanded) {
            HomeMediaMode.None
        } else {
            homeMediaModeFor(tier, media.hasSession, cardFits, driveCard)
        }
        val paged = mode == HomeMediaMode.Paged
        val portrait = group == ScreenGroup.PortraitS || group == ScreenGroup.PortraitL
        val fontScale = LocalDensity.current.fontScale
        val wideCardHeight = when {
            driveCard && portrait -> driveWideHeight(group, areaWidth, fontScale)
            // Portrait L without a Drive Card keeps the media card of before it: the Pen's fixed height.
            mode == HomeMediaMode.Card && group == ScreenGroup.PortraitL -> PortraitLWideMediaCard.H
            else -> 0.dp
        }
        val mapBounds = homeMapBounds(group, mode == HomeMediaMode.Card, areaWidth, areaHeight, driveCard, wideCardHeight)
        val speedOverride = if (free) {
            freeSpeedKph?.let {
                val speed = freeDriveSpeed(it, units)
                if (obdSpeedKph != null) DriveCardSpeedOverride(speed, overLimit = false) else DriveCardSpeedOverride(speed, overLimit = false, source = DriveSourceGps)
            }
        } else {
            obdSpeedKph?.let {
                driveCardSpeedOverride(
                    speedKph = it,
                    speedLimitKph = nav.progress.speedLimitKph.takeIf { nav.active },
                    cameraLimit = accessoryHud?.cameraLimit,
                    units = units,
                )
            }
        }
        // --- Small screens: the map page and the media page that slides over it ------------------
        val scope = rememberCoroutineScope()
        val pageState = remember { MediaPageState(scope) }
        val cameraUrgent by remember(mapAccessory) { derivedStateOf { mapAccessory?.demandsMapPage() == true } }
        val mapRequired = paged && mapPageRequired(
            guiding = nav.active && nav.progress.nextManeuver != null,
            nextManeuverMeters = nav.progress.nextManeuver?.distanceMeters,
            speedMps = nav.progress.speedMps,
            cameraUrgent = cameraUrgent,
        )
        val swipeEnabled = paged && !mapRequired
        val currentSwipeEnabled by rememberUpdatedState(swipeEnabled)
        LaunchedEffect(mapRequired) { if (mapRequired) pageState.animateTo(0) }
        LaunchedEffect(paged) { if (!paged) pageState.jumpTo(0) }
        BackHandler(enabled = paged && pageState.target == 1) { pageState.animateTo(0) }
        val widthPx = with(LocalDensity.current) { areaWidth.toPx() }
        val onPageDrag: (Float, Float) -> Unit = { delta, width ->
            pageState.dragTo(pageProgressAfterDrag(pageState.progress, delta, width, currentSwipeEnabled))
        }
        val onPageStop: (Float) -> Unit = { velocity ->
            pageState.animateTo(pageSettleTarget(pageState.progress, velocity, currentSwipeEnabled))
        }
        val dragState = rememberDraggableState { delta -> onPageDrag(delta, widthPx) }
        val pageDrag = Modifier.draggable(
            state = dragState,
            orientation = Orientation.Horizontal,
            enabled = swipeEnabled,
            onDragStopped = { velocity -> onPageStop(velocity) },
        )

        // Visible only while the card is on screen: the Small media page covers it, and a camera confirmation must not run unseen.
        val driveCardOnScreen by remember(driveCard, paged) {
            derivedStateOf { homeDriveCardOnScreen(driveCard, paged, pageState.isMediaShown) }
        }
        DisposableEffect(mapAccessory, driveCardOnScreen) {
            mapAccessory?.setDriveCardVisible(driveCardOnScreen)
            onDispose { mapAccessory?.setDriveCardVisible(false) }
        }

        val chipAction = chipActionFor(mode)
        val showChip = !driveCard && (mode == HomeMediaMode.Chip || paged)
        val nowPlaying = if (showChip) {
            kitNowPlaying(media)?.let { it.copy(description = nowPlayingDescription(it.title)) }
                ?: kitNowPlayingIdle().copy(description = "Not playing. Open media")
        } else {
            null
        }
        val actions = KitHudActions(
            onMuteAlert = accessoryHud?.onAlertButton ?: {},
            onEnd = onStopNavigation,
            onSearch = onSearchClick,
            onPlayPause = onMediaPlayPause,
            onReport = accessoryHud?.onReport ?: {},
            onCloseReport = accessoryHud?.onCloseReport ?: {},
            onUndoReport = accessoryHud?.onUndoReport ?: {},
            mapControls = if (navigationContentOwnsControls) emptyList() else mapControls,
            onOpenAppSettings = onLocationPermissionClick,
            onRetry = onSearchClick,
            onNavigationSettings = onNavigationSettingsClick,
            onChipClick = {
                when (chipAction) {
                    ChipAction.ShowMediaPage -> if (swipeEnabled) pageState.animateTo(1)
                    ChipAction.OpenMedia -> onMediaClick()
                }
            },
            onNextTurnClick = { pageState.animateTo(0) },
            onDotsClick = {
                if (pageState.target == 1) pageState.animateTo(0) else if (swipeEnabled) pageState.animateTo(1)
            },
            chipModifier = if (paged) pageDrag else Modifier,
            dotsModifier = if (paged) pageDrag else Modifier,
            minTouch = 48.dp,
        )
        val mediaActions = KitMediaActions(onMediaPrevious, onMediaPlayPause, onMediaNext)
        val mediaKit = kitMedia(media)

        fun hud(mediaPage: Boolean) = homeHud(
            nav, accessoryHud, nowPlaying, mediaPage,
            ownsControls = navigationContentOwnsControls,
            // The page dots are on the media page only; the map page has none.
            dots = paged && mediaPage,
            units = units,
            driveCard = driveCard,
            searchOpen = searchOpen && search == null,
        )
            .copy(searchPanel = search)

        // The Drive Card of every group: tall beside the map (landscape), wide under it (portrait). A tap on its music
        // pill opens the media page on the paged Small groups, the Media screen elsewhere.
        val driveActions = KitDriveActions(
            onPrevious = mediaActions.onPrevious, onPlayPause = mediaActions.onPlayPause, onNext = mediaActions.onNext,
            onConfirmYes = accessoryHud?.onConfirmYes ?: {}, onConfirmNo = accessoryHud?.onConfirmNo ?: {},
            onUndo = accessoryHud?.onUndoReport ?: {}, onMute = accessoryHud?.onAlertButton,
            onTrip = onTripClick,
        )
        val driveCorner = when (group) {
            ScreenGroup.LandscapeS -> PenScreens.LandscapeS.Idle.MapPane.Radius
            ScreenGroup.PortraitS -> PortraitScreenSpec.PortraitS.radius
            ScreenGroup.PortraitL -> PortraitScreenSpec.PortraitL.radius
            else -> mapScreenSpecFor(group).paneRadius
        }
        val driveContent: @Composable (width: Dp, height: Dp, wide: Boolean) -> Unit = { width, height, wide ->
            val openMedia: () -> Unit = { if (paged) { if (swipeEnabled) pageState.animateTo(1) } else onMediaClick() }
            HomeDriveCardSlot(media, openMedia, onMediaSessionSelect, Modifier.width(width).height(height)) { session ->
                // One ImageBitmap per artwork: the album glow is keyed on it, and a new wrapper on every
                // recomposition (each speed fix) would restart it and flash the Pen's default glow colour.
                val artwork = session?.artwork
                val art = remember(artwork) { artwork?.asImageBitmap() }
                val driveSessions = media.availableSessions.ifEmpty { listOfNotNull(media.selectedSession) }
                val driveDots = if (session == null) null else KitDriveDots(
                    driveSessions.size, driveSessions.indexOfFirst { it.id == media.selectedSessionId }.coerceAtLeast(0),
                )
                val driveState = homeDriveState(
                    accessoryHud, session, units, road = nav.progress.currentRoad,
                    speedOverride = speedOverride,
                    art = { bitmap -> if (bitmap === artwork && art != null) art else bitmap.asImageBitmap() },
                    dots = driveDots,
                    tripState = state.tripState,
                )
                if (wide) {
                    KitDriveCardWide(driveState, width, actions = driveActions, corner = driveCorner, height = height, free = free)
                } else {
                    KitDriveCard(driveState, height, actions = driveActions, corner = driveCorner, width = width, free = free)
                }
            }
        }

        val pagedModifier = Modifier.fillMaxSize().edgePageSwipe(
            enabled = swipeEnabled, edge = PageEdgeStrip, onDrag = onPageDrag, onStop = onPageStop,
        )
        // The page swipe is on the parent of the map and the pane, so the map stays in its hit-test path:
        // a sibling overlay would take every tap, pan and zoom the swipe leaves unconsumed.
        Box(pagedModifier) {
        // The map stays at one place in the composition whatever the group (rotation, card, pages): only its
        // bounds change, so an embedded surface is resized rather than rebuilt. The kit pane (clip, HUD) draws over it.
        Box(
            Modifier.offset(mapBounds.x, mapBounds.y).size(mapBounds.width, mapBounds.height)
                .clip(penShape(mapBounds.radius)).testTag("$Tag/Map"),
        ) { navigationContent() }
        when (group) {
            ScreenGroup.LandscapeS, ScreenGroup.PortraitS -> Box(Modifier.fillMaxSize()) {
                if (group == ScreenGroup.PortraitS) {
                    PortraitPaneArea(
                        PortraitScreenSpec.PortraitS, hud(false), null, actions, mediaActions, Tag, areaWidth, areaHeight, scales,
                        card = if (driveCard) ({ driveContent(areaWidth, wideCardHeight, true) }) else null,
                        cardHeight = wideCardHeight,
                    )
                } else {
                    val cardWidth = if (driveCard) driveCardWidth(group, areaHeight) else 0.dp
                    SmallScales(scales) {
                        LandscapeSPane(
                            hud(false), null, actions, mediaActions, Tag,
                            Modifier.width(mapBounds.width).fillMaxHeight(),
                        )
                    }
                    if (driveCard) {
                        Box(Modifier.offset(areaWidth - cardWidth, 0.dp).testTag("$Tag/DriveCard")) {
                            SmallScales(scales) { driveContent(cardWidth, areaHeight, false) }
                        }
                    }
                    // The report sheet is the screen's, centred on it and over the dock edge: the area starts at the pane's
                    // x and y in the Pen screen, so the sheet's offset is its Pen position less that corner. It measures to its
                    // content (it grows with the font scale), so it hangs from its bottom edge, the Pen's gap above the screen's.
                    val screenWidth = areaWidth + PenScreens.LandscapeS.Idle.MapPane.X + PenScreens.LandscapeS.Idle.MapPane.Y
                    Box(
                        Modifier
                            .offset((screenWidth - LandscapeSReportSheetWidth) / 2 - PenScreens.LandscapeS.Idle.MapPane.X, 0.dp)
                            .size(LandscapeSReportSheetWidth, areaHeight + PenScreens.LandscapeS.Idle.MapPane.Y - LandscapeSReportSheetBottomGap),
                        contentAlignment = Alignment.BottomCenter,
                    ) { SmallScales(scales) { LandscapeSReportSheet(hud(false), actions, Tag) } }
                }
                if (paged && mediaKit != null) {
                    val mediaVisible by remember { derivedStateOf { pageState.isMediaShown } }
                    if (mediaVisible) {
                        val nestedScroll = remember(pageState) {
                            MediaPageNestedScroll(pageState, { widthPx }, { currentSwipeEnabled })
                        }
                        Box(
                            Modifier.fillMaxSize()
                                .graphicsLayer { translationX = (1f - pageState.progress) * size.width }
                                .nestedScroll(nestedScroll),
                        ) {
                            SessionPager(media, onMediaSessionSelect, Modifier.fillMaxSize()) { session ->
                                val page = kitMedia(session)
                                val pageActions = KitMediaActions(
                                    mediaActions.onPrevious, mediaActions.onPlayPause, mediaActions.onNext,
                                    onSeek = onMediaSeek?.let { seek -> { fraction -> onMediaSessionSelect(session.id); seek(seekPositionMs(fraction, session.durationMs)) } },
                                )
                                if (group == ScreenGroup.PortraitS) {
                                    PortraitPaneArea(
                                        PortraitScreenSpec.PortraitS, hud(true), page, actions, pageActions, Tag, areaWidth, areaHeight, scales,
                                    )
                                } else {
                                    SmallScales(scales) { LandscapeSPane(hud(true), page, actions, pageActions, Tag, Modifier.fillMaxSize()) }
                                }
                            }
                        }
                    }
                }
            }

            ScreenGroup.PortraitL -> PortraitPaneArea(
                PortraitScreenSpec.PortraitL, hud(false), null, actions, mediaActions, Tag, areaWidth, areaHeight, scales,
                card = when {
                    driveCard -> ({ driveContent(areaWidth, wideCardHeight, true) })
                    mode == HomeMediaMode.Card -> ({
                        HomeMediaCard(media, onMediaClick, onMediaSessionSelect, Modifier.fillMaxWidth().height(wideCardHeight)) { s ->
                            PortraitLMediaCard(kitMedia(s), "$Tag/MediaCardWide", mediaActions)
                        }
                    })
                    else -> null
                },
                cardHeight = wideCardHeight,
                cardTag = if (driveCard) "DriveCard" else "MediaCardWide",
            )

            else -> LandscapeMPaneArea(
                mapScreenSpecFor(group), hud(false), actions, Tag, areaWidth, areaHeight, scales,
                cardTag = if (driveCard) "DriveCard" else "MediaCard",
                cardWidthOverride = if (driveCard) driveCardWidth(group, areaHeight) else null,
                card = if (mode == HomeMediaMode.Card) {
                    { cardHeight ->
                        if (driveCard) {
                            driveContent(driveCardWidth(group, cardHeight), cardHeight, false)
                        } else {
                            val cardWidth = PenKit.MediaCard.W * (cardHeight / PenKit.MediaCard.H)
                            HomeMediaCard(media, onMediaClick, onMediaSessionSelect, Modifier.width(cardWidth).height(cardHeight)) { s ->
                                KitMediaCard(
                                    s.source?.label.orEmpty(), kitMedia(s).title, kitMedia(s).artist, kitMedia(s).art, kitMedia(s).elapsed, kitMedia(s).total,
                                    s.progress, s.isPlaying, height = cardHeight,
                                    onPrevious = mediaActions.onPrevious, onPlayPause = mediaActions.onPlayPause, onNext = mediaActions.onNext,
                                    canSkipPrevious = s.controls.canSkipPrevious, canPlayPause = s.controls.canPlayPause, canSkipNext = s.controls.canSkipNext,
                                )
                            }
                        }
                    }
                } else {
                    null
                },
            )
        }
        }
    }
}

/** The Landscape S pane draws no scaling of its own: it gets the tier's. */
@Composable
private fun SmallScales(scales: org.wpfy.carlauncher.design.theme.OverlayScales, content: @Composable () -> Unit) =
    org.wpfy.carlauncher.design.theme.ProvideOverlayScaling(scales, content)

/** One page per media source; swiping settles on a source and selects it. A single source has no pager. */
@Composable
private fun SessionPager(
    state: MediaState,
    onSessionSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    page: @Composable (MediaSourceSession) -> Unit,
) {
    val sessions = state.availableSessions.ifEmpty { listOfNotNull(state.selectedSession) }
    if (sessions.size <= 1) {
        Box(modifier) { sessions.firstOrNull()?.let { page(it) } }
        return
    }
    val ids = sessions.map(MediaSourceSession::id)
    val pager = rememberPagerState(initialPage = ids.indexOf(state.selectedSessionId).coerceAtLeast(0)) { sessions.size }
    val selected by rememberUpdatedState(state.selectedSessionId)
    LaunchedEffect(state.selectedSessionId, ids) {
        val index = ids.indexOf(state.selectedSessionId)
        if (index >= 0 && pager.currentPage != index) pager.scrollToPage(index)
    }
    LaunchedEffect(pager, ids) {
        snapshotFlow { pager.settledPage }.collect { index ->
            val id = ids.getOrNull(index) ?: return@collect
            if (id != selected) onSessionSelect(id)
        }
    }
    Box(modifier) {
        HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1) { index -> page(sessions[index]) }
    }
}

/**
 * The Drive Card slot: the card per source (swipe to select), a tap on it runs [onClick] (opens the media page or the
 * Media screen); with nothing playing, the flat card (the same tap).
 */
@Composable
private fun HomeDriveCardSlot(
    state: MediaState,
    onClick: () -> Unit,
    onSessionSelect: (String) -> Unit,
    modifier: Modifier,
    card: @Composable (MediaSourceSession?) -> Unit,
) {
    Box(modifier.testTag("$Tag/MediaCardSlot").clickable(onClick = onClick)) {
        if (!state.hasSession) card(null) else SessionPager(state, onSessionSelect, Modifier.fillMaxSize()) { card(it) }
    }
}

/** The Free media card slot: the kit card per source (swipe to select), tap to open Media, the source dots over its corner; with nothing playing, the idle card. */
@Composable
private fun HomeMediaCard(
    state: MediaState,
    onClick: () -> Unit,
    onSessionSelect: (String) -> Unit,
    modifier: Modifier,
    card: @Composable (MediaSourceSession) -> Unit,
) {
    Box(modifier.testTag("$Tag/MediaCardSlot").clickable(onClick = onClick)) {
        if (!state.hasSession) {
            Box(
                Modifier.fillMaxSize().clip(penShape(PenKit.MediaCard.Radius)).background(PenKit.MediaCard.Fill),
                contentAlignment = Alignment.Center,
            ) {
                KitStateMessage(
                    "Nothing playing", "Start playback in any media app",
                    Modifier.padding(horizontal = 12.dp), tag = "$Tag/MediaCardIdle", icon = PenIcons.MusicNote,
                )
            }
            return@Box
        }
        SessionPager(state, onSessionSelect, Modifier.fillMaxSize()) { card(it) }
        val sessions = state.availableSessions.ifEmpty { listOfNotNull(state.selectedSession) }
        val selectedIndex = sessions.indexOfFirst { it.id == state.selectedSessionId }.coerceAtLeast(0)
        MediaSourceDots(
            count = sessions.size, selectedIndex = selectedIndex,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 14.dp),
        )
    }
}
