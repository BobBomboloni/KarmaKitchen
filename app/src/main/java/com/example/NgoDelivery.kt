package com.example

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cloud.Account
import com.example.cloud.Cloud
import com.example.cloud.CloudSync
import com.example.cloud.KitchenPhone
import com.example.cloud.currentNgoName
import com.example.ui.theme.DangerColor
import com.example.ui.theme.OnPrimaryGreen
import com.example.ui.theme.OnPrimaryGreenContainer
import com.example.ui.theme.OutlineColor
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.PrimaryGreenLight
import com.example.ui.theme.SurfaceHighColor
import com.example.ui.theme.SurfaceVariantColor
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch

/** "Eat by 9:30 PM · 2 h 10 min left", or null when the donation has no eat-by time. */
internal fun eatByText(donation: NgoDonation, now: Long): String? =
    donation.eatByMs?.let { "Eat by ${clockText(it, now)} · ${timeLeftText(it, now)} left" }

/**
 * The NGO says who is going to collect a pickup, their number (only the donor sees it) and roughly
 * when they will get there. The last runner is filled in, since it is usually the same person.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AcceptSheet(donation: NgoDonation, onConfirm: (runner: String, phone: String, eta: Int) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val saved = remember { loadRunner(context) }
    // Without an account (the single-phone demo) one of the sample volunteers is suggested.
    var name by remember {
        mutableStateOf(saved.first.ifBlank { Account.user?.name ?: if (Cloud.enabled) "" else volunteerFor(donation.id) })
    }
    var phone by remember { mutableStateOf(saved.second.ifBlank { Account.user?.phone ?: "" }) }
    val suggested = remember(donation.id) { suggestedEta(donation.distanceKm) }
    val choices = remember(suggested) { (listOf(15, 30, 45, 60) + suggested).distinct().sorted() }
    var eta by remember { mutableStateOf(suggested) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceHighColor,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            Text("Who is collecting it?", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Text(
                "${donation.title} from ${donation.donor}. They will see this name and can call this number.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(60) },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.take(20) },
                label = { Text("Phone") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("They will get there in about", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                choices.forEach { minutes ->
                    val selected = minutes == eta
                    Text(
                        "$minutes min",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) OnPrimaryGreenContainer else TextPrimary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (selected) PrimaryGreenLight else SurfaceVariantColor)
                            .border(1.dp, if (selected) PrimaryGreen else Color.Transparent, CircleShape)
                            .clickable { eta = minutes }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
            if (donation.distanceKm >= 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "${distanceText(donation.distanceKm)} away in a straight line.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
            Spacer(Modifier.height(24.dp))
            KarmaButton(
                onClick = {
                    saveRunner(context, name.trim(), phone.trim())
                    onConfirm(name.trim(), phone.trim(), eta)
                },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = OnPrimaryGreen)
            ) { Text("Accept and tell the donor", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold) }
        }
    }
}

/**
 * Accepted but not picked up yet. For a pickup the runner taps "Picked up" with the food in hand;
 * for a drop-off the NGO logs it when the donor arrives. Either way it can be put back up for
 * other NGOs if it falls through.
 */
