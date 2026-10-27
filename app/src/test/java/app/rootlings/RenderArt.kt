package app.rootlings

import app.rootlings.data.Kind
import app.rootlings.data.Mood
import app.rootlings.domain.Hour
import app.rootlings.domain.SunPos
import app.rootlings.ui.art.Buf
import app.rootlings.ui.art.Foes
import app.rootlings.ui.art.Look
import app.rootlings.ui.art.Outside
import app.rootlings.ui.art.Spirit
import app.rootlings.ui.art.WindowScene
import app.rootlings.ui.art.arena
import app.rootlings.ui.art.blit
import app.rootlings.ui.art.cuttingArt
import app.rootlings.ui.art.rgb
import app.rootlings.ui.art.toBitmap
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Sheets of every sprite and scene, drawn to PNGs for a look (run with `-Prender`). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class RenderArt {
    private val out = File(System.getProperty("render.out") ?: "build/art").apply { mkdirs() }

    private fun save(b: Buf, name: String, scale: Int = 4) {
        val big = android.graphics.Bitmap.createScaledBitmap(b.toBitmap(), b.w * scale, b.h * scale, false)
        File(out, "$name.png").outputStream().use { big.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    private val hues = mapOf(Kind.ROSETTE to 170, Kind.ALOE to 120, Kind.BALL to 100, Kind.PATTERN to 128, Kind.CANE to 100, Kind.ZZ to 125)
    private val flowers = mapOf(Kind.ORCHID to 0xFFE070C0.toInt(), Kind.SPATHE to 0xFFF4F4E8.toInt(), Kind.VIOLET to 0xFF9060C8.toInt(), Kind.BULB to 0xFFD83838.toInt(), Kind.TREE to 0xFFF8F8F0.toInt(), Kind.BUSH to 0xFFF06080.toInt())

    @Test fun kinds() {
        val kinds = Kind.entries
        val b = Buf(34 * 3, 34 * kinds.size)
        b.r(0, 0, b.w, b.h, rgb(0xE4ECD0))
        kinds.forEachIndexed { i, k ->
            for (s in 1..3) b.blit(Spirit.draw(Look(k, hues[k] ?: 120, 44, 42, s, Mood.CALM, if (s == 3) flowers[k] else null, i)), (s - 1) * 34 + 1, i * 34 + 1)
        }
        save(b, "kinds", 3)
    }

    @Test fun moods() {
        val moods = Mood.entries
        val b = Buf(34 * moods.size, 34 * 2)
        b.r(0, 0, b.w, b.h, rgb(0xE4ECD0))
        moods.forEachIndexed { i, m ->
            b.blit(Spirit.draw(Look(Kind.MONSTERA, 140, 46, 40, 2, m)), i * 34 + 1, 1)
            b.blit(Spirit.draw(Look(Kind.PADDLE, 96, 38, 44, 2, m)), i * 34 + 1, 35)
        }
        save(b, "moods")
    }

    @Test fun foes() {
        val names = listOf("mite", "scale", "thrips", "mealy", "gnat", "aphid", "whitefly", "rot", "burn", "mildew", "dark", "springtail")
        val b = Buf(34 * 6, 34 * 2)
        b.r(0, 0, b.w, b.h, rgb(0xF8F8E0))
        names.forEachIndexed { i, n -> b.blit(Foes.draw(n), (i % 6) * 34 + 1, (i / 6) * 34 + 1) }
        save(b, "foes")
        save(arena(), "arena")
        save(cuttingArt(Look(Kind.GRASS, 84, 50, 46, 1), "WATER", .7), "cutting")
    }

    @Test fun windows() {
        val looks = listOf(Look(Kind.MONSTERA, 140, 46, 40, 2, Mood.SUN), Look(Kind.FICUS, 96, 40, 38, 1, Mood.CALM, pot = 1), Look(Kind.GRASS, 84, 50, 46, 3, Mood.DRY, pot = 2),
            Look(Kind.ORCHID, 130, 40, 42, 3, Mood.CALM, 0xFFE070C0.toInt(), 3))
        for ((name, o) in listOf(
            "window-sun" to Outside(SunPos(30.0, 175.0), Hour(8.0, 60.0, 20.0, 400.0, 80.0, 0.0), 180, 420.0),
            "window-rain" to Outside(SunPos(20.0, 150.0), Hour(6.0, 90.0, 95.0, 0.0, 60.0, 1.2), 90, 30.0),
            "window-night" to Outside(SunPos(-20.0, 0.0), Hour(2.0, 80.0, 30.0, 0.0, 0.0, 0.0), 0, 0.0),
        )) save(WindowScene(o, looks, 3).frame(4), name)
    }
}
