package dev.minimalphone.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/** A launchable app on the device that can be allowed or pinned on MinimalBro's Home screen. */
data class LaunchableApp(
    val packageName: String,
    val label: String,
)

/**
 * Reads apps with a launcher entry using the narrow `<queries>` intent in AndroidManifest.xml.
 */
object AppCatalog {

    suspend fun load(context: Context): List<LaunchableApp> = withContext(Dispatchers.IO) {
        val manager = context.packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        runCatching {
            manager.queryIntentActivities(query, 0)
                .asSequence()
                .mapNotNull { it.activityInfo?.packageName }
                .filter { it != context.packageName }
                .distinct()
                .mapNotNull { packageName -> readApp(manager, packageName) }
                .sortedBy { it.label.lowercase(Locale.getDefault()) }
                .toList()
        }.getOrDefault(emptyList())
    }

    private fun readApp(manager: PackageManager, packageName: String): LaunchableApp? =
        runCatching {
            val info = manager.getApplicationInfo(packageName, 0)
            LaunchableApp(
                packageName = packageName,
                label = manager.getApplicationLabel(info).toString(),
            )
        }.getOrNull()
}
