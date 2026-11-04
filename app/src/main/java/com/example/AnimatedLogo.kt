package com.example

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The KarmaKitchen app-icon mark with a little life: it pops in, and the leaf on the K sways
 * gently, like a plant in a breeze.
 */
@Composable
fun AnimatedKarmaLogo(modifier: Modifier = Modifier, size: Dp = 112.dp) {
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
    }
    val sway = rememberLeafSway()
    Box(modifier = modifier.size(size * 1.25f), contentAlignment = Alignment.Center) {
        KarmaKitchenMark(
            size = size,
            leafTurn = sway,
            modifier = Modifier.graphicsLayer {
                val pop = 0.7f + 0.3f * entrance.value
                scaleX = pop
                scaleY = pop
                alpha = entrance.value.coerceIn(0f, 1f)
            }
        )
    }
}
