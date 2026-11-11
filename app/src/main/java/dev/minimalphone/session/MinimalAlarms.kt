package dev.minimalphone.session

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Schedules end-of-commitment bookkeeping alarms for timed minimal detox sessions. */
object MinimalAlarms {

    private const val REQUEST_CODE = 81

    fun schedule(context: Context, endsAtMillis: Long) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        runCatching {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
                manager.canScheduleExactAlarms()
            ) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAtMillis, pending(context))
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAtMillis, pending(context))
            }
        }
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        runCatching { manager.cancel(pending(context)) }
    }

    private fun pending(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, MinimalAlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

class MinimalAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        MinimalRepository.get(context).syncWithClock()
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val repo = MinimalRepository.get(context)
            repo.syncWithClock()
            val active = repo.active.value
            if (active != null && !active.isIndefinite) {
                MinimalAlarms.schedule(context, active.endsAtMillis)
            }
        }
    }
}
