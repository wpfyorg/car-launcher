package org.wpfy.carlauncher.feature.media

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import org.wpfy.carlauncher.design.adapters.kitMedia
import org.wpfy.carlauncher.design.kit.KitStateMessage
import org.wpfy.carlauncher.design.kit.PenIcons
import org.wpfy.carlauncher.design.kit.ScreenGroup
import org.wpfy.carlauncher.design.kit.penShape
import org.wpfy.carlauncher.design.kit.screenGroupFor
import org.wpfy.carlauncher.design.screens.KitDockState
import org.wpfy.carlauncher.design.screens.KitMediaActions
import org.wpfy.carlauncher.design.screens.KitMediaState
import org.wpfy.carlauncher.design.screens.LandscapeLMediaScreen
import org.wpfy.carlauncher.design.screens.LandscapeMMediaScreen
import org.wpfy.carlauncher.design.screens.LandscapeSMediaScreen
import org.wpfy.carlauncher.design.screens.LandscapeXLMediaScreen
import org.wpfy.carlauncher.design.screens.PortraitMediaScreen
import org.wpfy.carlauncher.design.screens.PortraitScreenSpec
import org.wpfy.carlauncher.design.screens.SystemScreenSpec
import org.wpfy.carlauncher.design.screens.UltrawideLMediaScreen
import org.wpfy.carlauncher.design.screens.UltrawideMMediaScreen
import org.wpfy.carlauncher.design.theme.CarSpacing
import org.wpfy.carlauncher.design.theme.LocalCarWindow

/*
 * The live media screen: the Pen kit media screen of the window's screen group (the pane only: the
 * shell draws the dock), fed by the media state. Play/pause, previous/next (as far as the session
 * offers them), opening the source app, swiping between sources and the album art are the live
 * behaviour; the access and idle states have no Pen frame.
 */

@Composable
fun MediaRoute(
    state: MediaState,
    controller: MediaSessionController,
    onSelectSession: (String) -> Unit,
) {
    val context = LocalContext.current
    Media(
        state = state,
        onSelectSession = onSelectSession,
        onRequestAccess = {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        },
        onOpenSource = { controller.openSource(context) },
        onPlayPause = controller::playPause,
        onPrevious = controller::previous,
        onNext = controller::next,
        onSeek = controller::seekTo,
    )
}

@Composable
fun Media(
    state: MediaState,
    onSelectSession: (String) -> Unit,
    onRequestAccess: () -> Unit,
    onOpenSource: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    onSeek: ((Long) -> Unit)? = null,
) {
    val group = screenGroupFor(LocalCarWindow.current)
    when {
        state.accessStatus == MediaAccessStatus.PermissionRequired -> MediaStateBody(
            group = group,
            title = "Allow notification access",
            message = "Car Launcher needs notification access to show and control what’s playing.",
            icon = PenIcons.Settings,
            actionLabel = "Open notification access",
            onAction = onRequestAccess,
            modifier = modifier,
        )

        !state.hasSession -> MediaStateBody(
            group = group,
            title = "Nothing playing",
            message = "Start playback in Spotify, YouTube Music, or another media app.",
            icon = PenIcons.MusicNote,
            modifier = modifier,
        )

        else -> MediaPager(
            state = state,
            group = group,
            onSelectSession = onSelectSession,
            onSeek = onSeek,
            actions = KitMediaActions(onPrevious, onPlayPause, onNext, onSource = onOpenSource),
            modifier = modifier,
        )
    }
}

/** The permission and idle states: a message in the group's pane. */
@Composable
private fun MediaStateBody(
    group: ScreenGroup,
    title: String,
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val spec = SystemScreenSpec.forGroup(group)
    Box(
        modifier.fillMaxSize().testTag("Screen/Pane")
            .clip(penShape(spec.paneRadius))
            .background(spec.paneFill),
    ) {
        KitStateMessage(title, message, Modifier.padding(horizontal = CarSpacing.Xl), tag = "Screen/Pane/State", icon = icon, actionLabel = actionLabel, onAction = onAction)
    }
}

