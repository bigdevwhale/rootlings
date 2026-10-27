package app.rootlings.ui.art

import app.rootlings.domain.Hour
import app.rootlings.domain.SunPos
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** The outside as a window sees it this hour. */
data class Outside(val sun: SunPos, val hour: Hour, val azimuth: Int, val gain: Double)

/**
 * One window: the real sky in its panes, the sun where it really is (if this window can see it),
 * the light patch on the sill, and the spirits standing three to a row, the first row on the sill.
 */
class WindowScene(val out: Outside, val looks: List<Look>, val seed: Int) : Scene(120, 36 * ((looks.size + 2) / 3).coerceAtLeast(1) + 36) {
    private val rows = ((looks.size + 2) / 3).coerceAtLeast(1)
    override val key = listOf(out, looks, seed)
    private val sprites = looks.map { l -> (0 until 2).map { Spirit.draw(l, it * 2) } }

    fun board(row: Int) = 58 + row * 36

    /** Which spirit is at a point of the picture, in its pixels. */
    fun at(x: Double, y: Double): Int? {
        val col = (x / 40).toInt().coerceIn(0, 2)
        val row = ((y - 22) / 36).toInt().coerceIn(0, rows - 1)
        return (row * 3 + col).takeIf { it < looks.size }
    }

    private fun bands(): Array<IntArray> {
        val a = out.sun.alt
        if (a < -6) return arrayOf(intArrayOf(24, 32, 64), intArrayOf(32, 40, 80), intArrayOf(40, 48, 96))
        if (a < 2) return arrayOf(intArrayOf(64, 72, 120), intArrayOf(160, 104, 120), intArrayOf(224, 152, 104))
        val t = (out.hour.cloud / 100).toFloat()
        fun l(x: IntArray, y: IntArray) = IntArray(3) { (x[it] + (y[it] - x[it]) * t).roundToInt() }
        return arrayOf(l(intArrayOf(72, 136, 224), intArrayOf(128, 136, 152)), l(intArrayOf(112, 168, 240), intArrayOf(152, 160, 170)), l(intArrayOf(168, 208, 248), intArrayOf(184, 192, 192)))
    }

    private fun c(a: IntArray) = (0xFF shl 24) or (a[0] shl 16) or (a[1] shl 8) or a[2]

    override fun base(b: Buf) {
        // wallpaper
        b.r(0, 0, w, h, rgb(0xE8D8B0))
        for (y in 2 until h step 6) for (x in ((y / 6) % 2) * 3 until w step 6) b.p(x, y, rgb(0xD8C494))
        // frame and sky
        b.r(9, 1, 102, 58, INK); b.r(10, 2, 100, 56, rgb(0xF4F0E0))
        val bd = bands()
        val x0 = 13; val y0 = 5; val sw = 94; val sh = 50
        for (y in 0 until sh) {
            val f = y / sh.toDouble() * 2
            val i = min(1, f.toInt())
            val t = f - i
            for (x in 0 until sw) b.p(x0 + x, y0 + y, c(if (bay(x, y) / 16.0 < t) bd[i + 1] else bd[i]))
        }
        val night = out.sun.alt < -6
        if (night) {
            for ((x, y) in listOf(22 to 9, 40 to 14, 71 to 8, 95 to 12, 85 to 20, 30 to 22)) b.p(x, y, rgb(0xF8F0C0))
            b.oval(92.0, 12.0, 3.0, 3.0, rgb(0xF0E8C0)); b.oval(93.0, 11.0, 2.0, 2.0, c(bd[0]))
        }
        val rel = Math.toRadians(out.sun.az - out.azimuth)
        if (out.sun.alt > 0 && cos(rel) > .15 && out.hour.cloud < 85) {
            val sx = 60 + sin(rel) * 42
            val sy = 52 - min(out.sun.alt, 40.0) * 1.05
            b.oval(sx, sy, 4.0, 4.0, rgb(0xF8D860)); b.oval(sx, sy, 3.0, 3.0, rgb(0xFFF8C8))
        }
        if (out.sun.alt > -6) {
            val n = (out.hour.cloud / 16).roundToInt()
            val dusk = out.sun.alt < 2
            val cw = if (dusk) rgb(0xB0A0B8) else rgb(0xF8F8F8)
            val cs = if (dusk) rgb(0x806880) else rgb(0xC8D0D8)
            for (i in 0 until n) {
                val cx = x0 + Math.floorMod(i * 31 + out.azimuth / 3 + seed, sw)
                val cy = y0 + 4 + (i * 11) % 30
                val r = 5.0 + i % 3 * 2
                clip(b, x0, y0, sw, sh) { it.oval(cx.toDouble(), cy + 1.0, r, 2.0, cs); it.oval(cx - 1.0, cy.toDouble(), r - 1, 2.0, cw); it.oval(cx + r / 2, cy - 1.0, r / 2, 1.0, cw) }
            }
        }
        b.r(58, y0, 4, sh, rgb(0xF4F0E0)); b.r(x0, 28, sw, 3, rgb(0xF4F0E0)); b.r(61, y0, 1, sh, rgb(0xC8C0A8)); b.r(x0, 30, sw, 1, rgb(0xC8C0A8))
        if (out.sun.alt > 2) for ((a, y) in listOf(16 to 24, 64 to 24)) { b.line(a, y, a + 6, y - 6, rgb(0xFFFFFF), .5f); b.line(a + 3, y, a + 7, y - 4, rgb(0xFFFFFF), .4f) }
        // the sill and the shelves below
        for (row in 0 until rows) {
            val y = board(row)
            if (row == 0) { b.r(2, y, 116, 4, rgb(0xD8B890)); b.r(2, y, 116, 1, rgb(0xF0D8B0)); b.r(0, y + 4, 120, 5, rgb(0xA07850)); b.r(0, y + 8, 120, 1, rgb(0x7C5C3D)) }
            else { b.r(0, y, 120, 3, rgb(0xB08A60)); b.r(0, y, 120, 1, rgb(0xD0AC80)); b.r(0, y + 3, 120, 2, rgb(0x7C5C3D)); b.r(4, y + 5, 3, h - y - 5, rgb(0x8C6A48)); b.r(113, y + 5, 3, h - y - 5, rgb(0x8C6A48)) }
        }
        val beam = min(1.0, out.gain / 380)
        if (beam > .02) for (y in 58 until 62) for (x in 8 until 112) if (bay(x, y) / 16.0 < beam * .9) b.p(x, y, rgb(0xFFF0A8))
        if (beam > .3) for (k in 0 until 4) for (s in 0 until 40) { val x = 20 + k * 24 + s / 2; val y = 14 + s; if (bay(x, y) / 16.0 < beam * .35) b.p(x, y, rgb(0xFFF8D0)) }
    }

