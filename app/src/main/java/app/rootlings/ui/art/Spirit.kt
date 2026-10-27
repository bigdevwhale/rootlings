package app.rootlings.ui.art

import app.rootlings.data.Kind
import app.rootlings.data.Mood
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** How one spirit looks right now. [pot] picks the pot: terracotta, cream, slate or mustard. */
data class Look(
    val kind: Kind,
    val hue: Int,
    val sat: Int,
    val lit: Int,
    val stage: Int,
    val mood: Mood = Mood.CALM,
    val flower: Int? = null,
    val pot: Int = 0,
)

private class Pal(l: Look) {
    val s = if (l.mood == Mood.GLOOM || l.mood == Mood.REST) l.sat * .45 else l.sat.toDouble()
    val d = hsl(l.hue.toDouble(), s, l.lit - 14.0)
    val b = hsl(l.hue.toDouble(), s, l.lit.toDouble())
    val l2 = hsl(l.hue + 8.0, s, l.lit + 13.0)
    val pale = hsl(l.hue.toDouble(), s * .6, l.lit + 26.0)
    val fl = l.flower
    val flD = l.flower?.let { mix(it, INK, .35f) }
    val bark = rgb(0x8C6038)
    val barkD = rgb(0x5E3E22)
}

/**
 * Every spirit is a 32×32 sprite: the plant's own shape growing from a little body with a face,
 * sitting in a pot. Three forms per kind; flowers when it's in bloom; the face and a small effect
 * follow its mood. The whole thing is ringed with ink, the handheld-game way.
 */
object Spirit {
    const val W = 32

    fun draw(l: Look, t: Int = 0): Buf {
        val b = Buf(W, W)
        val c = Pal(l)
        val st = l.stage.coerceIn(1, 3)
        back(b, l.kind, c, st)
        val ey = body(b, l.kind, c)
        front(b, l.kind, c, st)
        face(b, l.mood, ey)
        pot(b, l.pot)
        b.outlineAll(INK)
        fx(b, l.mood, t)
        return b
    }

    // ---------------------------------------------------------------- the body

    /** Draws the body and returns the eye row. Cacti are their own body. */
    private fun body(b: Buf, k: Kind, c: Pal): Int = when (k) {
        Kind.PADDLE -> { b.oval(16.0, 17.0, 6.0, 8.0, c.d); b.oval(15.0, 16.0, 5.0, 7.0, c.b); b.r(12, 12, 1, 3, c.l2); b.p(13, 11, c.l2); 16 }
        Kind.COLUMN -> {
            b.oval(16.0, 15.0, 5.0, 10.0, c.d); b.oval(15.0, 14.0, 4.0, 9.0, c.b)
            for (y in 7..22) { b.p(13, y, c.d); b.p(18, y, c.d) }
            b.r(12, 9, 1, 4, c.l2); 15
        }
        Kind.BALL -> {
            b.oval(16.0, 18.0, 7.0, 6.0, c.d); b.oval(15.0, 17.0, 6.0, 5.0, c.b)
            for (a in listOf(-50, -20, 20, 50)) { val x = 16 + (sin(Math.toRadians(a.toDouble())) * 5).roundToInt(); b.line(x, 13, x + (if (a < 0) -1 else 1), 21, c.d) }
            b.r(11, 15, 2, 1, c.l2); 18
        }
        Kind.ROSETTE -> { b.oval(16.0, 20.0, 7.0, 5.0, c.b); b.oval(15.0, 19.0, 6.0, 4.0, c.l2); b.r(11, 17, 2, 1, c.pale); 19 }
        else -> { b.oval(16.0, 20.0, 7.0, 5.0, c.d); b.oval(15.0, 19.0, 6.0, 4.0, c.b); b.r(11, 17, 2, 1, c.l2); b.p(11, 18, c.l2); 19 }
    }

    // ---------------------------------------------------------------- what grows behind it

