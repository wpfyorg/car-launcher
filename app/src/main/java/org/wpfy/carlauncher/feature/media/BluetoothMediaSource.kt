package org.wpfy.carlauncher.feature.media

/** Whether a Bluetooth phone is connected. Unknown means we cannot tell (no permission or no answer). */
enum class BluetoothLinkState { Unknown, Disconnected, Connected }

/** Bluetooth phone link as seen by the media layer. */
data class BluetoothLink(
    val state: BluetoothLinkState = BluetoothLinkState.Unknown,
    val deviceName: String? = null,
)

/** Packages whose media sessions mirror a connected Bluetooth phone. */
internal val BluetoothSessionPackages: Set<String> =
    setOf("com.android.bluetooth", "com.google.android.bluetooth")

internal fun isBluetoothSessionPackage(packageName: String): Boolean =
    packageName in BluetoothSessionPackages

/**
 * Whether a Bluetooth session counts as connected. A session in an error state never does: the
 * AOSP sink keeps its session after a disconnect and marks it with an error. With an unknown link,
 * only a "live" session does: priority below 4 (playing, buffering, paused or has metadata).
 */
internal fun bluetoothSessionConnected(link: BluetoothLink, priority: Int, isError: Boolean): Boolean =
    when (link.state) {
        BluetoothLinkState.Connected -> !isError
        BluetoothLinkState.Disconnected -> false
        BluetoothLinkState.Unknown -> priority < 4 && !isError
    }

/**
 * The candidates worth showing: a Bluetooth session that does not count as connected is dropped,
 * so stale metadata never shows and cannot be commanded. Local sessions are never touched.
 */
internal fun usableCandidates(candidates: List<SessionCandidate>, link: BluetoothLink): List<SessionCandidate> =
    candidates.filter { !it.isBluetooth || bluetoothSessionConnected(link, it.priority, it.isError) }

/**
 * The connected flag for the selector. A known link decides it. While the link is unknown, a
 * Bluetooth session that is not in error counts. Pass every candidate, before [usableCandidates],
 * so a session going from idle to live is not mistaken for a reconnect.
 */
internal fun bluetoothConnectedSignal(link: BluetoothLink, candidates: List<SessionCandidate>): Boolean =
    when (link.state) {
        BluetoothLinkState.Connected -> true
        BluetoothLinkState.Disconnected -> false
        BluetoothLinkState.Unknown -> candidates.any { it.isBluetooth && !it.isError }
    }

/** The label of the Bluetooth source: the phone's name, or "Bluetooth" when it is not known. */
internal fun bluetoothSourceLabel(link: BluetoothLink): String = link.deviceName ?: "Bluetooth"

/**
 * Chooses the media source with a Bluetooth phone as a first-class source.
 * A connected phone wins over local sessions; an explicit pick of another source overrides
 * that until the phone reconnects. The caller passes Bluetooth candidates only while they
 * count as connected, so a disconnected phone is never selected or commanded.
 */
internal class MediaSourceSelector {
    private var explicitId: String? = null
    private var bluetoothConnected = false

    /** Records an explicit user choice (swipe). */
    fun choose(id: String) {
        explicitId = id
    }

    /** Tracks the Bluetooth link; a disconnected-to-connected transition clears the explicit choice. */
    fun onBluetoothConnected(connected: Boolean) {
        if (connected && !bluetoothConnected) explicitId = null
        bluetoothConnected = connected
    }

    /** Drops the explicit choice so the automatic order applies again. */
    fun forgetChoice() {
        explicitId = null
    }

    fun clear() {
        explicitId = null
        bluetoothConnected = false
    }

    /** Order: explicit choice, connected Bluetooth (best priority), then [chooseSession]. */
    fun select(candidates: List<SessionCandidate>, preferredPackage: String?): SessionCandidate? {
        explicitId?.let { id ->
            candidates.firstOrNull { it.id == id }?.let { return it }
            explicitId = null
        }
        candidates.filter { it.isBluetooth }.minByOrNull { it.priority }?.let { return it }
        return chooseSession(candidates, null, preferredPackage)
    }
}
