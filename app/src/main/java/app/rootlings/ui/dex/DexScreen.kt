package app.rootlings.ui.dex

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.rootlings.R
import app.rootlings.container
import app.rootlings.data.Element
import app.rootlings.data.Mood
import app.rootlings.data.Photos
import app.rootlings.data.Plant
import app.rootlings.data.Species
import app.rootlings.ui.Home
import app.rootlings.ui.Nav
import app.rootlings.ui.Overlay
import app.rootlings.ui.Words
import app.rootlings.ui.art.Foes
import app.rootlings.ui.art.Spirit
import app.rootlings.ui.art.sepia
import app.rootlings.ui.art.silhouette
import app.rootlings.ui.components.Badge
import app.rootlings.ui.components.BufImage
import app.rootlings.ui.components.Chip
import app.rootlings.ui.components.Dialog
import app.rootlings.ui.components.Eyebrow
import app.rootlings.ui.components.Field
import app.rootlings.ui.components.Kind
import app.rootlings.ui.components.Page
import app.rootlings.ui.components.PixelButton
import app.rootlings.ui.components.Says
import app.rootlings.ui.components.pixelBox
import app.rootlings.ui.plant.Photo
import app.rootlings.ui.theme.C
import app.rootlings.ui.theme.T

private enum class Filter { ALL, CAUGHT, SEEN, OWN }

@Composable
fun DexScreen(home: Home, nav: Nav) {
    var filter by rememberSaveable { mutableStateOf(Filter.ALL) }
    var el by rememberSaveable { mutableStateOf<Element?>(null) }
    val ctx = LocalContext.current
    val list = home.content.species.filter { s ->
        (el == null || s.element == el) && when (filter) {
            Filter.ALL -> true
            Filter.CAUGHT -> s.id in home.caughtSpecies
            Filter.SEEN -> s.id in home.seenSpecies
            Filter.OWN -> s.own
        }
    }
    LazyVerticalGrid(GridCells.Adaptive(108.dp), Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(stringResource(R.string.dex_title), style = T.Display, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.dex_count, home.caughtSpecies.size, home.seenSpecies.size, home.content.species.size), style = T.Small)
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (f in Filter.entries) Chip(ctx.resources.getStringArray(R.array.dex_filters)[f.ordinal], f == filter, { filter = f })
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip(stringResource(R.string.all_types), el == null, { el = null })
                    for (e in Element.entries) Chip(Words.element(ctx, e), e == el, { el = if (el == e) null else e })
                }
                Text(stringResource(R.string.dex_legend), style = T.Small)
            }
        }
        items(list, key = { it.id }) { s -> Cell(home, s) { nav.push(Overlay.Species(s.id)) } }
        if (list.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { Says(stringResource(R.string.dex_empty), style = T.Body) }
        item(span = { GridItemSpan(maxLineSpan) }) { Defeated(home) }
        item(span = { GridItemSpan(maxLineSpan) }) {
            PixelButton(stringResource(R.string.herbarium), { nav.push(Overlay.Herbarium) }, Modifier.fillMaxWidth().padding(bottom = 20.dp), kind = Kind.GHOST)
        }
    }
}

/** The sprite a dex shows for a species: in colour if yours, sepia if seen, a shadow otherwise. */
fun dexSprite(home: Home, s: Species) = Spirit.draw(home.look(s, home.g.plants.filter { it.species == s.id }.maxOfOrNull { it.stage } ?: 2)).also {
    when {
        s.id in home.caughtSpecies -> Unit
        s.id in home.seenSpecies -> it.sepia()
        else -> it.silhouette()
    }
}

