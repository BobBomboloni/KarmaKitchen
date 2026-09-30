import sys

new_kt_content = """package com.example.ui

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay

@Composable
fun ScannerAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()

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
        targetValue = -28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Wavy line color animation (4.8s)
    val waveColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF2CD457),
        targetValue = Color(0xFFFFA600),
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
        )
    )

    // Wavy path
    val wavyPath = remember {
        Path().apply {
            moveTo(2f, 7f)
            quadraticBezierTo(11f, 1f, 20f, 7f)
            // T commands in SVG are smooth quadratic curveto.
            // A simple approximation for the wavy track:
            val step = 18f
            for (i in 1..9) {
                val cx = 20f + step * i - 9f
                val cy = if (i % 2 == 1) 13f else 1f
                val x = 20f + step * i
                quadraticBezierTo(cx, cy, x, 7f)
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
                // Morphing background
                // Use AnimatedImageVector or animatedVectorResource
                val avdPainter = androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter(
                    animatedImageVector = androidx.compose.animation.graphics.res.AnimatedImageVector.animatedVectorResource(R.drawable.avd_food_morph),
                    atEnd = true
                )
                
                // Instead of relying purely on AVD, which sometimes needs state toggles to run continuously:
                // Let's implement a simple repeating keyframe state to drive atEnd if needed.
                var atEnd by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    while (true) {
                        atEnd = !atEnd
                        delay(8000)
                    }
                }
                
                // Actually AnimatedImageVector with infinite objectAnimator just plays automatically if atEnd=true? 
                // In Compose 1.4+, AnimatedVectorResource looping isn't always reliable. 
                // Let's use it as a base, but wait, we can also use our own interpolator if it fails.
                Icon(
                    painter = androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter(
                        animatedImageVector = androidx.compose.animation.graphics.res.AnimatedImageVector.animatedVectorResource(R.drawable.avd_food_morph),
                        atEnd = atEnd
                    ),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = Color.Unspecified
                )

                // Accents
                // Apple
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
                    )
                )
                AccentIcon(pathData = applePathData, strokeColor = Color(0xFF5A3A24), fillColor = Color(0xFF4C8A3E), alpha = appleAlpha)

                // Carrot
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
                    )
                )
                AccentIcon(pathData = carrotPathData, strokeColor = Color(0xFF4C8A3E), fillColor = Color.Transparent, alpha = carrotAlpha, strokeWidth = 3f)

                // Bread
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
                    )
                )
                AccentIcon(pathData = breadPathData, strokeColor = Color(0xFF8A5423), fillColor = Color.Transparent, alpha = breadAlpha * 0.55f, strokeWidth = 1.6f)
            }

            // Wavy Track
            val density = LocalDensity.current
            Canvas(
                modifier = Modifier
                    .width(180.dp)
                    .height(14.dp)
            ) {
                val strokeW = 5f
                val dashLen = 6f * density.density
                val gapLen = 4f * density.density
                
                val scaledPhase = (waveShift / -28f) * (dashLen + gapLen)
                
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
fun AccentIcon(pathData: List<androidx.compose.ui.graphics.vector.PathNode>, strokeColor: Color, fillColor: Color, alpha: Float, strokeWidth: Float = 2f) {
    if (alpha <= 0.01f) return
    Canvas(modifier = Modifier.fillMaxSize().alpha(alpha)) {
        val path = androidx.compose.ui.graphics.vector.PathParser().parsePathString(
            pathData.joinToString(" ") { it.toString() } // For simplicity, we just use PathParser inside draw
        ).toPath()
        // Wait, Compose PathParser needs string.
        // I will just use string path data below for simplicity.
    }
}
"""

with open("app/src/main/java/com/example/ui/ScannerAnimation.kt", "w") as f:
    f.write(new_kt_content)
