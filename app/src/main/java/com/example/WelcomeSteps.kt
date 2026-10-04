package com.example

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentGold
import com.example.ui.theme.InfoColor
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.SecondaryAmber
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/** One step of "How it works" on the donor welcome screen, with its custom illustration. */
data class WelcomeStep(val title: String, val body: String, val art: Int, val tint: Color)

@Composable
fun welcomeSteps(): List<WelcomeStep> = listOf(
    WelcomeStep(
        "Photograph the food",
        "The app estimates servings and checks how fresh it looks, so there is nothing to type.",
        R.drawable.illus_step_camera,
        PrimaryGreen
    ),
    WelcomeStep(
        "Choose a pickup spot",
        "Your location fills in automatically, so a volunteer can find you.",
        R.drawable.illus_step_map,
        InfoColor
    ),
    WelcomeStep(
        "A volunteer collects it",
        "Nearby NGOs see your post and send someone over. You can follow the pickup from your home screen.",
        R.drawable.illus_step_pickup,
        SecondaryAmber
    ),
    WelcomeStep(
        "See the smiles",
        "The shelter can send you a photo of the people who ate your food, so you see who you helped.",
        R.drawable.illus_step_smile,
        AccentCoral
    ),
    WelcomeStep(
        "Earn and spend karma",
        "Points arrive once the NGO confirms it received the food. Swap them for gift cards from partner brands.",
        R.drawable.illus_step_karma,
        AccentGold
    )
)

/** Picture on one side, text on the other. [artOnLeft] alternates down the list. */
@Composable
fun WelcomeStepRow(step: WelcomeStep, artOnLeft: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (artOnLeft) StepArt(step)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = step.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = step.body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        if (!artOnLeft) StepArt(step)
    }
}

@Composable
private fun StepArt(step: WelcomeStep) {
    Box(
        modifier = Modifier
            .size(104.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(step.tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(step.art),
            contentDescription = null,
            modifier = Modifier.size(84.dp)
        )
    }
}
