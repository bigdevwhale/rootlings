package app.rootlings.domain

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/** Where the sun is: [alt] above the horizon and [az] from north clockwise, both in degrees. */
data class SunPos(val alt: Double, val az: Double)

object Sun {
    private fun rad(d: Double) = Math.toRadians(d)

    /** The sun at an instant, good to a fraction of a degree, which is plenty for a windowsill. */
    fun at(ms: Long, lat: Double, lon: Double): SunPos {
        val n = ms / 86_400_000.0 + 2440587.5 - 2451545.0
        val l = (280.46 + 0.9856474 * n) % 360
        val g = rad((357.528 + 0.9856003 * n) % 360)
        val lam = rad(l + 1.915 * sin(g) + 0.020 * sin(2 * g))
        val eps = rad(23.439 - 0.0000004 * n)
        val ra = atan2(cos(eps) * sin(lam), cos(lam))
        val dec = asin(sin(eps) * sin(lam))
        val gmst = ((18.697374558 + 24.06570982441908 * n) % 24 + 24) % 24
        val h = rad(gmst * 15 + lon) - ra
        val phi = rad(lat)
        val alt = asin(sin(phi) * sin(dec) + cos(phi) * cos(dec) * cos(h))
        val az = atan2(-sin(h), tan(dec) * cos(phi) - sin(phi) * cos(h))
        return SunPos(Math.toDegrees(alt), (Math.toDegrees(az) + 360) % 360)
    }
}
