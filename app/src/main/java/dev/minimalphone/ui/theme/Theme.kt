package dev.minimalphone.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.minimalphone.session.Wallpaper

val SageMint = Color(0xFF9AD0A8)
val SkyTint = Color(0xFFA8C1FA)
val WarmTerra = Color(0xFFF0AE84)

@Immutable
data class MinimalPalette(
    val calmAccent: Color,
    val subtleBorder: Color,
    val ringTrack: Color,
    val isLightSurface: Boolean,
)

val LocalMinimalPalette = staticCompositionLocalOf {
    MinimalPalette(
        calmAccent = Color(0xFF436B4D),
        subtleBorder = Color.Black.copy(alpha = 0.08f),
        ringTrack = Color.Black.copy(alpha = 0.10f),
        isLightSurface = true,
    )
}

private val WarmPaperLightScheme = lightColorScheme(
    primary = Color(0xFF436B4D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE6DC),
    onPrimaryContainer = Color(0xFF14291A),
    secondary = Color(0xFF9B5A3A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF1E2D7),
    onSecondaryContainer = Color(0xFF33180A),
    tertiary = Color(0xFF56736A),
    background = Color(0xFFF3F4ED),
    onBackground = Color(0xFF18221A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF18221A),
    surfaceVariant = Color(0xFFE5EAE0),
    onSurfaceVariant = Color(0xFF526054),
    outline = Color(0xFFC3CCC0),
    outlineVariant = Color(0xFFE0E5DB),
)

private val PureInkScheme = darkColorScheme(
    primary = Color(0xFFE4EBE6),
    onPrimary = Color(0xFF0A0D0B),
    primaryContainer = Color(0xFF1C211E),
    onPrimaryContainer = Color(0xFFE4EBE6),
    secondary = Color(0xFF8FE3C8),
    onSecondary = Color(0xFF072118),
    secondaryContainer = Color(0xFF14352B),
    onSecondaryContainer = Color(0xFFBDF0E0),
    tertiary = SkyTint,
    background = Color(0xFF050607),
    onBackground = Color(0xFFECEFEA),
    surface = Color(0xFF0E1113),
    onSurface = Color(0xFFECEFEA),
    surfaceVariant = Color(0xFF181D20),
    onSurfaceVariant = Color(0xFF9BA4A0),
    outline = Color(0xFF2B3136),
    outlineVariant = Color(0xFF1A1F23),
)

private val CalmSageDarkScheme = darkColorScheme(
    primary = SageMint,
    onPrimary = Color(0xFF092313),
    primaryContainer = Color(0xFF1F3C28),
    onPrimaryContainer = Color(0xFFC5ECD0),
    secondary = Color(0xFFDFC89D),
    onSecondary = Color(0xFF261E09),
    secondaryContainer = Color(0xFF3C321C),
    onSecondaryContainer = Color(0xFFF7E7C8),
    tertiary = Color(0xFF9ECBD3),
    background = Color(0xFF0C140F),
    onBackground = Color(0xFFE8EFEA),
    surface = Color(0xFF16211A),
    onSurface = Color(0xFFE8EFEA),
    surfaceVariant = Color(0xFF213026),
    onSurfaceVariant = Color(0xFFA7BBAE),
    outline = Color(0xFF35483C),
    outlineVariant = Color(0xFF202D24),
)

private val TwilightScheme = darkColorScheme(
    primary = SkyTint,
    onPrimary = Color(0xFF0B1C3D),
    primaryContainer = Color(0xFF22345E),
    onPrimaryContainer = Color(0xFFD8E4FF),
    secondary = Color(0xFFDFB7F2),
    onSecondary = Color(0xFF2A1438),
    secondaryContainer = Color(0xFF3E2452),
    onSecondaryContainer = Color(0xFFF3DBFF),
    tertiary = Color(0xFF8FE3C8),
    background = Color(0xFF0B101C),
    onBackground = Color(0xFFEAEFFD),
    surface = Color(0xFF161F33),
    onSurface = Color(0xFFEAEFFD),
    surfaceVariant = Color(0xFF222E4A),
    onSurfaceVariant = Color(0xFFABB7D4),
    outline = Color(0xFF354263),
    outlineVariant = Color(0xFF222B42),
)