    private fun back(b: Buf, k: Kind, c: Pal, st: Int) {
        when (k) {
            Kind.MONSTERA -> {
                if (st == 1) { b.line(17, 15, 19, 11, c.d); b.leaf(19.0, 12.0, 20.0, 7.0, 6.0, c.d, c.b, c.l2) }
                else {
                    b.line(17, 15, 21, 10, c.d); b.line(18, 15, 22, 10, c.d)
                    monsteraLeaf(b, 21.0, 11.0, 22.0, 11.0, 11.0, c)
                    if (st == 3) { b.line(14, 15, 10, 10, c.d); monsteraLeaf(b, 11.0, 11.0, -38.0, 10.0, 10.0, c) }
                }
            }
            Kind.PHILO -> {
                val n = st + 1
                vine(b, 10.0, 23.0, 4.0, 22.0 + st * 3, c, n, left = true)
                if (st > 1) vine(b, 22.0, 23.0, 28.0, 21.0 + st * 3, c, n, left = false)
                b.line(17, 15, 19, 12, c.d); b.leaf(19.0, 12.0, 30.0, 6.0, 5.0, c.d, c.b, c.l2)
                if (st == 3) { b.line(15, 15, 12, 11, c.d); b.leaf(12.0, 11.0, -30.0, 6.0, 5.0, c.d, c.b, c.l2) }
            }
            Kind.FICUS -> {
                b.r(16, 7, 1, 9, c.d)
                if (st > 1) b.leaf(15.0, 13.0, -55.0, 9.0, 5.0, c.d, c.b, c.l2, c.l2)
                if (st > 2) b.leaf(17.0, 12.0, 55.0, 9.0, 5.0, c.d, c.b, c.l2, c.l2)
                b.leaf(16.0, 11.0, 0.0, 10.0, if (st == 1) 5.0 else 7.0, c.d, c.b, c.l2, c.l2)
            }
            Kind.TREE -> {
                val top = intArrayOf(10, 8, 6)[st - 1]
                b.line(16, 16, 16, top + 2, c.bark); b.line(17, 16, 17, top + 3, c.barkD)
                if (st == 3) b.line(16, 11, 12, 8, c.bark)
                val rx = doubleArrayOf(4.0, 6.0, 8.0)[st - 1]
                val ry = doubleArrayOf(3.0, 4.0, 5.0)[st - 1]
                val cy = top.toDouble()
                for (j in -ry.toInt() - 1..ry.toInt() + 1) for (i in -rx.toInt() - 1..rx.toInt() + 1) {
                    val e = (i / rx).let { it * it } + (j / ry).let { it * it }
                    if (e > 1.05 || hash(i + 40, j + 40) < .12) continue
                    val col = if (i + j < -2) c.l2 else if (i + j > 2) c.d else c.b
                    b.p(16 + i, cy.toInt() + j, col)
                }
                if (c.fl != null) for (k2 in 0 until 4 + st * 2) { val a = hash(k2, 9) * 6.28; b.p((16 + cos(a) * rx * .8).roundToInt(), (cy + sin(a) * ry * .8).roundToInt(), c.fl) }
            }
            Kind.GRASS -> {
                val n = intArrayOf(4, 6, 8)[st - 1]
                for (i in 0 until n) {
                    val a = Math.toRadians(-75 + 150.0 * i / (n - 1))
                    val len = 9.0 + (if (i % 2 == 1) 3 else 0) + st
                    b.curve(16.0, 15.0, 16 + sin(a) * len * .35, 15 - len, 16 + sin(a) * len * 1.3, 15 - cos(a) * len * .8, if (i % 2 == 1) c.b else c.l2)
                }
            }
            Kind.ROSETTE -> {
                if (c.fl != null || st == 3) {
                    b.line(21, 14, 25, 4, mix(c.d, c.bark, .5f))
                    val f = c.fl ?: rgb(0xF08850)
                    for ((x, y) in listOf(25 to 3, 26 to 4, 24 to 5, 27 to 6)) b.p(x, y, f)
                }
                val n = 6 + st * 2
                for (k2 in 0 until n) {
                    val a = Math.toRadians(k2 * 360.0 / n)
                    val x = 16 + cos(a) * (5 + st)
                    val y = 18 + sin(a) * (3.5 + st * .6)
                    b.oval(x, y, 2.0, 2.0, c.d); b.p(x.roundToInt(), y.roundToInt() - 1, c.l2)
                }
            }
            Kind.ALOE -> {
                val angles = when (st) { 1 -> listOf(-30, 0, 30); 2 -> listOf(-50, -22, 0, 22, 50); else -> listOf(-65, -42, -20, 0, 20, 42, 65) }
                for (a in angles) {
                    val len = 9.0 + st * 1.5 - kotlin.math.abs(a) / 25.0
                    b.leaf(16.0, 18.0, a.toDouble(), len, 3.4, c.d, c.b, c.l2)
                    val rad = Math.toRadians(a.toDouble())
                    for (s in 3 until len.toInt() step 3) b.p((16 + sin(rad) * s - cos(rad)).roundToInt(), (18 - cos(rad) * s).roundToInt(), c.pale)
                }
                if (c.fl != null && st > 1) { b.line(19, 12, 23, 2, c.d); for (y in 2..5) b.p(23 + (y % 2), y, c.fl) }
            }
            Kind.PADDLE -> if (st > 1) {
                b.oval(9.0, 9.0, 3.0, 4.0, c.d); b.oval(9.0, 8.0, 2.0, 3.0, c.b)
                b.oval(23.0, 8.0, 3.0, 4.0, c.d); b.oval(23.0, 7.0, 2.0, 3.0, c.b)
            }
            Kind.COLUMN -> {
                if (st > 1) { b.r(19, 15, 4, 2, c.d); b.oval(22.0, 11.0, 2.0, 4.0, c.d); b.oval(22.0, 10.0, 1.0, 3.0, c.b) }
                if (st > 2) { b.r(10, 17, 3, 2, c.d); b.oval(10.0, 13.0, 2.0, 4.0, c.d); b.oval(10.0, 12.0, 1.0, 3.0, c.b) }
            }
            Kind.BALL -> Unit
            Kind.FERN -> {
                val ends = when (st) {
                    1 -> listOf(11 to 5, 22 to 5)
                    2 -> listOf(4 to 13, 10 to 3, 22 to 3, 28 to 13)
                    else -> listOf(2 to 21, 5 to 9, 13 to 2, 20 to 2, 27 to 9, 30 to 21)
                }
                for ((ex, ey) in ends) {
                    var k2 = 0
                    val sx = if (ex < 16) -1 else 1
                    b.curve(16.0, 15.0, (16 + ex) / 2.0, minOf(ey, 15) - 6.0, ex.toDouble(), ey.toDouble(), c.d) { x, y, t ->
                        if (t < .12 || (k2++) % 2 == 1) return@curve
                        b.p(x, y - 1, c.b); b.p(x + sx, y + 1, c.b)
                        if (t < .6) { b.p(x - sx, y - 1, c.l2); b.p(x + sx * 2, y + 1, c.l2) }
                    }
                }
            }
            Kind.PATTERN -> {
                fun leaf(cx: Int, cy: Int) {
                    b.oval(cx.toDouble(), cy.toDouble(), 3.0, 5.0, c.d); b.oval(cx.toDouble(), cy - 1.0, 2.0, 4.0, c.b)
                    for (y in cy - 4..cy + 3 step 2) b.r(cx - 1, y, 3, 1, c.pale)
                    b.p(cx, cy + 5, rgb(0x7A4868))
                }
                if (st > 1) leaf(16, 6)
                if (st > 2) { leaf(6, 13); leaf(26, 13) }
                leaf(10, 10); leaf(22, 10)
            }
            Kind.BLADE -> {
                val xs = when (st) { 1 -> listOf(14, 18); 2 -> listOf(11, 15, 18, 21); else -> listOf(9, 12, 15, 18, 21, 23) }
                xs.forEachIndexed { i, x ->
                    val len = 11.0 + st * 2 + (if (i % 2 == 0) 2 else -1)
                    val tilt = (x - 16) * 1.6
                    b.leaf(x.toDouble(), 17.0, tilt, len, 4.0, c.d, c.b, c.l2)
                    val rad = Math.toRadians(tilt)
                    for (s in 2 until len.toInt() - 1 step 3) b.p((x + sin(rad) * s).roundToInt(), (17 - cos(rad) * s).roundToInt(), c.pale)
                }
            }
            Kind.ZZ -> {
                val stalks = when (st) { 1 -> listOf(-14 to -12); 2 -> listOf(-14 to -12, 14 to -12); else -> listOf(-16 to -10, 0 to -14, 16 to -10) }
                for ((dx, dy) in stalks) {
                    var k2 = 0
                    val ex = 16.0 + dx * .55
                    val ey = 15.0 + dy
                    b.curve(16.0, 16.0, 16 + dx * .1, 10.0, ex, ey, c.d) { x, y, t ->
                        if (t < .2 || (k2++) % 3 != 0) return@curve
                        b.oval(x - 2.0, y.toDouble(), 1.0, 1.0, c.b); b.oval(x + 2.0, y.toDouble(), 1.0, 1.0, c.b)
                        b.p(x - 2, y - 1, c.l2); b.p(x + 2, y - 1, c.l2)
                    }
                    b.oval(ex, ey - 1, 1.0, 1.0, c.b)
                }
            }
            Kind.PALM -> {
                b.line(16, 15, 16, 11, c.bark)
                val ends = when (st) { 1 -> listOf(8 to 8, 24 to 8); 2 -> listOf(4 to 14, 9 to 4, 23 to 4, 28 to 14); else -> listOf(1 to 18, 4 to 8, 12 to 1, 20 to 1, 28 to 8, 31 to 18) }
                for ((ex, ey) in ends) {
                    var k2 = 0
                    b.curve(16.0, 11.0, (16 + ex) / 2.0, minOf(ey, 11) - 5.0, ex.toDouble(), ey.toDouble(), c.d) { x, y, t ->
                        if (t < .15 || (k2++) % 2 == 1) return@curve
                        b.p(x, y + 1, c.b); b.p(x, y + 2, if (t < .5) c.l2 else c.b)
                    }
                }
            }
            Kind.CANE -> {
                val top = intArrayOf(12, 8, 4)[st - 1]
                for (y in top..16) { b.p(15, y, c.bark); b.p(16, y, c.bark); b.p(17, y, c.barkD); if (y % 3 == 0) b.p(16, y, c.barkD) }
                val n = 5 + st * 2
                for (i in 0 until n) {
                    val a = -80 + 160.0 * i / (n - 1)
                    b.leaf(16.0, top.toDouble(), a, 7.0 + st, 2.4, c.d, c.b, c.l2)
                }
            }
            Kind.ORCHID -> {
                b.leaf(13.0, 21.0, -100.0, 10.0, 5.0, c.d, c.b, c.l2)
                b.leaf(19.0, 21.0, 100.0, 10.0, 5.0, c.d, c.b, c.l2)
                if (st > 2) b.leaf(14.0, 19.0, -70.0, 8.0, 4.0, c.d, c.b, c.l2)
                if (st > 1 || c.fl != null) {
                    b.curve(18.0, 15.0, 19.0, 2.0, 26.0, 4.0, mix(c.d, c.bark, .5f))
                    val f = c.fl
                    if (f != null) for ((x, y) in listOf(26 to 5, 23 to 3, 20 to 4)) { b.oval(x.toDouble(), y.toDouble(), 2.0, 2.0, f); b.p(x, y, c.flD!!) }
                    else b.p(26, 4, c.l2)
                }
            }
            Kind.SPATHE -> {
                b.leaf(13.0, 16.0, -40.0, 11.0, 6.0, c.d, c.b, c.l2, c.l2)
                b.leaf(19.0, 16.0, 40.0, 11.0, 6.0, c.d, c.b, c.l2, c.l2)
                if (st > 2) b.leaf(16.0, 14.0, -8.0, 12.0, 6.0, c.d, c.b, c.l2, c.l2)
                if (st > 1 || c.fl != null) {
                    val f = c.fl ?: rgb(0xF4F4E8)
                    b.line(18, 15, 22, 6, c.d)
                    b.leaf(22.0, 7.0, 20.0, 7.0, 5.0, mix(f, INK, .25f), f, mix(f, rgb(0xFFFFFF), .4f))
                    b.line(22, 6, 23, 3, rgb(0xE8C048))
                }
            }
            Kind.VIOLET -> {
                for (a in listOf(-105, -70, 70, 105) + if (st > 1) listOf(-40, 40) else emptyList()) b.leaf(16.0, 21.0, a.toDouble(), 7.0 + st, 5.0, c.d, c.b, c.l2)
            }
            Kind.BUSH -> {
                val n = intArrayOf(5, 8, 11)[st - 1]
                for (i in 0 until n) {
                    val a = -95 + 190.0 * i / (n - 1) + (hash(i, 3) - .5) * 20
                    b.leaf(16.0, 16.0, a, 7.0 + hash(i, 5) * 3 + st, 4.6, c.d, c.b, c.l2, c.pale)
                }
                if (c.fl != null && st > 1) for (i in 0 until 3 + st) { val x = 9 + (hash(i, 21) * 14).toInt(); val y = 4 + (hash(i, 22) * 6).toInt(); b.r(x, y, 2, 2, c.fl); b.p(x, y, c.flD!!) }
            }
            Kind.COIN -> {
                val ends = when (st) { 1 -> listOf(10 to 8, 22 to 8, 16 to 5); 2 -> listOf(7 to 10, 12 to 5, 20 to 5, 25 to 10, 16 to 3); else -> listOf(4 to 13, 8 to 6, 13 to 2, 19 to 2, 24 to 6, 28 to 13, 16 to 7) }
                for ((x, y) in ends) { b.line(16, 16, x, y + 2, c.d); b.oval(x.toDouble(), y.toDouble(), 2.6, 2.4, c.b); b.p(x - 1, y - 1, c.l2); b.p(x, y, c.d) }
            }
            Kind.TRAIL -> {
                vine(b, 10.0, 23.0, 5.0, 23.0 + st * 3, c, 0, left = true, beads = true)
                vine(b, 22.0, 23.0, 27.0, 22.0 + st * 3, c, 0, left = false, beads = true)
                if (st > 2) vine(b, 12.0, 24.0, 9.0, 31.0, c, 0, left = true, beads = true)
                for (a in listOf(-40, 0, 40)) b.leaf(16.0, 15.0, a.toDouble(), 5.0, 3.0, c.d, c.b, c.l2)
                if (c.fl != null && st > 1) for ((x, y) in listOf(5 to 27, 27 to 26)) { b.oval(x.toDouble(), y.toDouble(), 1.0, 1.0, c.fl) }
            }
            Kind.HERB -> {
                val xs = when (st) { 1 -> listOf(14, 18); 2 -> listOf(12, 16, 20); else -> listOf(10, 13, 16, 19, 22) }
                xs.forEachIndexed { i, x ->
                    val top = 6 + (hash(i, 31) * 4).toInt() - st
                    b.line(x, 16, x, top, c.d)
                    for (y in top + 1..14 step 3) { b.leaf(x.toDouble(), y.toDouble(), -60.0, 4.0, 2.4, c.d, c.b, c.l2); b.leaf(x.toDouble(), y + 1.0, 60.0, 4.0, 2.4, c.d, c.b, c.l2) }
                    if (c.fl != null) { b.p(x, top - 1, c.fl); b.p(x, top - 2, c.fl) }
                }
            }
            Kind.BULB -> {
                for (a in listOf(-45, -18, 18, 45).take(if (st == 1) 2 else 4)) b.leaf(16.0, 17.0, a.toDouble(), 10.0 + st * 2, 3.0, c.d, c.b, c.l2)
                if (st == 3 || c.fl != null) {
                    b.line(16, 15, 16, 4, c.d); b.line(17, 15, 17, 5, c.b)
                    val f = c.fl ?: rgb(0xD84040)
                    for (a in listOf(-60, 0, 60)) b.leaf(16.5, 4.0, a.toDouble(), 5.0, 4.0, c.flD ?: mix(f, INK, .35f), f, mix(f, rgb(0xFFFFFF), .4f))
                }
            }
            Kind.CARNIVORE -> {
                val traps = when (st) { 1 -> listOf(10 to 10, 22 to 10); 2 -> listOf(7 to 12, 13 to 6, 20 to 6, 25 to 12); else -> listOf(5 to 15, 9 to 7, 16 to 3, 23 to 7, 27 to 15) }
                for ((x, y) in traps) {
                    b.line(16, 16, x, y + 3, c.d)
                    b.oval(x.toDouble(), y.toDouble(), 3.0, 2.0, c.b)
                    b.r(x - 2, y, 5, 1, rgb(0xC83838))
                    for (i in -2..2 step 2) { b.p(x + i, y - 3, c.l2); b.p(x + i, y + 3, c.l2) }
                }
            }
        }
    }

