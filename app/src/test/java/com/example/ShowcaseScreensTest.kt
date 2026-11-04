package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Opens every main screen of the show build, so a screen that crashes fails the build check.
 * Run `gradle :app:recordRoborazziDebug --tests com.example.ShowcaseScreensTest` to also save each
 * screen as a PNG (light and dark) in app/build/showcase/, for checking without a phone.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-normal-long-notround-any-420dpi-keyshidden-nonav", sdk = [36])
class ShowcaseScreensTest {

    @get:Rule val rule = createComposeRule()

    private val profile = UserProfile(name = "shamikh", address = "2, Sudha Nagar, Chikuwadi, Haripura, Vadodara", karmaPoints = 1390)

    private fun shot(name: String, tall: Boolean, dark: Boolean, content: @Composable () -> Unit) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        if (tall) RuntimeEnvironment.setQualifiers("+h3200dp")
        ThemeSettings.set(context, dark)
        SmileStore.load(context)
        VoucherStore.load(context)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            MyApplicationTheme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content() }
            }
        }
        rule.mainClock.advanceTimeBy(3000)
        rule.onRoot().captureRoboImage(filePath = "build/showcase/${name}_${if (dark) "dark" else "light"}.png")
    }

    private fun both(name: String, tall: Boolean = false, content: @Composable () -> Unit) = shot(name, tall, false, content)
    private fun dark(name: String, tall: Boolean = false, content: @Composable () -> Unit) = shot(name, tall, true, content)

    @Test fun role() = both("01_role") { RoleSelectionScreen(rememberNavController()) }
    @Test fun roleDark() = dark("01_role") { RoleSelectionScreen(rememberNavController()) }
    @Test fun welcome() = both("02_welcome", tall = true) { WelcomeScreen(rememberNavController()) }
    @Test fun home() = both("03_home", tall = true) { DonorDashboardScreen(rememberNavController(), profile) }
    @Test fun homeDark() = dark("03_home", tall = true) { DonorDashboardScreen(rememberNavController(), profile) }
    @Test fun homeNoName() = both("03b_home_noname") { DonorDashboardScreen(rememberNavController(), UserProfile()) }
    @Test fun donate() = both("04_donate", tall = true) { DonationCreationScreen(rememberNavController(), profile) {} }
    @Test fun store() = both("05_store", tall = true) { KarmaStoreScreen(rememberNavController(), profile) {} }
    @Test fun storeDark() = dark("05_store", tall = true) { KarmaStoreScreen(rememberNavController(), profile) {} }
    @Test fun smileWall() = both("06_smilewall", tall = true) { SmileWallScreen(profile.name) }
    @Test fun tiers() = both("07_tiers", tall = true) { TierListScreen(rememberNavController(), profile) }
    @Test fun profileEdit() = both("08_profile") { ProfileEditScreen(profile, {}, {}) }
    @Test fun ngoHome() = both("09_ngo_home", tall = true) { NgoDashboardScreen(rememberNavController()) }
    @Test fun ngoHomeDark() = dark("09_ngo_home", tall = true) { NgoDashboardScreen(rememberNavController()) }
    @Test fun ngoOffers() = both("10_ngo_offers", tall = true) { NgoOffersScreen(rememberNavController()) }
    @Test fun ngoStock() = both("11_ngo_stock", tall = true) { NgoStockScreen(rememberNavController()) }
    @Test fun ngoSmiles() = both("12_ngo_smiles", tall = true) { NgoSmilesScreen(rememberNavController()) }
    @Test fun track() = both("13_track") { TrackPickupScreen(rememberNavController(), recentDonations.first { it.inTransit }.id, ngoSide = false) }
    @Test fun trackNgo() = both("14_track_ngo") { TrackPickupScreen(rememberNavController(), recentDonations.first { it.inTransit }.id, ngoSide = true) }
}
