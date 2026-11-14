package dev.minimalphone.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.minimalphone.apps.LaunchableApp
import dev.minimalphone.session.AppTier
import dev.minimalphone.session.AppTierClassifier
import dev.minimalphone.ui.theme.LocalMinimalPalette
import kotlinx.coroutines.launch

enum class DrawerLayoutMode {
    GRID,
    LIST,
}

private val ALPHABET_INDEX = listOf(
    "#", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L",
    "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NormalAppDrawer(
    apps: List<LaunchableApp>,
    appsLoaded: Boolean,
    allowedPackages: Set<String>,
    favoritePackages: Set<String>,
    appTiers: Map<String, AppTier>,
    onSetAppTier: (String, AppTier?) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenCurateMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val palette = LocalMinimalPalette.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var layoutMode by remember { mutableStateOf(DrawerLayoutMode.GRID) }
    var activeScrubLetter by remember { mutableStateOf<String?>(null) }
    var contextMenuApp by remember { mutableStateOf<LaunchableApp?>(null) }

    val sortedApps = remember(apps) {
        apps.sortedBy { it.label.lowercase() }
    }

    val filteredApps = remember(sortedApps, searchQuery) {
        if (searchQuery.isBlank()) {
            sortedApps
        } else {
            sortedApps.filter {
                it.label.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val favoriteAppObjects = remember(sortedApps, favoritePackages) {
        sortedApps.filter { it.packageName in favoritePackages }
    }

    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()

    val scrollToLetter: (String) -> Unit = { letter ->
        activeScrubLetter = letter
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val targetIndex = if (letter == "#") {
            0
        } else {
            filteredApps.indexOfFirst {
                it.label.startsWith(letter, ignoreCase = true)
            }.takeIf { it >= 0 } ?: -1
        }
        if (targetIndex >= 0) {
            scope.launch {
                if (layoutMode == DrawerLayoutMode.GRID) {
                    gridState.scrollToItem(targetIndex)
                } else {
                    listState.scrollToItem(targetIndex)
                }
            }
        }
    }

    val launchApp: (LaunchableApp) -> Unit = { app ->
        val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent != null) {
            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            runCatching { context.startActivity(launchIntent) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 18.dp, end = 4.dp, top = 12.dp, bottom = 8.dp),
    ) {
        // App Drawer Top Bar: Search Bar & Action Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = if (appsLoaded) "Search ${apps.size} apps..." else "Search apps...",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(18.dp),
                        tint = colors.onSurfaceVariant,
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surface.copy(alpha = 0.88f),
                    unfocusedContainerColor = colors.surface.copy(alpha = 0.65f),
                    focusedBorderColor = colors.primary.copy(alpha = 0.65f),
                    unfocusedBorderColor = palette.subtleBorder,
                ),
                modifier = Modifier.weight(1f),
            )

            // Layout Mode Switcher (Grid vs List)
            Surface(
                onClick = {
                    layoutMode = if (layoutMode == DrawerLayoutMode.GRID) {
                        DrawerLayoutMode.LIST
                    } else {
                        DrawerLayoutMode.GRID
                    }
                },
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, palette.subtleBorder),
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (layoutMode == DrawerLayoutMode.GRID) "⊞" else "≡",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurfaceVariant,
                    )
                }
            }

            // Quick Curation / Focus Rules Button
            Surface(
                onClick = onOpenCurateMode,
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, palette.subtleBorder),
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "⚙",
                        fontSize = 18.sp,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }

        // Horizontal Pinned Favorites Row (when not searching)
        if (favoriteAppObjects.isNotEmpty() && searchQuery.isBlank()) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 14.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                favoriteAppObjects.forEach { fav ->
                    Surface(
                        onClick = { launchApp(fav) },
                        shape = RoundedCornerShape(999.dp),
                        color = colors.primaryContainer.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, palette.subtleBorder),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppIcon(packageName = fav.packageName, size = 18.dp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = fav.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onPrimaryContainer,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (!appsLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Loading apps...",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        } else {
            // App Drawer Content + Alphabet Fast-Scroll Index
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                if (filteredApps.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No apps match \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                } else if (layoutMode == DrawerLayoutMode.GRID) {
                    // Standard 4-column Android Phone App Drawer Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        state = gridState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 26.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 48.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            AppGridItem(
                                app = app,
                                onClick = { launchApp(app) },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    contextMenuApp = app
                                },
                            )
                        }
                    }
                } else {
                    // Clean Vertical List View
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 26.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 48.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            val currentTier = appTiers[app.packageName] ?: AppTierClassifier.suggestTier(app.packageName, app.label)
                            val isAllowed = app.packageName in allowedPackages

                            AppListItem(
                                app = app,
                                isAllowed = isAllowed,
                                currentTier = currentTier,
                                onClick = { launchApp(app) },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    contextMenuApp = app
                                },
                            )
                        }
                    }
                }

                // Vertical Alphabet Fast-Scrubber Bar along the right edge
                if (searchQuery.isBlank() && filteredApps.size > 8) {
                    AlphabetScrubber(
                        letters = ALPHABET_INDEX,
                        activeLetter = activeScrubLetter,
                        onLetterSelected = scrollToLetter,
                        onDragEnd = { activeScrubLetter = null },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 2.dp),
                    )
                }

                // Center Floating Letter Indicator when scrubbing
                if (activeScrubLetter != null) {
                    Surface(
                        shape = CircleShape,
                        color = colors.primary,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .size(64.dp)
                            .align(Alignment.Center),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = activeScrubLetter ?: "",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.onPrimary,
                            )
                        }
                    }
                }
            }
        }
    }

    // App Long-Press Action Menu Bottom Sheet
    contextMenuApp?.let { app ->
        AppContextMenuBottomSheet(
            app = app,
            isAllowed = app.packageName in allowedPackages,
            isFavorite = app.packageName in favoritePackages,
            currentTier = appTiers[app.packageName] ?: AppTierClassifier.suggestTier(app.packageName, app.label),
            onDismiss = { contextMenuApp = null },
            onLaunch = {
                contextMenuApp = null
                launchApp(app)
            },
            onToggleFavorite = {
                onToggleFavorite(app.packageName)
            },
            onSetTier = { tier ->
                onSetAppTier(app.packageName, tier)
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppGridItem(
    app: LaunchableApp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(packageName = app.packageName, size = 48.dp)
        Spacer(Modifier.height(6.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Normal,
            color = colors.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppListItem(
    app: LaunchableApp,
    isAllowed: Boolean,
    currentTier: AppTier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val palette = LocalMinimalPalette.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, palette.subtleBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
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
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (isAllowed) currentTier.title.takeWhile { it != ':' } else "Focus Blocked",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isAllowed) {
                        when (currentTier) {
                            AppTier.TIER_0 -> colors.primary
                            AppTier.TIER_1 -> colors.secondary
                            AppTier.TIER_2 -> colors.error
                        }
                    } else colors.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun AlphabetScrubber(
    letters: List<String>,
    activeLetter: String?,
    onLetterSelected: (String) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(22.dp)
            .pointerInput(letters) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val itemHeight = size.height / letters.size.toFloat()
                        val index = (offset.y / itemHeight).toInt().coerceIn(0, letters.lastIndex)
                        onLetterSelected(letters[index])
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val itemHeight = size.height / letters.size.toFloat()
                        val index = (change.position.y / itemHeight).toInt().coerceIn(0, letters.lastIndex)
                        onLetterSelected(letters[index])
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd,
                )
            },
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { letter ->
            val isSelected = letter == activeLetter
            Text(
                text = letter,
                style = TextStyle(
                    fontSize = 9.sp,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                    color = if (isSelected) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.5f),
                ),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onLetterSelected(letter) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppContextMenuBottomSheet(
    app: LaunchableApp,
    isAllowed: Boolean,
    isFavorite: Boolean,
    currentTier: AppTier,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSetTier: (AppTier?) -> Unit,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val palette = LocalMinimalPalette.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 12.dp),
        ) {
            // Header: App icon, Title, Package name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AppIcon(packageName = app.packageName, size = 46.dp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Action 1: Open App
            Surface(
                onClick = onLaunch,
                shape = RoundedCornerShape(14.dp),
                color = colors.primaryContainer.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, palette.subtleBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🚀", fontSize = 18.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Open App",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onPrimaryContainer,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Action 2: Pin / Unpin from Home
            Surface(
                onClick = {
                    onToggleFavorite()
                    onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                color = colors.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, palette.subtleBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (isFavorite) "⭐" else "☆", fontSize = 18.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (isFavorite) "Unpin from Home" else "Pin to Home",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurface,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Action 3: System App Info
            Surface(
                onClick = {
                    onDismiss()
                    val infoIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", app.packageName, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    runCatching { context.startActivity(infoIntent) }
                },
                shape = RoundedCornerShape(14.dp),
                color = colors.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, palette.subtleBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("ℹ️", fontSize = 18.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "App Info (System)",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurface,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Action 4: Uninstall
            Surface(
                onClick = {
                    onDismiss()
                    val deleteIntent = Intent(Intent.ACTION_DELETE).apply {
                        data = Uri.parse("package:${app.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    runCatching { context.startActivity(deleteIntent) }
                },
                shape = RoundedCornerShape(14.dp),
                color = colors.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, palette.subtleBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🗑️", fontSize = 18.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Uninstall",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.error,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Focus Lock-In Tier Classification
            Text(
                text = "Lock-In Focus Tier",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(
                    selected = isAllowed && currentTier == AppTier.TIER_0,
                    onClick = {
                        onSetTier(AppTier.TIER_0)
                        onDismiss()
                    },
                    label = { Text("T0: Essential") },
                )
                FilterChip(
                    selected = isAllowed && currentTier == AppTier.TIER_1,
                    onClick = {
                        onSetTier(AppTier.TIER_1)
                        onDismiss()
                    },
                    label = { Text("T1: Utility") },
                )
                FilterChip(
                    selected = isAllowed && currentTier == AppTier.TIER_2,
                    onClick = {
                        onSetTier(AppTier.TIER_2)
                        onDismiss()
                    },
                    label = { Text("T2: Dopamine") },
                )
                FilterChip(
                    selected = !isAllowed,
                    onClick = {
                        onSetTier(null)
                        onDismiss()
                    },
                    label = { Text("Block") },
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}
