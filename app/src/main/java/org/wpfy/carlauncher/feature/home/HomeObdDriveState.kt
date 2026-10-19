package org.wpfy.carlauncher.feature.home

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.wpfy.carlauncher.feature.obd.ObdConnectionState
import org.wpfy.carlauncher.feature.obd.ObdRepository
import org.wpfy.carlauncher.feature.obd.ObdRepositoryFactory
import org.wpfy.carlauncher.feature.obd.VehicleSnapshot

/** A speed sample is fresh through age 1,500 ms inclusive; older or future samples fall back to GPS. */
internal const val DriveObdSpeedFreshMillis = 1_500L

data class HomeObdDriveState(
    val connection: ObdConnectionState = ObdConnectionState.NoAdapter,
    val vehicle: VehicleSnapshot = VehicleSnapshot(),
    val nowElapsedRealtimeMillis: Long = 0L,
)

/** App-process owner for Home OBD telemetry; only its selected speed sample is exposed to the Drive Card. */
class HomeObdDriveStateHolder internal constructor(
    private val repository: ObdRepository,
    private val scope: CoroutineScope,
    private val monotonic: () -> Long = SystemClock::elapsedRealtime,
    private val freshnessMillis: Long = DriveObdSpeedFreshMillis,
) {
    private val mutableState = MutableStateFlow(HomeObdDriveState())
    val state: StateFlow<HomeObdDriveState> = mutableState.asStateFlow()
    private var ownerJob: Job? = null

    init {
        require(freshnessMillis > 0)
        ownerJob = scope.launch {
            // Only the speed sample reaches Home, so RPM and other sensor updates never recompose it.
            combine(repository.connectionState, repository.vehicleSnapshot) { connection, vehicle ->
                connection to VehicleSnapshot(
                    speedKph = vehicle.speedKph,
                    speedSampledAtElapsedRealtimeMillis = vehicle.speedSampledAtElapsedRealtimeMillis,
                )
            }.distinctUntilChanged().collectLatest { (connection, vehicle) ->
                val base = HomeObdDriveState(connection, vehicle, monotonic())
                mutableState.value = base
                val sampledAt = vehicle.speedSampledAtElapsedRealtimeMillis ?: return@collectLatest
                if (connection !is ObdConnectionState.Connected) return@collectLatest
                val remaining = freshnessMillis - (base.nowElapsedRealtimeMillis - sampledAt)
                if (remaining >= 0) {
                    delay(remaining + 1L)
                    mutableState.value = base.copy(nowElapsedRealtimeMillis = monotonic())
                }
            }
        }
        scope.launch {
            try {
                repository.start()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // A failed remembered-selection read leaves GPS as the Drive Card's source.
            }
        }
    }

    fun close() {
        ownerJob?.cancel()
        repository.close()
        scope.cancel()
    }

    companion object {
        fun createForProcess(context: Context): HomeObdDriveStateHolder {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            return HomeObdDriveStateHolder(
                repository = ObdRepositoryFactory.create(context.applicationContext, scope),
                scope = scope,
            )
        }
    }
}

fun freshObdSpeedKph(
    state: HomeObdDriveState,
    nowElapsedRealtimeMillis: Long,
    freshnessMillis: Long = DriveObdSpeedFreshMillis,
): Double? {
    if (state.connection !is ObdConnectionState.Connected) return null
    val speed = state.vehicle.speedKph?.takeIf { it.isFinite() && it >= 0.0 } ?: return null
    val sampledAt = state.vehicle.speedSampledAtElapsedRealtimeMillis ?: return null
    val age = nowElapsedRealtimeMillis - sampledAt
    return speed.takeIf { age in 0..freshnessMillis }
}
