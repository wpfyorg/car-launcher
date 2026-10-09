package org.wpfy.carlauncher.feature.media

/** What the Bluetooth monitor reports. Addresses identify devices; names may be missing. */
internal sealed interface BluetoothLinkEvent {
    /** A Bluetooth phone connected. */
    data class PhoneConnected(val address: String, val name: String?) : BluetoothLinkEvent

    /**
     * Phones found connected at start, in no known order. Their names are kept, but while more than
     * one seeded phone is connected the link shows no name; a later [PhoneConnected] wins. A [complete]
     * seed (every profile answered) is authoritative: it proves which phones are connected, so the
     * link becomes known and seeded phones missing from it are dropped.
     */
    data class PhonesSeeded(val phones: List<ConnectedPhone>, val complete: Boolean = false) : BluetoothLinkEvent

    /** A device disconnected; ignored unless it is a phone we know is connected. */
    data class DeviceDisconnected(val address: String) : BluetoothLinkEvent

    /** The Bluetooth adapter is off or turning off: no link can be up. */
    data object AdapterOff : BluetoothLinkEvent

    /** The adapter just turned on, so no link exists at this moment. */
    data object AdapterOn : BluetoothLinkEvent

    /** The permission needed to read device names is missing, so the link is unknown. */
    data object PermissionMissing : BluetoothLinkEvent

    /** The permission is now granted; the link stays unknown until the next event. */
    data object PermissionGranted : BluetoothLinkEvent
}

/** A connected phone. [seeded] marks one found at start, whose connection order is unknown. */
internal data class ConnectedPhone(val address: String, val name: String?, val seeded: Boolean = false)

/**
 * What the monitor knows. [phones] is in connection order, the latest last. [known] turns true only
 * when an event proves no hidden connection remains (adapter off or just on). The monitor cannot see
 * links that existed before it started, so a list of phones alone is never a complete snapshot.
 */
internal data class BluetoothLinkModel(
    val phones: List<ConnectedPhone> = emptyList(),
    val known: Boolean = false,
    val permissionMissing: Boolean = false,
)

/** Applies one [event] to this model. */
internal fun BluetoothLinkModel.reduce(event: BluetoothLinkEvent): BluetoothLinkModel = when (event) {
    is BluetoothLinkEvent.PhoneConnected ->
        if (permissionMissing) {
            this
        } else {
            copy(phones = phones.filterNot { it.address == event.address } + ConnectedPhone(event.address, event.name))
        }
    is BluetoothLinkEvent.PhonesSeeded ->
        if (permissionMissing) {
            this
        } else {
            val seeded = event.phones.distinctBy { it.address }
            val seededAddresses = seeded.mapTo(HashSet()) { it.address }
            // A complete seed replaces earlier seeded entries; phones the broadcasts reported stay.
            val current = if (event.complete) phones.filter { !it.seeded || it.address in seededAddresses } else phones
            val present = current.mapTo(HashSet()) { it.address }
            val fresh = seeded.filter { it.address !in present }
            // A known phone without a name takes the name the seed found, keeping its place.
            val named = current.map { phone ->
                val name = if (phone.name == null) seeded.firstOrNull { it.address == phone.address }?.name else null
                if (name != null) phone.copy(name = name) else phone
            }
            // Seeded phones sit before any phone the broadcasts reported, which are more recent.
            // An empty answer proves nothing: restricted profiles can answer with no devices.
            copy(
                phones = fresh.map { it.copy(seeded = true) } + named,
                known = known || (event.complete && seeded.isNotEmpty()),
            )
        }
    is BluetoothLinkEvent.DeviceDisconnected ->
        if (phones.none { it.address == event.address }) this else copy(phones = phones.filterNot { it.address == event.address })
    BluetoothLinkEvent.AdapterOff -> copy(phones = emptyList(), known = !permissionMissing)
    BluetoothLinkEvent.AdapterOn -> copy(phones = emptyList(), known = !permissionMissing)
    BluetoothLinkEvent.PermissionMissing -> BluetoothLinkModel(permissionMissing = true)
    BluetoothLinkEvent.PermissionGranted -> if (permissionMissing) BluetoothLinkModel() else this
}

/** Connected with the latest phone's name while any phone is connected; disconnected when none is; else unknown. */
internal fun BluetoothLinkModel.toLink(): BluetoothLink = when {
    phones.isNotEmpty() -> {
        val latest = phones.last()
        val orderUnknown = latest.seeded && phones.count { it.seeded } > 1
        BluetoothLink(BluetoothLinkState.Connected, if (orderUnknown) null else latest.name)
    }
    known -> BluetoothLink(BluetoothLinkState.Disconnected)
    else -> BluetoothLink()
}