    private inline fun clip(b: Buf, x: Int, y: Int, w: Int, h: Int, draw: (Buf) -> Unit) {
        val t = Buf(b.w, b.h)
        draw(t)
        for (j in y until y + h) for (i in x until x + w) { val c = t.get(i, j); if (c ushr 24 != 0) b.p(i, j, c) }
    }

    override fun anim(b: Buf, t: Int) {
        // rain streaks or snow in the panes
        val p = out.hour.precip
        if (p > .05 && out.sun.alt > -8) {
            val snow = out.hour.temp < .5
            val n = if (p > 1.5) 26 else 14
            for (i in 0 until n) {
                val x = 13 + Math.floorMod(i * 37 + (if (snow) t / 3 else t) * (if (snow) 1 else 0), 94)
                val y = 5 + Math.floorMod(i * 23 + t * (if (snow) 1 else 3), 50)
                if (x in 58..61 || y in 28..30) continue
                if (snow) b.p(x, y, rgb(0xFFFFFF)) else { b.p(x, y, rgb(0xC8D8F0)); b.p(x - 1, y - 1, rgb(0xA8C0E0)) }
            }
        }
        looks.forEachIndexed { i, _ ->
            val row = i / 3
            val col = i % 3
            val bob = if (((t + i * 5) / 9) % 2 == 0) 0 else 1
            val s = sprites[i][(t / 5 + i) % 2]
            b.blit(s, 4 + col * 40, board(row) - 31 - bob)
        }
        if (out.sun.alt < -6) for (y in 0 until h) for (x in 0 until w) b.mul(x, y, rgb(0x7078B0), 1f)
        else if (out.sun.alt < 2) for (y in 0 until h) for (x in 0 until w) b.mul(x, y, rgb(0xD0A8A0), 1f)
    }
}

/** The battle ground: two grassy platforms on a pale dithered field. */
fun arena(): Buf {
    val b = Buf(120, 80)
    for (y in 0 until 80) for (x in 0 until 120) b.p(x, y, if (bay(x, y) / 16.0 < y / 80.0) rgb(0xE8F0C8) else rgb(0xF8F8E0))
    for ((cx, cy, rx) in listOf(Triple(88, 36, 26), Triple(32, 74, 28))) {
        b.oval(cx.toDouble(), cy.toDouble(), rx.toDouble(), 5.0, rgb(0x689048)); b.oval(cx.toDouble(), cy - 1.0, rx - 2.0, 4.0, rgb(0x98C068)); b.oval(cx - 4.0, cy - 2.0, rx - 10.0, 2.0, rgb(0xB0D880))
    }
    for ((x, y) in listOf(10 to 20, 50 to 10, 112 to 60, 70 to 62)) b.r(x, y, 2, 1, rgb(0xC8D8A8))
    return b
}

