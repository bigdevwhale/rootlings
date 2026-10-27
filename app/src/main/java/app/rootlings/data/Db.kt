package app.rootlings.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** A window the plants stand at. [azimuth] is the way it looks out, degrees from north clockwise. */
@Entity
data class Sill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val azimuth: Int,
    /** What is outside: 0 open sky, 1 trees or houses across the way, 2 a wall close by. */
    val view: Int = 0,
    val order: Int = 0,
)

/**
 * A real plant and its spirit. [growth] counts new leaves (or pads, fronds…) since it was caught;
 * [moist] is the soil moisture estimate (0..100) as of [moistAt]; [dryK] is how much faster or
 * slower than the book its pot dries, learnt from finger checks.
 */
@Entity
data class Plant(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val species: String,
    val name: String,
    val sill: Long,
    /** 0 on the sill, 1 about a metre from the glass, 2 deeper in the room. */
    val spot: Int = 0,
    val caught: Long,
    val hue: Int,
    val growth: Int = 0,
    val stage: Int = 1,
    val photo: String? = null,
    val photoAt: Long = 0,
    val parent: Long? = null,
    /** The ancestors, nearest first, as names: "Усач ← хлорофитум бабушки Зои". */
    val lineage: String = "",
    /** When it died or was given away, and which. */
    val gone: Long? = null,
    val goneHow: Int = 0,
    val dryK: Double = 1.0,
    val moist: Double = 60.0,
    val moistAt: Long,
    /** The last finger check said the soil is still wet. */
    val soggyAt: Long = 0,
    val bloomAt: Long = 0,
    val order: Int = 0,
) {
    val alive get() = gone == null

    companion object {
        const val DIED = 1
        const val GIVEN = 2
    }
}

/** One thing that happened to a plant: a check, a watering, a photo, a new form… */
@Entity
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plant: Long,
    val at: Long,
    val type: String,
    val value: Int = 0,
    val text: String = "",
    val photo: String? = null,
) {
    companion object {
        const val CAUGHT = "caught"
        const val CHECK = "check"
        const val WATER = "water"
        const val MOLT = "molt"
        const val LEAF = "leaf"
        const val BLOOM = "bloom"
        const val EVOLVE = "evolve"
        const val FIGHT = "fight"
        const val WON = "won"
        const val CUTTING = "cutting"
        const val HATCH = "hatch"
        const val MOVED = "moved"
        const val GONE = "gone"
    }
}

/** A battle against a pest: [step] moves made so far, the last at [stepAt]. */
@Entity
data class Fight(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plant: Long,
    val pest: String,
    val started: Long,
    val step: Int = 0,
    val stepAt: Long = 0,
    val won: Long? = null,
)

/** A cutting rooting in water or soil: an egg. */
@Entity
data class Cutting(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parent: Long,
    val species: String,
    val started: Long,
    val medium: String,
    val hatched: Long? = null,
    val child: Long? = null,
    /** Given away as a code instead of hatching here. */
    val given: Boolean = false,
)

/** A plant seen somewhere (a friend's, a café's, the botanical garden's), for the dex. */
@Entity
data class Sighting(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val species: String,
    val at: Long,
    val place: String,
    val photo: String? = null,
)

/** A species the user described themselves, because the catalogue doesn't have it. */
@Entity
data class OwnSpecies(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latin: String = "",
    val kind: String,
    val element: String,
    /** 0 low light, 1 medium, 2 bright, 3 full sun. */
    val light: Int,
    /** 0 let it dry out, 1 top half dry, 2 top few cm dry, 3 keep evenly moist. */
    val water: Int,
    val humid: Boolean,
    val toxic: Boolean,
    val rest: Boolean,
    val bloom: Int? = null,
    val hue: Int,
    val cutting: String = "SOIL",
) {
    val key get() = "own_$id"

    fun species(): Species {
        val dli = when (light) { 0 -> 1.5 to 5.0; 1 -> 3.0 to 10.0; 2 -> 6.0 to 16.0; else -> 12.0 to 30.0 }
        val (dry, at) = when (water) { 0 -> 4.0 to 10; 1 -> 7.0 to 25; 2 -> 9.0 to 40; else -> 11.0 to 55 }
        return Species(
            id = key, kind = Kind.of(kind), element = Element.of(element), latin = latin, name = Tx(name, name),
            dliMin = dli.first, dliMax = dli.second, dry = dry, waterAt = at, humid = humid, toxic = toxic, rest = rest,
            bloom = bloom, hue = hue, sat = 42, lit = 42, s2 = 4, s3 = 10, cutting = Propagation.of(cutting), rootDays = 21,
            forms = listOf(Tx("Sprout", "Росток"), Tx("Grown", "Подросток"), Tx("Elder", "Старожил")), tip = Tx("", ""), own = true,
        )
    }
}

