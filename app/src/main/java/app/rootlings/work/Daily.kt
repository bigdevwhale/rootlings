package app.rootlings.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.rootlings.MainActivity
import app.rootlings.R
import app.rootlings.container
import app.rootlings.data.Garden
import app.rootlings.localized
import app.rootlings.ru
import app.rootlings.ui.Home
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Once a morning: fresh weather, then at most one quiet note — whose soil is worth a finger check,
 * or that the radiators came on.
 */
class Daily(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container
        runCatching { c.refreshWeather() }
        val s = c.settings.now()
        val now = LocalDateTime.now()
        if (!s.onboarded || !s.notify || now.hour < 8 || now.hour > 21) return Result.success()
        val today = now.toLocalDate().toEpochDay()
        if (s.notifiedDay == today) return Result.success()
        val g = c.garden.state.first()
        val ctx = applicationContext.localized()
        val home = Home(g, c.content, s, Garden.sky(g, s.place?.lat, s.place?.lon), now, ctx.ru)
        val due = home.needsCheck
        val heating = home.heating && s.heatingSeen != now.year && now.monthValue in listOf(9, 10, 11, 12)
        val text = when {
            heating -> ctx.getString(R.string.n_heating)
            due.isNotEmpty() -> ctx.getString(R.string.n_check_text, due.take(4).joinToString(", ") { it.name })
            else -> return Result.success()
        }
        post(ctx, ctx.getString(if (heating) R.string.n_heating_title else R.string.n_check_title), text)
        c.settings.update { it.copy(notifiedDay = today, heatingSeen = if (heating) now.year else it.heatingSeen) }
        return Result.success()
    }

    private fun post(ctx: Context, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, ctx.getString(R.string.channel), NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        nm.notify(1, NotificationCompat.Builder(ctx, CHANNEL).setSmallIcon(R.drawable.ic_stat_leaf).setContentTitle(title).setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text)).setContentIntent(open).setAutoCancel(true).build())
    }

    companion object {
        private const val CHANNEL = "morning"

        /** Every few hours; the worker itself picks the first run after eight in the morning. */
        fun schedule(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<Daily>(4, TimeUnit.HOURS).setInitialDelay(Duration.ofMinutes(20)).build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("daily", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
