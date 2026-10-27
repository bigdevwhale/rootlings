package app.rootlings.ui.art

import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** The battle enemies, 32×32, one per pest sprite name. */
object Foes {
    fun draw(sprite: String): Buf {
        val b = Buf(32, 32)
        when (sprite) {
            "mite" -> mite(b)
            "scale" -> scale(b)
            "thrips" -> thrips(b)
            "mealy" -> mealy(b)
            "gnat" -> gnat(b)
            "aphid" -> aphid(b)
            "whitefly" -> whitefly(b)
            "rot" -> rot(b)
            "burn" -> burn(b)
            "mildew" -> mildew(b)
            "dark" -> dark(b)
            "springtail" -> springtail(b)
            else -> mite(b)
        }
        b.outlineAll(INK)
        return b
    }

    private fun eyes(b: Buf, x1: Int, x2: Int, y: Int, angry: Boolean = true) {
        val w = rgb(0xFFFFFF)
        b.r(x1, y, 2, 2, w); b.r(x2, y, 2, 2, w); b.p(x1 + 1, y + 1, INK); b.p(x2, y + 1, INK)
        if (angry) { b.p(x1 - 1, y - 1, INK); b.p(x1, y - 1, INK); b.p(x2 + 1, y - 1, INK); b.p(x2 + 2, y - 1, INK) }
    }

    private fun grin(b: Buf, x: Int, y: Int, w: Int = 4) {
        b.r(x, y, w, 1, INK); b.p(x - 1, y - 1, INK); b.p(x + w, y - 1, INK)
        b.p(x + 1, y, rgb(0xFFFFFF))
    }

    private fun mite(b: Buf) {
        val leg = rgb(0x7A2A1E)
        for (s in listOf(-1, 1)) for (i in 0 until 4) { val y = 17 + i * 2; b.line(16 + s * 5, y, 16 + s * 10, y - 2 + i, leg); b.p(16 + s * 11, y - 1 + i, leg) }
        b.oval(16.0, 20.0, 7.0, 5.0, rgb(0xA83228)); b.oval(15.0, 19.0, 6.0, 4.0, rgb(0xD04838)); b.r(12, 17, 2, 1, rgb(0xF08070))
        b.oval(16.0, 12.0, 4.0, 3.0, rgb(0xA83228)); b.oval(16.0, 11.0, 3.0, 2.0, rgb(0xC04030))
        b.line(13, 9, 11, 5, leg); b.line(19, 9, 21, 5, leg)
        eyes(b, 13, 17, 10); grin(b, 14, 14)
        for ((x, y) in listOf(13 to 21, 18 to 19, 16 to 23)) b.p(x, y, rgb(0x8A2A20))
    }

    private fun scale(b: Buf) {
        for ((cx, cy, r) in listOf(Triple(9, 22, 4), Triple(23, 21, 4), Triple(16, 17, 7))) {
            b.oval(cx.toDouble(), cy.toDouble(), r.toDouble(), r * .75, rgb(0x6A4424)); b.oval(cx - 1.0, cy - 1.0, r - 1.0, r * .75 - 1, rgb(0x9A6A3A))
            b.p(cx - r / 2, cy - r / 2, rgb(0xC8985A))
        }
        eyes(b, 12, 18, 15); grin(b, 14, 20)
    }

    private fun thrips(b: Buf) {
        val body = rgb(0x3A3020)
        b.leaf(16.0, 27.0, 0.0, 22.0, 6.0, rgb(0x2A2014), body, rgb(0x5A4A30))
        b.leaf(15.0, 15.0, -55.0, 12.0, 3.0, rgb(0xC8C8B8), rgb(0xE8E8D8), rgb(0xFFFFFF))
        b.leaf(17.0, 15.0, 55.0, 12.0, 3.0, rgb(0xC8C8B8), rgb(0xE8E8D8), rgb(0xFFFFFF))
        for (i in 0 until 3) { b.line(13, 16 + i * 3, 9, 18 + i * 3, body); b.line(19, 16 + i * 3, 23, 18 + i * 3, body) }
        b.line(15, 6, 13, 2, body); b.line(17, 6, 19, 2, body)
        eyes(b, 13, 17, 7)
        for (y in listOf(20, 23)) b.r(14, y, 4, 1, rgb(0xC0A060))
    }

