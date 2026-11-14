package dev.minimalphone.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.minimalphone.apps.LaunchableApp
import dev.minimalphone.session.AppTier
import dev.minimalphone.session.AppTierClassifier
import dev.minimalphone.ui.theme.LocalMinimalPalette

private enum class AppFilter(val label: String) {
    ALL("All"),
    TIER_0("T0: Essentials"),
    TIER_1("T1: Utility"),
    TIER_2("T2: Dopamine"),
    ALLOWED("Allowed"),
    FAVORITES("Pinned"),
}

@Composable
fun AppsCurateTab(
    apps: List<LaunchableApp>,
    appsLoaded: Boolean,
    allowedPackages: Set<String>,
    favoritePackages: Set<String>,
    appTiers: Map<String, AppTier> = emptyMap(),
    onSetAppTier: (String, AppTier?) -> Unit = { _, _ -> },
    onAutoClassifyAll: () -> Unit = {},
    onToggleAllowed: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onAllowEssentials: () -> Unit,
    onAllowAll: () -> Unit,
    onClearAllowed: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val palette = LocalMinimalPalette.current
    var searchQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(AppFilter.ALL) }

    val filtered = remember(apps, searchQuery, filter, allowedPackages, favoritePackages, appTiers) {
        apps.filter { app ->
            val tier = appTiers[app.packageName] ?: AppTierClassifier.suggestTier(app.packageName, app.label)
            val matchesFilter = when (filter) {
                AppFilter.ALL -> true
                AppFilter.TIER_0 -> app.packageName in allowedPackages && tier == AppTier.TIER_0
                AppFilter.TIER_1 -> app.packageName in allowedPackages && tier == AppTier.TIER_1
                AppFilter.TIER_2 -> app.packageName in allowedPackages && tier == AppTier.TIER_2
                AppFilter.ALLOWED -> app.packageName in allowedPackages
                AppFilter.FAVORITES -> app.packageName in favoritePackages
            }
            val matchesQuery = searchQuery.isBlank() ||
                app.label.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(
            text = "Minimal Apps",
            style = MaterialTheme.typography.headlineSmall,
            color = colors.onBackground,
        )
        Text(
            text = "${allowedPackages.size} allowed · No limit · ${favoritePackages.size} pinned on Home",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search installed apps...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface.copy(alpha = 0.72f),
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppFilter.entries.forEach { entry ->
                val countSuffix = when (entry) {
                    AppFilter.ALL -> ""
                    AppFilter.TIER_0 -> {
                        val count = allowedPackages.count { (appTiers[it] ?: AppTierClassifier.suggestTier(it)) == AppTier.TIER_0 }
                        " ($count)"
                    }
                    AppFilter.TIER_1 -> {
                        val count = allowedPackages.count { (appTiers[it] ?: AppTierClassifier.suggestTier(it)) == AppTier.TIER_1 }
                        " ($count)"
                    }
                    AppFilter.TIER_2 -> {
                        val count = allowedPackages.count { (appTiers[it] ?: AppTierClassifier.suggestTier(it)) == AppTier.TIER_2 }
                        " ($count)"
                    }
                    AppFilter.ALLOWED -> " (${allowedPackages.size})"
                    AppFilter.FAVORITES -> " (${favoritePackages.size})"
                }
                FilterChip(
                    selected = filter == entry,
                    onClick = { filter = entry },
                    label = { Text("${entry.label}$countSuffix") },
                    shape = RoundedCornerShape(999.dp),
                )
            }
            TextButton(onClick = onAutoClassifyAll) { Text("Auto-tier") }
            TextButton(onClick = onAllowEssentials) { Text("Essentials") }
            TextButton(onClick = onAllowAll) { Text("Allow all") }
            if (allowedPackages.isNotEmpty()) {
                TextButton(onClick = onClearAllowed) { Text("Clear") }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (!appsLoaded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(10.dp))
                Text("Reading apps...", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            text = "No matching apps in this view.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    }
                } else {
                    items(filtered, key = { it.packageName }) { app ->
                        val isAllowed = app.packageName in allowedPackages
                        val isFavorite = app.packageName in favoritePackages
                        val currentTier = appTiers[app.packageName] ?: AppTierClassifier.suggestTier(app.packageName, app.label)
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isAllowed) {
                                colors.primaryContainer.copy(alpha = 0.35f)
                            } else {
                                colors.surface
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isAllowed) colors.primary.copy(alpha = 0.35f) else palette.subtleBorder,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    if (isAllowed) {
                                        onSetAppTier(app.packageName, null)
                                    } else {
                                        onSetAppTier(app.packageName, currentTier)
                                    }
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AppIcon(packageName = app.packageName, size = 36.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (isAllowed) FontWeight.SemiBold else FontWeight.Normal,
                                        color = colors.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = if (!isAllowed) {
                                            "Hidden · Default: ${currentTier.name}"
                                        } else {
                                            "${currentTier.title.takeWhile { it != ':' }} ${if (isFavorite) "· Pinned" else ""}"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isAllowed) {
                                            when (currentTier) {
                                                AppTier.TIER_0 -> colors.primary
                                                AppTier.TIER_1 -> colors.secondary
                                                AppTier.TIER_2 -> colors.error
                                            }
                                        } else colors.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { onToggleFavorite(app.packageName) }) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = if (isFavorite) "Unpin favorite" else "Pin favorite",
                                        tint = if (isFavorite) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.35f),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Surface(
                                        onClick = { onSetAppTier(app.packageName, AppTier.TIER_0) },
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (isAllowed && currentTier == AppTier.TIER_0) colors.primary else colors.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, if (isAllowed && currentTier == AppTier.TIER_0) colors.primary else palette.subtleBorder),
                                    ) {
                                        Text(
                                            text = "T0",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAllowed && currentTier == AppTier.TIER_0) colors.onPrimary else colors.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }

                                    Surface(
                                        onClick = { onSetAppTier(app.packageName, AppTier.TIER_1) },
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (isAllowed && currentTier == AppTier.TIER_1) colors.secondary else colors.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, if (isAllowed && currentTier == AppTier.TIER_1) colors.secondary else palette.subtleBorder),
                                    ) {
                                        Text(
                                            text = "T1",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAllowed && currentTier == AppTier.TIER_1) colors.onSecondary else colors.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }

                                    Surface(
                                        onClick = { onSetAppTier(app.packageName, AppTier.TIER_2) },
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (isAllowed && currentTier == AppTier.TIER_2) colors.error else colors.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, if (isAllowed && currentTier == AppTier.TIER_2) colors.error else palette.subtleBorder),
                                    ) {
                                        Text(
                                            text = "T2",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAllowed && currentTier == AppTier.TIER_2) colors.onError else colors.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }

                                    if (isAllowed) {
                                        IconButton(
                                            onClick = { onSetAppTier(app.packageName, null) },
                                            modifier = Modifier.size(28.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Hide",
                                                tint = colors.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
