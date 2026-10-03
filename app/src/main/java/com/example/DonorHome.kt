package com.example

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.api.FoodWasteFacts
import com.example.api.fetchFoodWasteFacts
import com.example.ui.theme.AccentGold
import com.example.ui.theme.InfoColor
import com.example.ui.theme.InfoContainer
import com.example.ui.theme.MealsCardBg
import com.example.ui.theme.MealsTextPrimary
import com.example.ui.theme.OnInfoContainer
import com.example.ui.theme.OnPrimaryGreen
import com.example.ui.theme.OnPrimaryGreenContainer
import com.example.ui.theme.OnSecondaryAmber
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
import com.example.ui.theme.DangerColor
import com.example.ui.theme.DangerContainer
import com.example.ui.theme.OnDangerContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.io.File

private val ScreenPadding = 20.dp
private val HeroHeight = 188.dp

// -----------------------------------------------------------------------------
// Category looks (kept here so HomeData.kt stays free of Android resources)
// -----------------------------------------------------------------------------

internal fun FoodCategory.art(): Int = when (this) {
    FoodCategory.Meals -> R.drawable.illus_food_meal
    FoodCategory.Bakery -> R.drawable.illus_food_bread
    FoodCategory.Fruit -> R.drawable.illus_food_fruit
    FoodCategory.Vegetables -> R.drawable.illus_food_veg
    FoodCategory.Packaged -> R.drawable.illus_food_pack
    FoodCategory.Dairy -> R.drawable.illus_food_dairy
}

/** Soft background colour behind a category's illustration. */
internal fun FoodCategory.tint(): Color = when (this) {
    FoodCategory.Meals -> SecondaryAmber
    FoodCategory.Bakery -> Color(0xFFD99A52)
    FoodCategory.Fruit -> Color(0xFFE2543B)
    FoodCategory.Vegetables -> Color(0xFFF0973A)
    FoodCategory.Packaged -> AccentGold
    FoodCategory.Dairy -> InfoColor
}

internal fun medalFor(tier: String): Int = when (tier) {
    "Silver" -> R.drawable.illus_medal_silver
    "Gold" -> R.drawable.illus_medal_gold
    "Platinum" -> R.drawable.illus_medal_platinum
    else -> R.drawable.illus_medal_bronze
}

// -----------------------------------------------------------------------------
// Screen
// -----------------------------------------------------------------------------