    private fun mealy(b: Buf) {
        val w = rgb(0xF4F0E4)
        for (i in 0 until 10) { val a = i * 36.0; b.line(16, 18, (16 + cos(Math.toRadians(a)) * 12).roundToInt(), (18 + sin(Math.toRadians(a)) * 9).roundToInt(), w) }
        b.oval(16.0, 18.0, 9.0, 7.0, rgb(0xD8D0C0)); b.oval(15.0, 17.0, 8.0, 6.0, w)
        for (x in 9..23 step 3) b.p(x, 18, rgb(0xC8C0B0))
        for ((x, y) in listOf(11 to 14, 20 to 13, 13 to 22, 21 to 21)) b.r(x, y, 2, 1, rgb(0xFFFFFF))
        eyes(b, 12, 18, 16); grin(b, 14, 20)
    }

    private fun gnat(b: Buf) {
        val wing = rgb(0xD0DCE8)
        b.leaf(13.0, 15.0, -60.0, 12.0, 6.0, rgb(0xA0B0C0), wing, rgb(0xFFFFFF))
        b.leaf(19.0, 15.0, 60.0, 12.0, 6.0, rgb(0xA0B0C0), wing, rgb(0xFFFFFF))
        b.oval(16.0, 20.0, 3.0, 7.0, rgb(0x202020)); b.oval(16.0, 12.0, 4.0, 3.0, rgb(0x303030))
        for (i in 0 until 3) { b.line(14, 18 + i * 2, 9, 22 + i * 3, rgb(0x202020)); b.line(18, 18 + i * 2, 23, 22 + i * 3, rgb(0x202020)) }
        b.line(14, 10, 11, 5, rgb(0x202020)); b.line(18, 10, 21, 5, rgb(0x202020))
        eyes(b, 13, 17, 11)
    }

    private fun aphid(b: Buf) {
        val g = rgb(0x78B040)
        for (s in listOf(-1, 1)) for (i in 0 until 3) b.line(16 + s * 6, 19 + i * 3, 16 + s * 11, 22 + i * 3, rgb(0x406020))
        b.oval(16.0, 20.0, 8.0, 8.0, rgb(0x507828)); b.oval(15.0, 19.0, 7.0, 7.0, g); b.r(11, 15, 2, 2, rgb(0xB0E070))
        b.oval(16.0, 11.0, 4.0, 3.0, rgb(0x60902C))
        b.line(14, 9, 9, 3, rgb(0x406020)); b.line(18, 9, 23, 3, rgb(0x406020))
        b.r(21, 25, 1, 4, rgb(0x406020)); b.r(11, 25, 1, 4, rgb(0x406020))
        eyes(b, 13, 17, 10); grin(b, 14, 23)
    }

    private fun whitefly(b: Buf) {
        b.leaf(16.0, 24.0, -40.0, 15.0, 9.0, rgb(0xD8D8D0), rgb(0xF8F8F4), rgb(0xFFFFFF))
        b.leaf(16.0, 24.0, 40.0, 15.0, 9.0, rgb(0xD8D8D0), rgb(0xF8F8F4), rgb(0xFFFFFF))
        b.oval(16.0, 18.0, 2.0, 6.0, rgb(0xE8D888)); b.oval(16.0, 11.0, 3.0, 2.0, rgb(0xE8D888))
        eyes(b, 13, 17, 10)
        b.line(15, 9, 12, 5, INK); b.line(17, 9, 20, 5, INK)
    }

