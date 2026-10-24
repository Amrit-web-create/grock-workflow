package com.customapplock.studio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

object Palette {
    // Monochrome palette. Names kept so every lock design shares one accent system.
    val Sapphire = Color(0xFF000000)
    val SapphireLight = Color(0xFF171717)
    val Cyan = Color(0xFFFFFFFF)
    val Purple = Color(0xFF9CA3AF)
    val Emerald = Color(0xFFD4D4D4)
    val Amber = Color(0xFF737373)
    val Rose = Color(0xFFEF4444)
    val Blue = Color(0xFF525252)
}

private val DarkScheme = darkColorScheme(
    primary = Color.White, onPrimary = Color.Black,
    primaryContainer = Color(0xFF262626), onPrimaryContainer = Color.White,
    secondary = Color(0xFFA3A3A3), tertiary = Color(0xFFD4D4D4),
    background = Color.Black, onBackground = Color.White,
    surface = Color.Black, onSurface = Color.White,
    surfaceVariant = Color(0xFF171717), onSurfaceVariant = Color(0xFFA3A3A3),
    outline = Color(0xFF404040), error = Palette.Rose,
    surfaceContainerLowest = Color(0xFF000000), surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF111111), surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF262626)
)

private val LightScheme = lightColorScheme(
    primary = Color.Black, onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E5E5), onPrimaryContainer = Color.Black,
    secondary = Color(0xFF525252), tertiary = Color(0xFF404040),
    background = Color.White, onBackground = Color.Black,
    surface = Color.White, onSurface = Color.Black,
    surfaceVariant = Color(0xFFF2F2F2), onSurfaceVariant = Color(0xFF525252),
    outline = Color(0xFFD4D4D4), error = Color(0xFFDC2626),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF7F7F7),
    surfaceContainer = Color(0xFFF2F2F2), surfaceContainerHigh = Color(0xFFEDEDED),
    surfaceContainerHighest = Color(0xFFE5E5E5)
)

private val AppTypography = Typography().let {
    it.copy(
        headlineMedium = it.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
        titleLarge = it.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = it.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    )
}

@Composable
fun AppLockTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = AppTypography,
        content = content
    )
}
