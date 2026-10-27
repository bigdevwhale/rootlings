package app.rootlings.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.rootlings.domain.Place
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class Settings(
    val onboarded: Boolean = false,
    /** The town the weather comes from; null keeps the app's own seasonal weather. */
    val place: Place? = null,
    val weatherAt: Long = 0,
    /** The keeper's name, signed on gifts. */
    val keeper: String = "",
    /** A gentle morning note when soil is worth checking. */
    val notify: Boolean = true,
    val notifiedDay: Long = -1,
    /** Cats or dogs at home: toxic plants get a mark. */
    val pets: Boolean = false,
    /** The heating-season notice was shown for this autumn (the year). */
    val heatingSeen: Int = 0,
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read(): Settings {
        val lat = this[LAT]
        val lon = this[LON]
        return Settings(
            onboarded = this[ONBOARDED] ?: false,
            place = if (lat != null && lon != null) Place(this[PLACE] ?: "", this[REGION] ?: "", lat, lon) else null,
            weatherAt = this[WEATHER_AT] ?: 0,
            keeper = this[KEEPER] ?: "",
            notify = this[NOTIFY] ?: true,
            notifiedDay = this[NOTIFIED] ?: -1,
            pets = this[PETS] ?: false,
            heatingSeen = (this[HEATING] ?: 0L).toInt(),
        )
    }

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            val p = s.place
            if (p != null) {
                it[PLACE] = p.name; it[REGION] = p.region; it[LAT] = p.lat; it[LON] = p.lon
            } else {
                it.remove(PLACE); it.remove(REGION); it.remove(LAT); it.remove(LON)
            }
            it[WEATHER_AT] = s.weatherAt
            it[KEEPER] = s.keeper
            it[NOTIFY] = s.notify
            it[NOTIFIED] = s.notifiedDay
            it[PETS] = s.pets
            it[HEATING] = s.heatingSeen.toLong()
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val PLACE = stringPreferencesKey("place")
        val REGION = stringPreferencesKey("region")
        val LAT = doublePreferencesKey("lat")
        val LON = doublePreferencesKey("lon")
        val WEATHER_AT = longPreferencesKey("weather_at")
        val KEEPER = stringPreferencesKey("keeper")
        val NOTIFY = booleanPreferencesKey("notify")
        val NOTIFIED = longPreferencesKey("notified")
        val PETS = booleanPreferencesKey("pets")
        val HEATING = longPreferencesKey("heating_seen")
    }
}
