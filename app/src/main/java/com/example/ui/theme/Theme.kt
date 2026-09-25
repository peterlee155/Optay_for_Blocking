package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF134E4A),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = IndigoSecondary,
    onSecondary = Color(0xFF1E1B4B),
    secondaryContainer = Color(0xFF312E81),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = EmeraldSuccess,
    onTertiary = Color(0xFF022C22),
    background = OptayDarkBackground,
    onBackground = TextPrimaryDark,
    surface = OptayDarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = OptayDarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = OptayDarkBorder,
    error = CoralBlocked,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = CyanPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFFAFE),
    onPrimaryContainer = Color(0xFF164E63),
    secondary = IndigoSecondaryDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E7FF),
    onSecondaryContainer = Color(0xFF312E81),
    tertiary = EmeraldSuccess,
    onTertiary = Color.White,
    background = OptayLightBackground,
    onBackground = TextPrimaryLight,
    surface = OptayLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = OptayLightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = OptayLightBorder,
    error = CoralBlocked,
    onError = Color.White
)

@Composable
fun OptayTheme(
    darkTheme: Boolean = true, // Default to sleek calm dark theme for Optay
    dynamicColor: Boolean = false, // Keep brand identity intentional
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = OptayTheme(darkTheme, dynamicColor, content)
