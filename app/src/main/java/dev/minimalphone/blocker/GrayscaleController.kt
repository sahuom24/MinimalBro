package dev.minimalphone.blocker

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log

/**
 * Manages system-level monochrome/grayscale display mode to strip visual dopamine
 * from high-friction and dopamine-trap applications.
 *
 * Utilizes Android's accessibility daltonizer:
 * - accessibility_display_daltonizer_enabled (1 for ON, 0 for OFF)
 * - accessibility_display_daltonizer (0 = Monochromacy)
 *
 * Requires WRITE_SECURE_SETTINGS (grantable via one-time ADB command).
 */
object GrayscaleController {

    private const val TAG = "GrayscaleController"
    const val SETTING_DALTONIZER_ENABLED = "accessibility_display_daltonizer_enabled"
    const val SETTING_DALTONIZER = "accessibility_display_daltonizer"
    const val MONOCHROMACY_VALUE = 0

    const val ADB_PERMISSION_COMMAND =
        "adb shell pm grant com.focusbro.minimal android.permission.WRITE_SECURE_SETTINGS"

    fun hasPermission(context: Context): Boolean {
        return context.checkCallingOrSelfPermission(
            android.Manifest.permission.WRITE_SECURE_SETTINGS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isGrayscaleActive(context: Context): Boolean {
        return runCatching {
            Settings.Secure.getInt(context.contentResolver, SETTING_DALTONIZER_ENABLED, 0) == 1
        }.getOrDefault(false)
    }

    /**
     * Programmatically forces the screen into monochrome when [enabled] is true,
     * or restores full color when [enabled] is false.
     * Returns true if settings write succeeded, false if lacking permission or error.
     */
    fun setGrayscale(context: Context, enabled: Boolean): Boolean {
        return runCatching {
            val resolver = context.contentResolver
            if (enabled) {
                Settings.Secure.putInt(resolver, SETTING_DALTONIZER, MONOCHROMACY_VALUE)
                Settings.Secure.putInt(resolver, SETTING_DALTONIZER_ENABLED, 1)
            } else {
                Settings.Secure.putInt(resolver, SETTING_DALTONIZER_ENABLED, 0)
            }
            true
        }.onFailure { err ->
            Log.w(TAG, "Cannot modify daltonizer secure settings: ${err.message}")
        }.getOrDefault(false)
    }
}
