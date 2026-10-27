package app.rootlings.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.rootlings.R
import app.rootlings.data.Element

/** The colours of an old handheld field guide: pale green screen, cream paper, red case. */
@Immutable
object C {
    val bg = Color(0xFFE4ECD0)
    val paper = Color(0xFFF8F8E8)
    val paper2 = Color(0xFFD8DCC0)
    val screen = Color(0xFFB8D0A0)
    val screenD = Color(0xFFA0B888)
    val ink = Color(0xFF202818)
    val muted = Color(0xFF607050)
    val faint = Color(0xFFA0A890)
    val frame = Color(0xFF6A7A58)
    val red = Color(0xFFC83828)
    val redDeep = Color(0xFF8C2018)
    val redLight = Color(0xFFF07060)
    val green = Color(0xFF48A058)
    val greenDeep = Color(0xFF286838)
    val greenLight = Color(0xFF78C880)
    val gold = Color(0xFFE8B030)
    val goldDeep = Color(0xFFB07818)
    val blue = Color(0xFF5898E8)
    val brown = Color(0xFF8C6A48)
    val night = Color(0xFF141A10)
    val info = Color(0xFFD8E8C0)

    fun element(e: Element) = when (e) {
        Element.DESERT -> Color(0xFFD89838)
        Element.MIST -> Color(0xFF7090A0)
        Element.RAIN -> Color(0xFF3878C0)
        Element.MEADOW -> Color(0xFF88B038)
        Element.FOREST -> Color(0xFF587838)
    }
}

/** m6x11 by Daniel Linssen with Cyrillic by Natto Games (free with attribution): one family, sizes set the hierarchy. */
val Pixel = FontFamily(Font(R.font.m6x11, FontWeight.Normal))

object T {
    val Display = TextStyle(fontFamily = Pixel, fontSize = 32.sp, lineHeight = 31.sp, color = C.ink)
    val Title = TextStyle(fontFamily = Pixel, fontSize = 27.sp, lineHeight = 27.sp, color = C.ink)
    val Heading = TextStyle(fontFamily = Pixel, fontSize = 23.sp, lineHeight = 24.sp, color = C.ink)
    val Body = TextStyle(fontFamily = Pixel, fontSize = 20.sp, lineHeight = 21.sp, color = C.ink)
    val Say = TextStyle(fontFamily = Pixel, fontSize = 22.sp, lineHeight = 23.sp, color = C.ink)
    val Small = TextStyle(fontFamily = Pixel, fontSize = 16.sp, lineHeight = 17.sp, color = C.muted)
    val Eyebrow = TextStyle(fontFamily = Pixel, fontSize = 16.sp, lineHeight = 17.sp, letterSpacing = 0.04.em, color = C.muted)
    val Button = TextStyle(fontFamily = Pixel, fontSize = 22.sp, lineHeight = 22.sp)
    val Number = TextStyle(fontFamily = Pixel, fontSize = 28.sp, lineHeight = 26.sp, color = C.ink)
}

@Composable
fun RootlingsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = C.green, onPrimary = C.paper, background = C.bg, surface = C.paper, onSurface = C.ink,
            onBackground = C.ink, surfaceContainer = C.paper, surfaceContainerHigh = C.paper, surfaceContainerHighest = C.paper2,
            outline = C.ink, error = C.red,
        ),
        typography = MaterialTheme.typography.copy(bodyLarge = T.Body, bodyMedium = T.Body, bodySmall = T.Small, labelLarge = T.Button, titleLarge = T.Title),
        content = content,
    )
}
