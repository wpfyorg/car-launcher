package com.openlauncher.app.debug.embedding

import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.MotionEvent
import android.view.View

/** Debug-only ADB gesture/type harness kept separate from provider lifecycle state. */
internal class NavigationDebugInput(
    private val host: View,
    private val activityView: ActivityViewCompat,
) {
    fun gesture(kind: String, xFraction: Float = 0.5f, yFraction: Float = 0.5f): Boolean =
        when (kind) {
            "tap" -> tap(xFraction, yFraction)
            "pan" -> pan()
            "pinch_in" -> pinch(zoomIn = true)
            "pinch_out" -> pinch(zoomIn = false)
            else -> false
        }

    fun type(text: String): Boolean {
        val events = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)
            .getEvents(text.toCharArray())
            ?: return false
        val ok = events.all { event -> activityView.forwardKeyEvent(event).isSuccess }
        Log.i(Tag, "type length=${text.length} ok=$ok")
        return ok
    }

    private fun tap(xFraction: Float, yFraction: Float): Boolean {
        val width = activityView.width.takeIf { it > 0 } ?: return false
        val height = activityView.height.takeIf { it > 0 } ?: return false
        val x = width * xFraction.coerceIn(0f, 1f)
        val y = height * yFraction.coerceIn(0f, 1f)
        val downTime = SystemClock.uptimeMillis()
        val down = event(downTime, MotionEvent.ACTION_DOWN, listOf(PointerPoint(0, x, y)))
        val up = event(downTime, MotionEvent.ACTION_UP, listOf(PointerPoint(0, x, y)))
        val ok = activityView.forwardMotionEvent(down).isSuccess &&
            activityView.forwardMotionEvent(up).isSuccess
        down.recycle()
        up.recycle()
        Log.i(Tag, "tap x=$xFraction y=$yFraction ok=$ok")
        return ok
    }

    private fun pan(): Boolean {
        val width = activityView.width.takeIf { it > 0 } ?: return false
        val height = activityView.height.takeIf { it > 0 } ?: return false
        val downTime = SystemClock.uptimeMillis()
        val points = listOf(0.62f, 0.57f, 0.52f, 0.47f, 0.42f).map { fraction ->
            width * fraction to height * 0.5f
        }
        points.forEachIndexed { index, (x, y) ->
            host.postDelayed(
                {
                    val action = when (index) {
                        0 -> MotionEvent.ACTION_DOWN
                        points.lastIndex -> MotionEvent.ACTION_UP
                        else -> MotionEvent.ACTION_MOVE
                    }
                    val motionEvent = event(downTime, action, listOf(PointerPoint(0, x, y)))
                    val ok = activityView.forwardMotionEvent(motionEvent).isSuccess
                    motionEvent.recycle()
                    if (index == points.lastIndex) Log.i(Tag, "pan ok=$ok")
                },
                index * GestureStepMillis,
            )
        }
        return true
    }

    private fun pinch(zoomIn: Boolean): Boolean {
        val width = activityView.width.takeIf { it > 0 } ?: return false
        val height = activityView.height.takeIf { it > 0 } ?: return false
        val centerX = width * 0.5f
        val centerY = height * 0.5f
        val near = width * 0.10f
        val far = width * 0.28f
        val spans = if (zoomIn) {
            listOf(near, width * 0.16f, width * 0.22f, far)
        } else {
            listOf(far, width * 0.22f, width * 0.16f, near)
        }
        val downTime = SystemClock.uptimeMillis()
        val steps = mutableListOf<Pair<Int, List<PointerPoint>>>()
        val first = spans.first()
        steps += MotionEvent.ACTION_DOWN to listOf(PointerPoint(0, centerX - first, centerY))
        steps += (MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)) to
            listOf(
                PointerPoint(0, centerX - first, centerY),
                PointerPoint(1, centerX + first, centerY),
            )
        spans.drop(1).forEach { span ->
            steps += MotionEvent.ACTION_MOVE to listOf(
                PointerPoint(0, centerX - span, centerY),
                PointerPoint(1, centerX + span, centerY),
            )
        }
        val last = spans.last()
        steps += (MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)) to
            listOf(
                PointerPoint(0, centerX - last, centerY),
                PointerPoint(1, centerX + last, centerY),
            )
        steps += MotionEvent.ACTION_UP to listOf(PointerPoint(0, centerX - last, centerY))

        steps.forEachIndexed { index, (action, points) ->
            host.postDelayed(
                {
                    val motionEvent = event(downTime, action, points)
                    val ok = activityView.forwardMotionEvent(motionEvent).isSuccess
                    motionEvent.recycle()
                    if (index == steps.lastIndex) Log.i(Tag, "pinch zoomIn=$zoomIn ok=$ok")
                },
                index * GestureStepMillis,
            )
        }
        return true
    }

    private fun event(
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

    private data class PointerPoint(val id: Int, val x: Float, val y: Float)

    private companion object {
        const val Tag = "NavigationDebugInput"
        const val GestureStepMillis = 45L
    }
}
