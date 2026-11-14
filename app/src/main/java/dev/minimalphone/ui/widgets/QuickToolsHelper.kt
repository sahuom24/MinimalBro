package dev.minimalphone.ui.widgets

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.CalendarContract
import android.provider.MediaStore
import dev.minimalphone.apps.DefaultCommunicationApps

/**
 * Essential quick utility handlers for MinimalBro's widget dashboard.
 */
object QuickToolsHelper {

    private const val SCRATCHPAD_PREFS = "minimal_scratchpad_prefs"
    private const val KEY_NOTE = "daily_intention_note"

    /**
     * Opens direct UPI scanner, or fallback to default UPI / Camera app.
     */
    fun openUpiScanner(context: Context): Boolean {
        // 1. Try launching dedicated camera / QR scanning
        val upiPkg = DefaultCommunicationApps.getUpiPackage(context)
        if (upiPkg != null) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(upiPkg)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (runCatching { context.startActivity(launchIntent) }.isSuccess) {
                    return true
                }
            }
        }

        // 2. Generic UPI URI intent
        val upiIntent = Intent(Intent.ACTION_VIEW, Uri.parse("upi://pay")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (upiIntent.resolveActivity(context.packageManager) != null) {
            if (runCatching { context.startActivity(upiIntent) }.isSuccess) {
                return true
            }
        }

        // 3. Fallback to Camera
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (cameraIntent.resolveActivity(context.packageManager) != null) {
            return runCatching { context.startActivity(cameraIntent) }.isSuccess
        }

        return false
    }

    /**
     * Opens the device's default Calendar application.
     */
    fun openCalendar(context: Context) {
        val calPkg = DefaultCommunicationApps.getCalendarPackage(context)
        val intent = (if (calPkg != null) context.packageManager.getLaunchIntentForPackage(calPkg) else null)
            ?: Intent(Intent.ACTION_VIEW).setData(CalendarContract.CONTENT_URI)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        runCatching { context.startActivity(intent) }
    }

    /**
     * Toggles the device flashlight / torch.
     */
    fun toggleFlashlight(context: Context, currentState: Boolean, onStateChanged: (Boolean) -> Unit) {
        runCatching {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
            val targetState = !currentState
            cameraManager.setTorchMode(cameraId, targetState)
            onStateChanged(targetState)
        }.onFailure {
            onStateChanged(false)
        }
    }

    /**
     * Cycles audio ringer mode between Normal, Vibrate, and Silent.
     */
    fun cycleRingerMode(context: Context): Int {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return AudioManager.RINGER_MODE_NORMAL
        val nextMode = when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
            AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
            else -> AudioManager.RINGER_MODE_NORMAL
        }
        runCatching { audioManager.ringerMode = nextMode }
        return audioManager.ringerMode
    }

    fun getCurrentRingerMode(context: Context): Int {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        return audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
    }

    fun readDailyNote(context: Context): String {
        return context.getSharedPreferences(SCRATCHPAD_PREFS, Context.MODE_PRIVATE)
            .getString(KEY_NOTE, "") ?: ""
    }

    fun saveDailyNote(context: Context, note: String) {
        context.getSharedPreferences(SCRATCHPAD_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NOTE, note)
            .apply()
    }
}
