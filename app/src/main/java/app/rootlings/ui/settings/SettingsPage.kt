package app.rootlings.ui.settings

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import app.rootlings.R
import app.rootlings.container
import app.rootlings.data.Sill
import app.rootlings.domain.Care
import app.rootlings.domain.Place
import app.rootlings.domain.Weather
import app.rootlings.ui.Home
import app.rootlings.ui.Nav
import app.rootlings.ui.Overlay
import app.rootlings.ui.Words
import app.rootlings.ui.components.Chip
import app.rootlings.ui.components.CommunityLinks
import app.rootlings.ui.components.Dialog
import app.rootlings.ui.components.Eyebrow
import app.rootlings.ui.components.Field
import app.rootlings.ui.components.Kind
import app.rootlings.ui.components.Menu
import app.rootlings.ui.components.MenuItem
import app.rootlings.ui.components.Page
import app.rootlings.ui.components.Panel
import app.rootlings.ui.components.PixelButton
import app.rootlings.ui.components.Says
import app.rootlings.ui.components.Stepper
import app.rootlings.ui.theme.C
import app.rootlings.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.roundToInt

/** A search for the town whose weather the windows share. */
@Composable
fun PlacePicker(ru: Boolean, onPicked: (Place) -> Unit) {
    val scope = rememberCoroutineScope()
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var failed by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Field(q, { q = it }, stringResource(R.string.place_search), Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            PixelButton(stringResource(R.string.find), {
                if (q.isNotBlank()) scope.launch {
                    val r = runCatching { withContext(Dispatchers.IO) { Weather.search(q, if (ru) "ru" else "en") } }
                    failed = r.isFailure
                    results = r.getOrDefault(emptyList())
                }
            })
        }
        if (failed) Text(stringResource(R.string.place_fail), style = T.Small.copy(color = C.red))
        for (p in results) Panel(Modifier.fillMaxWidth(), onClick = { results = emptyList(); q = p.name; focus.clearFocus(); onPicked(p) }, fill = C.paper2) {
            Text(p.name, style = T.Body)
            Text(p.region, style = T.Small)
        }
    }
}

@Composable
fun SettingsPage(home: Home, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val set = home.settings
    Page(stringResource(R.string.settings), nav.pop) {
        Eyebrow(stringResource(R.string.weather))
        Text(set.place?.let { stringResource(R.string.place_now, it.name) } ?: stringResource(R.string.place_none), style = T.Body)
        Text(stringResource(R.string.place_hint), style = T.Small)
        PlacePicker(home.ru) { picked -> c.write { settings.update { it.copy(place = picked, weatherAt = 0) }; refreshWeather(force = true) } }
        if (set.place != null) PixelButton(stringResource(R.string.place_off), { c.write { settings.update { it.copy(place = null) } } }, Modifier.fillMaxWidth(), kind = Kind.GHOST)

        Eyebrow(stringResource(R.string.home_title))
        Menu(listOf(
            MenuItem(stringResource(R.string.windows_edit), note = home.g.sills.size.toString()) { nav.push(Overlay.Windows) },
            MenuItem(stringResource(R.string.vacation)) { nav.push(Overlay.Vacation) },
            MenuItem(stringResource(R.string.herbarium)) { nav.push(Overlay.Herbarium) },
        ))
        Toggle(stringResource(R.string.pets_home), set.pets) { c.write { settings.update { it.copy(pets = !it.pets) } } }
        Toggle(stringResource(R.string.notify_text), set.notify) { c.write { settings.update { it.copy(notify = !it.notify) } } }
        var keeper by remember { mutableStateOf(set.keeper) }
        Text(stringResource(R.string.keeper), style = T.Small)
        Field(keeper, { keeper = it.take(24); c.write { settings.update { s -> s.copy(keeper = keeper) } } }, stringResource(R.string.keeper_hint))

        Eyebrow(stringResource(R.string.language))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val cur = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').substringBefore(',')
            for ((tag, label) in listOf("" to stringResource(R.string.lang_system), "ru" to "Русский", "en" to "English")) {
                Chip(label, cur == tag, {
                    AppCompatDelegate.setApplicationLocales(if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag))
                })
            }
        }

        Eyebrow(stringResource(R.string.community))
        CommunityLinks()

        Eyebrow(stringResource(R.string.about))
        Text(stringResource(R.string.about_text), style = T.Body)
        Text(stringResource(R.string.credits), style = T.Small)
    }
}

@Composable
private fun Toggle(text: String, on: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = T.Body, modifier = Modifier.weight(1f))
        PixelButton(stringResource(if (on) R.string.on else R.string.off), onClick, kind = if (on) Kind.GREEN else Kind.GHOST)
    }
}

