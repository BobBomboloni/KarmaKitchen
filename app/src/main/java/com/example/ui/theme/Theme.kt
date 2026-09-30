package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.Black,
    primaryContainer = PrimaryGreenLight,
    onPrimaryContainer = Color.White,
    secondary = MealsTextPrimary,
    onSecondary = Color.Black,
    secondaryContainer = MealsCardBg,
    onSecondaryContainer = MealsTextPrimary,
    tertiary = PrimaryGreen,
    background = BackgroundColor,
    onBackground = TextPrimary,
    surface = SurfaceColor,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantColor,
    onSurfaceVariant = TextSecondary,
    outline = OutlineColor
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = DarkColorScheme, typography = Typography, content = content)
}

