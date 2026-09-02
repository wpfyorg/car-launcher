package com.openlauncher.app.feature.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.openlauncher.app.data.settings.StartupScreen
import com.openlauncher.app.data.settings.TextSizePreset
import com.openlauncher.app.design.component.CarHeader
import com.openlauncher.app.design.component.CarIconButton
import com.openlauncher.app.design.component.CarListRow
import com.openlauncher.app.design.component.CarSwitch
import com.openlauncher.app.design.theme.CarColors
import com.openlauncher.app.design.theme.CarSpacing
import com.openlauncher.app.distribution.DistributionFeatures
import com.openlauncher.app.feature.navigation.offline.OfflineMapRegion
import com.openlauncher.app.feature.navigation.offline.OfflineMapRegionState
import com.openlauncher.app.launcher.LauncherApp
import kotlin.math.roundToInt

@Composable
fun SettingsRoute(stateHolder: SettingsStateHolder) {
    val context = LocalContext.current
    val diagnostics = remember(context) { DeviceDiagnostics.from(context) }
    val isDefaultHome = remember(context) { isDefaultHome(context) }
    val notificationAccess = remember(context) { notificationAccessEnabled(context) }

    SettingsScreen(
        state = stateHolder.uiState,
        diagnostics = diagnostics,
        isDefaultHome = isDefaultHome,
        notificationAccess = notificationAccess,
        onOpenHomeSettings = { openSystemSettings(context, Settings.ACTION_HOME_SETTINGS) },
        onStartOnBootChange = stateHolder::setStartOnBoot,
        onStartupScreenChange = stateHolder::setStartupScreen,
        onTextSizeChange = stateHolder::setTextSizePreset,
        onPreferredMediaAppChange = stateHolder::setPreferredMediaApp,
        onNavigationAppChange = stateHolder::setNavigationApp,
        onCompatibilityModeChange = stateHolder::setNavigationCompatibilityMode,
        onOfflineMapDownload = stateHolder::downloadOfflineMap,
        onOfflineMapCancel = stateHolder::cancelOfflineMapDownload,
        onOfflineMapDelete = stateHolder::deleteOfflineMap,
        onCopyDiagnostics = { copyDiagnostics(context, diagnostics.copyText) },
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    diagnostics: DeviceDiagnostics,
    isDefaultHome: Boolean,
    notificationAccess: Boolean,
    onOpenHomeSettings: () -> Unit,
    onStartOnBootChange: (Boolean) -> Unit,
    onStartupScreenChange: (StartupScreen) -> Unit,
    onTextSizeChange: (TextSizePreset) -> Unit,
    onPreferredMediaAppChange: (LauncherApp?) -> Unit,
    onNavigationAppChange: (LauncherApp?) -> Unit,
    onCompatibilityModeChange: (Boolean) -> Unit,
    onOfflineMapDownload: (String) -> Unit,
    onOfflineMapCancel: (String) -> Unit,
    onOfflineMapDelete: (String) -> Unit,
    onCopyDiagnostics: () -> Unit,
) {
    var panel by rememberSaveable { mutableStateOf(SettingsPanel.Main.name) }
    val activePanel = runCatching { SettingsPanel.valueOf(panel) }.getOrDefault(SettingsPanel.Main)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        when (activePanel) {
            SettingsPanel.Main -> SettingsList(
                state = state,
                diagnostics = diagnostics,
                isDefaultHome = isDefaultHome,
                notificationAccess = notificationAccess,
                onOpenHomeSettings = onOpenHomeSettings,
                onStartOnBootChange = onStartOnBootChange,
                onOpenStartupScreen = { panel = SettingsPanel.StartupScreen.name },
                onOpenTextSize = { panel = SettingsPanel.TextSize.name },
                onOpenMediaApp = { panel = SettingsPanel.MediaApp.name },
                onOpenNavigationApp = { panel = SettingsPanel.NavigationApp.name },
                onOpenOfflineMaps = { panel = SettingsPanel.OfflineMaps.name },
                onCompatibilityModeChange = onCompatibilityModeChange,
                onCopyDiagnostics = onCopyDiagnostics,
            )

            SettingsPanel.StartupScreen -> OptionPicker(
                title = "Startup screen",
                options = StartupScreen.entries.map { it.label to (it == state.settings.startupScreen) },
                onBack = { panel = SettingsPanel.Main.name },
                onSelected = { index ->
                    onStartupScreenChange(StartupScreen.entries[index])
                    panel = SettingsPanel.Main.name
                },
            )

            SettingsPanel.TextSize -> OptionPicker(
                title = "Text size",
                options = TextSizePreset.entries.map { it.label to (it == state.settings.textSizePreset) },
                onBack = { panel = SettingsPanel.Main.name },
                onSelected = { index ->
                    onTextSizeChange(TextSizePreset.entries[index])
                    panel = SettingsPanel.Main.name
                },
            )

            SettingsPanel.MediaApp -> AppPicker(
                title = "Preferred media app",
                apps = state.apps,
                selectedAppKey = state.settings.preferredMediaAppKey,
                onBack = { panel = SettingsPanel.Main.name },
                onSelected = { app ->
                    onPreferredMediaAppChange(app)
                    panel = SettingsPanel.Main.name
                },
            )

            SettingsPanel.NavigationApp -> AppPicker(
                title = "Navigation app",
                apps = state.apps,
                selectedAppKey = state.settings.navigationAppKey,
                onBack = { panel = SettingsPanel.Main.name },
                onSelected = { app ->
                    onNavigationAppChange(app)
                    panel = SettingsPanel.Main.name
                },
            )

            SettingsPanel.OfflineMaps -> OfflineMapsManager(
                regions = state.offlineMapRegions,
                onBack = { panel = SettingsPanel.Main.name },
                onDownload = onOfflineMapDownload,
                onCancel = onOfflineMapCancel,
                onDelete = onOfflineMapDelete,
            )
        }
    }
}

