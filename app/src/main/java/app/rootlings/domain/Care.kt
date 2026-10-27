package app.rootlings.domain

import app.rootlings.data.Fight
import app.rootlings.data.Mood
import app.rootlings.data.Plant
import app.rootlings.data.Sill
import app.rootlings.data.Species
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * The sky over the windows: every day's hours (real where fetched, the app's own guess where not)
 * and the light each window gets from them.
 */
class Sky(val lat: Double, val lon: Double, val zone: ZoneId, private val days: Map<LocalDate, DayWeather>) {
    private val cache = HashMap<LocalDate, DayWeather>()
    private val gains = HashMap<Triple<LocalDate, Int, Int>, DoubleArray>()

    fun day(date: LocalDate): DayWeather = days[date] ?: cache.getOrPut(date) { Weather.simulated(date, lat, lon, zone) }
    fun real(date: LocalDate) = days[date]?.real == true

    fun sun(date: LocalDate, hour: Int, minute: Int = 30): SunPos =
        Sun.at(date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli(), lat, lon)

    fun sunNow(now: LocalDateTime): SunPos = Sun.at(now.atZone(zone).toInstant().toEpochMilli(), lat, lon)

    /** W/m² on the sill for each hour of a day. */
    fun gains(date: LocalDate, sill: Sill): DoubleArray = gains.getOrPut(Triple(date, sill.azimuth, sill.view)) {
        val d = day(date)
        DoubleArray(24) { h -> Light.gain(d.hours[h], sun(date, h), sill.azimuth.toDouble(), sill.view) }
    }

    /** The daily light integral at a spot: on the sill, a metre in, or deeper in the room. */
    fun dli(date: LocalDate, sill: Sill, spot: Int): Double = Light.dli(gains(date, sill)) * Light.spot(spot)

    /** Days in a row, up to yesterday, colder than +8 °C on average: the radiators are surely on. */
    fun coldDays(today: LocalDate): Int {
        var n = 0
        var d = today.minusDays(1)
        while (n < 30 && day(d).mean < 8) { n++; d = d.minusDays(1) }
        return n
    }

    fun heating(today: LocalDate) = coldDays(today) >= 5

    /** Winter in this hemisphere: the months a resting cactus wants cool and dry. */
    fun winter(date: LocalDate): Boolean {
        val m = date.monthValue
        return if (lat >= 0) m == 11 || m == 12 || m == 1 || m == 2 else m in 5..8
    }
}

object Light {
    /**
     * Sunlight reaching a sill behind a window looking out at [azimuth]: the direct beam on vertical
     * glass, half the sky's diffuse light, through glass; what's outside ([view]) takes its share.
     */
    fun gain(h: Hour, sun: SunPos, azimuth: Double, view: Int): Double {
        if (sun.alt <= 1) return 0.0
        val s = sin(Math.toRadians(sun.alt))
        val dni = min(h.direct / max(s, .08), 950.0)
        val cosI = cos(Math.toRadians(sun.alt)) * cos(Math.toRadians(sun.az - azimuth))
        // Trees or houses across the way hide the low sun; a near wall hides most of it.
        val blocked = when (view) { 1 -> sun.alt < 15; 2 -> sun.alt < 40; else -> false }
        val direct = if (blocked) 0.0 else dni * max(0.0, cosI)
        val sky = h.diffuse * .5 * when (view) { 1 -> .75; 2 -> .45; else -> 1.0 }
        return (direct + sky) * .72
    }

    /** W/m² of sunlight over the day as mol of photosynthetic light per m². */
    fun dli(gains: DoubleArray): Double = gains.sumOf { it * 2.02 * 3600 } / 1e6

    /** How much of the sill's light is left at a spot: light falls off fast away from the glass. */
    fun spot(spot: Int) = when (spot) { 0 -> 1.0; 1 -> .45; else -> .2 }
}

/** Everything a card shows about one plant at one moment. */
data class Status(
    val mood: Mood,
    val moist: Double,
    /** Days until the soil is worth a finger check; 0 is now. */
    val checkIn: Int,
    val dliToday: Double,
    val dliWeek: Double,
    val gainNow: Double,
    val rest: Boolean,
    val heating: Boolean,
    val evolve: Boolean,
    val molt: Boolean,
    val fight: Fight?,
    val blooming: Boolean,
)

object Care {
    const val DAY = 86_400_000L
    const val MOLT_DAYS = 28

    fun rest(sp: Species, sky: Sky, date: LocalDate) = sp.rest && sky.winter(date)

    /** % of soil moisture a plant drinks in a day: its book rate, the light it got, dry heated air, its own pot. */
    fun rate(sp: Species, p: Plant, dli: Double, heating: Boolean, rest: Boolean): Double {
        val light = .6 + .4 * min(dli / sp.dliMin, 2.0)
        val air = if (heating) 1.25 else 1.0
        val sleep = if (rest) .5 else 1.0
        return sp.dry * light * air * sleep * p.dryK
    }

