package org.wpfy.carlauncher.core.performance

import android.content.Context
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.view.Choreographer
import java.util.ArrayDeque

internal interface PerformanceClock {
    fun elapsedRealtime(): Long

    fun processUptimeMs(): Long

    fun processCpuTimeMs(): Long

    fun processStartElapsedRealtime(): Long
}

private object AndroidPerformanceClock : PerformanceClock {
    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()

    override fun processUptimeMs(): Long = elapsedProcessUptimeMs(
        elapsedRealtimeMs = SystemClock.elapsedRealtime(),
        processStartElapsedMs = Process.getStartElapsedRealtime(),
    )

    override fun processCpuTimeMs(): Long = Process.getElapsedCpuTime()

    override fun processStartElapsedRealtime(): Long = Process.getStartElapsedRealtime()
}

internal fun elapsedProcessUptimeMs(
    elapsedRealtimeMs: Long,
    processStartElapsedMs: Long,
): Long = (elapsedRealtimeMs - processStartElapsedMs).coerceAtLeast(0L)

internal interface ProcessMemoryReader {
    fun totalPssKb(): Int

    fun nativeHeapAllocatedBytes(): Long
}

private object AndroidProcessMemoryReader : ProcessMemoryReader {
    override fun totalPssKb(): Int {
        val memoryInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memoryInfo)
        return memoryInfo.totalPss
    }

    override fun nativeHeapAllocatedBytes(): Long = Debug.getNativeHeapAllocatedSize()
}

internal interface FrameCallbackScheduler {
    fun postFrameCallback(callback: Choreographer.FrameCallback)

    fun removeFrameCallback(callback: Choreographer.FrameCallback)
}

internal interface SamplingTimeoutScheduler {
    fun postDelayed(runnable: Runnable, delayMs: Long)

    fun removeCallbacks(runnable: Runnable)
}

private class AndroidSamplingTimeoutScheduler(
    private val handler: Handler = Handler(Looper.getMainLooper()),
) : SamplingTimeoutScheduler {
    override fun postDelayed(runnable: Runnable, delayMs: Long) {
        handler.postDelayed(runnable, delayMs)
    }

    override fun removeCallbacks(runnable: Runnable) {
        handler.removeCallbacks(runnable)
    }
}

private object NoOpSamplingTimeoutScheduler : SamplingTimeoutScheduler {
    override fun postDelayed(runnable: Runnable, delayMs: Long) = Unit

    override fun removeCallbacks(runnable: Runnable) = Unit
}

private class AndroidFrameCallbackScheduler(
    private val choreographer: Choreographer,
) : FrameCallbackScheduler {
    override fun postFrameCallback(callback: Choreographer.FrameCallback) {
        choreographer.postFrameCallback(callback)
    }

    override fun removeFrameCallback(callback: Choreographer.FrameCallback) {
        choreographer.removeFrameCallback(callback)
    }
}

