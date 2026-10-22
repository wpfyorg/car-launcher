package org.wpfy.carlauncher.feature.settings

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import android.os.Build
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import org.wpfy.carlauncher.core.platform.startOnBootRowMode
import org.wpfy.carlauncher.core.diagnostics.DeviceDiagnostics
import org.wpfy.carlauncher.core.diagnostics.DiagnosticsLogShare
import org.wpfy.carlauncher.data.settings.AppTheme
import org.wpfy.carlauncher.data.settings.NavigationCardPosition
import org.wpfy.carlauncher.data.settings.OverlaySize
import org.wpfy.carlauncher.data.settings.RailPosition
import org.wpfy.carlauncher.data.settings.SpeedUnitSetting
import org.wpfy.carlauncher.data.settings.CameraAlertDistance
import org.wpfy.carlauncher.data.settings.OverspeedWarning
import org.wpfy.carlauncher.data.settings.rememberUnitRegionLocale
import org.wpfy.carlauncher.data.settings.resolveUnitSystem
import org.wpfy.carlauncher.data.settings.StartupScreen
import org.wpfy.carlauncher.data.settings.speedUnitSummary
import org.wpfy.carlauncher.data.settings.TextSizePreset
import org.wpfy.carlauncher.data.settings.VoiceVolume
import org.wpfy.carlauncher.design.adapters.BluetoothRow
import org.wpfy.carlauncher.design.adapters.RailPickerHint
import org.wpfy.carlauncher.design.adapters.SettingsActions
import org.wpfy.carlauncher.design.adapters.SettingsFeatures
import org.wpfy.carlauncher.design.adapters.SettingsInput
import org.wpfy.carlauncher.design.adapters.SettingsSection
import org.wpfy.carlauncher.design.adapters.aboutLines
import org.wpfy.carlauncher.design.adapters.appLabel
import org.wpfy.carlauncher.design.adapters.kitAppChoiceOptions
import org.wpfy.carlauncher.design.adapters.kitChoiceOptions
import org.wpfy.carlauncher.design.adapters.SpeedCameraAttribution
import org.wpfy.carlauncher.design.adapters.kitOfflineMapEntries
import org.wpfy.carlauncher.design.adapters.kitSpeedCameraEntries
import org.wpfy.carlauncher.design.adapters.kitSettingsCategories
import org.wpfy.carlauncher.design.adapters.kitSettingsEntries
import org.wpfy.carlauncher.design.adapters.overlaySizeSubtitle
import org.wpfy.carlauncher.design.adapters.ref
import org.wpfy.carlauncher.design.kit.screenGroupFor
import org.wpfy.carlauncher.design.screens.OfflineMapsScreen
import org.wpfy.carlauncher.design.screens.SettingsAppChoiceScreen
import org.wpfy.carlauncher.design.screens.SettingsChoiceScreen
import org.wpfy.carlauncher.design.screens.SettingsListScreen
import org.wpfy.carlauncher.design.screens.SystemScreenSpec
import org.wpfy.carlauncher.design.theme.LocalCarWindow
import org.wpfy.carlauncher.distribution.DistributionFeatures
import org.wpfy.carlauncher.feature.media.BluetoothLink
import org.wpfy.carlauncher.launcher.LauncherApp

/*
 * The live settings: the Pen kit settings list of the window's screen group (the pane only: the shell
 * draws the dock), with the choice pickers the rows open, fed by the settings state. Every row, choice,
 * app picker and the offline maps manager are the live behaviour; the kit draws them.
 */

