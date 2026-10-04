package com.example

import android.Manifest
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.api.FoodAnalysisResult
import com.example.api.analyzeFoodWithGemini
import com.example.api.uriToBase64
import com.example.ui.DonationChatbot
import com.example.ui.ScannerAnimation
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.DangerColor
import com.example.ui.theme.DangerContainer
import com.example.ui.theme.InfoContainer
import com.example.ui.theme.MealsCardBg
import com.example.ui.theme.MealsTextPrimary
import com.example.ui.theme.NonVegColor
import com.example.ui.theme.NonVegContainer
import com.example.ui.theme.OnDangerContainer
import com.example.ui.theme.OnPrimaryGreen
import com.example.ui.theme.OnPrimaryGreenContainer
import com.example.ui.theme.OutlineColor
import com.example.ui.theme.OutlineStrong
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.PrimaryGreenLight
import com.example.ui.theme.SecondaryAmber
import com.example.ui.theme.SurfaceColor
import com.example.ui.theme.SurfaceVariantColor
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

/** When the donor would like the food collected. */
enum class PickupWindow(val label: String, val hint: String) {
    Soon("As soon as possible", "A volunteer should arrive in about 25 minutes"),
    Hour("In about an hour", "Volunteers will be asked to come around then"),
    Evening("Tonight, 7 to 9 PM", "Good for food that is made in the afternoon")
}

/** Everything the donor has entered so far, kept in one place so the steps can share it. */
@Stable
private class DonationDraft {
    var photo by mutableStateOf<Uri?>(null)
    var analysis by mutableStateOf<FoodAnalysisResult?>(null)
    var analyzing by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    var title by mutableStateOf("")
    var servings by mutableStateOf("")
    var isVeg by mutableStateOf(true)
    var shelfLife by mutableStateOf("")
    var quality by mutableStateOf("")
    var storageTip by mutableStateOf("")

    var window by mutableStateOf(PickupWindow.Soon)
    var note by mutableStateOf("")
    var address by mutableStateOf("")
    var addressEdited by mutableStateOf(false)

    val coins: Int get() = calculateKarmaPoints(servings, quality)

    fun clearScan() {
        photo = null
        analysis = null
        analyzing = false
        error = null
    }

    fun reset() {
        clearScan()
        title = ""
        servings = ""
        isVeg = true
        shelfLife = ""
        quality = ""
        storageTip = ""
        window = PickupWindow.Soon
        note = ""
        address = ""
        addressEdited = false
    }
}