/** A cutting rooting: in a glass of water (roots grow with [progress]), or a little pot of soil. */
fun cuttingArt(look: Look, medium: String, progress: Double): Buf {
    val b = Buf(32, 40)
    val c = hsl(look.hue.toDouble(), look.sat.toDouble(), look.lit.toDouble())
    val cl = hsl(look.hue + 8.0, look.sat.toDouble(), look.lit + 13.0)
    val cd = hsl(look.hue.toDouble(), look.sat.toDouble(), look.lit - 14.0)
    if (medium == "WATER") {
        for (y in 14 until 39) { val k = if (y > 36) 1 else 0; b.r(8 + k, y, 16 - 2 * k, 1, if (y > 20) (if (bay(0, y) < 8) rgb(0x88B8C8) else rgb(0x6898B0)) else rgb(0xC8E0E0)) }
        b.r(8, 14, 1, 25, rgb(0xE8F8F8)); b.r(10, 22, 1, 12, rgb(0xB8E0E8))
        val roots = (progress * 6).toInt().coerceIn(0, 6)
        for (i in 0 until roots) { val x = 13 + i * 2; b.line(x, 28, x + (if (i % 2 == 1) 2 else -2), 30 + i * 2, rgb(0xF0E8D0)) }
        b.line(16, 28, 16, 10, cd)
    } else {
        b.r(9, 27, 14, 2, rgb(0xC06A40)); b.r(9, 27, 14, 1, rgb(0xE0905C))
        for (y in 29 until 39) { val k = (y - 29) / 4; b.r(10 + k, y, 12 - 2 * k, 1, rgb(0xB05A34)); b.r(20 - k, y, 2, 1, rgb(0x8C4426)) }
        b.r(10, 26, 12, 2, rgb(0x5A3A20))
        b.line(16, 26, 16, 12, cd)
        if (progress > .5) for (x in listOf(12, 20)) b.p(x, 25, cl)
    }
    for ((dx, dy, len) in listOf(Triple(-1.0, -1.0, 8.0), Triple(1.0, -1.0, 8.0), Triple(-1.0, -.4, 7.0), Triple(1.0, -.4, 7.0), Triple(-.3, -1.0, 9.0), Triple(.4, -1.0, 9.0))) {
        b.leaf(16.0, 12.0, Math.toDegrees(kotlin.math.atan2(dx, -dy)), len * .8, 2.6, cd, c, cl)
    }
    b.outlineAll(INK)
    return b
}

/** Small icons, 16×16. */
object Icons {
    private val k = rgb(0xF8F8E8)

    fun tab(name: String): Buf {
        val b = Buf(16, 16)
        when (name) {
            "window" -> { b.r(2, 1, 12, 11, k); b.r(3, 2, 10, 9, rgb(0x5890E0)); b.r(7, 2, 2, 9, k); b.r(3, 6, 10, 1, k); b.r(1, 13, 14, 2, rgb(0xD8B890)) }
            "dex" -> { b.r(2, 2, 12, 12, rgb(0xC83828)); b.r(3, 3, 4, 4, rgb(0x5898E8)); b.r(9, 4, 4, 1, k); b.r(3, 9, 10, 4, rgb(0xB8D0A0)) }
            "egg" -> { b.oval(8.0, 9.0, 4.0, 5.0, k); b.oval(8.0, 9.0, 3.0, 4.0, rgb(0xF0E8C8)); b.p(6, 7, rgb(0x88C050)); b.p(10, 10, rgb(0x88C050)); b.p(7, 12, rgb(0x88C050)) }
            "bug" -> { b.oval(8.0, 10.0, 4.0, 3.0, rgb(0xD04838)); b.oval(8.0, 5.0, 2.0, 2.0, rgb(0xD04838)); for (s in listOf(-1, 1)) for (i in 0 until 3) b.p(8 + s * 6, 8 + i * 2, rgb(0xD04838)); b.p(7, 5, k); b.p(9, 5, k) }
            "gear" -> { b.oval(8.0, 8.0, 5.0, 5.0, k); b.oval(8.0, 8.0, 2.0, 2.0, rgb(0xC83828)); for ((x, y) in listOf(8 to 1, 8 to 14, 1 to 8, 14 to 8, 3 to 3, 12 to 3, 3 to 12, 12 to 12)) b.r(x, y, 2, 2, k) }
        }
        b.outlineAll(INK)
        return b
    }

    /** The catching ball: a red-and-cream seed. */
    fun seedball(): Buf {
        val b = Buf(20, 20)
        b.oval(10.0, 10.0, 8.0, 8.0, rgb(0xF8F8E8))
        for (y in 2 until 10) for (x in 0 until 20) if ((x - 10) * (x - 10) + (y - 10) * (y - 10) <= 72) b.p(x, y, rgb(0xC83828))
        b.r(2, 9, 17, 2, INK); b.oval(10.0, 10.0, 3.0, 3.0, INK); b.oval(10.0, 10.0, 2.0, 2.0, rgb(0xF8F8E8))
        b.p(6, 5, rgb(0xF08070)); b.p(7, 4, rgb(0xF08070))
        b.outlineAll(INK)
        return b
    }
}
