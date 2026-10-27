package app.rootlings.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.rootlings.R
import app.rootlings.container
import app.rootlings.data.Kind
import app.rootlings.data.Mood
import app.rootlings.ru
import app.rootlings.ui.art.Look
import app.rootlings.ui.art.Spirit
import app.rootlings.ui.components.BufImage
import app.rootlings.ui.components.Kind as BtnKind
import app.rootlings.ui.components.PixelButton
import app.rootlings.ui.components.Says
import app.rootlings.ui.settings.PlacePicker
import app.rootlings.ui.settings.SillDialog
import app.rootlings.ui.theme.C
import app.rootlings.ui.theme.T

/** Four pages: what this is, the town for the weather, the first window, and off we go. */
@Composable
fun Onboarding() {
    val ctx = LocalContext.current
    val c = ctx.container
    var page by rememberSaveable { mutableIntStateOf(0) }
    var town by rememberSaveable { mutableStateOf<String?>(null) }
    var window by rememberSaveable { mutableStateOf<String?>(null) }
    var addWindow by remember { mutableStateOf(false) }
    val notify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    Column(Modifier.fillMaxSize().background(C.bg)) {
        Box(Modifier.fillMaxWidth().background(C.red).statusBarsPadding().padding(16.dp)) {
            Text(stringResource(R.string.app_title), style = T.Display.copy(color = C.paper))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                val kinds = listOf(Kind.MONSTERA to 140, Kind.BALL to 100, Kind.FERN to 118, Kind.GRASS to 84)
                kinds.forEachIndexed { i, (k, h) ->
                    BufImage(remember(k, page) { Spirit.draw(Look(k, h, 46, 42, 2, if (page == i) Mood.SUN else Mood.CALM, pot = i)) }, scale = 2.dp)
                }
            }
            when (page) {
                0 -> {
                    Says(stringResource(R.string.ob_1))
                    Text(stringResource(R.string.ob_1b), style = T.Body)
                }
                1 -> {
                    Says(stringResource(R.string.ob_2))
                    Text(stringResource(R.string.ob_2b), style = T.Small)
                    if (town != null) Text(stringResource(R.string.place_now, town!!), style = T.Body.copy(color = C.greenDeep))
                    PlacePicker(ctx.ru) { p -> town = p.name; c.write { settings.update { it.copy(place = p, weatherAt = 0) }; refreshWeather(force = true) } }
                }
                2 -> {
                    Says(stringResource(R.string.ob_3))
                    Text(stringResource(R.string.ob_3b), style = T.Small)
                    if (window != null) Text("✓ " + window, style = T.Body.copy(color = C.greenDeep))
                    PixelButton(stringResource(R.string.window_add), { addWindow = true }, Modifier.fillMaxWidth(), kind = if (window == null) BtnKind.GREEN else BtnKind.GHOST)
                }
                else -> {
                    Says(stringResource(R.string.ob_4))
                    Text(stringResource(R.string.ob_4b), style = T.Body)
                }
            }
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (page > 0) PixelButton(stringResource(R.string.back), { page-- }, kind = BtnKind.GHOST)
            Box(Modifier.weight(1f))
            PixelButton(stringResource(when { page == 3 -> R.string.ob_start; page == 1 && town == null -> R.string.skip; else -> R.string.next }), {
                if (page < 3) page++ else {
                    if (Build.VERSION.SDK_INT >= 33) runCatching { notify.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    c.write { settings.update { it.copy(onboarded = true) } }
                }
            }, kind = if (page == 3) BtnKind.RED else BtnKind.GREEN, enabled = page != 2 || window != null)
        }
    }
    if (addWindow) SillDialog(null, onSave = { n, a, v -> window = n; c.write { garden.addSill(n, a, v) }; addWindow = false }, onDelete = null, onClose = { addWindow = false })
}
