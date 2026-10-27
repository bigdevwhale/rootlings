package app.rootlings.ui.plant

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.rootlings.R
import app.rootlings.container
import app.rootlings.data.Mood
import app.rootlings.data.Note
import app.rootlings.data.Photos
import app.rootlings.data.Plant
import app.rootlings.data.Propagation
import app.rootlings.ui.Home
import app.rootlings.ui.Nav
import app.rootlings.ui.Overlay
import app.rootlings.ui.Tab
import app.rootlings.ui.Words
import app.rootlings.ui.art.Spirit
import app.rootlings.ui.art.silhouette
import app.rootlings.ui.components.Badge
import app.rootlings.ui.components.Bar
import app.rootlings.ui.components.BufImage
import app.rootlings.ui.components.Chip
import app.rootlings.ui.components.Dialog
import app.rootlings.ui.components.Eyebrow
import app.rootlings.ui.components.Field
import app.rootlings.ui.components.Kind
import app.rootlings.ui.components.Menu
import app.rootlings.ui.components.MenuItem
import app.rootlings.ui.components.Page
import app.rootlings.ui.components.PixelButton
import app.rootlings.ui.components.Says
import app.rootlings.ui.components.Stepper
import app.rootlings.ui.components.pixelBox
import app.rootlings.ui.theme.C
import app.rootlings.ui.theme.T
import kotlinx.coroutines.delay