@Composable
fun SettingsRoute(
    stateHolder: SettingsStateHolder,
    /** The Bluetooth phone link, from the media state. */
    bluetoothLink: BluetoothLink = BluetoothLink(),
    /** Called after the Bluetooth permission may have changed, so the link monitor re-reads it. */
    onBluetoothPermissionChanged: () -> Unit = {},
) {
    val context = LocalContext.current
    var diagnostics by remember(context) {
        mutableStateOf(DeviceDiagnostics.from(context))
    }
    var diagnosticsCaptureGeneration by remember(context) { mutableStateOf(0) }
    var voices by remember(context) { mutableStateOf(listOf(SystemDefaultVoice)) }
    // Voices are installed or removed in system settings, so they load again each time the screen resumes.
    var voiceReload by remember(context) { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    var homeApp by remember(context) { mutableStateOf(isDefaultHome(context)) }
    var notificationAccess by remember(context) { mutableStateOf(notificationAccessEnabled(context)) }
    var bluetoothGranted by remember(context) { mutableStateOf(bluetoothPermissionGranted(context)) }
    var bluetoothRequested by rememberSaveable { mutableStateOf(false) }
    val bluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        bluetoothGranted = granted
        onBluetoothPermissionChanged()
    }
    val currentOnBluetoothPermissionChanged by rememberUpdatedState(onBluetoothPermissionChanged)
    // These are changed in system settings, so read them again each time the screen resumes.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                homeApp = isDefaultHome(context)
                notificationAccess = notificationAccessEnabled(context)
                bluetoothGranted = bluetoothPermissionGranted(context)
                currentOnBluetoothPermissionChanged()
                voiceReload++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val importOfflineMap = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(stateHolder::importOfflineMap)
    }

    LaunchedEffect(context, voiceReload) {
        if (DistributionFeatures.supportsVoiceGuidance) voices = voiceChoices(loadSystemVoices(context))
    }

    LaunchedEffect(context) {
        val requestGeneration = ++diagnosticsCaptureGeneration
        val freshDiagnostics = DeviceDiagnostics.capture(context)
        if (requestGeneration == diagnosticsCaptureGeneration) {
            diagnostics = freshDiagnostics
        }
    }

    Settings(
        state = stateHolder.uiState,
        diagnostics = diagnostics,
        isDefaultHome = homeApp,
        notificationAccess = notificationAccess,
        bluetooth = BluetoothRow(permissionNeeded = !bluetoothGranted, link = bluetoothLink),
        onBluetooth = {
            // After a request the system may not show the dialog again; the app's details are the only way left.
            val permanentlyDenied = bluetoothRequested &&
                context.findActivity()?.shouldShowRequestPermissionRationale(BluetoothConnectPermission) == false
            if (permanentlyDenied) {
                openAppDetails(context)
            } else {
                bluetoothRequested = true
                bluetoothLauncher.launch(BluetoothConnectPermission)
            }
        },
        onOpenHomeSettings = { openSystemSettings(context, Settings.ACTION_HOME_SETTINGS) },
        onStartOnBootChange = stateHolder::setStartOnBoot,
        onStartupScreenChange = stateHolder::setStartupScreen,
        onTextSizeChange = stateHolder::setTextSizePreset,
        onAppThemeChange = stateHolder::setAppTheme,
        onPreferredMediaAppChange = stateHolder::setPreferredMediaApp,
        onNavigationAppChange = stateHolder::setNavigationApp,
        onNavigationCardPositionChange = stateHolder::setNavigationCardPosition,
        onRailPositionChange = stateHolder::setRailPosition,
        onOverlaySizeChange = stateHolder::setOverlaySize,
        onCompatibilityModeChange = stateHolder::setNavigationCompatibilityMode,
        onVoiceGuidanceChange = { enabled -> stateHolder.setVoiceGuidanceMuted(!enabled) },
        onVoiceVolumeChange = stateHolder::setVoiceVolume,
        onVoiceChange = stateHolder::setVoiceName,
        onSpeedUnitChange = stateHolder::setSpeedUnit,
        onCameraAlertDistanceChange = stateHolder::setCameraAlertDistance,
        onOverspeedWarningChange = stateHolder::setOverspeedWarning,
        voices = voices,
        onOfflineMapDownload = stateHolder::downloadOfflineMap,
        onOfflineMapCancel = stateHolder::cancelOfflineMapDownload,
        onOfflineMapDelete = stateHolder::deleteOfflineMap,
        onOfflineMapImport = { importOfflineMap.launch(OfflineMapImportMimeTypes) },
        onSpeedCameraDownload = stateHolder::downloadSpeedCameraRegion,
        onSpeedCameraDelete = stateHolder::deleteSpeedCameraRegion,
        onSpeedCamerasOpen = stateHolder::refreshSpeedCameraRegions,
        onCopyDiagnostics = {
            scope.launch {
                val requestGeneration = ++diagnosticsCaptureGeneration
                val freshDiagnostics = DeviceDiagnostics.capture(context)
                if (requestGeneration == diagnosticsCaptureGeneration) {
                    diagnostics = freshDiagnostics
                }
                copyDiagnostics(context, freshDiagnostics.copyText)
            }
        },
        onShareDiagnosticLogs = {
            scope.launch {
                val requestGeneration = ++diagnosticsCaptureGeneration
                val freshDiagnostics = DeviceDiagnostics.capture(context)
                if (requestGeneration == diagnosticsCaptureGeneration) {
                    diagnostics = freshDiagnostics
                }
                DiagnosticsLogShare.share(context, freshDiagnostics.copyText)
                    .onFailure {
                        Toast.makeText(context, "Unable to share diagnostic logs", Toast.LENGTH_SHORT).show()
                    }
            }
        },
        iconLoader = stateHolder::loadIcon,
    )
}

