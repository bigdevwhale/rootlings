package app.rootlings

import androidx.test.core.app.ApplicationProvider
import app.rootlings.data.Content
import app.rootlings.data.Mood
import app.rootlings.data.Plant
import app.rootlings.data.Sill
import app.rootlings.domain.Care
import app.rootlings.domain.Gift
import app.rootlings.domain.GiftCode
import app.rootlings.domain.Sky
import app.rootlings.domain.Weather
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CareTest {
    private val content by lazy { Content.load(ApplicationProvider.getApplicationContext()) }
    private val zone = ZoneId.of("Europe/Moscow")
    private val sky = Sky(55.75, 37.62, zone, emptyMap())
    private val south = Sill(id = 1, name = "S", azimuth = 180)
    private val north = Sill(id = 2, name = "N", azimuth = 0)

    @Test fun contentIsWhole() {
        assertTrue(content.species.size > 120)
        assertEquals(content.species.size, content.species.map { it.id }.toSet().size)
        assertTrue(content.pests.all { it.moves.size >= 3 })
        val codes = content.symptoms.map { it.code }.toSet()
        assertTrue(content.pests.all { p -> p.symptoms.all { it in codes } })
        for (m in listOf(Mood.CALM, Mood.SUN, Mood.GLOOM, Mood.DRY, Mood.REST, Mood.NEW, Mood.EVOLVE, Mood.HATCH))
            for (e in app.rootlings.data.Element.entries) assertNotNull("$m $e", content.line(m, e, 3))
        assertTrue(content.species.all { it.dliMin < it.dliMax && it.forms.size == 3 })
    }

    @Test fun southGetsMoreLightThanNorthOnASummerDay() {
        // over a whole month, so one cloudy day can't decide it; in winter the low sun makes the gap wide
        val days = (1..28).map { LocalDate.of(2026, 2, it) }
        assertTrue(days.sumOf { sky.dli(it, south, 0) } > days.sumOf { sky.dli(it, north, 0) } * 1.5)
        assertTrue(days.sumOf { sky.dli(it, south, 2) } < days.sumOf { sky.dli(it, south, 0) } / 3)
    }

    @Test fun potsDryAndChecksTeach() {
        val sp = content.species("nephrolepis_exaltata")!!
        val t0 = LocalDate.of(2026, 10, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val p = Plant(id = 1, species = sp.id, name = "F", sill = 2, caught = t0, hue = 110, moist = 100.0, moistAt = t0)
        val later = Care.moisture(p, sp, north, sky, t0 + 4 * Care.DAY)
        assertTrue(later in 30.0..95.0)
        // the finger says it's already dry: the pot dries faster than the book
        val checked = Care.checked(p, sp, north, sky, t0 + 4 * Care.DAY, 0)
        assertTrue(checked.dryK > 1.0)
        assertEquals(12.0, checked.moist, .01)
    }

    @Test fun cactusRestsInWinter() {
        val sp = content.species("opuntia_microdasys")!!
        val now = LocalDateTime.of(2026, 12, 10, 13, 0)
        val p = Plant(id = 2, species = sp.id, name = "C", sill = 1, caught = 0, hue = 96, moist = 40.0, moistAt = now.atZone(zone).toInstant().toEpochMilli())
        assertEquals(Mood.REST, Care.status(p, sp, south, sky, now, null).mood)
    }

    @Test fun heatingSeasonFromColdDays() {
        assertTrue(sky.heating(LocalDate.of(2026, 1, 15)))
        assertTrue(!sky.heating(LocalDate.of(2026, 7, 15)))
    }

    @Test fun vacationPlansWaterings() {
        val sp = content.species("nephrolepis_exaltata")!!
        val now = LocalDateTime.of(2026, 5, 1, 10, 0)
        val p = Plant(id = 3, species = sp.id, name = "F", sill = 2, caught = 0, hue = 110, moist = 60.0, moistAt = now.atZone(zone).toInstant().toEpochMilli())
        assertTrue(Care.waterings(p, sp, north, sky, now, 14).size >= 2)
    }

    @Test fun simulatedDaysAreWhole() {
        val d = Weather.simulated(LocalDate.of(2026, 3, 3), 55.0, 37.0, zone)
        assertEquals(24, d.hours.size)
        assertTrue(d.hours.any { it.direct + it.diffuse > 0 })
    }

    @Test fun giftCodeRoundTrips() {
        val g = Gift("chlorophytum_comosum", "Усик", "Усач ← хлорофитум бабушки Зои", 90, "Марат")
        val text = "Держи! " + GiftCode.encode(g) + " 🌱"
        assertEquals(g, GiftCode.decode(text))
        assertEquals(null, GiftCode.decode("no code here"))
    }
}