private suspend fun resolveAddress(context: Context, fallback: String): String {
    return try {
        val client = LocationServices.getFusedLocationProviderClient(context)
        @Suppress("MissingPermission")
        val location: Location? = client.lastLocation.await()
        if (location != null) {
            val addresses = Geocoder(context, Locale.getDefault()).getFromLocation(location.latitude, location.longitude, 1)
            addresses?.firstOrNull()?.getAddressLine(0) ?: "${location.latitude}, ${location.longitude}"
        } else {
            fallback
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        fallback
    }
}

// -----------------------------------------------------------------------------
// Screen
// -----------------------------------------------------------------------------

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DonationCreationScreen(navController: NavController, userProfile: UserProfile, onProfileUpdate: (UserProfile) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )

    val draft = remember { DonationDraft() }
    var step by remember { mutableStateOf(1) }
    var showCamera by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }

    // Fill in the pickup address from the phone's location when the pickup step opens.
    LaunchedEffect(step, locationPermissions.allPermissionsGranted) {
        if (step == 3 && !draft.addressEdited && draft.address.isBlank()) {
            if (!locationPermissions.allPermissionsGranted) {
                locationPermissions.launchMultiplePermissionRequest()
            } else {
                locating = true
                draft.address = resolveAddress(context, userProfile.address)
                locating = false
            }
        }
    }

    fun analyzeUri(uri: Uri) {
        draft.photo = uri
        draft.analysis = null
        draft.analyzing = true
        draft.error = null
        scope.launch {
            try {
                val base64 = uriToBase64(context, uri)
                val result = analyzeFoodWithGemini(base64)
                draft.analysis = result
                draft.title = result.title
                draft.servings = result.quantity
                draft.isVeg = result.isVeg
                draft.quality = result.quality
                draft.shelfLife = result.shelfLife
                draft.storageTip = result.storageTip
            } catch (e: Exception) {
                draft.error = e.message ?: "Failed to analyze food with Gemini AI."
            } finally {
                draft.analyzing = false
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) analyzeUri(uri)
    }

    BackHandler(enabled = step in 2..3) { step -= 1 }

    if (showCamera) {
        if (cameraPermissionState.status.isGranted) {
            CameraCapture(
                onImageCaptured = { uri ->
                    showCamera = false
                    analyzeUri(uri)
                },
                onError = {
                    showCamera = false
                    draft.error = "Camera error: ${it.message}"
                },
                onClose = { showCamera = false }
            )
        } else {
            LaunchedEffect(Unit) { cameraPermissionState.launchPermissionRequest() }
            CameraPermissionPrompt(
                onCancel = { showCamera = false },
                onAllow = { cameraPermissionState.launchPermissionRequest() }
            )
        }
        return
    }

    if (step == 4) {
        DonationSuccess(
            draft = draft,
            onHome = { navController.goToTab(Screen.Dashboard.route) },
            onAnother = {
                draft.reset()
                step = 1
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DonateHeader(
            step = step,
            onBack = {
                if (step > 1) step -= 1 else navController.goToTab(Screen.Dashboard.route)
            }
        )
        AnimatedContent(
            targetState = step,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                val forward = targetState > initialState
                (fadeIn(tween(250)) + slideInHorizontally(tween(300)) { if (forward) it / 8 else -it / 8 }) togetherWith
                    (fadeOut(tween(150)) + slideOutHorizontally(tween(300)) { if (forward) -it / 8 else it / 8 })
            },
            label = "donationStep"
        ) { current ->
            when (current) {
                1 -> PhotoStep(
                    draft = draft,
                    onTakePhoto = { showCamera = true },
                    onPickGallery = { galleryLauncher.launch("image/*") },
                    onRescan = {
                        draft.clearScan()
                        showCamera = true
                    },
                    onContinue = { step = 2 }
                )
                2 -> DetailsStep(draft = draft, onBack = { step = 1 }, onNext = { step = 3 })
                else -> PickupStep(
                    draft = draft,
                    locating = locating,
                    onBack = { step = 2 },
                    onConfirm = {
                        val id = java.util.UUID.randomUUID().toString()
                        val category = guessCategory(draft.title)
                        DonationLog.add(
                            DonationItem(
                                title = draft.title.trim(),
                                ngo = "Nearby NGOs",
                                date = "Just now",
                                status = STATUS_POSTED,
                                points = draft.coins,
                                category = category,
                                etaMinutes = 25,
                                stage = 0,
                                id = id
                            )
                        )
                        // The receiver side (another role on this phone) sees it as a new offer.
                        NgoState.post(
                            NgoDonation(
                                id = id,
                                donor = userProfile.name.trim().ifBlank { "A donor" },
                                title = draft.title.trim(),
                                servings = servingsCount(draft.servings),
                                isVeg = draft.isVeg,
                                shelfLife = draft.shelfLife.trim(),
                                distanceKm = 1.4,
                                pickup = draft.window.label,
                                category = category,
                                stage = OfferStage.Offered,
                                note = draft.note.trim(),
                                photo = draft.photo?.toString(),
                                fromDonor = true
                            )
                        )
                        step = 4
                    }
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Header, stepper and shared pieces
// -----------------------------------------------------------------------------

@Composable
private fun DonateHeader(step: Int, onBack: () -> Unit) {
    Column(modifier = Modifier.padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    if (step > 1) Icons.Filled.ArrowBack else Icons.Filled.Close,
                    contentDescription = if (step > 1) "Back" else "Close",
                    tint = TextPrimary
                )
            }
            Text(
                "Donate food",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                color = TextPrimary
            )
        }
        Spacer(Modifier.height(8.dp))
        DonateStepper(step, Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun DonateStepper(step: Int, modifier: Modifier = Modifier) {
    val labels = listOf("Photo", "Details", "Pickup")
    Row(modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            val number = index + 1
            val done = step > number
            val current = step == number
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepLine(visible = index > 0, filled = step >= number, modifier = Modifier.weight(1f))
                    StepBubble(number = number, done = done, current = current)
                    StepLine(visible = index < labels.lastIndex, filled = step > number, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (done || current) TextPrimary else TextTertiary
                )
            }
        }
    }
}

@Composable
private fun StepLine(visible: Boolean, filled: Boolean, modifier: Modifier = Modifier) {
    val color by animateColorAsState(
        when {
            !visible -> Color.Transparent
            filled -> PrimaryGreen
            else -> OutlineColor
        },
        tween(300),
        label = "stepLine"
    )
    Box(modifier = modifier.height(3.dp).background(color))
}

@Composable
private fun StepBubble(number: Int, done: Boolean, current: Boolean) {
    val fill by animateColorAsState(
        if (done) PrimaryGreen else SurfaceVariantColor,
        tween(300),
        label = "bubbleFill"
    )
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(fill)
            .then(if (current) Modifier.border(2.dp, PrimaryGreen, CircleShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (done) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = OnPrimaryGreen, modifier = Modifier.size(16.dp))
        } else {
            Text(
                "$number",
                style = MaterialTheme.typography.labelMedium,
                color = if (current) PrimaryGreen else TextTertiary
            )
        }
    }
}

@Composable
private fun StepTitle(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

/** Bottom bar that stays put while the page above it scrolls. */
@Composable
private fun ActionBar(content: @Composable RowScope.() -> Unit) {
    Column {
        HorizontalDivider(color = OutlineColor)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
private fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    KarmaButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = OnPrimaryGreen)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SecondaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    KarmaOutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** The small square veg / non-veg mark that food apps use. */
@Composable
fun VegMark(isVeg: Boolean, size: Dp = 16.dp) {
    val color = if (isVeg) PrimaryGreen else NonVegColor
    Box(
        modifier = Modifier
            .size(size)
            .border(1.5.dp, color, RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.45f)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
private fun CameraPermissionPrompt(onCancel: () -> Unit, onAllow: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceColor)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(painterResource(R.drawable.illus_step_camera), contentDescription = null, modifier = Modifier.size(88.dp))
                Spacer(Modifier.height(12.dp))
                Text("Camera access needed", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Text(
                    "KarmaKitchen needs the camera to check how fresh your food is and how long it will keep.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryAction("Cancel", onCancel, Modifier.weight(1f))
                    PrimaryAction("Allow", onAllow, Modifier.weight(1f))
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Step 1: photo and AI check
// -----------------------------------------------------------------------------

@Composable
private fun PhotoStep(
    draft: DonationDraft,
    onTakePhoto: () -> Unit,
    onPickGallery: () -> Unit,
    onRescan: () -> Unit,
    onContinue: () -> Unit
) {
    val result = draft.analysis
    val photo = draft.photo
    var chatOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StepTitle("Check the food", "We estimate the quantity, freshness and shelf life from your photo.")
            }

            when {
                draft.analyzing -> {
                    item { AnalyzingCard(photo) }
                    item { ResultSkeleton() }
                }

                result != null -> {
                    item { ResultPhoto(photo, result, onRetake = onRescan) }
                    item { ResultHeader(result) }
                    item { ResultStats(result) }
                    item { if (result.isSafeToDonate) StorageTip(result.storageTip) else UnsafeNotice(result.rejectionReason) }
                }

                else -> {
                    item {
                        if (photo == null) ViewfinderCard(onTakePhoto) else PhotoPreview(photo)
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            PrimaryAction(
                                text = if (photo == null) "Take a photo" else "Retake photo",
                                onClick = onTakePhoto,
                                modifier = Modifier.fillMaxWidth()
                            )
                            SecondaryAction("Choose from gallery", onPickGallery, Modifier.fillMaxWidth())
                        }
                    }
                    if (photo == null) {
                        item { TipsCard() }
                        item { AcceptedFoodCard() }
                    }
                }
            }

            draft.error?.let { message -> item { ErrorCard(message) } }

            item { SafetyChat(open = chatOpen, onToggle = { chatOpen = !chatOpen }) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Powered by Gemini", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                }
            }
        }

        if (result != null) {
            ActionBar {
                if (result.isSafeToDonate) {
                    PrimaryAction("Looks right, continue", onContinue, Modifier.weight(1f))
                } else {
                    PrimaryAction("Scan something else", onRescan, Modifier.weight(1f))
                }
            }
        }
    }
}

/** Empty state: a viewfinder with the camera drawing; the whole card opens the camera. */
@Composable
private fun ViewfinderCard(onClick: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "cameraPulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "cameraScale"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .bounceCard(RoundedCornerShape(24.dp), onClick, SurfaceColor),
        contentAlignment = Alignment.Center
    ) {
        ViewfinderCorners(PrimaryGreen.copy(alpha = 0.85f), Modifier.fillMaxSize())
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.illus_step_camera),
                contentDescription = null,
                modifier = Modifier
                    .size(104.dp)
                    .scale(scale)
            )
            Spacer(Modifier.height(8.dp))
            Text("Tap to open the camera", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text("Put the dish or packet in the frame", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
internal fun ViewfinderCorners(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val length = 30.dp.toPx()
        val inset = 16.dp.toPx()
        val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        fun corner(x: Float, y: Float, dx: Float, dy: Float) {
            val path = Path().apply {
                moveTo(x + dx * length, y)
                lineTo(x, y)
                lineTo(x, y + dy * length)
            }
            drawPath(path, color, style = stroke)
        }
        corner(inset, inset, 1f, 1f)
        corner(size.width - inset, inset, -1f, 1f)
        corner(inset, size.height - inset, 1f, -1f)
        corner(size.width - inset, size.height - inset, -1f, -1f)
    }
}

@Composable
private fun PhotoPreview(photo: Uri) {
    AsyncImage(
        model = photo,
        contentDescription = "Your photo",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(24.dp))
    )
}

@Composable
private fun TipsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("For a good scan", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        listOf(
            "Use good light and avoid flash glare",
            "Keep the whole dish or packet in the frame",
            "Take the lid or foil off first"
        ).forEach { tip ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                Text(tip, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun AcceptedFoodCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .padding(vertical = 16.dp)
    ) {
        Text(
            "What we can take",
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FoodCategory.entries.toList(), key = { it.name }) { category ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(category.tint().copy(alpha = 0.16f))
                        .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(painterResource(category.art()), contentDescription = null, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(category.label, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "We can't take spoiled food, opened leftovers from plates or raw meat.",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun AnalyzingCard(photo: Uri?) {
    val messages = listOf(
        "Looking at the dish",
        "Checking how fresh it is",
        "Estimating the servings",
        "Working out the shelf life"
    )
    var index by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1400)
            index = (index + 1) % messages.size
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceVariantColor)
    ) {
        if (photo != null) {
            AsyncImage(
                model = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundColor.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            ScannerAnimation(modifier = Modifier.fillMaxSize())
        }
        Text(
            text = messages[index] + "...",
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(BackgroundColor.copy(alpha = 0.75f))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

/** Grey blocks in the shape of the result, shimmering while the AI is still looking at the photo. */
@Composable
internal fun ResultSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PlaceholderBlock(Modifier.fillMaxWidth(0.55f).height(24.dp))
        PlaceholderBlock(Modifier.fillMaxWidth(0.85f).height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) { PlaceholderBlock(Modifier.weight(1f).height(68.dp), corner = 14.dp) }
        }
    }
}

@Composable
private fun PlaceholderBlock(modifier: Modifier, corner: androidx.compose.ui.unit.Dp = 8.dp) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(SurfaceVariantColor)
            .shimmer()
    )
}

@Composable
private fun ResultPhoto(photo: Uri?, result: FoodAnalysisResult, onRetake: () -> Unit) {
    val safe = result.isSafeToDonate
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceVariantColor)
    ) {
        if (photo != null) {
            AsyncImage(
                model = photo,
                contentDescription = "Your photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (safe) PrimaryGreen else DangerColor)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (safe) Icons.Filled.Check else Icons.Filled.Block,
                contentDescription = null,
                tint = if (safe) OnPrimaryGreen else BackgroundColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                if (safe) "Safe to donate" else "Not safe to donate",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (safe) OnPrimaryGreen else BackgroundColor
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(BackgroundColor.copy(alpha = 0.75f))
                .clickable(onClick = onRetake)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("Retake", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
        }
    }
}

@Composable
private fun ResultHeader(result: FoodAnalysisResult) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            result.title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        VegMark(result.isVeg, size = 18.dp)
        Spacer(Modifier.width(6.dp))
        Text(
            if (result.isVeg) "Veg" else "Non-veg",
            style = MaterialTheme.typography.labelMedium,
            color = if (result.isVeg) PrimaryGreen else NonVegColor
        )
    }
}

@Composable
private fun ResultStats(result: FoodAnalysisResult) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("Quantity", result.quantity, Modifier.weight(1f))
        StatTile("Shelf life", result.shelfLife, Modifier.weight(1f))
        StatTile(
            "Quality",
            result.quality,
            Modifier.weight(1f),
            valueColor = if (result.isSafeToDonate) PrimaryGreen else DangerColor
        )
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = TextPrimary) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceColor)
            .padding(12.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            color = valueColor,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StorageTip(tip: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceVariantColor)
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Filled.TipsAndUpdates, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Storage advice", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Text(tip, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        }
    }
}

@Composable
private fun UnsafeNotice(reason: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DangerContainer)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Dangerous, contentDescription = null, tint = DangerColor, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text("This food can't be donated", style = MaterialTheme.typography.titleMedium, color = OnDangerContainer)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            reason ?: "It looks spoiled, expired or unsafe to eat, so it can't be accepted.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Please compost it or dispose of it safely.",
            style = MaterialTheme.typography.bodySmall,
            color = OnDangerContainer,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ErrorCard(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DangerContainer)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = DangerColor)
        Spacer(Modifier.width(10.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = OnDangerContainer, modifier = Modifier.weight(1f))
    }
}

