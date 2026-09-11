package org.wpfy.carlauncher.core.diagnostics

import java.util.Locale
import org.wpfy.carlauncher.core.performance.PerformanceBudgets
import org.wpfy.carlauncher.core.performance.PerformanceSnapshot

internal fun formatPerformanceDiagnostics(snapshot: PerformanceSnapshot): String = buildString {
    appendLine("Performance uptime: ${snapshot.processUptimeMs} ms · CPU: ${snapshot.processCpuTimeMs} ms")
    appendLine(
        "Java heap: ${snapshot.javaHeapUsedBytes.toMiB()} / ${snapshot.javaHeapMaxBytes.toMiB()} MiB · " +
            "native heap: ${snapshot.nativeHeapAllocatedBytes.toMiB()} MiB",
    )
    appendLine(
        snapshot.totalPssKb?.let { totalPssKb ->
            "PSS: ${totalPssKb.toPssMiB()} MiB " +
                "(idle ${PerformanceBudgets.classifyIdlePss(totalPssKb)}; " +
                "navigation ${PerformanceBudgets.classifyNavigationPss(totalPssKb)})"
        } ?: "PSS: Unavailable",
    )
    appendLine("Activity first draw: ${snapshot.activityCreateToFirstDrawMs.durationText()}")
    appendLine("Process first draw: ${snapshot.processStartToFirstDrawMs.durationText()}")
    appendLine(
        snapshot.verifiedColdLaunchToFirstDrawMs?.let { durationMs ->
            "Verified cold first draw: $durationMs ms " +
                "(${PerformanceBudgets.classifyVerifiedColdLaunch(durationMs)})"
        } ?: "Verified cold first draw: Unavailable",
    )
    appendLine(
        snapshot.lastWarmReturnToFocusMs?.let { durationMs ->
            "Warm HOME return: $durationMs ms (${PerformanceBudgets.classifyWarmReturn(durationMs)})"
        } ?: "Warm HOME return: Unavailable",
    )

    val frames = snapshot.frameTimingSummary
    if (frames.totalFrames == 0) {
        append("Frame cadence: Unavailable")
    } else {
        val jankPercent = frames.jankPercent
        append(
            "Frame cadence: ${jankPercent.oneDecimal()}% jank, ${frames.severeJankFrames} severe / " +
                "${frames.totalFrames} intervals " +
                "(idle ${PerformanceBudgets.classifyIdleJank(jankPercent)}; " +
                "active ${PerformanceBudgets.classifyActiveJank(jankPercent)})",
        )
    }
}

private fun Long?.durationText(): String = this?.let { "$it ms" } ?: "Unavailable"

private fun Long.toMiB(): String = (this.toDouble() / BytesPerMiB).oneDecimal()

private fun Int.toPssMiB(): String = (this.toDouble() / PerformanceBudgets.KibibytesPerMebibyte).oneDecimal()

private fun Double.oneDecimal(): String = String.format(Locale.US, "%.1f", this)

private const val BytesPerMiB = 1024.0 * 1024.0
