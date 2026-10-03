package com.example

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PrimaryGreen

/** The karma coin: a gold coin with a bowl of food on it and a crown above. It replaces "KP". */
@Composable
fun KarmaCoin(modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Image(
        painter = painterResource(R.drawable.illus_karma_coin),
        contentDescription = "Karma coins",
        modifier = modifier.size(size)
    )
}

/** An amount of karma: the number followed by the coin, sized to match the text. */
@Composable
fun KarmaAmount(
    amount: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = PrimaryGreen,
    fontWeight: FontWeight? = null
) {
    val coinSize = with(LocalDensity.current) { style.fontSize.toDp() } * 1.5f
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = amount,
            style = style.copy(fontFeatureSettings = "tnum"),
            color = color,
            fontWeight = fontWeight
        )
        Spacer(Modifier.width(4.dp))
        KarmaCoin(size = coinSize)
    }
}

fun formatKarma(points: Int): String = "%,d".format(points)
