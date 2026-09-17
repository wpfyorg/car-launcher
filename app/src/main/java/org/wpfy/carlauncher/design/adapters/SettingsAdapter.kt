package org.wpfy.carlauncher.design.adapters

import androidx.compose.ui.graphics.vector.ImageVector
import org.wpfy.carlauncher.core.platform.StartOnBootRowMode
import org.wpfy.carlauncher.core.diagnostics.DeviceDiagnostics
import org.wpfy.carlauncher.data.settings.LauncherSettings
import org.wpfy.carlauncher.design.kit.PenIcons
import org.wpfy.carlauncher.design.screens.SettingsCategory
import org.wpfy.carlauncher.design.screens.SettingsEntry
import androidx.compose.ui.graphics.ImageBitmap
import org.wpfy.carlauncher.data.settings.OverlaySize
import org.wpfy.carlauncher.data.settings.UnitSystem
import org.wpfy.carlauncher.design.screens.AppChoiceOption
import org.wpfy.carlauncher.design.screens.ChoiceOption
import org.wpfy.carlauncher.design.screens.OfflineMapAction
import org.wpfy.carlauncher.design.screens.OfflineMapEntry
import org.wpfy.carlauncher.feature.media.BluetoothLink
import org.wpfy.carlauncher.feature.media.BluetoothLinkState
import org.wpfy.carlauncher.feature.navigation.offline.OfflineMapRegion
import org.wpfy.carlauncher.feature.navigation.offline.OfflineMapRegionState
import org.wpfy.carlauncher.feature.navigation.speed.SpeedCameraFailureIntegrity
import org.wpfy.carlauncher.feature.navigation.speed.SpeedCameraRegion
import org.wpfy.carlauncher.feature.navigation.speed.SpeedCameraRegionState
import kotlin.math.roundToInt

/*
 * Live settings -> the kit's settings list: the same sections, rows, titles and subtitles as the live
 * screen, in the same order. Rows a distribution does not support are left out, as live. The kit has
 * link and switch rows only; a row that only shows a value is a link without an action (no chevron).
 */

/** The live screen's sections, with the Pen's node names and icons. */
enum class SettingsSection(val node: String, val title: String, val icon: ImageVector) {
    Launcher("Launcher", "Launcher", PenIcons.Home),
    Appearance("Appearance", "Appearance", PenIcons.Palette),
    Media("Media", "Media", PenIcons.MusicNote),
    Navigation("Navigation", "Navigation", PenIcons.Navigation),
    About("AboutAndDiagnostics", "About and diagnostics", PenIcons.Info),
}

/** The build flavor's switches (DistributionFeatures), passed in so both flavors can be tested. */
data class SettingsFeatures(
    val themeSelection: Boolean,
    val externalNavigation: Boolean,
    val offlineMaps: Boolean,
    val navigationCompatibilityMode: Boolean,
    /** Spoken turn-by-turn guidance (the integrated navigation of the Pro build). */
    val voiceGuidance: Boolean = false,
    /** Speed camera regions to download, the alerts and their warnings (the Pro build). */
    val speedCameraAlerts: Boolean = false,
)

/** The About lines of the live screen. */
data class AboutLines(val appVersion: String, val android: String, val abi: String, val display: String, val permissions: String)

/** Copies the device diagnostics displayed in the About settings section. */
fun DeviceDiagnostics.aboutLines() = AboutLines(appVersion, android, abi, display, permissions)

/** What the Bluetooth row shows: whether the permission is still to be granted, and the phone link. */
data class BluetoothRow(
    val permissionNeeded: Boolean = false,
    val link: BluetoothLink = BluetoothLink(),
)

