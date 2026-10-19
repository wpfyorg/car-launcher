package org.wpfy.carlauncher.feature.home

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.wpfy.carlauncher.design.kit.PenKit
import org.wpfy.carlauncher.design.kit.PenScreens
import org.wpfy.carlauncher.design.kit.ScreenGroup
import org.wpfy.carlauncher.design.screens.MapScreenSpec
import org.wpfy.carlauncher.design.screens.PortraitLWideMediaCard
import org.wpfy.carlauncher.design.screens.PortraitScreenSpec
import org.wpfy.carlauncher.design.theme.OverlayTier

/** How Home presents media next to the map. */
internal enum class HomeMediaMode {
    /** The media card beside (or, in tall windows, below) the map. */
    Card,

    /** The map takes the whole pane; media is a now-playing chip on it. */
    Chip,

    /** Small screens: a full-pane map page and a full-pane media page that slides over it. */
    Paged,

    /** Only while the navigation app is expanded to the whole area: the map alone. */
    None,
}

/**
 * Small screens page between a full-pane map and a full-pane media page. Larger windows keep the media card while it
 * fits and fall back to the chip otherwise. With nothing playing there is no media page, but the card (or the chip)
 * stays in its idle state so media can be started. The Drive Card ([driveCard]) takes its slot in every group
 * whether or not it "fits", so it never falls back to the chip; on Small it sits beside the map on the map page.
 *
 * [cardFits] is [homeCardFits].
 */
internal fun homeMediaModeFor(
    tier: OverlayTier,
    hasSession: Boolean,
    cardFits: Boolean,
    driveCard: Boolean = false,
): HomeMediaMode = when {
    tier == OverlayTier.Small -> if (hasSession) HomeMediaMode.Paged else HomeMediaMode.Chip
    driveCard -> HomeMediaMode.Card
    !cardFits -> HomeMediaMode.Chip
    else -> HomeMediaMode.Card
}

/** Room the map keeps beside (or above) the media card while guiding, and without guidance. */
internal val GuidanceMinMapWidth = 420.dp
internal val MinMapWidth = 280.dp
internal val GuidanceMinMapHeight = 360.dp
internal val MinMapHeight = 240.dp

/**
 * Whether the media card has room in [group] for an area of [areaWidth] x [areaHeight] (the shell's content):
 * beside the map in the landscape groups, under it at Portrait L (the media card that stays when there is no Drive Card),
 * never on the Small groups (they page). The map pane keeps [GuidanceMinMapWidth] (or height, in portrait) while guiding.
 */
internal fun homeCardFits(group: ScreenGroup, areaWidth: Dp, areaHeight: Dp, guiding: Boolean): Boolean = when (group) {
    ScreenGroup.LandscapeS, ScreenGroup.PortraitS -> false
    ScreenGroup.PortraitL -> {
        val inset = PortraitScreenSpec.PortraitL.inset
        areaHeight - PortraitLWideMediaCard.H - inset >= if (guiding) GuidanceMinMapHeight else MinMapHeight
    }
    else -> {
        val spec = mapScreenSpecFor(group)
        val cardWidth = PenKit.MediaCard.W * (areaHeight / PenKit.MediaCard.H)
        areaWidth - cardWidth - spec.paneY >= if (guiding) GuidanceMinMapWidth else MinMapWidth
    }
}

/** The landscape screen spec of a Medium, Large or XLarge group. */
internal fun mapScreenSpecFor(group: ScreenGroup): MapScreenSpec = when (group) {
    ScreenGroup.LandscapeL, ScreenGroup.UltrawideL -> MapScreenSpec.LandscapeL
    ScreenGroup.LandscapeXL -> MapScreenSpec.LandscapeXL
    else -> MapScreenSpec.LandscapeM
}

/** What tapping the chip does: on a paged screen it shows the media page, otherwise it opens the full media screen. */
internal enum class ChipAction { OpenMedia, ShowMediaPage }

internal fun chipActionFor(mode: HomeMediaMode): ChipAction =
    if (mode == HomeMediaMode.Paged) ChipAction.ShowMediaPage else ChipAction.OpenMedia

// --- Small-screen paging ---------------------------------------------------------------------

/** The next maneuver is this close (metres) or this soon (seconds): the map must stay in view. */
internal const val ManeuverReturnMeters = 300.0
internal const val ManeuverReturnSeconds = 15.0

/** True when the next maneuver is within [ManeuverReturnMeters] or [ManeuverReturnSeconds] at the current speed. */
internal fun maneuverIsClose(distanceMeters: Double?, speedMps: Double?): Boolean {
    if (distanceMeters == null || distanceMeters < 0.0) return false
    if (distanceMeters <= ManeuverReturnMeters) return true
    val speed = speedMps?.takeIf { it > MinSpeedForTimeMps } ?: return false
    return distanceMeters / speed <= ManeuverReturnSeconds
}

private const val MinSpeedForTimeMps = 0.5

/**
 * The media page has to give way to the map: during guidance when the next maneuver is close, or
 * when a camera alert has escalated. While true the page animates back and swiping to media is off.
 */
