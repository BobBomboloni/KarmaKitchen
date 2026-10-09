package com.example

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ui.theme.AmberText
import com.example.ui.theme.InfoColor
import com.example.ui.theme.InfoContainer
import com.example.ui.theme.LocalPalette
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
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/*
 * Live pickup: the donor (or the NGO) follows one donation from "who will carry it?" to "delivered".
 * It is the show version of Karma Relay (see DeliveryDemo.kt): the map is drawn by the app, so it
 * needs no Maps key, and the carriers are simulated. Share and Map really open other apps.
 */

private val Pad = 20.dp

/** One donation as the tracking screen shows it, from either side. */
private data class TrackInfo(
    val id: String,
    val title: String,
    val from: String,
    val to: String,
    /** 0 = nobody carrying it yet, 1 = on the way, 2 = delivered. */
    val stage: Int,
    val carrierName: String?,
    val etaMinutes: Int,
    val points: Int
)

private fun trackInfo(id: String, ngoSide: Boolean): TrackInfo? {
    if (!ngoSide) {
        (DonationLog.submitted.firstOrNull { it.id == id } ?: recentDonations.firstOrNull { it.id == id })?.let { d ->
            val stage = if (d.status == STATUS_DELIVERED) 2 else d.stage
            return TrackInfo(d.id, d.title, "You", d.ngo, stage, d.volunteer, d.etaMinutes ?: 12, d.points)
        }
    }
    return NgoState.donations.firstOrNull { it.id == id }?.let { d ->
        val stage = when (d.stage) {
            OfferStage.Offered -> 0
            OfferStage.OnTheWay -> 1
            OfferStage.Received -> 2
        }
        TrackInfo(d.id, d.title, d.donor, NGO_NAME, stage, d.volunteer, d.etaMinutes ?: 12, 0)
    }
}

/** Where the Map button points: the NGO's area for the donor, the pickup area for the NGO. */
private fun mapQuery(info: TrackInfo, ngoSide: Boolean): String = when {
    ngoSide -> DEFAULT_PICKUP_AREA
    info.to == NGO_NAME -> NGO_AREA
    else -> sampleNgoRequests.firstOrNull { it.name == info.to }?.let { "${it.area}, Vadodara" } ?: "Vadodara"
}

@Composable
fun TrackPickupScreen(navController: NavController, donationId: String, ngoSide: Boolean) {
    val context = LocalContext.current
    // A one-second clock for the countdowns. The map keeps its own, smoother clock.
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(1000)
            value = System.currentTimeMillis()
        }
    }
    val info = trackInfo(donationId, ngoSide)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = Pad, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Column(Modifier.weight(1f)) {
                Text("Live pickup", style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp), color = TextPrimary)
                if (info != null) {
                    Text(info.title, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        if (info == null) {
            Text(
                "This pickup isn't here any more.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                modifier = Modifier.padding(Pad)
            )
            return@Column
        }

        val searchStart = DeliveryDemo.searchStart(info.id, now)
        val phase = if (info.stage == 0) relayPhase(now - searchStart) else RelayPhase.Dispatched
        // Nobody took it in time: the relay hands it to a Karma Rider.
        LaunchedEffect(info.stage, phase) {
            if (info.stage == 0 && phase == RelayPhase.Dispatched) DeliveryDemo.dispatch(info.id, CarrierKind.KarmaRider)
        }
        val tripStart = if (info.stage >= 1) DeliveryDemo.tripStart(info.id, now) else now
        val kind = DeliveryDemo.kindOf(info.id)
        val carrier = carrierFor(info.id, kind, info.carrierName)
        val progress = when (info.stage) {
            0 -> 0f
            1 -> tripProgress(now - tripStart, info.etaMinutes)
            else -> 1f
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Pad)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            DemoMap(
                stage = info.stage,
                tripStart = tripStart,
                etaMinutes = info.etaMinutes,
                kind = kind,
                pill = when {
                    info.stage == 0 -> "Finding a carrier"
                    info.stage == 2 -> "Delivered"
                    progress >= 1f -> "Arrived"
                    else -> "Arriving in ${minutesLeft(progress, info.etaMinutes)} min"
                }
            )
            MapLegend(from = if (ngoSide) info.from else "You", to = if (ngoSide) "You" else info.to)

            when (info.stage) {
                0 -> RelayCard(
                    elapsedMs = now - searchStart,
                    onCourier = { DeliveryDemo.dispatch(info.id, CarrierKind.Courier) }
                )
                1 -> {
                    CarrierCard(
                        carrier = carrier,
                        status = when {
                            progress >= 1f && ngoSide -> "${carrier.name} is here with the food. Check it in from Offers."
                            progress >= 1f -> "${carrier.name} has reached ${info.to}. They're checking the food."
                            progress < 0.25f && !ngoSide -> "${carrier.name} is on the way to you."
                            ngoSide -> "${carrier.name} is bringing it from ${info.from}."
                            else -> "${carrier.name} has the food and is taking it to ${info.to}."
                        },
                        onShare = {
                            val text = "I'm donating ${info.title} with KarmaKitchen. ${carrier.name} (${carrier.kind.label}) is taking it to ${info.to}, " +
                                "arriving in about ${minutesLeft(progress, info.etaMinutes)} min."
                            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                            context.startActivity(Intent.createChooser(send, "Share this pickup"))
                        },
                        onMap = {
                            val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(mapQuery(info, ngoSide)))
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        }
                    )
                    if (kind == CarrierKind.Courier) SponsorStrip()
                    HandoverCard(code = handoverCode(info.id), carrierName = carrier.name, ngoSide = ngoSide)
                    EatByCard(msLeft = DeliveryDemo.cookedAt(info.id, now) + EAT_WITHIN_MS - now)
                }
                else -> DeliveredCard(to = info.to, carrier = carrier, points = if (ngoSide) 0 else info.points)
            }

            Text(
                "Prototype preview: the map, riders and courier are simulated. In the full app the carrier's phone shares live GPS.",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
    }
}

