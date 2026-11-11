package dev.minimalphone.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import android.provider.Telephony
import android.telecom.TelecomManager

/**
 * Resolves essential phone, messaging, calendar, UPI, and quick-search apps on the device.
 */
object DefaultCommunicationApps {

    fun getPhonePackage(context: Context): String? {
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val defaultDialer = telecom?.defaultDialerPackage
        if (!defaultDialer.isNullOrBlank()) return defaultDialer

        val dialIntent = Intent(Intent.ACTION_DIAL)
        val resolve = context.packageManager.resolveActivity(dialIntent, PackageManager.MATCH_DEFAULT_ONLY)
        val resolved = resolve?.activityInfo?.packageName
        if (!resolved.isNullOrBlank()) return resolved

        val fallbacks = listOf("com.google.android.dialer", "com.android.phone", "com.samsung.android.dialer")
        return fallbacks.firstOrNull { isPackageInstalled(context, it) }
    }

    fun getMessagesPackage(context: Context): String? {
        val defaultSms = Telephony.Sms.getDefaultSmsPackage(context)
        if (!defaultSms.isNullOrBlank()) return defaultSms

        val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))
        val resolve = context.packageManager.resolveActivity(smsIntent, PackageManager.MATCH_DEFAULT_ONLY)
        val resolved = resolve?.activityInfo?.packageName
        if (!resolved.isNullOrBlank()) return resolved

        val fallbacks = listOf(
            "com.google.android.apps.messaging",
            "com.android.mms",
            "com.samsung.android.messaging",
        )
        return fallbacks.firstOrNull { isPackageInstalled(context, it) }
    }

    fun getCalendarPackage(context: Context): String? {
        val intent = Intent(Intent.ACTION_VIEW).setData(CalendarContract.CONTENT_URI)
        val resolve = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val resolved = resolve?.activityInfo?.packageName
        if (!resolved.isNullOrBlank() && isPackageInstalled(context, resolved)) return resolved

        val fallbacks = listOf(
            "com.google.android.calendar",
            "com.android.calendar",
            "com.samsung.android.calendar",
            "com.miui.calendar",
        )
        return fallbacks.firstOrNull { isPackageInstalled(context, it) }
    }

    fun getUpiPackage(context: Context): String? {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("upi://pay"))
        val resolve = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val resolved = resolve?.activityInfo?.packageName
        if (!resolved.isNullOrBlank() && isPackageInstalled(context, resolved)) return resolved

        val fallbacks = listOf(
            "com.google.android.apps.nbu.paisa.user",
            "com.phonepe.app",
            "net.one97.paytm",
            "in.org.npci.upiapp",
            "com.dreamplug.androidapp",
            "in.amazon.mShop.android.shopping",
            "com.mobikwik_new",
        )
        return fallbacks.firstOrNull { isPackageInstalled(context, it) }
    }

    fun getGooglePackage(context: Context): String? {
        val fallbacks = listOf(
            "com.google.android.googlequicksearchbox",
            "com.android.chrome",
            "com.google.android.apps.searchlite",
        )
        return fallbacks.firstOrNull { isPackageInstalled(context, it) }
    }

    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return runCatching {
            context.packageManager.getApplicationInfo(packageName, 0).enabled
        }.getOrDefault(false)
    }

    fun resolveDefaultMinimalPackages(context: Context): Set<String> = buildSet {
        getPhonePackage(context)?.let { add(it) }
        getMessagesPackage(context)?.let { add(it) }
        getCalendarPackage(context)?.let { add(it) }
        getUpiPackage(context)?.let { add(it) }
        getGooglePackage(context)?.let { add(it) }
    }

    fun openPhone(context: Context) {
        val pkg = getPhonePackage(context)
        val intent = (if (pkg != null) context.packageManager.getLaunchIntentForPackage(pkg) else null)
            ?: Intent(Intent.ACTION_DIAL)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        runCatching { context.startActivity(intent) }
    }

    fun openMessages(context: Context) {
        val pkg = getMessagesPackage(context)
        val intent = (if (pkg != null) context.packageManager.getLaunchIntentForPackage(pkg) else null)
            ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        runCatching { context.startActivity(intent) }
    }
}
