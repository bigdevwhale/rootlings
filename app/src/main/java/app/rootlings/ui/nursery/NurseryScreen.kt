package app.rootlings.ui.nursery

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.rootlings.R
import app.rootlings.container
import app.rootlings.data.Cutting
import app.rootlings.data.Mood
import app.rootlings.domain.Care
import app.rootlings.domain.Gift
import app.rootlings.domain.GiftCode
import app.rootlings.ui.Home
import app.rootlings.ui.Nav
import app.rootlings.ui.Overlay
import app.rootlings.ui.Words
import app.rootlings.ui.art.Spirit
import app.rootlings.ui.art.cuttingArt
import app.rootlings.ui.components.Bar
import app.rootlings.ui.components.BufImage
import app.rootlings.ui.components.Chip
import app.rootlings.ui.components.Dialog
import app.rootlings.ui.components.Eyebrow
import app.rootlings.ui.components.Field
import app.rootlings.ui.components.Kind
import app.rootlings.ui.components.Menu
import app.rootlings.ui.components.MenuItem
import app.rootlings.ui.components.PixelButton
import app.rootlings.ui.components.Says
import app.rootlings.ui.components.pixelBox
import app.rootlings.ui.theme.C
import app.rootlings.ui.theme.T
import kotlinx.coroutines.delay

@Composable
fun NurseryScreen(home: Home, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    var hatching by remember { mutableStateOf<Cutting?>(null) }
    var receiving by remember { mutableStateOf<Gift?>(null) }
    var code by remember { mutableStateOf("") }
    val active = home.g.cuttings.filter { it.hatched == null }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.nursery_title), style = T.Display)
        if (active.isEmpty()) Says(stringResource(R.string.nursery_empty), style = T.Body)
        for (cut in active) Egg(home, cut, onHatch = { hatching = cut }, onGive = {
            val parent = home.g.plant(cut.parent)
            val lineage = parent?.let { listOf(it.name, it.lineage).filter(String::isNotBlank).joinToString(" ← ") } ?: ""
            val own = home.g.own.firstOrNull { it.key == cut.species }
            val g = Gift(cut.species, ctx.getString(R.string.gift_default_name), lineage, (parent?.hue ?: 110) + 6, home.settings.keeper, own)
            val text = ctx.getString(R.string.gift_text, parent?.name ?: "", GiftCode.encode(g))
            runCatching { ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            c.write { garden.given(cut) }
        }, onDrop = { c.write { garden.dropCutting(cut) } })

        Eyebrow(stringResource(R.string.gift_receive))
        Text(stringResource(R.string.gift_receive_hint), style = T.Small)
        Field(code, { code = it }, stringResource(R.string.gift_paste), single = false)
        val decoded = remember(code) { GiftCode.decode(code) }
        if (code.isNotBlank() && decoded == null) Text(stringResource(R.string.gift_bad), style = T.Small.copy(color = C.red))
        PixelButton(stringResource(R.string.gift_accept), { receiving = decoded }, Modifier.fillMaxWidth(), enabled = decoded != null && home.g.sills.isNotEmpty())

        val kids = home.g.cuttings.filter { it.hatched != null }
        if (kids.isNotEmpty()) {
            Eyebrow(stringResource(R.string.nursery_history))
            for (k in kids) {
                val parent = home.g.plant(k.parent)
                val child = k.child?.let { home.g.plant(it) }
                Text(stringResource(if (k.given) R.string.hist_given else R.string.hist_hatched, parent?.name ?: "?", child?.name ?: "", Words.dateMs(ctx, k.hatched ?: 0)), style = T.Body)
            }
        }
    }
    hatching?.let { cut -> HatchDialog(home, cut, onDone = { name, sill, spot -> c.write { val id = garden.hatch(cut, name, sill, spot) }; hatching = null }, onClose = { hatching = null }) }
    receiving?.let { g -> PlaceDialog(home, stringResource(R.string.gift_where, g.name), g.name, onDone = { name, sill, spot -> c.write { garden.receive(g.copy(name = name), sill, spot) }; receiving = null; code = "" }, onClose = { receiving = null }) }
}