    private fun front(b: Buf, k: Kind, c: Pal, st: Int) {
        when (k) {
            Kind.PADDLE -> {
                for ((x, y) in listOf(12 to 12, 19 to 11, 14 to 16, 18 to 18, 11 to 19, 20 to 20, 16 to 13, 9 to 7, 23 to 6)) b.p(x, y, rgb(0xF8F0B0))
                if (st == 3 || c.fl != null) { b.oval(16.0, 9.0, 2.0, 1.0, c.fl ?: rgb(0xF0C040)); b.p(16, 9, rgb(0xC07020)) }
            }
            Kind.COLUMN -> {
                for (y in 8..22 step 3) { b.p(13, y, rgb(0xF8F0B0)); b.p(18, y + 1, rgb(0xF8F0B0)) }
                if (st == 3 || c.fl != null) { b.oval(16.0, 5.0, 2.0, 1.0, c.fl ?: rgb(0xF0A0C0)) }
            }
            Kind.BALL -> {
                for ((x, y) in listOf(11 to 15, 14 to 13, 18 to 13, 21 to 15, 10 to 19, 22 to 19, 16 to 12)) b.p(x, y, rgb(0xF8F0B0))
                if (st == 3 || c.fl != null) { val f = c.fl ?: rgb(0xF07090); for (x in 13..19 step 2) b.r(x, 11, 2, 1, f); b.p(16, 11, rgb(0xF8E070)) }
            }
            Kind.VIOLET -> if (st > 1 || c.fl != null) {
                val f = c.fl ?: rgb(0x9060C8)
                for ((x, y) in listOf(13 to 12, 16 to 11, 19 to 12, 15 to 13, 18 to 13).take(2 + st)) { b.r(x - 1, y - 1, 2, 2, f); b.p(x, y, rgb(0xF8E070)) }
            }
            else -> Unit
        }
    }