private enum class Ask { NONE, TOUCH, WATER_AFTER, EVOLVE, MOLT, SYMPTOMS, CUTTING, MOVE, NAME, GONE }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlantPage(home: Home, id: Long, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val p = home.g.plant(id) ?: run { nav.pop(); return }
    val sp = home.species(p)
    val st = home.status(p)
    var ask by remember { mutableStateOf(Ask.NONE) }
    var shot by rememberSaveable { mutableStateOf<String?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok && shot != null) ask = Ask.MOLT else shot = null }
    fun takePhoto() { val (f, uri) = Photos.fresh(ctx); shot = f.name; runCatching { camera.launch(uri) } }

    Page(p.name, nav.pop) {
        // the dex entry
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.pixelBox(C.screen, bevel = null).padding(8.dp)) { BufImage(remember(home.look(p)) { Spirit.draw(home.look(p)) }, scale = 4.dp) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(p.name.uppercase(), style = T.Display)
                Text(sp.name(home.ru), style = T.Body)
                if (sp.latin.isNotBlank()) Text(sp.latin, style = T.Small)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Badge(Words.element(ctx, sp.element), C.element(sp.element)) }
                Text(stringResource(R.string.form_n, sp.forms[p.stage - 1](home.ru)), style = T.Small.copy(color = C.ink))
                Text(stringResource(R.string.with_you, Words.age(ctx, p, home.nowMs)), style = T.Small)
                if (p.lineage.isNotBlank()) Text("← " + p.lineage, style = T.Small)
                if (sp.toxic) Text(stringResource(R.string.toxic), style = T.Small.copy(color = C.red))
            }
        }
        val line = home.content.line(st.mood, sp.element, (home.today.toEpochDay() + p.id).toInt())
        Says(line?.invoke(home.ru) ?: "…")

        // light
        val mx = maxOf(sp.dliMax * 1.2, st.dliToday * 1.1, 1.0)
        val r = st.dliToday / sp.dliMin
        Stat(stringResource(R.string.light), stringResource(R.string.light_value, Words.num(st.dliToday), Words.num(sp.dliMin), Words.num(sp.dliMax)))
        Bar((st.dliToday / mx).toFloat(), if (r >= 1) C.green else if (r >= .6) C.gold else C.red, band = (sp.dliMin / mx).toFloat()..(sp.dliMax / mx).toFloat())
        Text(lightNote(home, p, st.dliWeek), style = T.Small)

        // soil
        Stat(stringResource(R.string.soil), when {
            st.rest -> stringResource(R.string.soil_rest)
            st.checkIn == 0 -> stringResource(R.string.soil_now)
            else -> stringResource(R.string.soil_in, Words.days(ctx, st.checkIn))
        })
        Bar((st.moist / 100).toFloat(), C.brown, tick = sp.waterAt / 100f)
        Text(stringResource(R.string.soil_hint), style = T.Small)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PixelButton(stringResource(R.string.touch), { ask = Ask.TOUCH }, Modifier.weight(1f))
            PixelButton(stringResource(R.string.watered), { c.write { garden.water(p) } }, Modifier.weight(1f), kind = Kind.GHOST)
        }
        if (home.heating && sp.humid) Text(stringResource(R.string.humid_note), style = T.Small.copy(color = C.ink))

        // forms
        Stat(stringResource(R.string.evolution), if (p.stage >= 3) stringResource(R.string.evo_top) else stringResource(R.string.evo_next, Words.leaves(ctx, maxOf(0, (if (p.stage == 1) sp.s2 else sp.s3) - p.growth))))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
            for (s in 1..3) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val buf = remember(p.stage, s, p.hue) { Spirit.draw(home.look(p, Mood.CALM, s)).also { if (s > p.stage) it.silhouette() } }
                    BufImage(buf, scale = 2.dp)
                    Text(if (s > p.stage) "???" else sp.forms[s - 1](home.ru).uppercase(), style = T.Small.copy(color = if (s == p.stage) C.ink else C.muted))
                }
            }
        }
        if (st.evolve) PixelButton(stringResource(R.string.evolve_go), { ask = Ask.EVOLVE }, Modifier.fillMaxWidth(), kind = Kind.GOLD)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PixelButton(stringResource(R.string.new_leaf), { c.write { garden.grew(p, 1, null, null) } }, Modifier.weight(1f), kind = Kind.GHOST)
            if (sp.bloom != null) PixelButton(stringResource(R.string.bloomed), { c.write { garden.bloomed(p) } }, Modifier.weight(1f), kind = Kind.GHOST)
        }

        // molt
        Stat(stringResource(R.string.molt), if (st.molt) stringResource(R.string.molt_due) else stringResource(R.string.molt_next, Words.dateMs(ctx, maxOf(p.photoAt, p.caught) + 28 * 86_400_000L)))
        p.photo?.let { Photo(it, Modifier.fillMaxWidth().height(220.dp)) }
        Text(stringResource(R.string.molt_hint), style = T.Small)
        PixelButton(stringResource(R.string.molt_go), { takePhoto() }, Modifier.fillMaxWidth(), kind = if (st.molt) Kind.GREEN else Kind.GHOST)

        // trouble, cuttings, place
        Eyebrow(stringResource(R.string.more))
        val fight = home.g.fight(p.id)
        Menu(listOfNotNull(
            if (fight != null) MenuItem(stringResource(R.string.to_battle), home.content.pest(fight.pest)?.nick?.invoke(home.ru) ?: "") { nav.tab(Tab.BATTLE); nav.pop() }
            else MenuItem(stringResource(R.string.trouble)) { ask = Ask.SYMPTOMS },
            if (sp.cutting.cuttable) MenuItem(stringResource(R.string.take_cutting), stringResource(R.string.cutting_days, sp.rootDays)) { ask = Ask.CUTTING } else null,
            MenuItem(stringResource(R.string.move_plant), Words.spot(ctx, p.spot)) { ask = Ask.MOVE },
            MenuItem(stringResource(R.string.rename)) { ask = Ask.NAME },
            MenuItem(stringResource(R.string.species_page)) { nav.push(Overlay.Species(sp.id)) },
            MenuItem(stringResource(R.string.farewell)) { ask = Ask.GONE },
        ))

        if (sp.tip(home.ru).isNotBlank()) { Eyebrow(stringResource(R.string.tip)); Text(sp.tip(home.ru), style = T.Body) }

        Eyebrow(stringResource(R.string.journal))
        for (n in home.g.notes(p.id).take(14)) Row {
            Text(Words.dateMs(ctx, n.at), style = T.Small, modifier = Modifier.size(width = 96.dp, height = 18.dp))
            Text(note(home, n), style = T.Small.copy(color = C.ink))
        }
    }

    when (ask) {
        Ask.TOUCH -> Dialog({ ask = Ask.NONE }) {
            Text(stringResource(R.string.touch_q), style = T.Say)
            Menu(listOf(
                MenuItem(stringResource(R.string.touch_dry)) { c.write { garden.check(p, sp, home.sky, 0) }; ask = if (st.rest) Ask.NONE else Ask.WATER_AFTER },
                MenuItem(stringResource(R.string.touch_damp)) { c.write { garden.check(p, sp, home.sky, 1) }; ask = Ask.NONE },
                MenuItem(stringResource(R.string.touch_wet)) { c.write { garden.check(p, sp, home.sky, 2) }; ask = Ask.NONE },
            ))
            if (st.rest) Text(stringResource(R.string.touch_rest), style = T.Small)
        }
        Ask.WATER_AFTER -> Dialog({ ask = Ask.NONE }) {
            Text(stringResource(R.string.water_now, p.name), style = T.Say)
            Text(stringResource(R.string.water_how), style = T.Small)
            Menu(listOf(MenuItem(stringResource(R.string.watered)) { c.write { garden.water(p) }; ask = Ask.NONE }, MenuItem(stringResource(R.string.later)) { ask = Ask.NONE }))
        }
        Ask.EVOLVE -> EvolveDialog(home, p) { c.write { garden.evolve(p) }; ask = Ask.NONE }
        Ask.MOLT -> MoltDialog(home, p, shot, onDone = { n -> val f = shot; c.write { garden.grew(p, n, f, f?.let { Photos.hue(app, it) }) }; shot = null; ask = Ask.NONE },
            onCancel = { Photos.delete(ctx, shot); shot = null; ask = Ask.NONE })
        Ask.SYMPTOMS -> SymptomsDialog(home, onPick = { pest -> c.write { garden.fight(p, pest) }; ask = Ask.NONE; nav.tab(Tab.BATTLE); nav.pop() }, onClose = { ask = Ask.NONE })
        Ask.CUTTING -> Dialog({ ask = Ask.NONE }) {
            Text(stringResource(R.string.cutting_q, p.name), style = T.Say)
            Text(stringResource(R.string.cutting_hint), style = T.Small)
            val water = sp.cutting == Propagation.WATER
            Menu(listOf(
                MenuItem(stringResource(R.string.cutting_water), if (water) stringResource(R.string.best) else "") { c.write { garden.cutting(p, "WATER") }; ask = Ask.NONE; nav.tab(Tab.NURSERY); nav.pop() },
                MenuItem(stringResource(R.string.cutting_soil), if (!water) stringResource(R.string.best) else "") { c.write { garden.cutting(p, "SOIL") }; ask = Ask.NONE; nav.tab(Tab.NURSERY); nav.pop() },
            ))
        }
        Ask.MOVE -> Dialog({ ask = Ask.NONE }) {
            Text(stringResource(R.string.move_q), style = T.Say)
            var sill by remember { mutableStateOf(p.sill) }
            var spot by remember { mutableIntStateOf(p.spot) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (s in home.g.sills) Chip(s.name, s.id == sill, { sill = s.id })
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 0..2) Chip(Words.spot(ctx, i), i == spot, { spot = i })
            }
            PixelButton(stringResource(R.string.done), { c.write { garden.move(p, sill, spot) }; ask = Ask.NONE }, Modifier.fillMaxWidth())
        }
        Ask.NAME -> Dialog({ ask = Ask.NONE }) {
            var name by remember { mutableStateOf(p.name) }
            Field(name, { name = it.take(16) }, stringResource(R.string.name_hint))
            PixelButton(stringResource(R.string.done), { if (name.isNotBlank()) c.write { garden.update(p.copy(name = name.trim())) }; ask = Ask.NONE }, Modifier.fillMaxWidth())
        }
        Ask.GONE -> Dialog({ ask = Ask.NONE }) {
            Text(stringResource(R.string.farewell_q, p.name), style = T.Say)
            Menu(listOf(
                MenuItem(stringResource(R.string.gone_died)) { c.write { garden.gone(p, Plant.DIED) }; ask = Ask.NONE; nav.pop(); nav.push(Overlay.Herbarium) },
                MenuItem(stringResource(R.string.gone_given)) { c.write { garden.gone(p, Plant.GIVEN) }; ask = Ask.NONE; nav.pop() },
                MenuItem(stringResource(R.string.cancel)) { ask = Ask.NONE },
            ))
        }
        Ask.NONE -> Unit
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
        Text(label.uppercase(), style = T.Heading, modifier = Modifier.weight(1f))
        Text(value, style = T.Small, textAlign = TextAlign.End)
    }
}

