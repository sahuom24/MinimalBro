package dev.minimalphone.ui

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.minimalphone.blocker.MinimalAccessibilityService
import dev.minimalphone.session.Interruptions
import dev.minimalphone.session.MinimalNotifier
import dev.minimalphone.ui.theme.MinimalBroTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MinimalViewModel by viewModels()
    private var sessionExpiredPackage by mutableStateOf<String?>(null)
    private var cooldownPackageInfo by mutableStateOf<Pair<String, Int>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MinimalNotifier.ensureChannels(this)
        handleIncomingIntent(intent)
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            MinimalBroTheme(wallpaper = settings.wallpaper) {
                MinimalBroApp(
                    viewModel = viewModel,
                    sessionExpiredPackage = sessionExpiredPackage,
                    cooldownPackageInfo = cooldownPackageInfo,
                    onDismissSessionExpired = { sessionExpiredPackage = null },
                    onDismissCooldown = { cooldownPackageInfo = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val expired = intent?.getStringExtra(MinimalAccessibilityService.EXTRA_SESSION_EXPIRED_PACKAGE)
        if (!expired.isNullOrBlank()) {
            sessionExpiredPackage = expired
        }
        val cooldown = intent?.getStringExtra(MinimalAccessibilityService.EXTRA_COOLDOWN_PACKAGE)
        val remaining = intent?.getIntExtra(MinimalAccessibilityService.EXTRA_COOLDOWN_REMAINING_SECS, 60) ?: 60
        if (!cooldown.isNullOrBlank()) {
            cooldownPackageInfo = cooldown to remaining
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.sync()
    }

    override fun onStart() {
        super.onStart()
        dev.minimalphone.ui.widgets.AppWidgetHostHelper.get(this).startListening()
    }

    override fun onStop() {
        super.onStop()
        dev.minimalphone.ui.widgets.AppWidgetHostHelper.get(this).stopListening()
    }
}

@Composable
fun MinimalBroApp(
    viewModel: MinimalViewModel,
    sessionExpiredPackage: String? = null,
    cooldownPackageInfo: Pair<String, Int>? = null,
    onDismissSessionExpired: () -> Unit = {},
    onDismissCooldown: () -> Unit = {},
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val active by viewModel.active.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val appsLoaded by viewModel.appsLoaded.collectAsStateWithLifecycle()
    val isDefaultHome by viewModel.isDefaultHome.collectAsStateWithLifecycle()
    val accessibilityAccess by viewModel.accessibilityAccess.collectAsStateWithLifecycle()
    val dndAccess by viewModel.dndAccess.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val writeSecureSettingsGranted by viewModel.writeSecureSettingsGranted.collectAsStateWithLifecycle()
    val justFinished by viewModel.justFinishedMinutes.collectAsStateWithLifecycle()

    var showDndExplain by remember { mutableStateOf(false) }
    var showAccessibilityExplain by remember { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshAccess() }

    val dndLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.refreshAccess() }

    val accessibilityLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.refreshAccess() }

    val homeSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.refreshAccess() }

    val credentialLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.endMinimal(completed = false)
        }
    }

    val openHomeSettings: () -> Unit = {
        val intent = Intent(Settings.ACTION_HOME_SETTINGS)
        if (intent.resolveActivity(context.packageManager) != null) {
            runCatching { homeSettingsLauncher.launch(intent) }
        }
    }

    val requestExit: () -> Unit = {
        val current = active
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        @Suppress("DEPRECATION")
        val intent = if (current != null && current.requireUnlockToEnd && keyguard != null) {
            runCatching {
                keyguard.createConfirmDeviceCredentialIntent(
                    "Exit Minimal Mode?",
                    "Unlock your phone to leave your minimal home screen.",
                )
            }.getOrNull()
        } else {
            null
        }
        if (intent != null) {
            credentialLauncher.launch(intent)
        } else {
            viewModel.endMinimal(completed = false)
        }
    }

    val current = active
    if (current != null) {
        BackHandler {
            // Home/Minimal launcher stays calm on Back press.
        }
        val allowedApps = remember(apps, current.allowedPackages) {
            apps.filter { it.packageName in current.allowedPackages }
        }
        val favoriteApps = remember(allowedApps, settings.favoritePackages) {
            allowedApps.filter { it.packageName in settings.favoritePackages }
        }
        MinimalHomeScreen(
            active = current,
            wallpaper = settings.wallpaper,
            allowedApps = allowedApps,
            favoriteApps = favoriteApps,
            showAppIcons = settings.showAppIcons,
            onExpired = { viewModel.endMinimal(completed = true) },
            onRequestExit = requestExit,
            onExtendHours = viewModel::extendHours,
        )
    } else {
        MinimalSetupScreen(
            settings = settings,
            history = history,
            apps = apps,
            appsLoaded = appsLoaded,
            isDefaultHome = isDefaultHome,
            accessibilityAccess = accessibilityAccess,
            dndAccess = dndAccess,
            notificationsEnabled = notificationsEnabled,
            justFinishedMinutes = justFinished,
            onDismissFinished = viewModel::dismissFinished,
            onStartMinimal = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                viewModel.startMinimal()
            },
            onCommitmentHours = viewModel::setCommitmentHours,
            onSetAppTier = viewModel::setAppTier,
            onAutoClassifyAll = viewModel::autoClassifyAll,
            onEnableGrayscale = viewModel::setEnableGrayscale,
            writeSecureSettingsGranted = writeSecureSettingsGranted,
            onToggleAllowed = viewModel::toggleAllowed,
            onToggleFavorite = viewModel::toggleFavorite,
            onAllowEssentials = viewModel::allowEssentialsPreset,
            onAllowAll = viewModel::allowAllApps,
            onClearAllowed = viewModel::clearAllowed,
            onShowAppIcons = viewModel::setShowAppIcons,
            onPauseInterruptions = { enabled ->
                viewModel.setPauseInterruptions(enabled)
                if (enabled && !dndAccess) showDndExplain = true
            },
            onRequireUnlock = viewModel::setRequireUnlock,
            onWallpaper = viewModel::setWallpaper,
            onOpenHomeSettings = openHomeSettings,
            onRequestAccessibilityAccess = { showAccessibilityExplain = true },
            onRequestDndAccess = { showDndExplain = true },
            onOpenNotificationSettings = {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                runCatching { context.startActivity(intent) }
            },
        )
    }

    if (showDndExplain) {
        AlertDialog(
            onDismissRequest = { showDndExplain = false },
            title = { Text("Allow Do Not Disturb access?") },
            text = {
                Text(
                    "MinimalBro uses Do Not Disturb only when silencing notifications is turned on " +
                        "and restores your previous state when Minimal mode ends.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDndExplain = false
                        Interruptions.accessSettingsIntent(context)?.let(dndLauncher::launch)
                    },
                ) { Text("Open settings") }
            },
            dismissButton = {
                TextButton(onClick = { showDndExplain = false }) { Text("Not now") }
            },
        )
    }

    if (showAccessibilityExplain) {
        AlertDialog(
            onDismissRequest = { showAccessibilityExplain = false },
            title = { Text("Accessibility Guard Disclosure") },
            text = {
                Text(
                    "MinimalBro optionally uses Android's Accessibility Service strictly during an active " +
                        "Minimal session to detect window state changes and redirect hidden apps back to " +
                        "your calm Minimal Home screen.\n\n" +
                        "• Window content reading is OFF (MinimalBro never reads screen text or passwords).\n" +
                        "• 100% offline: no data ever leaves your device.\n" +
                        "• Revoke anytime in Android Accessibility Settings.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAccessibilityExplain = false
                        accessibilityLauncher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                ) { Text("Open settings") }
            },
            dismissButton = {
                TextButton(onClick = { showAccessibilityExplain = false }) { Text("Not now") }
            },
        )
    }

    sessionExpiredPackage?.let { pkg ->
        val appLabel = apps.firstOrNull { it.packageName == pkg }?.label ?: "This app"
        SessionEndedOverlay(
            packageName = pkg,
            appLabel = appLabel,
            initialCooldownSecs = 60,
            onDismiss = onDismissSessionExpired,
        )
    }

    cooldownPackageInfo?.let { (pkg, remaining) ->
        val appLabel = apps.firstOrNull { it.packageName == pkg }?.label ?: "This app"
        SessionEndedOverlay(
            packageName = pkg,
            appLabel = appLabel,
            initialCooldownSecs = remaining,
            onDismiss = onDismissCooldown,
        )
    }
}
