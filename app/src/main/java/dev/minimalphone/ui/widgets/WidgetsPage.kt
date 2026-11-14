package dev.minimalphone.ui.widgets

import android.media.AudioManager
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.minimalphone.ui.theme.LocalMinimalPalette
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Secondary Home Screen Page: Essential Bento Widgets.
 * Includes:
 * 1. Minimal Calendar & 7-day strip
 * 2. Instant UPI QR Scanner
 * 3. Daily Step Counts
 * 4. Essential Utilities (Torch, Ringer mode, Daily Intention Scratchpad)
 */
@Composable
fun WidgetsPage(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val palette = LocalMinimalPalette.current

    val stepManager = remember { StepCountManager.get(context) }
    val steps by stepManager.dailySteps.collectAsState()

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
        // Page header / title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Essentials",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Light,
                    color = colors.onBackground,
                )
                Text(
                    text = "Daily glance & quick actions",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = colors.surfaceVariant.copy(alpha = 0.6f),
            ) {
                Text(
                    text = "Page 2/2",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        // Widget 1: Calendar Bento Card
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
                        text = "Open",
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
                        .size(48.dp)
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
                        text = "Tap to scan any QR code · GPay, PhonePe, Paytm",
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
                    Column {
                        Text(
                            text = "Daily Movement",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant,
                        )
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

                Spacer(Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { stepFraction },
                    color = colors.primary,
                    trackColor = colors.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                )
            }
        }

        // Widget 4: Quick Toggles (Flashlight & Ringer mode)
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
                modifier = Modifier.weight(1f),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isTorchOn) colors.primary else colors.surfaceVariant.copy(alpha = 0.7f),
                                CircleShape,
                            ),
                    ) {
                        Text(
                            text = if (isTorchOn) "☀" else "☼",
                            fontSize = 18.sp,
                            color = if (isTorchOn) colors.onPrimary else colors.onSurface,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Flashlight",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                    )
                    Text(
                        text = if (isTorchOn) "On" else "Off",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isTorchOn) colors.primary else colors.onSurfaceVariant,
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
                modifier = Modifier.weight(1f),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .background(colors.surfaceVariant.copy(alpha = 0.7f), CircleShape),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Ringer",
                            tint = colors.onSurface,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Sound Mode",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                    )
                    val ringerText = when (ringerMode) {
                        AudioManager.RINGER_MODE_NORMAL -> "Normal"
                        AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
                        AudioManager.RINGER_MODE_SILENT -> "Silent"
                        else -> "Sound"
                    }
                    Text(
                        text = ringerText,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary,
                    )
                }
            }
        }

        // Widget 5: Daily Intention Scratchpad
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
                    Text(
                        text = "Daily Focus Intention",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
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
                        placeholder = { Text("What is your primary focus for today?") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surface,
                            unfocusedContainerColor = colors.surface,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(
                        text = if (dailyNote.isNotBlank()) dailyNote else "Tap here or the pencil to set your mindful focus for today...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (dailyNote.isNotBlank()) colors.onSurface else colors.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isEditingNote = true }
                            .padding(vertical = 4.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
    }
}
