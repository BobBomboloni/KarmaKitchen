# KarmaKitchen

AI-powered surplus food redistribution (SDG 2, 12, 13). Android app built with Kotlin and Jetpack Compose.

## What works
- Donor flow: a three-step Donate screen (photo and AI check, details with pickup time and a note, pickup address and coins summary). Gemini checks safety and quality; a submitted donation then shows up on the home screen tracker.
- Receiver (NGO) side with its own bottom bar: Home (availability, storage level, urgent-need broadcast that shows up on the donor home, attention list, weekly meals), Offers (accept or decline, track the volunteer, log intake with the camera and AI, with a manual fallback), Stock (expiry bars, hand out or dispose) and Smiles (send photos to donors). Because both roles run on one phone, a donation posted from the Donate screen appears as an offer, and when the NGO records it the donor's tracker updates and the coins are added to their balance.
- Warm light theme by default with a dark mode switch in Profile (the choice is saved); every screen, including the role selector, follows it. The role selector's logo is drawn in Compose (`AnimatedLogo.kt`), so it works on both backgrounds; the old black-background video (`res/raw/logo_animation.mp4`) is no longer used and can be deleted to make the app about 20 MB smaller. Headlines, screen titles and big numbers use a soft serif (Fraunces); body text stays in Plus Jakarta Sans.
- Karma coins (shown with a crowned food coin instead of "KP") and tiers.
- Karma Store: a balance card with tier progress, popular and category browsing, reward detail sheets, a cart with the tier discount applied (5% Silver, 10% Gold, 20% Platinum), demo voucher codes you can copy, a My rewards tab that survives restarts, and give-back rewards that turn coins into meals and trees.
- Donor home: pickup location, a swipeable banner (the newest smile photo first, then donate, redeem points and a live food-waste counter), a tracker for the donation that is on its way, a food-type row that filters the NGOs asking for help, tier progress, a row of smile photos, rewards, community goal with top donors, recent donations and a live feed. NGO requests, the leaderboard, community totals, the feed and the tracker are made-up demo content (see `HomeData.kt`).
- Smile Wall: an NGO opens a received donation, takes or picks a photo of the people who enjoyed the food (with a consent check) and sends it to the donor, who sees it on their Smile Wall. Photos are stored privately on the phone, so for now both roles share one device. Two sample photos are added on first launch so the wall is not empty; they also show under "Sent by you" on the receiver's Smiles tab.

- Accounts and a shared backend (Firebase, free Spark plan): sign in with Google or email, pick donor or NGO once, and the profile is saved in Firestore. A donation posted on a donor's phone shows up live as an offer on every verified NGO's phone; when an NGO accepts it and later records it as received, the donor's tracker follows along and the coins are added. The balance is the coins on received donations minus coins spent in the store. Code is in `cloud/`, security rules in `firestore.rules`.
- Gemini runs through Firebase AI Logic, so no API key is packed into the APK. A busy Gemini (HTTP 503) is retried for about 15 seconds and then tried on a lighter model; if it is still busy, the scan shows "Our AI is busy right now" with a Try again button.

Without `app/google-services.json` the app runs in the old single-phone demo mode: no sign-in, both roles on one phone, and the AI features are off.

## Not built yet
Photos and smiles shared between phones (they stay on the phone that took them), NGO stock in the cloud, coin spending checked against the balance, push notifications, matching donors to the nearest NGO, auto-matching, collector role, live volunteer tracking and raw-material requests. With an account, NGO stock, the leaderboard, NGO requests, the feed and community totals are still demo content.

## Setup
Everything below is free and needs no card. Stay on Firebase's Spark plan.

1. Copy `.env.example` to `.env` and set `MAPS_API_KEY`. (`GEMINI_API_KEY` is no longer used.)
2. Create a project at [console.firebase.google.com](https://console.firebase.google.com) and add an Android app with the package name `com.aistudio.karmakitchen.hxfm`. Add the SHA-1 of every computer that builds the app (Android Studio: Gradle panel > Tasks > android > signingReport, the `debug` variant).
3. Authentication > Sign-in method: turn on **Google** and **Email/Password**.
4. Download `google-services.json` (Project settings > Your apps) **after** turning on Google sign-in and adding the SHA-1, and put it in `app/`. Google sign-in only works if the file has an `oauth_client` entry. The file is git-ignored because this repo is public, so share it with teammates directly.
5. Firestore Database > Create database, location `asia-south1 (Mumbai)`, production mode. Then open Rules, paste in `firestore.rules` and press Publish.
6. AI Logic > Get started > **Gemini Developer API**.
7. App Check > Apps > register the Android app with **Play Integrity** (it asks for the SHA-256 fingerprint). Every phone or emulator that runs a debug build also needs its own debug token, or the AI answers "Firebase App Check token is invalid": run the app, search Logcat for "debug secret", copy the code, and add it under App Check > Apps > ⋮ > Manage debug tokens. Uninstalling the app makes a new code.
8. Open in Android Studio and run on a device or emulator (minSdk 24).

To test a donation end to end you need two accounts, ideally on two phones: sign up as a donor on one and as an NGO on the other. New NGOs cannot see or accept offers until they are verified: in Firestore, open `ngos/<the NGO's user id>` and set `verified` to `true`.

Restrict the Maps key to this app's package and signing SHA-1 before a public release. The old Gemini key was packed into earlier APKs, so delete it in Google AI Studio.

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

Motion is used to say something: the coin balance counts up when coins arrive (with a "+600" in the pill), a "Delivered" stamp lands on the tracker when the NGO confirms the food, cards and buttons dip while pressed, and grey placeholders shimmer while Gemini checks a photo. The helpers are in `Motion.kt`.

Colours come from two palettes in `ui/theme/Color.kt` (`LightPalette`, `DarkPalette`). Screens use the names (`TextPrimary`, `SurfaceColor`, `PrimaryGreen`, ...), never raw hex, so both themes stay in step. `PaletteContrastTest` checks that every text and background pair keeps at least 4.5:1 contrast in both palettes. Text that sits on a photo uses fixed white because the photo does not change with the theme.

Typefaces: [Plus Jakarta Sans](https://github.com/tokotype/PlusJakartaSans) for body text and [Fraunces](https://github.com/undercasetype/Fraunces) for headlines and numbers (both SIL Open Font License, see `licenses/`).
