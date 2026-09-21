package org.wpfy.carlauncher.design.kit

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * The colour work behind the album art background of the media surfaces, on plain ARGB ints so it
 * runs anywhere (no Bitmap, no RenderEffect): the art's dominant colour, the dark tint made from it,
 * a box blur, and the least tint alpha that keeps the surface's text readable over the brightest
 * part of the blurred art.
 */
internal object MediaArtColors {
    /** WCAG AA for body text: every text fill of a media surface keeps this over the art. */
    const val TextContrast = 4.5

    /** The tint keeps the art's hue but never gets lighter than this (relative luminance). */
    const val TintLuminance = 0.05

    /**
     * The art's dominant colour: pixels (alpha at least half) fall into 4-bit-per-channel buckets,
     * each weighed by its population and lifted by its saturation so a vivid colour beats a slightly
     * larger mass of grey, as Palette's vibrant swatches do; the winner is its bucket's average.
     * Null when no pixel is opaque enough.
     */
    fun dominantColor(pixels: IntArray): Int? {
        val count = IntArray(4096)
        val r = LongArray(4096)
        val g = LongArray(4096)
        val b = LongArray(4096)
        for (p in pixels) {
            if (p ushr 24 < 128) continue
            val pr = p shr 16 and 0xFF
            val pg = p shr 8 and 0xFF
            val pb = p and 0xFF
            val i = (pr shr 4 shl 8) or (pg shr 4 shl 4) or (pb shr 4)
            count[i]++
            r[i] += pr.toLong(); g[i] += pg.toLong(); b[i] += pb.toLong()
        }
        var best = -1
        var bestScore = 0.0
        for (i in 0 until 4096) {
            val n = count[i]
            if (n == 0) continue
            val c = argb(r[i] / n, g[i] / n, b[i] / n)
            val score = n * (0.25 + saturation(c))
            if (score > bestScore) { bestScore = score; best = i }
        }
        if (best < 0) return null
        val n = count[best]
        return argb(r[best] / n, g[best] / n, b[best] / n)
    }

    /**
     * [dominant] made dark enough to carry [text] on its own: hue kept, saturation capped, luminance
     * at most [TintLuminance] and lower still until [text] keeps [target] contrast on it, so a full
     * tint is always readable.
     */
    fun tint(dominant: Int, text: Int, target: Double = TextContrast): Int {
        val hsl = toHsl(dominant)
        var c = fromHsl(hsl[0], min(hsl[1], 0.75f), min(hsl[2], 0.32f))
        while (c and 0xFFFFFF != 0 && (luminance(c) > TintLuminance || !readable(text, c, target))) c = scale(c, 0.9f)
        return c
    }

    /** [text] (its own alpha honoured) keeps [target] contrast on the opaque [bg]. */
    fun readable(text: Int, bg: Int, target: Double = TextContrast): Boolean =
        contrast(over(text, (text ushr 24) / 255f, bg), bg) >= target

    /**
     * The least alpha of [tint] over [brightest] (the brightest pixel of the blurred art) at which
     * [text] (its own alpha honoured) keeps [target] contrast; every darker pixel then keeps it too.
     */
    fun scrimAlpha(tint: Int, brightest: Int, text: Int, target: Double = TextContrast): Float {
        fun readable(a: Float) = readable(text, over(tint, a, brightest), target)
        if (readable(0f)) return 0f
        var lo = 0f
        var hi = 1f
        repeat(16) { val mid = (lo + hi) / 2; if (readable(mid)) hi = mid else lo = mid }
        return hi
    }

    /**
     * The scrim over the blurred art, as (position, ARGB) stops along the surface's gradient: the
     * tint of [dominant] for [text], at the least readable alpha over [brightest] and deepening
     * towards the end. Null without a dominant colour: the surface keeps its Pen gradient.
     */
    fun scrim(dominant: Int?, brightest: Int, text: Int): List<Pair<Float, Int>>? {
        if (dominant == null) return null
        val tint = tint(dominant, text)
        val a = scrimAlpha(tint, brightest, text)
        fun at(min: Float) = (((max(a, min) * 255 + 0.5f).toInt() shl 24) or (tint and 0xFFFFFF))
        return listOf(0f to at(0.35f), 0.6f to at(0.7f), 1f to at(0.92f))
    }

    /** The brightest (highest relative luminance) pixel. */
    fun brightest(pixels: IntArray): Int = pixels.maxByOrNull { luminance(it) } ?: 0xFF000000.toInt()

