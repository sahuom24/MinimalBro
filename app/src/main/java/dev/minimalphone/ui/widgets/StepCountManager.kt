package dev.minimalphone.ui.widgets

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

/**
 * Reads real-time hardware step counts for the current day.
 * Manages sensor registration, day change resets, and local persistence.
 */
class StepCountManager private constructor(context: Context) : SensorEventListener {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val sensorManager =
        appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val _dailySteps = MutableStateFlow(readSavedTodaySteps())
    val dailySteps: StateFlow<Int> = _dailySteps.asStateFlow()

    private val _hasSensor = MutableStateFlow(stepSensor != null)
    val hasSensor: StateFlow<Boolean> = _hasSensor.asStateFlow()

    init {
        register()
    }

    fun register() {
        stepSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun unregister() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return
        val rawTotal = event.values.getOrNull(0)?.toInt() ?: return
        handleNewSensorValue(rawTotal)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun handleNewSensorValue(rawTotal: Int) {
        val todayKey = getTodayDateKey()
        val savedDay = prefs.getString(KEY_SAVED_DAY, null)
        var baseOffset = prefs.getInt(KEY_BASE_OFFSET, -1)

        if (savedDay != todayKey || baseOffset == -1 || rawTotal < baseOffset) {
            // New day or reboot detected
            baseOffset = rawTotal
            prefs.edit()
                .putString(KEY_SAVED_DAY, todayKey)
                .putInt(KEY_BASE_OFFSET, baseOffset)
                .putInt(KEY_TODAY_STEPS, 0)
                .apply()
        }

        val calculated = (rawTotal - baseOffset).coerceAtLeast(0)
        _dailySteps.value = calculated
        prefs.edit().putInt(KEY_TODAY_STEPS, calculated).apply()
    }

    private fun readSavedTodaySteps(): Int {
        val todayKey = getTodayDateKey()
        val savedDay = prefs.getString(KEY_SAVED_DAY, null)
        if (savedDay != todayKey) return 0
        return prefs.getInt(KEY_TODAY_STEPS, 0)
    }

    private fun getTodayDateKey(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}_${cal.get(Calendar.DAY_OF_YEAR)}"
    }

    companion object {
        private const val PREFS_NAME = "minimal_step_prefs"
        private const val KEY_SAVED_DAY = "saved_day"
        private const val KEY_BASE_OFFSET = "base_offset"
        private const val KEY_TODAY_STEPS = "today_steps"

        @Volatile
        private var instance: StepCountManager? = null

        fun get(context: Context): StepCountManager =
            instance ?: synchronized(this) {
                instance ?: StepCountManager(context).also { instance = it }
            }
    }
}
