package dev.minimalphone.session

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.minimalphone.R
import dev.minimalphone.ui.MainActivity
import java.text.DateFormat
import java.util.Date

/**
 * Minimal, quiet notifications for active minimal sessions and timed detox completion.
 */
object MinimalNotifier {

    private const val CHANNEL_ONGOING = "minimal_in_progress"
    private const val CHANNEL_DONE = "minimal_complete"
    private const val ID_ONGOING = 2001
    private const val ID_DONE = 2002

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ONGOING,
                "Minimal mode active",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows quiet MinimalBro status while active."
                setShowBadge(false)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DONE,
                "Minimal commitment complete",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Shown when a timed Minimal detox finishes."
                setShowBadge(false)
            },
        )
    }

    fun showOngoing(context: Context, active: ActiveMinimalSession) {
        ensureChannels(context)
        val endsAt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(active.endsAtMillis))
        val text = if (active.isIndefinite) {
            "Ongoing minimal phone · exit anytime"
        } else {
            "Minimal phone active until $endsAt"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_stat_minimal)
            .setContentTitle("MinimalBro active")
            .setContentText(text)
            .setContentIntent(openApp(context))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (active.isIndefinite) {
            builder
                .setWhen(active.startedAtMillis)
                .setUsesChronometer(true)
                .setChronometerCountDown(false)
        } else {
            builder
                .setWhen(active.endsAtMillis)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
        }

        post(context, ID_ONGOING, builder.build())
    }

    fun clearOngoing(context: Context) {
        runCatching { NotificationManagerCompat.from(context).cancel(ID_ONGOING) }
    }

    fun showComplete(context: Context, minutes: Int) {
        ensureChannels(context)
        val label = if (minutes >= 60) {
            val hours = minutes / 60
            "$hours ${if (hours == 1) "hour" else "hours"} minimal."
        } else {
            "$minutes ${if (minutes == 1) "minute" else "minutes"} minimal."
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_stat_minimal)
            .setContentTitle("Minimal commitment complete")
            .setContentText(label)
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        post(context, ID_DONE, notification)
    }

    private fun post(context: Context, id: Int, notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