@Composable
private fun lightNote(home: Home, p: Plant, week: Double): String {
    val sp = home.species(p)
    return when {
        week < sp.dliMin * .6 && p.spot > 0 -> stringResource(R.string.light_closer)
        week < sp.dliMin * .6 -> stringResource(R.string.light_low)
        week < sp.dliMin -> stringResource(R.string.light_bitlow)
        week > sp.dliMax -> stringResource(R.string.light_high)
        else -> stringResource(R.string.light_ok)
    }
}

@Composable
private fun note(home: Home, n: Note): String {
    val ctx = LocalContext.current
    return when (n.type) {
        Note.CAUGHT -> stringResource(R.string.n_caught)
        Note.CHECK -> stringResource(R.string.n_check, ctx.resources.getStringArray(R.array.readings)[n.value.coerceIn(0, 2)])
        Note.WATER -> stringResource(R.string.n_water)
        Note.MOLT -> stringResource(R.string.n_molt, n.value)
        Note.LEAF -> stringResource(R.string.n_leaf)
        Note.BLOOM -> stringResource(R.string.n_bloom)
        Note.EVOLVE -> stringResource(R.string.n_evolve, n.value)
        Note.FIGHT -> stringResource(R.string.n_fight, home.content.pest(n.text)?.nick?.invoke(home.ru) ?: n.text)
        Note.WON -> stringResource(R.string.n_won, home.content.pest(n.text)?.nick?.invoke(home.ru) ?: n.text)
        Note.CUTTING -> stringResource(R.string.n_cutting)
        Note.HATCH -> stringResource(R.string.n_hatch, n.text)
        Note.MOVED -> stringResource(R.string.n_moved)
        Note.GONE -> stringResource(R.string.n_gone)
        else -> n.type
    }
}

