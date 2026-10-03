package com.example.ui

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.WarningColor
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun ScannerAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")

    // Dotting animation for "Analyzing food..."
    var dotCount by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(350)
            dotCount = (dotCount + 1) % 4
        }
    }
    val dots = ".".repeat(dotCount)

    // Wavy line shift animation
    val waveShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveShift"
    )

    // Wavy line color animation (4.8s)
    val waveColor by infiniteTransition.animateColor(
        initialValue = PrimaryGreen,
        targetValue = PrimaryGreen, 
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4800
                PrimaryGreen at 0
                PrimaryGreen at 2160 // 45%
                WarningColor at 2400 // 50%
                WarningColor at 4560 // 95%
                PrimaryGreen at 4800 // 100%
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "waveColor"
    )

    val density = LocalDensity.current
    val wavyPath = remember(density.density) {
        Path().apply {
            moveTo(2f * density.density, 7f * density.density)
            val scale = density.density
            quadraticBezierTo(11f * scale, 1f * scale, 20f * scale, 7f * scale)
            val step = 18f * scale
            for (i in 1..9) {
                val cx = (20f * scale) + step * i - (9f * scale)
                val cy = if (i % 2 == 1) 13f * scale else 1f * scale
                val x = (20f * scale) + step * i
                quadraticBezierTo(cx, cy, x, 7f * scale)
            }
        }
    }

    Box(
        modifier = modifier
            .padding(vertical = 48.dp, horizontal = 40.dp)
            .scale(0.85f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Box(modifier = Modifier.size(150.dp)) {
                // Background shape morph
                val morphProgress by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(8000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "morphProgress"
                )
                
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val scale = 1.5f * density.density // 100x100 -> 150dp
                    val (currentPath, currentColor) = getMorphState(morphProgress, scale)
                    drawPath(path = currentPath, color = currentColor, style = Fill)
                }

                // Accents
                val appleAlpha by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = keyframes {
                            durationMillis = 8000
                            1f at 0
                            1f at 800
                            0f at 1280
                            0f at 6320
                            1f at 7680
                            1f at 8000
                        },
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "appleAlpha"
                )
                AccentApple(alpha = appleAlpha)

                val carrotAlpha by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = keyframes {
                            durationMillis = 8000
                            0f at 0
                            0f at 5680
                            1f at 6160
                            1f at 7120
                            0f at 7600
                            0f at 8000
                        },
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "carrotAlpha"
                )
                AccentCarrot(alpha = carrotAlpha)

                val breadAlpha by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = keyframes {
                            durationMillis = 8000
                            0f at 0
                            0f at 3680
                            1f at 4160
                            1f at 5120
                            0f at 5600
                            0f at 8000
                        },
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "breadAlpha"
                )
                AccentBread(alpha = breadAlpha * 0.55f)
            }

            // Wavy Track
            Canvas(
                modifier = Modifier
                    .width(180.dp)
                    .height(14.dp)
            ) {
                val strokeW = 5f * density.density
                val dashLen = 6f * density.density
                val gapLen = 4f * density.density
                val scaledPhase = (waveShift / 28f) * (dashLen + gapLen)
                
                drawPath(
                    path = wavyPath,
                    color = waveColor,
                    style = Stroke(
                        width = strokeW,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(dashLen, gapLen),
                            phase = -scaledPhase
                        )
                    )
                )
            }

            // Status text
            Text(
                text = "Analyzing food$dots",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp
            )
        }
    }
}

// ---------------------------------------------------------
// Manual path definitions and morphing logic
// ---------------------------------------------------------