/** Everything the rows show. App labels are resolved by the caller; null means none chosen. */
data class SettingsInput(
    val settings: LauncherSettings,
    val features: SettingsFeatures,
    val about: AboutLines,
    val isDefaultHome: Boolean = false,
    /** Android 10+ starts only the default Home app at boot; see [StartOnBootRowMode]. */
    val startOnBootMode: StartOnBootRowMode = StartOnBootRowMode.Standard,
    val notificationAccess: Boolean = false,
    val bluetooth: BluetoothRow = BluetoothRow(),
    val pinnedAppCount: Int = 0,
    val mediaAppLabel: String? = null,
    val navigationAppLabel: String? = null,
    val offlineMapRegions: List<OfflineMapRegion> = emptyList(),
    val speedCameraRegions: List<SpeedCameraRegion> = emptyList(),
    /** The window is portrait, where the dock is always along the bottom edge whatever side is chosen. */
    val bottomDockOnly: Boolean = false,
    /** The label of the chosen guidance voice. */
    val voiceLabel: String = "System default",
    /** The speed unit row's value ("Automatic · mph"). */
    val speedUnitSummary: String = "Automatic",
    /** The units camera alert distances and overspeed tolerances are shown in. */
    val unitSystem: UnitSystem = UnitSystem.Metric,
)

/** Taps on the rows; each takes the place of the live row's callback. */
class SettingsActions(
    val onHomeApp: () -> Unit = {},
    val onStartOnBoot: (Boolean) -> Unit = {},
    val onStartupScreen: () -> Unit = {},
    val onTheme: () -> Unit = {},
    val onTextSize: () -> Unit = {},
    val onRailPosition: () -> Unit = {},
    val onOverlaySize: () -> Unit = {},
    val onBluetooth: () -> Unit = {},
    val onMediaApp: () -> Unit = {},
    val onNavigationApp: () -> Unit = {},
    val onNavigationCardPosition: () -> Unit = {},
    val onOfflineMaps: () -> Unit = {},
    val onSpeedCameras: () -> Unit = {},
    val onCompatibilityMode: (Boolean) -> Unit = {},
    val onVoiceGuidance: (Boolean) -> Unit = {},
    val onVoiceVolume: () -> Unit = {},
    val onVoice: () -> Unit = {},
    val onSpeedUnit: () -> Unit = {},
    val onCameraAlertDistance: () -> Unit = {},
    val onOverspeedWarning: () -> Unit = {},
    val onCopyDiagnostics: () -> Unit = {},
    val onShareDiagnosticLogs: () -> Unit = {},
)

private const val ChooseAnApp = "Choose an app"
private const val StartOnBootSystemHomeBody = "Android starts the default Home app at boot · turn off to pick another Home app"
private const val StartOnBootNeedsHomeBody = "Android starts only the default Home app at boot · choose Car Launcher as Home"

/** The section list beside the rows of a two-pane screen, [selected] chosen. */
fun kitSettingsCategories(selected: SettingsSection, onSelect: (SettingsSection) -> Unit = {}): List<SettingsCategory> =
    SettingsSection.entries.map { section ->
        SettingsCategory(section.node, section.title, section.icon, selected = section == selected, onClick = { onSelect(section) })
    }

/**
 * The rows: every section under its header (one scrolling list), or only the rows of [only]
 * (the details of a two-pane screen).
 */
fun kitSettingsEntries(
    input: SettingsInput,
    actions: SettingsActions = SettingsActions(),
    only: SettingsSection? = null,
): List<SettingsEntry> {
    val sections = if (only == null) SettingsSection.entries else listOf(only)
    return sections.flatMap { section ->
        val rows = rowsOf(section, input, actions)
        if (only == null) listOf<SettingsEntry>(SettingsEntry.Section(section.node, section.title)) + rows else rows
    }
}

/** Builds a link row with an alphanumeric node name derived from [title]. */
private fun link(title: String, value: String, onClick: (() -> Unit)? = null) =
    SettingsEntry.Link(title.filter(Char::isLetterOrDigit), title, value, onClick)

/**
 * The Bluetooth row: asks for the permission while it is missing (the phone's name needs it), then
 * only shows the state.
 */
private fun bluetoothRow(row: BluetoothRow, actions: SettingsActions): SettingsEntry =
    if (row.permissionNeeded) {
        link("Bluetooth", "Off · allow to show the connected phone", actions.onBluetooth)
    } else {
        when (row.link.state) {
            BluetoothLinkState.Connected -> link("Bluetooth", "On · ${row.link.deviceName ?: "Phone"} connected")
            BluetoothLinkState.Disconnected -> link("Bluetooth", "On · no phone connected")
            // Android cannot be asked for the current link, so it is unknown until the next (dis)connection.
            BluetoothLinkState.Unknown -> link("Bluetooth", "On")
        }
    }