private val EspressoScheme = darkColorScheme(
    primary = WarmTerra,
    onPrimary = Color(0xFF2B1306),
    primaryContainer = Color(0xFF5A2B14),
    onPrimaryContainer = Color(0xFFFCE0CE),
    secondary = Color(0xFFB6C472),
    onSecondary = Color(0xFF202506),
    secondaryContainer = Color(0xFF3E421A),
    onSecondaryContainer = Color(0xFFE1EAAB),
    tertiary = Color(0xFFD8B8A8),
    background = Color(0xFF120D0B),
    onBackground = Color(0xFFF4E9DF),
    surface = Color(0xFF211713),
    onSurface = Color(0xFFF4E9DF),
    surfaceVariant = Color(0xFF33241D),
    onSurfaceVariant = Color(0xFFCBB8AC),
    outline = Color(0xFF4A372D),
    outlineVariant = Color(0xFF2C201A),
)

private val MinimalTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 48.sp,
        lineHeight = 52.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = (-1.1).sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.5).sp,
    ),
    headlineSmall = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.3).sp,
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Medium,
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
    ),
    bodyMedium = TextStyle(
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.4.sp,
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
    ),
)

@Composable
fun MinimalBroTheme(
    wallpaper: Wallpaper = Wallpaper.PAPER,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val (colorScheme, isLight) = resolveColorScheme(
        wallpaper = wallpaper,
        darkTheme = darkTheme,
        dynamicProvider = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                runCatching {
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                }.getOrNull()
            } else {
                null
            }
        },
    )

    val palette = MinimalPalette(
        calmAccent = colorScheme.primary,
        subtleBorder = colorScheme.onSurface.copy(alpha = if (isLight) 0.09f else 0.08f),
        ringTrack = colorScheme.onSurface.copy(alpha = if (isLight) 0.12f else 0.10f),
        isLightSurface = isLight,
    )

    CompositionLocalProvider(LocalMinimalPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MinimalTypography,
            content = content,
        )
    }
}

private fun resolveColorScheme(
    wallpaper: Wallpaper,
    darkTheme: Boolean,
    dynamicProvider: () -> ColorScheme?,
): Pair<ColorScheme, Boolean> {
    return when (wallpaper) {
        Wallpaper.PAPER -> WarmPaperLightScheme to true
        Wallpaper.AMOLED -> PureInkScheme to false
        Wallpaper.DYNAMIC -> {
            val dynamic = dynamicProvider()
            if (dynamic != null) {
                dynamic to !darkTheme
            } else if (darkTheme) {
                CalmSageDarkScheme to false
            } else {
                WarmPaperLightScheme to true
            }
        }
        Wallpaper.FOREST -> CalmSageDarkScheme to false
        Wallpaper.DUSK -> TwilightScheme to false
        Wallpaper.NIGHT -> EspressoScheme to false
    }
}

fun wallpaperBrush(wallpaper: Wallpaper, darkTheme: Boolean = true): Brush = when (wallpaper) {
    Wallpaper.PAPER -> Brush.verticalGradient(listOf(Color(0xFFF3F4ED), Color(0xFFEBEFE5)))
    Wallpaper.AMOLED -> Brush.verticalGradient(listOf(Color(0xFF050607), Color(0xFF0A0C0E)))
    Wallpaper.DYNAMIC -> if (darkTheme) {
        Brush.verticalGradient(listOf(Color(0xFF0C140F), Color(0xFF141E18)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFF3F4ED), Color(0xFFE7EDE4)))
    }
    Wallpaper.FOREST -> Brush.verticalGradient(listOf(Color(0xFF0B130E), Color(0xFF16251C)))
    Wallpaper.DUSK -> Brush.verticalGradient(listOf(Color(0xFF0A0E19), Color(0xFF162038)))
    Wallpaper.NIGHT -> Brush.verticalGradient(listOf(Color(0xFF110C0A), Color(0xFF231712)))
}

fun wallpaperSwatch(wallpaper: Wallpaper): Brush = when (wallpaper) {
    Wallpaper.PAPER -> Brush.linearGradient(listOf(Color(0xFFF3F4ED), Color(0xFF436B4D)))
    Wallpaper.AMOLED -> Brush.linearGradient(listOf(Color(0xFF050607), Color(0xFFE4EBE6)))
    Wallpaper.DYNAMIC -> Brush.linearGradient(listOf(Color(0xFF8FE3C8), Color(0xFFA8C1FA)))
    Wallpaper.FOREST -> Brush.linearGradient(listOf(Color(0xFF17221B), Color(0xFF9AD0A8)))
    Wallpaper.DUSK -> Brush.linearGradient(listOf(Color(0xFF171F33), Color(0xFFA8C1FA)))
    Wallpaper.NIGHT -> Brush.linearGradient(listOf(Color(0xFF211713), Color(0xFFF0AE84)))
}
