package dev.minimalphone.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.minimalphone.session.MinimalRecord
import dev.minimalphone.ui.theme.LocalMinimalPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Continuously depleting circular countdown ring: full (360°) at session start and
 * smoothly depleting toward 0° as the session deadline approaches.
 */
@Composable
fun DepletingCountdownRing(
    elapsedFraction: Float,
    remainingFraction: Float,
    ringColor: Color,
    trackColor: Color,
    sizeDp: Dp = 56.dp,
    strokeDp: Dp = 5.dp,
    showTipDot: Boolean = true,
) {
    Canvas(Modifier.size(sizeDp)) {
        val stroke = strokeDp.toPx()
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(inset, inset)

        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke),
        )

        val sweep = (360f * remainingFraction).coerceIn(0f, 360f)
        if (sweep > 0.2f) {
            val startAngle = -90f + (360f * elapsedFraction)
            drawArc(
                color = ringColor,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            if (showTipDot && sweep < 359.5f) {
                val radius = arcSize.width / 2f
                val cx = size.width / 2f
                val cy = size.height / 2f
                val angleRad = (startAngle * PI / 180.0).toFloat()
                val tipX = cx + radius * cos(angleRad)
                val tipY = cy + radius * sin(angleRad)
                drawCircle(
                    color = ringColor,
                    radius = stroke * 0.72f,
                    center = Offset(tipX, tipY),
                )
            }
        }
    }
}

@Composable
fun CalmStatsCard(history: List<MinimalRecord>) {
    val colors = MaterialTheme.colorScheme
    val palette = LocalMinimalPalette.current

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, palette.subtleBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "MINIMAL REFLECTION",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatCell("Total offline", formatLoggedMinutes(totalMinutes(history)))
                StatCell("Today", formatLoggedMinutes(todayMinutes(history)))
                StatCell("Completed", "${completedCount(history)}/${history.size}")
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    val colors = MaterialTheme.colorScheme
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
    }
}