@Composable
private fun Cell(home: Home, s: Species, onClick: () -> Unit) {
    val known = s.id in home.caughtSpecies || s.id in home.seenSpecies
    Column(Modifier.pixelBox(C.paper).clickable(onClick = onClick).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth()) {
            Text(if (s.own) "★" else "№" + (home.content.species.indexOf(s) + 1).toString().padStart(3, '0'), style = T.Small, modifier = Modifier.weight(1f))
            Box(Modifier.padding(top = 3.dp).size(10.dp).background(C.ink).padding(2.dp).background(C.element(s.element)))
        }
        BufImage(remember(s.id, home.caughtSpecies, home.seenSpecies) { dexSprite(home, s) }, scale = 2.dp)
        Text(if (known || s.own) s.name(home.ru) else "? ? ?", style = T.Small.copy(color = C.ink), textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun Defeated(home: Home) {
    val won = home.g.fights.filter { it.won != null }.groupBy { it.pest }
    if (won.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Eyebrow(stringResource(R.string.defeated))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((id, fs) in won) {
                val pest = home.content.pest(id) ?: continue
                Column(Modifier.pixelBox(C.paper).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    BufImage(remember(id) { Foes.draw(pest.sprite) }, scale = 2.dp)
                    Text(pest.nick(home.ru), style = T.Small.copy(color = C.ink))
                    Text("×" + fs.size, style = T.Small)
                }
            }
        }
    }
}

@Composable
fun SpeciesPage(home: Home, id: String, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val s = home.content.species(id) ?: run { nav.pop(); return }
    var seeing by remember { mutableStateOf(false) }
    val mine = home.g.plants.filter { it.species == id }
    Page(s.name(home.ru), nav.pop) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.pixelBox(C.screen, bevel = null).padding(8.dp)) { BufImage(remember(id, home.caughtSpecies, home.seenSpecies) { dexSprite(home, s) }, scale = 4.dp) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(s.name(home.ru).uppercase(), style = T.Title)
                if (s.latin.isNotBlank()) Text(s.latin, style = T.Small)
                Badge(Words.element(ctx, s.element), C.element(s.element))
                Text(Words.kind(ctx, s.kind), style = T.Small)
                Text(stringResource(when { s.id in home.caughtSpecies -> R.string.dex_caught; s.id in home.seenSpecies -> R.string.dex_seen; else -> R.string.dex_unknown }), style = T.Small.copy(color = C.ink))
            }
        }
        if (s.tip(home.ru).isNotBlank()) Says(s.tip(home.ru), style = T.Body)
        Eyebrow(stringResource(R.string.care))
        Fact(stringResource(R.string.light), stringResource(R.string.light_range, Words.num(s.dliMin), Words.num(s.dliMax)))
        Fact(stringResource(R.string.water), ctx.resources.getStringArray(R.array.water_at).let { a -> a[when { s.waterAt <= 12 -> 0; s.waterAt <= 30 -> 1; s.waterAt <= 45 -> 2; else -> 3 }] })
        if (s.humid) Fact(stringResource(R.string.air), stringResource(R.string.air_humid))
        if (s.rest) Fact(stringResource(R.string.winter), stringResource(R.string.winter_rest))
        Fact(stringResource(R.string.pets), stringResource(if (s.toxic) R.string.pets_toxic else R.string.pets_ok))
        Fact(stringResource(R.string.propagation), ctx.resources.getStringArray(R.array.propagation)[s.cutting.ordinal])
        Eyebrow(stringResource(R.string.forms))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            val top = mine.maxOfOrNull { it.stage } ?: 0
            for (st in 1..3) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BufImage(remember(id, st, top) { Spirit.draw(home.look(s, st)).also { if (st > top) it.silhouette() } }, scale = 2.dp)
                Text(if (st > top) "???" else s.forms[st - 1](home.ru).uppercase(), style = T.Small)
            }
        }
        if (mine.isNotEmpty()) {
            Eyebrow(stringResource(R.string.yours))
            for (p in mine) Row(Modifier.fillMaxWidth().pixelBox(C.paper).clickable { if (p.alive) nav.push(Overlay.Plant(p.id)) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                BufImage(remember(p.id, p.stage) { Spirit.draw(home.look(p)).also { if (!p.alive) it.sepia() } }, scale = 1.5.dp)
                Text(p.name.uppercase(), style = T.Heading, modifier = Modifier.weight(1f).padding(start = 8.dp))
                Text(Words.age(ctx, p, home.nowMs), style = T.Small)
            }
        }
        val seen = home.g.sightings.filter { it.species == id }
        if (seen.isNotEmpty()) {
            Eyebrow(stringResource(R.string.sightings))
            for (v in seen) Column(Modifier.fillMaxWidth().pixelBox(C.paper).padding(8.dp)) {
                Text(v.place.ifBlank { "—" }, style = T.Body)
                Text(Words.dateMs(ctx, v.at), style = T.Small)
                v.photo?.let { Photo(it, Modifier.fillMaxWidth().height(160.dp)) }
            }
        }
        PixelButton(stringResource(R.string.saw_it), { seeing = true }, Modifier.fillMaxWidth(), kind = Kind.GHOST)
        PixelButton(stringResource(R.string.catch_this), { nav.push(Overlay.Catch()) }, Modifier.fillMaxWidth())
    }
    if (seeing) SawDialog(home, s, onCancel = { seeing = false }) { place, photo -> c.write { garden.saw(id, place, photo) }; seeing = false }
}