    // ---------------------------------------------------------------- helpers

    private fun monsteraLeaf(b: Buf, x: Double, y: Double, deg: Double, len: Double, wid: Double, c: Pal) {
        b.leaf(x, y, deg, len, wid, c.d, c.b, c.l2, c.l2)
        val a = Math.toRadians(deg)
        val ux = sin(a); val uy = -cos(a)
        for (t in listOf(.3, .55, .78)) for (side in listOf(-1, 1)) {
            val mx = x + ux * len * t
            val my = y + uy * len * t
            for (r in listOf(wid * .32, wid * .42, wid * .5)) b.erase((mx + -uy * side * r).roundToInt(), (my + ux * side * r).roundToInt())
        }
    }

    private fun vine(b: Buf, x0: Double, y0: Double, x1: Double, y1: Double, c: Pal, leaves: Int, left: Boolean, beads: Boolean = false) {
        var k2 = 0
        b.curve(x0, y0, x1 + (if (left) 1 else -1), y0, x1, y1, c.d) { x, y, t ->
            if (t < .1) return@curve
            if (beads) { if ((k2++) % 2 == 0) { b.p(x + (if (left) -1 else 1), y, c.b); b.p(x, y + 1, c.l2) } }
            else if ((k2++) % 3 == 0) b.leaf(x.toDouble(), y.toDouble(), if (left) -120.0 else 120.0, 4.0, 3.4, c.d, c.b, c.l2)
        }
        if (leaves > 0) b.leaf(x1, y1, 180.0, 4.0, 3.4, c.d, c.b, c.l2)
    }

