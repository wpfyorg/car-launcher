package com.openlauncher.app.debug.embedding

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Android 9 system-build experiment based on AOSP's hidden android.app.ActivityView API.
 *
 * This stays in the Free+Debug source set because ActivityView is not an SDK API and requires
 * platform/signature privileges for input forwarding and arbitrary third-party embedding.
 */
class ActivityViewPocActivity : Activity() {
    private lateinit var host: FrameLayout
    private lateinit var statusView: TextView
    private lateinit var capabilities: EmbeddingCapabilities
    private var activityView: ActivityViewCompat? = null
    private val activityManagerCompat = runCatching { ActivityManagerCompat() }.getOrNull()
    private var pendingTarget: LaunchTarget? = null
    private var readyPollCount = 0
    private var compactSize = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        statusView = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(12, 8, 12, 8)
        }

        host = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(
                statusView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP or Gravity.END,
                ),
            )
        }

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.rgb(32, 33, 35))
            setPadding(8, 8, 8, 8)
            addView(actionButton("Probe") { launch(LaunchTarget.Probe) })
            addView(actionButton("Organic") { launch(LaunchTarget.OrganicMaps) })
            addView(actionButton("Maps") { launch(LaunchTarget.Maps) })
            addView(actionButton("Back") { performEmbeddedBack() })
            addView(actionButton("Resize") { toggleActivityViewSize() })
            addView(actionButton("Release") { releaseActivityView() })
            addView(actionButton("Recreate") { recreateActivityView() })
            addView(actionButton("Pan") { performPanGesture() })
            addView(actionButton("Long") { performLongPressGesture() })
            addView(actionButton("Pinch") { performPinchGesture() })
        }

        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.BLACK)
                addView(
                    controls,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ),
                )
                addView(
                    host,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1f,
                    ),
                )
            },
        )

        capabilities = EmbeddingCapabilities.detect(this)
        logStatus(capabilities.summary())
        if (capabilities.canHostOwnedActivity) {
            createActivityView()
        }
    }

    override fun onDestroy() {
        releaseActivityView()
        super.onDestroy()
    }

    private fun createActivityView() {
        if (activityView != null) return
        if (!capabilities.canHostOwnedActivity) {
            logStatus("ActivityView blocked\n${capabilities.summary()}")
            return
        }

        val view = ActivityViewCompat(this, capabilities)
        view.attach().fold(
            onSuccess = {
                activityView = view
                host.addView(
                    view,
                    0,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
                readyPollCount = 0
                waitForReady()
                logStatus("ActivityView attached; waiting for virtual display")
            },
            onFailure = { error ->
                logStatus("ActivityView create failed: ${error.javaClass.simpleName}: ${error.message}")
            },
        )
    }

    private fun waitForReady() {
        val view = activityView ?: return
        val displayId = view.virtualDisplayId

        if (displayId != null && displayId >= 0) {
            logStatus("ActivityView ready display=$displayId")
            pendingTarget?.also {
                pendingTarget = null
                launch(it)
            }
            return
        }

        if (readyPollCount++ < MaxReadyPolls) {
            host.postDelayed(::waitForReady, ReadyPollMillis)
        } else {
            logStatus("ActivityView did not become ready")
        }
    }

    private fun launch(target: LaunchTarget) {
        if (!capabilities.canHostOwnedActivity) {
            logStatus("launch blocked\n${capabilities.summary()}")
            return
        }
        if (target != LaunchTarget.Probe && !capabilities.canHostExternalActivity) {
            logStatus("${target.label} blocked: INTERNAL_SYSTEM_WINDOW not granted")
            return
        }
        if (activityView == null) createActivityView()

        val view = activityView ?: return
        val displayId = view.virtualDisplayId
        if (displayId == null || displayId < 0) {
            pendingTarget = target
            logStatus("queued ${target.label}; ActivityView not ready")
            return
        }

        val intent = targetIntent(target) ?: run {
            logStatus("No launcher intent for ${target.label}")
            return
        }
        view.startActivity(intent, external = target != LaunchTarget.Probe).fold(
            onSuccess = {
                logStatus("started ${target.label} in ActivityView display=$displayId")
                if (target == LaunchTarget.OrganicMaps) {
                    val packageName = intent.component?.packageName ?: intent.`package`
                    if (packageName != null) {
                        scheduleOrganicMapsAttach(view, displayId, packageName)
                    }
                }
            },
            onFailure = { error ->
                logStatus("start ${target.label} failed: ${error.javaClass.simpleName}: ${error.message}")
            },
        )
    }

    private fun scheduleOrganicMapsAttach(
        view: ActivityViewCompat,
        displayId: Int,
        packageName: String,
        attempt: Int = 0,
        stableStackId: Int? = null,
        stableDisplayId: Int? = null,
        stablePolls: Int = 0,
    ) {
        host.postDelayed(
            {
                if (activityView !== view || view.virtualDisplayId != displayId) return@postDelayed
                val manager = activityManagerCompat ?: run {
                    logStatus("Organic Maps attach blocked: ActivityManager bridge unavailable")
                    return@postDelayed
                }
                manager.findTopActivityStack(
                    packageName,
                    OrganicMapsMainActivity,
                ).fold(
                    onSuccess = { location ->
                        when {
                            location == null && attempt < OrganicMapsAttachMaxAttempts ->
                                scheduleOrganicMapsAttach(
                                    view,
                                    displayId,
                                    packageName,
                                    attempt + 1,
                                )
                            location == null -> logStatus("Organic Maps attach timed out")
                            location.displayId == displayId &&
                                location.stackId == stableStackId &&
                                location.displayId == stableDisplayId &&
                                stablePolls >= OrganicMapsTargetStablePolls ->
                                logStatus("Organic Maps confirmed stable in ActivityView display=$displayId")
                            location.displayId == displayId ->
                                scheduleOrganicMapsAttach(
                                    view,
                                    displayId,
                                    packageName,
                                    attempt + 1,
                                    stableStackId = location.stackId,
                                    stableDisplayId = location.displayId,
                                    stablePolls = if (
                                        location.stackId == stableStackId &&
                                        location.displayId == stableDisplayId
                                    ) {
                                        stablePolls + 1
                                    } else {
                                        1
                                    },
                                )
                            capabilities.startTasksFromRecents &&
                                (location.stackId != stableStackId ||
                                    location.displayId != stableDisplayId ||
                                    stablePolls < OrganicMapsRecentsStablePolls) ->
                                scheduleOrganicMapsAttach(
                                    view,
                                    displayId,
                                    packageName,
                                    attempt + 1,
                                    stableStackId = location.stackId,
                                    stableDisplayId = location.displayId,
                                    stablePolls = if (
                                        location.stackId == stableStackId &&
                                        location.displayId == stableDisplayId
                                    ) {
                                        stablePolls + 1
                                    } else {
                                        1
                                    },
                                )
                            capabilities.startTasksFromRecents ->
                                manager.startTaskFromRecents(location.taskId, displayId).fold(
                                    onSuccess = { result ->
                                        logStatus(
                                            "requested Organic Maps task attach=${location.taskId} " +
                                                "display=${location.displayId}->$displayId result=$result",
                                        )
                                        scheduleOrganicMapsAttach(
                                            view,
                                            displayId,
                                            packageName,
                                            attempt + 1,
                                        )
                                    },
                                    onFailure = { error ->
                                        logStatus(
                                            "Organic Maps task attach failed: " +
                                                "${error.javaClass.simpleName}: ${error.message}",
                                        )
                                    },
                                )
                            !capabilities.manageActivityStacks ->
                                logStatus(
                                    "Organic Maps attach blocked: START_TASKS_FROM_RECENTS and " +
                                        "MANAGE_ACTIVITY_STACKS not granted",
                                )
                            location.stackId != stableStackId ->
                                scheduleOrganicMapsAttach(
                                    view,
                                    displayId,
                                    packageName,
                                    attempt + 1,
                                    stableStackId = location.stackId,
                                    stableDisplayId = location.displayId,
                                    stablePolls = 1,
                                )
                            stablePolls < OrganicMapsRehomeStablePolls ->
                                scheduleOrganicMapsAttach(
                                    view,
                                    displayId,
                                    packageName,
                                    attempt + 1,
                                    stableStackId = location.stackId,
                                    stableDisplayId = location.displayId,
                                    stablePolls = stablePolls + 1,
                                )
                            else -> manager.moveStackToDisplay(location.stackId, displayId).fold(
                                onSuccess = {
                                    logStatus(
                                        "rehomed Organic Maps stack=${location.stackId} " +
                                            "display=${location.displayId}->$displayId",
                                    )
                                },
                                onFailure = { error ->
                                    logStatus(
                                        "Organic Maps rehome failed: " +
                                            "${error.javaClass.simpleName}: ${error.message}",
                                    )
                                },
                            )
                        }
                    },
                    onFailure = { error ->
                        logStatus(
                            "Organic Maps attach failed: ${error.javaClass.simpleName}: ${error.message}",
                        )
                    },
                )
            },
            OrganicMapsAttachPollMillis,
        )
    }

    private fun targetIntent(target: LaunchTarget): Intent? = when (target) {
        LaunchTarget.Probe -> Intent(this, EmbeddedProbeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }
        LaunchTarget.OrganicMaps -> OrganicMapsPackages.firstNotNullOfOrNull { packageName ->
            val launcherIntent = packageManager.getLaunchIntentForPackage(packageName)
                ?: return@firstNotNullOfOrNull null
            if (!capabilities.startAnyActivity) {
                launcherIntent
            } else {
                Intent(Intent.ACTION_MAIN).apply {
                    component = ComponentName(packageName, OrganicMapsMainActivity)
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION,
                    )
                }
            }
        }
        LaunchTarget.Maps -> packageManager.getLaunchIntentForPackage(GoogleMapsPackage)
    }

    private fun performEmbeddedBack() {
        val view = activityView ?: return
        view.performBackPress().fold(
            onSuccess = { logStatus("embedded back sent") },
            onFailure = { error ->
                logStatus("embedded back failed: ${error.javaClass.simpleName}: ${error.message}")
            },
        )
    }

    private fun releaseActivityView() {
        val view = activityView ?: return
        view.release()
        host.removeView(view)
        activityView = null
        pendingTarget = null
        logStatus("ActivityView released")
    }

    private fun recreateActivityView() {
        releaseActivityView()
        createActivityView()
    }

    private fun toggleActivityViewSize() {
        val view = activityView ?: return
        val width = host.width
        val height = host.height
        if (width == 0 || height == 0) {
            logStatus("resize blocked: host has not been measured")
            return
        }

        compactSize = !compactSize
        val params = FrameLayout.LayoutParams(
            if (compactSize) (width * CompactScale).toInt() else ViewGroup.LayoutParams.MATCH_PARENT,
            if (compactSize) (height * CompactScale).toInt() else ViewGroup.LayoutParams.MATCH_PARENT,
        ).apply {
            gravity = if (compactSize) Gravity.CENTER else Gravity.NO_GRAVITY
        }
        view.layoutParams = params
        view.requestLayout()
        val label = if (compactSize) "compact" else "full"
        logStatus("ActivityView resize requested: $label display=${view.virtualDisplayId}")
    }

    private fun performPanGesture() {
        val view = gestureView() ?: return
        val downTime = SystemClock.uptimeMillis()
        val points = listOf(
            900f to 300f,
            980f to 300f,
            1080f to 300f,
            1180f to 300f,
            1280f to 300f,
        )
        points.forEachIndexed { index, (x, y) ->
            host.postDelayed(
                {
                    val action = when (index) {
                        0 -> MotionEvent.ACTION_DOWN
                        points.lastIndex -> MotionEvent.ACTION_UP
                        else -> MotionEvent.ACTION_MOVE
                    }
                    val event = singlePointerEvent(downTime, action, x, y)
                    val success = forwardMotion(view, event, "pan")
                    if (index == points.lastIndex && success) {
                        logStatus("pan gesture sent display=${view.virtualDisplayId}")
                    }
                },
                index * GestureStepMillis,
            )
        }
    }

    private fun performLongPressGesture() {
        val view = gestureView() ?: return
        val downTime = SystemClock.uptimeMillis()
        val x = 1200f
        val y = 300f
        forwardMotion(view, singlePointerEvent(downTime, MotionEvent.ACTION_DOWN, x, y), "long press")
        host.postDelayed(
            {
                val success = forwardMotion(
                    view,
                    singlePointerEvent(downTime, MotionEvent.ACTION_UP, x, y),
                    "long press",
                )
                if (success) logStatus("long-press gesture sent display=${view.virtualDisplayId}")
            },
            LongPressMillis,
        )
    }

    private fun performPinchGesture() {
        val view = gestureView() ?: return
        val downTime = SystemClock.uptimeMillis()
        val steps = listOf(
            GestureStep(
                MotionEvent.ACTION_DOWN,
                listOf(PointerPoint(0, 700f, 300f)),
            ),
            GestureStep(
                MotionEvent.ACTION_POINTER_DOWN or
                    (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                listOf(PointerPoint(0, 700f, 300f), PointerPoint(1, 900f, 300f)),
            ),
            GestureStep(
                MotionEvent.ACTION_MOVE,
                listOf(PointerPoint(0, 620f, 300f), PointerPoint(1, 980f, 300f)),
            ),
            GestureStep(
                MotionEvent.ACTION_MOVE,
                listOf(PointerPoint(0, 520f, 300f), PointerPoint(1, 1080f, 300f)),
            ),
            GestureStep(
                MotionEvent.ACTION_MOVE,
                listOf(PointerPoint(0, 420f, 300f), PointerPoint(1, 1180f, 300f)),
            ),
            GestureStep(
                MotionEvent.ACTION_POINTER_UP or
                    (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                listOf(PointerPoint(0, 420f, 300f), PointerPoint(1, 1180f, 300f)),
            ),
            GestureStep(
                MotionEvent.ACTION_UP,
                listOf(PointerPoint(0, 420f, 300f)),
            ),
        )
        steps.forEachIndexed { index, step ->
            host.postDelayed(
                {
                    val event = multiPointerEvent(downTime, step.action, step.points)
                    val success = forwardMotion(view, event, "pinch")
                    if (index == steps.lastIndex && success) {
                        logStatus("pinch gesture sent display=${view.virtualDisplayId}")
                    }
                },
                index * GestureStepMillis,
            )
        }
    }

    private fun gestureView(): ActivityViewCompat? {
        val view = activityView
        if (view == null || view.virtualDisplayId == null) {
            logStatus("gesture blocked: ActivityView not ready")
            return null
        }
        return view
    }

    private fun singlePointerEvent(
        downTime: Long,
        action: Int,
        x: Float,
        y: Float,
    ): MotionEvent = multiPointerEvent(
        downTime,
        action,
        listOf(PointerPoint(0, x, y)),
    )

    private fun multiPointerEvent(
        downTime: Long,
        action: Int,
        points: List<PointerPoint>,
    ): MotionEvent {
        val properties = Array(points.size) { index ->
            MotionEvent.PointerProperties().apply {
                id = points[index].id
                toolType = MotionEvent.TOOL_TYPE_FINGER
            }
        }
        val coordinates = Array(points.size) { index ->
            MotionEvent.PointerCoords().apply {
                x = points[index].x
                y = points[index].y
                pressure = 1f
                size = 1f
            }
        }
        return MotionEvent.obtain(
            downTime,
            SystemClock.uptimeMillis(),
            action,
            points.size,
            properties,
            coordinates,
            0,
            0,
            1f,
            1f,
            0,
            0,
            InputDevice.SOURCE_TOUCHSCREEN,
            0,
        )
    }

    private fun forwardMotion(
        view: ActivityViewCompat,
        event: MotionEvent,
        label: String,
    ): Boolean {
        val result = view.forwardMotionEvent(event)
        event.recycle()
        return result.fold(
            onSuccess = { true },
            onFailure = { error ->
                logStatus("$label failed: ${error.javaClass.simpleName}: ${error.message}")
                false
            },
        )
    }

    private fun actionButton(label: String, onClick: () -> Unit): View =
        Button(this).apply {
            text = label
            setOnClickListener { onClick() }
        }

    private fun logStatus(message: String) {
        Log.i(Tag, message.replace('\n', ' '))
        statusView.text = message
    }

    private enum class LaunchTarget(val label: String) {
        Probe("probe"),
        OrganicMaps("Organic Maps"),
        Maps("maps"),
    }

    private data class GestureStep(
        val action: Int,
        val points: List<PointerPoint>,
    )

    private data class PointerPoint(
        val id: Int,
        val x: Float,
        val y: Float,
    )

    companion object {
        private const val Tag = "OpenLauncherActivityView"
        private const val GoogleMapsPackage = "com.google.android.apps.maps"
        private const val OrganicMapsMainActivity = "app.organicmaps.MwmActivity"
        private val OrganicMapsPackages = listOf("app.organicmaps", "app.organicmaps.web")
        private const val CompactScale = 0.67f
        private const val ReadyPollMillis = 100L
        private const val MaxReadyPolls = 50
        private const val GestureStepMillis = 90L
        private const val LongPressMillis = 900L
        private const val OrganicMapsAttachPollMillis = 750L
        private const val OrganicMapsRecentsStablePolls = 4
        private const val OrganicMapsTargetStablePolls = 4
        private const val OrganicMapsRehomeStablePolls = 4
        private const val OrganicMapsAttachMaxAttempts = 24
    }
}
