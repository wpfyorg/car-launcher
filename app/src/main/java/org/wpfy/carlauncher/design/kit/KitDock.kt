package org.wpfy.carlauncher.design.kit

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import org.wpfy.carlauncher.design.kit.PenKit.DockLandscape as Landscape
import org.wpfy.carlauncher.design.kit.PenKit.DockLandscapeCompact as Compact
import org.wpfy.carlauncher.design.kit.PenKit.DockPortrait as Portrait
import org.wpfy.carlauncher.design.theme.FixedScale

/*
 * The docks (Pen "Dock · Landscape" and "Dock · Portrait" of the Kits cluster). Pinned apps draw the
 * app's own launcher icon from the system at the Pen tile size; every other glyph is the Pen's.
 * The clock and the badge count keep their Pen size whatever the user's font scale, as in the Pen's
 * large-text frames.
 */

/** A pinned app: its Pen node name in reference renders, its label, its system icon (null while loading). */
class KitDockApp(val node: String, val label: String, val icon: ImageBitmap?, val onClick: () -> Unit)

/**
 * What the docks do on tap. The defaults are the Pen's look; a live shell that turns the all-apps
 * button into a way back sets [allAppsIcon] and [allAppsDescription], and one that turns the bell
 * into a way back sets [notificationsDescription] (null keeps "Notifications" with its count).
 * A shell that lets pinned apps be reordered sets [onMoveApp]: long-press a pinned app, drag it along
 * the dock and release to call it with the app's index and the index it was dropped on. Left null, the
 * apps only open on tap, as in the Pen.
 */
class KitDockActions(
    val onNotifications: () -> Unit = {},
    val onVoice: () -> Unit = {},
    val onAllApps: () -> Unit = {},
    val allAppsIcon: ImageVector? = null,
    val allAppsDescription: String = "All apps",
    val notificationsDescription: String? = null,
    val onMoveApp: ((from: Int, to: Int) -> Unit)? = null,
)

/**
 * The landscape dock of the Medium, Large and XL tiers: the Large dock at [scale] (PenKit.DockScale
 * of the tier), as tall as [height]. Clock and bell at the top, pinned apps centred between two equal
 * spacers, voice and all apps at the bottom.
 */
@Composable
fun KitDockLandscape(
    clock: String,
    notificationCount: Int,
    apps: List<KitDockApp>,
    height: Dp,
    scale: Float,
    modifier: Modifier = Modifier,
    actions: KitDockActions = KitDockActions(),
) {
    val c = "DockLandscape"
    Box(modifier.width(Landscape.W * scale).height(height)) {
        FixedScale(scale) {
            Column(
                modifier = Modifier.pen(c).width(Landscape.W).fillMaxHeight().background(Landscape.Fill)
                    .padding(top = Landscape.PadTop, bottom = Landscape.PadBottom),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PenText(clock, Landscape.Clock.Style, Landscape.Clock.Fill, Modifier.pen(c, "Clock"), fixed = true)
                DockBell(
                    c, "Notifications", notificationCount, Landscape.Notifications.W, Landscape.Notifications.Bell.W, Landscape.Notifications.Bell.Fill,
                    Landscape.Notifications.Badge.X, Landscape.Notifications.Badge.Y, Landscape.Notifications.Badge.W, Landscape.Notifications.Badge.Fill,
                    Landscape.Notifications.Badge.Stroke, Landscape.Notifications.Badge.StrokeColor, Landscape.Notifications.Badge.Count.Style, Landscape.Notifications.Badge.Count.Fill,
                    actions.onNotifications, actions.notificationsDescription,
                )
                Spacer(Modifier.pen(c, "SpacerTop").weight(1f))
                Column(Modifier.pen(c, "PinnedApps"), verticalArrangement = Arrangement.spacedBy(Landscape.PinnedApps.Gap)) {
                    apps.forEachIndexed { index, app ->
                        DockApp(
                            c, "PinnedApps/${app.node}", app, Landscape.PinnedApps.Calculator.W, Landscape.PinnedApps.Calculator.Radius,
                            DockReorder(index, apps.size, vertical = true, Landscape.PinnedApps.Gap, actions.onMoveApp),
                        )
                    }
                }
                Spacer(Modifier.pen(c, "SpacerBottom").weight(1f))
                PenDisc(
                    Landscape.VoiceAssistant.W, Color.Transparent, Landscape.VoiceAssistant.Icon.Vector, Landscape.VoiceAssistant.Icon.W, Landscape.VoiceAssistant.Icon.Fill,
                    Modifier.pen(c, "VoiceAssistant"), iconModifier = Modifier.pen(c, "VoiceAssistant/Icon"), description = "Voice assistant", onClick = actions.onVoice,
                )
                PenDisc(
                    Landscape.AllApps.W, Landscape.AllApps.Fill, actions.allAppsIcon ?: Landscape.AllApps.Icon.Vector, Landscape.AllApps.Icon.W, Landscape.AllApps.Icon.Fill,
                    Modifier.pen(c, "AllApps"), iconModifier = Modifier.pen(c, "AllApps/Icon"), description = actions.allAppsDescription, onClick = actions.onAllApps,
                )
            }
        }
    }
}

