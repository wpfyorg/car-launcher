package org.wpfy.carlauncher.feature.media

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Watches the Bluetooth phone link for the media layer. */
interface BluetoothLinkMonitor {
    val link: StateFlow<BluetoothLink>

    fun start()
    fun stop()

    /** Re-reads the Bluetooth permission, for when it was granted or revoked outside the app. */
    fun refreshPermission()
}

/**
 * Follows connected Bluetooth phones with the public connection broadcasts. Android has no public
 * call that returns the current ACL state, so [start] also asks the classic audio profiles and the
 * link stays unknown until an event when that fails. Only phones count: OBD dongles, speakers and
 * the like are ignored.
 */
class AndroidBluetoothLinkMonitor(context: Context) : BluetoothLinkMonitor {
    private val appContext = context.applicationContext
    private val _link = MutableStateFlow(BluetoothLink())
    override val link: StateFlow<BluetoothLink> = _link.asStateFlow()

    private var model = BluetoothLinkModel()
    private var registered = false
    private var seedGeneration = 0
    private var seedRequested = false
    private var seedsInFlight = 0
    private val lateAnswer: () -> Unit = {
        // A profile answered after the timeout; the generation guard in the seed drops older results.
        if (registered && hasPermission()) seedConnectedPhones()
    }
    private var reseedPending = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            handle(intent)
        }
    }

    override fun start() {
        if (registered) return
        // Bluetooth broadcasts come from a privileged app that does not run under the system UID, so
        // a receiver that is not exported misses them. These are protected broadcasts, so other apps
        // cannot send them even though the receiver is exported.
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            },
            ContextCompat.RECEIVER_EXPORTED,
        )
        registered = true
        ProfileConnectionSource.addLateAnswerListener(lateAnswer)
        // A phone may have left while the receiver was off; the seed below rebuilds what is connected.
        model = BluetoothLinkModel()
        _link.value = model.toLink()
        refreshPermission()
    }

    /**
     * The broadcasts only report changes, so a phone that connected before the monitor started would
     * stay unknown. [ProfileConnectionSource] lists the phones connected over the classic audio
     * profiles, which never include an LE-only phone. The result is dropped if a link event, stop()
     * or a newer seeding got there first. Anything that fails leaves the link as it was.
     */
    private fun seedConnectedPhones() {
        if (!hasPermission()) return
        val generation = ++seedGeneration
        seedsInFlight++
        ProfileConnectionSource.snapshot(appContext) { phones, complete ->
            seedsInFlight--
            if (!registered || !hasPermission()) return@snapshot
            if (generation == seedGeneration) {
                if (phones.isNotEmpty() || complete) dispatch(BluetoothLinkEvent.PhonesSeeded(phones, complete))
            } else if (reseedPending && seedsInFlight == 0) {
                // A link event dropped this snapshot; ask again so the phones it held are not lost.
                reseedPending = false
                seedConnectedPhones()
            }
        }
    }

    /** A link event changed the model, so a seed query still running is stale; it is replaced once. */
    private fun invalidateSeed() {
        seedGeneration++
        if (seedsInFlight > 0) reseedPending = true
    }

    override fun stop() {
        if (!registered) return
        registered = false
        ProfileConnectionSource.removeLateAnswerListener(lateAnswer)
        seedGeneration++
        reseedPending = false
        seedRequested = false
        runCatching { appContext.unregisterReceiver(receiver) }
    }

    override fun refreshPermission() {
        if (hasPermission()) {
            dispatch(BluetoothLinkEvent.PermissionGranted)
            // A profile proxy that never answers is never released, and this runs on every Settings
            // resume, so seed once per start and once per grant rather than on each call.
            if (!seedRequested) {
                seedRequested = true
                seedConnectedPhones()
            }
        } else {
            seedRequested = false
            dispatch(BluetoothLinkEvent.PermissionMissing)
        }
    }

    private fun dispatch(event: BluetoothLinkEvent) {
        model = model.reduce(event)
        _link.value = model.toLink()
    }

    private fun handle(intent: Intent) {
        if (!hasPermission()) {
            invalidateSeed()
            seedRequested = false
            dispatch(BluetoothLinkEvent.PermissionMissing)
            return
        }
        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                if (isLowEnergyLink(intent)) return
                val device = deviceOf(intent) ?: return
                if (!isPhone(device)) return
                // Only an event that changes the modelled link makes an in-flight seed stale.
                invalidateSeed()
                dispatch(BluetoothLinkEvent.PhoneConnected(addressOf(device) ?: return, nameOf(device)))
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                if (isLowEnergyLink(intent)) return
                val device = deviceOf(intent) ?: return
                val address = addressOf(device) ?: return
                // A phone the model holds is gone whatever classification returns now: the class and
                // UUID reads can fail and return null, which must not leave it Connected.
                if (model.phones.any { it.address == address }) {
                    invalidateSeed()
                    dispatch(BluetoothLinkEvent.DeviceDisconnected(address))
                    return
                }
                if (!isPhone(device)) return
                // The seed may hold this phone before the model does, so drop it.
                invalidateSeed()
            }
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                    // No re-seed while the adapter is off; the one after it turns on replaces this seed.
                    seedGeneration++
                    reseedPending = false
                    ProfileConnectionSource.onAdapterOff()
                    dispatch(BluetoothLinkEvent.AdapterOff)
                } else if (state == BluetoothAdapter.STATE_ON) {
                    seedGeneration++
                    reseedPending = false // the fresh seed below replaces any pending one
                    ProfileConnectionSource.onAdapterOn()
                    seedRequested = false
                    dispatch(BluetoothLinkEvent.AdapterOn)
                    if (registered && hasPermission()) {
                        seedRequested = true
                        seedConnectedPhones()
                    }
                }
            }
        }
    }

    /** Up to API 30 the BLUETOOTH permission is granted at install; from API 31 BLUETOOTH_CONNECT is asked at run time. */
    private fun hasPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) ==
        PackageManager.PERMISSION_GRANTED

    /** From API 33 the broadcast names the transport. Audio runs over classic Bluetooth, so an LE-only link is ignored. */
    private fun isLowEnergyLink(intent: Intent): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            intent.getIntExtra(BluetoothDevice.EXTRA_TRANSPORT, BluetoothDevice.TRANSPORT_AUTO) == BluetoothDevice.TRANSPORT_LE

    private fun deviceOf(intent: Intent): BluetoothDevice? =
        IntentCompat.getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
}