/** One page per media source; swiping settles on a source and selects it. */
@Composable
private fun MediaPager(
    state: MediaState,
    group: ScreenGroup,
    onSelectSession: (String) -> Unit,
    onSeek: ((Long) -> Unit)?,
    actions: KitMediaActions,
    modifier: Modifier,
) {
    val sessions = state.availableSessions.ifEmpty { listOfNotNull(state.selectedSession) }
    if (sessions.isEmpty()) return

    val sessionIds = sessions.map(MediaSourceSession::id)
    val pagerState = rememberPagerState(initialPage = sessionIds.indexOf(state.selectedSessionId).coerceAtLeast(0)) { sessions.size }
    val currentSelectedSessionId = rememberUpdatedState(state.selectedSessionId)

    LaunchedEffect(state.selectedSessionId, sessionIds) {
        val selectedIndex = sessionIds.indexOf(state.selectedSessionId)
        if (selectedIndex >= 0 && pagerState.currentPage != selectedIndex) pagerState.scrollToPage(selectedIndex)
    }
    LaunchedEffect(pagerState, sessionIds) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val id = sessionIds.getOrNull(page) ?: return@collect
            if (id != currentSelectedSessionId.value) onSelectSession(id)
        }
    }

    val spec = SystemScreenSpec.forGroup(group)
    Box(modifier.fillMaxSize().clip(penShape(spec.paneRadius))) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize().testTag("Screen/Pager"), beyondViewportPageCount = 1) { page ->
            val session = sessions[page]
            // The art keeps its identity while the position ticks: the backdrop is cached per bitmap.
            val art = remember(session.artwork) { session.artwork?.asImageBitmap() }
            val icon = remember(session.source?.icon) { session.source?.icon?.asImageBitmap() }
            val media = kitMedia(session) { bitmap -> if (bitmap === session.artwork) art!! else icon!! }
            // The page's controls act on its own source: it is selected first when another one is.
            val select = { if (session.id != currentSelectedSessionId.value) onSelectSession(session.id) }
            val pageActions = KitMediaActions(
                onPrevious = { select(); actions.onPrevious() },
                onPlayPause = { select(); actions.onPlayPause() },
                onNext = { select(); actions.onNext() },
                onSource = actions.onSource?.let { open -> { select(); open() } },
                onSeek = onSeek?.let { seek -> { fraction -> select(); seek(seekPositionMs(fraction, session.durationMs)) } },
            )
            MediaScreenOf(group, media, pageActions)
        }
        MediaSourceDots(
            count = sessions.size,
            selectedIndex = pagerState.currentPage,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = CarSpacing.Xl, end = CarSpacing.Xl),
        )
    }
}

/** The kit media screen of [group], its pane only. */
@Composable
private fun MediaScreenOf(group: ScreenGroup, media: KitMediaState, actions: KitMediaActions) {
    when (group) {
        ScreenGroup.LandscapeS -> LandscapeSMediaScreen(NoDock, media, mediaActions = actions, showDock = false, fitPane = true)
        ScreenGroup.LandscapeM -> LandscapeMMediaScreen(NoDock, media, mediaActions = actions, showDock = false, fitPane = true)
        ScreenGroup.UltrawideM -> UltrawideMMediaScreen(NoDock, media, mediaActions = actions, showDock = false, fitPane = true)
        ScreenGroup.LandscapeL -> LandscapeLMediaScreen(NoDock, media, mediaActions = actions, showDock = false, fitPane = true)
        ScreenGroup.UltrawideL -> UltrawideLMediaScreen(NoDock, media, mediaActions = actions, showDock = false, fitPane = true)
        ScreenGroup.LandscapeXL -> LandscapeXLMediaScreen(NoDock, media, mediaActions = actions, showDock = false, fitPane = true)
        ScreenGroup.PortraitS -> PortraitMediaScreen(PortraitScreenSpec.PortraitS, NoDock, media, mediaActions = actions, showDock = false, fitPane = true)
        ScreenGroup.PortraitL -> PortraitMediaScreen(PortraitScreenSpec.PortraitL, NoDock, media, mediaActions = actions, showDock = false, fitPane = true)
    }
}

private val NoDock = KitDockState("", 0)
