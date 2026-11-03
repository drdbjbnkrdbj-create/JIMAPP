package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val JimDarkColorScheme = darkColorScheme(
    primary = JimPrimaryBlueLight,
    onPrimary = Color.White,
    primaryContainer = JimNavyElevated,
    onPrimaryContainer = Color.White,
    secondary = JimSkyAccent,
    onSecondary = JimNavyDark,
    background = JimNavyDark,
    onBackground = JimDarkTextPrimary,
    surface = JimNavySurface,
    onSurface = JimDarkTextPrimary,
    surfaceVariant = JimNavyCard,
    onSurfaceVariant = JimDarkTextSecondary,
    outline = JimNavyBorder,
    outlineVariant = Color(0xFF1E3252),
    error = ErrorRed,
    onError = Color.White
)

val JimLightColorScheme = lightColorScheme(
    primary = JimPrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEFF6FF),
    onPrimaryContainer = JimPrimaryBlue,
    secondary = JimPrimaryBlueLight,
    onSecondary = Color.White,
    background = JimLightBg,
    onBackground = JimLightTextPrimary,
    surface = JimLightSurface,
    onSurface = JimLightTextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = JimLightTextSecondary,
    outline = JimLightBorder,
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    onError = Color.White
)

@Composable
fun JimTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) JimDarkColorScheme else JimLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
