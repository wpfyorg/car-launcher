package org.wpfy.carlauncher.core.diagnostics

import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.Base64

internal fun buildDiagnosticsReport(
    diagnostics: String,
    logs: String,
    generatedAt: String,
): String {
    val approvedLogs = logs
        .lineSequence()
        .mapNotNull(::approvedExportLogLine)
        .joinToString("\n")

    return buildString {
    appendLine("Car Launcher beta diagnostics")
    appendLine("Generated: $generatedAt")
    appendLine()
    appendLine(diagnostics.trim())
    appendLine()
    appendLine("Recent Car Launcher logs")
        appendLine(approvedLogs.ifBlank { "No app logs captured yet." })
    }
}

internal fun encodePersistedLogRecord(
    timestamp: String,
    level: String,
    tag: String,
    message: String,
): String = listOf(
    PersistedLogVersion,
    timestamp,
    level,
    tag.base64(),
    message.base64(),
).joinToString("\t")

private fun approvedExportLogLine(line: String): String? {
    val fields = line.split('\t')
    if (fields.size != PersistedLogFieldCount || fields[0] != PersistedLogVersion) return null

    val timestamp = fields[1]
    val level = fields[2]
    val tag = fields[3].decodeBase64() ?: return null
    val message = fields[4].decodeBase64() ?: return null

    if (level != "I" || !timestamp.isValidOffsetTimestamp()) return null
    if (!isApprovedLogMessage(tag, message)) return null

    return "$timestamp I/$tag: $message"
}

private fun isApprovedLogMessage(tag: String, message: String): Boolean = when (tag) {
    "AppLog" -> message == "app-owned diagnostics log initialized"
    "DiagnosticsLogShare" -> message == "diagnostics log share requested"
    "MainActivity" -> message == "onStart" ||
        message == "onResume" ||
        message == "onPause" ||
        message == "onStop" ||
        OnCreatePattern.matches(message) ||
        OnDestroyPattern.matches(message)
    else -> false
}

private fun String.base64(): String = Base64.getEncoder().encodeToString(toByteArray(Charsets.UTF_8))

private fun String.decodeBase64(): String? = runCatching {
    String(Base64.getDecoder().decode(this), Charsets.UTF_8)
}.getOrNull()

private fun String.isValidOffsetTimestamp(): Boolean = try {
    OffsetDateTime.parse(this)
    true
} catch (_: DateTimeParseException) {
    false
}

private const val PersistedLogVersion = "CL1"
private const val PersistedLogFieldCount = 5
private val OnCreatePattern = Regex("onCreate restored=(true|false)")
private val OnDestroyPattern = Regex("onDestroy changingConfigurations=(true|false)")
