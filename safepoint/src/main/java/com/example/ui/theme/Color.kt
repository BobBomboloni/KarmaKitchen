package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// -----------------------------------------------------------------------------
// KarmaKitchen palette
//
// There are two palettes: a warm light one (the default) and a green-tinted dark one. The names
// below (TextPrimary, SurfaceColor, PrimaryGreen ...) are read from whichever palette the theme
// provides, so screens never mention light or dark. Pairs that are used together (for example
// PrimaryGreen with OnPrimaryGreen) keep at least 4.5:1 contrast in both palettes; outlines used
// for input fields keep 3:1.
//
// Use the semantic tokens (Success/Warning/Danger/Info) for status, never raw hex.
// -----------------------------------------------------------------------------

@Immutable
class AppPalette(
    val isDark: Boolean,
    // Screens, cards and tiles
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceHigh: Color,
    val outline: Color,
    val outlineStrong: Color,
    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    // Brand green (buttons, links, success) and its container
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    // Mango amber: `amber` fills buttons and bars, `amberText` is the readable version for text
    val amber: Color,
    val onAmber: Color,
    val amberText: Color,
    val mealsBg: Color,
    val mealsText: Color,
    // Status
    val danger: Color,
    val onDanger: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val nonVeg: Color,
    val nonVegContainer: Color,
    val gold: Color,
    val karmaCardBg: Color
)

/** Green-tinted dark palette. */
val DarkPalette = AppPalette(
    isDark = true,
    background = Color(0xFF0E1410),
    surface = Color(0xFF1A231D),
    surfaceVariant = Color(0xFF26332B),
    surfaceHigh = Color(0xFF222D26),
    outline = Color(0xFF2E3B33),
    outlineStrong = Color(0xFF66776B),
    textPrimary = Color(0xFFEEF3EE),
    textSecondary = Color(0xFFAEBBB1),
    textTertiary = Color(0xFF93A298),
    primary = Color(0xFF7DC87A),
    onPrimary = Color(0xFF0A2A10),
    primaryContainer = Color(0xFF1F4A2A),
    onPrimaryContainer = Color(0xFFC9F0CB),
    amber = Color(0xFFFFB347),
    onAmber = Color(0xFF3B2300),
    amberText = Color(0xFFFFB347),
    mealsBg = Color(0xFF3D2A10),
    mealsText = Color(0xFFFFC066),
    danger = Color(0xFFFF6F66),
    onDanger = Color(0xFF3B0907),
    dangerContainer = Color(0xFF3D1A18),
    onDangerContainer = Color(0xFFFFD6D2),
    info = Color(0xFF7EC8F0),
    infoContainer = Color(0xFF17384A),
    onInfoContainer = Color(0xFFCFEBFA),
    nonVeg = Color(0xFFFF8A65),
    nonVegContainer = Color(0xFF3B2218),
    gold = Color(0xFFF2C14E),
    karmaCardBg = Color(0xFF183524)
)

/** Warm paper background, white cards, leaf green and turmeric. The default. */
val LightPalette = AppPalette(
    isDark = false,
    background = Color(0xFFF6F0E3),
    surface = Color(0xFFFFFDF8),
    surfaceVariant = Color(0xFFEEE6D4),
    surfaceHigh = Color(0xFFFFFFFF),
    outline = Color(0xFFE2D8C3),
    outlineStrong = Color(0xFF85796A),
    textPrimary = Color(0xFF2A2118),
    textSecondary = Color(0xFF5A5042),
    textTertiary = Color(0xFF716656),
    primary = Color(0xFF1A6B35),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCEBD3),
    onPrimaryContainer = Color(0xFF15502A),
    amber = Color(0xFFF2A20F),
    onAmber = Color(0xFF3A2200),
    amberText = Color(0xFF885000),
    mealsBg = Color(0xFFFCEBC6),
    mealsText = Color(0xFF744400),
    danger = Color(0xFFBF2F24),
    onDanger = Color(0xFFFFFFFF),
    dangerContainer = Color(0xFFFBE3DF),
    onDangerContainer = Color(0xFF7A1C13),
    info = Color(0xFF1B6CA8),
    infoContainer = Color(0xFFDDEBF6),
    onInfoContainer = Color(0xFF0F3A5F),
    nonVeg = Color(0xFFB8420F),
    nonVegContainer = Color(0xFFFCE6DA),
    gold = Color(0xFFD99A00),
    karmaCardBg = Color(0xFFE3F0DC)
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

