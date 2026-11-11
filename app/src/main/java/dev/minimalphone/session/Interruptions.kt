package dev.minimalphone.session

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * Optional reversible Do Not Disturb helper for MinimalBro.
 */
object Interruptions {

    fun hasAccess(context: Context): Boolean = runCatching {
        notificationManager(context).isNotificationPolicyAccessGranted
    }.getOrDefault(false)

    fun accessSettingsIntent(context: Context): Intent? = runCatching {
        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).takeIf {
            it.resolveActivity(context.packageManager) != null
        }
    }.getOrNull()

    fun pause(context: Context): Boolean {
        if (!hasAccess(context)) return false
        return try {
            val manager = notificationManager(context)
            if (manager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
                false
            } else {
                manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                true
            }
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    fun resume(context: Context) {
        if (!hasAccess(context)) return
        try {
            notificationManager(context)
                .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        } catch (_: SecurityException) {
        } catch (_: IllegalArgumentException) {
        }
    }

    fun notificationsAllowed(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun notificationManager(context: Context): NotificationManager =
        context.getSystemService(NotificationManager::class.java)
}
