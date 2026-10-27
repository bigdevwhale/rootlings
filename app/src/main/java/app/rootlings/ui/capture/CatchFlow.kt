package app.rootlings.ui.capture

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.rootlings.R
import app.rootlings.container
import app.rootlings.data.Element
import app.rootlings.data.Kind
import app.rootlings.data.Mood
import app.rootlings.data.OwnSpecies
import app.rootlings.data.Photos
import app.rootlings.data.Species
import app.rootlings.ui.Home
import app.rootlings.ui.Nav
import app.rootlings.ui.Overlay
import app.rootlings.ui.Words
import app.rootlings.ui.art.Look
import app.rootlings.ui.art.Spirit
import app.rootlings.ui.components.BufImage
import app.rootlings.ui.components.Chip
import app.rootlings.ui.components.Dialog
import app.rootlings.ui.components.Eyebrow
import app.rootlings.ui.components.Field
import app.rootlings.ui.components.Kind as BtnKind
import app.rootlings.ui.components.Menu
import app.rootlings.ui.components.MenuItem
import app.rootlings.ui.components.Page
import app.rootlings.ui.components.PixelButton
import app.rootlings.ui.components.Says
import app.rootlings.ui.components.pixelBox
import app.rootlings.ui.plant.Photo
import app.rootlings.ui.theme.C
import app.rootlings.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Catching a plant: a photo with the phone's own camera (optional), the species from the catalogue
 * or one described by hand, then where it stands and what its soil is like today.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CatchFlow(home: Home, sillId: Long?, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val scope = rememberCoroutineScope()
    var photo by rememberSaveable { mutableStateOf<String?>(null) }
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    var speciesId by rememberSaveable { mutableStateOf<String?>(null) }
    var own by rememberSaveable { mutableStateOf(false) }
    var hue by rememberSaveable { mutableStateOf<Int?>(null) }
    var caught by remember { mutableStateOf<Long?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) { photo = pending; val f = pending; scope.launch { hue = withContext(Dispatchers.IO) { f?.let { Photos.hue(ctx, it) } } } } else Photos.delete(ctx, pending)
        pending = null
    }
    val sp = speciesId?.let { home.content.species(it) }

    Page(stringResource(R.string.catch_title), nav.pop) {
        // 1. the photo
        Eyebrow(stringResource(R.string.catch_photo))
        if (photo != null) Photo(photo!!, Modifier.fillMaxWidth().height(220.dp))
        Text(stringResource(R.string.catch_photo_hint), style = T.Small)
        PixelButton(stringResource(if (photo == null) R.string.catch_shoot else R.string.catch_reshoot), {
            val (f, uri) = Photos.fresh(ctx); pending = f.name; runCatching { camera.launch(uri) }
        }, Modifier.fillMaxWidth(), kind = if (photo == null) BtnKind.GREEN else BtnKind.GHOST)

        // 2. who it is
        Eyebrow(stringResource(R.string.catch_who))
        when {
            own -> OwnForm(home, hue) { o -> c.write { val id = garden.addOwn(o); speciesId = id }; own = false }
            sp == null -> SpeciesSearch(home, onPick = { speciesId = it.id }, onOwn = { own = true })
            else -> Row(Modifier.fillMaxWidth().pixelBox(C.paper).clickable { speciesId = null }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                BufImage(remember(sp.id, hue) { Spirit.draw(home.look(sp).copy(hue = hue ?: sp.hue)) }, scale = 2.dp)
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(sp.name(home.ru), style = T.Heading)
                    Text(sp.latin + " · " + Words.element(ctx, sp.element), style = T.Small)
                }
                Text(stringResource(R.string.change), style = T.Small.copy(color = C.red))
            }
        }

        // 3. where and how
        if (sp != null && !own) {
            var name by remember(sp.id) { mutableStateOf("") }
            var sill by remember { mutableStateOf(sillId ?: home.g.sills.firstOrNull()?.id ?: 0L) }
            var spot by remember { mutableIntStateOf(0) }
            var soil by remember { mutableIntStateOf(1) }
            Eyebrow(stringResource(R.string.catch_name))
            Field(name, { name = it.take(16) }, stringResource(R.string.name_hint))
            Eyebrow(stringResource(R.string.catch_where))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (s in home.g.sills) Chip(s.name, s.id == sill, { sill = s.id })
                Chip("+", false, { nav.push(Overlay.Windows) })
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 0..2) Chip(Words.spot(ctx, i), i == spot, { spot = i })
            }
            Eyebrow(stringResource(R.string.catch_soil))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ctx.resources.getStringArray(R.array.readings).forEachIndexed { i, s -> Chip(s, i == soil, { soil = i }) }
            }
            PixelButton(stringResource(R.string.catch_go), {
                val n = name.trim().ifBlank { sp.name(home.ru).substringBefore(' ') }
                val ph = photo
                scope.launch {
                    caught = c.garden.catchPlant(sp.id, n, sill, spot, hue ?: sp.hue, ph, app.rootlings.domain.Care.reading(soil))
                }
            }, Modifier.fillMaxWidth(), kind = BtnKind.RED, enabled = home.g.sills.any { it.id == sill })
        }
    }

    caught?.let { id ->
        val p = home.g.plant(id)
        Dialog({ nav.pop() }) {
            if (p != null && sp != null) {
                Box(Modifier.fillMaxWidth().background(C.screen).padding(10.dp), contentAlignment = Alignment.Center) {
                    BufImage(remember(id) { Spirit.draw(home.look(sp).copy(hue = p.hue, stage = 1, mood = Mood.NEW, pot = (id % 4).toInt())) }, scale = 4.dp)
                }
                Text(stringResource(R.string.caught, p.name), style = T.Say)
                home.content.line(Mood.NEW, sp.element, id.toInt())?.let { Text(it(home.ru), style = T.Body) }
            } else Text("…", style = T.Say)
            Menu(listOf(MenuItem(stringResource(R.string.hooray)) { nav.pop(); if (p != null) nav.push(Overlay.Plant(p.id)) }))
        }
    }
}

