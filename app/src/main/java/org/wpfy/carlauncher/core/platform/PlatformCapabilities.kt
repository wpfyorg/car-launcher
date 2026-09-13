package org.wpfy.carlauncher.core.platform

enum class CapabilitySupport {
    Supported,
    Unsupported,
    Unknown,
}

data class PlatformCapabilities(
    val secondaryDisplayActivities: CapabilitySupport,
    val embeddedNavigationHostPrerequisites: CapabilitySupport,
    val embeddedInputForwardingPrerequisites: CapabilitySupport,
    val locationProviderAvailable: CapabilitySupport,
    val locationPermissionGranted: CapabilitySupport,
    val locationProviderEnabled: CapabilitySupport,
    val notificationListenerPlatformSupport: CapabilitySupport,
    val hardwareRadioIntegration: CapabilitySupport,
    val reverseCameraOverlay: CapabilitySupport,
    val customStatusBroadcast: CapabilitySupport,
    val ignitionSleepWakeIntegration: CapabilitySupport,
)

data class DistributionCapabilitySnapshot(
    val embeddedNavigationHostPrerequisites: CapabilitySupport,
    val embeddedInputForwardingPrerequisites: CapabilitySupport,
)

data class PlatformSnapshot(
    val profile: HeadUnitProfile,
    val capabilities: PlatformCapabilities,
)

internal fun notificationListenerPlatformSupport(
    sdkInt: Int,
    isLowRam: Boolean,
): CapabilitySupport = if (sdkInt <= 29 && isLowRam) {
    CapabilitySupport.Unsupported
} else {
    CapabilitySupport.Supported
}
