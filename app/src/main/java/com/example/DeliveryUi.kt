package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OutlineColor
import com.example.ui.theme.PrimaryGreen
import kotlinx.coroutines.delay

/** The current time, updated every [everyMs], so countdowns and "running late" move on their own. */
@Composable
fun rememberNow(everyMs: Long = 30_000L): State<Long> = produceState(System.currentTimeMillis()) {
    while (true) {
        delay(everyMs)
        value = System.currentTimeMillis()
    }
}

/** Opens a link in whichever app handles it (Maps, WhatsApp, the browser). */
fun openLink(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { Log.w("DeliveryUi", "Nothing on this phone can open $url", it) }
}

/** Opens the dialler with [phone] filled in. The person still taps call themselves. */
fun dial(context: Context, phone: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.trim()}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { Log.w("DeliveryUi", "Could not open the dialler", it) }
}

/** The phone's share sheet, for example to post an offer in a WhatsApp group of local NGOs. */
fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    runCatching { context.startActivity(Intent.createChooser(send, "Share with NGOs").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { Log.w("DeliveryUi", "Could not open the share sheet", it) }
}

/**
 * Call, WhatsApp and Directions for the other side of a pickup. Each one only shows when there is
 * something to open: a number for the first two, a spot or an address for directions.
 */
@Composable
fun ContactButtons(
    phone: String?,
    place: Place?,
    address: String,
    whatsAppText: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val number = phone?.trim()?.ifBlank { null }
    val chat = number?.let { whatsAppUrl(it, whatsAppText) }
    val directions = directionsUrl(place, address)
    if (number == null && directions == null) return
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (number != null) {
            ContactButton(Icons.Filled.Call, "Call", Modifier.weight(1f)) { dial(context, number) }
        }
        if (chat != null) {
            ContactButton(Icons.Filled.Sms, "WhatsApp", Modifier.weight(1f)) { openLink(context, chat) }
        }
        if (directions != null) {
            ContactButton(Icons.Filled.Navigation, "Directions", Modifier.weight(1f)) { openLink(context, directions) }
        }
    }
}

@Composable
private fun ContactButton(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    KarmaOutlinedButton(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, OutlineColor),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen),
        contentPadding = ButtonDefaults.TextButtonContentPadding
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