    /** A box blur of [radius] run [passes] times (three come close to a Gaussian); edges clamp. */
    fun blur(pixels: IntArray, width: Int, height: Int, radius: Int, passes: Int = 3): IntArray {
        var src = pixels.copyOf()
        var tmp = IntArray(src.size)
        repeat(passes) {
            boxPass(src, tmp, width, height, radius, horizontal = true)
            boxPass(tmp, src, width, height, radius, horizontal = false)
        }
        return src
    }

    private fun boxPass(src: IntArray, dst: IntArray, w: Int, h: Int, radius: Int, horizontal: Boolean) {
        val lines = if (horizontal) h else w
        val len = if (horizontal) w else h
        val span = radius * 2 + 1
        for (line in 0 until lines) {
            fun at(i: Int): Int {
                val k = i.coerceIn(0, len - 1)
                return src[if (horizontal) line * w + k else k * w + line]
            }
            var sa = 0; var sr = 0; var sg = 0; var sb = 0
            for (i in -radius..radius) { val p = at(i); sa += p ushr 24; sr += p shr 16 and 0xFF; sg += p shr 8 and 0xFF; sb += p and 0xFF }
            for (i in 0 until len) {
                dst[if (horizontal) line * w + i else i * w + line] =
                    ((sa / span) shl 24) or ((sr / span) shl 16) or ((sg / span) shl 8) or (sb / span)
                val out = at(i - radius)
                val inc = at(i + radius + 1)
                sa += (inc ushr 24) - (out ushr 24)
                sr += (inc shr 16 and 0xFF) - (out shr 16 and 0xFF)
                sg += (inc shr 8 and 0xFF) - (out shr 8 and 0xFF)
                sb += (inc and 0xFF) - (out and 0xFF)
            }
        }
    }

    /** [top] at [alpha] over the opaque [bottom] (sRGB channels, as the canvas blends). */
    fun over(top: Int, alpha: Float, bottom: Int): Int {
        fun ch(s: Int) = ((top shr s and 0xFF) * alpha + (bottom shr s and 0xFF) * (1 - alpha) + 0.5f).toInt()
        return argb(ch(16).toLong(), ch(8).toLong(), ch(0).toLong())
    }

    /** WCAG relative luminance of an opaque colour. */
    fun luminance(c: Int): Double {
        fun lin(v: Int): Double { val s = v / 255.0; return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4) }
        return 0.2126 * lin(c shr 16 and 0xFF) + 0.7152 * lin(c shr 8 and 0xFF) + 0.0722 * lin(c and 0xFF)
    }

    /** WCAG contrast ratio of two opaque colours. */
    fun contrast(a: Int, b: Int): Double {
        val la = luminance(a); val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun argb(r: Long, g: Long, b: Long): Int = (0xFF shl 24) or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()

    private fun scale(c: Int, f: Float): Int =
        argb(((c shr 16 and 0xFF) * f).toLong(), ((c shr 8 and 0xFF) * f).toLong(), ((c and 0xFF) * f).toLong())

    private fun saturation(c: Int): Float = toHsl(c)[1]

    private fun toHsl(c: Int): FloatArray {
        val r = (c shr 16 and 0xFF) / 255f; val g = (c shr 8 and 0xFF) / 255f; val b = (c and 0xFF) / 255f
        val mx = max(r, max(g, b)); val mn = min(r, min(g, b)); val l = (mx + mn) / 2
        if (mx == mn) return floatArrayOf(0f, 0f, l)
        val d = mx - mn
        val s = if (l > 0.5f) d / (2 - mx - mn) else d / (mx + mn)
        val h = when (mx) {
            r -> ((g - b) / d + (if (g < b) 6 else 0))
            g -> (b - r) / d + 2
            else -> (r - g) / d + 4
        } * 60f
        return floatArrayOf(h, s, l)
    }

    private fun fromHsl(h: Float, s: Float, l: Float): Int {
        val c = (1 - kotlin.math.abs(2 * l - 1)) * s
        val x = c * (1 - kotlin.math.abs((h / 60f) % 2 - 1))
        val m = l - c / 2
        val (r, g, b) = when ((h / 60f).toInt().coerceIn(0, 5)) {
            0 -> Triple(c, x, 0f); 1 -> Triple(x, c, 0f); 2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c); 4 -> Triple(x, 0f, c); else -> Triple(c, 0f, x)
        }
        fun ch(v: Float) = ((v + m) * 255 + 0.5f).toInt().coerceIn(0, 255).toLong()
        return argb(ch(r), ch(g), ch(b))
    }
}
