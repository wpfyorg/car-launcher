package org.wpfy.carlauncher.feature.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.Rating
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.service.notification.NotificationListenerService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import org.wpfy.carlauncher.feature.notifications.LauncherNotificationListenerService
import org.wpfy.carlauncher.feature.notifications.NotificationListenerBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

interface MediaRepository {
    val state: StateFlow<MediaState>
    val sessionController: MediaSessionController

    fun start()
    fun stop()
    fun refresh()
    fun selectSession(id: String)
    fun setPreferredPackage(packageName: String?)

    /** Re-reads the Bluetooth permission after the user answered the request or changed it in system settings. */
    fun refreshBluetoothPermission()
}

class AndroidMediaRepository(
    context: Context,
    private val bluetoothMonitor: BluetoothLinkMonitor = AndroidBluetoothLinkMonitor(context),
    /** True only for the launcher's own instance; secondary readers must not publish. */
    private val publishSelection: Boolean = false,
) : MediaRepository {
    private val appContext = context.applicationContext
    private val mediaSessionManager =
        appContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val listenerComponent = ComponentName(
        appContext,
        LauncherNotificationListenerService::class.java,
    )
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sourceCache = mutableMapOf<String, MediaSource>()

    private val _state = MutableStateFlow(MediaState())
    override val state: StateFlow<MediaState> = _state.asStateFlow()

    private val trackedControllers = linkedMapOf<MediaSession.Token, MediaController>()
    private val sessionIds = mutableMapOf<MediaSession.Token, String>()
    private var nextSessionId = 1L
    private var selectedController: MediaController? = null
    private val selector = MediaSourceSelector()
    private var preferredPackage: String? = null
    private var sessionsListenerRegistered = false
    private var started = false
    private var tickerJob: Job? = null
    private var linkJob: Job? = null

    override val sessionController = MediaSessionController { selectedController }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            refreshFromTrackedControllers()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            refreshFromTrackedControllers()
        }

        override fun onSessionDestroyed() {
            refreshSessions()
        }
    }

    private val activeSessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            attachControllers(controllers.orEmpty())
        }

    private val notificationRefreshListener: () -> Unit = {
        mainHandler.post(::refreshSessions)
    }

    override fun start() {
        if (started) return
        started = true
        NotificationListenerBridge.addListener(notificationRefreshListener)
        bluetoothMonitor.start()
        // A new link only reselects; the sessions themselves are unchanged.
        linkJob = scope.launch {
            bluetoothMonitor.link.collect { link ->
                _state.value = _state.value.copy(bluetooth = link)
                if (hasMediaAccess()) refreshFromTrackedControllers()
            }
        }
        refresh()
        tickerJob = scope.launch {
            while (isActive) {
                refreshAccessAndPosition()
                delay(1_000L)
            }
        }
    }

    override fun stop() {
        if (!started) return
        started = false
        tickerJob?.cancel()
        tickerJob = null
        linkJob?.cancel()
        linkJob = null
        bluetoothMonitor.stop()
        NotificationListenerBridge.removeListener(notificationRefreshListener)
        unregisterSessionsListener()
        clearControllers()
    }

    override fun refresh() {
        mainHandler.post(::refreshAccessAndSessions)
    }

    override fun selectSession(id: String) {
        val select = {
            val controller = trackedControllers.values.firstOrNull { controller ->
                sessionIdFor(controller) == id
            }
            if (controller != null) {
                selector.choose(id)
                refreshFromTrackedControllers()
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            select()
        } else {
            mainHandler.post(select)
        }
    }

    /** Drops the in-session choice made through [selectSession]; the preference applies again. */
    fun forgetSessionChoice() {
        val forget = {
            selector.forgetChoice()
            if (hasMediaAccess()) refreshFromTrackedControllers()
        }
        if (Looper.myLooper() == Looper.getMainLooper()) forget() else mainHandler.post(forget)
    }

    override fun setPreferredPackage(packageName: String?) {
        val update = {
            if (preferredPackage != packageName) {
                preferredPackage = packageName
                if (hasMediaAccess()) refreshFromTrackedControllers()
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) update() else mainHandler.post(update)
    }

    override fun refreshBluetoothPermission() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            bluetoothMonitor.refreshPermission()
        } else {
            mainHandler.post(bluetoothMonitor::refreshPermission)
        }
    }

    private fun refreshAccessAndPosition() {
        val hasAccess = hasMediaAccess()
        if (!hasAccess) {
            if (_state.value.accessStatus != MediaAccessStatus.PermissionRequired ||
                _state.value.hasSession
            ) {
                unregisterSessionsListener()
                clearControllers()
                _state.value = MediaState(
                    accessStatus = MediaAccessStatus.PermissionRequired,
                    bluetooth = bluetoothMonitor.link.value,
                )
            }
            return
        }

        if (!sessionsListenerRegistered) {
            refreshAccessAndSessions()
            return
        }

        if (selectedController == null) {
            refreshSessions()
        } else {
            refreshFromTrackedControllers()
        }
    }

    private fun refreshAccessAndSessions() {
        if (!hasMediaAccess()) {
            unregisterSessionsListener()
            clearControllers()
            _state.value = MediaState(
                accessStatus = MediaAccessStatus.PermissionRequired,
                bluetooth = bluetoothMonitor.link.value,
            )
            return
        }

        registerSessionsListener()
        if (NotificationListenerBridge.service == null) {
            runCatching { NotificationListenerService.requestRebind(listenerComponent) }
        }
        refreshSessions()
    }

    private fun registerSessionsListener() {
        if (sessionsListenerRegistered) return
        sessionsListenerRegistered = runCatching {
            mediaSessionManager.addOnActiveSessionsChangedListener(
                activeSessionsChangedListener,
                listenerComponent,
                mainHandler,
            )
            true
        }.getOrDefault(false)
    }

    private fun unregisterSessionsListener() {
        if (!sessionsListenerRegistered) return
        runCatching {
            mediaSessionManager.removeOnActiveSessionsChangedListener(activeSessionsChangedListener)
        }
        sessionsListenerRegistered = false
    }

    private fun refreshSessions() {
        if (!hasMediaAccess()) {
            refreshAccessAndSessions()
            return
        }

        val controllers = runCatching {
            mediaSessionManager.getActiveSessions(listenerComponent)
        }.getOrElse {
            emptyList()
        }
        attachControllers(controllers)
    }

    private fun attachControllers(controllers: List<MediaController>) {
        val incomingByToken = controllers.associateBy(MediaController::getSessionToken)
        val removedTokens = trackedControllers.keys - incomingByToken.keys
        removedTokens.forEach { token ->
            trackedControllers.remove(token)?.unregisterCallback(controllerCallback)
            sessionIds.remove(token)
        }

        controllers.forEach { controller ->
            val token = controller.sessionToken
            if (trackedControllers.containsKey(token)) return@forEach
            trackedControllers[token] = controller
            controller.registerCallback(controllerCallback, mainHandler)
        }

        refreshFromTrackedControllers()
    }

    private fun controllerPriority(controller: MediaController): Int {
        return when (controller.playbackState?.state) {
            PlaybackState.STATE_PLAYING -> 0
            PlaybackState.STATE_BUFFERING,
            PlaybackState.STATE_CONNECTING,
            -> 1
            PlaybackState.STATE_PAUSED -> 2
            else -> if (controller.metadata != null) 3 else 4
        }
    }

    private fun publishSelected() {
        if (!publishSelection) return
        val selected = selectedController
        ActiveMediaSelection.publish(selected?.let { ActiveMedia(it.packageName, it.sessionToken) })
    }

    /** This repository's id for the session behind [token], once it tracks that session. Main thread. */
    fun sessionIdOf(token: MediaSession.Token): String? = sessionIds[token]

    private fun clearControllers() {
        trackedControllers.values.forEach { controller ->
            controller.unregisterCallback(controllerCallback)
        }
        trackedControllers.clear()
        sessionIds.clear()
        selectedController = null
        selector.clear()
        publishSelected()
    }

    private fun refreshFromTrackedControllers() {
        val link = bluetoothMonitor.link.value
        val candidates = trackedControllers.values.map { controller ->
            SessionCandidate(
                id = sessionIdFor(controller),
                packageName = controller.packageName,
                priority = controllerPriority(controller),
                isBluetooth = isBluetoothSessionPackage(controller.packageName),
                isError = controller.playbackState?.state == PlaybackState.STATE_ERROR,
            )
        }
        // A Bluetooth session that is not connected is dropped from the pager and the commands too.
        val usable = usableCandidates(candidates, link)
        val usableIds = usable.mapTo(mutableSetOf(), SessionCandidate::id)
        val controllers = trackedControllers.values.filter { sessionIdFor(it) in usableIds }
        selector.onBluetoothConnected(bluetoothConnectedSignal(link, candidates))
        if (controllers.isEmpty()) {
            selectedController = null
            selector.clear()
            publishSelected()
            _state.value = MediaState(accessStatus = MediaAccessStatus.Available, bluetooth = link)
            return
        }

        val sessions = controllers.map { sessionFor(it, link) }
        val sessionsById = sessions.associateBy(MediaSourceSession::id)
        val selectedSessionId = selector.select(usable, preferredPackage)?.id
        val selectedSession = selectedSessionId?.let(sessionsById::get)
        selectedController = selectedSessionId?.let { id ->
            controllers.firstOrNull { controller -> sessionIdFor(controller) == id }
        }
        if (selectedSession != null) publishSelected()

        if (selectedSession == null) {
            selectedController = null
            publishSelected()
            _state.value = MediaState(
                accessStatus = MediaAccessStatus.Available,
                availableSessions = sessions,
                bluetooth = link,
            )
            return
        }

        _state.value = MediaState(
            accessStatus = MediaAccessStatus.Available,
            hasSession = true,
            title = selectedSession.title,
            artist = selectedSession.artist,
            artwork = selectedSession.artwork,
            source = selectedSession.source,
            durationMs = selectedSession.durationMs,
            positionMs = selectedSession.positionMs,
            isPlaying = selectedSession.isPlaying,
            isRepeatEnabled = selectedSession.isRepeatEnabled,
            isShuffleEnabled = selectedSession.isShuffleEnabled,
            isFavorite = selectedSession.isFavorite,
            controls = selectedSession.controls,
            availableSessions = sessions,
            selectedSessionId = selectedSession.id,
            bluetooth = link,
        )
    }

    private fun sessionFor(controller: MediaController, link: BluetoothLink): MediaSourceSession {
        val metadata = controller.metadata
        val playbackState = controller.playbackState
        val durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)
            ?.coerceAtLeast(0L)
            ?: 0L
        val source = sourceFor(controller.packageName, link)
        val title = metadata.firstNonBlank(
            MediaMetadata.METADATA_KEY_DISPLAY_TITLE,
            MediaMetadata.METADATA_KEY_TITLE,
        ).ifBlank { "Unknown track" }
        val artist = metadata.firstNonBlank(
            MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE,
            MediaMetadata.METADATA_KEY_ARTIST,
            MediaMetadata.METADATA_KEY_ALBUM_ARTIST,
            MediaMetadata.METADATA_KEY_ALBUM,
        )
        val actions = playbackState?.actions ?: 0L
        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING ||
            playbackState?.state == PlaybackState.STATE_BUFFERING
        val rating = metadata?.getRating(MediaMetadata.METADATA_KEY_USER_RATING)
        val supportsHeartRating = controller.ratingType == Rating.RATING_HEART &&
            playbackState.hasAction(PlaybackState.ACTION_SET_RATING)
        val repeatAction = playbackState.customActionMatching("repeat")
        val shuffleAction = playbackState.customActionMatching("shuffle")

        return MediaSourceSession(
            id = sessionIdFor(controller),
            title = title,
            artist = artist,
            artwork = metadata.artwork(),
            source = source,
            durationMs = durationMs,
            positionMs = playbackState.currentPosition(durationMs),
            isPlaying = isPlaying,
            isRepeatEnabled = null,
            isShuffleEnabled = null,
            isFavorite = if (supportsHeartRating) rating?.hasHeart() else null,
            controls = MediaControls(
                canPlayPause = actions has PlaybackState.ACTION_PLAY ||
                    actions has PlaybackState.ACTION_PAUSE ||
                    actions has PlaybackState.ACTION_PLAY_PAUSE,
                canSkipPrevious = actions has PlaybackState.ACTION_SKIP_TO_PREVIOUS,
                canSkipNext = actions has PlaybackState.ACTION_SKIP_TO_NEXT,
                canSeek = actions has PlaybackState.ACTION_SEEK_TO,
                canRepeat = repeatAction != null,
                canShuffle = shuffleAction != null,
                canFavorite = supportsHeartRating || playbackState.favoriteCustomAction() != null,
            ),
        )
    }

    private fun sessionIdFor(controller: MediaController): String {
        val token = controller.sessionToken
        return sessionIds.getOrPut(token) {
            "${controller.packageName}:${nextSessionId++}"
        }
    }

    /** The source of a session. The Bluetooth source is labelled with the phone's name, which is never cached. */
    private fun sourceFor(packageName: String, link: BluetoothLink): MediaSource? {
        if (!isBluetoothSessionPackage(packageName)) return sourceFor(packageName)
        val base = sourceFor(packageName) ?: MediaSource(packageName, label = "")
        return base.copy(label = bluetoothSourceLabel(link))
    }

    private fun sourceFor(packageName: String): MediaSource? {
        if (packageName.isBlank()) return null
        sourceCache[packageName]?.let { return it }

        return runCatching {
            val packageManager = appContext.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            MediaSource(
                packageName = packageName,
                label = packageManager.getApplicationLabel(applicationInfo).toString(),
                icon = runCatching {
                    packageManager.getApplicationIcon(applicationInfo).toBitmap(72, 72)
                }.getOrNull(),
            )
        }.getOrNull()?.also { sourceCache[packageName] = it }
    }

    private fun MediaMetadata?.artwork(): Bitmap? {
        if (this == null) return null

        val directArtwork = listOf(
            MediaMetadata.METADATA_KEY_ALBUM_ART,
            MediaMetadata.METADATA_KEY_ART,
            MediaMetadata.METADATA_KEY_DISPLAY_ICON,
        ).firstNotNullOfOrNull { key ->
            runCatching { getBitmap(key) }.getOrNull()
        }
        if (directArtwork != null) return directArtwork

        val artworkUri = firstNonBlank(
            MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
            MediaMetadata.METADATA_KEY_ART_URI,
            MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI,
        )
        if (artworkUri.isBlank()) return null

        return runCatching {
            val uri = Uri.parse(artworkUri)
            if (uri.scheme !in setOf("content", "android.resource", "file")) return@runCatching null
            appContext.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
        }.getOrNull()
    }

    private fun hasMediaAccess(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(appContext)
            .contains(appContext.packageName)
    }
}

private fun MediaMetadata?.firstNonBlank(vararg keys: String): String {
    if (this == null) return ""
    return keys.firstNotNullOfOrNull { key ->
        getString(key)?.takeIf(String::isNotBlank)
    }.orEmpty()
}

private fun PlaybackState?.currentPosition(durationMs: Long): Long {
    if (this == null || position < 0L) return 0L
    val basePosition = position
    val adjustedPosition = if (
        state == PlaybackState.STATE_PLAYING &&
        lastPositionUpdateTime > 0L
    ) {
        val elapsedMs = (SystemClock.elapsedRealtime() - lastPositionUpdateTime).coerceAtLeast(0L)
        basePosition + (elapsedMs * playbackSpeed).roundToLong()
    } else {
        basePosition
    }
    return if (durationMs > 0L) {
        adjustedPosition.coerceIn(0L, durationMs)
    } else {
        adjustedPosition.coerceAtLeast(0L)
    }
}

private infix fun Long.has(action: Long): Boolean = this and action != 0L
