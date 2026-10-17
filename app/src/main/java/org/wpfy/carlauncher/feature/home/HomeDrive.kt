package org.wpfy.carlauncher.feature.home

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.Locale
import kotlin.math.roundToInt
import org.wpfy.carlauncher.data.settings.UnitSystem
import org.wpfy.carlauncher.design.adapters.kitMedia
import org.wpfy.carlauncher.design.adapters.kitNoSpeed
import org.wpfy.carlauncher.design.adapters.speedUnitLabelFor
import org.wpfy.carlauncher.design.kit.KitDriveDots
import org.wpfy.carlauncher.design.kit.KitDriveMedia
import org.wpfy.carlauncher.design.kit.KitDriveState
import org.wpfy.carlauncher.design.kit.KitDriveTile
import org.wpfy.carlauncher.design.kit.PenKit
import org.wpfy.carlauncher.design.kit.KitSpeedPill
import org.wpfy.carlauncher.design.screens.KitHudState
import org.wpfy.carlauncher.feature.media.MediaSourceSession
import org.wpfy.carlauncher.feature.trip.TripState

/** The speed source the Drive Card names while the speed comes from the location fix. */
internal const val DriveSourceGps = "GPS"

internal data class DriveCardSpeedOverride(
    val speed: KitHudState.Speed,
    val overLimit: Boolean,
    val source: String = "OBD-II",
)

private const val AltitudeLabel = "ALT"
private const val FeetPerMetre = 3.28084
private const val MetresPerMile = 1609.344
private const val MilesPerMetrePerSecond = 2.2369363

/**
 * The Drive Card of Home: the accessory's speed, limit and camera (an edition without one shows "--" and no pill),
 * the selected media session's music pill (none: its idle "Not playing" look), and the trip, average and altitude tiles
 * ("--" until there is something to read).
 * A recording trip fills the existing trip and average tiles. [road] is the road the limit belongs to. Over the limit
 * without a camera the pill stays the limit pill (the speed turns the over colour); a camera pill carries the alert's
 * muted state, and in the last 50 m over the limit the alert is full, which the card draws as its critical look.
 * [dots] are the media source dots of the status row.
 */
internal fun homeDriveState(
    accessory: MapAccessoryHud?,
    media: MediaSourceSession?,
    units: UnitSystem,
    road: String? = null,
    source: String = DriveSourceGps,
    speedOverride: DriveCardSpeedOverride? = null,
    art: (Bitmap) -> ImageBitmap = Bitmap::asImageBitmap,
    dots: KitDriveDots? = null,
    tripState: TripState = TripState.Idle,
): KitDriveState {
    val speed = speedOverride?.speed ?: accessory?.speed ?: kitNoSpeed(units)
    val over = speedOverride?.overLimit ?: (accessory?.over == true)
    val camera = accessory?.cameraLabel
    val cameraSign = driveCameraSign(accessory, speedOverride)
    val confirm = accessory?.confirmPrompt
    val pill = when {
        accessory?.reportSent != null -> KitSpeedPill.Sent(
            accessory.reportSent.title,
            PenKit.SpeedPillSent.Undo.Label.Reference,
        )
        confirm != null -> KitSpeedPill.Confirm(
            confirm.title,
            PenKit.SpeedPillConfirm.Text.Timer.Reference.replace("8", confirm.secondsLeft.toString()),
        )
        camera != null -> KitSpeedPill.Camera(cameraSign, camera, muted = accessory?.cameraMuted == true, description = accessory?.alert?.description)
        speed.limit != null -> KitSpeedPill.Limit(speed.limit, road.orEmpty())
        else -> null
    }
    return KitDriveState(
        speed = speed.value,
        // The card sets its unit in capitals (Pen "KM/H"); the map's speed bubble keeps the reading's own case.
        unit = speed.unit.uppercase(Locale.ROOT),
        speedDescription = speed.description,
        over = over,
        withinLimit = speed.withinLimit && !over,
        pill = pill,
        source = speedOverride?.source ?: source,
        // Never absent: with no trip, no altitude or nothing playing, the pill shows its idle look.
        trip = (tripState as? TripState.Recording)?.trip?.let { driveTripTile(it.distanceM, units) } ?: driveIdleTile(PenKit.DriveCard.Tiles.Trip.Label.Text.Reference, distanceUnitLabelFor(units)),
        average = (tripState as? TripState.Recording)?.trip?.let { driveAverageTile(it.avgMovingSpeedMps, units) } ?: driveIdleTile(PenKit.DriveCard.Tiles.Avg.Label.Text.Reference, speedUnitLabelFor(units)),
        altitude = accessory?.altitudeMeters?.let { driveAltitudeTile(it, units) } ?: driveIdleTile(AltitudeLabel, if (units == UnitSystem.Imperial) "ft" else "m"),
        media = media?.let { driveMedia(it, art) } ?: DriveIdleMedia,
        alert = if (over && camera != null && accessory?.cameraMuted != true) {
            driveAlertIntensity(accessory?.cameraMeters, accessory?.cameraWarningDistanceMeters)
        } else {
            0f
        },
        critical = over && camera != null && accessory?.cameraMuted != true &&
            accessory?.cameraMeters?.let { it.isFinite() && it <= 50.0 } == true,
        dots = dots,
    )
}

