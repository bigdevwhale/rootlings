package app.rootlings.domain

import app.rootlings.data.Day
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

/** One hour outside: °C, relative humidity %, cloud cover %, direct and diffuse radiation on the ground (W/m²), mm of rain or snow. */
data class Hour(val temp: Double, val rh: Double, val cloud: Double, val direct: Double, val diffuse: Double, val precip: Double)

/** A town the weather comes from. */
data class Place(val name: String, val region: String, val lat: Double, val lon: Double)

/** A local day's 24 hours. */
class DayWeather(val date: LocalDate, val hours: List<Hour>, val real: Boolean) {
    val mean: Double get() = hours.map { it.temp }.average()

    fun toRow(at: Long) = Day(
        date.toEpochDay(),
        hours.joinToString(";") { h -> listOf(h.temp, h.rh, h.cloud, h.direct, h.diffuse, h.precip).joinToString(",") { String.format(Locale.US, "%.1f", it) } },
        mean, at, real,
    )

    companion object {
        fun of(d: Day): DayWeather? = runCatching {
            val hs = d.hours.split(';').map { r -> r.split(',').map(String::toDouble).let { Hour(it[0], it[1], it[2], it[3], it[4], it[5]) } }
            if (hs.size != 24) null else DayWeather(LocalDate.ofEpochDay(d.day), hs, d.real)
        }.getOrNull()
    }
}

