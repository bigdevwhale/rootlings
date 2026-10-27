package app.rootlings.domain

import android.util.Base64
import app.rootlings.data.OwnSpecies
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

/** A rooted cutting given away: who it is, whose child, and, for a species of one's own, what it is. */
data class Gift(val species: String, val name: String, val lineage: String, val hue: Int, val from: String, val own: OwnSpecies? = null)

/**
 * A gift travels as a short text code anyone can paste into a messenger. No server: the code itself
 * carries the plant's name, species and ancestry.
 */
object GiftCode {
    private const val PREFIX = "ROOT1-"

    fun encode(g: Gift): String {
        val j = JSONObject().put("s", g.species).put("n", g.name).put("l", g.lineage).put("h", g.hue).put("f", g.from)
        g.own?.let { o ->
            j.put("o", JSONObject().put("n", o.name).put("la", o.latin).put("k", o.kind).put("e", o.element).put("li", o.light)
                .put("w", o.water).put("hu", o.humid).put("t", o.toxic).put("r", o.rest).put("b", o.bloom ?: 0).put("c", o.cutting))
        }
        val raw = j.toString().toByteArray(Charsets.UTF_8)
        val d = Deflater(9).apply { setInput(raw); finish() }
        val out = ByteArrayOutputStream()
        val buf = ByteArray(512)
        while (!d.finished()) out.write(buf, 0, d.deflate(buf))
        return PREFIX + Base64.encodeToString(out.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    /** The gift in a pasted text, wherever the code sits in it; null if there's none. */
    fun decode(text: String): Gift? = runCatching {
        val code = Regex("ROOT1-([A-Za-z0-9_-]+)").find(text)?.groupValues?.get(1) ?: return null
        val bytes = Base64.decode(code, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val inf = Inflater().apply { setInput(bytes) }
        val out = ByteArrayOutputStream()
        val buf = ByteArray(512)
        while (!inf.finished()) { val n = inf.inflate(buf); if (n == 0 && inf.needsInput()) break; out.write(buf, 0, n) }
        val j = JSONObject(out.toString(Charsets.UTF_8.name()))
        val own = j.optJSONObject("o")?.let { o ->
            OwnSpecies(name = o.getString("n"), latin = o.optString("la"), kind = o.getString("k"), element = o.getString("e"), light = o.getInt("li"),
                water = o.getInt("w"), humid = o.getBoolean("hu"), toxic = o.getBoolean("t"), rest = o.getBoolean("r"),
                bloom = o.optInt("b").takeIf { it != 0 }, hue = j.optInt("h", 110), cutting = o.optString("c", "SOIL"))
        }
        Gift(j.getString("s"), j.getString("n"), j.optString("l"), j.optInt("h", 110), j.optString("f"), own)
    }.getOrNull()
}
