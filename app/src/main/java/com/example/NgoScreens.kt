package com.example

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.api.bitmapToBase64
import com.example.api.verifyIntakeWithGemini
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.DangerColor
import com.example.ui.theme.DangerContainer
import com.example.ui.theme.InfoColor
import com.example.ui.theme.MealsCardBg
import com.example.ui.theme.MealsTextPrimary
import com.example.ui.theme.OnDangerContainer
import com.example.ui.theme.OnPrimaryGreen
import com.example.ui.theme.OnPrimaryGreenContainer
import com.example.ui.theme.OnSecondaryAmber
import com.example.ui.theme.OutlineColor
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.PrimaryGreenLight
import com.example.ui.theme.SecondaryAmber
import com.example.ui.theme.SurfaceColor
import com.example.ui.theme.SurfaceHighColor
import com.example.ui.theme.SurfaceVariantColor
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Gutter = 20.dp

/** Meals received on each of the last seven days; the last one is today. */
private val weeklyMeals = listOf(220, 310, 280, 190, 260, 150, 342)

// -----------------------------------------------------------------------------
// Shared pieces
// -----------------------------------------------------------------------------

@Composable
private fun ScreenTitle(title: String, subtitle: String) {
    Column(Modifier.padding(horizontal = Gutter)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
        color = TextPrimary,
        modifier = modifier.padding(horizontal = Gutter)
    )
}

@Composable
private fun SegmentedTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = Gutter)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceColor)
            .padding(4.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selected
            val background by animateColorAsState(
                if (isSelected) PrimaryGreenLight else Color.Transparent,
                tween(200),
                label = "segment"
            )
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) OnPrimaryGreenContainer else TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(background)
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(SurfaceVariantColor)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CategoryTile(category: FoodCategory, size: Int = 52) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(category.tint().copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Image(painterResource(category.art()), contentDescription = null, modifier = Modifier.size((size * 0.72f).dp))
    }
}

@Composable
private fun EmptyState(art: Int, title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painterResource(art), contentDescription = null, modifier = Modifier.size(120.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun NgoCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .padding(16.dp)
    ) { content() }
}

// -----------------------------------------------------------------------------
// Role cards (used by the role selection screen)
// -----------------------------------------------------------------------------

@Composable
fun RoleCard(title: String, subtitle: String, art: Int, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceColor)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Image(painterResource(art), contentDescription = null, modifier = Modifier.size(60.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSecondary)
    }
}

// -----------------------------------------------------------------------------
// Home
// -----------------------------------------------------------------------------

