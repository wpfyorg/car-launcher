package org.wpfy.carlauncher.core.platform

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import org.wpfy.carlauncher.distribution.DistributionPlatformCapabilities

class PlatformDetector(context: Context) {
    private val appContext = context.applicationContext ?: context

    fun snapshot(): PlatformSnapshot {
        val identity = platformIdentity()
        val location = detectLocationCapabilities()
        val distribution = DistributionPlatformCapabilities.detect(appContext)

        return PlatformSnapshot(
            profile = identity.toHeadUnitProfile(),
            capabilities = PlatformCapabilities(
                secondaryDisplayActivities = detectSecondaryDisplayActivities(),
                embeddedNavigationHostPrerequisites = distribution.embeddedNavigationHostPrerequisites,
                embeddedInputForwardingPrerequisites = distribution.embeddedInputForwardingPrerequisites,
                locationProviderAvailable = location.available,
                locationPermissionGranted = detectLocationPermission(),
                locationProviderEnabled = location.enabled,
                notificationListenerPlatformSupport = detectNotificationListenerSupport(
                    identity.sdkInt,
                ),
                hardwareRadioIntegration = CapabilitySupport.Unknown,
                reverseCameraOverlay = CapabilitySupport.Unknown,
                customStatusBroadcast = CapabilitySupport.Unknown,
                ignitionSleepWakeIntegration = CapabilitySupport.Unknown,
            ),
        )
    }

    private fun platformIdentity(): PlatformIdentity {
        val display = realDisplayMetrics()
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val model = Build.MODEL.orEmpty()
        val product = Build.PRODUCT.orEmpty()
        val device = Build.DEVICE.orEmpty()
        val fingerprint = Build.FINGERPRINT.orEmpty()

        return PlatformIdentity(
            manufacturer = manufacturer,
            model = model,
            product = product,
            device = device,
            fingerprint = fingerprint,
            sdkInt = Build.VERSION.SDK_INT,
            primaryAbi = Build.SUPPORTED_ABIS.firstOrNull(),
            displayWidthPx = display.widthPixels,
            displayHeightPx = display.heightPixels,
            densityDpi = display.densityDpi,
            isEmulator = hasEmulatorEvidence(
                fingerprint = fingerprint,
                model = model,
                product = product,
            ),
        )
    }

    @Suppress("DEPRECATION")
    private fun realDisplayMetrics(): DisplayMetrics = runCatching {
        val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        DisplayMetrics().also(windowManager.defaultDisplay::getRealMetrics)
    }.getOrDefault(DisplayMetrics())

    private fun detectSecondaryDisplayActivities(): CapabilitySupport = probeSupport {
        appContext.packageManager.hasSystemFeature(
            PackageManager.FEATURE_ACTIVITIES_ON_SECONDARY_DISPLAYS,
        )
    }

    private fun detectLocationPermission(): CapabilitySupport = probeSupport {
        appContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            appContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun detectLocationCapabilities(): LocationCapabilities = runCatching {
        val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = locationManager.allProviders.filter { provider ->
            provider == LocationManager.GPS_PROVIDER || provider == LocationManager.NETWORK_PROVIDER
        }
        val available = if (providers.isEmpty()) {
            CapabilitySupport.Unsupported
        } else {
            CapabilitySupport.Supported
        }

        var enabledProbeFailed = false
        for (provider in providers) {
            val enabled = runCatching { locationManager.isProviderEnabled(provider) }
                .getOrElse {
                    enabledProbeFailed = true
                    false
                }
            if (enabled) {
                return@runCatching LocationCapabilities(
                    available = available,
                    enabled = CapabilitySupport.Supported,
                )
            }
        }

        LocationCapabilities(
            available = available,
            enabled = if (enabledProbeFailed) {
                CapabilitySupport.Unknown
            } else {
                CapabilitySupport.Unsupported
            },
        )
    }.getOrDefault(
        LocationCapabilities(
            available = CapabilitySupport.Unknown,
            enabled = CapabilitySupport.Unknown,
        ),
    )

    private fun detectNotificationListenerSupport(sdkInt: Int): CapabilitySupport =
        notificationListenerPlatformSupportFromProbe(sdkInt) {
            val activityManager =
                appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.isLowRamDevice
        }
}

private data class LocationCapabilities(
    val available: CapabilitySupport,
    val enabled: CapabilitySupport,
)

private fun probeSupport(probe: () -> Boolean): CapabilitySupport = runCatching {
    if (probe()) CapabilitySupport.Supported else CapabilitySupport.Unsupported
}.getOrDefault(CapabilitySupport.Unknown)

internal fun notificationListenerPlatformSupportFromProbe(
    sdkInt: Int,
    lowRamProbe: () -> Boolean,
): CapabilitySupport {
    if (sdkInt >= 30) return CapabilitySupport.Supported
    return runCatching {
        notificationListenerPlatformSupport(sdkInt, lowRamProbe())
    }.getOrDefault(CapabilitySupport.Unknown)
}

private fun hasEmulatorEvidence(
    fingerprint: String,
    model: String,
    product: String,
): Boolean {
    val normalizedFingerprint = fingerprint.normalizeIdentityField()
    val normalizedModel = model.normalizeIdentityField()
    val normalizedProduct = product.normalizeIdentityField()

    return normalizedFingerprint.startsWith("generic") ||
        normalizedFingerprint.startsWith("unknown") ||
        "emulator" in normalizedFingerprint ||
        "vbox" in normalizedFingerprint ||
        "googlesdk" in normalizedModel ||
        "emulator" in normalizedModel ||
        "androidsdkbuiltfor" in normalizedModel ||
        normalizedProduct.startsWith("sdk") ||
        "emulator" in normalizedProduct ||
        "simulator" in normalizedProduct
}

private fun String.normalizeIdentityField(): String = buildString(length) {
    this@normalizeIdentityField.forEach { character ->
        when (character) {
            in 'A'..'Z' -> append(character.lowercaseChar())
            in 'a'..'z', in '0'..'9' -> append(character)
        }
    }
}
