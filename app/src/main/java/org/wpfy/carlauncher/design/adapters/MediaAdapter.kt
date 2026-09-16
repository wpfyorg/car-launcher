package org.wpfy.carlauncher.design.adapters

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.wpfy.carlauncher.design.screens.KitHudState
import org.wpfy.carlauncher.design.screens.KitMediaState
import org.wpfy.carlauncher.design.theme.CarColors
import org.wpfy.carlauncher.feature.media.MediaSourceSession
import org.wpfy.carlauncher.feature.media.MediaState
import org.wpfy.carlauncher.feature.media.canSeekTrack
import org.wpfy.carlauncher.feature.media.formatDuration
import org.wpfy.carlauncher.feature.media.seekPositionMs

/*
 * Live media state -> the kit's media card and now-playing chip. Both follow the selected session,
 * with the live wording for a missing title or artist; no session draws neither. Artwork and the source icon go through
 * [art] so the conversion stays out of the way of plain JVM tests.
 */

private const val UnknownTrack = "Unknown track"

/** The media card/screen of the selected session, or null while nothing plays. */
fun kitMedia(
    state: MediaState,
    art: (Bitmap) -> ImageBitmap = Bitmap::asImageBitmap,
): KitMediaState? = state.selectedSession?.let { kitMedia(it, art) }

/** Maps a session to a media card, falling back to the source label when the artist is blank; the controls it does not offer are disabled. */
fun kitMedia(
    session: MediaSourceSession,
    art: (Bitmap) -> ImageBitmap = Bitmap::asImageBitmap,
): KitMediaState = KitMediaState(
    source = session.source?.label.orEmpty(),
    title = session.title.ifBlank { UnknownTrack },
    artist = session.artist.ifBlank { session.source?.label.orEmpty() },
    art = session.artwork?.let(art),
    elapsed = formatDuration(session.positionMs),
    total = formatDuration(session.durationMs),
    progress = session.progress,
    playing = session.isPlaying,
    canSkipPrevious = session.controls.canSkipPrevious,
    canPlayPause = session.controls.canPlayPause,
    canSkipNext = session.controls.canSkipNext,
    sourceIcon = session.source?.icon?.let(art),
    canSeek = session.canSeekTrack(),
    elapsedAt = { fraction -> formatDuration(seekPositionMs(fraction, session.durationMs)) },
)

/** The HUD's now-playing chip while nothing plays: it stays on the map, idle, and opens the Media screen. */
fun kitNowPlayingIdle(): KitHudState.NowPlaying = KitHudState.NowPlaying(
    title = "Not playing",
    artist = "Tap to open media",
    art = null,
    artFallback = CarColors.SurfaceElevated,
    playing = false,
    canPlayPause = false,
)

/** The HUD's now-playing chip of the selected session, or null while nothing plays. */
fun kitNowPlaying(
    state: MediaState,
    art: (Bitmap) -> ImageBitmap = Bitmap::asImageBitmap,
): KitHudState.NowPlaying? = state.selectedSession?.let { session ->
    KitHudState.NowPlaying(
        title = session.title.ifBlank { UnknownTrack },
        artist = session.artist.ifBlank { session.source?.label.orEmpty() },
        art = session.artwork?.let(art),
        artFallback = CarColors.SurfaceElevated,
        playing = session.isPlaying,
        canPlayPause = session.controls.canPlayPause,
    )
}
