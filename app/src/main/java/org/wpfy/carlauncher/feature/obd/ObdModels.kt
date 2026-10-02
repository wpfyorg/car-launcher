package org.wpfy.carlauncher.feature.obd

/** Only transport types implemented by the backend are selectable. */
enum class ObdTransportKind { BLUETOOTH_SPP, WIFI_TCP }

data class ObdAdapter(
    val address: String,
    val name: String? = null,
    val transport: ObdTransportKind = ObdTransportKind.BLUETOOTH_SPP,
    val port: Int = 35000,
)

enum class ObdConnectionStep { TRANSPORT, INITIALIZING, DISCOVERING, IDENTIFYING }
enum class ObdPairingReason { PERMISSION_REQUIRED, NOT_BONDED }
enum class ObdError { BLUETOOTH_OFF, CONNECTION_FAILED, ADAPTER_REJECTED, VEHICLE_UNREACHABLE, TIMEOUT, STORAGE }

sealed interface ObdConnectionState {
    data object Unavailable : ObdConnectionState
    data object NoAdapter : ObdConnectionState
    data class Disconnected(val adapter: ObdAdapter) : ObdConnectionState
    data class PairingNeeded(val reason: ObdPairingReason) : ObdConnectionState
    data class Connecting(val step: ObdConnectionStep, val attempt: Int) : ObdConnectionState
    data class Connected(val protocol: String, val vin: String? = null) : ObdConnectionState
    data class Error(val reason: ObdError, val retrying: Boolean) : ObdConnectionState
}

enum class ObdSensor { RPM, SPEED, COOLANT, LOAD, THROTTLE, INTAKE, FUEL_LEVEL, VOLTAGE, FUEL_RATE }

/** SI-based values. Missing/failed samples remain null, never fabricated as zero.
 * Sensor and snapshot timestamps are epoch milliseconds; speed freshness also has a separate monotonic marker.
 */
data class VehicleSnapshot(
    val rpm: Double? = null,
    val speedKph: Double? = null,
    val coolantC: Double? = null,
    val loadPercent: Double? = null,
    val throttlePercent: Double? = null,
    val intakeC: Double? = null,
    val fuelLevelPercent: Double? = null,
    val moduleVoltage: Double? = null,
    val fuelRateLitresPerHour: Double? = null,
    val updatedAtEpochMillis: Long? = null,
    /** Monotonic elapsed-realtime sample time for speed; unrelated sensor updates never refresh it. */
    val speedSampledAtElapsedRealtimeMillis: Long? = null,
    val sampledAtEpochMillis: Map<ObdSensor, Long> = emptyMap(),
)

data class ObdDtcReport(
    val stored: List<String>,
    /** Null when Mode 07 is unsupported, distinct from a supported empty pending list. */
    val pending: List<String>?,
    val milOn: Boolean?,
    val scannedAtEpochMillis: Long,
)

class ObdUnavailableException : IllegalStateException("OBD is unavailable in this distribution")
class ObdNotConnectedException : IllegalStateException("OBD is not connected")
class ObdClearConfirmationRequiredException : IllegalArgumentException("Explicit confirmation is required to clear DTCs")
