package org.wpfy.carlauncher.core.diagnostics

import java.io.File

internal class BoundedLogStore(
    private val directory: File,
    private val maxFileBytes: Long = DefaultMaxFileBytes,
) {
    init {
        require(maxFileBytes >= 2L)
    }

    private val currentFile: File
        get() = File(directory, CurrentFileName)

    private val previousFile: File
        get() = File(directory, PreviousFileName)

    @Synchronized
    fun append(line: String) {
        directory.mkdirs()
        val fitted = fitLine(line)
        val entry = "$fitted\n".toByteArray(Charsets.UTF_8)
        if (currentFile.exists() && currentFile.length() + entry.size > maxFileBytes) {
            currentFile.copyTo(previousFile, overwrite = true)
            currentFile.writeBytes(byteArrayOf())
        }
        currentFile.appendBytes(entry)
    }

    @Synchronized
    fun readText(): String = buildString {
        if (previousFile.exists()) append(previousFile.readText())
        if (currentFile.exists()) append(currentFile.readText())
    }

    private fun fitLine(line: String): String {
        val contentLimit = maxFileBytes - 1L
        if (line.toByteArray(Charsets.UTF_8).size <= contentLimit) return line

        var candidate = line.take(contentLimit.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        while (candidate.toByteArray(Charsets.UTF_8).size > contentLimit && candidate.isNotEmpty()) {
            candidate = candidate.dropLast(1)
        }
        return candidate
    }

    companion object {
        const val DefaultMaxFileBytes = 256L * 1024L
        private const val CurrentFileName = "car-launcher.log"
        private const val PreviousFileName = "car-launcher.log.1"
    }
}