private val applePath = floatArrayOf(50.0f, 27.0f, 55.5f, 27.0f, 61.16f, 24.0f, 66.5f, 25.42f, 71.84f, 26.84f, 78.79f, 30.74f, 82.04f, 35.5f, 85.29f, 40.26f, 86.43f, 48.08f, 86.0f, 54.0f, 85.57f, 59.92f, 82.69f, 66.24f, 79.44f, 71.0f, 76.19f, 75.76f, 71.41f, 79.41f, 66.5f, 82.58f, 61.59f, 85.75f, 55.5f, 90.0f, 50.0f, 90.0f, 44.5f, 90.0f, 38.41f, 85.75f, 33.5f, 82.58f, 28.59f, 79.41f, 23.81f, 75.76f, 20.56f, 71.0f, 17.31f, 66.24f, 14.43f, 59.92f, 14.0f, 54.0f, 13.57f, 48.08f, 14.71f, 40.26f, 17.96f, 35.5f, 21.21f, 30.74f, 28.16f, 26.84f, 33.5f, 25.42f, 38.84f, 24.0f, 44.5f, 27.0f, 50.0f, 27.0f)
private val bananaPath = floatArrayOf(40.45f, 76.55f, 38.83f, 75.69f, 38.89f, 69.48f, 39.0f, 65.94f, 39.1f, 62.41f, 39.73f, 58.56f, 41.09f, 55.35f, 42.46f, 52.14f, 44.62f, 48.97f, 47.18f, 46.67f, 49.74f, 44.36f, 53.12f, 42.54f, 56.46f, 41.52f, 59.79f, 40.5f, 63.68f, 40.28f, 67.21f, 40.54f, 70.73f, 40.8f, 76.92f, 41.39f, 77.6f, 43.09f, 78.29f, 44.79f, 73.17f, 48.31f, 71.33f, 50.74f, 69.49f, 53.17f, 68.12f, 55.63f, 66.55f, 57.68f, 64.98f, 59.72f, 63.64f, 61.45f, 61.9f, 63.02f, 60.16f, 64.58f, 58.31f, 65.73f, 56.11f, 67.08f, 53.91f, 68.43f, 51.32f, 69.53f, 48.71f, 71.11f, 46.1f, 72.69f, 42.07f, 77.41f, 40.45f, 76.55f)
private val breadPath = floatArrayOf(41.0f, 15.0f, 45.5f, 13.17f, 47.0f, 12.0f, 50.0f, 12.0f, 53.0f, 12.0f, 54.5f, 13.17f, 59.0f, 15.0f, 63.5f, 16.83f, 72.83f, 17.17f, 77.0f, 23.0f, 81.17f, 28.83f, 84.0f, 41.0f, 84.0f, 50.0f, 84.0f, 59.0f, 80.83f, 71.17f, 77.0f, 77.0f, 73.17f, 82.83f, 65.5f, 83.33f, 61.0f, 85.0f, 56.5f, 86.67f, 53.67f, 87.0f, 50.0f, 87.0f, 46.33f, 87.0f, 43.5f, 86.67f, 39.0f, 85.0f, 34.5f, 83.33f, 26.83f, 82.83f, 23.0f, 77.0f, 19.17f, 71.17f, 16.0f, 59.0f, 16.0f, 50.0f, 16.0f, 41.0f, 18.83f, 28.83f, 23.0f, 23.0f, 27.17f, 17.17f, 36.5f, 16.83f, 41.0f, 15.0f)
private val carrotPath = floatArrayOf(50.0f, 17.0f, 54.17f, 17.0f, 59.17f, 17.33f, 63.0f, 19.0f, 66.83f, 20.67f, 72.17f, 23.33f, 73.0f, 27.0f, 73.83f, 30.67f, 70.17f, 36.33f, 68.0f, 41.0f, 65.83f, 45.67f, 62.33f, 50.33f, 60.0f, 55.0f, 57.67f, 59.67f, 55.67f, 63.33f, 54.0f, 69.0f, 52.33f, 74.67f, 51.5f, 89.0f, 50.0f, 89.0f, 48.5f, 89.0f, 46.83f, 74.67f, 45.0f, 69.0f, 43.17f, 63.33f, 41.17f, 59.67f, 39.0f, 55.0f, 36.83f, 50.33f, 33.83f, 45.67f, 32.0f, 41.0f, 30.17f, 36.33f, 27.0f, 30.67f, 28.0f, 27.0f, 29.0f, 23.33f, 34.33f, 20.67f, 38.0f, 19.0f, 41.67f, 17.33f, 45.83f, 17.0f, 50.0f, 17.0f)

private fun lerp(start: Float, end: Float, fraction: Float): Float = start + (end - start) * fraction
private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    return Color(
        red = lerp(start.red, end.red, fraction),
        green = lerp(start.green, end.green, fraction),
        blue = lerp(start.blue, end.blue, fraction),
        alpha = lerp(start.alpha, end.alpha, fraction)
    )
}