internal val OfflineMapImportMimeTypes = arrayOf(
    "application/zip",
    "application/octet-stream",
    "application/x-zip-compressed",
    "*/*",
)

@Composable
fun Settings(
    state: SettingsUiState,
    diagnostics: DeviceDiagnostics,
    isDefaultHome: Boolean,
    notificationAccess: Boolean,
    onOpenHomeSettings: () -> Unit,
    onStartOnBootChange: (Boolean) -> Unit,
    onStartupScreenChange: (StartupScreen) -> Unit,
    onTextSizeChange: (TextSizePreset) -> Unit,
    onAppThemeChange: (AppTheme) -> Unit,
    onPreferredMediaAppChange: (LauncherApp?) -> Unit,
    onNavigationAppChange: (LauncherApp?) -> Unit,
    onNavigationCardPositionChange: (NavigationCardPosition) -> Unit,
    onRailPositionChange: (RailPosition) -> Unit,
    onOverlaySizeChange: (OverlaySize) -> Unit = {},
    onCompatibilityModeChange: (Boolean) -> Unit,
    onOfflineMapDownload: (String) -> Unit,
    onOfflineMapCancel: (String) -> Unit,
    onOfflineMapDelete: (String) -> Unit,
    onOfflineMapImport: () -> Unit,
    onCopyDiagnostics: () -> Unit,
    onShareDiagnosticLogs: () -> Unit,
    modifier: Modifier = Modifier,
    bluetooth: BluetoothRow = BluetoothRow(),
    onBluetooth: () -> Unit = {},
    onVoiceGuidanceChange: (Boolean) -> Unit = {},
    onVoiceVolumeChange: (VoiceVolume) -> Unit = {},
    onVoiceChange: (String?) -> Unit = {},
    onSpeedUnitChange: (SpeedUnitSetting) -> Unit = {},
    onSpeedCameraDownload: (String) -> Unit = {},
    onSpeedCameraDelete: (String) -> Unit = {},
    onSpeedCamerasOpen: () -> Unit = {},
    onCameraAlertDistanceChange: (CameraAlertDistance) -> Unit = {},
    onOverspeedWarningChange: (OverspeedWarning) -> Unit = {},
    /** The voices the voice picker lists, the system default first. */
    voices: List<VoiceChoice> = listOf(SystemDefaultVoice),
    iconLoader: suspend (LauncherApp) -> ImageBitmap? = { null },
    features: SettingsFeatures = distributionSettingsFeatures(),
) {
    val spec = SystemScreenSpec.forGroup(screenGroupFor(LocalCarWindow.current))
    var panel by rememberSaveable { mutableStateOf(SettingsPanel.Main.name) }
    var section by rememberSaveable { mutableStateOf(SettingsSection.Launcher.name) }
    val activePanel = restoredSettingsPanel(panel, features)
    val activeSection = runCatching { SettingsSection.valueOf(section) }.getOrDefault(SettingsSection.Launcher)
    val settings = state.settings
    val units = resolveUnitSystem(settings.speedUnit, rememberUnitRegionLocale())
    val fill = modifier.fillMaxSize()
    val back = { panel = SettingsPanel.Main.name }

    /** An option picker: choosing saves and returns to the list; Back returns without a change. */
    @Composable
    fun <T> Choice(title: String, values: List<T>, current: T, label: (T) -> String, onChange: (T) -> Unit, hint: String? = null) {
        SettingsChoiceScreen(
            spec = spec,
            title = title,
            options = kitChoiceOptions(values, current, label) {
                onChange(it)
                back()
            },
            modifier = fill,
            onBack = back,
            hint = hint,
            showDock = false,
            lazy = true,
        )
    }

    val apps = state.apps.map(LauncherApp::ref)
    val navigationApps = state.navigationApps.map(LauncherApp::ref)
    val icons = remember { mutableStateMapOf<String, ImageBitmap>() }

    /** An app picker: the first row is "None"; icons load as rows come into view. */
    @Composable
    fun AppChoice(title: String, list: List<LauncherApp>, selectedKey: String?, onChange: (LauncherApp?) -> Unit) {
        val byKey = list.associateBy(LauncherApp::stableKey)
        SettingsAppChoiceScreen(
            spec = spec,
            title = title,
            options = kitAppChoiceOptions(
                noneTitle = "None",
                noneSub = "Do not use a preferred app",
                apps = list.map(LauncherApp::ref),
                selectedKey = selectedKey,
                icon = { icons[it.key] },
                onSelect = { ref ->
                    onChange(ref?.let { byKey[it.key] })
                    back()
                },
                loadIcon = { ref ->
                    suspend { if (ref.key !in icons) byKey[ref.key]?.let { app -> iconLoader(app)?.let { icons[ref.key] = it } } }
                },
            ),
            modifier = fill,
            onBack = back,
            showDock = false,
            lazy = true,
        )
    }

    when (activePanel) {
        SettingsPanel.Main -> {
            val input = SettingsInput(
                settings = settings,
                features = features,
                about = diagnostics.aboutLines(),
                isDefaultHome = isDefaultHome,
                startOnBootMode = startOnBootRowMode(Build.VERSION.SDK_INT, isDefaultHome),
                notificationAccess = notificationAccess,
                bluetooth = bluetooth,
                pinnedAppCount = state.pinnedAppCount,
                mediaAppLabel = appLabel(apps, settings.preferredMediaAppKey),
                navigationAppLabel = appLabel(apps, settings.navigationAppKey),
                offlineMapRegions = state.offlineMapRegions,
                speedCameraRegions = state.speedCameraRegions,
                bottomDockOnly = spec.portrait != null,
                voiceLabel = currentVoiceChoice(voices, settings.voiceName).label,
                speedUnitSummary = speedUnitSummary(settings.speedUnit, rememberUnitRegionLocale()),
                unitSystem = units,
            )
            val actions = SettingsActions(
                onHomeApp = onOpenHomeSettings,
                onStartOnBoot = onStartOnBootChange,
                onStartupScreen = { panel = SettingsPanel.StartupScreen.name },
                onTheme = { panel = SettingsPanel.Theme.name },
                onTextSize = { panel = SettingsPanel.TextSize.name },
                onRailPosition = { panel = SettingsPanel.RailPosition.name },
                onOverlaySize = { panel = SettingsPanel.OverlaySize.name },
                onBluetooth = onBluetooth,
                onMediaApp = { panel = SettingsPanel.MediaApp.name },
                onNavigationApp = { panel = SettingsPanel.NavigationApp.name },
                onNavigationCardPosition = { panel = SettingsPanel.NavigationCards.name },
                onOfflineMaps = { panel = SettingsPanel.OfflineMaps.name },
                onSpeedCameras = {
                    panel = SettingsPanel.SpeedCameras.name
                    onSpeedCamerasOpen()
                },
                onCompatibilityMode = onCompatibilityModeChange,
                onVoiceGuidance = onVoiceGuidanceChange,
                onSpeedUnit = { panel = SettingsPanel.SpeedUnit.name },
                onCameraAlertDistance = { panel = SettingsPanel.CameraAlertDistance.name },
                onOverspeedWarning = { panel = SettingsPanel.OverspeedWarning.name },
                onVoiceVolume = { panel = SettingsPanel.VoiceVolume.name },
                onVoice = { panel = SettingsPanel.Voice.name },
                onCopyDiagnostics = onCopyDiagnostics,
                onShareDiagnosticLogs = onShareDiagnosticLogs,
            )
            // Two panes (the sections beside the chosen one's rows) where the group's Pen frame has them; else one list.
            val twoPane = spec.categories != null
            SettingsListScreen(
                spec = spec,
                title = "Settings",
                entries = kitSettingsEntries(input, actions, only = if (twoPane) activeSection else null),
                modifier = fill,
                categoryList = if (twoPane) kitSettingsCategories(activeSection) { section = it.name } else emptyList(),
                showDock = false,
                lazy = true,
            )
        }

        SettingsPanel.StartupScreen -> Choice("Startup screen", StartupScreen.entries, settings.startupScreen, StartupScreen::label, onStartupScreenChange)
        SettingsPanel.TextSize -> Choice("Text size", TextSizePreset.entries, settings.textSizePreset, TextSizePreset::label, onTextSizeChange)
        SettingsPanel.Theme -> Choice("Theme", AppTheme.entries, settings.appTheme, AppTheme::label, onAppThemeChange)
        SettingsPanel.NavigationCards -> Choice(
            "Navigation card position", NavigationCardPosition.entries, settings.navigationCardPosition, NavigationCardPosition::label, onNavigationCardPositionChange,
        )
        SettingsPanel.RailPosition -> Choice("Navigation rail", RailPosition.entries, settings.railPosition, RailPosition::label, onRailPositionChange, RailPickerHint)
        SettingsPanel.OverlaySize -> Choice("Overlay size", OverlaySize.entries, settings.overlaySize, { it.overlaySizeSubtitle() }, onOverlaySizeChange)
        SettingsPanel.SpeedUnit -> Choice("Speed unit", SpeedUnitSetting.entries, settings.speedUnit, SpeedUnitSetting::label, onSpeedUnitChange)
        SettingsPanel.CameraAlertDistance -> Choice(
            "Camera alert distance", CameraAlertDistance.entries, settings.cameraAlertDistance, { it.label(units) }, onCameraAlertDistanceChange,
        )
        SettingsPanel.OverspeedWarning -> Choice(
            "Overspeed warning", OverspeedWarning.entries, settings.overspeedWarning, { it.label(units) }, onOverspeedWarningChange,
        )
        SettingsPanel.VoiceVolume -> Choice("Guidance volume", VoiceVolume.entries, settings.voiceVolume, VoiceVolume::label, onVoiceVolumeChange)
        SettingsPanel.Voice -> Choice(
            "Guidance voice", voices, currentVoiceChoice(voices, settings.voiceName), VoiceChoice::label, { onVoiceChange(it.name) },
        )
        SettingsPanel.MediaApp -> AppChoice("Preferred media app", state.apps, settings.preferredMediaAppKey, onPreferredMediaAppChange)
        SettingsPanel.NavigationApp -> AppChoice("Navigation app", state.navigationApps, settings.navigationAppKey, onNavigationAppChange)
        SettingsPanel.OfflineMaps -> OfflineMapsScreen(
            spec = spec,
            title = "Offline maps",
            importLabel = "Import",
            entries = kitOfflineMapEntries(
                regions = state.offlineMapRegions,
                importError = state.offlineMapImportError,
                onDownload = onOfflineMapDownload,
                onCancel = onOfflineMapCancel,
                onDelete = onOfflineMapDelete,
            ),
            modifier = fill,
            onBack = back,
            onImport = onOfflineMapImport,
            showDock = false,
            lazy = true,
        )
        SettingsPanel.SpeedCameras -> OfflineMapsScreen(
            spec = spec,
            title = "Speed cameras",
            importLabel = null,
            entries = kitSpeedCameraEntries(
                regions = state.speedCameraRegions,
                onDownload = onSpeedCameraDownload,
                onDelete = onSpeedCameraDelete,
            ),
            modifier = fill,
            onBack = back,
            showDock = false,
            lazy = true,
            hint = SpeedCameraAttribution,
        )
    }
}