/**
 * The Small tier's landscape dock (Pen "Dock · Landscape · Small"): clock, bell, voice and all apps
 * only; pinned apps live in the drawer on screens this short. As tall as [height], in real dp.
 */
@Composable
fun KitDockLandscapeCompact(
    clock: String,
    notificationCount: Int,
    height: Dp,
    modifier: Modifier = Modifier,
    actions: KitDockActions = KitDockActions(),
) {
    val c = "DockLandscapeCompact"
    Column(
        modifier = modifier.pen(c).width(Compact.W).height(height).padding(top = Compact.PadTop, bottom = Compact.PadBottom),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PenText(clock, Compact.Clock.Style, Compact.Clock.Fill, Modifier.pen(c, "Clock"), fixed = true)
        DockBell(
            c, "Notifications", notificationCount, Compact.Notifications.W, Compact.Notifications.Bell.W, Compact.Notifications.Bell.Fill,
            Compact.Notifications.Badge.X, Compact.Notifications.Badge.Y, Compact.Notifications.Badge.W, Compact.Notifications.Badge.Fill,
            Compact.Notifications.Badge.Stroke, Compact.Notifications.Badge.StrokeColor, Compact.Notifications.Badge.Count.Style, Compact.Notifications.Badge.Count.Fill,
            actions.onNotifications, actions.notificationsDescription,
        )
        Spacer(Modifier.pen(c, "Spacer").weight(1f))
        PenDisc(
            Compact.VoiceAssistant.W, Color.Transparent, Compact.VoiceAssistant.Icon.Vector, Compact.VoiceAssistant.Icon.W, Compact.VoiceAssistant.Icon.Fill,
            Modifier.pen(c, "VoiceAssistant"), iconModifier = Modifier.pen(c, "VoiceAssistant/Icon"), description = "Voice assistant", onClick = actions.onVoice,
        )
        PenDisc(
            Compact.AllApps.W, Compact.AllApps.Fill, actions.allAppsIcon ?: Compact.AllApps.Icon.Vector, Compact.AllApps.Icon.W, Compact.AllApps.Icon.Fill,
            Modifier.pen(c, "AllApps"), iconModifier = Modifier.pen(c, "AllApps/Icon"), description = actions.allAppsDescription, onClick = actions.onAllApps,
        )
    }
}

/**
 * The portrait dock (Pen "Dock · Portrait"), [width] wide: all apps and voice at the start, pinned
 * apps centred between two equal spacers, bell and clock at the end.
 */
