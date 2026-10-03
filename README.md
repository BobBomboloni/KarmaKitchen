# KarmaKitchen

AI-powered surplus food redistribution (SDG 2, 12, 13). Android app built with Kotlin and Jetpack Compose.

## What works
- Donor flow: photograph food, Gemini checks safety and quality, donor reviews and submits.
- NGO flow: intake verification scan, inventory and deliveries (demo data).
- Karma Points, tiers and the Karma Store (local state, demo vouchers).
- Smile Wall: an NGO opens a received donation, takes or picks a photo of the people who enjoyed the food (with a consent check) and sends it to the donor, who sees it on their Smile Wall. Photos are stored privately on the phone, so for now both roles share one device. Two labelled example photos are added on first launch so the wall is not empty.

## Not built yet
Auto-matching, collector role, live delivery tracking, raw-material requests, and a shared backend (so smiles can reach the donor's own phone).

## Setup
1. Copy `.env.example` to `.env` and set `GEMINI_API_KEY` and `MAPS_API_KEY`.
2. Open in Android Studio and run on a device or emulator (minSdk 24).

Note: the Gemini key is currently packaged in the APK. Before a public release, move AI calls behind Firebase AI Logic with App Check or a backend proxy, and restrict the Maps key to this app's package and signing SHA-1.

## Brand logos
The Karma Store shows partner logos on brand-coloured tiles. The bundled McDonald's, Swiggy, Spotify, Samsung, Puma and Nike marks come from [Simple Icons](https://simpleicons.org) (CC0). All brand names and logos are trademarks of their owners and are shown only to illustrate the rewards store in this prototype; a real release would need each partner's permission.

To add or replace a logo, put a file named `logo_<brand>` (for example `logo_nike.png`) in `app/src/main/res/drawable` and match the `logoName` of the reward in `MainActivity.kt`. A reward without a logo file shows its name on a tile.

## Design notes
The interface avoids the common "template" look: one typeface in real weights, sentence-case labels instead of small capitals, plain-language copy, tonal cards without borders, a single corner radius, and no decorative icon badges. Numbers use tabular figures rather than a monospace font.

Typeface: [Plus Jakarta Sans](https://github.com/tokotype/PlusJakartaSans) (SIL Open Font License, see `licenses/`).
