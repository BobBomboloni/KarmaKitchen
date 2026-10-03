# KarmaKitchen

AI-powered surplus food redistribution (SDG 2, 12, 13). Android app built with Kotlin and Jetpack Compose.

## What works
- Donor flow: a three-step Donate screen (photo and AI check, details with pickup time and a note, pickup address and coins summary). Gemini checks safety and quality; a submitted donation then shows up on the home screen tracker.
- Receiver (NGO) side with its own bottom bar: Home (availability, storage level, urgent-need broadcast that shows up on the donor home, attention list, weekly meals), Offers (accept or decline, track the volunteer, log intake with the camera and AI, with a manual fallback), Stock (expiry bars, hand out or dispose) and Smiles (send photos to donors). Because both roles run on one phone, a donation posted from the Donate screen appears as an offer, and when the NGO records it the donor's tracker updates and the coins are added to their balance.
- Karma coins (shown with a crowned food coin instead of "KP") and tiers.
- Karma Store: a balance card with tier progress, popular and category browsing, reward detail sheets, a cart with the tier discount applied (5% Silver, 10% Gold, 20% Platinum), demo voucher codes you can copy, a My rewards tab that survives restarts, and give-back rewards that turn coins into meals and trees.
- Donor home: pickup location, a swipeable banner (donate, redeem points, a live food-waste counter), a tracker for the donation that is on its way, a food-type row that filters the NGOs asking for help, tier progress, a row of smile photos, rewards, community goal with top donors, recent donations and a live feed. NGO requests, the leaderboard, community totals, the feed and the tracker are made-up demo content (see `HomeData.kt`).
- Smile Wall: an NGO opens a received donation, takes or picks a photo of the people who enjoyed the food (with a consent check) and sends it to the donor, who sees it on their Smile Wall. Photos are stored privately on the phone, so for now both roles share one device. Two sample photos are added on first launch so the wall is not empty; they also show under "Sent by you" on the receiver's Smiles tab.

## Not built yet
Auto-matching, collector role, real delivery tracking (the home tracker shows a demo donation), raw-material requests, and a shared backend (so smiles can reach the donor's own phone).

## Setup
1. Copy `.env.example` to `.env` and set `GEMINI_API_KEY` and `MAPS_API_KEY`.
2. Open in Android Studio and run on a device or emulator (minSdk 24).

Note: the Gemini key is currently packaged in the APK. Before a public release, move AI calls behind Firebase AI Logic with App Check or a backend proxy, and restrict the Maps key to this app's package and signing SHA-1.

## Brand logos
The Karma Store shows partner logos on brand-coloured tiles. The bundled McDonald's, Swiggy, Spotify, Samsung, Puma, Nike, Zomato, Starbucks and Netflix marks come from [Simple Icons](https://simpleicons.org) (CC0). All brand names and logos are trademarks of their owners and are shown only to illustrate the rewards store in this prototype; a real release would need each partner's permission.

To add or replace a logo, put a file named `logo_<brand>` (for example `logo_nike.png`) in `app/src/main/res/drawable` and match the `logoName` of the reward in `MainActivity.kt`. A reward without a logo file shows its name on a tile.

## Illustrations
The welcome steps, food-type row, banner and tier medals use custom flat illustrations drawn for this app (`app/src/main/res/drawable/illus_*.xml`, vector drawables, no stock art). They are generated from `tools/illustrations/art.py` (run `python3 art.py`; it writes SVG previews and the Android XML into an `out/` folder), so colours and shapes can be changed in one place. The smile photos on the Smile Wall (and the food photos below, once added) are the only photographs in the app.

## Food photos
The "What are you donating?" row on the donor Home shows a round photo for each food type when one is bundled. Add up to six JPGs to `app/src/main/res/drawable-nodpi/` named `photo_meals.jpg`, `photo_bakery.jpg`, `photo_fruit.jpg`, `photo_vegetables.jpg`, `photo_packaged.jpg` and `photo_dairy.jpg` (square crop, about 600 x 600 px, under 150 KB each). A type without a photo keeps its illustration. Use pictures you have the right to use, for example from Unsplash or Pexels, and credit the photographer here.

## Design notes
The interface avoids the common "template" look: one typeface in real weights, sentence-case labels instead of small capitals, plain-language copy, tonal cards without borders, a single corner radius, and illustrations that carry meaning (a camera for the photo step, a map pin for pickup) instead of generic icon badges. Numbers use tabular figures rather than a monospace font.

Moving between tabs slides the new screen in from the side the tab sits on (going from Home to Donate comes in from the right, going back from Donate to Home comes in from the left), with the old screen fading out just before the new one fades in. The motion lives in `NavTransitions.kt` and the direction rule in `TabNavigation.kt`.

The home screen borrows layout ideas from food-delivery and donation apps (location header, banner carousel, category row, request cards with progress bars, order-style tracker) while keeping one accent colour and plain copy.

Typeface: [Plus Jakarta Sans](https://github.com/tokotype/PlusJakartaSans) (SIL Open Font License, see `licenses/`).
