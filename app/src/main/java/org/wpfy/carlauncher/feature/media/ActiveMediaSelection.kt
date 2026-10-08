package org.wpfy.carlauncher.feature.media

import android.media.session.MediaSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** The session the launcher shows: its app and its token, which is the same in every repository. */
data class ActiveMedia(val packageName: String, val sessionToken: MediaSession.Token? = null)

/**
 * Process-level, read-only view of the media session the launcher currently shows. The launcher's
 * repository publishes it so other surfaces (the Free navigation panel) can follow the driver's pick.
 */
object ActiveMediaSelection {
    private val _selection = MutableStateFlow<ActiveMedia?>(null)
    val selection: StateFlow<ActiveMedia?> = _selection

    internal fun publish(selection: ActiveMedia?) {
        _selection.value = selection
    }
}

/** The launcher's current pick wins over the stored preference; blank counts as none. */
internal fun resolvePreferredPackage(active: String?, stored: String?): String? =
    active?.takeIf { it.isNotBlank() } ?: stored?.takeIf { it.isNotBlank() }

/**
 * Session id the panel must hold to match the launcher's pick; null only when none matches, so an
 * already selected pick still reads as present. The launcher's exact session wins; otherwise the
 * first session of its app.
 */
internal fun sessionToSelect(
    sessions: List<MediaSourceSession>,
    activePackage: String?,
    activeSessionId: String? = null,
): String? {
    if (activePackage.isNullOrBlank()) return null
    val target = activeSessionId?.let { id -> sessions.firstOrNull { it.id == id } }
        ?: sessions.firstOrNull { it.source?.packageName == activePackage }
        ?: return null
    return target.id
}