@Composable
fun DonorDashboardScreen(navController: NavController, userProfile: UserProfile) {
    val facts by rememberFoodWasteFacts()
    var selectedCategory by remember { mutableStateOf<FoodCategory?>(null) }
    val ngos = remember(selectedCategory) { ngosAccepting(selectedCategory) }
    val smiles = SmileStore.smiles.toList()
    // Donations made in this session (from the Donate screen) come first.
    val donations = DonationLog.submitted.toList() + recentDonations
    val activeDonation = donations.firstOrNull { it.inTransit }

    val openDonate = { navController.goToTab(Screen.Donate.route) }
    val openStore = { navController.goToTab(Screen.Store.route) }
    val openSmiles = { navController.goToTab(Screen.Smiles.route) }
    val openTiers = { navController.navigate(Screen.Tiers.route) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        item {
            Column {
                HomeHeader(
                    profile = userProfile,
                    onAddress = { navController.navigate(Screen.Profile.route) },
                    onKarma = { openTiers() },
                    onProfile = { navController.navigate(Screen.Profile.route) }
                )
                Spacer(Modifier.height(14.dp))
                HomeGreeting(userProfile.name)
                Spacer(Modifier.height(16.dp))
                HeroCarousel(
                    points = userProfile.karmaPoints,
                    facts = facts,
                    onDonate = { openDonate() },
                    onStore = { openStore() }
                )
            }
        }

        activeDonation?.let { donation ->
            item { ActiveDonationCard(donation, Modifier.padding(horizontal = ScreenPadding)) }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionHeader("What are you donating?", Modifier.padding(horizontal = ScreenPadding))
                CategoryRow(
                    selected = selectedCategory,
                    onSelect = { selectedCategory = it }
                )
            }
        }

        item {
            NgoSection(
                selected = selectedCategory,
                ngos = ngos,
                onClearFilter = { selectedCategory = null },
                onDonate = { openDonate() }
            )
        }

        item {
            YourImpactCard(
                profile = userProfile,
                smileCount = smiles.size,
                onClick = { openTiers() },
                modifier = Modifier.padding(horizontal = ScreenPadding)
            )
        }

        item { SmilesSection(smiles = smiles, onOpen = { openSmiles() }) }

        item { RewardsSection(points = userProfile.karmaPoints, onOpenStore = { openStore() }) }

        item {
            CommunityCard(
                yourMeals = DEMO_MEALS_SHARED,
                modifier = Modifier.padding(horizontal = ScreenPadding)
            )
        }

        item {
            Column(Modifier.padding(horizontal = ScreenPadding)) {
                SectionHeader("Your recent donations")
                Spacer(Modifier.height(4.dp))
                donations.take(6).forEachIndexed { index, donation ->
                    DonationRow(donation, showDivider = index < donations.take(6).lastIndex)
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = ScreenPadding)) {
                SectionHeader("Happening near you")
                Spacer(Modifier.height(4.dp))
                dummyFeed.forEachIndexed { index, feedItem ->
                    FeedRow(feedItem, showDivider = index < dummyFeed.lastIndex)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Header and greeting
// -----------------------------------------------------------------------------

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {}
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (action != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.labelLarge,
                color = PrimaryGreen,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun HomeHeader(
    profile: UserProfile,
    onAddress: () -> Unit,
    onKarma: () -> Unit,
    onProfile: () -> Unit
) {
    val initials = remember(profile.name) {
        profile.name.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onAddress)
                .padding(vertical = 4.dp)
        ) {
            Text("Pickup from", style = MaterialTheme.typography.labelMedium, color = TextTertiary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = pickupLabel(profile.address),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        KarmaPill(points = profile.karmaPoints, onClick = onKarma)
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(PrimaryGreenLight)
                .clickable(onClick = onProfile),
            contentAlignment = Alignment.Center
        ) {
            ProfileAvatar(initials = initials)
        }
    }
}

@Composable
private fun KarmaPill(points: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(SurfaceColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "%,d".format(points),
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            color = TextPrimary
        )
        Spacer(Modifier.width(5.dp))
        KarmaCoin(size = 22.dp)
    }
}

@Composable
private fun HomeGreeting(name: String) {
    val greeting = remember { greetingForHour(java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) }
    val firstName = name.trim().substringBefore(" ")
    Text(
        text = buildAnnotatedString {
            if (firstName.isBlank()) {
                append(greeting)
            } else {
                withStyle(SpanStyle(color = TextSecondary)) { append("$greeting, ") }
                append(firstName)
            }
        },
        style = MaterialTheme.typography.headlineSmall,
        color = TextPrimary,
        modifier = Modifier.padding(horizontal = ScreenPadding)
    )
}

// -----------------------------------------------------------------------------
// Hero carousel
// -----------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroCarousel(points: Int, facts: FoodWasteFacts, onDonate: () -> Unit, onStore: () -> Unit) {
    // The pager has many "virtual" pages so it can keep sliding forward and loop without ever
    // rewinding across the slides. Slide number = page % slideCount.
    val slideCount = 3
    val virtualPages = slideCount * 1000
    val pagerState = rememberPagerState(
        initialPage = virtualPages / 2 - (virtualPages / 2) % slideCount,
        pageCount = { virtualPages }
    )

    // Slide on every few seconds. The loop must NOT be restarted when the page changes: doing that
    // cancelled the slide animation halfway and left the banner stuck between two slides.
    LaunchedEffect(Unit) {
        while (true) {
            delay(6000)
            if (!pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    Column {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = ScreenPadding),
            pageSpacing = 12.dp
        ) { page ->
            when (page % slideCount) {
                0 -> DonateSlide(onDonate)
                1 -> RewardsSlide(points, onStore)
                else -> FactSlide(facts)
            }
        }
        Spacer(Modifier.height(12.dp))
        PagerDots(
            count = slideCount,
            current = pagerState.currentPage % slideCount,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun PagerDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { index ->
            val selected = index == current
            val width by animateDpAsState(if (selected) 18.dp else 6.dp, tween(250), label = "dotWidth")
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(if (selected) PrimaryGreen else OutlineStrong)
            )
        }
    }
}

@Composable
private fun HeroFrame(background: Color, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HeroHeight)
            .clip(RoundedCornerShape(24.dp))
            .background(background),
        content = content
    )
}

@Composable
private fun DonateSlide(onDonate: () -> Unit) {
    HeroFrame(PrimaryGreenLight) {
        Image(
            painter = painterResource(R.drawable.illus_hero_food),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(width = 150.dp, height = 123.dp)
                .offset(x = 6.dp, y = 4.dp)
        )
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp)
                .widthIn(max = 190.dp)
        ) {
            Text(
                "Got extra food?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Photograph it and a volunteer will collect it.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnPrimaryGreenContainer,
                modifier = Modifier.widthIn(max = 160.dp)
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onDonate,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TextPrimary, contentColor = OnPrimaryGreen),
                contentPadding = PaddingValues(horizontal = 18.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text("Donate food", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun RewardsSlide(points: Int, onOpenStore: () -> Unit) {
    HeroFrame(MealsCardBg) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp)
                .widthIn(max = 170.dp)
        ) {
            Text(
                "Redeem your points",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MealsTextPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "You have ${"%,d".format(points)} points to swap for gift cards.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onOpenStore,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryAmber, contentColor = OnSecondaryAmber),
                contentPadding = PaddingValues(horizontal = 18.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text("Open store", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        }
        // Three partner tiles, fanned out like gift cards.
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp)
                .size(width = 124.dp, height = 150.dp)
        ) {
            BrandLogoTile(storeRewards[2], 64.dp, Modifier.align(Alignment.TopEnd).rotate(9f))
            BrandLogoTile(storeRewards[1], 64.dp, Modifier.align(Alignment.CenterStart).rotate(-8f))
            BrandLogoTile(storeRewards[0], 64.dp, Modifier.align(Alignment.BottomEnd).rotate(4f))
        }
    }
}

@Composable
private fun FactSlide(facts: FoodWasteFacts) {
    val seconds by rememberElapsedSeconds()
    HeroFrame(InfoContainer) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 20.dp)
        ) {
            Text(
                "Since you opened the app",
                style = MaterialTheme.typography.labelMedium,
                color = OnInfoContainer
            )
            Text(
                text = "%,d kg".format((facts.indiaWasteKgPerSec * seconds / 100).toLong() * 100),
                style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
                color = InfoColor
            )
            Text(
                "of food wasted in India",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = facts.positiveMessage.ifBlank { "Every meal you donate helps someone today." },
                style = MaterialTheme.typography.bodyMedium,
                color = OnInfoContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy((-6).dp)
        ) {
            Image(painterResource(R.drawable.illus_food_bread), contentDescription = null, modifier = Modifier.size(34.dp).rotate(-10f))
            Image(painterResource(R.drawable.illus_food_fruit), contentDescription = null, modifier = Modifier.size(34.dp))
            Image(painterResource(R.drawable.illus_food_veg), contentDescription = null, modifier = Modifier.size(34.dp).rotate(8f))
        }
    }
}