/** What this build flavor offers. */
internal fun distributionSettingsFeatures() = SettingsFeatures(
    themeSelection = DistributionFeatures.supportsThemeSelection,
    externalNavigation = DistributionFeatures.supportsExternalNavigation,
    offlineMaps = DistributionFeatures.supportsOfflineMaps,
    navigationCompatibilityMode = DistributionFeatures.supportsNavigationCompatibilityMode,
    voiceGuidance = DistributionFeatures.supportsVoiceGuidance,
    speedCameraAlerts = DistributionFeatures.supportsSpeedCameraAlerts,
)

/** The panel a saved [name] resumes at: the main list when it is unknown or this build no longer offers it (an update can). */
internal fun restoredSettingsPanel(name: String, features: SettingsFeatures): SettingsPanel {
    val panel = runCatching { SettingsPanel.valueOf(name) }.getOrNull() ?: return SettingsPanel.Main
    val offered = when (panel) {
        SettingsPanel.Theme -> features.themeSelection
        SettingsPanel.NavigationApp -> features.externalNavigation
        SettingsPanel.OfflineMaps -> features.offlineMaps
        SettingsPanel.SpeedCameras -> features.speedCameraAlerts
        SettingsPanel.VoiceVolume, SettingsPanel.Voice -> features.voiceGuidance
        SettingsPanel.CameraAlertDistance, SettingsPanel.OverspeedWarning -> features.speedCameraAlerts
        else -> true
    }
    return if (offered) panel else SettingsPanel.Main
}

internal enum class SettingsPanel {
    Main,
    StartupScreen,
    Theme,
    TextSize,
    MediaApp,
    NavigationApp,
    NavigationCards,
    RailPosition,
    OverlaySize,
    SpeedUnit,
    CameraAlertDistance,
    OverspeedWarning,
    VoiceVolume,
    Voice,
    OfflineMaps,
    SpeedCameras,
}

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

/** Manifest.permission.BLUETOOTH_CONNECT as text: the constant needs API 31, and the permission is asked only from there. */
private const val BluetoothConnectPermission = "android.permission.BLUETOOTH_CONNECT"

/** The permission to read the Bluetooth phone's name: asked at run time from API 31, granted at install before that. */
private fun bluetoothPermissionGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        context.checkSelfPermission(BluetoothConnectPermission) == PackageManager.PERMISSION_GRANTED

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current != null) {
        if (current is Activity) return current
        current = (current as? ContextWrapper)?.baseContext
    }
    return null
}

private fun openAppDetails(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    runCatching { context.startActivity(intent) }
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
