package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Every role is set explicitly. Anything left out falls back to Material's
// purple-tinted defaults, which used to leak into the nav bar, dialogs, menus
// and chips.
private val DarkColorScheme =
  darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = OnPrimaryGreen,
    primaryContainer = PrimaryGreenLight,
    onPrimaryContainer = OnPrimaryGreenContainer,
    inversePrimary = Color(0xFF2E6B38),
    secondary = SecondaryAmber,
    onSecondary = OnSecondaryAmber,
    secondaryContainer = MealsCardBg,
    onSecondaryContainer = MealsTextPrimary,
    tertiary = InfoColor,
    onTertiary = Color(0xFF062A3B),
    tertiaryContainer = InfoContainer,
    onTertiaryContainer = OnInfoContainer,
    error = DangerColor,
    onError = OnDanger,
    errorContainer = DangerContainer,
    onErrorContainer = OnDangerContainer,
    background = BackgroundColor,
    onBackground = TextPrimary,
    surface = SurfaceColor,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantColor,
    onSurfaceVariant = TextSecondary,
    inverseSurface = Color(0xFFDDE5DE),
    inverseOnSurface = Color(0xFF1A211C),
    outline = OutlineStrong,
    outlineVariant = OutlineColor,
    scrim = Color.Black,
    surfaceDim = BackgroundColor,
    surfaceBright = Color(0xFF2D3A31),
    surfaceContainerLowest = Color(0xFF0B100D),
    surfaceContainerLow = Color(0xFF121915),
    surfaceContainer = SurfaceColor,
    surfaceContainerHigh = SurfaceHighColor,
    surfaceContainerHighest = SurfaceVariantColor
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = DarkColorScheme, typography = Typography, content = content)
}
