package app.rootlings.data

import android.content.Context

/** A text in both languages. */
data class Tx(val en: String, val ru: String) {
    operator fun invoke(ru: Boolean) = if (ru) this.ru else en
}

enum class Element { DESERT, MIST, RAIN, MEADOW, FOREST;
    companion object { fun of(s: String) = entries.firstOrNull { it.name.equals(s, true) } ?: FOREST }
}

/** The sprite archetypes; every species is drawn as one of them. */
enum class Kind { MONSTERA, PHILO, FICUS, TREE, GRASS, ROSETTE, ALOE, PADDLE, COLUMN, BALL, FERN, PATTERN, BLADE, ZZ, PALM, CANE, ORCHID, SPATHE, VIOLET, BUSH, COIN, TRAIL, HERB, BULB, CARNIVORE;
    companion object { fun of(s: String) = entries.firstOrNull { it.name.equals(s, true) } ?: BUSH }
}

enum class Propagation { WATER, SOIL, LEAF, DIVISION, OFFSET, SEED, NONE;
    val cuttable get() = this != SEED && this != NONE
    companion object { fun of(s: String) = entries.firstOrNull { it.name.equals(s, true) } ?: NONE }
}

/**
 * What a species needs: light as a daily light integral (mol/m²/day), how fast its pot dries on an
 * ordinary day (% of moisture), and when to water (the moisture estimate it may fall to).
 */
data class Species(
    val id: String,
    val kind: Kind,
    val element: Element,
    val latin: String,
    val name: Tx,
    val dliMin: Double,
    val dliMax: Double,
    val dry: Double,
    val waterAt: Int,
    val humid: Boolean,
    val toxic: Boolean,
    val rest: Boolean,
    /** The flower colour, or null if it doesn't flower indoors in a way anyone notices. */
    val bloom: Int?,
    val hue: Int,
    val sat: Int,
    val lit: Int,
    val s2: Int,
    val s3: Int,
    val cutting: Propagation,
    val rootDays: Int,
    val forms: List<Tx>,
    val tip: Tx,
    /** A species the user described themselves. */
    val own: Boolean = false,
)

data class Symptom(val code: String, val name: Tx)

data class Move(val n: Int, val day: Int, val name: Tx, val how: Tx, val hit: Tx)

data class Pest(
    val id: String,
    val sprite: String,
    val name: Tx,
    val nick: Tx,
    val latin: String,
    val symptoms: List<String>,
    val likes: String,
    val level: Int,
    val about: Tx,
    val moves: List<Move>,
)

/** A spirit's mood, as far as its card goes. */
enum class Mood { CALM, SUN, GLOOM, DRY, SOGGY, NIGHT, REST, PEST, HEAT, SCORCH, NEW, EVOLVE, HATCH;
    companion object { fun of(s: String) = entries.firstOrNull { it.name.equals(s, true) } }
}

class Content(
    val species: List<Species>,
    val pests: List<Pest>,
    val symptoms: List<Symptom>,
    private val lines: Map<Pair<Mood, Element?>, List<Tx>>,
) {
    private val byId = species.associateBy { it.id }
    private val pestById = pests.associateBy { it.id }

    fun species(id: String): Species? = byId[id]
    fun pest(id: String): Pest? = pestById[id]

    /** What the spirit says: one of its element's lines for the mood, the same all day. */
    fun line(mood: Mood, el: Element, seed: Int): Tx? {
        val pool = lines[mood to el].orEmpty() + lines[mood to null].orEmpty()
        if (pool.isEmpty()) return null
        return pool[Math.floorMod(seed, pool.size)]
    }

    /** The pests these symptoms point to, the best match first. */
    fun diagnose(seen: Set<String>): List<Pest> =
        pests.map { it to it.symptoms.count { s -> s in seen } }.filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<Pest, Int>> { it.second }.thenBy { it.first.level }).map { it.first }

    /** The same content with the user's own species added. */
    fun with(own: List<Species>) = Content(species.filter { !it.own } + own, pests, symptoms, lines)

    companion object {
        private fun rows(ctx: Context, name: String): List<List<String>> =
            ctx.assets.open("content/$name").bufferedReader(Charsets.UTF_8).useLines { seq ->
                seq.filter { it.isNotBlank() && !it.startsWith("#") }.map { it.split('\t') }.toList()
            }

        fun species(r: List<String>): Species {
            val forms = r[21].split(',').map { it.trim() }
            val formsRu = r[22].split(',').map { it.trim() }
            return Species(
                id = r[0], kind = Kind.of(r[1]), element = Element.of(r[2]), latin = r[3], name = Tx(r[4], r[5]),
                dliMin = r[6].toDouble(), dliMax = r[7].toDouble(), dry = r[8].toDouble(), waterAt = r[9].toInt(),
                humid = r[10] == "1", toxic = r[11] == "1", rest = r[12] == "1",
                bloom = r[13].takeIf { it.startsWith("#") }?.let { (it.removePrefix("#").toLong(16) or 0xFF000000).toInt() },
                hue = r[14].toInt(), sat = r[15].toInt(), lit = r[16].toInt(), s2 = r[17].toInt(), s3 = r[18].toInt(),
                cutting = Propagation.of(r[19]), rootDays = r[20].toInt(),
                forms = (0 until 3).map { Tx(forms.getOrElse(it) { forms.last() }, formsRu.getOrElse(it) { formsRu.last() }) },
                tip = Tx(r[23], r[24]),
            )
        }

        fun load(ctx: Context): Content {
            val names = ctx.assets.list("content").orEmpty()
            val species = names.filter { it.startsWith("species") && it.endsWith(".tsv") }.sorted().flatMap { rows(ctx, it) }.map(::species)
                .sortedBy { it.latin }
            val moves = rows(ctx, "moves.tsv").groupBy { it[0] }.mapValues { (_, l) ->
                l.map { Move(it[1].toInt(), it[2].toInt(), Tx(it[3], it[4]), Tx(it[5], it[6]), Tx(it[7], it[8])) }.sortedBy { it.n }
            }
            val pests = rows(ctx, "pests.tsv").map {
                Pest(it[0], it[1], Tx(it[2], it[3]), Tx(it[4], it[5]), it[6], it[7].split(',').map(String::trim), it[8], it[9].toInt(), Tx(it[10], it[11]), moves[it[0]].orEmpty())
            }
            val symptoms = rows(ctx, "symptoms.tsv").map { Symptom(it[0], Tx(it[1], it[2])) }
            val lines = rows(ctx, "lines.tsv").mapNotNull { r ->
                val m = Mood.of(r[0]) ?: return@mapNotNull null
                Triple(m, if (r[1] == "*") null else Element.of(r[1]), Tx(r[2], r[3]))
            }.groupBy({ it.first to it.second }, { it.third })
            return Content(species, pests, symptoms, lines)
        }
    }
}
