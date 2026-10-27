package app.rootlings.ui

import androidx.compose.runtime.Immutable
import app.rootlings.data.Content
import app.rootlings.data.GardenState
import app.rootlings.data.Mood
import app.rootlings.data.Plant
import app.rootlings.data.Settings
import app.rootlings.data.Species
import app.rootlings.domain.Care
import app.rootlings.domain.Sky
import app.rootlings.domain.Status
import app.rootlings.ui.art.Look
import java.time.LocalDateTime

/** Everything a screen reads, at one moment. */
@Immutable
class Home(
    val g: GardenState,
    base: Content,
    val settings: Settings,
    val sky: Sky,
    val now: LocalDateTime,
    val ru: Boolean,
) {
    val content: Content = base.with(g.own.map { it.species() })
    val nowMs: Long = now.atZone(sky.zone).toInstant().toEpochMilli()
    val today = now.toLocalDate()
    val heating = sky.heating(today)

    private val statuses = HashMap<Long, Status>()

    fun species(p: Plant): Species = content.species(p.species) ?: content.species.first()

    fun status(p: Plant): Status = statuses.getOrPut(p.id) { Care.status(p, species(p), g.sill(p.sill), sky, now, g.fight(p.id)) }

    /** How a plant's spirit looks: its species' shape and its own green, flowers when in bloom. */
    fun look(p: Plant, mood: Mood? = null, stage: Int = p.stage): Look {
        val sp = species(p)
        val st = status(p)
        val flower = if (st.blooming || (stage == 3 && sp.bloom != null)) sp.bloom ?: 0xFFF0F0F0.toInt() else null
        return Look(sp.kind, p.hue, sp.sat, sp.lit, stage, mood ?: st.mood, flower, (p.id % 4).toInt())
    }

    fun look(sp: Species, stage: Int = 2, mood: Mood = Mood.CALM) = Look(sp.kind, sp.hue, sp.sat, sp.lit, stage, mood, if (stage == 3) sp.bloom else null, 0)

    val alive get() = g.alive
    val needsCheck get() = alive.filter { status(it).checkIn == 0 && !status(it).rest }
    val fights get() = g.fights.filter { it.won == null && g.plant(it.plant)?.alive == true }
    val caughtSpecies: Set<String> by lazy { g.plants.map { it.species }.toSet() }
    val seenSpecies: Set<String> by lazy { g.sightings.map { it.species }.toSet() }
}
