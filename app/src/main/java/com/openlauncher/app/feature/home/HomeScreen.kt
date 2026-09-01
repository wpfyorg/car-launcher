package com.openlauncher.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AltRoute
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.openlauncher.app.design.theme.CarColors
import com.openlauncher.app.design.theme.CarShapes
import com.openlauncher.app.design.theme.CarSpacing
import com.openlauncher.app.feature.media.MediaState

@Composable
fun HomeRoute(
    mediaState: MediaState,
    onMediaClick: () -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaPrevious: () -> Unit,
    onMediaNext: () -> Unit,
) {
    HomeScreen(
        state = HomeUiState(media = mediaState),
        onMediaClick = onMediaClick,
        onMediaPlayPause = onMediaPlayPause,
        onMediaPrevious = onMediaPrevious,
        onMediaNext = onMediaNext,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onSearchClick: () -> Unit = {},
    onStopNavigation: () -> Unit = {},
    onMediaClick: () -> Unit = {},
    onMediaPlayPause: () -> Unit = {},
    onMediaPrevious: () -> Unit = {},
    onMediaNext: () -> Unit = {},
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isPortraitFamily = maxWidth < 880.dp && maxHeight >= 560.dp
        val contextWidth = when {
            maxWidth >= 1700.dp -> 600.dp
            maxWidth >= 1150.dp -> 420.dp
            else -> 304.dp
        }

        if (isPortraitFamily) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = CarSpacing.Md, end = CarSpacing.Md, bottom = CarSpacing.Md),
                verticalArrangement = Arrangement.spacedBy(CarSpacing.Md),
            ) {
                NavigationSurface(
                    state = state.navigation,
                    onSearchClick = onSearchClick,
                    onStopNavigation = onStopNavigation,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )

                MediaCard(
                    state = state.media,
                    onClick = onMediaClick,
                    onPlayPause = onMediaPlayPause,
                    onPrevious = onMediaPrevious,
                    onNext = onMediaNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(196.dp),
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = CarSpacing.Md, end = CarSpacing.Md, bottom = CarSpacing.Md),
                horizontalArrangement = Arrangement.spacedBy(CarSpacing.Md),
            ) {
                NavigationSurface(
                    state = state.navigation,
                    onSearchClick = onSearchClick,
                    onStopNavigation = onStopNavigation,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )

                ContextColumn(
                    media = state.media,
                    onMediaClick = onMediaClick,
                    onMediaPlayPause = onMediaPlayPause,
                    onMediaPrevious = onMediaPrevious,
                    onMediaNext = onMediaNext,
                    width = contextWidth,
                )
            }
        }
    }
}

@Composable
private fun NavigationSurface(
    state: HomeNavigationState,
    onSearchClick: () -> Unit,
    onStopNavigation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(CarShapes.medium)
            .background(CarColors.Background),
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .size(56.dp),
            shape = CircleShape,
            color = CarColors.AccentStrong,
            shadowElevation = 8.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Navigation,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = Color.White,
                )
            }
        }

        when (state) {
            is HomeNavigationState.Active -> {
                TurnCard(
                    state = state,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                )
                EtaCard(
                    state = state,
                    onStopNavigation = onStopNavigation,
                    onSearchClick = onSearchClick,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                )
            }

            HomeNavigationState.Parked -> NavigationStatusCard(
                icon = Icons.Rounded.Navigation,
                title = "Ready to drive",
                body = "Choose a destination to start navigation",
                actionLabel = "Search destination",
                onAction = onSearchClick,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
            )

            HomeNavigationState.GpsUnavailable -> NavigationStatusCard(
                icon = Icons.Rounded.LocationOff,
                title = "GPS unavailable",
                body = "Waiting for a location fix",
                actionLabel = "Try again",
                onAction = onSearchClick,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
            )

            HomeNavigationState.NavigationUnavailable -> NavigationStatusCard(
                icon = Icons.Rounded.AltRoute,
                title = "Navigation app unavailable",
                body = "Choose or install a compatible navigation app",
                actionLabel = "Navigation settings",
                onAction = onSearchClick,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
            )

            HomeNavigationState.Arrived -> NavigationStatusCard(
                icon = Icons.Rounded.CheckCircle,
                title = "You’ve arrived",
                body = "Route guidance has ended",
                actionLabel = "Search destination",
                onAction = onSearchClick,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun TurnCard(
    state: HomeNavigationState.Active,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(386.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFF006A54),
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp),
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Rounded.Navigation,
                contentDescription = null,
                modifier = Modifier.size(46.dp),
                tint = Color.White,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.distanceToTurn,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = state.street,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFF015141),
                    shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
                )
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Then",
                color = CarColors.TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
            )
            Icon(
                imageVector = Icons.Rounded.Navigation,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = CarColors.TextPrimary,
            )
            Text(
                text = state.nextTurnLabel,
                color = CarColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EtaCard(
    state: HomeNavigationState.Active,
    onStopNavigation: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.width(377.dp),
        color = CarColors.Surface,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CarColors.Outline),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = state.duration,
                color = Color(0xFF5BBF6B),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "${state.distanceRemaining} · ${state.arrivalTime}",
                color = CarColors.TextSecondary,
                style = MaterialTheme.typography.bodyLarge,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(CarColors.Outline),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TonalActionButton(Icons.Rounded.Close, "Stop navigation", onStopNavigation, wide = true)
                TonalActionButton(Icons.Rounded.AltRoute, "Routes", {})
                TonalActionButton(Icons.Rounded.Search, "Search", onSearchClick)
                TonalActionButton(Icons.Rounded.Place, "Places", {})
            }
        }
    }
}