class PerformanceMonitor internal constructor(
    @Suppress("UNUSED_PARAMETER")
    private val applicationContext: Context? = null,
    private val clock: PerformanceClock = AndroidPerformanceClock,
    private val memoryReader: ProcessMemoryReader = AndroidProcessMemoryReader,
    private val frameSchedulerProvider: () -> FrameCallbackScheduler = {
        AndroidFrameCallbackScheduler(Choreographer.getInstance())
    },
    private val timeoutScheduler: SamplingTimeoutScheduler = if (applicationContext == null) {
        NoOpSamplingTimeoutScheduler
    } else {
        AndroidSamplingTimeoutScheduler()
    },
    private val isMainThread: () -> Boolean = {
        Looper.myLooper() == Looper.getMainLooper()
    },
    private val maxSnapshotHistory: Int = DefaultMaxSnapshotHistory,
) {
    init {
        require(maxSnapshotHistory > 0) { "maxSnapshotHistory must be positive" }
    }

    private val processStartElapsedMs = clock.processStartElapsedRealtime()
    private val snapshotHistory = ArrayDeque<PerformanceSnapshot>(maxSnapshotHistory)
    private val historyLock = Any()
    private val frameStateLock = Any()

    private var frameScheduler: FrameCallbackScheduler? = null
    private var isResumed = false
    @Volatile
    private var isSampling = false
    private var samplingReason: String? = null
    private var samplingStartedElapsedMs = 0L
    private var lastFrameTimeNanos = NoFrameTimestamp
    private var activeTotalFrames = 0
    private var activeJankFrames = 0
    private var activeSevereJankFrames = 0
    private var completedFrameTiming = EmptyFrameTiming

    @Volatile
    private var activityCreateToFirstDrawMs: Long? = null
    @Volatile
    private var processStartToFirstDrawMs: Long? = null
    @Volatile
    private var verifiedColdLaunchToFirstDrawMs: Long? = null
    @Volatile
    private var lastWarmReturnToFocusMs: Long? = null

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            onFrame(frameTimeNanos)
        }
    }
    private val samplingTimeout = Runnable {
        if (isSampling) stopFrameSamplingInternal()
    }

    fun snapshot(includePss: Boolean = false): PerformanceSnapshot {
        val capturedAtElapsedMs = clock.elapsedRealtime()
        val runtime = Runtime.getRuntime()
        val snapshot = PerformanceSnapshot(
            capturedAtElapsedMs = capturedAtElapsedMs,
            processUptimeMs = clock.processUptimeMs(),
            processCpuTimeMs = clock.processCpuTimeMs(),
            javaHeapUsedBytes = (runtime.totalMemory() - runtime.freeMemory()).coerceAtLeast(0L),
            javaHeapMaxBytes = runtime.maxMemory(),
            nativeHeapAllocatedBytes = memoryReader.nativeHeapAllocatedBytes(),
            totalPssKb = if (includePss) memoryReader.totalPssKb() else null,
            activityCreateToFirstDrawMs = activityCreateToFirstDrawMs,
            processStartToFirstDrawMs = processStartToFirstDrawMs,
            verifiedColdLaunchToFirstDrawMs = verifiedColdLaunchToFirstDrawMs,
            lastWarmReturnToFocusMs = lastWarmReturnToFocusMs,
            frameTimingSummary = currentFrameTiming(),
        )
        synchronized(historyLock) {
            if (snapshotHistory.size == maxSnapshotHistory) {
                snapshotHistory.removeFirst()
            }
            snapshotHistory.addLast(snapshot)
        }
        return snapshot
    }

    fun recentSnapshots(): List<PerformanceSnapshot> = synchronized(historyLock) {
        snapshotHistory.toList()
    }

    fun startFrameSampling(reason: String) {
        checkMainThread()
        check(isResumed) { "frame sampling requires a resumed activity" }
        startFrameSamplingInternal(reason)
    }

    fun stopFrameSampling() {
        checkMainThread()
        stopFrameSamplingInternal()
    }

    fun onResume() {
        checkMainThread()
        if (isResumed) return
        isResumed = true
    }

    fun onPause() {
        checkMainThread()
        isResumed = false
        stopFrameSamplingInternal()
        clearFrameTiming()
    }

    fun beginActivityTiming(
        activityCreateElapsedMs: Long = clock.elapsedRealtime(),
        verifiedColdLaunchProven: Boolean = false,
    ): PerformanceTimingState = PerformanceTimingState(
        activityCreateElapsedMs = activityCreateElapsedMs,
        processStartElapsedMs = processStartElapsedMs,
        verifiedColdLaunchProven = verifiedColdLaunchProven,
    )

    fun recordFirstDraw(measurement: PerformanceTimingState.FirstDrawMeasurement) {
        activityCreateToFirstDrawMs = measurement.activityCreateToFirstDrawMs
        if (processStartToFirstDrawMs == null) {
            processStartToFirstDrawMs = measurement.processStartToFirstDrawMs
        }
        if (verifiedColdLaunchToFirstDrawMs == null && measurement.verifiedColdLaunchToFirstDrawMs != null) {
            verifiedColdLaunchToFirstDrawMs = measurement.verifiedColdLaunchToFirstDrawMs
        }
    }

    fun recordWarmReturnToFocus(durationMs: Long?) {
        lastWarmReturnToFocusMs = durationMs
    }

    fun recordWarmReturn(durationMs: Long?) {
        recordWarmReturnToFocus(durationMs)
    }

    private fun startFrameSamplingInternal(reason: String) {
        frameScheduler?.let { scheduler ->
            if (isSampling) {
                scheduler.removeFrameCallback(frameCallback)
            }
        }
        timeoutScheduler.removeCallbacks(samplingTimeout)
        val scheduler = frameScheduler ?: frameSchedulerProvider().also { frameScheduler = it }
        synchronized(frameStateLock) {
            samplingReason = reason
            samplingStartedElapsedMs = clock.elapsedRealtime()
            completedFrameTiming = EmptyFrameTiming
            resetActiveFrameTimingLocked()
            isSampling = true
        }
        scheduler.postFrameCallback(frameCallback)
        timeoutScheduler.postDelayed(samplingTimeout, MaxSamplingWindowMs)
    }

    private fun stopFrameSamplingInternal() {
        if (!isSampling) return
        frameScheduler?.removeFrameCallback(frameCallback)
        timeoutScheduler.removeCallbacks(samplingTimeout)
        synchronized(frameStateLock) {
            completedFrameTiming = activeFrameTimingLocked()
            isSampling = false
            samplingReason = null
            resetActiveFrameTimingLocked()
        }
    }

    private fun onFrame(frameTimeNanos: Long) {
        if (!isSampling) return

        val windowElapsedMs = clock.elapsedRealtime() - samplingStartedElapsedMs
        if (windowElapsedMs >= MaxSamplingWindowMs) {
            stopFrameSamplingInternal()
            return
        }

        val reachedIntervalBound = synchronized(frameStateLock) {
            if (!isSampling) return
            if (lastFrameTimeNanos != NoFrameTimestamp) {
                val intervalNanos = (frameTimeNanos - lastFrameTimeNanos).coerceAtLeast(0L)
                activeTotalFrames += 1
                if (intervalNanos > JankIntervalNanos) {
                    activeJankFrames += 1
                }
                if (intervalNanos > SevereJankIntervalNanos) {
                    activeSevereJankFrames += 1
                }
            }
            lastFrameTimeNanos = frameTimeNanos
            activeTotalFrames >= MaxFrameIntervalsPerWindow
        }

        if (reachedIntervalBound) {
            stopFrameSamplingInternal()
        } else {
            frameScheduler?.postFrameCallback(frameCallback)
        }
    }

    private fun currentFrameTiming(): FrameTimingSummary = synchronized(frameStateLock) {
        if (isSampling) activeFrameTimingLocked() else completedFrameTiming
    }

    private fun activeFrameTimingLocked(): FrameTimingSummary = FrameTimingSummary(
        totalFrames = activeTotalFrames,
        jankFrames = activeJankFrames,
        severeJankFrames = activeSevereJankFrames,
    )

    private fun clearFrameTiming() = synchronized(frameStateLock) {
        completedFrameTiming = EmptyFrameTiming
        resetActiveFrameTimingLocked()
    }

    private fun resetActiveFrameTimingLocked() {
        lastFrameTimeNanos = NoFrameTimestamp
        activeTotalFrames = 0
        activeJankFrames = 0
        activeSevereJankFrames = 0
    }

    private fun checkMainThread() {
        check(isMainThread()) { "performance sampling must run on the main thread" }
    }

    companion object {
        const val MaxSamplingWindowMs: Long = 10_000L
        const val MaxFrameIntervalsPerWindow: Int = 600

        private const val DefaultMaxSnapshotHistory = 8
        private const val NanosPerMillisecond = 1_000_000L
        private const val JankIntervalNanos = 32L * NanosPerMillisecond
        private const val SevereJankIntervalNanos = 100L * NanosPerMillisecond
        private const val NoFrameTimestamp = Long.MIN_VALUE
        private val EmptyFrameTiming = FrameTimingSummary(0, 0, 0)

        @Volatile
        private var processInstance: PerformanceMonitor? = null

        @JvmStatic
        fun get(context: Context): PerformanceMonitor {
            val applicationContext = context.applicationContext
            return processInstance ?: synchronized(this) {
                processInstance ?: PerformanceMonitor(applicationContext).also { processInstance = it }
            }
        }
    }
}