/** Seconds since the app process started; ticks once a second for the big counter. */
@Composable
private fun rememberElapsedSeconds(): State<Double> = produceState(
    initialValue = (System.currentTimeMillis() - AppStartTime) / 1000.0
) {
    while (true) {
        delay(1000)
        value = (System.currentTimeMillis() - AppStartTime) / 1000.0
    }
}

private val defaultFacts = FoodWasteFacts(
    worldWaste = "1.05 Billion Tonnes",
    indiaWaste = "78 Million Tonnes",
    gujaratWaste = "Thousands of Tonnes",
    indiaWasteKgPerSec = 2178.2,
    gujaratWasteKgPerSec = 112.5,
    positiveMessage = "Every meal you donate helps someone today."
)

/** Shows built-in figures straight away, then swaps in cached or freshly fetched ones. */
@Composable
private fun rememberFoodWasteFacts(): State<FoodWasteFacts> {
    val context = LocalContext.current
    return produceState(initialValue = loadCachedFacts(context) ?: defaultFacts) {
        if (loadCachedFacts(context) == null) {
            val fetched = try {
                fetchFoodWasteFacts()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            if (fetched != null) {
                saveCachedFacts(context, fetched)
                value = fetched
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Active donation tracker
// -----------------------------------------------------------------------------

@Composable
private fun ActiveDonationCard(donation: DonationItem, modifier: Modifier = Modifier) {
    val steps = listOf("Posted", "Picked up", "Delivered")
    val stage = donation.stage
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SecondaryAmber.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(painterResource(R.drawable.illus_step_pickup), contentDescription = null, modifier = Modifier.size(46.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (stage == 0) "Waiting for a volunteer" else "Donation on its way",
                        style = MaterialTheme.typography.labelMedium,
                        color = PrimaryGreen
                    )
                    Text(
                        donation.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (stage == 0) "Nearby NGOs can see it now" else "${donation.volunteer ?: "A volunteer"} is taking it to ${donation.ngo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                donation.etaMinutes?.let { eta ->
                    Spacer(Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "$eta",
                            style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                            color = TextPrimary
                        )
                        Text("min", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            TrackerSteps(labels = steps, stage = stage)
        }
    }
}

@Composable
private fun TrackerSteps(labels: List<String>, stage: Int) {
    Row(Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TrackLine(visible = index > 0, filled = index <= stage, modifier = Modifier.weight(1f))
                    TrackDot(done = index < stage, current = index == stage)
                    TrackLine(visible = index < labels.lastIndex, filled = index < stage, modifier = Modifier.weight(1f))
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

@Composable
private fun TrackLine(visible: Boolean, filled: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(3.dp)
            .background(
                when {
                    !visible -> Color.Transparent
                    filled -> PrimaryGreen
                    else -> OutlineColor
                }
            )
    )
}

@Composable
private fun TrackDot(done: Boolean, current: Boolean) {
    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
        if (current) PulsingHalo()
        val active = done || current
        Box(
            modifier = Modifier
                .size(if (active) 14.dp else 12.dp)
                .clip(CircleShape)
                .background(if (active) PrimaryGreen else SurfaceVariantColor)
                .then(if (active) Modifier else Modifier.border(2.dp, OutlineStrong, CircleShape)),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = OnPrimaryGreen, modifier = Modifier.size(10.dp))
            }
        }
    }
}

@Composable
private fun PulsingHalo() {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "haloAlpha"
    )
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(PrimaryGreen.copy(alpha = alpha))
    )
}

// -----------------------------------------------------------------------------
// Categories and NGOs
// -----------------------------------------------------------------------------

@Composable
private fun CategoryRow(selected: FoodCategory?, onSelect: (FoodCategory?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(FoodCategory.entries.toList(), key = { it.name }) { category ->
            val isSelected = selected == category
            CategoryItem(
                category = category,
                selected = isSelected,
                onClick = { onSelect(if (isSelected) null else category) }
            )
        }
    }
}

@Composable
private fun CategoryItem(category: FoodCategory, selected: Boolean, onClick: () -> Unit) {
    val ring by animateColorAsState(if (selected) PrimaryGreen else Color.Transparent, tween(200), label = "ring")
    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(category.tint().copy(alpha = 0.16f))
                .border(2.dp, ring, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(painterResource(category.art()), contentDescription = null, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            category.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) PrimaryGreen else TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun NgoSection(
    selected: FoodCategory?,
    ngos: List<NgoRequest>,
    onClearFilter: () -> Unit,
    onDonate: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader(
            title = if (selected == null) "Needs help tonight" else "Accepting ${selected.label.lowercase()}",
            modifier = Modifier.padding(horizontal = ScreenPadding),
            action = if (selected != null) "Show all" else null,
            onAction = onClearFilter
        )
        Column(
            modifier = Modifier
                .padding(horizontal = ScreenPadding)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ngos.forEachIndexed { index, ngo ->
                key(ngo.id) {
                    NgoRequestCard(ngo, onDonate, Modifier.appearOnScreen(index))
                }
            }
        }
    }
}

@Composable
private fun NgoRequestCard(ngo: NgoRequest, onDonate: () -> Unit, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(ngo.progress, tween(700), label = "pledged")
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ngo.icon.tint().copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(painterResource(ngo.icon.art()), contentDescription = null, modifier = Modifier.size(38.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            ngo.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.Verified, contentDescription = "Verified", tint = InfoColor, modifier = Modifier.size(16.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${ngo.area} · ${"%.1f".format(ngo.distanceKm)} km",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (ngo.urgent) {
                            Spacer(Modifier.width(8.dp))
                            UrgentChip()
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(ngo.need, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = PrimaryGreen,
                trackColor = SurfaceVariantColor
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${ngo.pledged} of ${ngo.goal} meals pledged",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Text(
                        ngo.closes,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (ngo.urgent) DangerColor else TextTertiary
                    )
                }
                Button(
                    onClick = onDonate,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = OnPrimaryGreen),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text("Donate", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun UrgentChip() {
    Text(
        "Urgent",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = OnDangerContainer,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DangerContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

// -----------------------------------------------------------------------------
// Impact, smiles, rewards
// -----------------------------------------------------------------------------

@Composable
private fun YourImpactCard(
    profile: UserProfile,
    smileCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tier = tierStatus(profile.karmaPoints)
    val animatedKarma by animateIntAsState(profile.karmaPoints, tween(800), label = "karma")
    val animatedProgress by animateFloatAsState(tier.progress, tween(800), label = "tierProgress")
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(medalFor(tier.current)),
                    contentDescription = "${tier.current} tier",
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("${tier.current} tier", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(
                        if (tier.next != null) "${tier.pointsToNext} points to ${tier.next}" else "Top tier reached",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSecondary)
            }
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = PrimaryGreen,
                trackColor = SurfaceVariantColor
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth()) {
                ImpactStat("%,d".format(animatedKarma), "Karma points", Modifier.weight(1f))
                ImpactStat("$DEMO_MEALS_SHARED", "Meals shared", Modifier.weight(1f))
                ImpactStat("$smileCount", if (smileCount == 1) "Smile" else "Smiles", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ImpactStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
            color = TextPrimary
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
private fun SmilesSection(smiles: List<SmileEntry>, onOpen: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader(
            title = "Smiles from your donations",
            modifier = Modifier.padding(horizontal = ScreenPadding),
            action = "See all",
            onAction = onOpen
        )
        if (smiles.isEmpty()) {
            Row(
                modifier = Modifier
                    .padding(horizontal = ScreenPadding)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceColor)
                    .clickable(onClick = onOpen)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(painterResource(R.drawable.illus_step_smile), contentDescription = null, modifier = Modifier.size(64.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    "When a shelter sends a photo of people enjoying your food, it shows up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = ScreenPadding),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(smiles.take(6), key = { it.id }) { smile -> SmileThumb(smile, onOpen) }
            }
        }
    }
}

@Composable
private fun SmileThumb(smile: SmileEntry, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 156.dp, height = 204.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = File(smile.photoPath),
            contentDescription = "Photo from ${smile.ngoName}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
                .padding(start = 12.dp, end = 12.dp, top = 28.dp, bottom = 12.dp)
        ) {
            Column {
                Text(
                    smile.ngoName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    timeAgoLabel(smile.sentAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun RewardsSection(points: Int, onOpenStore: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader(
            title = "Rewards for you",
            modifier = Modifier.padding(horizontal = ScreenPadding),
            action = "Open store",
            onAction = onOpenStore
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(homeRewards.take(4), key = { it.id }) { reward ->
                VoucherCard(reward, canAfford = points >= reward.points, onClick = onOpenStore)
            }
        }
    }
}

@Composable
private fun VoucherCard(item: RewardItem, canAfford: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .background(item.tileColor),
            contentAlignment = Alignment.Center
        ) {
            BrandLogoTile(item, 52.dp)
        }
        Column(Modifier.padding(12.dp)) {
            Text(
                item.title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(item.brand, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
            Spacer(Modifier.height(8.dp))
            Text(
                if (canAfford) "Ready to redeem" else "${"%,d".format(item.points)} points",
                style = MaterialTheme.typography.labelMedium,
                color = if (canAfford) PrimaryGreen else TextTertiary
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Community
// -----------------------------------------------------------------------------

@Composable
private fun CommunityCard(yourMeals: Int, modifier: Modifier = Modifier) {
    val target = communityProgress()
    val progress by animateFloatAsState(target, tween(900), label = "communityGoal")
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = PrimaryGreen,
                        strokeWidth = 9.dp,
                        trackColor = SurfaceVariantColor,
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        "${(target * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                        color = TextPrimary
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "%,d".format(COMMUNITY_MEALS_THIS_MONTH),
                        style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                        color = TextPrimary
                    )
                    Text(
                        "meals shared in Vadodara this month",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Goal: ${"%,d".format(COMMUNITY_MEALS_GOAL)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = OutlineColor)
            Spacer(Modifier.height(12.dp))
            Text("Top donors this week", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            topDonorsThisWeek.forEachIndexed { index, donor -> DonorRow(rank = index + 1, name = donor.name, meals = donor.meals) }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryGreenLight)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "You are #$DEMO_WEEKLY_RANK this week",
                    style = MaterialTheme.typography.labelLarge,
                    color = OnPrimaryGreenContainer,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "$yourMeals meals",
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    color = OnPrimaryGreenContainer
                )
            }
        }
    }
}

@Composable
private fun DonorRow(rank: Int, name: String, meals: Int) {
    val tints = listOf(SecondaryAmber, InfoColor, PrimaryGreen)
    val tint = tints[(rank - 1).coerceIn(0, tints.lastIndex)]
    val initials = name.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$rank",
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            color = TextTertiary,
            modifier = Modifier.width(20.dp)
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(initials, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = tint)
        }
        Spacer(Modifier.width(12.dp))
        Text(name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
        Text(
            "$meals meals",
            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
            color = TextSecondary
        )
    }
}

// -----------------------------------------------------------------------------
// Lists
// -----------------------------------------------------------------------------

@Composable
private fun DonationRow(item: DonationItem, showDivider: Boolean) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(item.category.tint().copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(item.category.art()), contentDescription = null, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    item.ngo,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    item.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                StatusChip(item.status)
                Spacer(Modifier.height(4.dp))
                KarmaAmount("+${item.points}", style = MaterialTheme.typography.labelMedium)
            }
        }
        if (showDivider) HorizontalDivider(color = OutlineColor)
    }
}

@Composable
private fun StatusChip(status: String) {
    val onTheWay = status != STATUS_DELIVERED
    Text(
        status,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = if (onTheWay) MealsTextPrimary else OnPrimaryGreenContainer,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (onTheWay) MealsCardBg else PrimaryGreenLight)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun FeedRow(item: LiveFeedItem, showDivider: Boolean) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(item.category.tint().copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(item.category.art()), contentDescription = null, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(item.message, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text(item.timeAgo, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            }
        }
        if (showDivider) HorizontalDivider(color = OutlineColor)
    }
}
