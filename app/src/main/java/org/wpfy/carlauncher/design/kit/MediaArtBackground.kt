package org.wpfy.carlauncher.design.kit

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * What a track's album art gives the media surfaces: the art blurred (small, drawn scaled up), its
 * dominant colour, and the brightest pixel of the blur (what the tint must cover for text to read).
 */
@Immutable
internal class MediaArtBackdrop(val blur: ImageBitmap, val dominant: Int, val brightest: Int)

/** Side of the square the art is shrunk to before the blur; the canvas scales it back up smoothly. */
private const val BlurSide = 32
private const val BlurRadius = 3
private const val CrossfadeMillis = 600

/** Backdrops by art: a track's art is one bitmap, so this is a cache per track. */
private val backdrops = LruCache<ImageBitmap, MediaArtBackdrop>(8)

/** Shrinks, blurs and measures [art] on the CPU, so it runs on every API level (RenderEffect needs 31). */
internal fun mediaArtBackdrop(art: ImageBitmap): MediaArtBackdrop? {
    backdrops.get(art)?.let { return it }
    val source = art.asAndroidBitmap().let { if (it.config == Bitmap.Config.HARDWARE) it.copy(Bitmap.Config.ARGB_8888, false) else it }
        ?: return null
    val small = Bitmap.createScaledBitmap(source, BlurSide, BlurSide, true)
    val pixels = IntArray(BlurSide * BlurSide)
    small.getPixels(pixels, 0, BlurSide, 0, 0, BlurSide, BlurSide)
    val dominant = MediaArtColors.dominantColor(pixels) ?: return null
    val blurred = MediaArtColors.blur(pixels, BlurSide, BlurSide, BlurRadius)
    val bitmap = Bitmap.createBitmap(blurred, BlurSide, BlurSide, Bitmap.Config.ARGB_8888)
    return MediaArtBackdrop(bitmap.asImageBitmap(), dominant, MediaArtColors.brightest(blurred)).also { backdrops.put(art, it) }
}

/** [art]'s backdrop: at once when cached, else made off the main thread (null until then, and without art). */
@Composable
internal fun rememberMediaArtBackdrop(art: ImageBitmap?): MediaArtBackdrop? =
    produceState(art?.let { backdrops.get(it) }, art) {
        value = art?.let { withContext(Dispatchers.Default) { runCatching { mediaArtBackdrop(it) }.getOrNull() } }
    }.value

/** The Pen linear gradient brush of [stops] at [rotation] for a box of [size] (see [penGradient]). */
internal fun penBrush(stops: List<Pair<Float, Color>>, rotation: Float, size: Size): Brush {
    val rad = Math.toRadians(rotation.toDouble())
    val dx = -sin(rad).toFloat()
    val dy = -cos(rad).toFloat()
    val half = (abs(size.width * dx) + abs(size.height * dy)) / 2f
    val c = Offset(size.width / 2f, size.height / 2f)
    return Brush.linearGradient(
        colorStops = stops.toTypedArray(),
        start = c - Offset(dx * half, dy * half),
        end = c + Offset(dx * half, dy * half),
    )
}

/**
 * The background of a media surface. Without album art it is the Pen's [fallback] gradient at
 * [rotation], as before. With art it is the art blurred to fill the surface, under a gradient of
 * the art's dominant colour (darkened) along the same [rotation]; the gradient is never thinner
 * than what keeps [text], the surface's faintest text fill, at 4.5:1 over the brightest part of the
 * blur, so text reads over any art. A new track's background fades in over the old one.
 */
@Composable
fun Modifier.mediaArtBackground(art: ImageBitmap?, fallback: List<Pair<Float, Color>>, rotation: Float, shape: Shape, text: Color): Modifier {
    val backdrop = rememberMediaArtBackdrop(art)
    var shown by remember { mutableStateOf(backdrop) }
    var previous by remember { mutableStateOf<MediaArtBackdrop?>(null) }
    val fade = remember { Animatable(1f) }
    LaunchedEffect(backdrop) {
        if (backdrop === shown) return@LaunchedEffect
        previous = shown
        shown = backdrop
        fade.snapTo(0f)
        fade.animateTo(1f, tween(CrossfadeMillis))
        previous = null
    }
    val textArgb = text.toArgb()
    val shownScrim = remember(shown, textArgb) { shown?.let { mediaArtScrim(it, textArgb) } }
    val previousScrim = remember(previous, textArgb) { previous?.let { mediaArtScrim(it, textArgb) } }
    return clip(shape).drawBehind {
        val f = fade.value
        if (f < 1f) drawBackdrop(previous, previousScrim, fallback, rotation, 1f)
        drawBackdrop(shown, shownScrim, fallback, rotation, f)
    }
}

/** The scrim gradient of [backdrop] for text [text]: the tint at the least readable alpha, deepening along the gradient. */
internal fun mediaArtScrim(backdrop: MediaArtBackdrop, text: Int): List<Pair<Float, Color>>? =
    MediaArtColors.scrim(backdrop.dominant, backdrop.brightest, text)?.map { (at, c) -> at to Color(c) }

private fun DrawScope.drawBackdrop(
    backdrop: MediaArtBackdrop?, scrim: List<Pair<Float, Color>>?, fallback: List<Pair<Float, Color>>, rotation: Float, alpha: Float,
) {
    if (backdrop == null || scrim == null) {
        drawRect(penBrush(fallback, rotation, size), alpha = alpha)
        return
    }
    // The blur fills the surface as a centre crop.
    val side = BlurSide.toFloat()
    val fit = max(size.width / side, size.height / side)
    val src = Size(size.width / fit, size.height / fit)
    drawImage(
        backdrop.blur,
        srcOffset = IntOffset(((side - src.width) / 2).roundToInt(), ((side - src.height) / 2).roundToInt()),
        srcSize = IntSize(src.width.roundToInt().coerceIn(1, BlurSide), src.height.roundToInt().coerceIn(1, BlurSide)),
        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        alpha = alpha,
        filterQuality = FilterQuality.Low,
    )
    drawRect(penBrush(scrim, rotation, size), alpha = alpha)
}
