package dev.minimalphone.ui

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.minimalphone.apps.AppCatalog
import dev.minimalphone.apps.DefaultCommunicationApps
import dev.minimalphone.apps.LaunchableApp
import dev.minimalphone.blocker.MinimalAccessibilityService
import dev.minimalphone.session.ActiveMinimalSession
import dev.minimalphone.session.Interruptions
import dev.minimalphone.session.MinimalConstants
import dev.minimalphone.session.MinimalRecord
import dev.minimalphone.session.MinimalRepository
import dev.minimalphone.session.MinimalSettings
import dev.minimalphone.session.Wallpaper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MinimalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MinimalRepository.get(application)

    val settings: StateFlow<MinimalSettings> = repository.settings
    val active: StateFlow<ActiveMinimalSession?> = repository.active
    val history: StateFlow<List<MinimalRecord>> = repository.history

    private val _apps = MutableStateFlow<List<LaunchableApp>>(emptyList())
    val apps: StateFlow<List<LaunchableApp>> = _apps.asStateFlow()

    private val _appsLoaded = MutableStateFlow(false)
    val appsLoaded: StateFlow<Boolean> = _appsLoaded.asStateFlow()

    private val _isDefaultHome = MutableStateFlow(false)
    val isDefaultHome: StateFlow<Boolean> = _isDefaultHome.asStateFlow()

    private val _accessibilityAccess = MutableStateFlow(false)
    val accessibilityAccess: StateFlow<Boolean> = _accessibilityAccess.asStateFlow()

    private val _dndAccess = MutableStateFlow(false)
    val dndAccess: StateFlow<Boolean> = _dndAccess.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _justFinishedMinutes = MutableStateFlow<Int?>(null)
    val justFinishedMinutes: StateFlow<Int?> = _justFinishedMinutes.asStateFlow()

    init {
        refreshAccess()
        viewModelScope.launch {
            _apps.value = AppCatalog.load(getApplication())
            _appsLoaded.value = true
        }
    }

    fun sync() {
        repository.syncWithClock()
        refreshAccess()
    }

    fun refreshAccess() {
        val context = getApplication<Application>()
        _accessibilityAccess.value = MinimalAccessibilityService.isServiceEnabled(context)
        _dndAccess.value = Interruptions.hasAccess(context)
        _notificationsEnabled.value = Interruptions.notificationsAllowed(context)
        _isDefaultHome.value = checkDefaultHome(context)
    }

    private fun checkDefaultHome(context: Application): Boolean {
        return runCatching {
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolved = context.packageManager.resolveActivity(
                homeIntent,
                PackageManager.MATCH_DEFAULT_ONLY,
            )?.activityInfo?.packageName
            resolved == context.packageName
        }.getOrDefault(false)
    }

    fun startMinimal() {
        _justFinishedMinutes.value = null
        repository.start()
    }

    fun endMinimal(completed: Boolean) {
        repository.endSession(completed = completed)
        _justFinishedMinutes.value = if (completed) {
            repository.history.value.firstOrNull()?.minutes
        } else {
            null
        }
    }

    fun extendHours(hours: Int) = repository.extendHours(hours)

    fun dismissFinished() {
        _justFinishedMinutes.value = null
    }

    fun setCommitmentHours(hours: Int) {
        val coerced = hours.coerceIn(MinimalConstants.INDEFINITE_HOURS, MinimalConstants.MAX_TIMED_HOURS)
        repository.updateSettings { it.copy(commitmentHours = coerced) }
    }

    fun toggleAllowed(packageName: String) = repository.toggleAllowed(packageName)

    fun toggleFavorite(packageName: String) = repository.toggleFavorite(packageName)

    fun allowEssentialsPreset() {
        val essentials = DefaultCommunicationApps.resolveDefaultMinimalPackages(getApplication())
        repository.updateSettings { current ->
            current.copy(allowedPackages = current.allowedPackages + essentials)
        }
    }

    fun allowAllApps() {
        val all = _apps.value.map { it.packageName }.toSet()
        repository.setAllowedPackages(all)
    }

    fun clearAllowed() = repository.clearAllowed()

    fun setShowAppIcons(enabled: Boolean) =
        repository.updateSettings { it.copy(showAppIcons = enabled) }

    fun setPauseInterruptions(enabled: Boolean) =
        repository.updateSettings { it.copy(pauseInterruptions = enabled) }

    fun setRequireUnlock(enabled: Boolean) =
        repository.updateSettings { it.copy(requireUnlockToEnd = enabled) }

    fun setWallpaper(wallpaper: Wallpaper) =
        repository.updateSettings { it.copy(wallpaper = wallpaper) }

    fun clearHistory() = repository.clearHistory()
}