// -----------------------------------------------------------------------------
// The map
// -----------------------------------------------------------------------------

/** A small drawn city map with the route, the two ends and the carrier moving along it. */
@Composable
private fun DemoMap(stage: Int, tripStart: Long, etaMinutes: Int, kind: CarrierKind, pill: String) {
    val dark = LocalPalette.current.isDark
    val land = SurfaceVariantColor
    val road = if (dark) Color(0xFF34443A) else Color(0xFFFFFDF8)
    val park = PrimaryGreen.copy(alpha = if (dark) 0.22f else 0.16f)
    val water = InfoColor.copy(alpha = if (dark) 0.28f else 0.20f)
    val route = PrimaryGreen
    val amber = SecondaryAmber

    // Every frame while the carrier is moving, so it glides instead of jumping once a second.
    val frameNow by produceState(System.currentTimeMillis(), stage) {
        if (stage != 1) return@produceState
        while (true) {
            withFrameMillis { }
            value = System.currentTimeMillis()
        }
    }
    val progress = when (stage) {
        0 -> 0f
        1 -> tripProgress(frameNow - tripStart, etaMinutes)
        else -> 1f
    }
    val pulse = rememberInfiniteTransition(label = "radar")
    val radar by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "radarSweep"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(land)
    ) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(park, Offset(size.width * 0.58f, size.height * 0.64f), Size(size.width * 0.20f, size.height * 0.26f), CornerRadius(18f))
            drawCircle(water, radius = size.minDimension * 0.13f, center = Offset(size.width * 0.31f, size.height * 0.17f))
            val main = 12.dp.toPx()
            val minor = 5.dp.toPx()
            listOf(0.43f, 0.69f).forEach { y -> drawLine(road, Offset(0f, y * size.height), Offset(size.width, y * size.height), minor) }
            listOf(0.33f, 0.67f).forEach { x -> drawLine(road, Offset(x * size.width, 0f), Offset(x * size.width, size.height), minor) }
            listOf(0.16f, 0.30f, 0.56f, 0.82f).forEach { y -> drawLine(road, Offset(0f, y * size.height), Offset(size.width, y * size.height), main) }
            listOf(0.16f, 0.50f, 0.84f).forEach { x -> drawLine(road, Offset(x * size.width, 0f), Offset(x * size.width, size.height), main) }

            fun pathOf(points: List<Pair<Float, Float>>) = Path().apply {
                points.forEachIndexed { i, p ->
                    if (i == 0) moveTo(p.first * size.width, p.second * size.height) else lineTo(p.first * size.width, p.second * size.height)
                }
            }
            val line = 5.dp.toPx()
            drawPath(
                pathOf(demoRoute),
                route.copy(alpha = 0.45f),
                style = Stroke(line, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f)))
            )
            if (progress > 0f) {
                drawPath(pathOf(routeUpTo(demoRoute, progress)), route, style = Stroke(line, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            if (stage == 0) {
                // Searching: rings spread out from the donor.
                val start = demoRoute.first()
                val centre = Offset(start.first * size.width, start.second * size.height)
                val maxR = size.minDimension * 0.55f
                listOf(radar, (radar + 0.5f) % 1f).forEach { r ->
                    drawCircle(amber.copy(alpha = 0.5f * (1f - r)), radius = maxR * r, center = centre, style = Stroke(3.dp.toPx()))
                }
            }
        }

        val start = demoRoute.first()
        val end = demoRoute.last()
        MapPin(start.first * w, start.second * h, Icons.Filled.Home, SecondaryAmber, OnSecondaryAmber)
        MapPin(end.first * w, end.second * h, Icons.Filled.VolunteerActivism, PrimaryGreen, OnPrimaryGreen)
        if (stage >= 1) {
            val at = pointAlong(demoRoute, progress)
            MapPin(
                at.first * w,
                at.second * h,
                if (kind == CarrierKind.Courier) Icons.Filled.LocalShipping else Icons.Filled.TwoWheeler,
                SurfaceHighColor,
                PrimaryGreen,
                size = 40.dp,
                ring = PrimaryGreen
            )
        }

        Text(
            pill,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = OnPrimaryGreenContainer,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(50))
                .background(PrimaryGreenLight)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun MapPin(
    x: Float,
    y: Float,
    icon: ImageVector,
    background: Color,
    tint: Color,
    size: Dp = 32.dp,
    ring: Color? = null
) {
    val half = with(LocalDensity.current) { size.toPx() / 2f }
    Box(
        modifier = Modifier
            .offset { IntOffset((x - half).roundToInt(), (y - half).roundToInt()) }
            .size(size)
            .clip(CircleShape)
            .background(background)
            .then(if (ring != null) Modifier.border(3.dp, ring, CircleShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
private fun MapLegend(from: String, to: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendDot(SecondaryAmber, "Pickup: $from", Modifier.weight(1f))
        LegendDot(PrimaryGreen, "Drop: $to", Modifier.weight(1f))
    }
}

@Composable
private fun LegendDot(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// -----------------------------------------------------------------------------
// Cards
// -----------------------------------------------------------------------------

@Composable
private fun TrackCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .border(1.dp, OutlineColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .animateContentSize(),
        content = content
    )
}

private enum class RungState { Waiting, Active, Skipped }

/** Karma Relay: nearby NGOs first, then Karma Riders, then a sponsored courier. */
@Composable
private fun RelayCard(elapsedMs: Long, onCourier: () -> Unit) {
    val phase = relayPhase(elapsedMs)
    val left = relaySecondsLeft(elapsedMs)
    TrackCard {
        Text("Finding someone to carry it", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(Modifier.height(2.dp))
        Text(
            "Karma Relay asks the closest people first, so the food never sits waiting.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(Modifier.height(14.dp))
        RelayRung(
            number = 1,
            icon = Icons.Filled.Groups,
            title = "Nearby NGOs",
            detail = if (phase == RelayPhase.AskingNgos) "4 NGOs within 5 km can see it · 0:${"%02d".format(left)} left" else "No NGO could send someone right now",
            state = if (phase == RelayPhase.AskingNgos) RungState.Active else RungState.Skipped
        )
        RelayRung(
            number = 2,
            icon = Icons.Filled.TwoWheeler,
            title = "Karma Riders",
            detail = if (phase == RelayPhase.AskingRiders) "Asking 3 student volunteers nearby · 0:${"%02d".format(left)}" else "3 riders online within 3 km",
            state = if (phase == RelayPhase.AskingNgos) RungState.Waiting else RungState.Active
        )
        RelayRung(
            number = 3,
            icon = Icons.Filled.LocalShipping,
            title = "Backup courier",
            detail = "Booked automatically if nobody is free before the food's eat-by time. The ride is sponsored, so it's free for you.",
            state = RungState.Waiting,
            last = true
        )
        TextButton(onClick = onCourier, modifier = Modifier.align(Alignment.End)) {
            Text("Demo: book the backup courier now", style = MaterialTheme.typography.labelLarge, color = PrimaryGreen)
        }
    }
}

@Composable
private fun RelayRung(number: Int, icon: ImageVector, title: String, detail: String, state: RungState, last: Boolean = false) {
    val active = state == RungState.Active
    val pulse = rememberInfiniteTransition(label = "rung")
    val glow by pulse.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "rungGlow"
    )
    Row(Modifier.padding(bottom = if (last) 0.dp else 12.dp)) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    when (state) {
                        RungState.Active -> PrimaryGreen.copy(alpha = glow)
                        RungState.Skipped -> SurfaceVariantColor
                        RungState.Waiting -> SurfaceVariantColor
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (active) PrimaryGreen else TextTertiary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "$number. $title",
                style = MaterialTheme.typography.titleSmall,
                color = if (state == RungState.Skipped) TextTertiary else TextPrimary
            )
            Text(detail, style = MaterialTheme.typography.bodySmall, color = if (active) TextPrimary else TextSecondary)
        }
    }
}

@Composable
private fun CarrierCard(carrier: Carrier, status: String, onShare: () -> Unit, onMap: () -> Unit) {
    var explainCall by remember { mutableStateOf(false) }
    TrackCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(PrimaryGreenLight),
                contentAlignment = Alignment.Center
            ) {
                Text(carrier.name.take(1), style = MaterialTheme.typography.titleLarge, color = OnPrimaryGreenContainer)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(carrier.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        carrier.kind.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberText,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(SecondaryAmber.copy(alpha = 0.16f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = SecondaryAmber, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "${carrier.rating} · ${carrier.trips} pickups · ${carrier.vehicle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(status, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        Text(carrier.kind.about, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionChip(Icons.Filled.Call, "Call", Modifier.weight(1f)) { explainCall = true }
            ActionChip(Icons.Filled.Share, "Share", Modifier.weight(1f), onShare)
            ActionChip(Icons.Filled.Map, "Map", Modifier.weight(1f), onMap)
        }
    }
    if (explainCall) {
        AlertDialog(
            onDismissRequest = { explainCall = false },
            confirmButton = { TextButton(onClick = { explainCall = false }) { Text("Got it", color = PrimaryGreen) } },
            title = { Text("Private calling") },
            text = {
                Text(
                    "In the full app this calls ${carrier.name} through a masked number, so neither of you sees the other's real phone number. " +
                        "It isn't connected in this prototype."
                )
            },
            containerColor = SurfaceHighColor
        )
    }
}

@Composable
private fun ActionChip(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .height(44.dp)
            .bounceCard(RoundedCornerShape(12.dp), onClick, PrimaryGreenLight),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = OnPrimaryGreenContainer, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = OnPrimaryGreenContainer)
    }
}

@Composable
private fun SponsorStrip() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(InfoContainer)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.VolunteerActivism, contentDescription = null, tint = InfoColor, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "This ride is paid for by KarmaKitchen's CSR partners, so it's free for you and the NGO.",
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary
        )
    }
}

@Composable
private fun HandoverCard(code: String, carrierName: String, ngoSide: Boolean) {
    TrackCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Key, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Handover code", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            code.forEach { digit ->
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceVariantColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$digit", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            if (ngoSide) "$carrierName collects the food with this code, so it only goes to the right person."
            else "Tell $carrierName this code at pickup. It proves the food went to the right person.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun EatByCard(msLeft: Long) {
    val share = (msLeft.toFloat() / EAT_WITHIN_MS).coerceIn(0f, 1f)
    TrackCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Timer, contentDescription = null, tint = AmberText, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Best eaten within ${eatByLabel(msLeft)}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { share },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = SecondaryAmber,
            trackColor = SurfaceVariantColor
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Cooked food is safest eaten within about 2 hours. Offers that can't arrive in time are hidden from NGOs.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun DeliveredCard(to: String, carrier: Carrier, points: Int) {
    TrackCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PrimaryGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = OnPrimaryGreen, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Delivered to $to", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text("Carried by ${carrier.name}, ${carrier.kind.label.lowercase()}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        if (points > 0) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("+${"%,d".format(points)} Karma coins added", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
        }
    }
}
