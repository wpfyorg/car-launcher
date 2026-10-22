package org.wpfy.carlauncher.feature.settings

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** A text-to-speech voice as the system reports it. */
data class SystemVoice(val name: String, val localeName: String)

/** One row of the voice picker; a null [name] is the system default voice. */
data class VoiceChoice(val name: String?, val label: String)

val SystemDefaultVoice = VoiceChoice(name = null, label = "System default")

/**
 * The picker rows: the system default first, then [voices] by name. Voices of one language and region share
 * a name, so repeats are numbered ("English (United States) 2") to tell them apart.
 */
fun voiceChoices(voices: List<SystemVoice>): List<VoiceChoice> {
    val sorted = voices.sortedBy(SystemVoice::name)
    val totals = sorted.groupingBy(SystemVoice::localeName).eachCount()
    val seen = mutableMapOf<String, Int>()
    return listOf(SystemDefaultVoice) + sorted.map { voice ->
        val number = (seen[voice.localeName] ?: 0) + 1
        seen[voice.localeName] = number
        val label = if ((totals[voice.localeName] ?: 0) > 1) "${voice.localeName} $number" else voice.localeName
        VoiceChoice(voice.name, label)
    }
}

/** The row of [name]; the system default when it is unset or no longer installed. */
fun currentVoiceChoice(choices: List<VoiceChoice>, name: String?): VoiceChoice =
    choices.firstOrNull { it.name != null && it.name == name } ?: SystemDefaultVoice

/** Whether a voice with these [features] is installed; one still to be downloaded cannot speak. */
fun isInstalledVoice(features: Set<String>?): Boolean =
    features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true

/** The saved voice when this device still offers it, else null (the system default). */
fun resolveVoiceName(available: Collection<String>, requested: String?): String? =
    requested?.takeIf { it in available }

/** The installed voices for the device language, read from a short-lived text-to-speech engine. */
suspend fun loadSystemVoices(context: Context, locale: Locale = Locale.getDefault()): List<SystemVoice> =
    suspendCancellableCoroutine { continuation ->
        // The engine can report its status before the constructor returns: hold it until the instance exists.
        var engine: TextToSpeech? = null
        var earlyStatus: Int? = null
        fun finish(instance: TextToSpeech, status: Int) {
            val voices = if (status == TextToSpeech.SUCCESS) {
                runCatching {
                    instance.voices.orEmpty()
                        .filter { it.locale.language == locale.language && isInstalledVoice(it.features) }
                        .map { SystemVoice(it.name, it.locale.getDisplayName(locale)) }
                }.getOrDefault(emptyList())
            } else {
                emptyList()
            }
            runCatching { instance.shutdown() }
            if (continuation.isActive) continuation.resume(voices)
        }
        val created = TextToSpeech(context.applicationContext) { status ->
            val instance = engine
            if (instance == null) earlyStatus = status else finish(instance, status)
        }
        engine = created
        earlyStatus?.let { finish(created, it) }
        continuation.invokeOnCancellation { runCatching { created.shutdown() } }
    }
