package dev.minimalphone.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.minimalphone.apps.DefaultCommunicationApps
import dev.minimalphone.apps.LaunchableApp
import dev.minimalphone.blocker.MinimalAccessibilityService
import dev.minimalphone.session.ActiveMinimalSession
import dev.minimalphone.session.AppTier
import dev.minimalphone.session.AppTierClassifier
import dev.minimalphone.session.MicroSessionManager
import dev.minimalphone.session.Wallpaper
import dev.minimalphone.ui.theme.LocalMinimalPalette
import dev.minimalphone.ui.theme.wallpaperBrush
import dev.minimalphone.ui.widgets.WidgetsPage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MinimalHomeScreen(
    active: ActiveMinimalSession,
    wallpaper: Wallpaper,
    allowedApps: List<LaunchableApp>,
    favoriteApps: List<LaunchableApp>,
    showAppIcons: Boolean,
    onExpired: () -> Unit,
    onRequestExit: () -> Unit,
    onExtendHours: (Int) -> Unit,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val palette = LocalMinimalPalette.current
    val scope = rememberCoroutineScope()

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var searchQuery by remember { mutableStateOf("") }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var speedBumpTarget by remember { mutableStateOf<Pair<LaunchableApp, AppTier>?>(null) }
    var cooldownTarget by remember { mutableStateOf<Pair<LaunchableApp, Int>?>(null) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

    val handleAppClick: (LaunchableApp) -> Unit = { app ->
        if (MicroSessionManager.isInCooldown(app.packageName)) {
            val remaining = MicroSessionManager.getRemainingCooldownSeconds(app.packageName)
            cooldownTarget = app to remaining
        } else {
            val tier = active.appTiers[app.packageName] ?: AppTierClassifier.suggestTier(app.packageName, app.label)
            if (tier == AppTier.TIER_0) {
                launchMinimalApp(context, app.packageName)
            } else {
                speedBumpTarget = app to tier
            }
        }
    }

    LaunchedEffect(active.endsAtMillis, active.isIndefinite) {
        while (true) {
            now = System.currentTimeMillis()
            if (!active.isIndefinite && now >= active.endsAtMillis) {
                onExpired()
                break
            }
            delay(100)
        }
    }

    val clockText = remember(now / 15_000L) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now))
    }
    val dateText = remember(now / 60_000L) {
        SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(now))
    }

    val statusText = if (active.isIndefinite) {
        "Minimal · ${formatElapsedShort(active.elapsedMillis(now))}"
    } else {
        "${formatCountdown(active.remainingMillis(now))} left"
    }

    val filteredApps = remember(allowedApps, searchQuery) {
        if (searchQuery.isBlank()) {
            allowedApps
        } else {
            allowedApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }
    }

    val backgroundBrush = if (wallpaper == Wallpaper.DYNAMIC) {
        Brush.verticalGradient(listOf(colors.background, colors.surface))
    } else {
        wallpaperBrush(wallpaper, darkTheme = !palette.isLightSurface)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 22.dp, vertical = 18.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Minimalist Bento Header: wall clock, date, depleting ring / ∞ indicator
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = colors.surface.copy(alpha = 0.82f),
                border = BorderStroke(1.dp, palette.subtleBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = clockText,
                            style = TextStyle(
                                color = colors.onSurface,
                                fontSize = 46.sp,
                                fontWeight = FontWeight.Light,
                                fontFeatureSettings = "tnum",
                            ),
                        )
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Box(contentAlignment = Alignment.Center) {
                            DepletingCountdownRing(
                                elapsedFraction = active.fractionElapsed(now),
                                remainingFraction = active.fractionRemaining(now),
                                ringColor = colors.primary,
                                trackColor = palette.ringTrack,
                                sizeDp = 44.dp,
                                strokeDp = 4.dp,
                                showTipDot = !active.isIndefinite,
                            )
                            Text(
                                text = if (active.isIndefinite) "∞" else "M",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                        )
                        if (!active.isIndefinite) {
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                onClick = { onExtendHours(24) },
                                shape = RoundedCornerShape(999.dp),
                                color = colors.primaryContainer.copy(alpha = 0.65f),
                            ) {
                                Text(
                                    text = "+1d",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Subtle 2-dot page indicator between Apps and Widgets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(if (pagerState.currentPage == 0) 18.dp else 6.dp, 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (pagerState.currentPage == 0) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.35f),
                        )
                        .clickable { scope.launch { pagerState.animateScrollToPage(0) } },
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(if (pagerState.currentPage == 1) 18.dp else 6.dp, 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (pagerState.currentPage == 1) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.35f),
                        )
                        .clickable { scope.launch { pagerState.animateScrollToPage(1) } },
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                if (page == 0) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (favoriteApps.isNotEmpty() && searchQuery.isBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                favoriteApps.forEach { fav ->
                                    Surface(
                                        onClick = { handleAppClick(fav) },
                                        shape = RoundedCornerShape(999.dp),
                                        color = colors.primaryContainer.copy(alpha = 0.65f),
                                        border = BorderStroke(1.dp, palette.subtleBorder),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            if (showAppIcons) {
                                                AppIcon(packageName = fav.packageName, size = 18.dp)
                                                Spacer(Modifier.width(7.dp))
                                            }
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

                        // Instant search across all allowed apps
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = "Search apps (${allowedApps.size})...",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    modifier = Modifier.size(18.dp),
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
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colors.surface.copy(alpha = 0.78f),
                                unfocusedContainerColor = colors.surface.copy(alpha = 0.58f),
                                focusedBorderColor = colors.primary.copy(alpha = 0.6f),
                                unfocusedBorderColor = palette.subtleBorder,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(Modifier.height(8.dp))

                        // Clean alphabetical distraction-free app list
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            if (filteredApps.isEmpty()) {
                                item {
                                    Text(
                                        text = if (allowedApps.isEmpty()) {
                                            "Calm Home active. Phone & Messages remain available below."
                                        } else {
                                            "No app matches \"$searchQuery\""
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 24.dp, horizontal = 8.dp),
                                    )
                                }
                            } else {
                                items(filteredApps, key = { it.packageName }) { app ->
                                    val appTier = active.appTiers[app.packageName] ?: AppTierClassifier.suggestTier(app.packageName, app.label)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { handleAppClick(app) }
                                            .padding(horizontal = 10.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (showAppIcons) {
                                            AppIcon(packageName = app.packageName, size = 28.dp)
                                        } else {
                                            val dotColor = when (appTier) {
                                                AppTier.TIER_0 -> colors.primary.copy(alpha = 0.65f)
                                                AppTier.TIER_1 -> colors.secondary.copy(alpha = 0.85f)
                                                AppTier.TIER_2 -> colors.error.copy(alpha = 0.75f)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(dotColor),
                                            )
                                        }
                                        Spacer(Modifier.width(14.dp))
                                        Text(
                                            text = app.label,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Normal,
                                            color = colors.onBackground,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                        if (appTier == AppTier.TIER_1) {
                                            Text(
                                                text = "5m",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.padding(horizontal = 6.dp),
                                            )
                                        } else if (appTier == AppTier.TIER_2) {
                                            Text(
                                                text = "Trap",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.error.copy(alpha = 0.7f),
                                                modifier = Modifier.padding(horizontal = 6.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    WidgetsPage(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Bottom utility dock: Phone, Messages, and mindful 1.5s hold to exit
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    onClick = { DefaultCommunicationApps.openPhone(context) },
                    shape = CircleShape,
                    color = colors.surfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.size(46.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Phone",
                            tint = colors.onSurface,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }

                Surface(
                    onClick = { DefaultCommunicationApps.openMessages(context) },
                    shape = CircleShape,
                    color = colors.surfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.size(46.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = AppIcons.Messages,
                            contentDescription = "Messages",
                            tint = colors.onSurface,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.surfaceVariant.copy(alpha = 0.68f))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    val job = scope.launch {
                                        val steps = 30
                                        for (i in 1..steps) {
                                            holdProgress = i / steps.toFloat()
                                            delay(50L)
                                        }
                                        holdProgress = 0f
                                        onRequestExit()
                                    }
                                    tryAwaitRelease()
                                    job.cancel()
                                    holdProgress = 0f
                                },
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (holdProgress > 0f) "Keep holding to exit..." else "Hold to exit Minimal",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant,
                        )
                        if (holdProgress > 0f) {
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { holdProgress },
                                color = colors.primary,
                                trackColor = colors.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                            )
                        }
                    }
                }
            }
        }

        speedBumpTarget?.let { (app, tier) ->
            IntentionSpeedBumpDialog(
                app = app,
                tier = tier,
                onDismiss = { speedBumpTarget = null },
                onProceed = { _ ->
                    speedBumpTarget = null
                    launchMinimalApp(context, app.packageName)
                },
            )
        }

        cooldownTarget?.let { (app, remainingSecs) ->
            SessionEndedOverlay(
                packageName = app.packageName,
                appLabel = app.label,
                initialCooldownSecs = remainingSecs,
                onDismiss = { cooldownTarget = null },
            )
        }
    }
}

private fun launchMinimalApp(context: Context, packageName: String) {
    MinimalAccessibilityService.noteAllowedAppLaunch(packageName)
    val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
    runCatching { context.startActivity(intent) }
}
