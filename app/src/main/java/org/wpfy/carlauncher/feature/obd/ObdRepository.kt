package org.wpfy.carlauncher.feature.obd

import kotlinx.coroutines.flow.StateFlow

/** Backend lifecycle is explicit: the caller starts/stops it; nothing runs merely by creating it. */
interface ObdRepository : AutoCloseable {
    val connectionState: StateFlow<ObdConnectionState>
    val vehicleSnapshot: StateFlow<VehicleSnapshot>
    suspend fun listPairedAdapters(): List<ObdAdapter>
    /** Restores the remembered adapter and starts automatic reconnect while this session is active. */
    suspend fun start()
    /** Persists the selection, replaces any active session, and connects. Null forgets the adapter. */
    suspend fun selectAdapter(adapter: ObdAdapter?)
    /** Stops reconnect and releases transport. Keeps the remembered selection. */
    suspend fun disconnect()
    suspend fun readDtcs(): ObdDtcReport
    /** Never retries or replays a clear command; call again with confirmation for every attempt. */
    suspend fun clearDtcs(confirmed: Boolean = false)
    /** Permanently releases this instance and its owned jobs, including a pending socket read. */
    override fun close()
}