/**
 * The Start-on-boot row. Android 10+ starts only the default Home app at boot, so there the row
 * follows the system: on while this launcher is Home (turning it off opens Home settings), and
 * turning it on opens Home settings while another launcher is Home.
 */
private fun startOnBootRow(input: SettingsInput, actions: SettingsActions): SettingsEntry {
    val stored = input.settings.startOnBoot
    return when (input.startOnBootMode) {
        StartOnBootRowMode.Standard ->
            switch("Start on boot", "Open the launcher after Android finishes booting", stored, actions.onStartOnBoot)
        StartOnBootRowMode.SystemHome ->
            switch("Start on boot", StartOnBootSystemHomeBody, true) { enabled -> if (!enabled) actions.onHomeApp() }
        StartOnBootRowMode.NeedsDefaultHome ->
            switch("Start on boot", StartOnBootNeedsHomeBody, false) { enabled ->
                actions.onStartOnBoot(enabled)
                if (enabled) actions.onHomeApp()
            }
    }
}

/** Builds a switch row with an alphanumeric node name and the supplied change callback. */
private fun switch(title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit) =
    SettingsEntry.Switch(title.filter(Char::isLetterOrDigit), title, body, checked, onChange)

/** Builds one section’s rows with current values and callbacks, omitting unsupported navigation features. */
private fun rowsOf(section: SettingsSection, input: SettingsInput, actions: SettingsActions): List<SettingsEntry> {
    val settings = input.settings
    val features = input.features
    return when (section) {
        SettingsSection.Launcher -> listOf(
            link(
                "Home app",
                if (input.isDefaultHome) "Car Launcher is the default Home app" else "Choose Car Launcher as your Home app",
                actions.onHomeApp,
            ),
            startOnBootRow(input, actions),
            link("Startup screen", settings.startupScreen.label, actions.onStartupScreen),
            link("Pinned apps", "${input.pinnedAppCount} pinned · pin or unpin apps in Apps"),
        )

        SettingsSection.Appearance -> listOfNotNull(
            // Without a choice of skin there is nothing to show: a saved skin is not drawn either.
            if (features.themeSelection) link("Theme", settings.appTheme.label, actions.onTheme) else null,
            link("Text size", settings.textSizePreset.label, actions.onTextSize),
            link(
                "Navigation rail",
                if (input.bottomDockOnly) "${settings.railPosition.label} · portrait always uses the bottom dock" else settings.railPosition.label,
                actions.onRailPosition,
            ),
            link("Overlay size", settings.overlaySize.overlaySizeSubtitle(), actions.onOverlaySize),
        )

        SettingsSection.Media -> listOf(
            link(
                "Notification access",
                if (input.notificationAccess) "On" else "Off · needed for media controls and notifications",
            ),
            bluetoothRow(input.bluetooth, actions),
            link("Preferred media app", input.mediaAppLabel ?: ChooseAnApp, actions.onMediaApp),
        )

        SettingsSection.Navigation -> listOfNotNull(
            if (features.externalNavigation) {
                link("External navigation app", input.navigationAppLabel ?: ChooseAnApp, actions.onNavigationApp)
            } else {
                null
            },
            link("Navigation card position", settings.navigationCardPosition.label, actions.onNavigationCardPosition),
            link("Speed unit", input.speedUnitSummary, actions.onSpeedUnit),
            if (features.speedCameraAlerts) {
                link("Camera alert distance", settings.cameraAlertDistance.label(input.unitSystem), actions.onCameraAlertDistance)
            } else {
                null
            },
            if (features.speedCameraAlerts) {
                link("Overspeed warning", settings.overspeedWarning.label(input.unitSystem), actions.onOverspeedWarning)
            } else {
                null
            },
            if (features.voiceGuidance) {
                switch("Voice guidance", "Speak turn-by-turn instructions", !settings.voiceGuidanceMuted, actions.onVoiceGuidance)
            } else {
                null
            },
            if (features.voiceGuidance) link("Guidance volume", settings.voiceVolume.label, actions.onVoiceVolume) else null,
            if (features.voiceGuidance) link("Guidance voice", input.voiceLabel, actions.onVoice) else null,
            if (features.offlineMaps) {
                link("Offline maps", offlineMapsSummary(input.offlineMapRegions), actions.onOfflineMaps)
            } else {
                null
            },
            if (features.speedCameraAlerts) {
                link("Speed cameras", speedCamerasSummary(input.speedCameraRegions), actions.onSpeedCameras)
            } else {
                null
            },
            if (features.navigationCompatibilityMode) {
                switch(
                    "Compatibility mode",
                    "Try this if the navigation app doesn’t show inside the launcher",
                    settings.navigationCompatibilityMode,
                    actions.onCompatibilityMode,
                )
            } else {
                null
            },
        )

        SettingsSection.About -> listOf(
            link("Car Launcher", "Version ${input.about.appVersion}"),
            link("Android", input.about.android),
            link("Architecture", input.about.abi),
            link("Display", input.about.display),
            link("Permissions", input.about.permissions),
            link("Copy diagnostics", "Copy device and launcher details to the clipboard", actions.onCopyDiagnostics),
            link("Share diagnostic logs", "Share recent Car Launcher logs and device details as a text file", actions.onShareDiagnosticLogs),
        )
    }
}