// Emulates a spring by using a simplified easing cubic-bezier(0.3, 1.3, 0.5, 1) approximation
// For simplicity, we just use linear tween between keyframes, but map the progress to the keyframes.
private fun getMorphState(progress: Float, scale: Float): Pair<Path, Color> {
    val keyframes = listOf(
        0.00f to Pair(applePath, Color(0xFF3fb84f)),
        0.12f to Pair(applePath, Color(0xFF3fb84f)),
        0.25f to Pair(bananaPath, Color(0xFFf4cc3a)),
        0.37f to Pair(bananaPath, Color(0xFFf4cc3a)),
        0.50f to Pair(breadPath, Color(0xFFd99a52)),
        0.62f to Pair(breadPath, Color(0xFFd99a52)),
        0.75f to Pair(carrotPath, Color(0xFFf0973a)),
        0.87f to Pair(carrotPath, Color(0xFFf0973a)),
        1.00f to Pair(applePath, Color(0xFF3fb84f))
    )
    
    var idx = 0
    while (idx < keyframes.size - 1 && progress > keyframes[idx + 1].first) {
        idx++
    }
    val (t1, state1) = keyframes[idx]
    val (t2, state2) = if (idx + 1 < keyframes.size) keyframes[idx + 1] else keyframes[idx]
    
    val fraction = if (t2 > t1) {
        val f = (progress - t1) / (t2 - t1)
        // Spring easing approximation: 
        val t = f
        (1.3 * t * t * t - 1.5 * t * t + 1.2 * t).coerceIn(0.0, 1.0).toFloat() // simple custom easing or just use f
    } else 0f
    
    val p1 = state1.first
    val p2 = state2.first
    val c1 = state1.second
    val c2 = state2.second
    
    val color = lerpColor(c1, c2, fraction)
    val path = Path().apply {
        moveTo(lerp(p1[0], p2[0], fraction) * scale, lerp(p1[1], p2[1], fraction) * scale)
        for (i in 1..12) {
            val offset = i * 6 - 4
            cubicTo(
                lerp(p1[offset], p2[offset], fraction) * scale, lerp(p1[offset+1], p2[offset+1], fraction) * scale,
                lerp(p1[offset+2], p2[offset+2], fraction) * scale, lerp(p1[offset+3], p2[offset+3], fraction) * scale,
                lerp(p1[offset+4], p2[offset+4], fraction) * scale, lerp(p1[offset+5], p2[offset+5], fraction) * scale,
            )
        }
        close()
    }
    return Pair(path, color)
}

@Composable
fun AccentApple(alpha: Float) {
    if (alpha <= 0.01f) return
    val density = LocalDensity.current
    val scale = 1.5f * density.density
    Canvas(modifier = Modifier.fillMaxSize().alpha(alpha)) {
        val stem = Path().apply {
            moveTo(50f * scale, 27f * scale)
            cubicTo(49f * scale, 20f * scale, 51f * scale, 15f * scale, 57f * scale, 12f * scale)
        }
        val leaf = Path().apply {
            moveTo(56f * scale, 13f * scale)
            cubicTo(62f * scale, 10f * scale, 68f * scale, 12f * scale, 70f * scale, 17f * scale)
            cubicTo(65f * scale, 19f * scale, 58f * scale, 18f * scale, 56f * scale, 13f * scale)
            close()
        }
        drawPath(path = stem, color = Color(0xFF5A3A24), style = Stroke(width = 2f * density.density, cap = StrokeCap.Round))
        drawPath(path = leaf, color = Color(0xFF4C8A3E), style = Fill)
    }
}

@Composable
fun AccentCarrot(alpha: Float) {
    if (alpha <= 0.01f) return
    val density = LocalDensity.current
    val scale = 1.5f * density.density
    Canvas(modifier = Modifier.fillMaxSize().alpha(alpha)) {
        val lines = Path().apply {
            moveTo(50f * scale, 19f * scale)
            lineTo(50f * scale, 5f * scale)
            moveTo(43f * scale, 20f * scale)
            lineTo(38f * scale, 8f * scale)
            moveTo(57f * scale, 20f * scale)
            lineTo(62f * scale, 8f * scale)
        }
        drawPath(path = lines, color = Color(0xFF4C8A3E), style = Stroke(width = 3f * density.density, cap = StrokeCap.Round))
    }
}

@Composable
fun AccentBread(alpha: Float) {
    if (alpha <= 0.01f) return
    val density = LocalDensity.current
    val scale = 1.5f * density.density
    Canvas(modifier = Modifier.fillMaxSize().alpha(alpha)) {
        val lines = Path().apply {
            moveTo(32f * scale, 30f * scale)
            cubicTo(40f * scale, 40f * scale, 45f * scale, 46f * scale, 44f * scale, 55f * scale)
            moveTo(50f * scale, 26f * scale)
            cubicTo(55f * scale, 37f * scale, 55f * scale, 45f * scale, 52f * scale, 56f * scale)
            moveTo(68f * scale, 30f * scale)
            cubicTo(62f * scale, 40f * scale, 58f * scale, 46f * scale, 60f * scale, 55f * scale)
        }
        drawPath(path = lines, color = Color(0xFF8A5423), style = Stroke(width = 1.6f * density.density, cap = StrokeCap.Round))
    }
}
