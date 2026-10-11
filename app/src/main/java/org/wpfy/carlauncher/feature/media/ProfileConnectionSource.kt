package org.wpfy.carlauncher.feature.media

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.UUID

/** A phone offers audio when it advertises the A2DP source service. */
private val A2dpSourceUuid: UUID = UUID.fromString("0000110A-0000-1000-8000-00805F9B34FB")

@SuppressLint("MissingPermission") // Only called once the Bluetooth permission is held.
internal fun addressOf(device: BluetoothDevice): String? = runCatching { device.address }.getOrNull()

@SuppressLint("MissingPermission") // Only called once the Bluetooth permission is held.
internal fun nameOf(device: BluetoothDevice): String? =
    runCatching { device.name }.getOrNull()?.takeIf(String::isNotBlank)

/** A phone by its device class, or by offering the A2DP source service. */
@SuppressLint("MissingPermission") // Only called once the Bluetooth permission is held.
internal fun isPhone(device: BluetoothDevice): Boolean {
    val byClass = runCatching { device.bluetoothClass?.majorDeviceClass }.getOrNull() == BluetoothClass.Device.Major.PHONE
    if (byClass) return true
    return runCatching { device.uuids }.getOrNull().orEmpty().any { it.uuid == A2dpSourceUuid }
}

/**
 * Hidden BluetoothProfile ids for the head-unit side of a phone link: A2DP_SINK and HEADSET_CLIENT.
 * Both are classic profiles, so they never list an LE-only device.
 */
private val Profiles = intArrayOf(11, 16)
private const val AnswerTimeoutMs = 3_000L

/**
 * Lists the phones connected over the classic audio profiles. Each profile proxy is requested once
 * per process and then held: on firmware without a profile service getProfileProxy() can accept the
 * request and never call back, so asking again on every monitor start would leak a connector each
 * time. A profile that stayed silent is not asked again, only waited for again after the adapter
 * cycles; a proxy is dropped when the adapter turns off or the service disconnects and is requested
 * again on the next snapshot.
 *
 * State is touched on the main thread only; device queries are binder calls and run on a thread.
 */
internal object ProfileConnectionSource {
    private val main = Handler(Looper.getMainLooper())
    private val proxies = HashMap<Int, BluetoothProfile>()
    private val requested = HashSet<Int>()
    private val silent = HashSet<Int>()
    private val refused = HashSet<Int>() // asked and refused outright; asked again after the adapter cycles
    private val waiters = ArrayList<(List<ConnectedPhone>, Boolean) -> Unit>()
    private val lateAnswerListeners = LinkedHashSet<() -> Unit>()
    private var timeoutPosted = false

    private val listener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            main.post {
                val wasHeld = profile in proxies
                val hadWaiters = waiters.isNotEmpty()
                proxies[profile] = proxy
                silent.remove(profile)
                flushIfResolved()
                // A new answer that no snapshot was waiting for missed its seed: tell the listeners
                // to ask again.
                if (!wasHeld && !hadWaiters) lateAnswerListeners.toList().forEach { it() }
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            main.post {
                val proxy = proxies.remove(profile)
                if (proxy != null) {
                    val adapter = runCatching { BluetoothAdapter.getDefaultAdapter() }.getOrNull()
                    runCatching { adapter?.closeProfileProxy(profile, proxy) }
                }
                requested.remove(profile)
                silent.remove(profile)
                // Asked again on the next snapshot. A snapshot already waiting is answered now, as
                // incomplete, once every other profile has answered; otherwise it keeps waiting
                // and the timeout bounds it.
                if (Profiles.all { it == profile || it in proxies || it in silent || it in refused }) flush()
            }
        }
    }

    /**
     * Calls [callback] on the main thread with the phones connected now and whether the answer is
     * complete: every profile answered and every query succeeded. Call from the main thread.
     */
    @SuppressLint("MissingPermission") // Callers hold the Bluetooth permission.
    fun snapshot(context: Context, callback: (phones: List<ConnectedPhone>, complete: Boolean) -> Unit) {
        val adapter = runCatching { BluetoothAdapter.getDefaultAdapter() }.getOrNull()
        if (adapter == null) {
            callback(emptyList(), false)
            return
        }
        for (profile in Profiles) {
            if (profile in requested || profile in silent || profile in refused) continue
            requested.add(profile)
            val asked = runCatching { adapter.getProfileProxy(context.applicationContext, listener, profile) }
                .getOrDefault(false)
            if (!asked) {
                requested.remove(profile)
                refused.add(profile)
            }
        }
        waiters.add(callback)
        if (isResolved()) {
            flush()
        } else if (!timeoutPosted) {
            timeoutPosted = true
            main.postDelayed({
                timeoutPosted = false
                // A profile that has not answered by now is not waited for again.
                for (profile in Profiles) if (profile in requested && profile !in proxies) silent.add(profile)
                flush()
            }, AnswerTimeoutMs)
        }
    }

    /** Calls [listener] on the main thread when a profile answers after its snapshot gave up on it. */
    fun addLateAnswerListener(listener: () -> Unit) {
        lateAnswerListeners.add(listener)
    }

    fun removeLateAnswerListener(listener: () -> Unit) {
        lateAnswerListeners.remove(listener)
    }

    /** The adapter turned off: held proxies are void. They are requested again after it turns on. */
    fun onAdapterOff() {
        val adapter = runCatching { BluetoothAdapter.getDefaultAdapter() }.getOrNull()
        for ((profile, proxy) in proxies) runCatching { adapter?.closeProfileProxy(profile, proxy) }
        for (profile in proxies.keys) requested.remove(profile)
        proxies.clear()
        forgetUnansweredProfiles()
    }

    /**
     * The adapter turned on: profiles refused while it was off are asked again; silent ones are
     * waited for again.
     */
    fun onAdapterOn() = forgetUnansweredProfiles()

    private fun forgetUnansweredProfiles() {
        refused.clear()
        silent.clear()
        // A request that never produced a proxy stays requested: its connector is still registered
        // and reconnects once the service comes up, so asking again would add a second connector.
        // The next snapshot waits for it again; a later answer re-seeds through the listeners.
    }

    private fun isResolved() = Profiles.all { it in proxies || it in silent || it in refused }

    private fun flushIfResolved() {
        if (isResolved()) flush()
    }

    private fun flush() {
        if (waiters.isEmpty()) return
        val callbacks = waiters.toList()
        waiters.clear()
        val held = proxies.values.toList()
        val allAnswered = Profiles.all { it in proxies }
        Thread {
            var queriesOk = true
            val found = LinkedHashMap<String, ConnectedPhone>()
            for (proxy in held) {
                val devices = runCatching { proxy.connectedDevices }.getOrNull()
                if (devices == null) {
                    queriesOk = false
                    continue
                }
                for (device in devices) {
                    if (!isPhone(device)) continue
                    val address = addressOf(device) ?: continue
                    found.putIfAbsent(address, ConnectedPhone(address, nameOf(device)))
                }
            }
            val phones = found.values.toList()
            val complete = allAnswered && queriesOk
            main.post { callbacks.forEach { it(phones, complete) } }
        }.apply { isDaemon = true }.start()
    }
}