// --- Pickers -----------------------------------------------------------------------------------

/** A picker's hint under the dock side options. */
const val RailPickerHint = "Portrait windows always use the bottom dock."

/** Explains automatic window scaling or returns the selected fixed size label. */
fun OverlaySize.overlaySizeSubtitle(): String =
    if (this == OverlaySize.Auto) "Auto · follows the window size" else label

/** Summarizes downloaded regions, prompting an import when no regions are listed. */
fun offlineMapsSummary(regions: List<OfflineMapRegion>): String {
    val downloaded = regions.count { it.state == OfflineMapRegionState.Downloaded }
    return when {
        regions.isEmpty() -> "None yet · import a map file"
        downloaded == 0 -> "No regions downloaded"
        downloaded == 1 -> "1 region downloaded"
        else -> "$downloaded regions downloaded"
    }
}

/** Summarizes the downloaded speed camera regions. */
fun speedCamerasSummary(regions: List<SpeedCameraRegion>): String {
    val downloaded = regions.count { it.state == SpeedCameraRegionState.Downloaded }
    return when (downloaded) {
        0 -> "None downloaded"
        1 -> "1 region"
        else -> "$downloaded regions"
    }
}

/** Resolves a selected app’s label, returning null for no selection or a fallback for a missing app. */
fun appLabel(apps: List<AppRef>, appKey: String?): String? {
    if (appKey == null) return null
    return apps.firstOrNull { it.key == appKey }?.label ?: "Selected app unavailable"
}

/** The options of a choice, [current] marked; each takes the place of the live row, whose text is its [label]. */
fun <T> kitChoiceOptions(values: List<T>, current: T, label: (T) -> String, onSelect: (T) -> Unit): List<ChoiceOption> =
    values.map { value ->
        ChoiceOption(label(value).filter(Char::isLetterOrDigit), label(value), null, selected = value == current) { onSelect(value) }
    }

/**
 * The options of an app choice: first the "none" row (the live "None"), then [apps]. [selectedKey] marks one (the
 * none row when null); icons come from the caller (null while loading) and [loadIcon] loads one while its row is composed.
 */
fun kitAppChoiceOptions(
    noneTitle: String,
    noneSub: String,
    apps: List<AppRef>,
    selectedKey: String?,
    icon: (AppRef) -> ImageBitmap? = { null },
    onSelect: (AppRef?) -> Unit = {},
    loadIcon: ((AppRef) -> suspend () -> Unit)? = null,
): List<AppChoiceOption> =
    listOf(AppChoiceOption("None", noneTitle, noneSub, selectedKey == null, null, PenIcons.Autorenew, { onSelect(null) })) +
        apps.mapIndexed { index, app ->
            AppChoiceOption(
                "App${index + 1}", app.label, app.packageName, app.key == selectedKey, icon(app),
                onClick = { onSelect(app) }, loadIcon = loadIcon?.invoke(app), key = app.key,
            )
        }