@Composable
fun KitDockPortrait(
    clock: String,
    notificationCount: Int,
    apps: List<KitDockApp>,
    width: Dp,
    modifier: Modifier = Modifier,
    actions: KitDockActions = KitDockActions(),
) {
    val c = "DockPortrait"
    Row(
        modifier = modifier.pen(c).width(width).height(Portrait.H).background(Portrait.Fill).padding(start = Portrait.PadStart, end = Portrait.PadEnd),
        horizontalArrangement = Arrangement.spacedBy(Portrait.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PenDisc(
            Portrait.AllApps.W, Portrait.AllApps.Fill, actions.allAppsIcon ?: Portrait.AllApps.Icon.Vector, Portrait.AllApps.Icon.W, Portrait.AllApps.Icon.Fill,
            Modifier.pen(c, "AllApps"), iconModifier = Modifier.pen(c, "AllApps/Icon"), description = actions.allAppsDescription, onClick = actions.onAllApps,
        )
        PenDisc(
            Portrait.Voice.W, Portrait.Voice.Fill, Portrait.Voice.Icon.Vector, Portrait.Voice.Icon.W, Portrait.Voice.Icon.Fill,
            Modifier.pen(c, "Voice"), iconModifier = Modifier.pen(c, "Voice/Icon"), description = "Voice assistant", onClick = actions.onVoice,
        )
        Spacer(Modifier.pen(c, "SpacerLeft").weight(1f))
        apps.forEachIndexed { index, app ->
            DockApp(
                c, app.node, app, Portrait.Maps.W, Portrait.Maps.Radius,
                DockReorder(index, apps.size, vertical = false, Portrait.Gap, actions.onMoveApp),
            )
        }
        Spacer(Modifier.pen(c, "Spacer").weight(1f))
        DockBell(
            c, "Notifications", notificationCount, Portrait.Notifications.W, Portrait.Notifications.Bell.W, Portrait.Notifications.Bell.Fill,
            Portrait.Notifications.Badge.X, Portrait.Notifications.Badge.Y, Portrait.Notifications.Badge.W, Portrait.Notifications.Badge.Fill,
            Portrait.Notifications.Badge.Stroke, Portrait.Notifications.Badge.StrokeColor, Portrait.Notifications.Badge.Count.Style, Portrait.Notifications.Badge.Count.Fill,
            actions.onNotifications, actions.notificationsDescription,
        )
        PenText(clock, Portrait.Clock.Style, Portrait.Clock.Fill, Modifier.pen(c, "Clock"), fixed = true)
    }
}

/** Where a pinned app sits among [count] along the dock, and what to call when it is dragged to another place. */
private class DockReorder(val index: Int, val count: Int, val vertical: Boolean, val gap: Dp, val onMove: ((Int, Int) -> Unit)?)

/**
 * The index a pinned app at [index] is dropped on after being dragged [offsetPx] along the dock,
 * where neighbouring apps are [pitchPx] apart: the nearest slot, kept inside the [count] apps.
 */
internal fun dragTargetIndex(index: Int, offsetPx: Float, pitchPx: Float, count: Int): Int {
    if (count <= 0 || pitchPx <= 0f) return index
    return (index + (offsetPx / pitchPx).roundToInt()).coerceIn(0, count - 1)
}

/** A pinned app: the system launcher icon at the Pen tile size. */
@Composable
private fun DockApp(c: String, path: String, app: KitDockApp, size: Dp, radius: Dp, reorder: DockReorder) {
    val onMove = reorder.onMove?.takeIf { reorder.count > 1 }
    val tile = Modifier.pen(c, path)
    val gestures = if (onMove == null) {
        Modifier.size(size).clip(penShape(radius)).penClick(app.onClick)
    } else {
        reorderableTile(size, radius, reorder, onMove, app.onClick)
    }
    Box(modifier = tile.then(gestures).semantics { contentDescription = app.label }) {
        if (app.icon != null) {
            Image(app.icon, null, Modifier.size(size), contentScale = ContentScale.Fit)
        } else {
            // Loading: the Pen's neutral tile.
            Box(Modifier.size(size).background(PenKit.DockPortrait.Calculator.Fill))
        }
    }
}

/** A tile that opens on tap and, after a long press, follows the finger along the dock until released. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun reorderableTile(size: Dp, radius: Dp, reorder: DockReorder, onMove: (Int, Int) -> Unit, onOpen: () -> Unit): Modifier {
    var dragging by remember { mutableStateOf(false) }
    var drag by remember { mutableStateOf(Offset.Zero) }
    val pitchPx = with(LocalDensity.current) { (size + reorder.gap).toPx() }
    val currentOpen by rememberUpdatedState(onOpen)
    val currentMove by rememberUpdatedState(onMove)
    fun finish(drop: Boolean) {
        val target = dragTargetIndex(reorder.index, if (reorder.vertical) drag.y else drag.x, pitchPx, reorder.count)
        dragging = false
        drag = Offset.Zero
        if (drop && target != reorder.index) currentMove(reorder.index, target)
    }
    return Modifier
        .zIndex(if (dragging) 1f else 0f)
        .graphicsLayer {
            translationX = if (dragging) drag.x else 0f
            translationY = if (dragging) drag.y else 0f
            scaleX = if (dragging) DragLift else 1f
            scaleY = if (dragging) DragLift else 1f
        }
        .size(size)
        .clip(penShape(radius))
        // The same click as penClick (indication, focus, Enter/Space/D-pad). The long click does nothing but
        // keep a long press from counting as a tap, and gives the long-press haptic.
        .combinedClickable(onClick = { currentOpen() }, onLongClick = {})
        // Inside the layer above, so the finger's offsets are not moved by the tile following it.
        .pointerInput(reorder.index, reorder.count, pitchPx) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    dragging = true
                    drag = Offset.Zero
                },
                onDrag = { change, amount ->
                    change.consume()
                    drag += amount
                },
                onDragEnd = { finish(drop = true) },
                onDragCancel = { finish(drop = false) },
            )
        }
        .semantics {
            // Screen readers and switch access cannot drag: one step along the dock where there is a slot.
            val (back, forward) = if (reorder.vertical) "Move up" to "Move down" else "Move left" to "Move right"
            customActions = listOfNotNull(
                if (reorder.index > 0) CustomAccessibilityAction(back) { currentMove(reorder.index, reorder.index - 1); true } else null,
                if (reorder.index < reorder.count - 1) CustomAccessibilityAction(forward) { currentMove(reorder.index, reorder.index + 1); true } else null,
            )
        }
}

private const val DragLift = 1.1f

/** The bell with its red count badge at the Pen offset; no badge at zero. */
@Composable
private fun DockBell(
    c: String,
    path: String,
    count: Int,
    size: Dp,
    bellSize: Dp,
    bellTint: Color,
    badgeX: Dp,
    badgeY: Dp,
    badgeSize: Dp,
    badgeFill: Color,
    badgeStroke: Dp,
    badgeStrokeColor: Color,
    countStyle: TextStyle,
    countColor: Color,
    onClick: () -> Unit,
    description: String? = null,
    bell: ImageVector = PenIcons.Notifications,
) {
    Box(
        modifier = Modifier.pen(c, path).size(size).clip(penShape(999f.toDp())).penClick(onClick)
            .semantics { contentDescription = description ?: if (count > 0) "Notifications, $count" else "Notifications" },
    ) {
        Icon(bell, null, Modifier.pen(c, "$path/Bell").align(Alignment.Center).size(bellSize), tint = bellTint)
        if (count > 0) {
            val shape = penShape(999f.toDp())
            Box(
                modifier = Modifier.offset(badgeX, badgeY).pen(c, "$path/Badge").size(badgeSize)
                    .background(badgeFill, shape).border(badgeStroke, badgeStrokeColor, shape),
                contentAlignment = Alignment.Center,
            ) {
                PenText(if (count > 9) "9+" else "$count", countStyle, countColor, Modifier.pen(c, "$path/Badge/Count"), fixed = true)
            }
        }
    }
}

private fun Float.toDp(): Dp = Dp(this)
