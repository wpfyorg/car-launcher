package com.openlauncher.app.feature.media

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
) {
    val progress: Float
        get() = if (durationMs > 0L) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}
