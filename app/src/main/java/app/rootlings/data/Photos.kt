package app.rootlings.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/** Plant photos live in the app's own files; the system camera writes them through a FileProvider. */
object Photos {
    private fun dir(ctx: Context) = File(ctx.filesDir, "photos").apply { mkdirs() }

    /** A fresh file for the camera to fill, and the content Uri to hand it. */
    fun fresh(ctx: Context): Pair<File, Uri> {
        val f = File(dir(ctx), UUID.randomUUID().toString() + ".jpg")
        return f to FileProvider.getUriForFile(ctx, ctx.packageName + ".photos", f)
    }

    fun file(ctx: Context, name: String) = File(dir(ctx), name)

    /** A small decode for showing on a card. */
    fun thumb(ctx: Context, name: String, max: Int = 480): Bitmap? = runCatching {
        val f = file(ctx, name)
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, o)
        var s = 1
        while (o.outWidth / (s * 2) >= max && o.outHeight / (s * 2) >= max) s *= 2
        BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = s })?.let { rotate(f, it) }
    }.getOrNull()

    private fun rotate(f: File, b: Bitmap): Bitmap {
        val deg = runCatching {
            when (androidx.exifinterface.media.ExifInterface(f.path).getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, 1)) {
                6 -> 90f; 3 -> 180f; 8 -> 270f; else -> 0f
            }
        }.getOrDefault(0f)
        if (deg == 0f) return b
        val m = android.graphics.Matrix().apply { postRotate(deg) }
        return Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true)
    }

    /**
     * The plant's own green: the average hue of the leafy pixels in its photo, so a pale variegated
     * pothos and a dark one get different spirits. Null when the photo has too little green.
     */
    fun hue(ctx: Context, name: String): Int? {
        val b = thumb(ctx, name, 64) ?: return null
        val hsv = FloatArray(3)
        var x = 0.0
        var y = 0.0
        var n = 0
        for (j in 0 until b.height step 2) for (i in 0 until b.width step 2) {
            Color.colorToHSV(b.getPixel(i, j), hsv)
            if (hsv[0] !in 50f..200f || hsv[1] < .18f || hsv[2] < .15f) continue
            x += kotlin.math.cos(Math.toRadians(hsv[0].toDouble())); y += kotlin.math.sin(Math.toRadians(hsv[0].toDouble())); n++
        }
        if (n < 20) return null
        return ((Math.toDegrees(kotlin.math.atan2(y, x)) + 360) % 360).toInt()
    }

    fun delete(ctx: Context, name: String?) { if (name != null) runCatching { file(ctx, name).delete() } }
}
