package app.rootlings.ui.art

import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** The outline ink every sprite is ringed with. */
val INK = rgb(0x202818)

fun hsl(h: Double, s: Double, l: Double): Int {
    val ss = (s / 100).coerceIn(0.0, 1.0)
    val ll = (l / 100).coerceIn(.04, .96)
    val c = (1 - kotlin.math.abs(2 * ll - 1)) * ss
    val hh = ((h % 360 + 360) % 360) / 60
    val x = c * (1 - kotlin.math.abs(hh % 2 - 1))
    val (r, g, b) = when (hh.toInt()) {
        0 -> Triple(c, x, 0.0)
        1 -> Triple(x, c, 0.0)
        2 -> Triple(0.0, c, x)
        3 -> Triple(0.0, x, c)
        4 -> Triple(x, 0.0, c)
        else -> Triple(c, 0.0, x)
    }
    val m = ll - c / 2
    fun v(q: Double) = ((q + m) * 255).roundToInt().coerceIn(0, 255)
    return (0xFF shl 24) or (v(r) shl 16) or (v(g) shl 8) or v(b)
}

/** A filled axis-aligned ellipse, the pixel way: whole rows. */
fun Buf.oval(cx: Double, cy: Double, rx: Double, ry: Double, c: Int) {
    val r0 = ry.roundToInt()
    for (j in -r0..r0) {
        val w = (rx * sqrt(max(0.0, 1 - (j * j) / ((ry + .5) * (ry + .5))))).roundToInt()
        for (i in -w..w) p((cx + i).roundToInt(), (cy + j).roundToInt(), c)
    }
}

/**
 * A leaf: an ellipse [len] long and [wid] wide, growing from (x, y) at [deg] (0 = straight up, clockwise).
 * The far half is shaded darker, the tip catches light, an optional vein runs down the middle.
 */
fun Buf.leaf(x: Double, y: Double, deg: Double, len: Double, wid: Double, dark: Int, base: Int, light: Int, vein: Int? = null) {
    val a = Math.toRadians(deg)
    val ux = sin(a)
    val uy = -cos(a)
    val cx = x + ux * len / 2
    val cy = y + uy * len / 2
    val r = (len / 2 + 1).toInt() + 1
    for (j in -r..r) for (i in -r..r) {
        val px = cx + i
        val py = cy + j
        val dx = px + .5 - cx
        val dy = py + .5 - cy
        val along = dx * ux + dy * uy
        val across = dx * -uy + dy * ux
        val e = (along / (len / 2)).let { it * it } + (across / (wid / 2)).let { it * it }
        if (e > 1) continue
        val col = when {
            vein != null && kotlin.math.abs(across) < .5 && along < len / 2 - 1 -> vein
            across > wid * .12 -> dark
            along > len * .22 && across < 0 -> light
            else -> base
        }
        p(px.toInt(), py.toInt(), col)
    }
}

/** Rings every opaque pixel with [c]: the black outline that makes a sprite read at any scale. */
fun Buf.outlineAll(c: Int) {
    val on = BooleanArray(w * h) { d[it] ushr 24 != 0 }
    for (y in 0 until h) for (x in 0 until w) {
        val i = y * w + x
        if (on[i]) continue
        if ((x > 0 && on[i - 1]) || (x < w - 1 && on[i + 1]) || (y > 0 && on[i - w]) || (y < h - 1 && on[i + w])) d[i] = c or 0xFF000000.toInt()
    }
}

fun Buf.erase(x: Int, y: Int, w: Int = 1, h: Int = 1) {
    for (j in 0 until h) for (i in 0 until w) if (x + i in 0 until this.w && y + j in 0 until this.h) d[(y + j) * this.w + x + i] = 0
}

/** Every opaque pixel as a pale shadow: an unknown species in the dex. */
fun Buf.silhouette(c: Int = rgb(0xA8B496)) {
    for (i in d.indices) if (d[i] ushr 24 != 0) d[i] = c or 0xFF000000.toInt()
}

/** Every opaque pixel in four browns: a pressed page, or a plant only seen somewhere. */
fun Buf.sepia() {
    val ramp = intArrayOf(rgb(0x483420), rgb(0x8C6A48), rgb(0xC0A070), rgb(0xE8D8B0))
    for (i in d.indices) {
        val c = d[i]
        if (c ushr 24 == 0) continue
        val l = (Buf.ch(c, 16) * .3 + Buf.ch(c, 8) * .59 + Buf.ch(c, 0) * .11) / 255
        d[i] = ramp[if (l < .3) 0 else if (l < .55) 1 else if (l < .75) 2 else 3]
    }
}

/** Draws [src] onto this buffer at (x, y), skipping clear pixels. */
fun Buf.blit(src: Buf, x: Int, y: Int) {
    for (j in 0 until src.h) for (i in 0 until src.w) {
        val c = src.d[j * src.w + i]
        if (c ushr 24 != 0) p(x + i, y + j, c)
    }
}

/** A quadratic curve as pixels; [each] is called with every new pixel and its t. */
fun Buf.curve(ax: Double, ay: Double, cx: Double, cy: Double, ex: Double, ey: Double, c: Int, each: ((Int, Int, Double) -> Unit)? = null) {
    var lx = Int.MIN_VALUE
    var ly = Int.MIN_VALUE
    var t = 0.0
    while (t <= 1.0001) {
        val (x, y) = quad(ax, ay, cx, cy, ex, ey, t)
        val xi = x.roundToInt()
        val yi = y.roundToInt()
        if (xi != lx || yi != ly) {
            p(xi, yi, c)
            each?.invoke(xi, yi, t)
            lx = xi; ly = yi
        }
        t += .02
    }
}
