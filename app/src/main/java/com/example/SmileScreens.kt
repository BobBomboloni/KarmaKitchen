package com.example

import android.Manifest
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private fun formatSmileDate(millis: Long): String =
    SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(millis))

// -----------------------------------------------------------------------------
// Donor side
// -----------------------------------------------------------------------------

/** Dashboard shortcut to the Smile Wall, with the latest photos as small thumbnails. */
@Composable
fun SmileTeaserCard(onClick: () -> Unit) {
    val smiles = SmileStore.smiles.toList()
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, OutlineColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (smiles.isEmpty()) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(SecondaryAmber.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Mood, contentDescription = null, tint = SecondaryAmber)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy((-14).dp)) {
                    smiles.take(3).forEach { smile ->
                        AsyncImage(
                            model = File(smile.photoPath),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(2.dp, SurfaceColor, CircleShape)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Smile Wall", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    if (smiles.isEmpty()) "See the smiles your food creates"
                    else "${smiles.size} ${if (smiles.size == 1) "smile" else "smiles"} from people you helped",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSecondary)
        }
    }
}

@Composable
fun SmileWallScreen(donorName: String) {
    val smiles = SmileStore.smiles.toList()
    val context = LocalContext.current
    var selected by remember { mutableStateOf<SmileEntry?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(SecondaryAmber.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Mood, contentDescription = null, tint = SecondaryAmber, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text("Smile Wall", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(
                        if (donorName.isBlank()) "The smiles your food created"
                        else "Thank you, $donorName. These smiles are yours.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SmileStat("Smiles", smiles.size.toString(), Modifier.weight(1f))
                SmileStat("People fed", smiles.sumOf { it.people }.toString(), Modifier.weight(1f))
                SmileStat("NGOs", smiles.map { it.ngoName }.distinct().size.toString(), Modifier.weight(1f))
            }
        }

        if (smiles.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, OutlineColor)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Mood, contentDescription = null, tint = SecondaryAmber, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No smiles yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "When an NGO receives your donation, it can send you a photo of the people who enjoyed the food. It will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            itemsIndexed(smiles, key = { _, smile -> smile.id }) { index, smile ->
                Box(modifier = Modifier.appearOnScreen(index)) {
                    SmilePhotoCard(smile) { selected = smile }
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "NGOs share photos only with the consent of the people in them (or a parent or guardian). You can remove any photo from your wall.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }
    }

    selected?.let { entry ->
        SmileDetailDialog(
            entry = entry,
            onDismiss = { selected = null },
            onRemove = {
                SmileStore.remove(context, entry.id)
                selected = null
            }
        )
    }
}

@Composable
private fun SmileStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .border(1.dp, OutlineColor, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMono),
            fontWeight = FontWeight.Black,
            color = SecondaryAmber
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun SmilePhotoCard(entry: SmileEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, OutlineColor)
    ) {
        Column {
            AsyncImage(
                model = File(entry.photoPath),
                contentDescription = "Photo from ${entry.ngoName}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(0.85f)
            )
            Column(modifier = Modifier.padding(12.dp).heightIn(min = 96.dp)) {
                Text(
                    "“${entry.message}”",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = TextPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    entry.ngoName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SecondaryAmber,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "for your ${entry.donationTitle}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SmileDetailDialog(entry: SmileEntry, onDismiss: () -> Unit, onRemove: () -> Unit) {
    var confirmRemove by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceHighColor),
            border = BorderStroke(1.dp, OutlineColor)
        ) {
            Column {
                AsyncImage(
                    model = File(entry.photoPath),
                    contentDescription = "Photo from ${entry.ngoName}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(320.dp)
                )
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "“${entry.message}”",
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(entry.ngoName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SecondaryAmber)
                    Text(
                        "${entry.people} people fed • ${entry.donationTitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(formatSmileDate(entry.sentAt), style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { if (confirmRemove) onRemove() else confirmRemove = true }) {
                            Text(if (confirmRemove) "Tap again to remove" else "Remove photo", color = DangerColor)
                        }
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = OnPrimaryGreen)
                        ) {
                            Text("Close", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// NGO / receiver side
// -----------------------------------------------------------------------------

/** One received donation in the NGO dashboard, with the button to send a smile. */
@Composable
fun ReceivalCard(receival: Receival, onSendSmile: () -> Unit) {
    val smileSent = SmileStore.hasSmileFor(receival.id)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .border(1.dp, OutlineColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(SecondaryAmber.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Restaurant, contentDescription = null, tint = SecondaryAmber)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(receival.title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(
                    "Received ${receival.receivedText} • ${receival.servings} servings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (smileSent) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Smile sent to the donor", color = SuccessColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Button(
                onClick = onSendSmile,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryAmber, contentColor = OnSecondaryAmber)
            ) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send a Smile", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Camera with a permission prompt in front of it. */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraGate(onCaptured: (Uri) -> Unit, onCancel: () -> Unit, onError: (String) -> Unit) {
    BackHandler { onCancel() }
    val permission = rememberPermissionState(Manifest.permission.CAMERA)
    if (permission.status.isGranted) {
        CameraCapture(
            onImageCaptured = onCaptured,
            onError = { onError(it.message ?: "Camera error") },
            onClose = onCancel,
            statusLabel = "Capture a happy moment"
        )
    } else {
        LaunchedEffect(Unit) { permission.launchPermissionRequest() }
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, OutlineColor)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = SecondaryAmber, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Camera Permission Needed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "KarmaKitchen needs camera access to take a photo for the donor.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = { permission.launchPermissionRequest() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryAmber, contentColor = OnSecondaryAmber)
                        ) {
                            Text("Allow")
                        }
                    }
                }
            }
        }
    }
}

/** The NGO takes or picks a photo for one received donation and sends it to the donor. */
@Composable
fun SendSmileScreen(navController: NavController, receivalId: String) {
    val receival = sampleReceivals.firstOrNull { it.id == receivalId }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    var consent by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Thank you for the food! Everyone enjoyed it.") }
    var people by remember { mutableStateOf(receival?.servings ?: 1) }
    var isSending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var sent by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            photoUri = uri
            error = null
        }
    }

    if (receival == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text("This donation could not be found.", color = TextSecondary)
        }
        return
    }

    if (showCamera) {
        CameraGate(
            onCaptured = { uri ->
                showCamera = false
                photoUri = uri
                error = null
            },
            onCancel = { showCamera = false },
            onError = {
                showCamera = false
                error = it
            }
        )
        return
    }

    fun send() {
        val source = photoUri ?: return
        isSending = true
        error = null
        scope.launch {
            try {
                val id = UUID.randomUUID().toString()
                val path = saveSmilePhoto(context, source, id)
                SmileStore.add(
                    context,
                    SmileEntry(
                        id = id,
                        donationId = receival.id,
                        donationTitle = receival.title,
                        ngoName = NGO_NAME,
                        message = message.trim().ifBlank { "Thank you!" },
                        people = people,
                        photoPath = path,
                        sentAt = System.currentTimeMillis()
                    )
                )
                sent = true
            } catch (e: Exception) {
                error = "Could not save that photo. Please try another one."
            } finally {
                isSending = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Send a Smile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Show the donor the people their food helped.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(start = 56.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, OutlineColor)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(SecondaryAmber.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Restaurant, contentDescription = null, tint = SecondaryAmber)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(receival.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(
                        "Received ${receival.receivedText} • ${receival.servings} servings",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceVariantColor)
                .border(1.dp, OutlineColor, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            val chosen = photoUri
            if (chosen != null) {
                AsyncImage(
                    model = chosen,
                    contentDescription = "Selected photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Filled.AddAPhoto, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Add a photo of the people enjoying the food",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { showCamera = true },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryAmber, contentColor = OnSecondaryAmber)
            ) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (photoUri == null) "Take photo" else "Retake", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gallery")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = message,
            onValueChange = { message = it.take(140) },
            label = { Text("Message to the donor") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("People who enjoyed the food", style = MaterialTheme.typography.bodyLarge, color = TextPrimary, modifier = Modifier.weight(1f))
            IconButton(onClick = { if (people > 1) people-- }) {
                Icon(Icons.Filled.Remove, contentDescription = "Fewer people", tint = TextPrimary)
            }
            Text(people.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SecondaryAmber)
            IconButton(onClick = { if (people < 500) people++ }) {
                Icon(Icons.Filled.Add, contentDescription = "More people", tint = TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceVariantColor),
            border = BorderStroke(1.dp, OutlineColor)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { consent = !consent }.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = consent,
                    onCheckedChange = { consent = it },
                    colors = CheckboxDefaults.colors(checkedColor = PrimaryGreen, checkmarkColor = OnPrimaryGreen)
                )
                Column(modifier = Modifier.padding(top = 12.dp, end = 8.dp)) {
                    Text(
                        "Everyone in this photo agreed to share it with the donor.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "For children, a parent or guardian must agree. Do not photograph anyone who prefers not to appear. Location data is removed from the photo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        error?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(it, color = DangerColor, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(20.dp))

        val canSend = photoUri != null && consent && !isSending
        Button(
            onClick = { send() },
            enabled = canSend,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SecondaryAmber, contentColor = OnSecondaryAmber)
        ) {
            if (isSending) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = OnSecondaryAmber, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send to donor", fontWeight = FontWeight.Bold)
            }
        }
        if (!canSend && !isSending) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Add a photo and confirm consent to continue.",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (sent) {
        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(Icons.Filled.Mood, contentDescription = null, tint = SecondaryAmber) },
            title = { Text("Smile sent!") },
            text = { Text("The donor will see this photo on their Smile Wall.") },
            confirmButton = {
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("Done", color = PrimaryGreen, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SurfaceHighColor,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}
