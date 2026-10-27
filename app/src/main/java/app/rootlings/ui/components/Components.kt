package app.rootlings.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.rootlings.ui.art.Buf
import app.rootlings.ui.art.toBitmap
import app.rootlings.ui.theme.C
import app.rootlings.ui.theme.T
import kotlin.math.roundToInt

/** One "UI pixel": outlines, bevels and notched corners come in multiples of it. */
val PX: Dp = 2.dp

/** A pixel box: a dark outline with notched corners, a fill, a light bevel on top and a darker lip below. */
fun Modifier.pixelBox(fill: Color, outline: Color = C.ink, bevel: Color? = Color.White.copy(alpha = .45f), lip: Color? = Color(0x30203018), pressed: Boolean = false): Modifier = drawBehind {
    val p = PX.toPx()
    val w = size.width
    val h = size.height
    drawRect(outline, Offset(p, 0f), Size(w - 2 * p, h))
    drawRect(outline, Offset(0f, p), Size(w, h - 2 * p))
    drawRect(fill, Offset(2 * p, p), Size(w - 4 * p, h - 2 * p))
    drawRect(fill, Offset(p, 2 * p), Size(w - 2 * p, h - 4 * p))
    if (!pressed) {
        bevel?.let { drawRect(it, Offset(2 * p, p), Size(w - 4 * p, p)) }
        lip?.let { drawRect(it, Offset(2 * p, h - 3 * p), Size(w - 4 * p, 2 * p)) }
    }
}

/** The old handheld text box: an ink frame, a cream gap, a thin inner frame. */
fun Modifier.textBox(fill: Color = C.paper): Modifier = drawBehind {
    val p = PX.toPx()
    val w = size.width
    val h = size.height
    drawRect(C.ink, Offset(p, 0f), Size(w - 2 * p, h))
    drawRect(C.ink, Offset(0f, p), Size(w, h - 2 * p))
    drawRect(fill, Offset(p, p), Size(w - 2 * p, h - 2 * p))
    drawRect(C.frame, Offset(3 * p, 3 * p), Size(w - 6 * p, h - 6 * p))
    drawRect(fill, Offset(4 * p, 4 * p), Size(w - 8 * p, h - 8 * p))
}

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    fill: Color = C.paper,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(
        modifier
            .pixelBox(fill, pressed = pressed && onClick != null)
            .then(if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** A text box with words in it, as the spirit speaks. */
@Composable
fun Says(text: String, modifier: Modifier = Modifier, style: TextStyle = T.Say) {
    Box(modifier.fillMaxWidth().textBox().padding(horizontal = 16.dp, vertical = 14.dp)) { Text(text, style = style) }
}

enum class Kind { GREEN, RED, GHOST, GOLD }

@Composable
fun PixelButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, kind: Kind = Kind.GREEN, enabled: Boolean = true) {
    val (fill, ink, lip) = when (kind) {
        Kind.GREEN -> Triple(C.green, C.paper, C.greenDeep)
        Kind.RED -> Triple(C.red, C.paper, C.redDeep)
        Kind.GHOST -> Triple(C.paper, C.ink, C.paper2)
        Kind.GOLD -> Triple(C.gold, C.ink, C.goldDeep)
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val a = if (enabled) 1f else .4f
    Box(
        modifier
            .offset { IntOffset(0, if (pressed) PX.roundToPx() else 0) }
            .pixelBox(fill.copy(alpha = a), C.ink, Color.White.copy(alpha = .35f * a), lip, pressed)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = T.Button.copy(color = ink.copy(alpha = a)), textAlign = TextAlign.Center, maxLines = 1)
    }
}

/** A ▶ menu of choices, the handheld way. Disabled rows are greyed out with a note. */
@Composable
fun Menu(items: List<MenuItem>, modifier: Modifier = Modifier) {
    Column(modifier) {
        for (it in items) {
            Row(
                Modifier.fillMaxWidth().clickable(enabled = it.enabled, onClick = it.onClick).padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (it.selected) "▶" else " ", style = T.Small.copy(color = C.ink), modifier = Modifier.width(18.dp))
                Text(it.text, style = T.Heading.copy(color = if (it.enabled) C.ink else C.faint), modifier = Modifier.weight(1f, fill = false))
                if (it.note.isNotEmpty()) Text("  " + it.note, style = T.Small)
            }
        }
    }
}

data class MenuItem(val text: String, val note: String = "", val enabled: Boolean = true, val selected: Boolean = false, val onClick: () -> Unit)

