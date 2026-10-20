package org.wpfy.carlauncher.feature.home

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs

/**
 * Swipe toward the media page from the end-edge strip of the map page. It watches the Initial
 * pass without consuming the touch-down, so taps and the map's own gestures anywhere (including on
 * the strip) are untouched; only a leftward drag that starts in the strip is taken over.
 */
internal fun Modifier.edgePageSwipe(
    enabled: Boolean,
    edge: Dp,
    onDrag: (deltaPx: Float, widthPx: Float) -> Unit,
    onStop: (velocityPxPerSecond: Float) -> Unit,
): Modifier = if (!enabled) {
    this
} else {
    pointerInput(edge) {
        val edgePx = edge.toPx()
        val slop = viewConfiguration.touchSlop
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val width = size.width.toFloat()
            if (!swipeStartsPageChange(down.position.x, width, edgePx)) return@awaitEachGesture
            val tracker = VelocityTracker()
            tracker.addPosition(down.uptimeMillis, down.position)
            var total = 0f
            var dragging = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                tracker.addPosition(change.uptimeMillis, change.position)
                if (!change.pressed) {
                    if (dragging) {
                        change.consume()
                        onStop(tracker.calculateVelocity().x)
                    }
                    break
                }
                val dx = change.positionChange().x
                total += dx
                if (!dragging) {
                    if (abs(total) > slop) {
                        // Toward the media page only; a drag back toward the map is not ours.
                        if (total > 0f) break
                        dragging = true
                        onDrag(total, width)
                        change.consume()
                    }
                } else {
                    onDrag(dx, width)
                    change.consume()
                }
            }
        }
    }
}

/**
 * Lets a horizontal swipe on the media page (anywhere) bring the map back. The page's own
 * horizontal pager (several media sources) scrolls first; only the part of a swipe it cannot use
 * moves the page, so source switching keeps working.
 */
internal class MediaPageNestedScroll(
    private val state: MediaPageState,
    private val widthPx: () -> Float,
    private val swipeEnabled: () -> Boolean,
) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        val progress = state.progress
        // Mid-swipe the page keeps following the finger in both directions.
        if (progress <= 0f || progress >= 1f || available.x == 0f) return Offset.Zero
        return move(available.x)
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || available.x <= 0f) return Offset.Zero
        return move(available.x)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val progress = state.progress
        if (progress <= 0f || progress >= 1f) return Velocity.Zero
        settle(available.x)
        return available
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val progress = state.progress
        if (progress <= 0f || progress >= 1f) return Velocity.Zero
        settle(available.x)
        return available
    }

    private fun move(deltaPx: Float): Offset {
        val width = widthPx()
        val before = state.progress
        val after = pageProgressAfterDrag(before, deltaPx, width, swipeEnabled())
        state.dragTo(after)
        return Offset((before - after) * width, 0f)
    }

    private fun settle(velocityX: Float) {
        state.animateTo(pageSettleTarget(state.progress, velocityX, swipeEnabled()))
    }
}
