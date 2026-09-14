package org.wpfy.carlauncher.core.platform

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/** What the Start-on-boot setting does on this device. */
internal enum class StartOnBootPolicy {
    /** The setting is off. */
    Disabled,

    /** Android 9 and older: the boot receiver may start the launcher itself. */
    ReceiverLaunch,

    /** Android 10+ and the default Home app: the system starts Home at boot, the receiver does nothing. */
    SystemHome,

    /** Android 10+ but another Home app is the default: the user must choose this launcher. */
    NeedsDefaultHome,
}

/** Android 10 (API 29) blocks activity starts from the background, so boot relies on the default Home app. */
private const val BackgroundStartRestrictedSdk = 29

internal fun startOnBootPolicy(sdkInt: Int, isDefaultHome: Boolean, enabled: Boolean): StartOnBootPolicy = when {
    !enabled -> StartOnBootPolicy.Disabled
    sdkInt < BackgroundStartRestrictedSdk -> StartOnBootPolicy.ReceiverLaunch
    isDefaultHome -> StartOnBootPolicy.SystemHome
    else -> StartOnBootPolicy.NeedsDefaultHome
}

/** The boot receiver starts an activity only where the system still allows it. */
internal fun StartOnBootPolicy.launchesFromReceiver(): Boolean = this == StartOnBootPolicy.ReceiverLaunch

/** How the Start-on-boot row behaves. */
enum class StartOnBootRowMode {
    /** Android 9 and older: the switch is the setting, honoured by the boot receiver. */
    Standard,

    /** Android 10+ and this is the default Home app: Android starts it at boot whatever the switch says. */
    SystemHome,

    /** Android 10+ but another Home app is the default: the user must choose this launcher. */
    NeedsDefaultHome,
}

fun startOnBootRowMode(sdkInt: Int, isDefaultHome: Boolean): StartOnBootRowMode = when {
    sdkInt < BackgroundStartRestrictedSdk -> StartOnBootRowMode.Standard
    isDefaultHome -> StartOnBootRowMode.SystemHome
    else -> StartOnBootRowMode.NeedsDefaultHome
}

/** Whether this app is the device's default Home app. */
internal fun isDefaultHomeApp(context: Context): Boolean {
    val homeIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
    val resolved = context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
    return resolved?.activityInfo?.packageName == context.packageName
}