    /** The moisture estimate now: the last known value, dried day by day since. */
    fun moisture(p: Plant, sp: Species, sill: Sill?, sky: Sky, now: Long): Double {
        var m = p.moist
        var t = p.moistAt
        while (t < now) {
            val date = Instant.ofEpochMilli(t).atZone(sky.zone).toLocalDate()
            val end = min(now, date.plusDays(1).atStartOfDay(sky.zone).toInstant().toEpochMilli())
            val dli = sill?.let { sky.dli(date, it, p.spot) } ?: sp.dliMin
            m -= rate(sp, p, dli, sky.heating(date), rest(sp, sky, date)) * (end - t) / DAY.toDouble()
            t = end
            if (m <= 0) return 0.0
        }
        return m.coerceIn(0.0, 100.0)
    }

    /** When to check: the book's watering point, much lower for a resting plant. */
    fun waterAt(sp: Species, rest: Boolean) = if (rest) 5 else sp.waterAt

    fun status(p: Plant, sp: Species, sill: Sill?, sky: Sky, now: LocalDateTime, fight: Fight?): Status {
        val ms = now.atZone(sky.zone).toInstant().toEpochMilli()
        val today = now.toLocalDate()
        val m = moisture(p, sp, sill, sky, ms)
        val resting = rest(sp, sky, today)
        val heating = sky.heating(today)
        val dliToday = sill?.let { sky.dli(today, it, p.spot) } ?: 0.0
        val dliWeek = sill?.let { s -> (0..6).map { sky.dli(today.minusDays(it.toLong()), s, p.spot) }.average() } ?: 0.0
        val hour = now.hour
        val gain = sill?.let { sky.gains(today, it)[hour] * Light.spot(p.spot) } ?: 0.0
        val sun = sky.sunNow(now)
        val at = waterAt(sp, resting)
        val r = rate(sp, p, max(dliWeek, .1), heating, resting).coerceAtLeast(.3)
        val checkIn = if (m <= at + 3) 0 else ((m - at) / r).toInt().coerceAtLeast(1)
        val age = (ms - p.caught) / DAY
        val evolve = (p.stage == 1 && p.growth >= sp.s2 && age >= 21) || (p.stage == 2 && p.growth >= sp.s3 && age >= 90)
        val molt = (ms - max(p.photoAt, p.caught)) / DAY >= MOLT_DAYS
        val mood = when {
            fight != null -> Mood.PEST
            resting -> Mood.REST
            sun.alt < -2 -> Mood.NIGHT
            ms - p.soggyAt < 2 * DAY -> Mood.SOGGY
            m <= at + 3 -> Mood.DRY
            gain > 330 && sp.dliMax < 20 -> Mood.SCORCH
            (dliWeek + dliToday) / 2 < sp.dliMin * .6 -> Mood.GLOOM
            gain > 120 -> Mood.SUN
            heating && sp.humid -> Mood.HEAT
            else -> Mood.CALM
        }
        return Status(mood, m, checkIn, dliToday, dliWeek, gain, resting, heating, evolve, molt, fight, ms - p.bloomAt < 45 * DAY)
    }

    /** What a finger check reads as, in moisture. */
    fun reading(r: Int) = when (r) { 0 -> 12.0; 1 -> 45.0; else -> 85.0 }

    /**
     * A finger check: the soil is set to what the finger felt, and the pot's drying speed is nudged
     * towards what happened since the last known value, so the next guess is closer.
     */
    fun checked(p: Plant, sp: Species, sill: Sill?, sky: Sky, now: Long, reading: Int): Plant {
        val felt = reading(reading)
        val guess = moisture(p, sp, sill, sky, now)
        val days = (now - p.moistAt) / DAY.toDouble()
        var k = p.dryK
        val predicted = p.moist - guess
        val actual = p.moist - felt
        if (days >= 1 && predicted > 6) k = (k * kotlin.math.sqrt((actual / predicted).coerceIn(.4, 2.5))).coerceIn(.35, 3.0)
        return p.copy(moist = felt, moistAt = now, dryK = k, soggyAt = if (reading == 2) now else 0)
    }

    fun watered(p: Plant, now: Long) = p.copy(moist = 100.0, moistAt = now, soggyAt = 0)

    /** The days in the next [n] a plant would want water, if nobody checks: for the neighbour's note. */
    fun waterings(p: Plant, sp: Species, sill: Sill?, sky: Sky, now: LocalDateTime, n: Int): List<LocalDate> {
        val out = mutableListOf<LocalDate>()
        var m = moisture(p, sp, sill, sky, now.atZone(sky.zone).toInstant().toEpochMilli())
        for (i in 0 until n) {
            val date = now.toLocalDate().plusDays(i.toLong())
            val rest = rest(sp, sky, date)
            if (m <= waterAt(sp, rest) + 2) { out += date; m = 100.0 }
            m -= rate(sp, p, sill?.let { sky.dli(date, it, p.spot) } ?: sp.dliMin, sky.heating(date), rest)
        }
        return out
    }
}