@Composable
private fun NavigationStatusCard(
    icon: ImageVector,
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.width(377.dp),
        color = CarColors.Surface,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CarColors.Outline),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = CarColors.AccentMuted,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = CarColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = body,
                        color = CarColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Surface(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
                color = CarColors.SurfaceElevated,
                shape = RoundedCornerShape(36.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = CarColors.TextPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = actionLabel,
                        color = CarColors.TextPrimary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun ContextColumn(
    media: MediaState,
    onMediaClick: () -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaPrevious: () -> Unit,
    onMediaNext: () -> Unit,
    width: Dp,
) {
    Column(
        modifier = Modifier
            .width(width)
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Top,
    ) {
        MediaCard(
            state = media,
            onClick = onMediaClick,
            onPlayPause = onMediaPlayPause,
            onPrevious = onMediaPrevious,
            onNext = onMediaNext,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun MediaCard(
    state: MediaState,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = Color(0xFF0B5968),
        shape = CarShapes.medium,
    ) {
        if (state.hasSession) {
            PlayingMediaContent(
                state = state,
                onPlayPause = onPlayPause,
                onPrevious = onPrevious,
                onNext = onNext,
            )
        } else {
            IdleMediaContent()
        }
    }
}

@Composable
private fun IdleMediaContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(88.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.12f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = Color.White,
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Nothing playing",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "Open Media to choose something to play",
            color = Color.White.copy(alpha = 0.76f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun PlayingMediaContent(
    state: MediaState,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title.ifBlank { "Unknown track" },
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = state.artist.ifBlank { state.source?.label.orEmpty() },
                    color = Color.White.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            state.source?.icon?.let { icon ->
                Image(
                    bitmap = icon.asImageBitmap(),
                    contentDescription = state.source.label,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(state.progress.coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(Color.White, RoundedCornerShape(10.dp)),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MediaControl(
                icon = Icons.Rounded.SkipPrevious,
                contentDescription = "Previous",
                onClick = onPrevious,
                enabled = state.controls.canSkipPrevious,
            )
            Surface(
                onClick = onPlayPause,
                enabled = state.controls.canPlayPause,
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = Color(0xFFAEEBFF),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(34.dp),
                        tint = Color(0xFF191C18),
                    )
                }
            }
            MediaControl(
                icon = Icons.Rounded.SkipNext,
                contentDescription = "Next",
                onClick = onNext,
                enabled = state.controls.canSkipNext,
            )
        }
    }
}

@Composable
private fun MediaControl(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(40.dp),
            tint = Color.White.copy(alpha = if (enabled) 1f else 0.35f),
        )
    }
}

@Composable
private fun TonalActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    wide: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(if (wide) 88.dp else 56.dp)
            .height(56.dp),
        color = if (wide) CarColors.SurfaceElevated else Color.Transparent,
        shape = RoundedCornerShape(56.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(32.dp),
                tint = CarColors.TextPrimary,
            )
        }
    }
}
