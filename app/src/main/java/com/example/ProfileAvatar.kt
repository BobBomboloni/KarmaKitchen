package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.OnPrimaryGreen
import com.example.ui.theme.OnPrimaryGreenContainer
import com.example.ui.theme.PrimaryGreen

/**
 * Content of the round profile button: the person's initials, or a small smiling-person
 * illustration while no name has been entered yet. Draw it inside a circular container.
 */
@Composable
fun ProfileAvatar(initials: String, modifier: Modifier = Modifier) {
    if (initials.isNotBlank()) {
        Text(
            text = initials,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        return
    }

    Canvas(modifier = modifier.fillMaxSize().semantics { contentDescription = "Profile" }) {
        val w = size.width
        val centerX = w / 2f
        val headRadius = w * 0.20f
        val headCenter = Offset(centerX, size.height * 0.40f)

        // Shoulders; the lower part is clipped away by the round container.
        drawOval(
            color = PrimaryGreen,
            topLeft = Offset(w * 0.14f, size.height * 0.64f),
            size = Size(w * 0.72f, size.height * 0.72f)
        )
        // Head with a hair cap.
        drawCircle(color = OnPrimaryGreenContainer, radius = headRadius, center = headCenter)
        drawArc(
            color = PrimaryGreen,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(headCenter.x - headRadius, headCenter.y - headRadius),
            size = Size(headRadius * 2f, headRadius * 2f)
        )
        // A small smile.
        val eyeRadius = w * 0.025f
        drawCircle(OnPrimaryGreen, eyeRadius, Offset(centerX - w * 0.07f, headCenter.y + w * 0.02f))
        drawCircle(OnPrimaryGreen, eyeRadius, Offset(centerX + w * 0.07f, headCenter.y + w * 0.02f))
        drawArc(
            color = OnPrimaryGreen,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(centerX - w * 0.07f, headCenter.y + w * 0.00f),
            size = Size(w * 0.14f, w * 0.10f),
            style = Stroke(width = w * 0.022f, cap = StrokeCap.Round)
        )
    }
}

/** Greeting for the dashboard header, based on the hour of the day (0-23). */
fun greetingForHour(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..20 -> "Good evening"
    else -> "Hello"
}
