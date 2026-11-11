package dev.minimalphone.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import dev.minimalphone.session.MinimalConstants
import dev.minimalphone.session.MinimalRecord
import dev.minimalphone.session.MinimalSettings
import dev.minimalphone.session.Wallpaper
import dev.minimalphone.ui.theme.LocalMinimalPalette
import dev.minimalphone.ui.theme.wallpaperSwatch

enum class MinimalTab(val title: String) {
    HOME("Minimal"),
    APPS("Apps"),
    TUNE("Style & Guard"),
}

@Composable
fun MinimalSetupScreen(
    settings: MinimalSettings,
    history: List<MinimalRecord>,
    apps: List<LaunchableApp>,
    appsLoaded: Boolean,
    isDefaultHome: Boolean,
    accessibilityAccess: Boolean,
    dndAccess: Boolean,
    notificationsEnabled: Boolean,
    justFinishedMinutes: Int?,
    onDismissFinished: () -> Unit,
    onStartMinimal: () -> Unit,
    onCommitmentHours: (Int) -> Unit,
    onToggleAllowed: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onAllowEssentials: () -> Unit,
    onAllowAll: () -> Unit,
    onClearAllowed: () -> Unit,
    onShowAppIcons: (Boolean) -> Unit,
    onPauseInterruptions: (Boolean) -> Unit,
    onRequireUnlock: (Boolean) -> Unit,
    onWallpaper: (Wallpaper) -> Unit,
    onOpenHomeSettings: () -> Unit,
    onRequestAccessibilityAccess: () -> Unit,
    onRequestDndAccess: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var selectedTab by remember { mutableStateOf(MinimalTab.HOME) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when (selectedTab) {
                    MinimalTab.HOME -> MinimalPlanTab(
                        settings = settings,
                        isDefaultHome = isDefaultHome,
                        justFinishedMinutes = justFinishedMinutes,
                        onDismissFinished = onDismissFinished,
                        onCommitmentHours = onCommitmentHours,
                        onOpenAppsTab = { selectedTab = MinimalTab.APPS },
                        onOpenHomeSettings = onOpenHomeSettings,
                        onStartMinimal = onStartMinimal,
                    )

                    MinimalTab.APPS -> AppsCurateTab(
                        apps = apps,
                        appsLoaded = appsLoaded,
                        allowedPackages = settings.allowedPackages,
                        favoritePackages = settings.favoritePackages,
                        onToggleAllowed = onToggleAllowed,
                        onToggleFavorite = onToggleFavorite,
                        onAllowEssentials = onAllowEssentials,
                        onAllowAll = onAllowAll,
                        onClearAllowed = onClearAllowed,
                    )

                    MinimalTab.TUNE -> MinimalTuneTab(
                        settings = settings,
                        history = history,
                        isDefaultHome = isDefaultHome,
                        accessibilityAccess = accessibilityAccess,
                        dndAccess = dndAccess,
                        notificationsEnabled = notificationsEnabled,
                        onShowAppIcons = onShowAppIcons,
                        onPauseInterruptions = onPauseInterruptions,
                        onRequireUnlock = onRequireUnlock,
                        onWallpaper = onWallpaper,
                        onOpenHomeSettings = onOpenHomeSettings,
                        onRequestAccessibilityAccess = onRequestAccessibilityAccess,
                        onRequestDndAccess = onRequestDndAccess,
                        onOpenNotificationSettings = onOpenNotificationSettings,
                    )
                }
            }

            NavigationBar(
                containerColor = colors.surface,
                tonalElevation = 2.dp,
            ) {
                MinimalTab.entries.forEach { tab ->
                    val icon = when (tab) {
                        MinimalTab.HOME -> AppIcons.HomeCalm
                        MinimalTab.APPS -> AppIcons.AppsDrawer
                        MinimalTab.TUNE -> AppIcons.Tune
                    }
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(icon, contentDescription = tab.title) },
                        label = { Text(tab.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MinimalPlanTab(
    settings: MinimalSettings,
    isDefaultHome: Boolean,
    justFinishedMinutes: Int?,
    onDismissFinished: () -> Unit,
    onCommitmentHours: (Int) -> Unit,
    onOpenAppsTab: () -> Unit,
    onOpenHomeSettings: () -> Unit,
    onStartMinimal: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val palette = LocalMinimalPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MinimalBro",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground,
                    )
                    Text(
                        text = "Calm minimalist phone · Unlimited apps",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = colors.primaryContainer,
                ) {
                    Text(
                        text = if (isDefaultHome) "Default Home" else "Launcher Ready",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            if (justFinishedMinutes != null) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    onClick = onDismissFinished,
                    shape = RoundedCornerShape(16.dp),
                    color = colors.secondaryContainer.copy(alpha = 0.72f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Completed ${formatLoggedMinutes(justFinishedMinutes)} minimal.",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSecondaryContainer,
                        )
                        Text(
                            text = "Dismiss",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.secondary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Commitment Hero Bento Card
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, palette.subtleBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "COMMITMENT",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                            )
                            Text(
                                text = formatCommitmentHours(settings.commitmentHours),
                                style = MaterialTheme.typography.headlineMedium,
                                color = colors.onSurface,
                            )
                            Text(
                                text = if (settings.commitmentHours <= 0) {
                                    "Permanent calm phone until you hold to exit"
                                } else {
                                    "Multi-day or hourly minimal commitment"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            NudgePill("-1d") {
                                val next = (settings.commitmentHours - 24).coerceAtLeast(0)
                                onCommitmentHours(next)
                            }
                            NudgePill("+1d") {
                                val base = if (settings.commitmentHours <= 0) 0 else settings.commitmentHours
                                onCommitmentHours((base + 24).coerceAtMost(MinimalConstants.MAX_TIMED_HOURS))
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            0 to "∞ Ongoing",
                            4 to "4h",
                            12 to "12h",
                            24 to "1d",
                            72 to "3d",
                            168 to "7d",
                            336 to "14d",
                            720 to "30d",
                        ).forEach { (hours, label) ->
                            FilterChip(
                                selected = settings.commitmentHours == hours,
                                onClick = { onCommitmentHours(hours) },
                                label = {
                                    Text(
                                        text = label,
                                        fontWeight = if (settings.commitmentHours == hours) FontWeight.Bold else FontWeight.Medium,
                                    )
                                },
                                shape = RoundedCornerShape(999.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.primaryContainer,
                                    selectedLabelColor = colors.onPrimaryContainer,
                                ),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Unlimited Apps & Favorites Card
            Surface(
                onClick = onOpenAppsTab,
                shape = RoundedCornerShape(22.dp),
                color = colors.primaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, palette.subtleBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${settings.allowedPackages.size} Essential Apps · No limit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface,
                        )
                        Text(
                            text = "${settings.favoritePackages.size} pinned on Home · Tap to curate anytime",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "Curate",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Default Home Launcher Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, palette.subtleBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isDefaultHome) "Active as Default Home" else "Use as Default Home Launcher",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.onSurface,
                        )
                        Text(
                            text = if (isDefaultHome) {
                                "Pressing Home opens your distraction-free MinimalBro launcher."
                            } else {
                                "Make MinimalBro your real phone Home screen cleanly."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = onOpenHomeSettings, shape = RoundedCornerShape(14.dp)) {
                        Text(if (isDefaultHome) "Switch" else "Set Home")
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = onStartMinimal,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
        ) {
            Text(
                text = minimalStartButtonLabel(settings),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun NudgePill(label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = colors.surfaceVariant.copy(alpha = 0.72f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun MinimalTuneTab(
    settings: MinimalSettings,
    history: List<MinimalRecord>,
    isDefaultHome: Boolean,
    accessibilityAccess: Boolean,
    dndAccess: Boolean,
    notificationsEnabled: Boolean,
    onShowAppIcons: (Boolean) -> Unit,
    onPauseInterruptions: (Boolean) -> Unit,
    onRequireUnlock: (Boolean) -> Unit,
    onWallpaper: (Wallpaper) -> Unit,
    onOpenHomeSettings: () -> Unit,
    onRequestAccessibilityAccess: () -> Unit,
    onRequestDndAccess: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val palette = LocalMinimalPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(
            text = "Style & Protection",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Tune your minimalist surface, theme, and optional distraction guard.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))

        if (history.isNotEmpty()) {
            CalmStatsCard(history)
            Spacer(Modifier.height(18.dp))
        }

        Text(
            text = "THEME PALETTE",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))

        val themes = Wallpaper.entries.chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            themes.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowItems.forEach { theme ->
                        val selected = theme == settings.wallpaper
                        Surface(
                            onClick = { onWallpaper(theme) },
                            shape = RoundedCornerShape(18.dp),
                            color = if (selected) colors.primaryContainer else colors.surface,
                            border = BorderStroke(
                                1.dp,
                                if (selected) colors.primary else palette.subtleBorder,
                            ),
                            modifier = Modifier.weight(1f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(wallpaperSwatch(theme)),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = theme.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = colors.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "CONTROLS",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                TuneRow(
                    title = "Show subtle app icons",
                    subtitle = if (settings.showAppIcons) {
                        "Muted icons shown beside typography on Home."
                    } else {
                        "Typography-only list (recommended for calmest screen)."
                    },
                    checked = settings.showAppIcons,
                    onCheckedChange = onShowAppIcons,
                )
                TuneRow(
                    title = "Hidden-app distraction guard",
                    subtitle = if (accessibilityAccess) {
                        "Active: redirects hidden apps opened from notifications or recents."
                    } else {
                        "Optional Accessibility guard to block hidden apps while Minimal is active."
                    },
                    checked = accessibilityAccess,
                    onCheckedChange = { onRequestAccessibilityAccess() },
                    onClick = onRequestAccessibilityAccess,
                )
                TuneRow(
                    title = "Silence notifications (DND)",
                    subtitle = if (dndAccess) {
                        "Turns Do Not Disturb on if off, and restores when Minimal ends."
                    } else {
                        "Grant Do Not Disturb access to silence pings during minimal mode."
                    },
                    checked = settings.pauseInterruptions,
                    onCheckedChange = onPauseInterruptions,
                    onClick = if (!dndAccess) onRequestDndAccess else null,
                )
                TuneRow(
                    title = "Require unlock to exit",
                    subtitle = "Confirm device PIN or biometric before exiting Minimal mode.",
                    checked = settings.requireUnlockToEnd,
                    onCheckedChange = onRequireUnlock,
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onOpenHomeSettings,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(if (isDefaultHome) "Home Active" else "Default Home")
            }
            if (!notificationsEnabled) {
                OutlinedButton(
                    onClick = onOpenNotificationSettings,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Notifications")
                }
            }
        }
    }
}

@Composable
private fun TuneRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick?.invoke() ?: onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        Spacer(Modifier.width(10.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

fun minimalStartButtonLabel(settings: MinimalSettings): String =
    if (settings.commitmentHours <= 0) {
        "Enter Ongoing Minimal"
    } else {
        "Enter ${formatCommitmentHours(settings.commitmentHours)} Minimal"
    }