/** The food-safety chat sits behind a tap so it does not push the page down. */
@Composable
private fun SafetyChat(open: Boolean, onToggle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Not sure it is safe to give?", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text("Ask the food safety assistant", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Icon(
                if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = TextSecondary
            )
        }
        AnimatedVisibility(
            visible = open,
            enter = expandVertically(tween(250)) + fadeIn(tween(250)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
        ) {
            Box(Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)) {
                DonationChatbot()
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Step 2: details
// -----------------------------------------------------------------------------

@Composable
private fun DetailsStep(draft: DonationDraft, onBack: () -> Unit, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                StepTitle("Review the details", "Gemini filled these in from your photo. Change anything that looks off.")
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceColor)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val photo = draft.photo
                    if (photo != null) {
                        AsyncImage(
                            model = photo,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            draft.title.ifBlank { "Your food" },
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Verified, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Checked by AI", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }

            item {
                FormSection("About the food") {
                    OutlinedTextField(
                        value = draft.title,
                        onValueChange = { draft.title = it },
                        label = { Text("Food name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = draft.servings,
                            onValueChange = { draft.servings = it },
                            label = { Text("Servings") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledTonalIconButton(
                            onClick = { draft.servings = adjustServings(draft.servings, -1) },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = SurfaceVariantColor, contentColor = TextPrimary)
                        ) { Icon(Icons.Filled.Remove, contentDescription = "Fewer servings") }
                        Spacer(Modifier.width(4.dp))
                        FilledTonalIconButton(
                            onClick = { draft.servings = adjustServings(draft.servings, 1) },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = SurfaceVariantColor, contentColor = TextPrimary)
                        ) { Icon(Icons.Filled.Add, contentDescription = "More servings") }
                    }
                    OutlinedTextField(
                        value = draft.shelfLife,
                        onValueChange = { draft.shelfLife = it },
                        label = { Text("Safe to eat for (for example 6 hours)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }

            item {
                FormSection("Dietary preference") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ChoiceTile(
                            selected = draft.isVeg,
                            label = "Veg",
                            accent = PrimaryGreen,
                            selectedBackground = PrimaryGreenLight,
                            leading = { VegMark(true) },
                            onClick = { draft.isVeg = true },
                            modifier = Modifier.weight(1f)
                        )
                        ChoiceTile(
                            selected = !draft.isVeg,
                            label = "Non-veg",
                            accent = NonVegColor,
                            selectedBackground = NonVegContainer,
                            leading = { VegMark(false) },
                            onClick = { draft.isVeg = false },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                FormSection("When should we collect it?") {
                    PickupWindow.entries.forEach { option ->
                        OptionRow(
                            selected = draft.window == option,
                            title = option.label,
                            subtitle = option.hint,
                            onClick = { draft.window = option }
                        )
                    }
                }
            }

            item {
                FormSection("Note for the volunteer") {
                    OutlinedTextField(
                        value = draft.note,
                        onValueChange = { draft.note = it },
                        label = { Text("Optional") },
                        placeholder = { Text("Gate code, floor or a landmark") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }
        }

        ActionBar {
            SecondaryAction("Back", onBack, Modifier.weight(1f))
            PrimaryAction(
                text = "Next: pickup",
                onClick = onNext,
                modifier = Modifier.weight(2f),
                enabled = draft.title.isNotBlank() && draft.servings.isNotBlank()
            )
        }
    }
}

@Composable
private fun FormSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        content()
    }
}

@Composable
private fun ChoiceTile(
    selected: Boolean,
    label: String,
    accent: Color,
    selectedBackground: Color,
    leading: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background by animateColorAsState(if (selected) selectedBackground else SurfaceColor, tween(200), label = "choiceBg")
    val border by animateColorAsState(if (selected) accent else OutlineColor, tween(200), label = "choiceBorder")
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .border(1.5.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        leading()
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) accent else TextPrimary)
    }
}

@Composable
private fun OptionRow(selected: Boolean, title: String, subtitle: String, onClick: () -> Unit) {
    val border by animateColorAsState(if (selected) PrimaryGreen else OutlineColor, tween(200), label = "optionBorder")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) PrimaryGreenLight.copy(alpha = 0.5f) else SurfaceColor)
            .border(1.5.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) PrimaryGreen else OutlineStrong, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(PrimaryGreen)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

// -----------------------------------------------------------------------------
// Step 3: pickup
// -----------------------------------------------------------------------------

@Composable
private fun PickupStep(draft: DonationDraft, locating: Boolean, onBack: () -> Unit, onConfirm: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StepTitle("Confirm the pickup", "Volunteers and NGO partners will collect the food from this address.")
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceColor)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(InfoContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.illus_step_map),
                            contentDescription = null,
                            modifier = Modifier.size(108.dp)
                        )
                    }
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = draft.address,
                            onValueChange = {
                                draft.address = it
                                draft.addressEdited = true
                            },
                            label = { Text("Pickup address") },
                            placeholder = { Text("Type where we should collect it") },
                            leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null, tint = PrimaryGreen) },
                            trailingIcon = {
                                if (locating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = PrimaryGreen
                                    )
                                }
                            },
                            minLines = 2,
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(R.drawable.illus_step_pickup), contentDescription = null, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(draft.window.hint, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }

            item { DonationSummary(draft) }

            item { CoinsCard(draft) }
        }

        ActionBar {
            SecondaryAction("Back", onBack, Modifier.weight(1f))
            PrimaryAction(
                text = "Confirm pickup",
                onClick = onConfirm,
                modifier = Modifier.weight(2f),
                enabled = draft.address.isNotBlank()
            )
        }
    }
}

