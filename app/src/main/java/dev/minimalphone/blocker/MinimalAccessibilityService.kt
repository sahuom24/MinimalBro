package dev.minimalphone.blocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telecom.TelecomManager
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import dev.minimalphone.apps.DefaultCommunicationApps
import dev.minimalphone.session.AppTier
import dev.minimalphone.session.AppTierClassifier
import dev.minimalphone.session.MicroSessionManager
import dev.minimalphone.session.MinimalRepository
import dev.minimalphone.ui.MainActivity

/**
 * Optional distraction guard for MinimalBro: during an active Minimal session,
 * keeps hidden/unselected apps restricted while allowing Phone, SMS, keyboards,
 * biometrics, notifications, and essential Android settings. Window content reading is OFF.
 */
class MinimalAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastToastTime = 0L
    private var currentForegroundApp: String? = null
    private var sessionCountdownRunnable: Runnable? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeInstance = this
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                AccessibilityServiceInfo.DEFAULT
            notificationTimeout = 50
        }
    }

    override fun onDestroy() {
        if (activeInstance == this) {
            activeInstance = null
        }
        sessionCountdownRunnable?.let { mainHandler.removeCallbacks(it) }
        sessionCountdownRunnable = null
        GrayscaleController.setGrayscale(this, false)
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        val className = event.className?.toString() ?: ""

        val repository = MinimalRepository.get(applicationContext)
        repository.syncWithClock()
        val active = repository.active.value ?: return

        if (packageName == applicationContext.packageName) {
            // User returned to MinimalBro Home screen
            currentForegroundApp = null
            sessionCountdownRunnable?.let { mainHandler.removeCallbacks(it) }
            sessionCountdownRunnable = null
            GrayscaleController.setGrayscale(this, false)
            return
        }
        if (packageName in SYSTEM_PACKAGES) return
        if (isBiometricOrSecurity(packageName, className)) return
        if (isInputMethod(packageName)) return
        if (isDialerOrInCall(packageName)) return
        if (isDefaultMessagesApp(packageName)) return

        if (isAllowedGameLauncherBridge(packageName, className, active.allowedPackages)) return

        if (packageName in active.allowedPackages) {
            handleAllowedPackageInSession(packageName, active)
            return
        }

        // Redirect non-minimal default launchers back to MinimalBro during active sessions
        if (isOtherLauncherPackage(packageName)) {
            bringMinimalHomeToFront()
            return
        }

        if (packageName == "com.android.settings" || packageName.endsWith(".settings")) {
            if (isAllowedSettingsScreen(className)) return
        }

        blockApp(packageName)
    }

    private fun handleAllowedPackageInSession(
        packageName: String,
        active: dev.minimalphone.session.ActiveMinimalSession,
    ) {
        // 1. Check if the app is currently in a 60-second cooldown
        if (MicroSessionManager.isInCooldown(packageName)) {
            val remainingSecs = MicroSessionManager.getRemainingCooldownSeconds(packageName)
            blockCooldownApp(packageName, remainingSecs)
            return
        }

        val tier = active.appTiers[packageName] ?: AppTierClassifier.suggestTier(packageName)

        // Tier 0: Essentials - unlimited access, zero friction, normal display
        if (tier == AppTier.TIER_0) {
            currentForegroundApp = packageName
            sessionCountdownRunnable?.let { mainHandler.removeCallbacks(it) }
            sessionCountdownRunnable = null
            GrayscaleController.setGrayscale(this, false)
            return
        }

        // Tier 1 & Tier 2:
        // Native Grayscale Toggle: Force monochrome for Tier 2 dopamine traps (or Chrome)
        if (active.enableGrayscale && (tier == AppTier.TIER_2 || packageName == "com.android.chrome")) {
            GrayscaleController.setGrayscale(this, true)
        } else {
            GrayscaleController.setGrayscale(this, false)
        }

        // Micro-Session Monitoring: 5-minute session countdown
        val session = MicroSessionManager.startOrGetSession(packageName)
        if (session == null) {
            blockCooldownApp(packageName, MicroSessionManager.getRemainingCooldownSeconds(packageName))
            return
        }

        currentForegroundApp = packageName
        sessionCountdownRunnable?.let { mainHandler.removeCallbacks(it) }

        val remainingMs = MicroSessionManager.getRemainingSessionMs(packageName)
        if (remainingMs <= 0) {
            triggerSessionExpired(packageName)
            return
        }

        val runnable = Runnable {
            if (currentForegroundApp == packageName) {
                triggerSessionExpired(packageName)
            }
        }
        sessionCountdownRunnable = runnable
        mainHandler.postDelayed(runnable, remainingMs)
    }

    private fun triggerSessionExpired(packageName: String) {
        MicroSessionManager.expireSession(packageName)
        GrayscaleController.setGrayscale(this, false)
        sessionCountdownRunnable?.let { mainHandler.removeCallbacks(it) }
        sessionCountdownRunnable = null
        currentForegroundApp = null

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra(EXTRA_SESSION_EXPIRED_PACKAGE, packageName)
        }
        runCatching { startActivity(intent) }
    }

    private fun blockCooldownApp(packageName: String, remainingSecs: Int) {
        GrayscaleController.setGrayscale(this, false)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra(EXTRA_COOLDOWN_PACKAGE, packageName)
            putExtra(EXTRA_COOLDOWN_REMAINING_SECS, remainingSecs)
        }
        runCatching { startActivity(intent) }

        val now = System.currentTimeMillis()
        if (now - lastToastTime > TOAST_INTERVAL_MS) {
            lastToastTime = now
            val label = getAppLabel(packageName)
            mainHandler.post {
                Toast.makeText(
                    applicationContext,
                    "$label is in a 60-second cooldown (${remainingSecs}s left). Take a breath.",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun bringMinimalHomeToFront() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                Intent.FLAG_ACTIVITY_NO_ANIMATION
        }
        runCatching { startActivity(intent) }
    }

    private fun blockApp(packageName: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra(EXTRA_BLOCKED_PACKAGE, packageName)
        }
        runCatching { startActivity(intent) }

        val now = System.currentTimeMillis()
        if (now - lastToastTime > TOAST_INTERVAL_MS) {
            lastToastTime = now
            val label = getAppLabel(packageName)
            mainHandler.post {
                Toast.makeText(
                    applicationContext,
                    "$label is hidden during Minimal mode",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun isAllowedGameLauncherBridge(
        packageName: String,
        className: String,
        allowedPackages: Set<String>,
    ): Boolean {
        if (packageName !in GAME_LAUNCHER_PACKAGES) return false
        val hasAllowedGame = allowedPackages.any { isGamePackage(it) }
        if (!hasAllowedGame) return false

        val recentAllowedLaunch =
            (System.currentTimeMillis() - lastAllowedLaunchAtMillis) <= GAME_BRIDGE_GRACE_MS &&
                isGamePackage(lastAllowedLaunchedPackage)
        if (recentAllowedLaunch) return true

        val lowerClass = className.lowercase()
        return lowerClass.contains("gametools") ||
            lowerClass.contains("booster") ||
            lowerClass.contains("popup") ||
            lowerClass.contains("overlay") ||
            lowerClass.contains("bridge")
    }

    private fun isGamePackage(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        return runCatching {
            val info = packageManager.getApplicationInfo(packageName, 0)
            info.category == ApplicationInfo.CATEGORY_GAME
        }.getOrDefault(false)
    }

    private fun isOtherLauncherPackage(packageName: String): Boolean {
        if (packageName == applicationContext.packageName) return false
        return runCatching {
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            packageManager.queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
                .any { it.activityInfo?.packageName == packageName }
        }.getOrDefault(false)
    }

    private fun isInputMethod(packageName: String): Boolean {
        return runCatching {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return false
            imm.inputMethodList.any { it.packageName == packageName }
        }.getOrDefault(false)
    }

    private fun isDialerOrInCall(packageName: String): Boolean {
        if (packageName == DefaultCommunicationApps.getPhonePackage(applicationContext)) return true
        return runCatching {
            val telecom = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            telecom?.defaultDialerPackage == packageName
        }.getOrDefault(false)
    }

    private fun isDefaultMessagesApp(packageName: String): Boolean {
        val defaultSms = DefaultCommunicationApps.getMessagesPackage(applicationContext)
        if (!defaultSms.isNullOrBlank() && packageName == defaultSms) return true
        return packageName == "com.google.android.apps.messaging" ||
            packageName == "com.android.mms" ||
            packageName == "com.samsung.android.messaging"
    }

    private fun isBiometricOrSecurity(packageName: String, className: String): Boolean {
        val lowerPkg = packageName.lowercase()
        val lowerClass = className.lowercase()
        if (lowerPkg.contains("fingerprint") ||
            lowerPkg.contains("biometric") ||
            lowerPkg.contains("authframework") ||
            lowerPkg.contains("authservice") ||
            lowerPkg.contains("securityadd") ||
            lowerPkg.contains("guardprovider") ||
            lowerPkg.contains("keyguard")
        ) {
            return true
        }
        return lowerClass.contains("fingerprint") ||
            lowerClass.contains("biometric") ||
            lowerClass.contains("faceunlock") ||
            lowerClass.contains("credential") ||
            lowerClass.contains("confirmlock") ||
            lowerClass.contains("keyguard") ||
            lowerClass.contains("devicecredential")
    }

    private fun isAllowedSettingsScreen(className: String): Boolean {
        val lower = className.lowercase()
        if (lower.contains("fingerprint") ||
            lower.contains("biometric") ||
            lower.contains("face") ||
            lower.contains("credential") ||
            lower.contains("confirmlock") ||
            lower.contains("password") ||
            lower.contains("pattern") ||
            lower.contains("pin") ||
            lower.contains("auth") ||
            lower.contains("security")
        ) {
            return true
        }
        if (lower.contains("installedappdetails") ||
            lower.contains("manageapplications") ||
            lower.contains("appinfo") ||
            lower.contains("specialaccess")
        ) {
            return false
        }
        return lower.contains("zenmode") ||
            lower.contains("sound") ||
            lower.contains("notification") ||
            lower.contains("volume") ||
            lower.contains("panel") ||
            lower.contains("display") ||
            lower.contains("bluetooth") ||
            lower.contains("wifi") ||
            lower.contains("network")
    }

    private fun getAppLabel(packageName: String): String {
        return runCatching {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault("This app")
    }

    override fun onInterrupt() {}

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "dev.minimalphone.extra.BLOCKED_PACKAGE"
        const val EXTRA_SESSION_EXPIRED_PACKAGE = "dev.minimalphone.extra.SESSION_EXPIRED_PACKAGE"
        const val EXTRA_COOLDOWN_PACKAGE = "dev.minimalphone.extra.COOLDOWN_PACKAGE"
        const val EXTRA_COOLDOWN_REMAINING_SECS = "dev.minimalphone.extra.COOLDOWN_REMAINING_SECS"
        private const val TOAST_INTERVAL_MS = 2000L
        private const val GAME_BRIDGE_GRACE_MS = 3500L

        @Volatile
        var activeInstance: MinimalAccessibilityService? = null
            private set

        @Volatile
        private var lastAllowedLaunchedPackage: String? = null

        @Volatile
        private var lastAllowedLaunchAtMillis: Long = 0L

        fun noteAllowedAppLaunch(packageName: String) {
            lastAllowedLaunchedPackage = packageName
            lastAllowedLaunchAtMillis = System.currentTimeMillis()
        }

        private val GAME_LAUNCHER_PACKAGES = setOf(
            "com.samsung.android.game.gamehome",
            "com.oplus.games",
            "com.coloros.gamespace",
        )

        val SYSTEM_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.google.android.systemui",
            "com.android.intentresolver",
            "com.samsung.android.app.cocktailbarservice",
            "com.samsung.android.app.smartcapture",
            "com.samsung.android.app.clipboardedge",
            "com.samsung.android.app.taskbar",
            "com.samsung.android.app.routines",
            "com.samsung.android.smartsuggestions",
            "com.samsung.android.scs",
            "com.samsung.android.rubin.app",
            "com.samsung.android.intellivoiceservice",
            "com.samsung.android.forest",
            "com.samsung.android.honeyboard",
            "com.samsung.android.game.gametools",
            "com.samsung.android.game.gos",
            "com.google.android.as",
            "com.google.android.as.oss",
            "com.google.android.ext.services",
            "com.xiaomi.joyose",
            "com.miui.systemui",
            "com.huawei.systemui",
            "com.oppo.systemui",
            "com.oplus.systemui",
            "com.vivo.systemui",
            "com.oneplus.systemui",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.google.android.gms",
            "com.android.keyguard",
            "com.samsung.android.biometrics.app.setting",
            "com.samsung.android.authframework",
            "com.samsung.android.authservice",
            "com.sec.android.app.syscal",
            "com.coloros.fingerprint",
            "com.oplus.fingerprint",
            "com.coloros.safecenter",
            "com.oplus.safecenter",
            "com.miui.securityadd",
            "com.miui.guardprovider",
            "com.miui.securitycenter",
            "com.vivo.fingerprint",
            "com.vivo.safecenter",
            "com.android.documentsui",
            "com.google.android.documentsui",
            "com.android.cellbroadcastreceiver",
            "com.google.android.cellbroadcastreceiver",
            "com.android.emergency",
            "com.google.android.apps.safetyhub",
            "com.google.android.marvin.talkback",
            "com.samsung.android.accessibility.talkback",
            "com.android.phone",
            "com.android.server.telecom",
            "com.google.android.dialer",
            "com.samsung.android.incallui",
            "com.samsung.android.dialer",
        )

        fun isServiceEnabled(context: Context): Boolean {
            return runCatching {
                val expectedComponentName =
                    ComponentName(context, MinimalAccessibilityService::class.java).flattenToString()
                val enabledServices = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                ) ?: return false
                val splitter = TextUtils.SimpleStringSplitter(':')
                splitter.setString(enabledServices)
                while (splitter.hasNext()) {
                    val service = splitter.next()
                    if (service.equals(expectedComponentName, ignoreCase = true) ||
                        service.contains(context.packageName, ignoreCase = true)
                    ) {
                        return true
                    }
                }
                false
            }.getOrDefault(false)
        }
    }
}