@Composable
fun NgoDashboardScreen(navController: NavController) {
    val greeting = remember { greetingForHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    val offers = NgoState.offers
    val onTheWay = NgoState.onTheWay
    val awaitingSmile = NgoState.awaitingSmile
    val needAttention = NgoState.needAttention
    val dayLetters = remember { lastSevenDayLetters(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            NgoHeader(
                greeting = greeting,
                onSwitchRole = { navController.popBackStack(Screen.RoleSelection.route, false) }
            )
        }

        item { AvailabilityCard() }

        item { BroadcastCard() }

        item {
            Row(
                modifier = Modifier
                    .padding(horizontal = Gutter)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NgoStat("%,d".format(NgoState.mealsToday), "Meals today", Modifier.weight(1f))
                NgoStat("%,d".format(NgoState.peopleServed), "People fed", Modifier.weight(1f))
                NgoStat("${onTheWay.size}", "On the way", Modifier.weight(1f))
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Needs your attention")
                Column(
                    modifier = Modifier.padding(horizontal = Gutter),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (offers.isEmpty() && needAttention == 0 && awaitingSmile.isEmpty()) {
                        Text("You are all caught up.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    if (offers.isNotEmpty()) {
                        AttentionRow(
                            icon = Icons.Filled.Inbox,
                            tint = PrimaryGreen,
                            title = if (offers.size == 1) "1 new offer" else "${offers.size} new offers",
                            body = "Accept or decline while the food is fresh",
                            count = offers.size,
                            onClick = { navController.goToTab(Screen.NgoOffers.route) }
                        )
                    }
                    if (needAttention > 0) {
                        AttentionRow(
                            icon = Icons.Filled.Warning,
                            tint = SecondaryAmber,
                            title = if (needAttention == 1) "1 item in stock needs action" else "$needAttention items in stock need action",
                            body = "Expiring soon or already expired",
                            count = needAttention,
                            onClick = { navController.goToTab(Screen.NgoStock.route) }
                        )
                    }
                    if (awaitingSmile.isNotEmpty()) {
                        AttentionRow(
                            icon = Icons.Filled.Mood,
                            tint = AccentCoralColor,
                            title = if (awaitingSmile.size == 1) "1 donation is waiting for a smile" else "${awaitingSmile.size} donations are waiting for a smile",
                            body = "Show donors who their food helped",
                            count = awaitingSmile.size,
                            onClick = { navController.goToTab(Screen.NgoSmiles.route) }
                        )
                    }
                }
            }
        }

        if (onTheWay.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.padding(horizontal = Gutter), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Coming your way",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "See all",
                            style = MaterialTheme.typography.labelLarge,
                            color = PrimaryGreen,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { navController.goToTab(Screen.NgoOffers.route) }
                                .padding(8.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.padding(horizontal = Gutter),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        onTheWay.take(3).forEach { donation ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SurfaceColor)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryTile(donation.category, 48)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(donation.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${donation.volunteer ?: "A volunteer"} is bringing it from ${donation.donor}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                donation.etaMinutes?.let { eta ->
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("$eta", style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"), color = TextPrimary)
                                        Text("min", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            NgoCard(Modifier.padding(horizontal = Gutter)) {
                WeeklyChart(weeklyMeals, dayLetters)
            }
        }
    }
}

private val AccentCoralColor = Color(0xFFFF7468)

@Composable
private fun NgoHeader(greeting: String, onSwitchRole: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Gutter, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SecondaryAmber.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Image(painterResource(R.drawable.illus_role_ngo), contentDescription = null, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(greeting, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    NGO_NAME,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Filled.Verified, contentDescription = "Verified", tint = InfoColor, modifier = Modifier.size(18.dp))
            }
            Text(NGO_AREA, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = TextPrimary)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Switch to donor") },
                    onClick = {
                        menuOpen = false
                        onSwitchRole()
                    }
                )
            }
        }
    }
}

@Composable
private fun AvailabilityCard() {
    val capacity = NgoState.capacityPercent
    val capacityColor = when {
        capacity >= 90 -> DangerColor
        capacity >= 75 -> SecondaryAmber
        else -> PrimaryGreen
    }
    val animatedCapacity by animateFloatAsState(capacity / 100f, tween(600), label = "capacity")
    NgoCard(Modifier.padding(horizontal = Gutter)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (NgoState.accepting) PrimaryGreen else TextTertiary)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Accepting donations", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
                Text(
                    if (NgoState.accepting) "Open until 9 PM. Donors nearby can send food." else "Paused. Donors will not see you.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Switch(
                checked = NgoState.accepting,
                onCheckedChange = { NgoState.accepting = it },
                colors = SwitchDefaults.colors(checkedThumbColor = OnPrimaryGreen, checkedTrackColor = PrimaryGreen)
            )
        }
        Spacer(Modifier.height(14.dp))
        Row {
            Text("Storage", style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.weight(1f))
            Text("$capacity% full", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animatedCapacity },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = capacityColor,
            trackColor = SurfaceVariantColor
        )
    }
}

@Composable
private fun BroadcastCard() {
    NgoCard(Modifier.padding(horizontal = Gutter)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Urgent need broadcast", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    if (NgoState.broadcasting) "Donors near you can see it on their home screen" else "Tell donors what you need most tonight",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (NgoState.broadcasting) SecondaryAmber else TextSecondary
                )
            }
            Switch(
                checked = NgoState.broadcasting,
                onCheckedChange = { NgoState.broadcasting = it },
                colors = SwitchDefaults.colors(checkedThumbColor = OnSecondaryAmber, checkedTrackColor = SecondaryAmber)
            )
        }
        AnimatedVisibility(
            visible = NgoState.broadcasting,
            enter = expandVertically(tween(250)) + fadeIn(tween(250)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
        ) {
            Column {
                Spacer(Modifier.height(14.dp))
                Text("What do you need?", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FoodCategory.entries.toList(), key = { it.name }) { category ->
                        val selected = category in NgoState.needs
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (selected) category.tint().copy(alpha = 0.2f) else SurfaceVariantColor)
                                .border(1.dp, if (selected) category.tint() else Color.Transparent, CircleShape)
                                .clickable {
                                    NgoState.needs = if (selected) NgoState.needs - category else NgoState.needs + category
                                }
                                .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(painterResource(category.art()), contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(category.label, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NgoStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .padding(14.dp)
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
            color = TextPrimary
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
private fun AttentionRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    body: String,
    count: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(body, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Text(
            "$count",
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            color = tint
        )
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSecondary)
    }
}

@Composable
private fun WeeklyChart(values: List<Int>, labels: List<String>) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val grow by animateFloatAsState(if (started) 1f else 0f, tween(800), label = "bars")
    val maxValue = (values.maxOrNull() ?: 1).coerceAtLeast(1)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Meals received this week", style = MaterialTheme.typography.titleSmall, color = TextPrimary, modifier = Modifier.weight(1f))
            Text(
                "%,d".format(values.sum()),
                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                color = PrimaryGreen
            )
        }
        Spacer(Modifier.height(14.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            val gap = 10.dp.toPx()
            val barWidth = (size.width - gap * (values.size - 1)) / values.size
            values.forEachIndexed { index, value ->
                val barHeight = size.height * (value.toFloat() / maxValue) * grow
                drawRoundRect(
                    color = if (index == values.lastIndex) PrimaryGreen else Color.White.copy(alpha = 0.14f),
                    topLeft = Offset(index * (barWidth + gap), size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row {
            labels.forEachIndexed { index, letter ->
                Text(
                    letter,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (index == labels.lastIndex) TextPrimary else TextTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Offers and deliveries
// -----------------------------------------------------------------------------

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NgoOffersScreen(navController: NavController) {
    var tab by remember { mutableStateOf(0) }
    var banner by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(banner) {
        if (banner != null) {
            delay(2800)
            banner = null
        }
    }

    // Checking a delivery: camera, then the AI result (or a manual fallback).
    val scope = rememberCoroutineScope()
    var targetId by remember { mutableStateOf<String?>(null) }
    var analyzing by remember { mutableStateOf(false) }
    var summary by remember { mutableStateOf<IntakeSummary?>(null) }
    var aiFailed by remember { mutableStateOf(false) }

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: android.graphics.Bitmap? ->
        if (bitmap == null) {
            targetId = null
        } else {
            analyzing = true
            scope.launch {
                try {
                    val result = verifyIntakeWithGemini(bitmapToBase64(bitmap))
                    summary = IntakeSummary(
                        verified = result.verifiedMatch,
                        freshness = result.freshness,
                        expiry = result.estimatedExpiration,
                        storage = result.storageInstructions,
                        tags = result.dietaryTags
                    )
                } catch (e: Exception) {
                    // Never invent a "verified" result: offer a manual check instead.
                    aiFailed = true
                } finally {
                    analyzing = false
                }
            }
        }
    }
    LaunchedEffect(cameraPermission.status.isGranted) {
        if (cameraPermission.status.isGranted && targetId != null && summary == null && !analyzing && !aiFailed) {
            cameraLauncher.launch(null)
        }
    }
    fun startIntake(id: String) {
        targetId = id
        summary = null
        aiFailed = false
        if (cameraPermission.status.isGranted) cameraLauncher.launch(null) else cameraPermission.launchPermissionRequest()
    }
    fun finishIntake(result: IntakeSummary) {
        val id = targetId ?: return
        val donation = NgoState.donations.firstOrNull { it.id == id }
        NgoState.markReceived(id, result)
        banner = if (donation?.fromDonor == true) "Added to stock. The donor's coins are on their way." else "Added to stock."
        targetId = null
        summary = null
        aiFailed = false
    }

    val offers = NgoState.offers
    val onTheWay = NgoState.onTheWay

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ScreenTitle("Offers", "Food that donors near you want to give") }
            item {
                SegmentedTabs(
                    labels = listOf("New (${offers.size})", "On the way (${onTheWay.size})"),
                    selected = tab,
                    onSelect = { tab = it }
                )
            }

            if (tab == 0) {
                if (offers.isEmpty()) {
                    item {
                        EmptyState(
                            R.drawable.illus_step_pickup,
                            "No new offers",
                            "When a donor posts food nearby, it shows up here."
                        )
                    }
                } else {
                    items(offers, key = { it.id }) { donation ->
                        OfferCard(
                            donation = donation,
                            onAccept = {
                                NgoState.accept(donation.id)
                                banner = "Accepted. ${volunteerFor(donation.id)} will collect it from ${donation.donor}."
                            },
                            onDecline = {
                                NgoState.decline(donation.id)
                                banner = "Offer declined."
                            },
                            modifier = Modifier.padding(horizontal = Gutter)
                        )
                    }
                }
            } else {
                if (onTheWay.isEmpty()) {
                    item {
                        EmptyState(
                            R.drawable.illus_step_map,
                            "Nothing on the way",
                            "Accept an offer and the volunteer's trip shows up here."
                        )
                    }
                } else {
                    items(onTheWay, key = { it.id }) { donation ->
                        OnTheWayCard(
                            donation = donation,
                            onLogIntake = { startIntake(donation.id) },
                            modifier = Modifier.padding(horizontal = Gutter)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = banner != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            Text(
                banner ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier
                    .padding(horizontal = Gutter, vertical = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceHighColor)
                    .padding(16.dp)
            )
        }
    }

    if (analyzing) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Checking the food") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = PrimaryGreen, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    Spacer(Modifier.width(16.dp))
                    Text("Checking how fresh it is", color = TextSecondary)
                }
            },
            confirmButton = {},
            containerColor = SurfaceHighColor,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }

    if (aiFailed) {
        AlertDialog(
            onDismissRequest = {
                aiFailed = false
                targetId = null
            },
            title = { Text("The AI check is not available") },
            text = { Text("Please look at the food yourself before you accept it. You can still record the delivery by hand.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        finishIntake(
                            IntakeSummary(
                                verified = false,
                                freshness = "Checked by hand",
                                expiry = "",
                                storage = "Store it as the donor advised",
                                tags = emptyList(),
                                manual = true
                            )
                        )
                    }
                ) { Text("Record by hand", color = PrimaryGreen, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        aiFailed = false
                        targetId = null
                    }
                ) { Text("Cancel") }
            },
            containerColor = SurfaceHighColor,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }

    summary?.let { result ->
        val donation = NgoState.donations.firstOrNull { it.id == targetId }
        IntakeSheet(
            title = donation?.title ?: "Delivery",
            summary = result,
            onConfirm = { finishIntake(result) },
            onDismiss = {
                summary = null
                targetId = null
            }
        )
    }
}

@Composable
private fun OfferCard(donation: NgoDonation, onAccept: () -> Unit, onDecline: () -> Unit, modifier: Modifier = Modifier) {
    NgoCard(modifier) {
        val photo = donation.photo
        if (photo != null) {
            AsyncImage(
                model = Uri.parse(photo),
                contentDescription = "Photo from the donor",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.height(12.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryTile(donation.category)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    donation.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VegMark(donation.isVeg, size = 14.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${donation.servings} servings · safe for ${donation.shelfLife.ifBlank { "a few hours" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoChip(Icons.Filled.LocationOn, "${"%.1f".format(donation.distanceKm)} km · ${donation.donor}")
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoChip(Icons.Filled.Schedule, donation.pickup)
        }
        if (donation.note.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text("Note: ${donation.note}", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onDecline,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Decline", style = MaterialTheme.typography.labelLarge) }
            Button(
                onClick = onAccept,
                modifier = Modifier
                    .weight(2f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = OnPrimaryGreen)
            ) { Text("Accept", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun OnTheWayCard(donation: NgoDonation, onLogIntake: () -> Unit, modifier: Modifier = Modifier) {
    val eta = donation.etaMinutes ?: 0
    // Close to the door once only a few minutes are left.
    val stage = if (eta > 15) 1 else 2
    NgoCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryTile(donation.category)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(donation.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "${donation.volunteer ?: "A volunteer"} · from ${donation.donor}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$eta", style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"), color = TextPrimary)
                Text("min", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        Spacer(Modifier.height(14.dp))
        DeliverySteps(listOf("Accepted", "Picked up", "Arriving"), stage)
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onLogIntake,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = OnPrimaryGreen)
        ) {
            Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Log intake", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DeliverySteps(labels: List<String>, stage: Int) {
    Row(Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(
                                when {
                                    index == 0 -> Color.Transparent
                                    index <= stage -> PrimaryGreen
                                    else -> OutlineColor
                                }
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(if (index <= stage) PrimaryGreen else SurfaceVariantColor)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(
                                when {
                                    index == labels.lastIndex -> Color.Transparent
                                    index < stage -> PrimaryGreen
                                    else -> OutlineColor
                                }
                            )
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (index <= stage) TextPrimary else TextTertiary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntakeSheet(title: String, summary: IntakeSummary, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val good = summary.verified
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceHighColor,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = Gutter, end = Gutter, bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (good) PrimaryGreen else SecondaryAmber),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (good) Icons.Filled.Check else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = if (good) OnPrimaryGreen else OnSecondaryAmber,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        if (good) "Intake checked" else "Please look at it yourself",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(title, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
            Spacer(Modifier.height(20.dp))
            IntakeLine("Freshness", summary.freshness)
            IntakeLine("Expires", summary.expiry.ifBlank { "Not known" })
            IntakeLine("Storage", summary.storage)
            if (summary.tags.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    summary.tags.forEach { tag ->
                        Text(
                            tag,
                            style = MaterialTheme.typography.labelMedium,
                            color = OnPrimaryGreenContainer,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(PrimaryGreenLight)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = OnPrimaryGreen)
            ) { Text("Add to stock", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun IntakeLine(label: String, value: String) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
    }
}

// -----------------------------------------------------------------------------
// Stock
// -----------------------------------------------------------------------------

@Composable
fun NgoStockScreen(navController: NavController) {
    val stock = NgoState.stock.toList().sortedBy { it.hoursLeft }
    val fresh = stock.count { stockStatus(it.hoursLeft) == StockStatus.Fresh }
    val soon = stock.count { stockStatus(it.hoursLeft) == StockStatus.Soon }
    val expired = stock.count { stockStatus(it.hoursLeft) == StockStatus.Expired }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenTitle("Stock", "What is in your store room right now") }
        item {
            Row(
                modifier = Modifier
                    .padding(horizontal = Gutter)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StockCount("$fresh", "Fresh", PrimaryGreen, Modifier.weight(1f))
                StockCount("$soon", "Expiring soon", SecondaryAmber, Modifier.weight(1f))
                StockCount("$expired", "Expired", DangerColor, Modifier.weight(1f))
            }
        }
        if (stock.isEmpty()) {
            item { EmptyState(R.drawable.illus_food_pack, "The store room is empty", "Food you receive is added here.") }
        } else {
            items(stock, key = { it.id }) { item ->
                StockCard(
                    item = item,
                    onDone = { NgoState.removeStock(item.id, distributed = stockStatus(item.hoursLeft) != StockStatus.Expired) },
                    modifier = Modifier.padding(horizontal = Gutter)
                )
            }
        }
    }
}

@Composable
private fun StockCount(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceColor)
            .padding(12.dp)
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"), color = color)
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}

@Composable
private fun StockCard(item: StockItem, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val status = stockStatus(item.hoursLeft)
    val color = when (status) {
        StockStatus.Fresh -> PrimaryGreen
        StockStatus.Soon -> SecondaryAmber
        StockStatus.Expired -> DangerColor
    }
    val progress by animateFloatAsState((item.hoursLeft / 72f).coerceIn(0f, 1f), tween(600), label = "shelf")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryTile(item.category, 48)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(item.quantity, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = SurfaceVariantColor
            )
            Spacer(Modifier.height(4.dp))
            Text(expiryLabel(item.hoursLeft), style = MaterialTheme.typography.labelMedium, color = color)
        }
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onDone) {
            Text(
                if (status == StockStatus.Expired) "Dispose" else "Handed out",
                style = MaterialTheme.typography.labelLarge,
                color = if (status == StockStatus.Expired) DangerColor else PrimaryGreen
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Smiles
// -----------------------------------------------------------------------------

@Composable
fun NgoSmilesScreen(navController: NavController) {
    val waiting = NgoState.awaitingSmile
    val sent = SmileStore.smiles.filter { it.isSentByThisNgo() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ScreenTitle("Smiles", "Show donors the people their food helped") }

        item {
            Row(
                modifier = Modifier
                    .padding(horizontal = Gutter)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MealsCardBg)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(painterResource(R.drawable.illus_step_smile), contentDescription = null, modifier = Modifier.size(72.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    "Send a photo of the people who enjoyed a donation. Only photograph people who agree, and a parent or guardian for children.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MealsTextPrimary
                )
            }
        }

        item { SectionTitle("Ready to send") }
        if (waiting.isEmpty()) {
            item {
                Text(
                    "You are all caught up. New deliveries show up here once you log them.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = Gutter)
                )
            }
        } else {
            items(waiting, key = { it.id }) { donation ->
                Row(
                    modifier = Modifier
                        .padding(horizontal = Gutter)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceColor)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryTile(donation.category, 48)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(donation.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            "Received ${donation.receivedText ?: "recently"} · ${donation.servings} servings",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { navController.navigate(Screen.SendSmile.routeFor(donation.id)) },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier.height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryAmber, contentColor = OnSecondaryAmber)
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Send", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item { SectionTitle("Sent by you") }
        if (sent.isEmpty()) {
            item {
                EmptyState(
                    R.drawable.illus_step_smile,
                    "No smiles sent yet",
                    "Photos you send appear here and on the donor's Smile Wall."
                )
            }
        } else {
            items(sent, key = { it.id }) { smile ->
                Row(
                    modifier = Modifier
                        .padding(horizontal = Gutter)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceColor)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = File(smile.photoPath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(smile.donationTitle, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "“${smile.message}”",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(smile.sentAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                    }
                }
            }
        }
    }
}