@Composable
private fun SettingsList(
    state: SettingsUiState,
    diagnostics: DeviceDiagnostics,
    isDefaultHome: Boolean,
    notificationAccess: Boolean,
    onOpenHomeSettings: () -> Unit,
    onStartOnBootChange: (Boolean) -> Unit,
    onOpenStartupScreen: () -> Unit,
    onOpenTextSize: () -> Unit,
    onOpenMediaApp: () -> Unit,
    onOpenNavigationApp: () -> Unit,
    onOpenOfflineMaps: () -> Unit,
    onCompatibilityModeChange: (Boolean) -> Unit,
    onCopyDiagnostics: () -> Unit,
) {
    val settings = state.settings
    val mediaLabel = appLabel(state.apps, settings.preferredMediaAppKey) ?: "Choose an app"
    val navigationLabel = appLabel(state.apps, settings.navigationAppKey) ?: "Choose an app"

    Column(
        modifier = Modifier
            .widthIn(max = SettingsContentWidth)
            .fillMaxSize(),
    ) {
        CarHeader(title = "Settings")
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = CarSpacing.Xl),
        ) {
            section("Launcher")
            settingRow(
                title = "Home app",
                subtitle = if (isDefaultHome) {
                    "Car Launcher is the default Home app"
                } else {
                    "Choose Car Launcher as your Home app"
                },
                onClick = onOpenHomeSettings,
                showChevron = true,
            )
            settingRow(
                title = "Start on boot",
                subtitle = "Open the launcher after Android finishes booting",
                onClick = { onStartOnBootChange(!settings.startOnBoot) },
                trailing = {
                    CarSwitch(
                        checked = settings.startOnBoot,
                        onCheckedChange = onStartOnBootChange,
                    )
                },
            )
            settingRow(
                title = "Startup screen",
                subtitle = settings.startupScreen.label,
                onClick = onOpenStartupScreen,
                showChevron = true,
            )
            settingRow(
                title = "Pinned apps",
                subtitle = "${state.pinnedAppCount} pinned · manage from the app drawer",
            )

            section("Appearance")
            settingRow(title = "Theme", subtitle = "Dark")
            settingRow(
                title = "Text size",
                subtitle = settings.textSizePreset.label,
                onClick = onOpenTextSize,
                showChevron = true,
            )

            section("Media")
            settingRow(
                title = "Notification access",
                subtitle = if (notificationAccess) {
                    "Granted"
                } else {
                    "Not configured · required for media controls and notifications"
                },
            )
            settingRow(
                title = "Preferred media app",
                subtitle = mediaLabel,
                onClick = onOpenMediaApp,
                showChevron = true,
            )

            section("Navigation")
            if (DistributionFeatures.supportsExternalNavigation) {
                settingRow(
                    title = "External navigation app",
                    subtitle = navigationLabel,
                    onClick = onOpenNavigationApp,
                    showChevron = true,
                )
                settingRow(
                    title = "Embedded navigation",
                    subtitle = if (settings.navigationAppKey == null) {
                        "Choose a navigation app first"
                    } else {
                        "Head-unit compatibility check pending"
                    },
                )
            }
            if (DistributionFeatures.supportsOfflineMaps) {
                settingRow(
                    title = "Offline maps",
                    subtitle = offlineMapsSummary(state.offlineMapRegions),
                    onClick = onOpenOfflineMaps,
                    showChevron = true,
                )
            }
            if (DistributionFeatures.supportsNavigationCompatibilityMode) {
                settingRow(
                    title = "Compatibility mode",
                    subtitle = "Fallback for head units that need legacy embedding behavior",
                    onClick = { onCompatibilityModeChange(!settings.navigationCompatibilityMode) },
                    trailing = {
                        CarSwitch(
                            checked = settings.navigationCompatibilityMode,
                            onCheckedChange = onCompatibilityModeChange,
                        )
                    },
                )
            }

            section("System / diagnostics")
            settingRow(title = "Car Launcher", subtitle = "Version ${diagnostics.appVersion}")
            settingRow(title = "Android", subtitle = diagnostics.android)
            settingRow(title = "Architecture", subtitle = diagnostics.abi)
            settingRow(title = "Display", subtitle = diagnostics.display)
            settingRow(title = "Permissions", subtitle = diagnostics.permissions)
            settingRow(
                title = "Copy diagnostics",
                subtitle = "Copy device and launcher details to the clipboard",
                onClick = onCopyDiagnostics,
                showChevron = true,
            )
        }
    }
}

