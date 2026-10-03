package com.example

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A partner's logo on a rounded tile in its brand colors.
 *
 * Logos are drawables named by [RewardItem.logoName]. To add or replace one, drop a file with that
 * exact name into app/src/main/res/drawable (for example logo_nike.png); no code change is needed.
 * Until a logo exists, the tile shows the brand name (or just its first letter on small tiles).
 *
 * The bundled McDonald's, Samsung, Puma and Nike marks come from Simple Icons (CC0). All brand
 * names and logos are trademarks of their owners and are shown here only to illustrate the rewards store.
 */
@Composable
fun BrandLogoTile(item: RewardItem, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val logoRes = remember(item.logoName) {
        context.resources.getIdentifier(item.logoName, "drawable", context.packageName)
    }
    val ownArtwork = logoRes != 0 && !item.tintLogo

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.26f))
            .background(if (ownArtwork) Color.White else item.tileColor),
        contentAlignment = Alignment.Center
    ) {
        when {
            logoRes != 0 && item.tintLogo -> Icon(
                painter = painterResource(logoRes),
                contentDescription = item.brand,
                tint = item.logoColor,
                modifier = Modifier.fillMaxSize(item.logoScale)
            )
            logoRes != 0 -> Image(
                painter = painterResource(logoRes),
                contentDescription = item.brand,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(0.72f)
            )
            else -> Text(
                text = if (size < 48.dp) item.brand.take(1) else item.brand,
                color = item.logoColor,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * (if (size < 48.dp) 0.45f else 0.19f)).sp,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