private const val OnThisDevice = "On this device"
private const val Available = "Available"
private const val NoMapsTitle = "No maps to download"
private const val NoMapsMessage = "Import an .olmap map file from USB or device storage"

/** Regions with something on the device (or on its way there) are listed first; the rest are available to download. */
private fun OfflineMapRegionState.onDevice(): Boolean = when (this) {
    OfflineMapRegionState.NotDownloaded, OfflineMapRegionState.Failed -> false
    else -> true
}

/**
 * The offline maps list: an import error first, then the regions under "On this device" and "Available" (a
 * section is left out when empty); no regions at all says so. The live subtitles and buttons: download (also
 * resume and update), cancel while it runs, delete once it is there.
 */
fun kitOfflineMapEntries(
    regions: List<OfflineMapRegion>,
    importError: String?,
    onDownload: (String) -> Unit = {},
    onCancel: (String) -> Unit = {},
    onDelete: (String) -> Unit = {},
): List<OfflineMapEntry> {
    val entries = mutableListOf<OfflineMapEntry>()
    if (importError != null) entries += OfflineMapEntry.Notice("ImportError", "Import failed", importError)
    if (regions.isEmpty()) {
        entries += OfflineMapEntry.Notice("NoMaps", NoMapsTitle, NoMapsMessage)
        return entries
    }
    fun region(r: OfflineMapRegion): OfflineMapEntry {
        val (action, verb, act) = when (r.state) {
            OfflineMapRegionState.NotDownloaded, OfflineMapRegionState.Failed -> Triple(OfflineMapAction.Download, "Download") { onDownload(r.id) }
            OfflineMapRegionState.Queued, OfflineMapRegionState.Downloading, OfflineMapRegionState.Installing ->
                Triple(OfflineMapAction.Cancel, "Cancel") { onCancel(r.id) }
            OfflineMapRegionState.Paused -> Triple(OfflineMapAction.Download, "Resume") { onDownload(r.id) }
            OfflineMapRegionState.Downloaded -> Triple(OfflineMapAction.Delete, "Delete") { onDelete(r.id) }
            OfflineMapRegionState.UpdateAvailable -> Triple(OfflineMapAction.Download, "Update") { onDownload(r.id) }
        }
        return OfflineMapEntry.Region(
            node = "Region-${r.id}", title = r.name, sub = offlineMapRegionSubtitle(r),
            progress = if (r.state == OfflineMapRegionState.Downloading) r.progress.coerceIn(0f, 1f) else null,
            action = action, actionDescription = "$verb ${r.name}", onAction = act,
        )
    }
    val (here, rest) = regions.partition { it.state.onDevice() }
    if (here.isNotEmpty()) entries += listOf<OfflineMapEntry>(OfflineMapEntry.Section("SectionOnThisDevice", OnThisDevice)) + here.map(::region)
    if (rest.isNotEmpty()) entries += listOf<OfflineMapEntry>(OfflineMapEntry.Section("SectionAvailable", Available)) + rest.map(::region)
    return entries
}

/** A region's line under its name: the detail, the size and the state, as live. */
fun offlineMapRegionSubtitle(region: OfflineMapRegion): String {
    val size = region.estimatedSizeBytes?.let(::formatBytes)
    val state = when (region.state) {
        OfflineMapRegionState.NotDownloaded -> "Not downloaded"
        OfflineMapRegionState.Queued -> "Queued"
        OfflineMapRegionState.Downloading -> "${(region.progress.coerceIn(0f, 1f) * 100).roundToInt()}% downloaded"
        OfflineMapRegionState.Paused -> "Paused"
        OfflineMapRegionState.Installing -> "Installing"
        OfflineMapRegionState.Downloaded -> "Downloaded"
        OfflineMapRegionState.UpdateAvailable -> "Update available"
        OfflineMapRegionState.Failed -> region.errorMessage ?: "Download failed"
    }
    return listOfNotNull(region.detail, size, state).joinToString(" · ")
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1_024L) return "$bytes B"
    val kib = bytes / 1_024.0
    if (kib < 1_024.0) return "${kib.roundToInt()} KB"
    val mib = kib / 1_024.0
    if (mib < 1_024.0) return "${mib.roundToInt()} MB"
    return "${((mib / 1_024.0) * 10).roundToInt() / 10.0} GB"
}

