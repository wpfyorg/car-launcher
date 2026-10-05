package org.wpfy.carlauncher.feature.navigation.external

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.wpfy.carlauncher.design.kit.KitMapControl
import org.wpfy.carlauncher.design.screens.KitSearchState
import org.wpfy.carlauncher.feature.home.MapAccessory
import org.wpfy.carlauncher.feature.navigation.NavigationState
import org.wpfy.carlauncher.launcher.LauncherApp

/** Open a chosen external navigation app using Android public APIs. */
@Composable
fun DistributionNavigationHomeContent(
    app: LauncherApp?,
    @Suppress("UNUSED_PARAMETER") compatibilityMode: Boolean,
    @Suppress("UNUSED_PARAMETER") expanded: Boolean = false,
    @Suppress("UNUSED_PARAMETER") onExpandedChange: (Boolean) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onNavigationStateChange: (NavigationState) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onStopNavigationHandlerChange: ((() -> Unit)?) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onMapAccessoryChange: (MapAccessory?) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onMapControlsChange: (List<KitMapControl>) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onSearchChange: (KitSearchState?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (app != null) {
            Button(onClick = {
                val intent = Intent.makeMainActivity(app.component)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
            }) { Text("Open ${app.label}") }
        }
    }
}
