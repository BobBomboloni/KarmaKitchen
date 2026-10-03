# KarmaKitchen

AI-powered surplus food redistribution (SDG 2, 12, 13). Android app built with Kotlin and Jetpack Compose.

## What works
- Donor flow: photograph food, Gemini checks safety and quality, donor reviews and submits.
- NGO flow: intake verification scan, inventory and deliveries (demo data).
- Karma Points, tiers and the Karma Store (local state, demo vouchers).

## Not built yet
Auto-matching, collector role, live delivery tracking, Smile Wall, raw-material requests, shared backend.

## Setup
1. Copy `.env.example` to `.env` and set `GEMINI_API_KEY` and `MAPS_API_KEY`.
2. Open in Android Studio and run on a device or emulator (minSdk 24).

Note: the Gemini key is currently packaged in the APK. Before a public release, move AI calls behind Firebase AI Logic with App Check or a backend proxy, and restrict the Maps key to this app's package and signing SHA-1.