@Composable
private fun Egg(home: Home, cut: Cutting, onHatch: () -> Unit, onGive: () -> Unit, onDrop: () -> Unit) {
    val ctx = LocalContext.current
    val parent = home.g.plant(cut.parent)
    val sp = home.content.species(cut.species) ?: return
    val days = ((home.nowMs - cut.started) / Care.DAY).toInt()
    val need = sp.rootDays.coerceAtLeast(7)
    val progress = (days / need.toDouble()).coerceIn(0.0, 1.0)
    val ready = days >= need / 2
    Column(Modifier.fillMaxWidth().pixelBox(C.paper).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val look = parent?.let { home.look(it) } ?: home.look(sp)
        BufImage(remember(cut.id, (progress * 6).toInt()) { cuttingArt(look, cut.medium, progress) }, scale = 4.dp)
        Text(stringResource(R.string.egg_of, parent?.name ?: sp.name(home.ru)).uppercase(), style = T.Heading)
        Text(stringResource(if (cut.medium == "WATER") R.string.egg_water else R.string.egg_soil, Words.days(ctx, days), Words.days(ctx, need)), style = T.Small)
        Bar(progress.toFloat(), C.green, Modifier.padding(horizontal = 24.dp))
        Text(stringResource(if (cut.medium == "WATER") R.string.egg_care_water else R.string.egg_care_soil), style = T.Small)
        PixelButton(stringResource(R.string.roots), onHatch, Modifier.fillMaxWidth(), kind = Kind.RED, enabled = ready)
        if (!ready) Text(stringResource(R.string.roots_wait), style = T.Small)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PixelButton(stringResource(R.string.give), onGive, Modifier.weight(1f), kind = Kind.GHOST, enabled = ready)
            PixelButton(stringResource(R.string.drop), onDrop, Modifier.weight(1f), kind = Kind.GHOST)
        }
    }
}

/** «Huh? The cutting is moving!» — the glass shakes, flashes, and a little spirit pops out. */
@Composable
private fun HatchDialog(home: Home, cut: Cutting, onDone: (String, Long, Int) -> Unit, onClose: () -> Unit) {
    val parent = home.g.plant(cut.parent)
    val sp = home.content.species(cut.species) ?: return
    var t by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (t < 20) { delay(110); t++ } }
    if (t < 20) Dialog(onClose) {
        Box(Modifier.fillMaxWidth().background(if (t in 14..19 && t % 2 == 0) C.paper else C.screen).padding(10.dp), contentAlignment = Alignment.Center) {
            BufImage(remember(cut.id) { cuttingArt(parent?.let { home.look(it) } ?: home.look(sp), cut.medium, 1.0) }, Modifier.graphicsLayer { translationX = if (t < 14) (if (t % 2 == 0) -8f else 8f) else 0f }, scale = 4.dp)
        }
        Text(stringResource(R.string.hatching), style = T.Say)
    } else PlaceDialog(home, stringResource(R.string.hatched, parent?.name ?: ""), stringResource(R.string.gift_default_name), onDone, onClose,
        sprite = { Spirit.draw((parent?.let { home.look(it, Mood.HATCH, 1) } ?: home.look(sp, 1, Mood.HATCH)).copy(hue = (parent?.hue ?: sp.hue) + 6)) },
        line = home.content.line(Mood.HATCH, sp.element, cut.id.toInt())?.invoke(home.ru))
}

/** Where a new spirit goes: a name, a window, a spot. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlaceDialog(home: Home, title: String, defaultName: String, onDone: (String, Long, Int) -> Unit, onClose: () -> Unit, sprite: (() -> app.rootlings.ui.art.Buf)? = null, line: String? = null) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(defaultName) }
    var sill by remember { mutableStateOf(home.g.sills.firstOrNull()?.id ?: 0L) }
    var spot by remember { mutableIntStateOf(0) }
    Dialog(onClose) {
        sprite?.let { Box(Modifier.fillMaxWidth().background(C.screen).padding(10.dp), contentAlignment = Alignment.Center) { BufImage(remember { it() }, scale = 4.dp) } }
        Text(title, style = T.Say)
        line?.let { Text(it, style = T.Body) }
        Field(name, { name = it.take(16) }, stringResource(R.string.name_hint))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { for (s in home.g.sills) Chip(s.name, s.id == sill, { sill = s.id }) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { for (i in 0..2) Chip(Words.spot(ctx, i), i == spot, { spot = i }) }
        Menu(listOf(MenuItem(stringResource(R.string.done), enabled = name.isNotBlank() && home.g.sills.isNotEmpty()) { onDone(name.trim(), sill, spot) }))
    }
}
