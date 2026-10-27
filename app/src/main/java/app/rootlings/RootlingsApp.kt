package app.rootlings

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import app.rootlings.data.Content
import app.rootlings.data.DemoSeed
import app.rootlings.data.Garden
import app.rootlings.data.GardenDb
import app.rootlings.data.SettingsStore
import app.rootlings.domain.Weather
import app.rootlings.work.Daily
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppContainer(context: Context) {
    val app = context.applicationContext
    val db = GardenDb.create(app)
    val settings = SettingsStore(app)
    val content = Content.load(app)
    val garden = Garden(db.dao())
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Fire-and-forget so a write survives the screen being closed right after tapping. */
    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch { block() }
    }

    /** Fetches the hourly weather if a town is set and the last fetch is over an hour old. */
    suspend fun refreshWeather(force: Boolean = false) {
        val s = settings.now()
        val p = s.place ?: return
        if (!force && System.currentTimeMillis() - s.weatherAt < 60 * 60 * 1000L) return
        val days = runCatching { withContext(Dispatchers.IO) { Weather.fetch(p.lat, p.lon) } }.getOrNull() ?: return
        garden.saveWeather(days)
        settings.update { it.copy(weatherAt = System.currentTimeMillis()) }
    }
}

class RootlingsApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        if (BuildConfig.DEMO) container.scope.launch { DemoSeed.seedIfEmpty(this@RootlingsApp, container) }
        runCatching { Daily.schedule(this) }
    }
}

val Context.container: AppContainer get() = (applicationContext as RootlingsApp).container

/** Resources in the in-app language, which AppCompat doesn't localise outside activities before Android 13. */
fun Context.localized(): Context {
    val tags = AppCompatDelegate.getApplicationLocales()
    if (tags.isEmpty) return this
    val config = Configuration(resources.configuration)
    config.setLocale(tags[0])
    return createConfigurationContext(config)
}

val Context.ru: Boolean get() = resources.configuration.locales[0].language == "ru"