@Composable
private fun Fact(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = T.Small, modifier = Modifier.weight(.4f))
        Text(value, style = T.Body, modifier = Modifier.weight(.6f))
    }
}

@Composable
private fun SawDialog(home: Home, s: Species, onCancel: () -> Unit, onSave: (String, String?) -> Unit) {
    val ctx = LocalContext.current
    var place by remember { mutableStateOf("") }
    var photo by rememberSaveable { mutableStateOf<String?>(null) }
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) photo = pending else Photos.delete(ctx, pending); pending = null }
    Dialog(onCancel) {
        Text(stringResource(R.string.saw_q, s.name(home.ru)), style = T.Say)
        Field(place, { place = it.take(40) }, stringResource(R.string.saw_where))
        photo?.let { Photo(it, Modifier.fillMaxWidth().height(140.dp)) }
        PixelButton(stringResource(R.string.catch_shoot), { val (f, uri) = Photos.fresh(ctx); pending = f.name; runCatching { camera.launch(uri) } }, Modifier.fillMaxWidth(), kind = Kind.GHOST)
        PixelButton(stringResource(R.string.save), { onSave(place, photo) }, Modifier.fillMaxWidth())
    }
}

/** The pressed pages of plants that died: kept, not mourned. */
@Composable
fun HerbariumPage(home: Home, nav: Nav) {
    val ctx = LocalContext.current
    val dead = home.g.plants.filter { it.goneHow == Plant.DIED }
    val given = home.g.plants.filter { it.goneHow == Plant.GIVEN }
    Page(stringResource(R.string.herbarium), nav.pop) {
        Says(stringResource(R.string.herbarium_intro), style = T.Body)
        if (dead.isEmpty()) Text(stringResource(R.string.herbarium_empty), style = T.Small)
        for (p in dead) {
            val sp = home.species(p)
            val notes = home.g.notes(p.id)
            Row(Modifier.fillMaxWidth().pixelBox(rgbPaper).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                BufImage(remember(p.id) { Spirit.draw(home.look(p, Mood.NIGHT)).also { it.sepia() } }, scale = 2.dp)
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(p.name.uppercase(), style = T.Heading)
                    Text(sp.name(home.ru) + " · " + sp.forms[p.stage - 1](home.ru), style = T.Small)
                    Text(Words.dateMs(ctx, p.caught) + " — " + Words.dateMs(ctx, p.gone ?: p.caught), style = T.Small)
                    Text(stringResource(R.string.herbarium_stats, notes.count { it.type == "water" }, p.growth), style = T.Small)
                }
            }
        }
        if (given.isNotEmpty()) {
            Eyebrow(stringResource(R.string.given_away))
            for (p in given) Text(p.name + " · " + home.species(p).name(home.ru) + " · " + Words.dateMs(ctx, p.gone ?: 0), style = T.Body)
        }
    }
}

private val rgbPaper = androidx.compose.ui.graphics.Color(0xFFF2E8CC)