/** The hourly weather of one local day, kept for the light and the heating season. */
@Entity
data class Day(
    @PrimaryKey val day: Long,
    /** "temp;rh;cloud;direct;diffuse;precip" rows for hours 0..23, comma-separated values. */
    val hours: String,
    val mean: Double,
    val at: Long,
    /** Measured (true) or the app's own seasonal guess. */
    val real: Boolean,
)

@Dao
interface GardenDao {
    @Query("SELECT * FROM Sill ORDER BY `order`, id") fun sills(): Flow<List<Sill>>
    @Query("SELECT * FROM Plant ORDER BY `order`, id") fun plants(): Flow<List<Plant>>
    @Query("SELECT * FROM Note ORDER BY at DESC") fun notes(): Flow<List<Note>>
    @Query("SELECT * FROM Fight ORDER BY started DESC") fun fights(): Flow<List<Fight>>
    @Query("SELECT * FROM Cutting ORDER BY started DESC") fun cuttings(): Flow<List<Cutting>>
    @Query("SELECT * FROM Sighting ORDER BY at DESC") fun sightings(): Flow<List<Sighting>>
    @Query("SELECT * FROM OwnSpecies ORDER BY id") fun own(): Flow<List<OwnSpecies>>
    @Query("SELECT * FROM Day ORDER BY day") fun days(): Flow<List<Day>>

    @Query("SELECT * FROM Plant WHERE id = :id") suspend fun plant(id: Long): Plant?
    @Query("SELECT * FROM Plant") suspend fun allPlants(): List<Plant>
    @Query("SELECT * FROM Sill") suspend fun allSills(): List<Sill>
    @Query("SELECT * FROM Day") suspend fun allDays(): List<Day>
    @Query("SELECT * FROM Fight") suspend fun allFights(): List<Fight>
    @Query("SELECT COUNT(*) FROM Sill") suspend fun sillCount(): Int

    @Insert suspend fun insert(s: Sill): Long
    @Insert suspend fun insert(p: Plant): Long
    @Insert suspend fun insert(n: Note): Long
    @Insert suspend fun insert(f: Fight): Long
    @Insert suspend fun insert(c: Cutting): Long
    @Insert suspend fun insert(s: Sighting): Long
    @Insert suspend fun insert(o: OwnSpecies): Long
    @Upsert suspend fun upsert(d: Day)

    @Update suspend fun update(s: Sill)
    @Update suspend fun update(p: Plant)
    @Update suspend fun update(f: Fight)
    @Update suspend fun update(c: Cutting)
    @Update suspend fun update(o: OwnSpecies)

    @Query("DELETE FROM Sill WHERE id = :id") suspend fun deleteSill(id: Long)
    @Query("DELETE FROM Fight WHERE id = :id") suspend fun deleteFight(id: Long)
    @Query("DELETE FROM Sighting WHERE id = :id") suspend fun deleteSighting(id: Long)
    @Query("DELETE FROM Cutting WHERE id = :id") suspend fun deleteCutting(id: Long)
    @Query("DELETE FROM Day WHERE day < :before") suspend fun pruneDays(before: Long)
}

@Database(entities = [Sill::class, Plant::class, Note::class, Fight::class, Cutting::class, Sighting::class, OwnSpecies::class, Day::class], version = 1, exportSchema = true)
abstract class GardenDb : RoomDatabase() {
    abstract fun dao(): GardenDao

    companion object {
        fun create(ctx: Context): GardenDb = Room.databaseBuilder(ctx, GardenDb::class.java, "garden.db").build()
    }
}
