package org.wpfy.carlauncher.core.performance

enum class BudgetStatus {
    Healthy,
    Advisory,
    Investigate,
    Unavailable,
}

object PerformanceBudgets {
    const val KibibytesPerMebibyte: Int = 1024

    private const val HealthyColdLaunchMs = 2_000L
    private const val AdvisoryColdLaunchMs = 3_000L
    private const val HealthyWarmReturnMs = 300L
    private const val AdvisoryWarmReturnMs = 600L
    private const val IdlePssLimitKb = 180 * KibibytesPerMebibyte
    private const val NavigationPssLimitKb = 450 * KibibytesPerMebibyte
    private const val IdleJankPercent = 5.0
    private const val ActiveJankPercent = 10.0
    private const val JankIntervalMs = 32L
    private const val SevereJankIntervalMs = 100L

    fun classifyVerifiedColdLaunch(durationMs: Long?): BudgetStatus = classifyDuration(
        durationMs = durationMs,
        healthyThroughMs = HealthyColdLaunchMs,
        advisoryThroughMs = AdvisoryColdLaunchMs,
    )

    fun classifyColdLaunch(durationMs: Long?): BudgetStatus =
        classifyVerifiedColdLaunch(durationMs)

    fun classifyWarmReturn(durationMs: Long?): BudgetStatus = classifyDuration(
        durationMs = durationMs,
        healthyThroughMs = HealthyWarmReturnMs,
        advisoryThroughMs = AdvisoryWarmReturnMs,
    )

    fun classifyIdlePss(totalPssKb: Int?): BudgetStatus =
        classifyPss(totalPssKb, IdlePssLimitKb)

    fun classifyNavigationPss(totalPssKb: Int?): BudgetStatus =
        classifyPss(totalPssKb, NavigationPssLimitKb)

    fun classifyIdleJank(jankPercent: Double?): BudgetStatus =
        classifyJank(jankPercent, IdleJankPercent)

    fun classifyActiveJank(jankPercent: Double?): BudgetStatus =
        classifyJank(jankPercent, ActiveJankPercent)

    fun summarizeFrameIntervals(intervals: LongArray): FrameTimingSummary {
        var jankFrames = 0
        var severeJankFrames = 0
        for (intervalMs in intervals) {
            require(intervalMs >= 0L) { "frame intervals must be non-negative" }
            if (intervalMs > JankIntervalMs) {
                jankFrames += 1
            }
            if (intervalMs > SevereJankIntervalMs) {
                severeJankFrames += 1
            }
        }
        return FrameTimingSummary(
            totalFrames = intervals.size,
            jankFrames = jankFrames,
            severeJankFrames = severeJankFrames,
        )
    }

    fun summarizeFrameIntervals(intervals: Iterable<Long>): FrameTimingSummary {
        var totalFrames = 0
        var jankFrames = 0
        var severeJankFrames = 0
        for (intervalMs in intervals) {
            require(intervalMs >= 0L) { "frame intervals must be non-negative" }
            totalFrames += 1
            if (intervalMs > JankIntervalMs) {
                jankFrames += 1
            }
            if (intervalMs > SevereJankIntervalMs) {
                severeJankFrames += 1
            }
        }
        return FrameTimingSummary(
            totalFrames = totalFrames,
            jankFrames = jankFrames,
            severeJankFrames = severeJankFrames,
        )
    }

    private fun classifyDuration(
        durationMs: Long?,
        healthyThroughMs: Long,
        advisoryThroughMs: Long,
    ): BudgetStatus = when {
        durationMs == null || durationMs < 0L -> BudgetStatus.Unavailable
        durationMs <= healthyThroughMs -> BudgetStatus.Healthy
        durationMs <= advisoryThroughMs -> BudgetStatus.Advisory
        else -> BudgetStatus.Investigate
    }

    private fun classifyPss(totalPssKb: Int?, limitKb: Int): BudgetStatus = when {
        totalPssKb == null || totalPssKb < 0 -> BudgetStatus.Unavailable
        totalPssKb <= limitKb -> BudgetStatus.Healthy
        else -> BudgetStatus.Investigate
    }

    private fun classifyJank(jankPercent: Double?, healthyThroughPercent: Double): BudgetStatus = when {
        jankPercent == null || jankPercent.isNaN() || jankPercent.isInfinite() || jankPercent < 0.0 ->
            BudgetStatus.Unavailable
        jankPercent <= healthyThroughPercent -> BudgetStatus.Healthy
        else -> BudgetStatus.Investigate
    }
}
