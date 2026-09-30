import sys

new_kt_content = """package com.example.ui

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
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
import androidx.core.graphics.PathParser
import com.example.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationGraphicsApi::class)
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
        initialValue = Color(0xFF2CD457),
        targetValue = Color(0xFF2CD457), // It will hit keyframes
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4800
                Color(0xFF2CD457) at 0
                Color(0xFF2CD457) at 2160 // 45%
                Color(0xFFFFA600) at 2400 // 50%
                Color(0xFFFFA600) at 4560 // 95%
                Color(0xFF2CD457) at 4800 // 100%
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "waveColor"
    )

    // Wavy path
    val density = LocalDensity.current
    val wavyPath = remember(density.density) {
        androidx.compose.ui.graphics.Path().apply {
            moveTo(2f * density.density, 7f * density.density)
            // Scale points by density to match original 180x14 viewBox
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
            // Scanner shape morphing and icons
            Box(
                modifier = Modifier.size(150.dp)
            ) {
                // Morphing background using AnimatedVectorDrawable
                var atEnd by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    while (true) {
                        atEnd = !atEnd
                        delay(8000)
                    }
                }
                
                val avd = AnimatedImageVector.animatedVectorResource(R.drawable.avd_food_morph)
                Icon(
                    painter = rememberAnimatedVectorPainter(avd, atEnd),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = Color.Unspecified
                )

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
                AccentIcon(
                    pathDataStr = "M50 27 C 49 20 51 15 57 12|M56 13 C 62 10 68 12 70 17 C 65 19 58 18 56 13 Z",
                    strokeColor = Color(0xFF5A3A24),
                    fillColor = Color(0xFF4C8A3E),
                    alpha = appleAlpha
                )

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
                AccentIcon(
                    pathDataStr = "M50 19 L50 5 M43 20 L38 8 M57 20 L62 8|",
                    strokeColor = Color(0xFF4C8A3E),
                    fillColor = Color.Transparent,
                    alpha = carrotAlpha,
                    strokeWidth = 3f
                )

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
                AccentIcon(
                    pathDataStr = "M32 30 C 40 40 45 46 44 55|M50 26 C 55 37 55 45 52 56|M68 30 C 62 40 58 46 60 55",
                    strokeColor = Color(0xFF8A5423),
                    fillColor = Color.Transparent,
                    alpha = breadAlpha * 0.55f,
                    strokeWidth = 1.6f
                )
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
                color = Color(0xFFF5EFE2),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp
            )
        }
    }
}

@Composable
fun AccentIcon(pathDataStr: String, strokeColor: Color, fillColor: Color, alpha: Float, strokeWidth: Float = 2f) {
    if (alpha <= 0.01f) return
    val density = LocalDensity.current
    
    // Parse the multiple paths separated by |
    val paths = remember(pathDataStr) {
        pathDataStr.split("|").filter { it.isNotBlank() }.map { pathStr ->
            // Scale path to exactly match 100x100 viewBox mapped to 150dp container
            val androidPath = PathParser.createPathFromPathData(pathStr)
            val composePath = androidx.compose.ui.graphics.asComposePath(androidPath)
            
            // The original SVG was 100x100 and displayed in a 150px container.
            // In Compose, the container is 150.dp, so we scale by 1.5 * density
            val scaleMatrix = android.graphics.Matrix().apply {
                setScale(1.5f * density.density, 1.5f * density.density)
            }
            androidPath.transform(scaleMatrix)
            androidx.compose.ui.graphics.asComposePath(androidPath)
        }
    }

    Canvas(modifier = Modifier.fillMaxSize().alpha(alpha)) {
        for (i in paths.indices) {
            if (strokeColor != Color.Transparent) {
                drawPath(
                    path = paths[i],
                    color = strokeColor,
                    style = Stroke(width = strokeWidth * density.density, cap = StrokeCap.Round)
                )
            }
            if (fillColor != Color.Transparent) {
                drawPath(
                    path = paths[i],
                    color = fillColor,
                    style = Fill
                )
            }
        }
    }
}
"""

with open("app/src/main/java/com/example/ui/ScannerAnimation.kt", "w") as f:
    f.write(new_kt_content)
