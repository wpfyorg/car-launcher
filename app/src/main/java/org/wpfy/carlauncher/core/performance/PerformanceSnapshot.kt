package org.wpfy.carlauncher.core.performance

data class FrameTimingSummary(
    val totalFrames: Int,
    val jankFrames: Int,
    val severeJankFrames: Int,
) {
    init {
        require(totalFrames >= 0) { "totalFrames must be non-negative" }
        require(jankFrames >= 0) { "jankFrames must be non-negative" }
        require(severeJankFrames >= 0) { "severeJankFrames must be non-negative" }
        require(jankFrames <= totalFrames) { "jankFrames cannot exceed totalFrames" }
        require(severeJankFrames <= jankFrames) {
            "severeJankFrames cannot exceed jankFrames"
        }
    }

    val jankPercent: Double
        get() = percentage(jankFrames)

    val severeJankPercent: Double
        get() = percentage(severeJankFrames)

    private fun percentage(frames: Int): Double =
        if (totalFrames == 0) 0.0 else frames * 100.0 / totalFrames
}

data class PerformanceSnapshot(
    val capturedAtElapsedMs: Long,
    val processUptimeMs: Long,
    val processCpuTimeMs: Long,
    val javaHeapUsedBytes: Long,
    val javaHeapMaxBytes: Long,
    val nativeHeapAllocatedBytes: Long,
    val totalPssKb: Int?,
    val activityCreateToFirstDrawMs: Long?,
    val processStartToFirstDrawMs: Long?,
    val verifiedColdLaunchToFirstDrawMs: Long?,
    val lastWarmReturnToFocusMs: Long?,
    val frameTimingSummary: FrameTimingSummary,
) {
    val frameTiming: FrameTimingSummary
        get() = frameTimingSummary
}
