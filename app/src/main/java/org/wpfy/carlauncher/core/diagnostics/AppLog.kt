package org.wpfy.carlauncher.core.diagnostics

import android.content.Context
import android.util.Log
import java.io.File
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object AppLog {
    @Volatile
    private var store: BoundedLogStore? = null

    fun initialize(context: Context) {
        if (store != null) return
        synchronized(this) {
            if (store == null) {
                store = BoundedLogStore(File(context.filesDir, "diagnostics"))
            }
        }
        i(Tag, "app-owned diagnostics log initialized")
    }

    fun snapshot(): String = runCatching { store?.readText().orEmpty() }.getOrDefault("")

    fun d(tag: String, message: String): Int = write(Log.DEBUG, tag, message, null)

    fun i(tag: String, message: String): Int = write(Log.INFO, tag, message, null)

    fun w(tag: String, message: String): Int = write(Log.WARN, tag, message, null)

    fun w(tag: String, message: String, error: Throwable): Int =
        write(Log.WARN, tag, message, error)

    fun e(tag: String, message: String, error: Throwable? = null): Int =
        write(Log.ERROR, tag, message, error)

    fun wtf(tag: String, message: String): Int = write(Log.ASSERT, tag, message, null)

    private fun write(
        priority: Int,
        tag: String,
        message: String,
        error: Throwable?,
    ): Int {
        val boundedMessage = message.take(MaxEntryCharacters)
        val logcatResult = when (priority) {
            Log.DEBUG -> Log.d(tag, boundedMessage)
            Log.INFO -> Log.i(tag, boundedMessage)
            Log.WARN -> if (error == null) Log.w(tag, boundedMessage) else Log.w(tag, boundedMessage, error)
            Log.ERROR -> if (error == null) Log.e(tag, boundedMessage) else Log.e(tag, boundedMessage, error)
            Log.ASSERT -> Log.wtf(tag, boundedMessage)
            else -> Log.println(priority, tag, boundedMessage)
        }

        val persistedMessage = buildString {
            append(boundedMessage)
            if (error != null) {
                append('\n')
                append(Log.getStackTraceString(error))
            }
        }.take(MaxEntryCharacters)
        runCatching {
            val timestamp = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            store?.append(
                encodePersistedLogRecord(
                    timestamp = timestamp,
                    level = levelName(priority),
                    tag = tag,
                    message = persistedMessage,
                ),
            )
        }
        return logcatResult
    }

    private fun levelName(priority: Int): String = when (priority) {
        Log.DEBUG -> "D"
        Log.INFO -> "I"
        Log.WARN -> "W"
        Log.ERROR -> "E"
        Log.ASSERT -> "A"
        else -> priority.toString()
    }

    private const val MaxEntryCharacters = 12_000
    private const val Tag = "AppLog"
}