/** The windows: their names, which way they look (by the phone's compass or by hand), what's outside. */
@Composable
fun WindowsPage(home: Home, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    var editing by remember { mutableStateOf<Sill?>(null) }
    var adding by remember { mutableStateOf(false) }
    Page(stringResource(R.string.windows), nav.pop) {
        Says(stringResource(R.string.windows_intro), style = T.Body)
        for (s in home.g.sills) Panel(Modifier.fillMaxWidth(), onClick = { editing = s }) {
            Text(s.name.uppercase(), style = T.Heading)
            Text(Words.compass(ctx, s.azimuth) + " · ${s.azimuth}° · " + Words.view(ctx, s.view), style = T.Small)
            Text(stringResource(R.string.window_plants, home.g.onSill(s.id).size), style = T.Small)
        }
        PixelButton(stringResource(R.string.window_add), { adding = true }, Modifier.fillMaxWidth())
    }
    if (adding) SillDialog(null, onSave = { n, a, v -> c.write { garden.addSill(n, a, v) }; adding = false }, onDelete = null, onClose = { adding = false })
    editing?.let { s ->
        SillDialog(s, onSave = { n, a, v -> c.write { garden.update(s.copy(name = n, azimuth = a, view = v)) }; editing = null },
            onDelete = if (home.g.onSill(s.id).isEmpty()) ({ c.write { garden.deleteSill(s) }; editing = null }) else null, onClose = { editing = null })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SillDialog(s: Sill?, onSave: (String, Int, Int) -> Unit, onDelete: (() -> Unit)?, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(s?.name ?: "") }
    var az by remember { mutableIntStateOf(s?.azimuth ?: 180) }
    var view by remember { mutableIntStateOf(s?.view ?: 0) }
    var compass by remember { mutableStateOf(false) }
    Dialog(onClose) {
        Field(name, { name = it.take(20) }, stringResource(R.string.window_name))
        Eyebrow(stringResource(R.string.window_faces))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ctx.resources.getStringArray(R.array.compass).forEachIndexed { i, w -> Chip(w, Math.floorMod((az + 22) / 45, 8) == i && !compass, { az = i * 45; compass = false }) }
        }
        if (compass) Compass { az = it }
        Text(stringResource(R.string.compass_now, Words.compass(ctx, az), az), style = T.Small.copy(color = C.ink))
        PixelButton(stringResource(if (compass) R.string.compass_stop else R.string.compass_go), { compass = !compass }, Modifier.fillMaxWidth(), kind = Kind.GHOST)
        if (compass) Text(stringResource(R.string.compass_hint), style = T.Small)
        Eyebrow(stringResource(R.string.window_view))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 0..2) Chip(Words.view(ctx, i), i == view, { view = i })
        }
        PixelButton(stringResource(R.string.save), { onSave(name.trim().ifBlank { Words.compass(ctx, az) }, Math.floorMod(az, 360), view) }, Modifier.fillMaxWidth())
        if (onDelete != null) PixelButton(stringResource(R.string.window_delete), onDelete, Modifier.fillMaxWidth(), kind = Kind.GHOST)
    }
}

/**
 * The phone's compass, read the way a person holds it: back pressed to the glass, screen to the
 * room. The direction the back faces is the way the window looks out.
 */
@Composable
private fun Compass(onAzimuth: (Int) -> Unit) {
    val ctx = LocalContext.current
    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val r = FloatArray(9)
        var smooth: Double? = null
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(r, e.values)
                // The device's back is −Z; its world direction is minus the third column.
                val east = -r[2].toDouble()
                val north = -r[5].toDouble()
                val a = (Math.toDegrees(atan2(east, north)) + 360) % 360
                val s = smooth?.let { o -> val d = ((a - o + 540) % 360) - 180; (o + d * .2 + 360) % 360 } ?: a
                smooth = s
                onAzimuth(s.roundToInt() % 360)
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
        }
        if (sensor != null) sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(l) }
    }
}

/** Away for a while: what each plant will want, and a note for whoever waters them. */
@Composable
fun VacationPage(home: Home, nav: Nav) {
    val ctx = LocalContext.current
    var days by remember { mutableIntStateOf(10) }
    val plan = home.alive.map { p -> p to Care.waterings(p, home.species(p), home.g.sill(p.sill), home.sky, home.now, days) }
    val text = buildString {
        appendLine(ctx.getString(R.string.vacation_note_head, days))
        for ((p, dates) in plan) {
            val sill = home.g.sill(p.sill)?.name ?: ""
            appendLine(if (dates.isEmpty()) ctx.getString(R.string.vacation_none, p.name, home.species(p).name(home.ru), sill)
                else ctx.getString(R.string.vacation_line, p.name, home.species(p).name(home.ru), sill, dates.joinToString(", ") { Words.date(ctx, it) }))
        }
        append(ctx.getString(R.string.vacation_note_foot))
    }
    Page(stringResource(R.string.vacation), nav.pop) {
        Says(stringResource(R.string.vacation_intro), style = T.Body)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.vacation_days), style = T.Body, modifier = Modifier.weight(1f))
            Stepper(days, { days = it }, 2..30)
        }
        for ((p, dates) in plan) Panel(Modifier.fillMaxWidth()) {
            Text(p.name.uppercase(), style = T.Heading)
            Text(if (dates.isEmpty()) stringResource(R.string.vacation_fine) else dates.joinToString(", ") { Words.date(ctx, it) }, style = T.Body)
        }
        PixelButton(stringResource(R.string.vacation_share), {
            runCatching { ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }, Modifier.fillMaxWidth().padding(bottom = 12.dp))
    }
}
