package app.rootlings.ui

import android.content.Context
import app.rootlings.R
import app.rootlings.data.Element
import app.rootlings.data.Kind
import app.rootlings.data.Mood
import app.rootlings.data.Plant
import app.rootlings.domain.Status
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object Words {
    fun element(c: Context, e: Element): String = c.getString(
        when (e) {
            Element.DESERT -> R.string.el_desert
            Element.MIST -> R.string.el_mist
            Element.RAIN -> R.string.el_rain
            Element.MEADOW -> R.string.el_meadow
            Element.FOREST -> R.string.el_forest
        },
    )

    fun kind(c: Context, k: Kind): String = c.resources.getStringArray(R.array.kinds).getOrElse(k.ordinal) { k.name }

    /** The short line under a spirit's name on the sill. */
    fun status(c: Context, st: Status): String = c.getString(
        when (st.mood) {
            Mood.PEST -> R.string.st_pest
            Mood.REST -> R.string.st_rest
            Mood.NIGHT -> R.string.st_night
            Mood.SOGGY -> R.string.st_soggy
            Mood.DRY -> R.string.st_dry
            Mood.SCORCH -> R.string.st_scorch
            Mood.GLOOM -> R.string.st_gloom
            Mood.SUN -> R.string.st_sun
            Mood.HEAT -> R.string.st_heat
            else -> if (st.evolve) R.string.st_evolve else if (st.molt) R.string.st_molt else R.string.st_calm
        },
    )

    /** The direction a window looks, as a compass word. */
    fun compass(c: Context, az: Int): String = c.resources.getStringArray(R.array.compass)[Math.floorMod(((az + 22) / 45), 8)]

    fun num(v: Double): String = String.format(Locale.US, "%.1f", v).replace('.', ',')

    fun date(c: Context, d: LocalDate, today: LocalDate = LocalDate.now()): String {
        val l = c.resources.configuration.locales[0]
        return d.format(DateTimeFormatter.ofPattern(if (d.year == today.year) "d MMMM" else "d MMMM yyyy", l))
    }

    fun dateMs(c: Context, ms: Long): String = date(c, java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).toLocalDate())

    fun days(c: Context, n: Int): String = c.resources.getQuantityString(R.plurals.days, n, n)

    fun leaves(c: Context, n: Int): String = c.resources.getQuantityString(R.plurals.leaves, n, n)

    /** "Margo, 2 years with you" style age. */
    fun age(c: Context, p: Plant, now: Long): String {
        val d = ((now - p.caught) / 86_400_000L).toInt()
        return when {
            d < 1 -> c.getString(R.string.age_new)
            d < 60 -> days(c, d)
            d < 730 -> c.resources.getQuantityString(R.plurals.months, d / 30, d / 30)
            else -> c.resources.getQuantityString(R.plurals.years, d / 365, d / 365)
        }
    }

    fun spot(c: Context, s: Int) = c.resources.getStringArray(R.array.spots)[s.coerceIn(0, 2)]
    fun view(c: Context, s: Int) = c.resources.getStringArray(R.array.views)[s.coerceIn(0, 2)]
}
