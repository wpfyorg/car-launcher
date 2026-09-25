package org.wpfy.carlauncher.design.screens

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/** The seek fraction (0 to 1) of a touch at [x] on a bar [width] wide; the bar fills from the right in [rtl]. A bar with no width reads 0. */
internal fun seekFraction(x: Float, width: Float, rtl: Boolean = false): Float {
    if (width <= 0f || x.isNaN()) return 0f
    val f = (x / width).coerceIn(0f, 1f)
    return if (rtl) 1f - f else f
}

/**
 * An invisible touch area that seeks: [onDrag] reports the fraction under the finger from the first touch on,
 * [onSeek] the fraction where it lifts (a tap is a drag that does not move), [onCancel] a gesture that ends
 * without a lift. The touch is consumed, so a pager around it does not swipe.
 */
@Composable
internal fun SeekArea(modifier: Modifier, onDrag: (Float) -> Unit, onSeek: (Float) -> Unit, onCancel: () -> Unit) {
    val drag = rememberUpdatedState(onDrag)
    val seek = rememberUpdatedState(onSeek)
    val cancel = rememberUpdatedState(onCancel)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(
        modifier.pointerInput(rtl) {
            awaitEachGesture {
                val down = awaitFirstDown()
                down.consume()
                var fraction = seekFraction(down.position.x, size.width.toFloat(), rtl)
                drag.value(fraction)
                while (true) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                    if (change == null) { cancel.value(); return@awaitEachGesture }
                    fraction = seekFraction(change.position.x, size.width.toFloat(), rtl)
                    // A cancelled stream arrives as an already consumed change that is no longer pressed: not a release.
                    if (!change.pressed && change.isConsumed) { cancel.value(); return@awaitEachGesture }
                    change.consume()
                    if (!change.pressed) { seek.value(fraction); return@awaitEachGesture }
                    drag.value(fraction)
                }
            }
        },
    )
}