@Composable
fun Photo(name: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var bmp by remember(name) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(name) { bmp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { Photos.thumb(ctx, name) } }
    Box(modifier.pixelBox(C.paper2, bevel = null).padding(4.dp)) {
        bmp?.let { Image(it.asImageBitmap(), null, Modifier.matchParentSize(), contentScale = ContentScale.Crop) }
    }
}

/** «What? MARGO is evolving!» — the sprite flickers between its two forms, then settles on the new one. */
@Composable
private fun EvolveDialog(home: Home, p: Plant, onDone: () -> Unit) {
    val sp = home.species(p)
    var t by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (t < 24) { delay(if (t < 16) 160L - t * 6 else 90L); t++ } }
    val stage = if (t >= 24 || t % 2 == 1) p.stage + 1 else p.stage
    Dialog(onDone) {
        Box(Modifier.fillMaxWidth().background(C.screen).padding(10.dp), contentAlignment = Alignment.Center) {
            BufImage(remember(stage, t >= 24) { Spirit.draw(home.look(p, if (t >= 24) Mood.EVOLVE else Mood.CALM, stage.coerceAtMost(3))) }, scale = 4.dp)
        }
        Text(if (t < 24) stringResource(R.string.evolving, p.name) else stringResource(R.string.evolved, p.name, sp.forms[(p.stage).coerceAtMost(2)](home.ru)), style = T.Say)
        if (t >= 24) Menu(listOf(MenuItem(stringResource(R.string.hooray), onClick = onDone)))
    }
}

@Composable
private fun MoltDialog(home: Home, p: Plant, shot: String?, onDone: (Int) -> Unit, onCancel: () -> Unit) {
    var n by remember { mutableIntStateOf(0) }
    Dialog(onCancel) {
        Text(stringResource(R.string.molt_compare), style = T.Say)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.before), style = T.Small)
                p.photo?.let { Photo(it, Modifier.fillMaxWidth().height(160.dp)) } ?: Box(Modifier.fillMaxWidth().height(160.dp).pixelBox(C.paper2), contentAlignment = Alignment.Center) { Text("?", style = T.Display) }
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.now), style = T.Small)
                shot?.let { Photo(it, Modifier.fillMaxWidth().height(160.dp)) }
            }
        }
        Text(stringResource(R.string.molt_q), style = T.Body)
        Stepper(n, { n = it }, 0..40)
        PixelButton(stringResource(R.string.save), { onDone(n) }, Modifier.fillMaxWidth())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SymptomsDialog(home: Home, onPick: (String) -> Unit, onClose: () -> Unit) {
    var seen by remember { mutableStateOf(setOf<String>()) }
    Dialog(onClose) {
        Text(stringResource(R.string.trouble_q), style = T.Say)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (s in home.content.symptoms) Chip(s.name(home.ru), s.code in seen, { seen = if (s.code in seen) seen - s.code else seen + s.code })
        }
        val found = home.content.diagnose(seen)
        if (seen.isNotEmpty()) {
            Text(stringResource(if (found.isEmpty()) R.string.trouble_none else R.string.trouble_found), style = T.Small)
            for (pest in found.take(4)) Column(Modifier.fillMaxWidth().pixelBox(C.paper).padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BufImage(remember(pest.sprite) { app.rootlings.ui.art.Foes.draw(pest.sprite) }, scale = 2.dp)
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(pest.name(home.ru), style = T.Heading)
                        Text(pest.about(home.ru), style = T.Small)
                    }
                }
                PixelButton(stringResource(R.string.fight_go, pest.nick(home.ru)), { onPick(pest.id) }, Modifier.fillMaxWidth(), kind = Kind.RED)
            }
        }
    }
}
