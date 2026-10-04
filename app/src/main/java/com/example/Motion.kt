package com.example

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import com.example.ui.theme.LocalPalette
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/*
 * Small pieces of motion that tell the user something, instead of decoration:
 *  - coins count up (or down) when the balance changes, with a "+600" that floats away
 *  - cards and buttons dip slightly while pressed, so a tap feels like it landed
 *  - placeholders shimmer while the AI is still working
 */

// -----------------------------------------------------------------------------
// Coin counter
// -----------------------------------------------------------------------------

/** The balance most recently shown on screen, so a screen that opens with a newer balance counts up from it. */
object CoinDisplay {
    var lastShown: Int = -1
}

@Stable
class CountedBalance(val value: Int, val gain: Int, val showGain: Boolean)

/**
 * The number to show for a balance of [target]. When the balance has changed since it was last
 * shown it counts there over a second or so. [CountedBalance.showGain] is true for a couple of
 * seconds while that happens, and [CountedBalance.gain] is the number of coins just added, for a
 * "+600" label.
 */
@Composable
fun rememberCountedBalance(target: Int): CountedBalance {
    val animated = remember { Animatable((if (CoinDisplay.lastShown < 0) target else CoinDisplay.lastShown).toFloat()) }
    var gain by remember { mutableIntStateOf(0) }
    var showGain by remember { mutableStateOf(false) }
    LaunchedEffect(target) {
        val from = animated.value.roundToInt()
        if (from != target) {
            gain = (target - from).coerceAtLeast(0)
            showGain = gain > 0
            animated.animateTo(target.toFloat(), tween(durationMillis = 1200, easing = FastOutSlowInEasing))
            CoinDisplay.lastShown = target
            delay(1100)
            showGain = false
        }
        CoinDisplay.lastShown = target
    }
    return CountedBalance(animated.value.roundToInt(), gain, showGain)
}

// -----------------------------------------------------------------------------
// Press feedback
// -----------------------------------------------------------------------------

/** Shrinks the element a little while [source] is pressed, then springs back. */
fun Modifier.pressScale(source: InteractionSource, pressedScale: Float = 0.96f): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Makes a card tappable: it clips to [shape], gets an optional [background], shows the ripple and
 * dips while pressed. Put it where `.clip(shape).background(colour).clickable { }` would go; the
 * modifiers after it (padding and so on) are scaled with the card.
 */
fun Modifier.bounceCard(shape: Shape, onClick: () -> Unit, background: Color = Color.Unspecified): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    Modifier
        .pressScale(source)
        .clip(shape)
        .then(if (background.isSpecified) Modifier.background(background) else Modifier)
        .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick)
}

/** A Material button that dips while pressed. Same parameters as `Button`. */
@Composable
fun KarmaButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val source = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = modifier.pressScale(source),
        enabled = enabled,
        shape = shape,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = source,
        content = content
    )
}

/** An outlined button that dips while pressed. Same parameters as `OutlinedButton`. */
@Composable
fun KarmaOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val source = remember { MutableInteractionSource() }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.pressScale(source),
        enabled = enabled,
        shape = shape,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        interactionSource = source,
        content = content
    )
}

// -----------------------------------------------------------------------------
// Shimmer
// -----------------------------------------------------------------------------

/** A soft band of light that sweeps across a placeholder while something loads. */
fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val sweep by transition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1300, easing = LinearEasing)),
        label = "shimmerSweep"
    )
    val band = if (LocalPalette.current.isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.7f)
    drawWithContent {
        drawContent()
        val centre = size.width * sweep
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, band, Color.Transparent),
                startX = centre - size.width * 0.35f,
                endX = centre + size.width * 0.35f
            )
        )
    }
}
