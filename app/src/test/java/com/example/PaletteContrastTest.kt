package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.ui.theme.AppPalette
import com.example.ui.theme.DarkPalette
import com.example.ui.theme.LightPalette
import org.junit.Assert.assertTrue
import org.junit.Test

/** Text and its background must stay readable (WCAG AA, 4.5:1) in both palettes; input borders need 3:1. */
class PaletteContrastTest {
    private fun contrast(a: Color, b: Color): Double {
        val hi = maxOf(a.luminance(), b.luminance()).toDouble()
        val lo = minOf(a.luminance(), b.luminance()).toDouble()
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun check(name: String, palette: AppPalette) {
        val text = listOf(
            "text on background" to (palette.textPrimary to palette.background),
            "text on card" to (palette.textPrimary to palette.surface),
            "secondary text on background" to (palette.textSecondary to palette.background),
            "secondary text on card" to (palette.textSecondary to palette.surface),
            "hint text on card" to (palette.textTertiary to palette.surface),
            "hint text on tile" to (palette.textTertiary to palette.surfaceVariant),
            "green on card" to (palette.primary to palette.surface),
            "green on background" to (palette.primary to palette.background),
            "green on its container" to (palette.primary to palette.primaryContainer),
            "button text on green" to (palette.onPrimary to palette.primary),
            "text on green container" to (palette.onPrimaryContainer to palette.primaryContainer),
            "amber text on card" to (palette.amberText to palette.surface),
            "amber text on background" to (palette.amberText to palette.background),
            "amber text on meals card" to (palette.amberText to palette.mealsBg),
            "meals text on meals card" to (palette.mealsText to palette.mealsBg),
            "button text on amber" to (palette.onAmber to palette.amber),
            "red on card" to (palette.danger to palette.surface),
            "button text on red" to (palette.onDanger to palette.danger),
            "text on red container" to (palette.onDangerContainer to palette.dangerContainer),
            "blue on card" to (palette.info to palette.surface),
            "text on blue container" to (palette.onInfoContainer to palette.infoContainer)
        )
        text.forEach { (label, pair) ->
            val ratio = contrast(pair.first, pair.second)
            assertTrue("$name: $label is only ${"%.2f".format(ratio)}:1", ratio >= 4.5)
        }
        val border = contrast(palette.outlineStrong, palette.surface)
        assertTrue("$name: input border is only ${"%.2f".format(border)}:1", border >= 3.0)
    }

    @Test fun lightPaletteIsReadable() = check("light", LightPalette)

    @Test fun darkPaletteIsReadable() = check("dark", DarkPalette)

    @Test fun paletteFlagsMatchTheirNames() {
        assertTrue(DarkPalette.isDark)
        assertTrue(!LightPalette.isDark)
    }
}
