package com.example

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.SecondaryAmber
import kotlin.math.PI
import kotlin.math.sin

/**
 * The KarmaKitchen logo with a little life: it pops in, floats gently over a warm glow, and a few
 * hearts drift up from the bowl. It is drawn in Compose, so it sits on the light or the dark
 * background (the old logo video only worked on a black one).
 */
@Composable
fun AnimatedKarmaLogo(modifier: Modifier = Modifier, size: Dp = 140.dp) {
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
    }
    val loop = rememberInfiniteTransition(label = "logo")
    val glow by loop.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    val bob by loop.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )
    val rise by loop.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "rise"
    )
    val density = LocalDensity.current
    val sizePx = with(density) { size.toPx() }
    val dpPx = density.density
    val glowColor = SecondaryAmber
    val heartColors = listOf(SecondaryAmber, AccentCoral, SecondaryAmber)

    Box(modifier = modifier.size(size * 1.5f), contentAlignment = Alignment.Center) {
        // Warm light behind the logo.
        Box(
            modifier = Modifier
                .size(size * 1.5f)
                .graphicsLayer { alpha = entrance.value }
                .drawBehind {
                    drawCircle(Brush.radialGradient(listOf(glowColor.copy(alpha = glow * 0.5f), Color.Transparent)))
                }
        )
        Image(
            painter = painterResource(R.drawable.ic_karma_logo),
            contentDescription = "KarmaKitchen logo",
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    val pop = 0.7f + 0.3f * entrance.value
                    scaleX = pop
                    scaleY = pop
                    alpha = entrance.value.coerceIn(0f, 1f)
                    translationY = bob * dpPx
                }
        )
        // Hearts drift up from the bowl, each a third of a cycle behind the last.
        heartColors.forEachIndexed { index, tint ->
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer {
                        val phase = (rise + index / 3f) % 1f
                        val fade = sin(PI * phase).toFloat()
                        translationY = (0.14f - phase * 0.7f) * sizePx
                        translationX = sin(phase * 2f * PI + index * 2.1).toFloat() * 0.12f * sizePx
                        alpha = fade * entrance.value
                        scaleX = 0.6f + 0.6f * fade
                        scaleY = 0.6f + 0.6f * fade
                    }
            )
        }
    }
}
