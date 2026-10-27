package app.rootlings.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.rootlings.R
import app.rootlings.container
import app.rootlings.data.Garden
import app.rootlings.data.GardenState
import app.rootlings.data.Settings
import app.rootlings.ru
import app.rootlings.ui.art.Icons
import app.rootlings.ui.battle.BattleScreen
import app.rootlings.ui.capture.CatchFlow
import app.rootlings.ui.components.BufImage
import app.rootlings.ui.dex.DexScreen
import app.rootlings.ui.dex.HerbariumPage
import app.rootlings.ui.dex.SpeciesPage
import app.rootlings.ui.nursery.NurseryScreen
import app.rootlings.ui.onboarding.Onboarding
import app.rootlings.ui.plant.PlantPage
import app.rootlings.ui.settings.SettingsPage
import app.rootlings.ui.settings.VacationPage
import app.rootlings.ui.settings.WindowsPage
import app.rootlings.ui.sill.SillScreen
import app.rootlings.ui.theme.C
import app.rootlings.ui.theme.T
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime

enum class Tab { SILL, DEX, NURSERY, BATTLE }

sealed interface Overlay {
    data class Plant(val id: Long) : Overlay
    data class Species(val id: String) : Overlay
    data class Catch(val sill: Long? = null) : Overlay
    data object Settings : Overlay
    data object Windows : Overlay
    data object Vacation : Overlay
    data object Herbarium : Overlay
}

/** What the screens can ask the root to open. */
class Nav(val push: (Overlay) -> Unit, val pop: () -> Unit, val tab: (Tab) -> Unit)

@Composable
fun RootlingsRoot(settings: Settings) {
    val ctx = LocalContext.current
    val c = ctx.container
    val state: GardenState? by c.garden.state.collectAsStateWithLifecycle(initialValue = null)
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = LocalDateTime.now() } }
    LifecycleResumeEffect(Unit) {
        now = LocalDateTime.now()
        val job = c.scope.launch { runCatching { c.refreshWeather() } }
        onPauseOrDispose { job.cancel() }
    }
    if (!settings.onboarded) { Onboarding(); return }
    val g = state ?: run { Box(Modifier.fillMaxSize().background(C.bg)); return }
    val home = remember(g, settings, now.hour, now.minute / 10, ctx.ru) {
        Home(g, c.content, settings, Garden.sky(g, settings.place?.lat, settings.place?.lon), now, ctx.ru)
    }

    var tab by remember { mutableStateOf(Tab.SILL) }
    val overlays = remember { mutableStateListOf<Overlay>() }
    val nav = remember { Nav(push = { overlays.add(it) }, pop = { if (overlays.isNotEmpty()) overlays.removeAt(overlays.lastIndex) }, tab = { tab = it }) }
    BackHandler(enabled = overlays.isEmpty() && tab != Tab.SILL) { tab = Tab.SILL }

    Box(Modifier.fillMaxSize().background(C.bg)) {
        Column(Modifier.fillMaxSize()) {
            Header(onSettings = { nav.push(Overlay.Settings) })
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    Tab.SILL -> SillScreen(home, nav)
                    Tab.DEX -> DexScreen(home, nav)
                    Tab.NURSERY -> NurseryScreen(home, nav)
                    Tab.BATTLE -> BattleScreen(home, nav)
                }
            }
            TabBar(tab, badge = home.fights.isNotEmpty()) { tab = it }
        }
        for (o in overlays) {
            key(o) {
                when (o) {
                    is Overlay.Plant -> PlantPage(home, o.id, nav)
                    is Overlay.Species -> SpeciesPage(home, o.id, nav)
                    is Overlay.Catch -> CatchFlow(home, o.sill, nav)
                    Overlay.Settings -> SettingsPage(home, nav)
                    Overlay.Windows -> WindowsPage(home, nav)
                    Overlay.Vacation -> VacationPage(home, nav)
                    Overlay.Herbarium -> HerbariumPage(home, nav)
                }
            }
        }
    }
}

@Composable
private fun key(o: Overlay, content: @Composable () -> Unit) = androidx.compose.runtime.key(o) { content() }

/** The red case of the field guide: a blue lens, three lights, the name. */
@Composable
private fun Header(onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(C.red).statusBarsPadding().drawBehind {
            drawRect(C.redDeep, Offset(0f, size.height - 4.dp.toPx()), Size(size.width, 4.dp.toPx()))
        }.padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Lens()
        Spacer(Modifier.width(8.dp))
        Row(Modifier.padding(bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            for (col in listOf(Color(0xFFF05040), Color(0xFFF8D030), Color(0xFF58C058))) Box(Modifier.size(8.dp).background(col))
        }
        Spacer(Modifier.weight(1f))
        Text(stringResource(R.string.app_title), style = T.Display.copy(color = C.paper, shadow = androidx.compose.ui.graphics.Shadow(C.redDeep, Offset(4f, 4f), 0f)))
        Box(Modifier.size(44.dp).clickable(onClick = onSettings), contentAlignment = Alignment.Center) { BufImage(Icons.tab("gear"), scale = 2.dp) }
    }
}

@Composable
private fun Lens() {
    Box(Modifier.size(38.dp).drawBehind {
        val p = size.width / 19
        fun r(x: Int, y: Int, w: Int, h: Int, col: Color) = drawRect(col, Offset(x * p, y * p), Size(w * p, h * p))
        r(5, 0, 9, 19, C.paper); r(0, 5, 19, 9, C.paper); r(2, 2, 15, 15, C.paper)
        r(6, 2, 7, 15, C.blue); r(2, 6, 15, 7, C.blue); r(4, 4, 11, 11, C.blue)
        r(5, 5, 4, 3, Color(0xFFA8D0F8)); r(11, 12, 4, 3, Color(0xFF2858A8))
    })
}

@Composable
private fun TabBar(tab: Tab, badge: Boolean, onTab: (Tab) -> Unit) {
    val items = listOf(Tab.SILL to (R.string.tab_sill to "window"), Tab.DEX to (R.string.tab_dex to "dex"), Tab.NURSERY to (R.string.tab_nursery to "egg"), Tab.BATTLE to (R.string.tab_battle to "bug"))
    Row(Modifier.fillMaxWidth().background(C.ink).navigationBarsPadding().padding(horizontal = 4.dp, vertical = 6.dp)) {
        for ((t, v) in items) {
            val sel = t == tab
            Column(Modifier.weight(1f).clickable { onTab(t) }.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box {
                    BufImage(Icons.tab(v.second), Modifier.padding(horizontal = 6.dp).then(if (sel) Modifier else Modifier.drawBehind { }), scale = 2.dp)
                    if (t == Tab.BATTLE && badge) Box(Modifier.align(Alignment.TopEnd).size(8.dp).background(C.red))
                }
                Spacer(Modifier.height(2.dp))
                Text((if (sel) "▶" else "") + stringResource(v.first).uppercase(), style = T.Small.copy(color = if (sel) C.paper else C.faint))
            }
        }
    }
}
