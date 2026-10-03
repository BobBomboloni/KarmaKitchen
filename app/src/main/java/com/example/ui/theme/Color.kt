package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// -----------------------------------------------------------------------------
// KarmaKitchen palette
//
// Leaf green and mango amber come straight from the logo. They sit on deep,
// green-tinted neutrals so the whole app feels like one family instead of a mix
// of grey, iOS green and Material green. Every text/background pair below was
// checked against WCAG AA (4.5:1); outlines used for input fields are 3:1+.
//
// Use the semantic tokens (Success/Warning/Danger/Info) for status, never raw hex.
// -----------------------------------------------------------------------------

// Brand
val PrimaryGreen = Color(0xFF7DC87A)          // main accent, buttons, success
val OnPrimaryGreen = Color(0xFF0A2A10)        // text/icons placed on PrimaryGreen
val PrimaryGreenLight = Color(0xFF1F4A2A)     // green container (chips, tags, nav indicator)
val OnPrimaryGreenContainer = Color(0xFFC9F0CB)
val SecondaryAmber = Color(0xFFFFB347)        // meals, urgency, NGO accent
val OnSecondaryAmber = Color(0xFF3B2300)

// Neutrals (green-tinted dark)
val BackgroundColor = Color(0xFF0E1410)       // screens
val SurfaceColor = Color(0xFF161D18)          // cards
val SurfaceVariantColor = Color(0xFF202A23)   // tiles inside cards, chat bubbles
val SurfaceHighColor = Color(0xFF1C241E)      // dialogs, menus
val OutlineColor = Color(0xFF2E3B33)          // subtle card borders and dividers
val OutlineStrong = Color(0xFF66776B)         // input borders (3:1 on surfaces)

// Text
val TextPrimary = Color(0xFFEEF3EE)
val TextSecondary = Color(0xFFAEBBB1)
val TextTertiary = Color(0xFF8A988E)          // hints and captions

// Feature cards on the donor dashboard
val KarmaCardBg = Color(0xFF183524)
val MealsCardBg = Color(0xFF3D2A10)
val MealsTextPrimary = Color(0xFFFFC066)

// Status colors
val SuccessColor = PrimaryGreen
val WarningColor = SecondaryAmber
val DangerColor = Color(0xFFFF6F66)
val OnDanger = Color(0xFF3B0907)
val DangerContainer = Color(0xFF3D1A18)
val OnDangerContainer = Color(0xFFFFD6D2)
val InfoColor = Color(0xFF7EC8F0)
val InfoContainer = Color(0xFF17384A)
val OnInfoContainer = Color(0xFFCFEBFA)
val NonVegColor = Color(0xFFFF8A65)
val NonVegContainer = Color(0xFF3B2218)
val AccentGold = Color(0xFFF2C14E)            // tips, highlights, torch

// Impact tiers (silver and platinum are clearly different: neutral vs icy blue)
val TierBronze = Color(0xFFD4915A)
val TierSilver = Color(0xFFB7C0C9)
val TierGold = Color(0xFFF2C14E)
val TierPlatinum = Color(0xFF8FD3E8)

// Reward partner tints. Brand colors that vanish on a dark card (Samsung blue,
// Puma/Nike black) are lightened so the icons stay visible.
val RewardMcDonalds = Color(0xFFFFC72C)
val RewardAmazon = Color(0xFFFF9900)
val RewardFlipkart = Color(0xFF5B9BFF)
val RewardSamsung = Color(0xFF6C8CFF)
val RewardSport = Color(0xFFE8EDE8)

// The logo animation video has a pure black backdrop, so the role-selection
// screen must stay true black to blend with it.
val VideoBackdrop = Color(0xFF000000)
