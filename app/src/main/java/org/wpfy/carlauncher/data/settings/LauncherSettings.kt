package org.wpfy.carlauncher.data.settings

enum class StartupScreen(val label: String) {
    Home("Home"),
    Apps("Apps"),
}

enum class TextSizePreset(
    val label: String,
    val scale: Float,
) {
    Small("Small", 0.9f),
    Standard("Standard", 1f),
    Large("Large", 1.15f),
}

enum class NavigationCardPosition(val label: String) {
    BottomLeft("Bottom left"),
    BottomRight("Bottom right"),
}

/**
 * Size of the map information tiles (turn card, camera alert, speed, ETA). Auto follows the window
 * size; a percentage overrides it. Controls keep following the window size and their touch targets.
 */
enum class OverlaySize(val label: String, val tileScale: Float?) {
    Auto("Auto", null),
    Percent100("100%", 1f),
    Percent90("90%", 0.9f),
    Percent80("80%", 0.8f),
    Percent75("75%", 0.75f),
    Percent50("50%", 0.5f),
}

enum class RailPosition(val label: String) {
    Left("Left"),
    Right("Right"),
    Bottom("Bottom"),
}

enum class AppTheme(val label: String) {
    Default("Default"),
    PaidUi("Paid UI"),
}

/** How loud spoken guidance is, as a share of the navigation audio stream's own volume. */
enum class VoiceVolume(val label: String, val level: Float) {
    Low("Low", 0.4f),
    Medium("Medium", 0.6f),
    High("High", 0.8f),
    Max("Max", 1f),
}

/**
 * Camera search range. Auto queries at least 4.2 km off route or 2.8 km on route;
 * speed-based look-ahead targets 90 s (500 m–4.2/4.5 km), falling back to 800 m/1.2 km
 * without speed. Each camera has its own warning distance.
 */
enum class CameraAlertDistance(val meters: Double?) {
    Auto(null),
    Meters300(300.0),
    Meters500(500.0),
    Kilometer1(1_000.0),
    ;

    /** The row/option text; mph mode shows feet and miles for the same distance. */
    fun label(units: UnitSystem): String = when (this) {
        Auto -> "Auto"
        Meters300 -> if (units == UnitSystem.Imperial) "1,000 ft" else "300 m"
        Meters500 -> if (units == UnitSystem.Imperial) "0.3 mi" else "500 m"
        Kilometer1 -> if (units == UnitSystem.Imperial) "0.6 mi" else "1 km"
    }
}

/** How far over a camera's posted limit the car must be before the beep; stored in km/h. */
enum class OverspeedWarning(val toleranceKph: Double?) {
    Off(null),
    AtLimit(0.0),
    Plus5(5.0),
    Plus10(10.0),
    ;

    fun label(units: UnitSystem): String = when (this) {
        Off -> "Off"
        AtLimit -> "At the limit"
        Plus5 -> if (units == UnitSystem.Imperial) "+3 mph" else "+5 km/h"
        Plus10 -> if (units == UnitSystem.Imperial) "+6 mph" else "+10 km/h"
    }
}

data class LauncherSettings(
    val onboardingCompleted: Boolean = false,
    val startOnBoot: Boolean = false,
    val startupScreen: StartupScreen = StartupScreen.Home,
    val textSizePreset: TextSizePreset = TextSizePreset.Standard,
    val preferredMediaAppKey: String? = null,
    val navigationAppKey: String? = null,
    val navigationCompatibilityMode: Boolean = false,
    val navigationCardPosition: NavigationCardPosition = NavigationCardPosition.BottomLeft,
    val railPosition: RailPosition = RailPosition.Left,
    val overlaySize: OverlaySize = OverlaySize.Auto,
    /** The Home media card is folded into the now-playing chip, giving the map the full width. */
    val mediaCardCollapsed: Boolean = false,
    val appTheme: AppTheme = AppTheme.Default,
    /** Spoken guidance is off. */
    val voiceGuidanceMuted: Boolean = false,
    val voiceVolume: VoiceVolume = VoiceVolume.Max,
    /** The system voice used for guidance; null follows the system default. */
    val voiceName: String? = null,
    val speedUnit: SpeedUnitSetting = SpeedUnitSetting.Auto,
    /** Pro: when the first speed-camera alert fires. */
    val cameraAlertDistance: CameraAlertDistance = CameraAlertDistance.Auto,
    /** Pro: the overspeed beep near a camera; "at the limit" by default. */
    val overspeedWarning: OverspeedWarning = OverspeedWarning.AtLimit,
)