object Weather {
    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 6000
        c.readTimeout = 9000
        c.setRequestProperty("User-Agent", "Rootlings/1.0 github.com/bigdevwhale/rootlings")
        try {
            if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    /**
     * The last week, today and the coming week, hour by hour: from Open-Meteo, or from MET Norway
     * when Open-Meteo can't be reached (some networks block it; MET has no history and no radiation,
     * so the light is worked out from the clouds). Nothing but the coordinates is sent.
     */
    fun fetch(lat: Double, lon: Double, zone: ZoneId = ZoneId.systemDefault()): List<DayWeather> =
        runCatching { openMeteo(lat, lon) }.getOrElse { metNo(lat, lon, zone) }

    private fun openMeteo(lat: Double, lon: Double): List<DayWeather> {
        val j = JSONObject(get(String.format(Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.3f&longitude=%.3f&hourly=temperature_2m,relative_humidity_2m,cloud_cover,direct_radiation,diffuse_radiation,precipitation&past_days=7&forecast_days=8&timezone=auto",
            lat, lon)))
        val h = j.getJSONObject("hourly")
        val time = h.getJSONArray("time")
        fun arr(k: String, i: Int) = h.getJSONArray(k).optDouble(i, 0.0).let { if (it.isNaN()) 0.0 else it }
        val by = (0 until time.length()).groupBy { time.getString(it).substring(0, 10) }
        return by.mapNotNull { (date, idx) ->
            if (idx.size != 24) return@mapNotNull null
            DayWeather(LocalDate.parse(date), idx.map { i ->
                Hour(arr("temperature_2m", i), arr("relative_humidity_2m", i), arr("cloud_cover", i), arr("direct_radiation", i), arr("diffuse_radiation", i), arr("precipitation", i))
            }, true)
        }
    }

    private fun metNo(lat: Double, lon: Double, zone: ZoneId): List<DayWeather> {
        val j = JSONObject(get(String.format(Locale.US, "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=%.3f&lon=%.3f", lat, lon)))
        val ts = j.getJSONObject("properties").getJSONArray("timeseries")
        // The forecast thins out to six-hourly steps; every hour takes the latest step at or before it.
        val steps = (0 until ts.length()).map { i ->
            val o = ts.getJSONObject(i)
            val at = LocalDateTime.ofInstant(Instant.parse(o.getString("time")), zone)
            val d = o.getJSONObject("data")
            val inst = d.getJSONObject("instant").getJSONObject("details")
            val p = d.optJSONObject("next_1_hours")?.optJSONObject("details")?.optDouble("precipitation_amount")
                ?: (d.optJSONObject("next_6_hours")?.optJSONObject("details")?.optDouble("precipitation_amount") ?: 0.0) / 6
            at to doubleArrayOf(inst.optDouble("air_temperature", 5.0), inst.optDouble("relative_humidity", 70.0), inst.optDouble("cloud_area_fraction", 60.0), p)
        }
        val first = steps.first().first.toLocalDate()
        return (0..7).mapNotNull { k ->
            val date = first.plusDays(k.toLong())
            val hours = (0 until 24).map { hr ->
                val t = date.atTime(hr, 30)
                val s = steps.lastOrNull { !it.first.isAfter(t) } ?: steps.first()
                val ms = t.atZone(zone).toInstant().toEpochMilli()
                val (dir, dif) = fromClouds(Sun.at(ms, lat, lon).alt, s.second[2])
                Hour(s.second[0], s.second[1], s.second[2], dir, dif, s.second[3].let { if (it.isNaN()) 0.0 else it })
            }
            DayWeather(date, hours, true)
        }
    }

    /**
     * Direct and diffuse radiation on the ground for a sun altitude and a cloud cover, when no one
     * measured it: a clear-sky curve dimmed by the clouds (Kasten–Czeplak), split by how grey it is.
     */
    fun fromClouds(alt: Double, cloud: Double): Pair<Double, Double> {
        if (alt <= 0) return 0.0 to 0.0
        val s = sin(Math.toRadians(alt))
        val clear = 1098 * s * exp(-0.057 / s)
        val c = (cloud / 100).coerceIn(0.0, 1.0)
        val ghi = clear * (1 - 0.75 * c.pow(3.4))
        val kd = (0.15 + 0.8 * c).coerceAtMost(1.0)
        return ghi * (1 - kd) to ghi * kd
    }

    /**
     * The window's own weather when there's no real one: plausible for the month and latitude,
     * the same for everyone on the same day.
     */
    fun simulated(date: LocalDate, lat: Double, lon: Double, zone: ZoneId = ZoneId.systemDefault()): DayWeather {
        val south = lat < 0
        val m = if (south) (date.monthValue + 5) % 12 + 1 else date.monthValue
        val base = doubleArrayOf(-7.0, -6.0, -1.0, 7.0, 13.0, 17.0, 19.0, 17.0, 11.0, 5.0, -1.0, -5.0)[m - 1] + (50 - kotlin.math.abs(lat)) * .35
        val e = date.toEpochDay().toInt()
        val grey = doubleArrayOf(.85, .8, .7, .6, .55, .5, .5, .5, .6, .75, .85, .88)[m - 1]
        val cloudDay = (grey * 100 + (rnd(e, 3) - .5) * 70).coerceIn(5.0, 100.0)
        val hours = (0 until 24).map { h ->
            val ms = date.atTime(h, 30).atZone(zone).toInstant().toEpochMilli()
            val alt = Sun.at(ms, lat, lon).alt
            val cl = (cloudDay + (rnd(e * 24 + h, 7) - .5) * 30).coerceIn(0.0, 100.0)
            val (dir, dif) = fromClouds(alt, cl)
            val temp = base + (rnd(e, 11) - .5) * 6 - 3 * cos((h - 3) / 24.0 * 2 * Math.PI)
            Hour(temp, 70 + 20 * cl / 100, cl, dir, dif, if (cl > 85 && rnd(e * 24 + h, 13) < .35) .4 else 0.0)
        }
        return DayWeather(date, hours, false)
    }

    private fun rnd(i: Int, j: Int): Double {
        var h = i * 374761393 + j * 668265263
        h = (h xor (h ushr 13)) * 1274126177
        return ((h xor (h ushr 16)).toLong() and 0xFFFFFFFFL) / 4294967296.0
    }

    /** Towns matching a name, for the settings search. */
    fun search(name: String, lang: String): List<Place> {
        val q = URLEncoder.encode(name.trim(), "UTF-8")
        val j = JSONObject(get("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=8&language=$lang&format=json"))
        val r = j.optJSONArray("results") ?: return emptyList()
        return (0 until r.length()).map { i ->
            val o = r.getJSONObject(i)
            Place(o.getString("name"), listOf(o.optString("admin1"), o.optString("country")).filter { it.isNotBlank() }.joinToString(", "),
                o.getDouble("latitude"), o.getDouble("longitude"))
        }
    }

    /** Typical coordinates for the phone's time zone, when no town is set: the sun still rises at the right hour. */
    fun guess(zone: ZoneId = ZoneId.systemDefault()): Place {
        val off = zone.rules.getOffset(Instant.now()).totalSeconds / 3600.0
        return Place("", "", 50.0, max(-180.0, minOf(180.0, off * 15)))
    }
}