/** The catalogue, searchable by any name; plus a way out for a plant it doesn't know. */
@Composable
fun SpeciesSearch(home: Home, onPick: (Species) -> Unit, onOwn: (() -> Unit)?) {
    var q by remember { mutableStateOf("") }
    Field(q, { q = it }, stringResource(R.string.search_hint))
    val ql = q.trim().lowercase()
    val found = if (ql.isEmpty()) home.content.species.take(8) else home.content.species.filter {
        ql in it.name.en.lowercase() || ql in it.name.ru.lowercase() || ql in it.latin.lowercase()
    }.take(24)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (s in found) Row(Modifier.fillMaxWidth().pixelBox(C.paper).clickable { onPick(s) }.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            BufImage(remember(s.id) { Spirit.draw(home.look(s)) }, scale = 1.5.dp)
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(s.name(home.ru), style = T.Body)
                Text(s.latin, style = T.Small)
            }
        }
        if (ql.isNotEmpty() && found.isEmpty()) Text(stringResource(R.string.search_none), style = T.Small)
        if (onOwn != null) PixelButton(stringResource(R.string.own_add), onOwn, Modifier.fillMaxWidth(), kind = BtnKind.GHOST)
    }
}

private val BLOOMS = listOf(null, 0xFFF4F4E8, 0xFFF06080, 0xFFD83838, 0xFFF0A030, 0xFFF0D040, 0xFF9060C8, 0xFF5890E0).map { it?.toInt() }

/** Describing a species of one's own: what it looks like and what it needs, in plain choices. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OwnForm(home: Home, photoHue: Int?, onSave: (OwnSpecies) -> Unit) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf("") }
    var latin by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(Kind.BUSH) }
    var el by remember { mutableStateOf(Element.RAIN) }
    var light by remember { mutableIntStateOf(1) }
    var water by remember { mutableIntStateOf(2) }
    var humid by remember { mutableStateOf(false) }
    var toxic by remember { mutableStateOf(false) }
    var rest by remember { mutableStateOf(false) }
    var bloom by remember { mutableStateOf<Int?>(null) }
    val hue = photoHue ?: 110
    Column(Modifier.fillMaxWidth().pixelBox(C.paper2).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Says(stringResource(R.string.own_intro), style = T.Body)
        Field(name, { name = it.take(28) }, stringResource(R.string.own_name))
        Field(latin, { latin = it.take(40) }, stringResource(R.string.own_latin))
        Eyebrow(stringResource(R.string.own_look))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (k in Kind.entries) Box(Modifier.pixelBox(if (k == kind) C.screen else C.paper, pressed = k == kind).clickable { kind = k }.padding(2.dp)) {
                BufImage(remember(k, hue, bloom) { Spirit.draw(Look(k, hue, 42, 42, 2, flower = bloom)) }, scale = 1.5.dp)
            }
        }
        Text(Words.kind(ctx, kind), style = T.Small.copy(color = C.ink))
        Eyebrow(stringResource(R.string.own_element))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (e in Element.entries) Chip(Words.element(ctx, e), e == el, { el = e })
        }
        Eyebrow(stringResource(R.string.own_light))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ctx.resources.getStringArray(R.array.own_light).forEachIndexed { i, s -> Chip(s, i == light, { light = i }) }
        }
        Eyebrow(stringResource(R.string.own_water))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ctx.resources.getStringArray(R.array.own_water).forEachIndexed { i, s -> Chip(s, i == water, { water = i }) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip(stringResource(R.string.own_humid), humid, { humid = !humid })
            Chip(stringResource(R.string.own_toxic), toxic, { toxic = !toxic })
            Chip(stringResource(R.string.own_rest), rest, { rest = !rest })
        }
        Eyebrow(stringResource(R.string.own_bloom))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (b in BLOOMS) Box(Modifier.size(30.dp).pixelBox(b?.let { Color(it) } ?: C.paper, pressed = b == bloom).clickable { bloom = b }, contentAlignment = Alignment.Center) {
                if (b == null) Text("—", style = T.Small)
                if (b == bloom) Text("✓", style = T.Small.copy(color = C.ink))
            }
        }
        PixelButton(stringResource(R.string.own_save), {
            onSave(OwnSpecies(name = name.trim(), latin = latin.trim(), kind = kind.name, element = el.name, light = light, water = water,
                humid = humid, toxic = toxic, rest = rest, bloom = bloom, hue = hue))
        }, Modifier.fillMaxWidth(), enabled = name.isNotBlank())
    }
}
