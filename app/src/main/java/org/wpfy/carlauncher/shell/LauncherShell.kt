package org.wpfy.carlauncher.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.wpfy.carlauncher.design.theme.CarColors
import org.wpfy.carlauncher.design.theme.CarSpacing
import org.wpfy.carlauncher.design.theme.CarWindow
import org.wpfy.carlauncher.design.theme.LocalCarWindow
import org.wpfy.carlauncher.design.component.CarHeader
import org.wpfy.carlauncher.data.settings.RailPosition

@Composable
fun LauncherShell(
    notificationCount: Int = 0,
    onAssistantClick: () -> Unit = {},
    railPosition: RailPosition = RailPosition.Left,
    state: ShellState = rememberShellState(),
    content: @Composable BoxScope.(ShellDestination, (ShellDestination) -> Unit) -> Unit = { destination, _ ->
        PlaceholderDestination(destination)
    },
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(CarColors.Background)
            .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout)),
    ) {
        val window = remember(maxWidth, maxHeight) { CarWindow(maxWidth.value, maxHeight.value) }
        val side = railSideFor(railPosition, window)
        // The kit dock of the window's screen group: its width on a side, its height at the bottom.
        val dockSize = dockThickness(side, window)

        CompositionLocalProvider(LocalCarWindow provides window) {
            // The destination content and the rail keep fixed positions in the composition. Moving
            // the rail (setting change, rotation) only changes bounds, so embedded surfaces are
            // resized in place instead of being detached and rebuilt.
            val bottomRailInset = if (side == RailSide.Bottom) dockSize else 0.dp
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // The bottom rail and margin already lift content off the bottom edge, so
                    // imePadding() below must only add whatever the keyboard covers beyond them.
                    .consumeWindowInsets(PaddingValues(bottom = bottomRailInset + CarSpacing.Md))
                    .padding(
                        start = if (side == RailSide.Left) dockSize else 0.dp,
                        end = if (side == RailSide.Right) dockSize else 0.dp,
                        bottom = bottomRailInset,
                    )
                    .padding(CarSpacing.Md),
            ) {
                key(state.destination) {
                    content(state.destination, state::navigateTo)
                }
            }

            LiveDock(
                selected = state.destination,
                onDestinationSelected = state::navigateTo,
                side = side,
                window = window,
                notificationCount = notificationCount,
                onAssistantClick = onAssistantClick,
                modifier = when (side) {
                    RailSide.Left -> Modifier.align(Alignment.CenterStart)
                    RailSide.Right -> Modifier.align(Alignment.CenterEnd)
                    RailSide.Bottom -> Modifier.align(Alignment.BottomCenter)
                },
            )
        }

    }
}

internal enum class RailSide { Left, Right, Bottom }

internal fun railSideFor(preference: RailPosition, window: CarWindow): RailSide {
    return when {
        window.effectiveRailBottom(prefersBottom = preference == RailPosition.Bottom) -> RailSide.Bottom
        preference == RailPosition.Right -> RailSide.Right
        else -> RailSide.Left
    }
}

@Composable
fun PlaceholderDestination(destination: ShellDestination) {
    Column(modifier = Modifier.fillMaxSize()) {
        CarHeader(title = destination.label)
    }
}