    private fun face(b: Buf, m: Mood, ey: Int) {
        val pink = rgb(0xF08878)
        if (m != Mood.GLOOM) { b.p(11, ey + 1, pink); b.p(20, ey + 1, pink) }
        if (m == Mood.SCORCH) { b.r(10, ey + 1, 2, 1, rgb(0xE05040)); b.r(20, ey + 1, 2, 1, rgb(0xE05040)) }
        when (m) {
            Mood.SUN, Mood.EVOLVE, Mood.HATCH -> { b.p(12, ey, INK); b.p(13, ey - 1, INK); b.p(14, ey, INK); b.p(17, ey, INK); b.p(18, ey - 1, INK); b.p(19, ey, INK) }
            Mood.NIGHT, Mood.REST -> { b.r(12, ey, 3, 1, INK); b.r(17, ey, 3, 1, INK) }
            Mood.GLOOM -> { b.p(13, ey, INK); b.p(18, ey, INK); b.p(12, ey - 1, INK); b.p(19, ey - 1, INK) }
            Mood.PEST, Mood.SCORCH -> { b.p(12, ey - 1, INK); b.p(13, ey, INK); b.p(12, ey + 1, INK); b.p(19, ey - 1, INK); b.p(18, ey, INK); b.p(19, ey + 1, INK) }
            Mood.NEW -> { b.r(13, ey - 1, 1, 2, INK); b.r(18, ey - 1, 1, 2, INK); b.p(12, ey - 1, rgb(0xFFFFFF)); b.p(17, ey - 1, rgb(0xFFFFFF)) }
            else -> { b.r(13, ey - 1, 1, 2, INK); b.r(18, ey - 1, 1, 2, INK) }
        }
        val my = ey + 2
        when (m) {
            Mood.SUN, Mood.EVOLVE, Mood.HATCH, Mood.NEW -> { b.r(15, my + 1, 2, 1, INK); b.p(14, my, INK); b.p(17, my, INK) }
            Mood.GLOOM -> { b.r(15, my, 2, 1, INK); b.p(14, my + 1, INK); b.p(17, my + 1, INK) }
            Mood.PEST -> { b.p(14, my, INK); b.p(15, my + 1, INK); b.p(16, my, INK); b.p(17, my + 1, INK) }
            Mood.DRY, Mood.SCORCH, Mood.HEAT -> b.r(15, my, 2, 2, INK)
            Mood.SOGGY -> { b.p(14, my + 1, INK); b.r(15, my, 2, 1, INK); b.p(17, my + 1, INK) }
            else -> b.r(15, my, 2, 1, INK)
        }
    }