class PerformanceTimingState internal constructor(
    val activityCreateElapsedMs: Long,
    private val processStartElapsedMs: Long,
    private val verifiedColdLaunchProven: Boolean = false,
) {
    data class FirstDrawMeasurement(
        val activityCreateToFirstDrawMs: Long,
        val processStartToFirstDrawMs: Long,
        val verifiedColdLaunchToFirstDrawMs: Long?,
    )

    private var destroyed = false
    private var hasStopped = false
    private var firstDrawRecorded = false
    private var firstDrawListenerRegistered = false
    private var firstDrawRemovalPending = false
    private var firstDrawRemovalPosted = false
    private var warmForegroundStartElapsedMs: Long? = null

    val hasPendingFirstDrawWork: Boolean
        get() = firstDrawListenerRegistered || firstDrawRemovalPending || firstDrawRemovalPosted

    val hasPendingWarmReturn: Boolean
        get() = warmForegroundStartElapsedMs != null

    fun registerFirstDrawListener(): Boolean {
        if (destroyed || firstDrawRecorded || hasPendingFirstDrawWork) return false
        firstDrawListenerRegistered = true
        return true
    }

    fun markFirstDraw(firstDrawElapsedMs: Long): FirstDrawMeasurement? {
        if (destroyed || firstDrawRecorded || !firstDrawListenerRegistered) return null

        firstDrawRecorded = true
        firstDrawListenerRegistered = false
        firstDrawRemovalPending = true
        val activityCreateToFirstDrawMs = elapsedSince(activityCreateElapsedMs, firstDrawElapsedMs)
        val processStartToFirstDrawMs = elapsedSince(processStartElapsedMs, firstDrawElapsedMs)
        return FirstDrawMeasurement(
            activityCreateToFirstDrawMs = activityCreateToFirstDrawMs,
            processStartToFirstDrawMs = processStartToFirstDrawMs,
            verifiedColdLaunchToFirstDrawMs = if (verifiedColdLaunchProven) {
                processStartToFirstDrawMs
            } else {
                null
            },
        )
    }

    fun markFirstDrawRemovalPosted(): Boolean {
        if (destroyed || !firstDrawRemovalPending || firstDrawRemovalPosted) return false
        firstDrawRemovalPosted = true
        return true
    }

    fun completeFirstDrawListenerRemoval(viewTreeObserverAlive: Boolean): Boolean {
        if (destroyed || !firstDrawRemovalPosted) return false
        firstDrawRemovalPosted = false
        firstDrawRemovalPending = false
        return viewTreeObserverAlive
    }

    fun onStop() {
        if (destroyed) return
        hasStopped = true
        warmForegroundStartElapsedMs = null
    }

    fun onPause() {
        if (destroyed) return
        warmForegroundStartElapsedMs = null
    }

    fun onStart(elapsedMs: Long): Boolean {
        if (destroyed || !hasStopped) return false
        hasStopped = false
        warmForegroundStartElapsedMs = elapsedMs
        return true
    }

    fun consumeWarmReturnOnFocus(focusElapsedMs: Long): Long? {
        if (destroyed) return null
        val startElapsedMs = warmForegroundStartElapsedMs ?: return null
        warmForegroundStartElapsedMs = null
        return if (focusElapsedMs >= startElapsedMs) {
            focusElapsedMs - startElapsedMs
        } else {
            null
        }
    }

    fun onWindowFocusChanged(hasFocus: Boolean, focusElapsedMs: Long): Long? =
        if (hasFocus) consumeWarmReturnOnFocus(focusElapsedMs) else null

    fun onDestroy() {
        destroyed = true
        hasStopped = true
        warmForegroundStartElapsedMs = null
        cancelPendingFirstDrawWork()
    }

    fun cancelPendingFirstDrawWork() {
        firstDrawListenerRegistered = false
        firstDrawRemovalPending = false
        firstDrawRemovalPosted = false
    }

    private fun elapsedSince(startElapsedMs: Long, endElapsedMs: Long): Long =
        (endElapsedMs - startElapsedMs).coerceAtLeast(0L)
}