@Composable
private fun DonationSummary(draft: DonationDraft) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        val photo = draft.photo
        if (photo != null) {
            AsyncImage(
                model = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Your donation", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                VegMark(draft.isVeg)
                Spacer(Modifier.width(8.dp))
                Text(
                    draft.title.trim(),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                "${draft.servings.trim()} · safe for ${draft.shelfLife.trim().ifBlank { "a few hours" }}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(draft.window.label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            if (draft.note.isNotBlank()) {
                Text("Note: ${draft.note.trim()}", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            }
        }
    }
}

@Composable
private fun CoinsCard(draft: DonationDraft) {
    val perServing = karmaPerServing(draft.quality)
    val counted = countedServings(draft.servings)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MealsCardBg)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KarmaCoin(size = 64.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("You will earn", style = MaterialTheme.typography.labelMedium, color = MealsTextPrimary)
            Text(
                text = "+${draft.coins}",
                style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                "$counted servings × $perServing coins each. They unlock when the NGO confirms it received the food.",
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary.copy(alpha = 0.8f)
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Success
// -----------------------------------------------------------------------------

@Composable
private fun DonationSuccess(draft: DonationDraft, onHome: () -> Unit, onAnother: () -> Unit) {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(150.dp).scale(pop.value), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.illus_step_pickup), contentDescription = null, modifier = Modifier.size(150.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrimaryGreen)
                    .border(3.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = OnPrimaryGreen, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Donation scheduled", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(
            "Nearby NGOs can see your ${draft.title.trim().ifBlank { "food" }} now.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(MealsCardBg)
                .padding(start = 16.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KarmaAmount(
                amount = "+${draft.coins} reserved",
                style = MaterialTheme.typography.titleMedium,
                color = MealsTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(24.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceColor)
                .padding(16.dp)
        ) {
            Text("What happens next", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(Modifier.height(12.dp))
            TimelineRow("An NGO accepts it", "Usually within a few minutes", first = true, last = false, current = true)
            TimelineRow("A volunteer picks it up", draft.window.hint, first = false, last = false, current = false)
            TimelineRow("Your coins unlock", "As soon as the NGO confirms it received the food", first = false, last = true, current = false)
        }

        Spacer(Modifier.height(24.dp))
        PrimaryAction("Back to home", onHome, Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onAnother) {
            Text("Donate something else", color = PrimaryGreen)
        }
    }
}

@Composable
private fun TimelineRow(title: String, body: String, first: Boolean, last: Boolean, current: Boolean) {
    Row(Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (current) PrimaryGreen else SurfaceVariantColor)
                    .then(if (current) Modifier else Modifier.border(2.dp, OutlineStrong, CircleShape))
            )
            if (!last) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .background(OutlineColor)
                )
            }
        }
        Column(Modifier.padding(bottom = if (last) 0.dp else 8.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = if (current || first) TextPrimary else TextSecondary
            )
            Text(body, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}
