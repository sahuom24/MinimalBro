package dev.minimalphone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.minimalphone.session.MicroSessionManager
import kotlinx.coroutines.delay

/**
 * Gentle full-screen session completion overlay.
 *
 * Appears when a 5-minute micro-session expires:
 * "Session ended. Did you finish what you came for?"
 * Enforces a 60-second cooldown before the app can be reopened.
 */
@Composable
fun SessionEndedOverlay(
    packageName: String,
    appLabel: String,
    initialCooldownSecs: Int = 60,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var remainingCooldown by remember { mutableIntStateOf(initialCooldownSecs) }

    LaunchedEffect(packageName) {
        while (true) {
            val remaining = MicroSessionManager.getRemainingCooldownSeconds(packageName)
            remainingCooldown = remaining
            if (remaining <= 0) break
            delay(500L)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = colors.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Header app info
                AppIcon(packageName = packageName, size = 48.dp)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = appLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                )

                Spacer(Modifier.height(24.dp))

                // Cooldown countdown circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(100.dp)
                        .background(colors.primaryContainer.copy(alpha = 0.45f), CircleShape)
                        .border(3.dp, colors.primary, CircleShape),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${remainingCooldown}s",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onPrimaryContainer,
                        )
                        Text(
                            text = "Cooldown",
                            fontSize = 11.sp,
                            color = colors.onPrimaryContainer.copy(alpha = 0.75f),
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Core Reflection Prompt
                Text(
                    text = "Session ended.",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Did you finish what you came for?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.primary,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = if (remainingCooldown > 0) {
                        "Take a 60-second pause to let the dopamine loop reset before deciding if you truly need to return."
                    } else {
                        "Cooldown complete. You may return mindfully whenever you choose."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(28.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Back to Calm Home")
                }
            }
        }
    }
}
