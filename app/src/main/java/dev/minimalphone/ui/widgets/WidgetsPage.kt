package dev.minimalphone.ui.widgets

import android.Manifest
import android.content.Context
import android.media.AudioManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.minimalphone.ui.theme.LocalMinimalPalette
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Essential Widgets Dashboard:
 * 1. Native Android App Widgets (Google Calendar, Samsung Health, Google Fit, Spotify, etc.)
 * 2. Instant UPI QR Scanner
 * 3. Daily Step Counts with runtime sensor permission & 1-tap official health widget linking
 * 4. Minimal Calendar & 7-day strip
 * 5. Quick Utilities (Torch, Ringer mode, Daily Intention Scratchpad)
 */
@Composable
fun WidgetsPage(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val palette = LocalMinimalPalette.current

    val appWidgetHelper = remember { AppWidgetHostHelper.get(context) }
    val savedWidgetIds by appWidgetHelper.widgetIds.collectAsState()
    var showAddWidgetSheet by remember { mutableStateOf(false) }

    val stepManager = remember { StepCountManager.get(context) }
    val steps by stepManager.dailySteps.collectAsState()
    var hasActivityPermission by remember {
        mutableStateOf(StepCountManager.hasActivityRecognitionPermission(context))
    }

    val activityPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        hasActivityPermission = isGranted
        if (isGranted) {
            stepManager.register()
        }
    }

    var hasCalendarPermission by remember {
        mutableStateOf(CalendarEventsReader.hasCalendarPermission(context))
    }
    var todayEvents by remember {
        mutableStateOf(CalendarEventsReader.readTodayEvents(context))
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        hasCalendarPermission = isGranted
        if (isGranted) {
            todayEvents = CalendarEventsReader.readTodayEvents(context)
        }
    }

    DisposableEffect(hasCalendarPermission) {
        if (hasCalendarPermission) {
            todayEvents = CalendarEventsReader.readTodayEvents(context)
        }
        onDispose {}
    }

    DisposableEffect(Unit) {
        stepManager.register()
        onDispose { stepManager.unregister() }
    }

    var isTorchOn by remember { mutableStateOf(false) }
    var ringerMode by remember { mutableIntStateOf(QuickToolsHelper.getCurrentRingerMode(context)) }
    var dailyNote by remember { mutableStateOf(QuickToolsHelper.readDailyNote(context)) }
    var isEditingNote by remember { mutableStateOf(false) }

    val todayCalendar = remember { Calendar.getInstance() }
    val monthYearText = remember {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Page header / title + Add Native Widget Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Widgets & Tools",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onBackground,
                )
                Text(
                    text = "Native app widgets & quick tools",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Surface(
                onClick = { showAddWidgetSheet = true },
                shape = RoundedCornerShape(999.dp),
                color = colors.primary,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Widget",
                        tint = colors.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Add Widget",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimary,
                    )
                }
            }
        }

        // Section 1: Native Android App Widgets (Built by installed apps)
        if (savedWidgetIds.isNotEmpty()) {
            savedWidgetIds.forEach { widgetId ->
                NativeAppWidgetCard(
                    appWidgetId = widgetId,
                    helper = appWidgetHelper,
                    onRemove = { appWidgetHelper.deleteWidget(widgetId) },
                )
            }
        } else {
            // Friendly prompt to add official app widgets
            Surface(
                onClick = { showAddWidgetSheet = true },
                shape = RoundedCornerShape(22.dp),
                color = colors.surface.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, palette.subtleBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .background(colors.primaryContainer.copy(alpha = 0.65f), CircleShape),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = colors.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Add Official App Widgets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface,
                        )
                        Text(
                            text = "Embed widgets from Google Calendar, Samsung Health, Google Fit, Spotify, etc.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Widget 2: Instant UPI QR Scanner Bento Card
        Surface(
            onClick = { QuickToolsHelper.openUpiScanner(context) },
            shape = RoundedCornerShape(24.dp),
            color = colors.surface.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, palette.subtleBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .background(colors.primaryContainer.copy(alpha = 0.65f), CircleShape),
                ) {
                    Text(
                        text = "QR",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "UPI Quick Scanner",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                    )
                    Text(
                        text = "Scan any QR code · GPay, PhonePe, Paytm",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = colors.primary,
                ) {
                    Text(
                        text = "Scan",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }
        }

        // Widget 3: Step Count & Mindful Movement
        val stepGoal = 10_000
        val stepFraction = (steps.toFloat() / stepGoal.toFloat()).coerceIn(0f, 1f)
        val healthApp = stepManager.detectedHealthApp

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surface.copy(alpha = 0.85f),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Daily Movement",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(8.dp))
                            val healthBadge = if (healthApp != null) "${healthApp.name} ↗" else "Health App ↗"
                            Surface(
                                onClick = { StepCountManager.openHealthApp(context) },
                                shape = RoundedCornerShape(999.dp),
                                color = colors.primaryContainer.copy(alpha = 0.6f),
                            ) {
                                Text(
                                    text = healthBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "%,d".format(steps),
                                style = TextStyle(
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Light,
                                    color = colors.onSurface,
                                    fontFeatureSettings = "tnum",
                                ),
                            )
                            Text(
                                text = " / %,d steps".format(stepGoal),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp),
                            )
                        }
                    }

                    Text(
                        text = "${(stepFraction * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                    )
                }

                Spacer(Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { stepFraction },
                    color = colors.primary,
                    trackColor = colors.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (!hasActivityPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Surface(
                            onClick = {
                                activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                            },
                            shape = RoundedCornerShape(999.dp),
                            color = colors.secondaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = "Grant Step Permission",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSecondaryContainer,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }

                    Surface(
                        onClick = { showAddWidgetSheet = true },
                        shape = RoundedCornerShape(999.dp),
                        color = colors.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = "+ Add Health Widget",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onPrimaryContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }

        // Widget 4: Minimal Calendar Bento Card
        Surface(
            onClick = { QuickToolsHelper.openCalendar(context) },
            shape = RoundedCornerShape(24.dp),
            color = colors.surface.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, palette.subtleBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Calendar",
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = monthYearText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface,
                        )
                    }
                    Text(
                        text = "Open App",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary,
                    )
                }

                Spacer(Modifier.height(14.dp))

                // 7-Day compact date strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    val currentDayIndex = (todayCalendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
                    val currentDayOfMonth = todayCalendar.get(Calendar.DAY_OF_MONTH)

                    daysOfWeek.forEachIndexed { index, dayName ->
                        val dayOffset = index - currentDayIndex
                        val dayNum = (currentDayOfMonth + dayOffset).coerceAtLeast(1)
                        val isToday = index == currentDayIndex

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isToday) colors.primaryContainer else Color.Transparent)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = dayName.take(1),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isToday) colors.onPrimaryContainer else colors.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "$dayNum",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isToday) colors.primary else colors.onSurface,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(palette.subtleBorder),
                )
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "TODAY'S SCHEDULE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurfaceVariant,
                    )
                    if (hasCalendarPermission && todayEvents.isNotEmpty()) {
                        Text(
                            text = "${todayEvents.size} event${if (todayEvents.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.primary,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (!hasCalendarPermission) {
                    Surface(
                        onClick = {
                            calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, palette.subtleBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Sync with Calendar app",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurface,
                            )
                            Text(
                                text = "Grant permission",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary,
                            )
                        }
                    }
                } else if (todayEvents.isEmpty()) {
                    Text(
                        text = "No scheduled events for today. Enjoy your day!",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        todayEvents.take(4).forEach { event ->
                            Surface(
                                onClick = { CalendarEventsReader.openEvent(context, event.id) },
                                shape = RoundedCornerShape(12.dp),
                                color = colors.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(colors.primary),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = event.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = event.formatTimeRange() + (if (!event.location.isNullOrBlank()) " · ${event.location}" else ""),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Widget 5: Quick Utilities (Flashlight & Ringer mode)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Flashlight Tile
            Surface(
                onClick = {
                    QuickToolsHelper.toggleFlashlight(context, isTorchOn) { newState ->
                        isTorchOn = newState
                    }
                },
                shape = RoundedCornerShape(20.dp),
                color = if (isTorchOn) colors.primaryContainer else colors.surface.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, if (isTorchOn) colors.primary else palette.subtleBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(84.dp),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = if (isTorchOn) "🔦 Torch ON" else "🔦 Torch",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isTorchOn) colors.onPrimaryContainer else colors.onSurface,
                    )
                    Text(
                        text = if (isTorchOn) "Tap to turn off" else "Tap to turn on",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }

            // Ringer Mode Tile
            Surface(
                onClick = {
                    ringerMode = QuickToolsHelper.cycleRingerMode(context)
                },
                shape = RoundedCornerShape(20.dp),
                color = colors.surface.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, palette.subtleBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(84.dp),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    val (modeLabel, iconStr) = when (ringerMode) {
                        AudioManager.RINGER_MODE_NORMAL -> "Normal Ring" to "🔔"
                        AudioManager.RINGER_MODE_VIBRATE -> "Vibrate" to "📳"
                        else -> "Silent" to "🔕"
                    }
                    Text(
                        text = "$iconStr $modeLabel",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                    )
                    Text(
                        text = "Tap to cycle mode",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }

        // Widget 6: Daily Intention Scratchpad
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = colors.surface.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, palette.subtleBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Daily Intention",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurfaceVariant,
                    )
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit intention",
                        tint = colors.primary,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { isEditingNote = !isEditingNote },
                    )
                }

                Spacer(Modifier.height(8.dp))

                if (isEditingNote) {
                    OutlinedTextField(
                        value = dailyNote,
                        onValueChange = {
                            dailyNote = it
                            QuickToolsHelper.saveDailyNote(context, it)
                        },
                        placeholder = { Text("What is your single most important goal today?") },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surface,
                            unfocusedContainerColor = colors.surface.copy(alpha = 0.7f),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(
                        text = dailyNote.ifBlank { "Tap to write today's single most important intention..." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (dailyNote.isBlank()) colors.onSurfaceVariant.copy(alpha = 0.6f) else colors.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isEditingNote = true }
                            .padding(vertical = 4.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showAddWidgetSheet) {
        AddWidgetBottomSheet(
            helper = appWidgetHelper,
            onDismiss = { showAddWidgetSheet = false },
            onWidgetAdded = { showAddWidgetSheet = false },
        )
    }
}