@Composable
internal fun AcceptedCard(
    donation: NgoDonation,
    now: Long,
    onPickedUp: () -> Unit,
    onLogIntake: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmRelease by remember { mutableStateOf(false) }
    val eta = minutesUntilArrival(donation.acceptedAtMs, donation.etaMinutes, now)
    NgoCard(modifier) {
        DonationPhoto(donation, height = 120.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryTile(donation.category)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(donation.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    if (donation.dropOff) "${donation.donor} is bringing it to you" else "${donation.volunteer ?: "Your runner"} is collecting it",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            if (eta != null && !donation.dropOff) Column(horizontalAlignment = Alignment.End) {
                Text("$eta", style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"), color = TextPrimary)
                Text("min", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        Spacer(Modifier.height(10.dp))
        InfoChip(Icons.Filled.Schedule, eatByText(donation, now) ?: donation.pickup)
        if (!donation.dropOff && donation.address.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text("Pickup at ${donation.address}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        if (donation.note.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text("Note: ${donation.note}", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        }
        DonorContact(donation, withDirections = !donation.dropOff, modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(14.dp))
        DeliverySteps(listOf("Accepted", "Picked up", "Received"), 0)
        Spacer(Modifier.height(14.dp))
        KarmaButton(
            onClick = if (donation.dropOff) onLogIntake else onPickedUp,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = OnPrimaryGreen)
        ) {
            Icon(if (donation.dropOff) Icons.Filled.CameraAlt else Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (donation.dropOff) "It arrived: log intake" else "Picked up",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        TextButton(onClick = { confirmRelease = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                if (donation.dropOff) "We can't take it after all" else "We can't collect it",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
        }
    }
    if (confirmRelease) {
        AlertDialog(
            onDismissRequest = { confirmRelease = false },
            title = { Text("Put it back up?") },
            text = { Text("Every NGO nearby will see the offer again, and ${donation.donor} will see it is waiting for someone else.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRelease = false
                    onRelease()
                }) { Text("Put it back up", color = PrimaryGreen, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { confirmRelease = false }) { Text("Keep it") } },
            containerColor = SurfaceHighColor,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}

/**
 * Call and WhatsApp the donor, and directions to the pickup. Only the NGO that accepted can read
 * the donor's number, so it is fetched once the donation is theirs.
 */
@Composable
internal fun DonorContact(donation: NgoDonation, modifier: Modifier = Modifier, withDirections: Boolean = true) {
    val phone by produceState(donation.donorPhone.ifBlank { null }, donation.id) {
        if (Cloud.enabled) value = CloudSync.donorPhone(donation.id) ?: value
    }
    ContactButtons(
        phone = phone,
        place = if (withDirections) donation.place else null,
        address = if (withDirections) donation.address else "",
        whatsAppText = "Hi, this is ${currentNgoName()} about your KarmaKitchen donation: ${donation.title}.",
        modifier = modifier
    )
}

/**
 * Asks the NGO to save where it is, from this phone's location while someone is at the NGO. Offers
 * then show how far away they are, nearest first, and donors can find the NGO to drop food off.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun NgoLocationCard(onSaved: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val permissions = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )
    val granted = permissions.permissions.any { it.status.isGranted }
    var busy by remember { mutableStateOf(false) }
    var asked by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun save() {
        busy = true
        error = null
        scope.launch {
            val place = phonePlace(context)
            if (place == null) {
                error = "Could not find this phone's location. Turn on location and try again."
            } else {
                try {
                    Account.saveNgoLocation(place, addressOf(context, place) ?: "")
                    onSaved()
                } catch (e: Exception) {
                    error = e.message ?: "Could not save the location. Please try again."
                }
            }
            busy = false
        }
    }

    LaunchedEffect(granted) {
        if (granted && asked) {
            asked = false
            save()
        }
    }

    NgoCard(modifier) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.MyLocation, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Where is your NGO?", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    "Tap this while you are at the NGO. Offers will show how far away they are, nearest first, and donors nearby can bring food to you.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = DangerColor)
        }
        Spacer(Modifier.height(12.dp))
        KarmaButton(
            onClick = {
                if (granted) {
                    save()
                } else {
                    asked = true
                    permissions.launchMultiplePermissionRequest()
                }
            },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = OnPrimaryGreen)
        ) {
            if (busy) {
                CircularProgressIndicator(color = OnPrimaryGreen, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Text("Use this phone's location", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Turns the "kitchen phone" alerts on or off. Meant for the phone that stays at the NGO: it keeps
 * listening while the app is closed and rings for each new offer.
 */
@Composable
internal fun KitchenPhoneCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { KitchenPhone.load(context) }
    val askToNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        if (allowed) KitchenPhone.turnOn(context)
    }
    NgoCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.NotificationsActive,
                contentDescription = null,
                tint = if (KitchenPhone.on) PrimaryGreen else TextTertiary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Ring for new offers", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    if (KitchenPhone.on) {
                        "This phone rings for each new offer, even with the app closed. After a restart, open the app once to turn it back on."
                    } else {
                        "Turn this on for the phone that stays at your kitchen."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = KitchenPhone.on,
                onCheckedChange = { on ->
                    when {
                        !on -> KitchenPhone.turnOff(context)
                        Build.VERSION.SDK_INT >= 33 && !KitchenPhone.hasNotificationPermission(context) ->
                            askToNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else -> KitchenPhone.turnOn(context)
                    }
                },
                colors = SwitchDefaults.colors(checkedThumbColor = OnPrimaryGreen, checkedTrackColor = PrimaryGreen, uncheckedBorderColor = OutlineColor)
            )
        }
    }
}