// Brand
val PrimaryGreen: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.primary          // main accent, buttons, success
val OnPrimaryGreen: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.onPrimary      // text/icons placed on PrimaryGreen
val PrimaryGreenLight: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.primaryContainer  // chips, tags, nav indicator
val OnPrimaryGreenContainer: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.onPrimaryContainer
val SecondaryAmber: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.amber          // fills: buttons, bars, switches
val OnSecondaryAmber: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.onAmber
val AmberText: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.amberText           // amber used as text or a small icon

// Neutrals
val BackgroundColor: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.background    // screens
val SurfaceColor: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surface          // cards
val SurfaceVariantColor: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surfaceVariant  // tiles inside cards, chat bubbles
val SurfaceHighColor: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surfaceHigh  // dialogs, menus
val OutlineColor: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.outline          // subtle card borders and dividers
val OutlineStrong: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.outlineStrong    // input borders (3:1 on surfaces)

// Text
val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.textPrimary
val TextSecondary: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.textSecondary
val TextTertiary: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.textTertiary     // hints and captions

// Feature cards on the donor dashboard
val KarmaCardBg: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.karmaCardBg
val MealsCardBg: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.mealsBg
val MealsTextPrimary: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.mealsText

// Status colors
val SuccessColor: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.primary
val WarningColor: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.amber
val DangerColor: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.danger
val OnDanger: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.onDanger
val DangerContainer: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.dangerContainer
val OnDangerContainer: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.onDangerContainer
val InfoColor: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.info
val InfoContainer: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.infoContainer
val OnInfoContainer: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.onInfoContainer
val NonVegColor: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.nonVeg
val NonVegContainer: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.nonVegContainer
val AccentGold: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.gold              // tips, highlights, torch
val AccentCoral = Color(0xFFFF7468)                                                                   // tint behind the "smiles" illustration

// Impact tiers (silver and platinum are clearly different: neutral vs icy blue)
val TierBronze = Color(0xFFD4915A)
val TierSilver = Color(0xFFB7C0C9)
val TierGold = Color(0xFFF2C14E)
val TierPlatinum = Color(0xFF8FD3E8)

/** Dark text for the badge that sits on a tier colour. */
val OnTierColor = Color(0xFF1F1A12)

/**
 * A bright accent colour (a tier, an amber or coral highlight) used as text: unchanged on the dark
 * palette, darkened on the light one so it stays readable.
 */
@Composable
@ReadOnlyComposable
fun readableInk(accent: Color): Color =
    if (LocalPalette.current.isDark) accent else androidx.compose.ui.graphics.lerp(accent, Color.Black, 0.5f)

// Karma Store brand tiles: each partner's own brand colors behind its logo.
val BrandMcDonaldsRed = Color(0xFFDA291C)
val BrandMcDonaldsGold = Color(0xFFFFC72C)
val BrandSamsungBlue = Color(0xFF1428A0)
val BrandSwiggyOrange = Color(0xFFFC8019)
val BrandSpotifyBlack = Color(0xFF191414)
val BrandSpotifyGreen = Color(0xFF1DB954)
val BrandZomatoRed = Color(0xFFE23744)
val BrandStarbucksGreen = Color(0xFF006241)
val BrandNetflixBlack = Color(0xFF141414)
val BrandNetflixRed = Color(0xFFE50914)
val BrandWhite = Color(0xFFFFFFFF)
val BrandBlack = Color(0xFF000000)
