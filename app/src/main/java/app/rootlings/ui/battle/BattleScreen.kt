package app.rootlings.ui.battle

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.rootlings.R
import app.rootlings.container
import app.rootlings.data.Fight
import app.rootlings.data.Mood
import app.rootlings.domain.Care
import app.rootlings.ui.Home
import app.rootlings.ui.Nav
import app.rootlings.ui.art.Foes
import app.rootlings.ui.art.Spirit
import app.rootlings.ui.art.arena
import app.rootlings.ui.components.BufImage
import app.rootlings.ui.components.Chip
import app.rootlings.ui.components.Menu
import app.rootlings.ui.components.MenuItem
import app.rootlings.ui.components.PX
import app.rootlings.ui.components.Says
import app.rootlings.ui.components.pixelBox
import app.rootlings.ui.components.textBox
import app.rootlings.ui.theme.C
import app.rootlings.ui.theme.T
import kotlinx.coroutines.delay

@Composable
fun BattleScreen(home: Home, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val fights = home.fights
    var selected by rememberSaveable { mutableStateOf<Long?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var won by remember { mutableStateOf<Fight?>(null) }
    var hit by remember { mutableIntStateOf(0) }
    val f = won ?: fights.firstOrNull { it.id == selected } ?: fights.firstOrNull()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.battle_title), style = T.Display)
        if (fights.size > 1) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (x in fights) Chip(home.g.plant(x.plant)?.name ?: "?", x.id == f?.id, { selected = x.id; won = null; message = null })
        }
        if (f == null) { Says(stringResource(R.string.battle_none), style = T.Body); return@Column }
        val p = home.g.plant(f.plant) ?: return@Column
        val pest = home.content.pest(f.pest) ?: return@Column
        val done = f.won != null
        val foeHp = 1f - f.step / pest.moves.size.toFloat()
        val plantHp = .35f + .65f * f.step / pest.moves.size
        Arena(
            foe = remember(pest.sprite) { Foes.draw(pest.sprite) },
            plant = remember(p.id, done, p.stage) { Spirit.draw(home.look(p, if (done) Mood.SUN else Mood.PEST)) },
            foeName = pest.nick(home.ru), foeLevel = pest.level, foeHp = foeHp,
            plantName = p.name.uppercase(), plantLevel = p.stage * 6 + p.growth / 3, plantHp = plantHp,
            hit = hit, fainted = done,
        )
        val days = ((home.nowMs - f.started) / Care.DAY).toInt()
        val next = pest.moves.getOrNull(f.step)
        val fill = { s: String -> s.replace("{plant}", p.name.uppercase()).replace("{foe}", pest.nick(home.ru)) }
        Column(Modifier.fillMaxWidth().textBox().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(message ?: when {
                done -> stringResource(R.string.battle_won, pest.nick(home.ru))
                f.step == 0 -> stringResource(R.string.battle_start, pest.nick(home.ru), pest.name(home.ru), p.name.uppercase())
                else -> stringResource(R.string.battle_what, p.name.uppercase())
            }, style = T.Say)
            if (!done && next != null) Text(next.how(home.ru), style = T.Small.copy(color = C.ink))
            if (!done) Menu(pest.moves.mapIndexed { i, m ->
                val open = days >= m.day
                MenuItem(
                    (if (i < f.step) "✓ " else "") + m.name(home.ru),
                    note = when { i < f.step -> ""; open -> stringResource(R.string.move_today); else -> stringResource(R.string.move_in, m.day - days) },
                    enabled = i == f.step && open, selected = i == f.step,
                ) {
                    message = fill(m.hit(home.ru)); hit++
                    if (i == pest.moves.lastIndex) won = f.copy(step = f.step + 1, won = home.nowMs)
                    c.write { garden.move(f, pest.moves.size) }
                }
            } + MenuItem(stringResource(R.string.retreat), note = stringResource(R.string.retreat_note)) { c.write { garden.retreat(f) }; message = null })
            else Menu(listOf(MenuItem(stringResource(R.string.hooray)) { won = null; message = null }))
        }
        if (!done) Text(pest.about(home.ru), style = T.Small)
        if (!done && (pest.likes == "dry") && home.heating) Text(stringResource(R.string.battle_dry_air), style = T.Small.copy(color = C.red))
    }
}

@Composable
private fun Arena(foe: app.rootlings.ui.art.Buf, plant: app.rootlings.ui.art.Buf, foeName: String, foeLevel: Int, foeHp: Float, plantName: String, plantLevel: Int, plantHp: Float, hit: Int, fainted: Boolean) {
    val bg = remember { arena() }
    var blink by remember { mutableStateOf(false) }
    LaunchedEffect(hit) { if (hit > 0) repeat(6) { blink = !blink; delay(110) }; blink = false }
    val drop by animateFloatAsState(if (fainted) 1f else 0f, label = "faint")
    BoxWithConstraints(Modifier.fillMaxWidth().widthIn(max = 480.dp).background(C.ink).padding(2.dp).aspectRatio(120f / 80f)) {
        val k: Dp = maxWidth / 120
        BufImage(bg, scale = k)
        Box(Modifier.offset(k * 74, k * 6 + k * 30 * drop).alpha((1 - drop) * if (blink) 0f else 1f)) { BufImage(foe, scale = k) }
        Box(Modifier.offset(k * 16, k * 42)) { BufImage(plant, scale = k) }
        HpBox(foeName, foeLevel, foeHp, Modifier.offset(k * 4, k * 4).width(k * 60))
        HpBox(plantName, plantLevel, plantHp, Modifier.offset(k * 58, k * 46).width(k * 58))
    }
}

@Composable
private fun HpBox(name: String, level: Int, hp: Float, modifier: Modifier) {
    val shown by animateFloatAsState(hp, label = "hp")
    Column(modifier.pixelBox(C.paper).padding(horizontal = 8.dp, vertical = 5.dp)) {
        Row { Text(name, style = T.Small.copy(color = C.ink), modifier = Modifier.weight(1f), maxLines = 1); Text(stringResource(R.string.level, level), style = T.Small.copy(color = C.ink)) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("HP", style = T.Small.copy(color = C.gold))
            Box(Modifier.padding(start = 4.dp).fillMaxWidth().height(8.dp).drawBehind {
                val p = PX.toPx()
                drawRect(C.ink); drawRect(Color(0xFF506048), Offset(p, p), Size(size.width - 2 * p, size.height - 2 * p))
                val col = if (shown > .5f) Color(0xFF48B048) else if (shown > .2f) C.gold else C.red
                drawRect(col, Offset(p, p), Size((size.width - 2 * p) * shown.coerceIn(0f, 1f), size.height - 2 * p))
            })
        }
    }
}
