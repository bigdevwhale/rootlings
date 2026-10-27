package app.rootlings.data

import app.rootlings.domain.Care
import app.rootlings.domain.DayWeather
import app.rootlings.domain.Gift
import app.rootlings.domain.Sky
import app.rootlings.domain.Weather
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId

/** Everything in the garden at one moment, straight from the database. */
class GardenState(
    val sills: List<Sill>,
    val plants: List<Plant>,
    val notes: List<Note>,
    val fights: List<Fight>,
    val cuttings: List<Cutting>,
    val sightings: List<Sighting>,
    val own: List<OwnSpecies>,
    val days: Map<LocalDate, DayWeather>,
) {
    val alive = plants.filter { it.alive }
    fun plant(id: Long) = plants.firstOrNull { it.id == id }
    fun sill(id: Long) = sills.firstOrNull { it.id == id }
    fun onSill(id: Long) = alive.filter { it.sill == id }
    fun fight(plant: Long) = fights.firstOrNull { it.plant == plant && it.won == null }
    fun notes(plant: Long) = notes.filter { it.plant == plant }
}

private data class Part(val sills: List<Sill>, val plants: List<Plant>, val notes: List<Note>, val fights: List<Fight>)

class Garden(private val dao: GardenDao) {
    val state: Flow<GardenState> = combine(
        combine(dao.sills(), dao.plants(), dao.notes(), dao.fights(), ::Part),
        dao.cuttings(), dao.sightings(), dao.own(), dao.days(),
    ) { a, cuttings, sightings, own, days ->
        GardenState(a.sills, a.plants, a.notes, a.fights, cuttings, sightings, own, days.mapNotNull { DayWeather.of(it) }.associateBy { it.date })
    }

    private val now get() = System.currentTimeMillis()

    suspend fun saveWeather(days: List<DayWeather>) {
        val at = now
        for (d in days) dao.upsert(d.toRow(at))
        dao.pruneDays(LocalDate.now().minusDays(40).toEpochDay())
    }

    suspend fun addSill(name: String, azimuth: Int, view: Int): Long = dao.insert(Sill(name = name, azimuth = azimuth, view = view, order = dao.sillCount()))
    suspend fun update(s: Sill) = dao.update(s)

    /** A window goes only when nothing stands at it. */
    suspend fun deleteSill(s: Sill): Boolean {
        if (dao.allPlants().any { it.sill == s.id && it.alive }) return false
        dao.deleteSill(s.id)
        return true
    }

    suspend fun catchPlant(species: String, name: String, sill: Long, spot: Int, hue: Int, photo: String?, moist: Double, lineage: String = "", parent: Long? = null): Long {
        val t = now
        val id = dao.insert(Plant(species = species, name = name, sill = sill, spot = spot, caught = t, hue = hue, photo = photo, photoAt = if (photo != null) t else 0,
            parent = parent, lineage = lineage, moist = moist, moistAt = t, order = dao.allPlants().size))
        dao.insert(Note(plant = id, at = t, type = Note.CAUGHT, photo = photo))
        return id
    }

    suspend fun update(p: Plant) = dao.update(p)

    suspend fun check(p: Plant, sp: Species, sky: Sky, reading: Int) {
        val t = now
        val cur = dao.plant(p.id) ?: p
        dao.update(Care.checked(cur, sp, dao.allSills().firstOrNull { it.id == cur.sill }, sky, t, reading))
        dao.insert(Note(plant = p.id, at = t, type = Note.CHECK, value = reading))
    }

    suspend fun water(p: Plant) {
        val t = now
        dao.update(Care.watered(dao.plant(p.id) ?: p, t))
        dao.insert(Note(plant = p.id, at = t, type = Note.WATER))
    }

    suspend fun grew(p: Plant, leaves: Int, photo: String?, hue: Int?) {
        val t = now
        val cur = dao.plant(p.id) ?: p
        dao.update(cur.copy(growth = cur.growth + leaves, photo = photo ?: cur.photo, photoAt = if (photo != null) t else cur.photoAt, hue = hue ?: cur.hue))
        dao.insert(Note(plant = p.id, at = t, type = if (photo != null) Note.MOLT else Note.LEAF, value = leaves, photo = photo))
    }