/** An HP-style bar with an optional target band. */
@Composable
fun Bar(fraction: Float, color: Color, modifier: Modifier = Modifier, band: ClosedFloatingPointRange<Float>? = null, tick: Float? = null) {
    Box(
        modifier.fillMaxWidth().height(14.dp).drawBehind {
            val p = PX.toPx()
            drawRect(C.ink)
            drawRect(C.paper, Offset(p, p), Size(size.width - 2 * p, size.height - 2 * p))
            val inner = size.width - 2 * p
            drawRect(color, Offset(p, p), Size(inner * fraction.coerceIn(0f, 1f), size.height - 2 * p))
            drawRect(Color(0x30000000), Offset(p, size.height - 3 * p), Size(inner * fraction.coerceIn(0f, 1f), 2 * p))
            band?.let {
                val x0 = p + inner * it.start.coerceIn(0f, 1f)
                val x1 = p + inner * it.endInclusive.coerceIn(0f, 1f)
                var x = x0
                while (x < x1) { drawRect(Color(0x30202818), Offset(x, p), Size(p, size.height - 2 * p)); x += 2 * p }
                drawRect(C.ink, Offset(x0, 0f), Size(p, size.height)); drawRect(C.ink, Offset(x1 - p, 0f), Size(p, size.height))
            }
            tick?.let { drawRect(C.red, Offset(p + inner * it - p / 2, 0f), Size(p, size.height)) }
        },
    )
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = C.muted) =
    Text(text.uppercase(), style = T.Eyebrow.copy(color = color), modifier = modifier)

/** A still pixel picture scaled with hard pixels, [scale] dp per art pixel. */
@Composable
fun BufImage(buf: Buf, modifier: Modifier = Modifier, scale: Dp = 2.dp) {
    val image = remember(buf) { buf.toBitmap().asImageBitmap() }
    Canvas(modifier.size(scale * buf.w, scale * buf.h)) {
        drawImage(image, srcOffset = IntOffset.Zero, srcSize = IntSize(buf.w, buf.h), dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()), filterQuality = FilterQuality.None)
    }
}

/** A red Pokédex-style title bar with a back arrow. */
@Composable
fun TopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().background(C.red).statusBarsPadding().padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(20.dp)) {
                val p = size.width / 10
                for (i in 0 until 5) {
                    drawRect(C.paper, Offset((1 + i) * p, (5 - i) * p - p / 2), Size(p * 2, p))
                    drawRect(C.paper, Offset((1 + i) * p, (4 + i) * p + p / 2), Size(p * 2, p))
                }
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(title.uppercase(), style = T.Title.copy(color = C.paper), modifier = Modifier.weight(1f), maxLines = 1)
        action?.invoke()
    }
}

/** A full-screen page over the tabs, with its own title bar; back closes it. */
@Composable
fun Page(title: String, onBack: () -> Unit, action: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(C.bg).clickable(remember { MutableInteractionSource() }, null) {}.imePadding()) {
        TopBar(title, onBack, action = action)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 12.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/** A handheld text box at the bottom of the screen over a dim layer; tap outside to close. */
@Composable
fun Dialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onDismiss)
    Box(Modifier.fillMaxSize().background(Color(0x99101408)).clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss).imePadding(), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 520.dp).navigationBarsPadding().padding(10.dp).padding(bottom = 14.dp)
                .textBox().clickable(remember { MutableInteractionSource() }, null) {}.padding(horizontal = 18.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

/** A plain pixel text field. */
@Composable
fun Field(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, style: TextStyle = T.Body, single: Boolean = true) {
    BasicTextField(value, onChange, modifier.fillMaxWidth().pixelBox(C.paper), textStyle = style, singleLine = single, cursorBrush = SolidColor(C.ink),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        decorationBox = { inner -> Box(Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) { if (value.isEmpty()) Text(hint, style = style.copy(color = C.faint)); inner() } })
}

/** − n + */
@Composable
fun Stepper(value: Int, onChange: (Int) -> Unit, range: IntRange) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PixelButton("−", { onChange((value - 1).coerceIn(range)) }, kind = Kind.GHOST, enabled = value > range.first)
        Text(value.toString(), style = T.Number, modifier = Modifier.widthIn(min = 40.dp), textAlign = TextAlign.Center)
        PixelButton("+", { onChange((value + 1).coerceIn(range)) }, kind = Kind.GHOST, enabled = value < range.last)
    }
}

/** A coloured type badge, like «ТУМАН.». */
@Composable
fun Badge(text: String, color: Color) {
    Text(text.uppercase(), style = T.Small.copy(color = C.paper), modifier = Modifier.pixelBox(color, bevel = null, lip = null).padding(horizontal = 8.dp, vertical = 3.dp))
}

/** A choice chip that stays pressed when on. */
@Composable
fun Chip(text: String, on: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(text, style = T.Body.copy(color = if (on) C.paper else C.ink), maxLines = 1,
        modifier = modifier.pixelBox(if (on) C.ink else C.paper, pressed = on).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 7.dp))
}

@Composable
fun Gap(h: Dp = 8.dp) = Spacer(Modifier.height(h))

/** A box filling the width, its contents centred. */
@Composable
fun Centered(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) =
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center, content = content)