@Composable
private fun OfflineMapsManager(
    regions: List<OfflineMapRegion>,
    onBack: () -> Unit,
    onDownload: (String) -> Unit,
    onCancel: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = SettingsContentWidth)
            .fillMaxSize(),
    ) {
        SettingsSubpageHeader(title = "Offline maps", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = CarSpacing.Lg),
        ) {
            if (regions.isEmpty()) {
                item(key = "offline-maps-unavailable") {
                    settingRowContent(
                        title = "Map downloads are not available yet",
                        subtitle = "The download manager shell is ready; the native map catalog and downloader are not connected in this build.",
                    )
                }
            } else {
                items(items = regions, key = OfflineMapRegion::id) { region ->
                    settingRowContent(
                        title = region.name,
                        subtitle = offlineMapRegionSubtitle(region),
                        trailing = offlineMapAction(
                            region = region,
                            onDownload = onDownload,
                            onCancel = onCancel,
                            onDelete = onDelete,
                        ),
                    )
                }
            }
        }
    }
}

private fun offlineMapsSummary(regions: List<OfflineMapRegion>): String {
    val downloaded = regions.count { it.state == OfflineMapRegionState.Downloaded }
    return when {
        regions.isEmpty() -> "Download manager shell · backend not connected"
        downloaded == 0 -> "No regions downloaded"
        downloaded == 1 -> "1 region downloaded"
        else -> "$downloaded regions downloaded"
    }
}

private fun offlineMapRegionSubtitle(region: OfflineMapRegion): String {
    val size = region.estimatedSizeBytes?.let(::formatBytes)
    val state = when (region.state) {
        OfflineMapRegionState.NotDownloaded -> "Not downloaded"
        OfflineMapRegionState.Queued -> "Queued"
        OfflineMapRegionState.Downloading -> "${(region.progress.coerceIn(0f, 1f) * 100).roundToInt()}% downloaded"
        OfflineMapRegionState.Downloaded -> "Downloaded"
        OfflineMapRegionState.Failed -> region.errorMessage ?: "Download failed"
    }
    return listOfNotNull(region.detail, size, state).joinToString(" · ")
}

private fun offlineMapAction(
    region: OfflineMapRegion,
    onDownload: (String) -> Unit,
    onCancel: (String) -> Unit,
    onDelete: (String) -> Unit,
): @Composable () -> Unit = {
    when (region.state) {
        OfflineMapRegionState.NotDownloaded,
        OfflineMapRegionState.Failed,
        -> TextButton(onClick = { onDownload(region.id) }) { Text("Download") }

        OfflineMapRegionState.Queued,
        OfflineMapRegionState.Downloading,
        -> TextButton(onClick = { onCancel(region.id) }) { Text("Cancel") }

        OfflineMapRegionState.Downloaded ->
            TextButton(onClick = { onDelete(region.id) }) { Text("Delete") }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1_024L) return "$bytes B"
    val kib = bytes / 1_024.0
    if (kib < 1_024.0) return "${kib.roundToInt()} KB"
    val mib = kib / 1_024.0
    if (mib < 1_024.0) return "${mib.roundToInt()} MB"
    return "${((mib / 1_024.0) * 10).roundToInt() / 10.0} GB"
}

@Composable
private fun OptionPicker(
    title: String,
    options: List<Pair<String, Boolean>>,
    onBack: () -> Unit,
    onSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = SettingsContentWidth)
            .fillMaxSize(),
    ) {
        SettingsSubpageHeader(title = title, onBack = onBack)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(options.size) { index ->
                val (label, selected) = options[index]
                settingRowContent(
                    title = label,
                    onClick = { onSelected(index) },
                    trailing = if (selected) selectedIcon() else null,
                )
            }
        }
    }
}

