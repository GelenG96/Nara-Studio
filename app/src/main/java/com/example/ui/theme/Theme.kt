package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val NaraDarkColorScheme = darkColorScheme(
    primary = WarmAmberPrimary,
    onPrimary = Color(0xFF391404),
    primaryContainer = WarmAmberContainer,
    onPrimaryContainer = Color(0xFFFFDBD0),
    secondary = SkySlateSecondary,
    onSecondary = Color(0xFF0E2034),
    secondaryContainer = SkySlateContainer,
    onSecondaryContainer = Color(0xFFD6E3F0),
    tertiary = MistSageTertiary,
    onTertiary = Color(0xFF0D261C),
    tertiaryContainer = MistSageContainer,
    onTertiaryContainer = Color(0xFFD5E8DE),
    background = MidnightBackground,
    onBackground = IvoryInk,
    surface = CharcoalSurface,
    onSurface = IvoryInk,
    surfaceVariant = CharcoalElevated,
    onSurfaceVariant = MutedAsh,
    outline = HairlineDark,
    outlineVariant = HairlineDark.copy(alpha = 0.6f),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val NaraLightColorScheme = lightColorScheme(
    primary = TerracottaPrimary,
    onPrimary = Color.White,
    primaryContainer = TerracottaContainer,
    onPrimaryContainer = Color(0xFF3E0F00),
    secondary = DeepSlateSecondary,
    onSecondary = Color.White,
    secondaryContainer = SlateContainer,
    onSecondaryContainer = Color(0xFF0E1D2D),
    tertiary = SageTertiary,
    onTertiary = Color.White,
    tertiaryContainer = SageContainer,
    onTertiaryContainer = Color(0xFF0F261C),
    background = ParchmentBackground,
    onBackground = CharcoalInk,
    surface = AlabasterSurface,
    onSurface = CharcoalInk,
    surfaceVariant = WarmSandContainer,
    onSurfaceVariant = MutedSepia,
    outline = HairlineLight,
    outlineVariant = HairlineLight.copy(alpha = 0.6f),
    error = ErrorCrimson,
    onError = Color.White
)

val NaraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

data class NaraPanelStyle(
    val panelOpacity: Float = 0.92f,
    val hasActiveWallpaper: Boolean = false
)

val LocalNaraPanelStyle = staticCompositionLocalOf { NaraPanelStyle() }

@Composable
fun NaraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    panelOpacity: Float = 0.92f,
    hasActiveWallpaper: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> NaraDarkColorScheme
        else -> NaraLightColorScheme
    }

    CompositionLocalProvider(
        LocalNaraPanelStyle provides NaraPanelStyle(
            panelOpacity = panelOpacity,
            hasActiveWallpaper = hasActiveWallpaper
        )
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NaraTypography,
            shapes = NaraShapes,
            content = content
        )
    }
}
