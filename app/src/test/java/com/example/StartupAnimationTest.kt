package com.example

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Draws the startup animation at several moments, so a drawing crash fails the build check.
 * Run `gradle :app:recordRoborazziDebug --tests com.example.StartupAnimationTest` to also save the
 * frames as PNGs in app/build/brand/.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class StartupAnimationTest {

    @get:Rule val rule = createComposeRule()

    private fun frames(dark: Boolean) {
        val time = mutableFloatStateOf(0f)
        rule.setContent {
            MyApplicationTheme(darkTheme = dark) { StartupAnimation(onFinished = {}, frozenAt = time.floatValue) }
        }
        listOf(0.5f, 0.75f, 1.2f, 1.7f, 2.1f, 2.6f, 3.25f, 3.5f, 3.8f, 4.2f, 5f).forEach { t ->
            time.floatValue = t
            rule.waitForIdle()
            val name = "intro_${if (dark) "dark" else "light"}_${(t * 100).toInt().toString().padStart(3, '0')}"
            rule.onRoot().captureRoboImage(filePath = "build/brand/$name.png")
        }
    }

    @Test fun lightFrames() = frames(dark = false)
    @Test fun darkFrames() = frames(dark = true)
}
