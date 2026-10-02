package org.wpfy.carlauncher.feature.obd

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Flavor-local capability, following the DistributionFeatures source-set pattern. */
object ObdFeatures { const val supportsObd = false }

object ObdRepositoryFactory {
    @Suppress("UNUSED_PARAMETER")
    fun create(context: Context, scope: CoroutineScope): ObdRepository = UnavailableObdRepository()
}

class UnavailableObdRepository : ObdRepository {
    override val connectionState = MutableStateFlow<ObdConnectionState>(ObdConnectionState.Unavailable).asStateFlow()
    override val vehicleSnapshot = MutableStateFlow(VehicleSnapshot()).asStateFlow()
    override suspend fun listPairedAdapters(): List<ObdAdapter> = emptyList()
    override suspend fun start() = Unit
    override suspend fun selectAdapter(adapter: ObdAdapter?) = Unit
    override suspend fun disconnect() = Unit
    override suspend fun readDtcs(): ObdDtcReport = throw ObdUnavailableException()
    override suspend fun clearDtcs(confirmed: Boolean) { throw ObdUnavailableException() }
    override fun close() = Unit
}
