package dev.minimalphone.session

import android.content.Context
import android.content.SharedPreferences
import dev.minimalphone.apps.DefaultCommunicationApps
import dev.minimalphone.blocker.GrayscaleController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Single local SharedPreferences repository for MinimalBro. */
class MinimalRepository private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<MinimalSettings> = _settings.asStateFlow()

    private val _active = MutableStateFlow(readActive())
    val active: StateFlow<ActiveMinimalSession?> = _active.asStateFlow()

    private val _history = MutableStateFlow(readHistory())
    val history: StateFlow<List<MinimalRecord>> = _history.asStateFlow()

    fun updateSettings(transform: (MinimalSettings) -> MinimalSettings) {
        val updated = transform(_settings.value)
        _settings.value = updated
        val tiersJson = JSONObject().apply {
            updated.appTiers.forEach { (pkg, tier) ->
                put(pkg, tier.name)
            }
        }.toString()

        prefs.edit()
            .putInt(KEY_HOURS, updated.commitmentHours)
            .putStringSet(KEY_ALLOWED, updated.allowedPackages)
            .putStringSet(KEY_FAVORITES, updated.favoritePackages)
            .putString(KEY_APP_TIERS, tiersJson)
            .putBoolean(KEY_ENABLE_GRAYSCALE, updated.enableGrayscale)
            .putBoolean(KEY_SHOW_ICONS, updated.showAppIcons)
            .putBoolean(KEY_PAUSE, updated.pauseInterruptions)
            .putBoolean(KEY_UNLOCK, updated.requireUnlockToEnd)
            .putString(KEY_WALLPAPER, updated.wallpaper.name)
            .apply()
    }

    fun setAppTier(packageName: String, tier: AppTier?) {
        updateSettings { current ->
            if (tier != null) {
                current.copy(
                    allowedPackages = current.allowedPackages + packageName,
                    appTiers = current.appTiers + (packageName to tier),
                )
            } else {
                current.copy(
                    allowedPackages = current.allowedPackages - packageName,
                    favoritePackages = current.favoritePackages - packageName,
                    appTiers = current.appTiers - packageName,
                )
            }
        }
    }

    fun toggleAllowed(packageName: String) {
        updateSettings { current ->
            val removing = packageName in current.allowedPackages
            val nextAllowed = if (removing) {
                current.allowedPackages - packageName
            } else {
                current.allowedPackages + packageName
            }
            val nextFavorites = if (removing) {
                current.favoritePackages - packageName
            } else {
                current.favoritePackages
            }
            val nextTiers = if (removing) {
                current.appTiers - packageName
            } else {
                current.appTiers + (packageName to (current.appTiers[packageName] ?: AppTierClassifier.suggestTier(packageName)))
            }
            current.copy(allowedPackages = nextAllowed, favoritePackages = nextFavorites, appTiers = nextTiers)
        }
    }

    fun toggleFavorite(packageName: String) {
        updateSettings { current ->
            val nextFavorites = if (packageName in current.favoritePackages) {
                current.favoritePackages - packageName
            } else {
                current.favoritePackages + packageName
            }
            // Pinning an app automatically allows it too so the user never pins a hidden app.
            val nextAllowed = current.allowedPackages + packageName
            current.copy(allowedPackages = nextAllowed, favoritePackages = nextFavorites)
        }
    }

    fun setAllowedPackages(packages: Set<String>) {
        updateSettings { current ->
            current.copy(
                allowedPackages = packages,
                favoritePackages = current.favoritePackages.intersect(packages),
            )
        }
    }

    fun clearAllowed() {
        updateSettings { it.copy(allowedPackages = emptySet(), favoritePackages = emptySet()) }
    }

    fun start(): ActiveMinimalSession {
        _active.value?.let { return it }

        val settings = _settings.value
        val now = System.currentTimeMillis()
        val indefinite = settings.commitmentHours <= 0
        val endsAt = if (indefinite) {
            now + INDEFINITE_SPAN_MILLIS
        } else {
            now + settings.commitmentHours.toLong() * 3_600_000L
        }
        val paused = settings.pauseInterruptions && Interruptions.pause(appContext)
        val allowedWithComm = buildSet {
            addAll(settings.allowedPackages)
            addAll(settings.favoritePackages)
            DefaultCommunicationApps.getPhonePackage(appContext)?.let { add(it) }
            DefaultCommunicationApps.getMessagesPackage(appContext)?.let { add(it) }
        }

        val session = ActiveMinimalSession(
            startedAtMillis = now,
            endsAtMillis = endsAt,
            isIndefinite = indefinite,
            allowedPackages = allowedWithComm,
            appTiers = settings.appTiers,
            enableGrayscale = settings.enableGrayscale,
            requireUnlockToEnd = settings.requireUnlockToEnd,
            interruptionsPaused = paused,
        )
        _active.value = session
        writeActive(session)
        MinimalNotifier.showOngoing(appContext, session)
        if (session.isIndefinite) {
            MinimalAlarms.cancel(appContext)
        } else {
            MinimalAlarms.schedule(appContext, session.endsAtMillis)
        }
        return session
    }

    fun endSession(completed: Boolean) {
        val active = _active.value ?: return
        val actualElapsedMins = (((System.currentTimeMillis() - active.startedAtMillis) / 60_000L) + 1)
            .coerceAtLeast(1L)
        val plannedMins = if (active.isIndefinite) {
            actualElapsedMins
        } else {
            (active.totalMillis / 60_000L).coerceAtLeast(1L)
        }
        val minutes = if (completed) {
            plannedMins.toInt()
        } else {
            actualElapsedMins.coerceIn(1L, plannedMins).toInt()
        }

        if (active.interruptionsPaused) Interruptions.resume(appContext)
        GrayscaleController.setGrayscale(appContext, false)
        MicroSessionManager.clearAll()
        MinimalAlarms.cancel(appContext)
        MinimalNotifier.clearOngoing(appContext)
        if (completed) MinimalNotifier.showComplete(appContext, minutes)

        _history.value = (listOf(MinimalRecord(System.currentTimeMillis(), minutes, completed)) +
            _history.value).take(MAX_HISTORY)
        writeHistory(_history.value)

        _active.value = null
        writeActive(null)
    }

    fun extendHours(hours: Int) {
        if (hours <= 0) return
        val active = _active.value ?: return
        if (active.isIndefinite) return
        val extended = active.copy(
            endsAtMillis = active.endsAtMillis + hours.toLong() * 3_600_000L,
        )
        _active.value = extended
        writeActive(extended)
        MinimalNotifier.showOngoing(appContext, extended)
        MinimalAlarms.schedule(appContext, extended.endsAtMillis)
    }

    fun syncWithClock() {
        val active = _active.value ?: return
        if (active.isIndefinite) return
        if (active.isOver()) {
            endSession(completed = true)
        }
    }

    fun clearHistory() {
        _history.value = emptyList()
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun readSettings(): MinimalSettings {
        val wallpaper = runCatching {
            Wallpaper.valueOf(prefs.getString(KEY_WALLPAPER, Wallpaper.PAPER.name)!!)
        }.getOrDefault(Wallpaper.PAPER)

        val allowed = if (prefs.contains(KEY_ALLOWED)) {
            prefs.getStringSet(KEY_ALLOWED, emptySet()).orEmpty().toSet()
        } else {
            DefaultCommunicationApps.resolveDefaultMinimalPackages(appContext)
        }
        val favorites = if (prefs.contains(KEY_FAVORITES)) {
            prefs.getStringSet(KEY_FAVORITES, emptySet()).orEmpty().toSet()
        } else {
            DefaultCommunicationApps.resolveDefaultMinimalPackages(appContext).take(3).toSet()
        }

        val appTiers = parseTiersJson(prefs.getString(KEY_APP_TIERS, null))

        return MinimalSettings(
            commitmentHours = prefs.getInt(KEY_HOURS, 0).coerceIn(0, MinimalConstants.MAX_TIMED_HOURS),
            allowedPackages = allowed,
            favoritePackages = favorites,
            appTiers = appTiers,
            showAppIcons = prefs.getBoolean(KEY_SHOW_ICONS, false),
            pauseInterruptions = prefs.getBoolean(KEY_PAUSE, false),
            requireUnlockToEnd = prefs.getBoolean(KEY_UNLOCK, false),
            wallpaper = wallpaper,
            enableGrayscale = prefs.getBoolean(KEY_ENABLE_GRAYSCALE, true),
        )
    }

    private fun readActive(): ActiveMinimalSession? {
        val started = prefs.getLong(KEY_STARTED_AT, 0L)
        val ends = prefs.getLong(KEY_ENDS_AT, 0L)
        if (started == 0L || ends == 0L) return null
        return ActiveMinimalSession(
            startedAtMillis = started,
            endsAtMillis = ends,
            isIndefinite = prefs.getBoolean(KEY_INDEFINITE, false),
            allowedPackages = prefs.getStringSet(KEY_SESSION_ALLOWED, emptySet()).orEmpty().toSet(),
            appTiers = parseTiersJson(prefs.getString(KEY_SESSION_TIERS, null)),
            enableGrayscale = prefs.getBoolean(KEY_SESSION_GRAYSCALE, true),
            requireUnlockToEnd = prefs.getBoolean(KEY_SESSION_UNLOCK, false),
            interruptionsPaused = prefs.getBoolean(KEY_SESSION_PAUSED, false),
        )
    }

    private fun writeActive(session: ActiveMinimalSession?) {
        val editor = prefs.edit()
        if (session == null) {
            editor
                .remove(KEY_STARTED_AT)
                .remove(KEY_ENDS_AT)
                .remove(KEY_INDEFINITE)
                .remove(KEY_SESSION_ALLOWED)
                .remove(KEY_SESSION_TIERS)
                .remove(KEY_SESSION_GRAYSCALE)
                .remove(KEY_SESSION_UNLOCK)
                .remove(KEY_SESSION_PAUSED)
        } else {
            val tiersJson = JSONObject().apply {
                session.appTiers.forEach { (pkg, tier) ->
                    put(pkg, tier.name)
                }
            }.toString()

            editor
                .putLong(KEY_STARTED_AT, session.startedAtMillis)
                .putLong(KEY_ENDS_AT, session.endsAtMillis)
                .putBoolean(KEY_INDEFINITE, session.isIndefinite)
                .putStringSet(KEY_SESSION_ALLOWED, session.allowedPackages)
                .putString(KEY_SESSION_TIERS, tiersJson)
                .putBoolean(KEY_SESSION_GRAYSCALE, session.enableGrayscale)
                .putBoolean(KEY_SESSION_UNLOCK, session.requireUnlockToEnd)
                .putBoolean(KEY_SESSION_PAUSED, session.interruptionsPaused)
        }
        editor.apply()
    }

    private fun parseTiersJson(raw: String?): Map<String, AppTier> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            buildMap {
                obj.keys().forEach { pkg ->
                    runCatching {
                        val tierName = obj.getString(pkg)
                        put(pkg, AppTier.valueOf(tierName))
                    }
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun readHistory(): List<MinimalRecord> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        MinimalRecord(
                            endedAtMillis = obj.getLong("at"),
                            minutes = obj.getInt("min"),
                            completed = obj.optBoolean("done", true),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeHistory(records: List<MinimalRecord>) {
        val array = JSONArray()
        records.take(MAX_HISTORY).forEach { r ->
            array.put(
                JSONObject()
                    .put("at", r.endedAtMillis)
                    .put("min", r.minutes)
                    .put("done", r.completed),
            )
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "minimal_bro_prefs"
        private const val MAX_HISTORY = 60
        private const val INDEFINITE_SPAN_MILLIS = 3650L * 86_400_000L

        private const val KEY_HOURS = "commitment_hours"
        private const val KEY_ALLOWED = "allowed_packages"
        private const val KEY_FAVORITES = "favorite_packages"
        private const val KEY_APP_TIERS = "app_tiers"
        private const val KEY_ENABLE_GRAYSCALE = "enable_grayscale"
        private const val KEY_SHOW_ICONS = "show_app_icons"
        private const val KEY_PAUSE = "pause_interruptions"
        private const val KEY_UNLOCK = "require_unlock"
        private const val KEY_WALLPAPER = "wallpaper"

        private const val KEY_STARTED_AT = "session_started_at"
        private const val KEY_ENDS_AT = "session_ends_at"
        private const val KEY_INDEFINITE = "session_indefinite"
        private const val KEY_SESSION_ALLOWED = "session_allowed"
        private const val KEY_SESSION_TIERS = "session_tiers"
        private const val KEY_SESSION_GRAYSCALE = "session_grayscale"
        private const val KEY_SESSION_UNLOCK = "session_unlock"
        private const val KEY_SESSION_PAUSED = "session_paused"

        private const val KEY_HISTORY = "history"

        @Volatile
        private var instance: MinimalRepository? = null

        fun get(context: Context): MinimalRepository = instance ?: synchronized(this) {
            instance ?: MinimalRepository(context).also { instance = it }
        }

        @androidx.annotation.VisibleForTesting
        internal fun resetForTests() {
            synchronized(this) {
                instance = null
                MicroSessionManager.clearAll()
            }
        }
    }
}
