package com.andoni.signosdaily.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.andoni.signosdaily.data.ThemeMode

// Same palette as Phrasal Daily with teal and indigo swapped, so both apps look like a family.
val Teal = Color(0xFF0D9488)
val Orange = Color(0xFFF97316)
val Indigo = Color(0xFF4F46E5)
val Flame = Color(0xFFFF7A1A)

private val Light = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCF2EC),
    onPrimaryContainer = Color(0xFF00332D),
    secondary = Orange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE3D0),
    onSecondaryContainer = Color(0xFF4A1A00),
    tertiary = Indigo,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0E0FF),
    onTertiaryContainer = Color(0xFF14105E),
    background = Color(0xFFF4FAF8),
    onBackground = Color(0xFF171D1C),
    surface = Color(0xFFF4FAF8),
    onSurface = Color(0xFF171D1C),
    surfaceVariant = Color(0xFFDAE5E1),
    onSurfaceVariant = Color(0xFF3F4946),
    surfaceContainer = Color(0xFFE8F0ED),
    surfaceContainerHigh = Color(0xFFE2EBE8),
    surfaceContainerLow = Color(0xFFEEF5F3),
    outline = Color(0xFF6F7976),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF7DD8CB),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFFA6F4E6),
    secondary = Color(0xFFFFB68C),
    onSecondary = Color(0xFF552100),
    secondaryContainer = Color(0xFF7A3300),
    onSecondaryContainer = Color(0xFFFFDBC8),
    tertiary = Color(0xFFC0C1FF),
    onTertiary = Color(0xFF221C8F),
    tertiaryContainer = Color(0xFF3A33B8),
    onTertiaryContainer = Color(0xFFE0E0FF),
    background = Color(0xFF0F1514),
    onBackground = Color(0xFFDEE4E1),
    surface = Color(0xFF0F1514),
    onSurface = Color(0xFFDEE4E1),
    surfaceVariant = Color(0xFF3F4946),
    onSurfaceVariant = Color(0xFFBEC9C5),
    surfaceContainer = Color(0xFF1B2120),
    surfaceContainerHigh = Color(0xFF252B2A),
    surfaceContainerLow = Color(0xFF171D1C),
    outline = Color(0xFF899390),
)

private val AppTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun SignosTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isDark(mode)) Dark else Light,
        typography = AppTypography,
        content = content,
    )
}