@Composable
private fun AppPicker(
    title: String,
    apps: List<LauncherApp>,
    selectedAppKey: String?,
    onBack: () -> Unit,
    onSelected: (LauncherApp?) -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = SettingsContentWidth)
            .fillMaxSize(),
    ) {
        SettingsSubpageHeader(title = title, onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = CarSpacing.Lg),
        ) {
            item(key = "none") {
                settingRowContent(
                    title = "None",
                    subtitle = "Do not use a preferred app",
                    onClick = { onSelected(null) },
                    trailing = if (selectedAppKey == null) selectedIcon() else null,
                )
            }
            items(items = apps, key = LauncherApp::stableKey) { app ->
                settingRowContent(
                    title = app.label,
                    subtitle = app.packageName,
                    onClick = { onSelected(app) },
                    trailing = if (app.stableKey == selectedAppKey) selectedIcon() else null,
                )
            }
        }
    }
}

@Composable
private fun SettingsSubpageHeader(title: String, onBack: () -> Unit) {
    CarHeader(
        title = title,
        leading = {
            CarIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
            )
        },
    )
}

private fun LazyListScope.section(title: String) {
    item(key = "section-$title") {
        Text(
            text = title,
            modifier = Modifier.padding(
                start = CarSpacing.Lg,
                top = CarSpacing.Xl,
                bottom = CarSpacing.Sm,
            ),
            color = CarColors.AccentMuted,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private fun LazyListScope.settingRow(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    item(key = "row-$title") {
        settingRowContent(
            title = title,
            subtitle = subtitle,
            onClick = onClick,
            trailing = trailing ?: if (showChevron) chevron() else null,
        )
    }
}

@Composable
private fun settingRowContent(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        CarListRow(
            title = title,
            subtitle = subtitle,
            onClick = onClick,
            trailing = trailing,
        )
        HorizontalDivider(
            modifier = Modifier.padding(start = CarSpacing.Lg),
            thickness = 1.dp,
            color = CarColors.Outline,
        )
    }
}

private fun chevron(): @Composable () -> Unit = {
    Icon(
        imageVector = Icons.Rounded.ChevronRight,
        contentDescription = null,
        tint = CarColors.TextSecondary,
    )
}

private fun selectedIcon(): @Composable () -> Unit = {
    Icon(
        imageVector = Icons.Rounded.Check,
        contentDescription = "Selected",
        tint = CarColors.AccentMuted,
    )
}

private fun appLabel(apps: List<LauncherApp>, appKey: String?): String? {
    if (appKey == null) return null
    return apps.firstOrNull { it.stableKey == appKey }?.label ?: "Selected app unavailable"
}

data class DeviceDiagnostics(
    val appVersion: String,
    val android: String,
    val abi: String,
    val display: String,
    val permissions: String,
) {
    val copyText: String = listOf(
        "Car Launcher $appVersion",
        android,
        "ABI: $abi",
        "Display: $display",
        "Permissions: $permissions",
    ).joinToString("\n")

    companion object {
        @Suppress("DEPRECATION")
        fun from(context: Context): DeviceDiagnostics {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS,
            )
            val metrics = realDisplayMetrics(context)
            val requestedPermissions = packageInfo.requestedPermissions.orEmpty()
            val grantedPermissions = requestedPermissions.count { permission ->
                context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
            }
            return DeviceDiagnostics(
                appVersion = packageInfo.versionName.orEmpty(),
                android = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                abi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty(),
                display = "${metrics.widthPixels} × ${metrics.heightPixels} · ${metrics.densityDpi} dpi",
                permissions = "$grantedPermissions/${requestedPermissions.size} manifest permissions granted",
            )
        }
    }
}

private enum class SettingsPanel {
    Main,
    StartupScreen,
    TextSize,
    MediaApp,
    NavigationApp,
    OfflineMaps,
}

private val SettingsContentWidth = 1032.dp

private fun isDefaultHome(context: Context): Boolean {
    val homeIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
    val resolved = context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
    return resolved?.activityInfo?.packageName == context.packageName
}

private fun notificationAccessEnabled(context: Context): Boolean {
    val enabledListeners = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners",
    ).orEmpty()
    return enabledListeners
        .split(':')
        .any { component -> component.substringBefore('/').equals(context.packageName, ignoreCase = true) }
}

private fun openSystemSettings(context: Context, action: String) {
    runCatching { context.startActivity(Intent(action)) }
        .recoverCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
}

private fun copyDiagnostics(context: Context, diagnostics: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Car Launcher diagnostics", diagnostics))
    Toast.makeText(context, "Diagnostics copied", Toast.LENGTH_SHORT).show()
}

@Suppress("DEPRECATION")
private fun realDisplayMetrics(context: Context): DisplayMetrics {
    return DisplayMetrics().also { metrics ->
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getRealMetrics(metrics)
    }
}
