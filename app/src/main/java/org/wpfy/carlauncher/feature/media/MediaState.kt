package org.wpfy.carlauncher.feature.media

import android.graphics.Bitmap

enum class MediaAccessStatus {
    PermissionRequired,
    Available,
}

data class MediaSource(
    val packageName: String,
    val label: String,
    val icon: Bitmap? = null,
)

data class MediaControls(
    val canPlayPause: Boolean = false,
    val canSkipPrevious: Boolean = false,
    val canSkipNext: Boolean = false,
    val canSeek: Boolean = false,
    val canRepeat: Boolean = false,
    val canShuffle: Boolean = false,
    val canFavorite: Boolean = false,
)

data class MediaSourceSession(
    val id: String,
    val title: String = "",
    val artist: String = "",
    val artwork: Bitmap? = null,
    val source: MediaSource? = null,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isRepeatEnabled: Boolean? = null,
    val isShuffleEnabled: Boolean? = null,
    val isFavorite: Boolean? = null,
    val controls: MediaControls = MediaControls(),
) {
    val progress: Float
        get() = if (durationMs > 0L) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}

data class MediaState(
    val accessStatus: MediaAccessStatus = MediaAccessStatus.PermissionRequired,
    val hasSession: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val artwork: Bitmap? = null,
    val source: MediaSource? = null,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isRepeatEnabled: Boolean? = null,
    val isShuffleEnabled: Boolean? = null,
    val isFavorite: Boolean? = null,
    val controls: MediaControls = MediaControls(),
    val availableSessions: List<MediaSourceSession> = emptyList(),
    val selectedSessionId: String? = null,
    /** The Bluetooth phone link; carries the device name and stays current even without media access. */
    val bluetooth: BluetoothLink = BluetoothLink(),
) {
    val progress: Float
        get() = if (durationMs > 0L) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val selectedSession: MediaSourceSession?
        get() = availableSessions.firstOrNull { it.id == selectedSessionId }
            ?: if (hasSession) {
                MediaSourceSession(
                    id = selectedSessionId ?: "selected",
                    title = title,
                    artist = artist,
                    artwork = artwork,
                    source = source,
                    durationMs = durationMs,
                    positionMs = positionMs,
                    isPlaying = isPlaying,
                    isRepeatEnabled = isRepeatEnabled,
                    isShuffleEnabled = isShuffleEnabled,
                    isFavorite = isFavorite,
                    controls = controls,
                )
            } else {
                null
            }
}

/** The progress bar can seek when the session offers seeking and the track has a length. */
internal fun MediaSourceSession.canSeekTrack(): Boolean = controls.canSeek && durationMs > 0L

/** The position in a track [durationMs] long at [fraction] (0 to 1), clamped to the track. */
internal fun seekPositionMs(fraction: Float, durationMs: Long): Long {
    if (durationMs <= 0L || fraction.isNaN()) return 0L
    return Math.round(fraction.coerceIn(0f, 1f).toDouble() * durationMs).coerceIn(0L, durationMs)
}

/** Formats milliseconds as minutes and two-digit seconds, or "--:--" for nonpositive durations. */
internal fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "--:--"
    val totalSeconds = durationMs / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
