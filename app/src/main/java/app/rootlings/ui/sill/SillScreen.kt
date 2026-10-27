package app.rootlings.ui.sill

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.rootlings.R
import app.rootlings.data.Sill
import app.rootlings.ui.Home
import app.rootlings.ui.Nav
import app.rootlings.ui.Overlay
import app.rootlings.ui.Words
import app.rootlings.ui.art.Buf
import app.rootlings.ui.art.Icons
import app.rootlings.ui.art.Outside
import app.rootlings.ui.art.PixelView
import app.rootlings.ui.art.WindowScene
import app.rootlings.ui.art.rgb
import app.rootlings.ui.components.BufImage
import app.rootlings.ui.components.Kind
import app.rootlings.ui.components.PixelButton
import app.rootlings.ui.components.Says
import app.rootlings.ui.components.textBox
import app.rootlings.ui.theme.C
import app.rootlings.ui.theme.T
import kotlin.math.roundToInt

@Composable
fun SillScreen(home: Home, nav: Nav) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 10.dp, end = 10.dp, top = 0.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item { InfoStrip(home) }
            if (home.heating) item { HeatingBox(home) }
            if (home.g.sills.isEmpty()) item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Says(stringResource(R.string.sill_none))
                    PixelButton(stringResource(R.string.window_add), { nav.push(Overlay.Windows) }, Modifier.fillMaxWidth())
                }
            }
            items(home.g.sills, key = { it.id }) { s -> Window(home, s, nav) }
            if (home.g.sills.isNotEmpty()) item {
                PixelButton(stringResource(R.string.windows_edit), { nav.push(Overlay.Windows) }, Modifier.fillMaxWidth(), kind = Kind.GHOST)
            }
        }
        if (home.g.sills.isNotEmpty()) Box(Modifier.align(Alignment.BottomEnd).padding(16.dp).size(64.dp).clickable { nav.push(Overlay.Catch()) }, contentAlignment = Alignment.Center) {
            BufImage(remember { Icons.seedball() }, scale = 3.dp)
        }
    }
}

/** The weather line under the red case: outside now, the sun today, the source. */
@Composable
private fun InfoStrip(home: Home) {
    val d = home.sky.day(home.today)
    val h = d.hours[home.now.hour]
    val sunHours = d.hours.count { it.direct > 120 }
    val t = h.temp.roundToInt()
    val place = home.settings.place?.name?.takeIf { it.isNotBlank() }
    Row(Modifier.fillMaxWidth().background(C.ink).padding(horizontal = 12.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text((if (t > 0) "+" else "") + "$t°", style = T.Body.copy(color = C.gold))
        Text(stringResource(R.string.info_clouds, h.cloud.roundToInt()), style = T.Small.copy(color = C.info))
        Text(stringResource(R.string.info_sun, sunHours), style = T.Small.copy(color = C.info))
        Text(place ?: stringResource(if (home.sky.real(home.today)) R.string.info_real else R.string.info_own), style = T.Small.copy(color = C.faint), modifier = Modifier.weight(1f), textAlign = TextAlign.End, maxLines = 1)
    }
}

@Composable
private fun HeatingBox(home: Home) {
    Column(Modifier.fillMaxWidth().textBox().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(stringResource(R.string.heating_title), style = T.Heading.copy(color = C.red))
        Text(stringResource(R.string.heating_text, home.sky.coldDays(home.today)), style = T.Body)
    }
}

@Composable
private fun Window(home: Home, s: Sill, nav: Nav) {
    val ctx = LocalContext.current
    val plants = home.g.onSill(s.id)
    val day = home.sky.day(home.today)
    val gains = home.sky.gains(home.today, s)
    val out = Outside(home.sky.sunNow(home.now), day.hours[home.now.hour], s.azimuth, gains[home.now.hour])
    val looks = plants.map { home.look(it) }
    val scene = remember(out, looks, s.id) { WindowScene(out, looks, s.id.toInt() * 17) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(s.name.uppercase(), style = T.Heading, modifier = Modifier.weight(1f), maxLines = 1)
            Text("☀ " + stringResource(R.string.dli_short, Words.num(home.sky.dli(home.today, s, 0))), style = T.Small)
        }
        Text(Words.compass(ctx, s.azimuth) + " · " + Words.view(ctx, s.view), style = T.Small)
        Box(Modifier.fillMaxWidth().widthIn(max = 480.dp).align(Alignment.CenterHorizontally).background(C.ink).padding(2.dp)) {
            PixelView(scene, Modifier.fillMaxWidth().pointerInput(scene) {
                detectTapGestures { o ->
                    val k = size.width / scene.w.toFloat()
                    val i = scene.at(o.x / k.toDouble(), o.y / k.toDouble())
                    if (i != null) nav.push(Overlay.Plant(plants[i].id)) else nav.push(Overlay.Catch(s.id))
                }
            })
        }
        if (plants.isEmpty()) Text(stringResource(R.string.sill_empty), style = T.Small, modifier = Modifier.clickable { nav.push(Overlay.Catch(s.id)) })
        for (row in plants.chunked(3)) {
            Row(Modifier.fillMaxWidth()) {
                for (p in row) {
                    Column(Modifier.weight(1f).clickable { nav.push(Overlay.Plant(p.id)) }.padding(horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(p.name.uppercase(), style = T.Heading, maxLines = 1, textAlign = TextAlign.Center)
                        Text(Words.status(ctx, home.status(p)), style = T.Small, textAlign = TextAlign.Center, maxLines = 2)
                    }
                }
                repeat(3 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
        BufImage(remember(gains.toList(), home.now.hour) { spark(gains, home.now.hour) }, Modifier.align(Alignment.CenterHorizontally), scale = 3.dp)
        Text(stringResource(R.string.spark_caption), style = T.Small, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

/** The sill's light hour by hour today, the current hour in red. */
private fun spark(gains: DoubleArray, hour: Int): Buf {
    val b = Buf(120, 12)
    val mx = maxOf(260.0, gains.maxOrNull() ?: 0.0)
    b.r(0, 11, 120, 1, rgb(0x202818))
    gains.forEachIndexed { i, g -> val hh = (g / mx * 10).roundToInt(); if (hh > 0) b.r(i * 5, 11 - hh, 4, hh, if (i == hour) rgb(0xC83828) else rgb(0xE8B030)) }
    b.r(hour * 5 + 1, 0, 2, 1, rgb(0xC83828))
    return b
}
