package org.wpfy.carlauncher.feature.media

internal data class SessionCandidate(
    val id: String,
    val packageName: String,
    /** Lower is better: playing before paused before idle. */
    val priority: Int,
    /** True for the Bluetooth phone session; see [MediaSourceSelector]. */
    val isBluetooth: Boolean = false,
    /** True when the session reports an error state. */
    val isError: Boolean = false,
)

/**
 * Picks the session to show. Order: explicit in-session choice, then the best (lowest)
 * priority overall, with the preferred app's session winning ties at that priority.
 * A playing app therefore beats a paused preferred one.
 */
internal fun chooseSession(
    candidates: List<SessionCandidate>,
    explicitId: String?,
    preferredPackage: String?,
): SessionCandidate? {
    candidates.firstOrNull { it.id == explicitId }?.let { return it }
    val best = candidates.minOfOrNull { it.priority } ?: return null
    val atBest = candidates.filter { it.priority == best }
    return atBest.firstOrNull { !preferredPackage.isNullOrBlank() && it.packageName == preferredPackage }
        ?: atBest.first()
}

/** Settings store a launcher component key such as "pkg/.Activity"; the session key is the package. */
internal fun packageOfAppKey(appKey: String?): String? =
    appKey?.substringBefore('/')?.takeIf { it.isNotBlank() }
