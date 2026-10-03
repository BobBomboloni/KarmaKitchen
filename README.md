# KarmaKitchen

AI-powered surplus food redistribution (SDG 2, 12, 13). Android app built with Kotlin and Jetpack Compose.

## What works
- Donor flow: photograph food, Gemini checks safety and quality, donor reviews and submits.
- NGO flow: intake verification scan, inventory and deliveries (demo data).
- Karma Points, tiers and the Karma Store (local state, demo vouchers).
- Smile Wall: an NGO opens a received donation, takes or picks a photo of the people who enjoyed the food (with a consent check) and sends it to the donor, who sees it on their Smile Wall. Photos are stored privately on the phone, so for now both roles share one device.

## Not built yet
Auto-matching, collector role, live delivery tracking, raw-material requests, and a shared backend (so smiles can reach the donor's own phone).

## Setup
1. Copy `.env.example` to `.env` and set `GEMINI_API_KEY` and `MAPS_API_KEY`.
2. Open in Android Studio and run on a device or emulator (minSdk 24).

Note: the Gemini key is currently packaged in the APK. Before a public release, move AI calls behind Firebase AI Logic with App Check or a backend proxy, and restrict the Maps key to this app's package and signing SHA-1.
