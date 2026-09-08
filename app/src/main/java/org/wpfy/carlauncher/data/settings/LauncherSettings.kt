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

data class LauncherSettings(
    val startOnBoot: Boolean = false,
    val startupScreen: StartupScreen = StartupScreen.Home,
    val textSizePreset: TextSizePreset = TextSizePreset.Standard,
    val preferredMediaAppKey: String? = null,
    val navigationAppKey: String? = null,
    val navigationCompatibilityMode: Boolean = false,
)