    private fun rot(b: Buf) {
        val br = rgb(0x5A3A20)
        b.oval(16.0, 19.0, 11.0, 8.0, rgb(0x3A2614)); b.oval(15.0, 18.0, 10.0, 7.0, br)
        for (x in listOf(7, 12, 19, 24)) for (y in 26..29) b.p(x, y + (x % 3), br)
        for ((x, y, r) in listOf(Triple(10, 13, 2), Triple(22, 12, 1), Triple(19, 22, 1))) b.oval(x.toDouble(), y.toDouble(), r.toDouble(), r.toDouble(), rgb(0x8A6A40))
        b.r(9, 18, 4, 1, rgb(0x7A5430)); b.r(20, 20, 3, 1, rgb(0x7A5430))
        eyes(b, 11, 18, 15); b.r(13, 21, 6, 2, INK); b.p(14, 22, rgb(0x90C060)); b.p(17, 22, rgb(0x90C060))
    }

    private fun burn(b: Buf) {
        val o = rgb(0xF08030)
        for (i in 0 until 8) { val a = Math.toRadians(i * 45.0); b.line((16 + cos(a) * 8).roundToInt(), (16 + sin(a) * 8).roundToInt(), (16 + cos(a) * 13).roundToInt(), (16 + sin(a) * 13).roundToInt(), o) }
        b.oval(16.0, 16.0, 8.0, 8.0, rgb(0xD84820)); b.oval(15.0, 15.0, 7.0, 7.0, rgb(0xF8B040)); b.oval(13.0, 13.0, 2.0, 2.0, rgb(0xFFF0A0))
        eyes(b, 12, 18, 14); grin(b, 14, 19)
    }

    private fun mildew(b: Buf) {
        val w = rgb(0xE8ECE8)
        for ((x, y, r) in listOf(Triple(10, 17, 6), Triple(21, 16, 7), Triple(16, 12, 6), Triple(16, 21, 8))) { b.oval(x.toDouble(), y.toDouble(), r.toDouble(), r * .8, rgb(0xC0C8C4)); b.oval(x - 1.0, y - 1.0, r - 1.0, r * .8 - 1, w) }
        for (i in 0 until 14) b.p(5 + (hash(i, 2) * 22).toInt(), 6 + (hash(i, 4) * 20).toInt(), rgb(0xFFFFFF))
        eyes(b, 12, 18, 15, angry = false); grin(b, 14, 20)
    }

    private fun dark(b: Buf) {
        val p = rgb(0x403060)
        b.oval(16.0, 14.0, 9.0, 9.0, rgb(0x281C40)); b.oval(15.0, 13.0, 8.0, 8.0, p)
        b.r(7, 14, 18, 10, p)
        for (x in 7..24 step 4) { b.r(x, 24, 2, 3, p); b.r(x + 2, 24, 2, 1, p) }
        val y = rgb(0xF8E070)
        b.r(11, 12, 3, 2, y); b.r(18, 12, 3, 2, y); b.p(12, 13, INK); b.p(19, 13, INK)
        b.r(13, 18, 6, 1, rgb(0x180C28))
    }

    private fun springtail(b: Buf) {
        val g = rgb(0x9098A0)
        b.oval(16.0, 22.0, 8.0, 4.0, rgb(0x687078)); b.oval(15.0, 21.0, 7.0, 3.0, g)
        b.oval(9.0, 19.0, 3.0, 3.0, g)
        b.line(23, 24, 27, 28, rgb(0x687078)); b.line(24, 24, 28, 27, rgb(0x687078))
        b.line(8, 16, 5, 11, rgb(0x687078)); b.line(10, 16, 11, 11, rgb(0x687078))
        for (x in 11..20 step 3) b.line(x, 24, x - 1, 27, rgb(0x687078))
        b.r(7, 18, 2, 2, rgb(0xFFFFFF)); b.p(8, 19, INK)
    }
}
