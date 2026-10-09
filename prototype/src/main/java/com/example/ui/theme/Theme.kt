package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.example.ThemeSettings

// Every role is set explicitly. Anything left out falls back to Material's
// purple-tinted defaults, which used to leak into the nav bar, dialogs, menus
// and chips.
private val DarkColorScheme =
  darkColorScheme(
    primary = DarkPalette.primary,
    onPrimary = DarkPalette.onPrimary,
    primaryContainer = DarkPalette.primaryContainer,
    onPrimaryContainer = DarkPalette.onPrimaryContainer,
    inversePrimary = Color(0xFF2E6B38),
    secondary = DarkPalette.amber,
    onSecondary = DarkPalette.onAmber,
    secondaryContainer = DarkPalette.mealsBg,
    onSecondaryContainer = DarkPalette.mealsText,
    tertiary = DarkPalette.info,
    onTertiary = Color(0xFF062A3B),
    tertiaryContainer = DarkPalette.infoContainer,
    onTertiaryContainer = DarkPalette.onInfoContainer,
    error = DarkPalette.danger,
    onError = DarkPalette.onDanger,
    errorContainer = DarkPalette.dangerContainer,
    onErrorContainer = DarkPalette.onDangerContainer,
    background = DarkPalette.background,
    onBackground = DarkPalette.textPrimary,
    surface = DarkPalette.surface,
    onSurface = DarkPalette.textPrimary,
    surfaceVariant = DarkPalette.surfaceVariant,
    onSurfaceVariant = DarkPalette.textSecondary,
    inverseSurface = Color(0xFFDDE5DE),
    inverseOnSurface = Color(0xFF1A211C),
    outline = DarkPalette.outlineStrong,
    outlineVariant = DarkPalette.outline,
    scrim = Color.Black,
    surfaceDim = DarkPalette.background,
    surfaceBright = Color(0xFF2D3A31),
    surfaceContainerLowest = Color(0xFF0B100D),
    surfaceContainerLow = Color(0xFF121915),
    surfaceContainer = DarkPalette.surface,
    surfaceContainerHigh = DarkPalette.surfaceHigh,
    surfaceContainerHighest = DarkPalette.surfaceVariant
  )

private val LightColorScheme =
  lightColorScheme(
    primary = LightPalette.primary,
    onPrimary = LightPalette.onPrimary,
    primaryContainer = LightPalette.primaryContainer,
    onPrimaryContainer = LightPalette.onPrimaryContainer,
    inversePrimary = Color(0xFF7DC87A),
    secondary = LightPalette.amber,
    onSecondary = LightPalette.onAmber,
    secondaryContainer = LightPalette.mealsBg,
    onSecondaryContainer = LightPalette.mealsText,
    tertiary = LightPalette.info,
    onTertiary = Color.White,
    tertiaryContainer = LightPalette.infoContainer,
    onTertiaryContainer = LightPalette.onInfoContainer,
    error = LightPalette.danger,
    onError = LightPalette.onDanger,
    errorContainer = LightPalette.dangerContainer,
    onErrorContainer = LightPalette.onDangerContainer,
    background = LightPalette.background,
    onBackground = LightPalette.textPrimary,
    surface = LightPalette.surface,
    onSurface = LightPalette.textPrimary,
    surfaceVariant = LightPalette.surfaceVariant,
    onSurfaceVariant = LightPalette.textSecondary,
    inverseSurface = Color(0xFF2A2118),
    inverseOnSurface = Color(0xFFF6F0E3),
    outline = LightPalette.outlineStrong,
    outlineVariant = LightPalette.outline,
    scrim = Color.Black,
    surfaceDim = Color(0xFFE9E1CF),
    surfaceBright = LightPalette.surface,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBF6EA),
    surfaceContainer = LightPalette.surface,
    surfaceContainerHigh = LightPalette.surfaceHigh,
    surfaceContainerHighest = LightPalette.surfaceVariant
  )

fun colorSchemeFor(palette: AppPalette): ColorScheme = if (palette.isDark) DarkColorScheme else LightColorScheme

/**
 * The app theme. Light is the default; [ThemeSettings.dark] (saved, and switched in Profile)
 * turns on the dark palette. A screen can force one palette by wrapping itself in
 * `MyApplicationTheme(darkTheme = true)`.
 */
@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = ThemeSettings.dark,
  content: @Composable () -> Unit,
) {
  val palette = if (darkTheme) DarkPalette else LightPalette
  CompositionLocalProvider(LocalPalette provides palette) {
    MaterialTheme(colorScheme = colorSchemeFor(palette), typography = Typography, content = content)
  }
}
