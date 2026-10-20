package org.wpfy.carlauncher.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import org.wpfy.carlauncher.design.screens.KitHudState

/** A prompt shown after passing a camera, asking whether it is still there. */
@Immutable
data class MapAccessoryConfirm(val title: String, val secondsLeft: Int)

/**
 * Optional layer an edition adds to the Home HUD beside the guidance parts: the speed readout, the
 * camera alert and the report flow. It does not draw: it says what the kit HUD shows ([MapAccessoryHud])
 * and Home draws it with the rest, in the kit's places. Editions without one pass `null` and the HUD
 * has no speed cluster and no Report button.
 */
@Stable
interface MapAccessory {
    /** Whether the Home Drive Card can show the accessory's camera confirmation prompt. */
    fun setDriveCardVisible(visible: Boolean) = Unit

    /**
     * The accessory needs the map in view right now (a camera alert escalated): small screens
     * bring the map page back and hold off swiping to the media page while this is true. Read
     * from a derived state, so cheap to poll.
     */
    fun demandsMapPage(): Boolean = false

    /** What the HUD shows now. Composable: it follows the speed, the alert and the report flow as they change. */
    @Composable
    fun hud(): MapAccessoryHud
}

/** The accessory's part of the kit HUD, and its taps. */
@Immutable
class MapAccessoryHud(
    val speed: KitHudState.Speed,
    val alert: KitHudState.Alert? = null,
    /** The alert's button: mutes it, or dismisses it once muted. */
    val onAlertButton: () -> Unit = {},
    /** The Report button is shown (there is a report flow). */
    val hasReport: Boolean = false,
    val reportSheet: KitHudState.ReportSheet? = null,
    val reportSent: KitHudState.ReportSent? = null,
    val onReport: () -> Unit = {},
    val onCloseReport: () -> Unit = {},
    val onUndoReport: () -> Unit = {},
    /** A live reading over the posted limit. */
    val over: Boolean = false,
    /** Metres to the camera of [alert]; null without a camera ahead. */
    val cameraMeters: Double? = null,
    /** Stable policy warning distance for this camera encounter; null without a camera alert. */
    val cameraWarningDistanceMeters: Double? = null,
    /** The camera ahead as the Drive Card's pill says it ("Speed camera · 300 m"); null without one. */
    val cameraLabel: String? = null,
    /** The camera's own posted limit, in the speed's unit, for when the road's limit is unknown. */
    val cameraLimit: String? = null,
    /** Altitude of the current fix in metres; null when the fix has none or is not current. */
    val altitudeMeters: Double? = null,
    /** The camera alert is muted ([onAlertButton] then dismisses it). */
    val cameraMuted: Boolean = false,
    val confirmPrompt: MapAccessoryConfirm? = null,
    val onConfirmYes: () -> Unit = {},
    val onConfirmNo: () -> Unit = {},
)
