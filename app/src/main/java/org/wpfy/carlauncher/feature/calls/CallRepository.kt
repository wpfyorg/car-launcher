package org.wpfy.carlauncher.feature.calls

import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import androidx.core.graphics.drawable.toBitmap
import org.wpfy.carlauncher.core.diagnostics.AppLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CallRepository internal constructor(
    context: Context,
    private val decodeAvatar: (Context, Icon) -> Bitmap?,
) {
    constructor(context: Context) : this(context, ::loadIconBitmap)

    private val appContext = context.applicationContext
    private val _state = MutableStateFlow<CallState>(CallState.None)
    val state: StateFlow<CallState> = _state.asStateFlow()

    private var selectedKey: String? = null
    private var answerIntent: PendingIntent? = null
    private var declineIntent: PendingIntent? = null
    private var hangUpIntent: PendingIntent? = null

    @Synchronized
    internal fun updateNotifications(notifications: List<StatusBarNotification>) {
        val candidate = notifications.asSequence()
            .filter { it.packageName != appContext.packageName }
            .filter { it.notification?.category == Notification.CATEGORY_CALL }
            .filter { it.notification?.visibility != Notification.VISIBILITY_SECRET }
            .mapNotNull(::toCandidate)
            .sortedWith(compareByDescending<Candidate> { it.incoming }.thenByDescending { it.postTime })
            .firstOrNull()
        if (candidate == null) {
            publishNone()
            return
        }

        selectedKey = candidate.key
        answerIntent = candidate.answer
        declineIntent = candidate.decline
        hangUpIntent = candidate.hangUp
        val avatar = candidate.avatarIcon?.let { decodeAvatar(appContext, it) }
        _state.value = if (candidate.incoming) {
            CallState.Incoming(
                callerName = candidate.name,
                number = candidate.number,
                avatar = avatar,
                sourcePackage = candidate.packageName,
                canAnswer = candidate.answer != null,
                canDecline = candidate.decline != null,
            )
        } else {
            CallState.Active(
                callerName = candidate.name,
                number = candidate.number,
                avatar = avatar,
                sourcePackage = candidate.packageName,
                startedAtElapsedMs = candidate.startedAtElapsedMs,
                canHangUp = candidate.hangUp != null,
            )
        }
    }

    @Synchronized
    fun clear() = publishNone()

    fun answer() = dispatch(CallAction.Answer)

    fun decline() = dispatch(CallAction.Decline)

    fun hangUp() = dispatch(CallAction.HangUp)

    @Synchronized
    private fun dispatch(action: CallAction) {
        if (selectedKey == null || _state.value == CallState.None) return
        val intent = when (action) {
            CallAction.Answer -> answerIntent
            CallAction.Decline -> declineIntent
            CallAction.HangUp -> hangUpIntent
        } ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val options = ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
                    )
                intent.send(appContext, 0, null, null, null, null, options.toBundle())
            } else {
                intent.send()
            }
        }.onFailure {
            AppLog.w(Tag, "call action dispatch failed")
        }
    }

    private fun toCandidate(sbn: StatusBarNotification): Candidate? {
        val notification = sbn.notification ?: return null
        return runCatching {
            val extras = notification.extras
            val person = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable(Notification.EXTRA_CALL_PERSON, android.app.Person::class.java)
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelable(Notification.EXTRA_CALL_PERSON) as? android.app.Person
            }
            val actions = notification.actions.orEmpty().toList()
            val labels = actions.map { it.title?.toString().orEmpty() }
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
            val explicitIncomingText = isIncomingEvidence(title.orEmpty()) || isIncomingEvidence(text.orEmpty())
            val incomingLabel = labels.any(::isIncomingEvidence)
            val hasAnswerAction = actions.any { isAnswerLabel(it.title?.toString().orEmpty()) }
            val callType = when {
                Build.VERSION.SDK_INT >= 34 -> extras.getInt(Notification.EXTRA_CALL_TYPE, CALL_TYPE_UNKNOWN)
                else -> extras.getInt(CALL_TYPE_EXTRA, CALL_TYPE_UNKNOWN)
            }
            val isScreening = callType == CALL_TYPE_SCREENING
            val isIncoming = callType == CALL_TYPE_INCOMING || isScreening ||
                (callType == CALL_TYPE_UNKNOWN && (explicitIncomingText || incomingLabel || hasAnswerAction))
            val isActive = callType == CALL_TYPE_ONGOING
            val answer = if (isIncoming) {
                getPendingIntent(extras, Notification.EXTRA_ANSWER_INTENT)
                    ?: actions.firstIntent(::isAnswerLabel)
                    ?: actions.firstOrNull {
                        it.semanticAction == Notification.Action.SEMANTIC_ACTION_CALL &&
                            explicitIncomingText &&
                            !isCallbackLabel(it.title?.toString().orEmpty()) &&
                            !isDeclineLabel(it.title?.toString().orEmpty()) &&
                            !isHangUpLabel(it.title?.toString().orEmpty())
                    }?.actionIntent
            } else {
                null
            }
            val decline = if (isIncoming) {
                getPendingIntent(extras, Notification.EXTRA_DECLINE_INTENT)
                    ?: (if (isScreening) getPendingIntent(extras, Notification.EXTRA_HANG_UP_INTENT) else null)
                    ?: actions.firstIntent(::isDeclineLabel)
                    ?: (if (isScreening) actions.firstIntent(::isHangUpLabel) else null)
            } else {
                null
            }
            val hangUp = if (isActive || (!isIncoming && !incomingLabel)) {
                getPendingIntent(extras, Notification.EXTRA_HANG_UP_INTENT) ?: actions.firstIntent(::isHangUpLabel)
            } else {
                null
            }
            val name = person?.name?.toString()?.trim()?.takeIf(String::isNotBlank)
                ?: title?.takeIf { it.isNotBlank() && !isStatusText(it) }
                ?: text?.takeIf {
                    it.isNotBlank() && !isStatusText(it) && numberPattern.find(it) == null
                }
            val number = person?.uri?.let(::numberFromUri)
                ?: numberFromText(title)
                ?: numberFromText(text)
            val started = if (extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER) &&
                !extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN) && !isIncoming
            ) {
                val wallNow = System.currentTimeMillis()
                val elapsedNow = SystemClock.elapsedRealtime()
                val whenElapsed = notification.`when` + (elapsedNow - wallNow)
                whenElapsed.takeIf {
                    notification.`when` > 0L && notification.`when` <= wallNow && it > 0L && it <= elapsedNow
                }
            } else {
                null
            }

            Candidate(
                key = sbn.key,
                packageName = sbn.packageName,
                postTime = sbn.postTime,
                incoming = isIncoming,
                name = name,
                number = number,
                avatarIcon = person?.icon,
                answer = answer,
                decline = decline,
                hangUp = hangUp,
                startedAtElapsedMs = started,
            )
        }.onFailure {
            AppLog.w(Tag, "call notification parsing failed")
        }.getOrNull()
    }

    private fun publishNone() {
        selectedKey = null
        answerIntent = null
        declineIntent = null
        hangUpIntent = null
        _state.value = CallState.None
    }

    private fun List<Notification.Action>.firstIntent(matches: (String) -> Boolean): PendingIntent? =
        firstOrNull { matches(it.title?.toString().orEmpty()) }?.actionIntent

    private fun isIncomingEvidence(value: String): Boolean =
        incomingPattern.containsMatchIn(value) && !callbackPattern.containsMatchIn(value)

    private fun isCallbackLabel(value: String): Boolean = callbackPattern.containsMatchIn(value)

    private fun isAnswerLabel(value: String): Boolean = answerPattern.containsMatchIn(value)

    private fun isDeclineLabel(value: String): Boolean = declinePattern.containsMatchIn(value)

    private fun isHangUpLabel(value: String): Boolean = hangUpPattern.containsMatchIn(value)

    private fun isStatusText(value: String): Boolean = statusPattern.containsMatchIn(value)

    private fun numberFromUri(value: String): String? {
        val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return null
        if (!uri.scheme.equals("tel", ignoreCase = true)) return null
        return uri.schemeSpecificPart.takeIf { numberPattern.matches(it) }
    }

    private fun numberFromText(value: String?): String? {
        if (value == null || isStatusText(value)) return null
        return numberPattern.find(value)?.value
    }

    private data class Candidate(
        val key: String,
        val packageName: String,
        val postTime: Long,
        val incoming: Boolean,
        val name: String?,
        val number: String?,
        val avatarIcon: Icon?,
        val answer: PendingIntent?,
        val decline: PendingIntent?,
        val hangUp: PendingIntent?,
        val startedAtElapsedMs: Long?,
    )

    private enum class CallAction { Answer, Decline, HangUp }

    private fun getPendingIntent(extras: android.os.Bundle, key: String): PendingIntent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(key, PendingIntent::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable(key) as? PendingIntent
        }

    companion object {
        private const val Tag = "Calls"

        private fun loadIconBitmap(context: Context, icon: Icon): Bitmap? = runCatching {
            icon.loadDrawable(context)?.toBitmap()
        }.getOrNull()
        private const val CALL_TYPE_INCOMING = Notification.CallStyle.CALL_TYPE_INCOMING
        private const val CALL_TYPE_ONGOING = Notification.CallStyle.CALL_TYPE_ONGOING
        private const val CALL_TYPE_SCREENING = Notification.CallStyle.CALL_TYPE_SCREENING
        private const val CALL_TYPE_UNKNOWN = Notification.CallStyle.CALL_TYPE_UNKNOWN
        private const val CALL_TYPE_EXTRA = "android.callType"

        private val incomingPattern = Regex("\\b(incoming|answer|ringing)\\b", RegexOption.IGNORE_CASE)
        private val callbackPattern = Regex("\\b(call back|callback|return call)\\b", RegexOption.IGNORE_CASE)
        private val answerPattern = Regex("\\b(answer|accept|pick up)\\b", RegexOption.IGNORE_CASE)
        private val declinePattern = Regex("\\b(decline|reject)\\b", RegexOption.IGNORE_CASE)
        private val hangUpPattern = Regex("\\b(end|hang ?up|disconnect)\\b", RegexOption.IGNORE_CASE)
        private val statusPattern = Regex(
            "\\b(call|incoming|outgoing|missed|ongoing|active|ringing|calling|call back|callback|" +
                "ended|declined|rejected|screening)\\b",
            RegexOption.IGNORE_CASE,
        )
        private val numberPattern = Regex("(?<![\\w])\\+?[0-9][0-9 ().-]{4,}[0-9](?![\\w])")

        @Volatile
        private var instance: CallRepository? = null

        fun getInstance(context: Context): CallRepository = instance ?: synchronized(this) {
            instance ?: CallRepository(context.applicationContext).also { instance = it }
        }
    }
}
