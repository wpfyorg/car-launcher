package org.wpfy.carlauncher.core.diagnostics

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wpfy.carlauncher.core.performance.PerformanceMonitor
import org.wpfy.carlauncher.core.performance.PerformanceSnapshot
import org.wpfy.carlauncher.core.platform.HeadUnitProfileId
import org.wpfy.carlauncher.core.platform.PlatformCapabilities
import org.wpfy.carlauncher.core.platform.PlatformDetector

data class DeviceDiagnostics(
    val appVersion: String,
    val android: String,
    val abi: String,
    val display: String,
    val profileId: HeadUnitProfileId,
    val manufacturer: String,
    val model: String,
    val product: String,
    val device: String,
    val capabilities: PlatformCapabilities,
    val permissions: String,
    val performance: String,
) {
    val copyText: String = buildString {
        appendLine("Car Launcher $appVersion")
        appendLine(android)
        appendLine("ABI: $abi")
        appendLine("Display: $display")
        appendLine("Head unit: $profileId")
        appendLine("Manufacturer: $manufacturer")
        appendLine("Model: $model")
        appendLine("Product: $product")
        appendLine("Device: $device")
        appendLine("Capabilities:")
        appendLine("  secondaryDisplayActivities=${capabilities.secondaryDisplayActivities}")
        appendLine(
            "  embeddedNavigationHostPrerequisites=${capabilities.embeddedNavigationHostPrerequisites}",
        )
        appendLine(
            "  embeddedInputForwardingPrerequisites=${capabilities.embeddedInputForwardingPrerequisites}",
        )
        appendLine("  locationProviderAvailable=${capabilities.locationProviderAvailable}")
        appendLine("  locationPermissionGranted=${capabilities.locationPermissionGranted}")
        appendLine("  locationProviderEnabled=${capabilities.locationProviderEnabled}")
        appendLine(
            "  notificationListenerPlatformSupport=${capabilities.notificationListenerPlatformSupport}",
        )
        appendLine("  hardwareRadioIntegration=${capabilities.hardwareRadioIntegration}")
        appendLine("  reverseCameraOverlay=${capabilities.reverseCameraOverlay}")
        appendLine("  customStatusBroadcast=${capabilities.customStatusBroadcast}")
        appendLine("  ignitionSleepWakeIntegration=${capabilities.ignitionSleepWakeIntegration}")
        appendLine("Permissions: $permissions")
        append(performance)
    }

    companion object {
        fun from(
            context: Context,
            includePss: Boolean = false,
        ): DeviceDiagnostics = from(
            context = context,
            performanceSnapshot = PerformanceMonitor.get(context).snapshot(includePss = includePss),
        )

        fun from(
            context: Context,
            performanceSnapshot: PerformanceSnapshot,
        ): DeviceDiagnostics {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS,
            )
            val platform = PlatformDetector(context).snapshot()
            val profile = platform.profile
            val requestedPermissions = packageInfo.requestedPermissions.orEmpty()
            val grantedPermissions = requestedPermissions.count { permission ->
                context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
            }

            return DeviceDiagnostics(
                appVersion = packageInfo.versionName.orEmpty(),
                android = "Android ${Build.VERSION.RELEASE} (API ${profile.sdkInt})",
                abi = profile.primaryAbi.orEmpty(),
                display = "${profile.displayWidthPx} × ${profile.displayHeightPx} · ${profile.densityDpi} dpi",
                profileId = profile.profileId,
                manufacturer = profile.manufacturer,
                model = profile.model,
                product = profile.product,
                device = profile.device,
                capabilities = platform.capabilities,
                permissions = "$grantedPermissions/${requestedPermissions.size} manifest permissions granted",
                performance = formatPerformanceDiagnostics(performanceSnapshot),
            )
        }

        suspend fun capture(context: Context): DeviceDiagnostics = withContext(Dispatchers.Default) {
            from(context = context, includePss = true)
        }
    }
}
