package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cloud.Cloud
import com.example.cloud.CloudSync
import com.example.cloud.NgoInfo
import com.example.ui.theme.OnPrimaryGreen
import com.example.ui.theme.OutlineColor
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.SurfaceHighColor
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

/** True when nobody has said yes for a while, so the donor is shown other ways to get the food there. */
internal fun DonationItem.hasNoTakers(now: Long): Boolean =
    stage == 0 && createdAtMs > 0 && now - createdAtMs > NO_TAKERS_MS

/**
 * What the donor can do while a donation is on its way to an NGO, under the tracker on the home
 * screen: call or message the runner, hand the food over, find the NGO for a drop-off, put it back
 * up when the runner is late, share it when nobody has taken it, or cancel before pickup. Without an
 * account only the hand-over works, for the single-phone demo.
 */
@Composable
internal fun DonorDeliveryPanel(donation: DonationItem, now: Long, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val cloud = Cloud.enabled
    var confirmCancel by remember { mutableStateOf(false) }
    var choosingNgo by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (donation.stage) {
            0 -> {
                donation.eatByMs?.let {
                    Text(
                        "Good until ${clockText(it, now)}. NGOs stop seeing it after that.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
                if (cloud && donation.hasNoTakers(now)) {
                    if (donation.dropOff) {
                        PanelAction("Let any NGO collect it instead", Icons.Filled.Storefront) {
                            CloudSync.switchDelivery(donation.id, null)
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PanelAction("Share", Icons.Filled.Share, Modifier.weight(1f)) {
                                shareText(
                                    context,
                                    shareOfferText(donation.title, donation.servings, donation.address, donation.eatByMs, now)
                                )
                            }
                            PanelAction("Drop it off", Icons.Filled.Storefront, Modifier.weight(1f)) { choosingNgo = true }
                        }
                    }
                }
            }
            1 -> if (donation.dropOff) {
                val ngo by produceState<NgoInfo?>(null, donation.ngoId) {
                    value = if (cloud) donation.ngoId?.let { CloudSync.ngoInfo(it) } else null
                }
                ngo?.address?.ifBlank { null }?.let {
                    Text("Take it to $it", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                ContactButtons(
                    phone = ngo?.phone,
                    place = ngo?.place,
                    address = ngo?.address ?: "",
                    whatsAppText = "Hi, I am bringing my KarmaKitchen donation: ${donation.title}."
                )
                if (cloud) {
                    TextButton(onClick = {
                        // It goes back to every NGO as a normal pickup.
                        CloudSync.release(donation.id)
                        CloudSync.switchDelivery(donation.id, null)
                    }) { Text("I can't bring it after all", style = MaterialTheme.typography.labelLarge, color = PrimaryGreen) }
                }
            } else {
                val phone by produceState<String?>(null, donation.id, donation.volunteer) {
                    value = if (cloud) CloudSync.runnerPhone(donation.id) else null
                }
                ContactButtons(
                    phone = phone,
                    place = null,
                    address = "",
                    whatsAppText = "${donation.volunteer?.let { "Hi $it" } ?: "Hi"}, the KarmaKitchen donation is ready: ${donation.title}."
                )
                KarmaButton(
                    onClick = { if (cloud) CloudSync.markPickedUp(donation.id) else NgoState.markPickedUp(donation.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = OnPrimaryGreen)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("I handed it over", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
                if (cloud && runnerIsLate(donation.acceptedAtMs, donation.etaMinutes, now)) {
                    Text(
                        "${donation.volunteer ?: "The runner"} is running late. You can offer it to other NGOs instead.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    PanelAction("Offer it to other NGOs", Icons.Filled.Storefront) { CloudSync.release(donation.id) }
                }
            }
        }
        if (cloud && donation.stage <= 1) {
            TextButton(onClick = { confirmCancel = true }, modifier = Modifier.align(Alignment.End)) {
                Text("Cancel donation", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            }
        }
    }

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("Cancel this donation?") },
            text = { Text("NGOs will stop seeing it. If someone is already on the way, please call them too.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmCancel = false
                    CloudSync.cancel(donation.id)
                }) { Text("Cancel donation", color = PrimaryGreen, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Keep it") } },
            containerColor = SurfaceHighColor,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }

    if (choosingNgo) {
        DropOffDialog(
            here = donation.place,
            onPick = { ngo ->
                choosingNgo = false
                CloudSync.switchDelivery(donation.id, ngo)
            },
            onDismiss = { choosingNgo = false }
        )
    }
}

/** The nearest verified NGOs, to pick one to bring the food to. */
@Composable
private fun DropOffDialog(here: Place?, onPick: (NgoInfo) -> Unit, onDismiss: () -> Unit) {
    val ngos by produceState<List<Pair<NgoInfo, Double>>?>(null, here) {
        value = CloudSync.verifiedNgos()
            .map { ngo -> ngo to if (here != null && ngo.place != null) distanceKm(here, ngo.place) else -1.0 }
            .sortedBy { (_, km) -> if (km < 0) Double.MAX_VALUE else km }
            .take(5)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Drop it off yourself") },
        text = {
            Column {
                Text(
                    "Pick an NGO. They say yes first, then you take it over and earn $DROP_OFF_BONUS bonus coins.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(8.dp))
                val list = ngos
                when {
                    list == null -> CircularProgressIndicator(color = PrimaryGreen, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                    list.isEmpty() -> Text("No NGOs could be found. Check your internet and try again.", color = TextSecondary)
                    else -> list.forEachIndexed { index, (ngo, km) ->
                        if (index > 0) HorizontalDivider(color = OutlineColor)
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(ngo) }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(ngo.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(
                                listOfNotNull(if (km >= 0) "${distanceText(km)} away" else null, ngo.address.ifBlank { null })
                                    .joinToString(" · ")
                                    .ifBlank { "Distance not known" },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        containerColor = SurfaceHighColor,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary
    )
}

@Composable
private fun PanelAction(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    KarmaOutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, OutlineColor),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