internal fun mapPageRequired(
    guiding: Boolean,
    nextManeuverMeters: Double?,
    speedMps: Double?,
    cameraUrgent: Boolean,
): Boolean = cameraUrgent || (guiding && maneuverIsClose(nextManeuverMeters, speedMps))

/** Width of the strip at the end edge of the map page that starts a swipe to the media page. */
internal val PageEdgeStrip = 24.dp

/** A swipe on the map page starts the page change only from the edge strip (otherwise the map pans). */
internal fun swipeStartsPageChange(startX: Float, widthPx: Float, edgePx: Float): Boolean =
    widthPx > 0f && startX >= widthPx - edgePx

private const val FlingVelocityPxPerSecond = 700f

/**
 * Where a page drag settles: 0 (map) or 1 (media). [velocityPxPerSecond] is the horizontal finger
 * velocity (negative toward the media page, which sits at the end edge). A fling decides first,
 * otherwise the page past halfway wins. With swiping suppressed it is always the map.
 */
internal fun pageSettleTarget(progress: Float, velocityPxPerSecond: Float, swipeEnabled: Boolean): Int = when {
    !swipeEnabled -> 0
    velocityPxPerSecond <= -FlingVelocityPxPerSecond -> 1
    velocityPxPerSecond >= FlingVelocityPxPerSecond -> 0
    progress >= 0.5f -> 1
    else -> 0
}

/** Progress after a horizontal finger move of [deltaPx]; the media page cannot be opened while suppressed. */
internal fun pageProgressAfterDrag(progress: Float, deltaPx: Float, widthPx: Float, swipeEnabled: Boolean): Float {
    if (widthPx <= 0f) return progress
    val next = progress - deltaPx / widthPx
    // Suppressed: the page can only move back toward the map.
    val upper = if (swipeEnabled) 1f else progress.coerceIn(0f, 1f)
    return next.coerceIn(0f, upper)
}

/** Map page ⇄ media page position: 0 is the map, 1 is the media page. */
@Stable
internal class MediaPageState(private val scope: CoroutineScope) {
    var progress by mutableFloatStateOf(0f)
        private set

    /** The page the position is heading to (0 map, 1 media). */
    var target by mutableIntStateOf(0)
        private set

    private var animation: Job? = null

    val isMediaShown: Boolean get() = progress > 0f

    fun animateTo(page: Int) {
        animation?.cancel()
        target = page
        animation = scope.launch {
            animate(progress, page.toFloat(), animationSpec = tween(PageMillis)) { value, _ -> progress = value }
        }
    }

    /** Follows the finger; cancels a running settle animation. */
    fun dragTo(value: Float) {
        animation?.cancel()
        progress = value
        target = if (value >= 0.5f) 1 else 0
    }

    fun jumpTo(page: Int) {
        animation?.cancel()
        progress = page.toFloat()
        target = page
    }
}

private const val PageMillis = 260

/** The map pane's place in the content area (what the kit screen's pane covers) and its corner radius. */
internal data class HomeMapBounds(val x: Dp, val y: Dp, val width: Dp, val height: Dp, val radius: Dp)

/**
 * Where the kit screen of [group] puts its map pane in an area of [areaWidth] x [areaHeight]: the whole area, less
 * the card beside it ([card]; the media card's width follows the area's height, the Drive Card's is [driveCardWidth])
 * or, in portrait, the wide Drive Card of [wideCardHeight] under it. Home keeps the map at one place in the composition
 * and sizes it to this, so rotating resizes it instead of rebuilding it.
 */
internal fun homeMapBounds(
    group: ScreenGroup,
    card: Boolean,
    areaWidth: Dp,
    areaHeight: Dp,
    driveCard: Boolean = false,
    wideCardHeight: Dp = 0.dp,
): HomeMapBounds = when (group) {
    ScreenGroup.LandscapeS -> {
        val beside = if (driveCard) driveCardWidth(group, areaHeight) + PenScreens.LandscapeS.Idle.MapPane.Y else 0.dp
        HomeMapBounds(0.dp, 0.dp, areaWidth - beside, areaHeight, PenScreens.LandscapeS.Idle.MapPane.Radius)
    }
    ScreenGroup.PortraitS, ScreenGroup.PortraitL -> {
        val spec = if (group == ScreenGroup.PortraitS) PortraitScreenSpec.PortraitS else PortraitScreenSpec.PortraitL
        val below = if (driveCard || card) wideCardHeight + spec.inset else 0.dp
        HomeMapBounds(0.dp, 0.dp, areaWidth, areaHeight - below, spec.radius)
    }
    else -> {
        val spec = mapScreenSpecFor(group)
        val beside = when {
            driveCard -> driveCardWidth(group, areaHeight) + spec.paneY
            card -> PenKit.MediaCard.W * (areaHeight / PenKit.MediaCard.H) + spec.paneY
            else -> 0.dp
        }
        HomeMapBounds(0.dp, 0.dp, areaWidth - beside, areaHeight, spec.paneRadius)
    }
}
