package dev.minimalphone.ui.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

/**
 * Manages hosting native third-party Android App Widgets (e.g. Google Calendar,
 * Samsung Health, Google Fit, Spotify, etc.) directly inside MinimalBro.
 */
class AppWidgetHostHelper private constructor(context: Context) {

    private val appContext = context.applicationContext
    val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(appContext)
    val appWidgetHost: AppWidgetHost = AppWidgetHost(appContext, APPWIDGET_HOST_ID)

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _widgetIds = MutableStateFlow(readSavedWidgetIds())
    val widgetIds: StateFlow<List<Int>> = _widgetIds.asStateFlow()

    fun startListening() {
        runCatching { appWidgetHost.startListening() }
    }

    fun stopListening() {
        runCatching { appWidgetHost.stopListening() }
    }

    fun allocateWidgetId(): Int {
        return appWidgetHost.allocateAppWidgetId()
    }

    fun bindWidget(widgetId: Int, provider: ComponentName): Boolean {
        return runCatching {
            appWidgetManager.bindAppWidgetIdIfAllowed(widgetId, provider)
        }.getOrDefault(false)
    }

    fun addWidgetId(id: Int) {
        val current = _widgetIds.value
        if (id !in current) {
            val updated = current + id
            _widgetIds.value = updated
            writeSavedWidgetIds(updated)
        }
    }

    fun removeWidgetId(id: Int) {
        val updated = _widgetIds.value.filter { it != id }
        _widgetIds.value = updated
        writeSavedWidgetIds(updated)
    }

    fun deleteWidget(id: Int) {
        runCatching { appWidgetHost.deleteAppWidgetId(id) }
        removeWidgetId(id)
    }

    fun getInstalledProviders(): List<AppWidgetProviderInfo> {
        return runCatching {
            appWidgetManager.installedProviders
        }.getOrDefault(emptyList()).sortedBy { info ->
            info.loadLabel(appContext.packageManager).lowercase()
        }
    }

    private fun readSavedWidgetIds(): List<Int> {
        val raw = prefs.getString(KEY_WIDGET_IDS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val id = arr.getInt(i)
                    // Ensure the widget provider is still valid
                    if (appWidgetManager.getAppWidgetInfo(id) != null) {
                        add(id)
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeSavedWidgetIds(ids: List<Int>) {
        val arr = JSONArray()
        ids.forEach { arr.put(it) }
        prefs.edit().putString(KEY_WIDGET_IDS, arr.toString()).apply()
    }

    companion object {
        const val APPWIDGET_HOST_ID = 2026
        private const val PREFS_NAME = "minimal_appwidget_prefs"
        private const val KEY_WIDGET_IDS = "saved_widget_ids"

        @Volatile
        private var instance: AppWidgetHostHelper? = null

        fun get(context: Context): AppWidgetHostHelper =
            instance ?: synchronized(this) {
                instance ?: AppWidgetHostHelper(context).also { instance = it }
            }
    }
}
