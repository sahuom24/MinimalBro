package dev.minimalphone.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.minimalphone.apps.LaunchableApp
import dev.minimalphone.session.AppTier
import kotlinx.coroutines.delay

/**
 * 5-Second Intention Speed Bump.
 *
 * Placed before launching Tier 1 or Tier 2 applications:
 * - A 5-second breathing circle animation to interrupt unconscious dopamine reflexes.
 * - Prompt: "What is your goal for opening this?"
 * - Mindful Cancel button to easily back out.
 */
@Composable
fun IntentionSpeedBumpDialog(
    app: LaunchableApp,
    tier: AppTier,
    onDismiss: () -> Unit,
    onProceed: (goal: String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var countdown by remember { mutableIntStateOf(5) }
    var intentionGoal by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000L)
            countdown -= 1
        }
    }

    // Breathing circle pulse animation: gentle 2.5s expansion & contraction
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathePulse",
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // App header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    AppIcon(packageName = app.packageName, size = 32.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface,
                        )
                        Text(
                            text = if (tier == AppTier.TIER_2) "Dopamine Trap · 5m micro-session" else "Utility · 5m micro-session",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (tier == AppTier.TIER_2) colors.error else colors.primary,
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // 5-Second Breathing Circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(120.dp),
                ) {
                    // Outer breathing glow / ripple
                    Box(
                        modifier = Modifier
                            .size(105.dp)
                            .scale(pulseScale)
                            .background(
                                color = colors.primary.copy(alpha = 0.12f),
                                shape = CircleShape,
                            ),
                    )

                    // Inner timer circle
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                color = colors.primaryContainer.copy(alpha = 0.5f),
                                shape = CircleShape,
                            )
                            .border(2.dp, colors.primary, CircleShape),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (countdown > 0) {
                                Text(
                                    text = "$countdown",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onPrimaryContainer,
                                )
                                Text(
                                    text = "Breathe",
                                    fontSize = 11.sp,
                                    color = colors.onPrimaryContainer.copy(alpha = 0.8f),
                                )
                            } else {
                                Text(
                                    text = "Ready",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primary,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Intention Prompt
                Text(
                    text = "What is your goal for opening this?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "A 5-second mindful pause to break unconscious scrolling habits.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = intentionGoal,
                    onValueChange = { intentionGoal = it },
                    placeholder = {
                        Text(
                            "e.g. Check delivery status, reply to client...",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.surfaceVariant.copy(alpha = 0.35f),
                        unfocusedContainerColor = colors.surfaceVariant.copy(alpha = 0.2f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(22.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { onProceed(intentionGoal.trim()) },
                        enabled = countdown == 0,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            disabledContainerColor = colors.onSurface.copy(alpha = 0.12f),
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = if (countdown > 0) "Pause (${countdown}s)" else "Continue",
                        )
                    }
                }
            }
        }
    }
}
