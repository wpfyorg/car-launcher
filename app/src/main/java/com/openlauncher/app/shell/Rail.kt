package com.openlauncher.app.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.openlauncher.app.design.component.CarRailButton
import com.openlauncher.app.design.theme.CarColors
import com.openlauncher.app.design.theme.CarDimensions
import com.openlauncher.app.design.theme.CarShapes
import com.openlauncher.app.design.theme.CarSpacing

enum class RailOrientation {
    Horizontal,
    Vertical,
}

@Composable
fun Rail(
    selected: ShellDestination,
    onDestinationSelected: (ShellDestination) -> Unit,
    notificationCount: Int = 0,
    onAssistantClick: () -> Unit = {},
    orientation: RailOrientation = RailOrientation.Vertical,
    modifier: Modifier = Modifier,
) {
    if (orientation == RailOrientation.Horizontal) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = CarSpacing.Md, vertical = CarSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppLauncherButton(selected, onDestinationSelected)
                CarRailButton(
                    icon = Icons.Rounded.Mic,
                    contentDescription = "Digital assistant",
                    selected = false,
                    onClick = onAssistantClick,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NotificationRailButton(
                    count = notificationCount,
                    selected = selected == ShellDestination.Notifications,
                    onClick = { onDestinationSelected(ShellDestination.Notifications) },
                )
                StatusArea()
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxHeight()
                .padding(vertical = CarSpacing.Md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppLauncherButton(selected, onDestinationSelected)
                CarRailButton(
                    icon = Icons.Rounded.Mic,
                    contentDescription = "Digital assistant",
                    selected = false,
                    onClick = onAssistantClick,
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                NotificationRailButton(
                    count = notificationCount,
                    selected = selected == ShellDestination.Notifications,
                    onClick = { onDestinationSelected(ShellDestination.Notifications) },
                )
                StatusArea()
            }
        }
    }
}

@Composable
private fun AppLauncherButton(
    selected: ShellDestination,
    onDestinationSelected: (ShellDestination) -> Unit,
) {
    CarRailButton(
        icon = if (selected == ShellDestination.Apps) Icons.Rounded.Home else Icons.Rounded.Apps,
        contentDescription = if (selected == ShellDestination.Apps) "Go home" else "Open app drawer",
        selected = selected == ShellDestination.Home || selected == ShellDestination.Apps,
        onClick = {
            onDestinationSelected(
                if (selected == ShellDestination.Apps) ShellDestination.Home else ShellDestination.Apps,
            )
        },
    )
}

@Composable
private fun NotificationRailButton(
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(modifier = Modifier.size(CarDimensions.RailActionSize)) {
        CarRailButton(
            icon = Icons.Rounded.Notifications,
            contentDescription = "Notifications",
            selected = selected,
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
        )
        if (count > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .background(CarColors.AccentMuted, CarShapes.extraLarge),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (count > 9) "9+" else count.toString(),
                    color = Color(0xFF002C6F),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
