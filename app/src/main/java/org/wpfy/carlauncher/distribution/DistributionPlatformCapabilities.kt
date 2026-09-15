package org.wpfy.carlauncher.distribution

import android.content.Context
import org.wpfy.carlauncher.core.platform.CapabilitySupport
import org.wpfy.carlauncher.core.platform.DistributionCapabilitySnapshot

/** Standard public builds do not have privileged external activity embedding. */
object DistributionPlatformCapabilities {
    @Suppress("UNUSED_PARAMETER")
    fun detect(context: Context): DistributionCapabilitySnapshot = DistributionCapabilitySnapshot(
        embeddedNavigationHostPrerequisites = CapabilitySupport.Unsupported,
        embeddedInputForwardingPrerequisites = CapabilitySupport.Unsupported,
    )
}