/** The line under the speed camera regions (the offline maps list's hint slot). */
const val SpeedCameraAttribution = "Speed camera data © OpenStreetMap contributors, ODbL"

private const val NoCamerasTitle = "No regions to download"
private const val NoCamerasMessage = "Connect to the internet to load the list of regions"

/** A region that is on the device, or on its way there, is listed under "On this device". */
private fun SpeedCameraRegionState.onDevice(): Boolean = when (this) {
    SpeedCameraRegionState.NotDownloaded, SpeedCameraRegionState.Failed -> false
    else -> true
}

/**
 * The speed camera list, in the offline maps list's rows: the regions under "On this device" and "Available" (a
 * section is left out when empty), the order the repository gave. The button downloads, or deletes once it is there;
 * while a region is on its way it cancels, which is a delete.
 */
fun kitSpeedCameraEntries(
    regions: List<SpeedCameraRegion>,
    onDownload: (String) -> Unit = {},
    onDelete: (String) -> Unit = {},
): List<OfflineMapEntry> {
    if (regions.isEmpty()) return listOf(OfflineMapEntry.Notice("NoRegions", NoCamerasTitle, NoCamerasMessage))
    fun region(r: SpeedCameraRegion): OfflineMapEntry {
        val (action, verb, act) = when (r.state) {
            SpeedCameraRegionState.NotDownloaded -> Triple(OfflineMapAction.Download, "Download") { onDownload(r.id) }
            SpeedCameraRegionState.Failed -> Triple(OfflineMapAction.Download, "Retry") { onDownload(r.id) }
            SpeedCameraRegionState.Queued, SpeedCameraRegionState.Downloading ->
                Triple(OfflineMapAction.Cancel, "Cancel") { onDelete(r.id) }
            SpeedCameraRegionState.Downloaded -> Triple(OfflineMapAction.Delete, "Delete") { onDelete(r.id) }
        }
        return OfflineMapEntry.Region(
            node = "Region-${r.id}", title = r.name, sub = speedCameraRegionSubtitle(r), progress = null,
            action = action, actionDescription = "$verb ${r.name}", onAction = act,
        )
    }
    val (here, rest) = regions.partition { it.state.onDevice() }
    val entries = mutableListOf<OfflineMapEntry>()
    if (here.isNotEmpty()) entries += listOf<OfflineMapEntry>(OfflineMapEntry.Section("SectionOnThisDevice", OnThisDevice)) + here.map(::region)
    if (rest.isNotEmpty()) entries += listOf<OfflineMapEntry>(OfflineMapEntry.Section("SectionAvailable", Available)) + rest.map(::region)
    return entries
}

/** A region's line under its name: the camera count, the download size and the state. */
fun speedCameraRegionSubtitle(region: SpeedCameraRegion): String {
    val cameras = if (region.rows > 0) "${region.rows} cameras" else null
    val size = region.bytes?.let(::formatBytes)
    val state = when (region.state) {
        SpeedCameraRegionState.NotDownloaded -> "Not downloaded"
        SpeedCameraRegionState.Queued -> "Queued"
        SpeedCameraRegionState.Downloading -> "Downloading"
        SpeedCameraRegionState.Downloaded -> "Downloaded"
        SpeedCameraRegionState.Failed ->
            if (region.failureReason == SpeedCameraFailureIntegrity) "Needs Google Play" else "Download failed"
    }
    return listOfNotNull(cameras, size, state).joinToString(" · ")
}
