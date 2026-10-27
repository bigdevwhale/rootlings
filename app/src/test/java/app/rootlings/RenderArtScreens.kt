package app.rootlings

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import app.rootlings.data.DemoSeed
import app.rootlings.data.Garden
import app.rootlings.ui.Home
import app.rootlings.ui.Nav
import app.rootlings.ui.Overlay
import app.rootlings.ui.art.Art
import app.rootlings.ui.battle.BattleScreen
import app.rootlings.ui.capture.CatchFlow
import app.rootlings.ui.dex.DexScreen
import app.rootlings.ui.dex.SpeciesPage
import app.rootlings.ui.nursery.NurseryScreen
import app.rootlings.ui.onboarding.Onboarding
import app.rootlings.ui.plant.PlantPage
import app.rootlings.ui.sill.SillScreen
import app.rootlings.ui.theme.C
import app.rootlings.ui.theme.RootlingsTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDateTime

/** Every main screen over the demo garden, drawn to PNGs (run with `-Prender`), in both languages. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }
    private val nav = Nav({}, {}, {})

    @Before fun still() { Art.still = true }

    private fun home(hour: Int = 13): Home = runBlocking {
        val app = ApplicationProvider.getApplicationContext<RootlingsApp>()
        val c = app.container
        DemoSeed.seedIfEmpty(app, c)
        val g = c.garden.state.first()
        val s = c.settings.now()
        Home(g, c.content, s, Garden.sky(g, s.place?.lat, s.place?.lon), LocalDateTime.now().withHour(hour).withMinute(10), app.ru)
    }

    private fun shot(name: String, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { RootlingsTheme { Box(Modifier.fillMaxSize().background(C.bg)) { content() } } }
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(500)
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun Home.id(sp: String) = g.plants.first { it.species == sp }.id

    @Test fun welcome() = shot("0-welcome") { Onboarding() }
    @Test fun sill() = home().let { h -> shot("1-sill") { SillScreen(h, nav) } }
    @Test fun plant() = home().let { h -> shot("2-plant") { PlantPage(h, h.id("monstera_deliciosa"), nav) } }
    @Test fun dex() = home().let { h -> shot("3-dex") { DexScreen(h, nav) } }
    @Test fun nursery() = home().let { h -> shot("4-nursery") { NurseryScreen(h, nav) } }
    @Test fun battle() = home().let { h -> shot("5-battle") { BattleScreen(h, nav) } }
    @Test fun catching() = home().let { h -> shot("6-catch") { CatchFlow(h, null, nav) } }
    @Test fun species() = home().let { h -> shot("7-species") { SpeciesPage(h, "goeppertia_orbifolia", nav) } }
    @Test fun night() = home(22).let { h -> shot("8-night") { SillScreen(h, nav) } }
}

@Config(sdk = [34], qualifiers = "en-w411dp-h891dp-xxhdpi")
class RenderArtScreensEn : ScreensBase() { override val lang = "en" }

@Config(sdk = [34], qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderArtScreensRu : ScreensBase() { override val lang = "ru" }