    suspend fun bloomed(p: Plant) {
        val t = now
        dao.update((dao.plant(p.id) ?: p).copy(bloomAt = t))
        dao.insert(Note(plant = p.id, at = t, type = Note.BLOOM))
    }

    suspend fun evolve(p: Plant) {
        val cur = dao.plant(p.id) ?: p
        if (cur.stage >= 3) return
        dao.update(cur.copy(stage = cur.stage + 1))
        dao.insert(Note(plant = p.id, at = now, type = Note.EVOLVE, value = cur.stage + 1))
    }

    suspend fun move(p: Plant, sill: Long, spot: Int) {
        dao.update((dao.plant(p.id) ?: p).copy(sill = sill, spot = spot))
        dao.insert(Note(plant = p.id, at = now, type = Note.MOVED, value = spot))
    }

    suspend fun gone(p: Plant, how: Int) {
        val t = now
        dao.update((dao.plant(p.id) ?: p).copy(gone = t, goneHow = how))
        dao.insert(Note(plant = p.id, at = t, type = Note.GONE, value = how))
        dao.allFights().filter { it.plant == p.id && it.won == null }.forEach { dao.deleteFight(it.id) }
    }

    suspend fun fight(p: Plant, pest: String) {
        val t = now
        dao.insert(Fight(plant = p.id, pest = pest, started = t))
        dao.insert(Note(plant = p.id, at = t, type = Note.FIGHT, text = pest))
    }

    /** One move made; the last one wins the battle. */
    suspend fun move(f: Fight, moves: Int) {
        val t = now
        val step = f.step + 1
        val won = if (step >= moves) t else null
        dao.update(f.copy(step = step, stepAt = t, won = won))
        if (won != null) dao.insert(Note(plant = f.plant, at = t, type = Note.WON, text = f.pest))
    }

    suspend fun retreat(f: Fight) = dao.deleteFight(f.id)

    suspend fun cutting(p: Plant, medium: String) {
        val t = now
        dao.insert(Cutting(parent = p.id, species = p.species, started = t, medium = medium))
        dao.insert(Note(plant = p.id, at = t, type = Note.CUTTING, text = medium))
    }

    suspend fun hatch(c: Cutting, name: String, sill: Long, spot: Int): Long {
        val parent = dao.plant(c.parent)
        val lineage = parent?.let { listOf(it.name, it.lineage).filter(String::isNotBlank).joinToString(" ← ") } ?: ""
        val id = catchPlant(c.species, name, sill, spot, (parent?.hue ?: 110) + 6, null, 70.0, lineage, c.parent)
        dao.update(c.copy(hatched = now, child = id))
        dao.insert(Note(plant = c.parent, at = now, type = Note.HATCH, text = name, value = id.toInt()))
        return id
    }

    suspend fun given(c: Cutting) = dao.update(c.copy(hatched = now, given = true))
    suspend fun dropCutting(c: Cutting) = dao.deleteCutting(c.id)

    /** A gift code pasted in: a new plant with its ancestry, and its species if it's someone's own. */
    suspend fun receive(g: Gift, sill: Long, spot: Int): Long {
        var species = g.species
        if (g.own != null) species = "own_" + dao.insert(g.own.copy(id = 0))
        val lineage = listOf(g.lineage, g.from).filter(String::isNotBlank).joinToString(" ← ")
        return catchPlant(species, g.name, sill, spot, g.hue, null, 70.0, lineage)
    }

    suspend fun saw(species: String, place: String, photo: String?) = dao.insert(Sighting(species = species, at = now, place = place, photo = photo))
    suspend fun unsee(s: Sighting) = dao.deleteSighting(s.id)

    suspend fun addOwn(o: OwnSpecies): String = "own_" + dao.insert(o)
    suspend fun update(o: OwnSpecies) = dao.update(o)

    companion object {
        /** The sky for a state and a place (or the time zone's guess when no town is set). */
        fun sky(s: GardenState, lat: Double?, lon: Double?, zone: ZoneId = ZoneId.systemDefault()): Sky {
            val g = Weather.guess(zone)
            return Sky(lat ?: g.lat, lon ?: g.lon, zone, if (lat != null) s.days else emptyMap())
        }
    }
}