    private val POTS = arrayOf(
        intArrayOf(rgb(0xC06A40).toInt(), rgb(0xE0905C), rgb(0xB05A34), rgb(0xC87048), rgb(0x8C4426)),
        intArrayOf(rgb(0xE8E0CC), rgb(0xFFFFF4), rgb(0xD8D0BC), rgb(0xF0E8D8), rgb(0xA8A090)),
        intArrayOf(rgb(0x5878A0), rgb(0x80A0C8), rgb(0x48688C), rgb(0x6888B0), rgb(0x30486C)),
        intArrayOf(rgb(0xD8A030), rgb(0xF0C860), rgb(0xC08C28), rgb(0xE0AC40), rgb(0x8C6018)),
    )

    private fun pot(b: Buf, style: Int) {
        val p = POTS[Math.floorMod(style, POTS.size)]
        b.r(9, 23, 14, 2, p[0]); b.r(9, 23, 14, 1, p[1])
        for (y in 25 until 32) {
            val k = (y - 25) / 3
            val x = 10 + k
            val w = 12 - 2 * k
            b.r(x, y, w, 1, p[2]); b.p(x, y, p[3]); b.r(x + w - 2, y, 2, 1, p[4])
        }
    }

    private fun fx(b: Buf, m: Mood, t: Int) {
        val blink = t % 4 < 2
        when (m) {
            Mood.SUN -> { val y = rgb(0xF8D030); for ((x, yy) in listOf(3 to 4, 2 to 5, 4 to 5, 3 to 6)) b.p(x, yy, y); b.p(3, 5, rgb(0xFFF8C0)); if (blink) b.p(7, 1, y) }
            Mood.NIGHT, Mood.REST -> {
                val z = rgb(0x405878)
                fun zz(x: Int, y: Int) { b.r(x, y, 3, 1, z); b.p(x + 1, y + 1, z); b.r(x, y + 2, 3, 1, z) }
                zz(25, 9 - (t / 3) % 2); zz(28, 4 - (t / 3) % 2)
                if (m == Mood.REST) { val s = rgb(0xE8F4FF); for ((x, y) in listOf(4 to 6, 3 to 7, 5 to 7, 4 to 8, 4 to 7)) b.p(x, y, s) }
            }
            Mood.GLOOM -> { b.oval(26.0, 4.0, 3.0, 1.0, rgb(0xA0A8A8)); b.oval(24.0, 5.0, 2.0, 1.0, rgb(0xB8C0C0)); for ((x, y) in listOf(24 to 8, 27 to 9, 25 to 11)) b.p(x, if (blink) y else y + 1, rgb(0x6888B0)) }
            Mood.DRY -> for ((x, y) in listOf(4 to 20, 27 to 18, 6 to 14, 26 to 12)) b.p(x, y, rgb(0xB08850))
            Mood.SOGGY -> { val w = rgb(0x5890D0); b.p(23, 15, w); b.p(23, 16, w); b.r(6, 31, 3, 1, w); b.r(24, 31, 3, 1, w) }
            Mood.HEAT -> { val w = rgb(0x5890D0); b.p(22, 15, w); b.p(22, 16, w); b.p(23, 17, w); for (x in 3..7) b.p(x, 4 + (x + t) % 2, rgb(0xE8A050)) }
            Mood.SCORCH -> for ((x, y) in listOf(2 to 2, 4 to 4, 6 to 2, 27 to 2, 29 to 4)) b.p(x, y, rgb(0xF06030))
            Mood.PEST -> { b.r(25, 10, 2, 2, rgb(0xD03830)); for ((x, y) in listOf(24 to 9, 27 to 9, 24 to 12, 27 to 12)) b.p(x, y, INK); b.line(20, 3, 29, 7, rgb(0xFFFFFF)); b.line(22, 2, 23, 8, rgb(0xFFFFFF)) }
            Mood.NEW, Mood.EVOLVE, Mood.HATCH -> {
                val s = rgb(0xF8E870)
                for ((x, y) in if (blink) listOf(3 to 5, 27 to 3, 29 to 14) else listOf(5 to 2, 26 to 7, 2 to 13)) { b.p(x, y, s); b.p(x - 1, y, s); b.p(x + 1, y, s); b.p(x, y - 1, s); b.p(x, y + 1, s) }
            }
            else -> Unit
        }
    }
}
