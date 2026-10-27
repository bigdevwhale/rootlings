package app.rootlings.data

import android.content.Context
import app.rootlings.AppContainer
import app.rootlings.domain.Place
import kotlinx.coroutines.flow.first

/**
 * The demo build's garden (`-Pdemo`): three windows in Moscow and half a year of care — a monstera
 * about to change form, a calathea fighting spider mites, a spider plant's baby rooting in a glass,
 * plants seen at friends', one of the keeper's own species, and a basil pressed in the herbarium.
 */
object DemoSeed {
    private const val DAY = 86_400_000L

    suspend fun seedIfEmpty(ctx: Context, c: AppContainer) {
        val dao = c.db.dao()
        if (dao.sillCount() > 0) return
        val ru = ctx.resources.configuration.locales[0].language == "ru"
        fun t(en: String, r: String) = if (ru) r else en
        c.settings.update { it.copy(onboarded = true, place = Place(t("Moscow", "Москва"), t("Russia", "Россия"), 55.7558, 37.6173), weatherAt = 0, keeper = t("Marat", "Марат")) }
        val now = System.currentTimeMillis()
        val east = dao.insert(Sill(name = t("Kitchen", "Кухня"), azimuth = 95, view = 1, order = 0))
        val south = dao.insert(Sill(name = t("Living room", "Гостиная"), azimuth = 180, view = 0, order = 1))
        val north = dao.insert(Sill(name = t("Bedroom", "Спальня"), azimuth = 5, view = 1, order = 2))
        var order = 0
        suspend fun plant(sp: String, name: String, sill: Long, days: Int, stage: Int, growth: Int, hue: Int, moist: Double, spot: Int = 0, dryK: Double = 1.0, lineage: String = "", parent: Long? = null, bloomDays: Int? = null): Long {
            val caught = now - days * DAY
            val id = dao.insert(Plant(species = sp, name = name, sill = sill, spot = spot, caught = caught, hue = hue, growth = growth, stage = stage,
                photoAt = now - (days % 26 + 2) * DAY, moist = moist, moistAt = now - DAY / 2, dryK = dryK, lineage = lineage, parent = parent,
                bloomAt = bloomDays?.let { now - it * DAY } ?: 0, order = order++))
            dao.insert(Note(plant = id, at = caught, type = Note.CAUGHT))
            return id
        }
        val margo = plant("monstera_deliciosa", t("Margo", "Марго"), east, 238, 2, 9, 140, 58.0)
        val lira = plant("ficus_lyrata", t("Lyra", "Лира"), east, 48, 1, 2, 96, 42.0, spot = 1)
        val whisk = plant("chlorophytum_comosum", t("Whiskers", "Усач"), east, 660, 3, 31, 84, 30.0, dryK = 1.2, lineage = t("Grandma Zoya's spider plant", "хлорофитум бабушки Зои"))
        plant("echeveria_elegans", t("Button", "Пуговка"), south, 166, 2, 6, 170, 22.0)
        plant("opuntia_microdasys", t("Hedgehog", "Ёж"), south, 485, 2, 3, 96, 12.0)
        val own = dao.insert(OwnSpecies(name = t("Grandma's flower", "Бабушкин цветок"), kind = Kind.BUSH.name, element = Element.MEADOW.name, light = 2, water = 2,
            humid = false, toxic = false, rest = false, bloom = 0xFFF06080.toInt(), hue = 120))
        plant("own_$own", t("Rosie", "Розочка"), south, 120, 2, 7, 120, 50.0, bloomDays = 12)
        val shurshun = plant("nephrolepis_exaltata", t("Rustle", "Шуршун"), north, 350, 2, 17, 118, 55.0)
        val stripe = plant("goeppertia_orbifolia", t("Stripe", "Полоска"), north, 86, 2, 6, 128, 48.0)
        plant("pilea_peperomioides", t("Penny", "Монетка"), north, 30, 1, 1, 110, 60.0, spot = 1, lineage = t("Margo's neighbour Kate", "соседка Катя"))
        // half a year of finger checks and waterings
        for (p in listOf(margo, lira, whisk, shurshun, stripe)) for (k in 1..24) {
            val at = now - k * 7 * DAY + (p % 3) * DAY
            dao.insert(Note(plant = p, at = at, type = if (k % 2 == 0) Note.WATER else Note.CHECK, value = (k + p.toInt()) % 3))
        }
        dao.insert(Note(plant = margo, at = now - 20 * DAY, type = Note.MOLT, value = 2))
        dao.insert(Note(plant = margo, at = now - 120 * DAY, type = Note.EVOLVE, value = 2))
        dao.insert(Note(plant = whisk, at = now - 300 * DAY, type = Note.EVOLVE, value = 3))
        // the battle in progress: two moves made
        dao.insert(Fight(plant = stripe, pest = "mite", started = now - 8 * DAY, step = 3, stepAt = now - 8 * DAY))
        dao.insert(Note(plant = stripe, at = now - 8 * DAY, type = Note.FIGHT, text = "mite"))
        // won earlier
        dao.insert(Fight(plant = margo, pest = "gnat", started = now - 90 * DAY, step = 4, stepAt = now - 70 * DAY, won = now - 70 * DAY))
        // a baby rooting in a glass
        dao.insert(Cutting(parent = whisk, species = "chlorophytum_comosum", started = now - 12 * DAY, medium = "WATER"))
        // seen elsewhere
        dao.insert(Sighting(species = "dracaena_trifasciata", at = now - 40 * DAY, place = t("Mum's", "у мамы")))
        dao.insert(Sighting(species = "epipremnum_aureum", at = now - 25 * DAY, place = t("Kate's", "у Кати")))
        dao.insert(Sighting(species = "phalaenopsis", at = now - 10 * DAY, place = t("the café on the corner", "кофейня на углу")))
        dao.insert(Sighting(species = "zamioculcas_zamiifolia", at = now - 5 * DAY, place = t("the office", "в офисе")))
        // pressed in the herbarium
        val basil = plant("ocimum_basilicum", t("Basil", "Базилик"), south, 200, 1, 3, 100, 50.0)
        dao.plant(basil)?.let { dao.update(it.copy(gone = now - 60 * DAY, goneHow = Plant.DIED)) }
        c.garden.state.first()
        runCatching { c.refreshWeather(force = true) }
    }
}
