package com.openlauncher.app.design.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.openlauncher.app.design.component.CarBadge
import com.openlauncher.app.design.component.CarGridItem
import com.openlauncher.app.design.component.CarHeader
import com.openlauncher.app.design.component.CarIconButton
import com.openlauncher.app.design.component.CarListRow
import com.openlauncher.app.design.component.CarRailButton
import com.openlauncher.app.design.component.CarSwitch
import com.openlauncher.app.design.theme.CarColors
import com.openlauncher.app.design.theme.CarTheme

@Preview(name = "Components 1600×720", widthDp = 1600, heightDp = 720)
@Preview(name = "Components 800×480", widthDp = 800, heightDp = 480)
@Composable
private fun ComponentCatalogPreview() {
    CarTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CarColors.Background)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CarHeader(
                title = "Settings",
                trailing = {
                    CarIconButton(
                        icon = Icons.Rounded.Search,
                        contentDescription = "Search",
                        onClick = {},
                        outlined = true,
                    )
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                CarRailButton(Icons.Rounded.Home, "Home", true, {})
                CarRailButton(Icons.Rounded.Notifications, "Notifications", false, {}, size = 56.dp, iconSize = 24.dp)
                CarBadge(text = "9+")
            }

            CarListRow(
                title = "Start on boot",
                subtitle = "Open the launcher after the head unit starts",
                trailing = { CarSwitch(checked = true, onCheckedChange = {}) },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                PreviewGridItem("Apps", Icons.Rounded.Apps, Modifier.weight(1f))
                PreviewGridItem("Settings", Icons.Rounded.Settings, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PreviewGridItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
) {
    CarGridItem(
        label = label,
        onClick = {},
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Color.White, androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = CarColors.Background,
            )
        }
    }
}