/** A tile with nothing to read yet. */
private const val IdleNumber = "--"

private fun driveIdleTile(label: String, unit: String) = KitDriveTile(label, IdleNumber, unit)

/** The music pill while nothing plays: the note placeholder, a caption and a play button that has nothing to resume. */
internal val DriveIdleMedia = KitDriveMedia(
    title = "Not playing", artist = "Tap to open media", art = null, progress = 0f, playing = false,
    canSkipPrevious = false, canPlayPause = false, canSkipNext = false,
)

private fun driveTripTile(distanceM: Double, units: UnitSystem): KitDriveTile = KitDriveTile(
    label = PenKit.DriveCard.Tiles.Trip.Label.Text.Reference,
    number = String.format(Locale.ROOT, "%.1f", if (units == UnitSystem.Imperial) distanceM / MetresPerMile else distanceM / 1_000.0),
    unit = distanceUnitLabelFor(units),
)

private fun driveAverageTile(speedMps: Double, units: UnitSystem): KitDriveTile = KitDriveTile(
    label = PenKit.DriveCard.Tiles.Avg.Label.Text.Reference,
    number = (speedMps * if (units == UnitSystem.Imperial) MilesPerMetrePerSecond else 3.6).roundToInt().toString(),
    unit = speedUnitLabelFor(units),
)

internal fun distanceUnitLabelFor(units: UnitSystem): String = if (units == UnitSystem.Imperial) "mi" else "km"

/**
 * The limit a camera pill signs: the road's, else the camera's own. Null (neither known) leaves the sign out: the
 * pill still names the camera, with no limit and no placeholder.
 */
internal fun driveCameraSign(
    accessory: MapAccessoryHud?,
    speedOverride: DriveCardSpeedOverride? = null,
): String? = accessory?.takeIf { it.cameraLabel != null }?.let {
    speedOverride?.speed?.limit ?: it.speed.limit ?: it.cameraLimit
}

/** Continuous 0..1 glow through the full camera advisory envelope. */
fun driveAlertIntensity(cameraMeters: Double?, warningDistanceMeters: Double?): Float {
    val distance = cameraMeters?.takeIf { it.isFinite() } ?: return 0f
    val warningDistance = warningDistanceMeters?.takeIf { it.isFinite() && it > 0.0 } ?: return 0f
    val glowRange = warningDistance
    return (1.0 - (distance.coerceAtLeast(0.0) / glowRange)).coerceIn(0.0, 1.0).toFloat()
}

/** The altitude tile, in metres or feet. */
internal fun driveAltitudeTile(meters: Double, units: UnitSystem): KitDriveTile = when (units) {
    UnitSystem.Metric -> KitDriveTile(AltitudeLabel, meters.roundToInt().toString(), "m")
    UnitSystem.Imperial -> KitDriveTile(AltitudeLabel, (meters * FeetPerMetre).roundToInt().toString(), "ft")
}

/** The Drive Card's music pill of a session, worded like the media card. */
internal fun driveMedia(session: MediaSourceSession, art: (Bitmap) -> ImageBitmap = Bitmap::asImageBitmap): KitDriveMedia {
    val m = kitMedia(session, art)
    return KitDriveMedia(
        title = m.title, artist = m.artist, art = m.art, progress = m.progress, playing = m.playing,
        canSkipPrevious = m.canSkipPrevious, canPlayPause = m.canPlayPause, canSkipNext = m.canSkipNext,
    )
}
