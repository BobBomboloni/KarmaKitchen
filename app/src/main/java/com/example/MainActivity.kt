package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.Alignment
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import android.Manifest
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import com.example.api.FoodAnalysisResult

import com.example.api.IntakeAnalysisResult
import com.example.api.verifyIntakeWithGemini
import com.example.api.bitmapToBase64
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator

import com.example.api.analyzeFoodWithGemini
import com.example.ui.DonationChatbot
import com.example.api.uriToBase64
import androidx.activity.compose.rememberLauncherForActivityResult
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await
import java.util.Locale
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition

val AppStartTime = System.currentTimeMillis()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            MyApplicationTheme {
                KarmaKitchenApp()
            }
        }
    }
}

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val karmaPoints: Int = 1240
)

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object RoleSelection : Screen("role_selection", "Role Selection", Icons.Filled.Star)
    object NgoDashboard : Screen("ngo_dashboard", "NGO Dashboard", Icons.Filled.Home)
    object Welcome : Screen("welcome", "Welcome", Icons.Filled.Star)
    object Dashboard : Screen("dashboard", "Home", Icons.Filled.Home)
    object Donate : Screen("donate", "Donate", Icons.Filled.AddCircle)
    object Store : Screen("store", "Store", Icons.Filled.ShoppingCart)
    object Profile : Screen("profile", "Profile", Icons.Filled.Person)
    object Tiers : Screen("tiers", "Impact tiers", Icons.Filled.Star)
    object Smiles : Screen("smiles", "Smiles", Icons.Filled.Mood)
    object SendSmile : Screen("send_smile/{receivalId}", "Send a smile", Icons.Filled.Mood) {
        fun routeFor(receivalId: String) = "send_smile/$receivalId"
    }

}

@Composable
fun KarmaKitchenApp() {
    val navController = rememberNavController()
    val items = BottomTabs
    val appContext = LocalContext.current
    var userProfile by remember { mutableStateOf(loadProfile(appContext)) }
    LaunchedEffect(userProfile) { saveProfile(appContext, userProfile) }
    remember(appContext) { SmileStore.load(appContext) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            AnimatedVisibility(
                visible = currentRoute == Screen.Dashboard.route,
                enter = scaleIn(tween(250)) + fadeIn(tween(250)),
                exit = scaleOut(tween(150)) + fadeOut(tween(150))
            ) {
                FloatingActionButton(
                    onClick = { navController.goToTab(Screen.Donate.route) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = OnPrimaryGreen
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "New donation")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        bottomBar = {
            AnimatedVisibility(
                visible = currentRoute != Screen.Welcome.route && currentRoute != Screen.RoleSelection.route && currentRoute != Screen.NgoDashboard.route && currentRoute != Screen.SendSmile.route,
                enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
                exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(200))
            ) {
                NavigationBar(containerColor = SurfaceColor, tonalElevation = 0.dp) {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = highlightedTabRoute(currentRoute) == screen.route,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = OnPrimaryGreenContainer,
                                selectedTextColor = PrimaryGreen,
                                indicatorColor = PrimaryGreenLight,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            onClick = { navController.goToTab(screen.route) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.RoleSelection.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 12 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutHorizontally(tween(300)) { it / 12 } }
        ) {
            composable(Screen.RoleSelection.route) { RoleSelectionScreen(navController) }
            composable(Screen.NgoDashboard.route) { NgoDashboardScreen(navController) }
            composable(Screen.Welcome.route) { WelcomeScreen(navController) }
            composable(Screen.Dashboard.route) { DonorDashboardScreen(navController, userProfile) }
            composable(Screen.Donate.route) { 
                DonationCreationScreen(
                    navController = navController, 
                    userProfile = userProfile,
                    onProfileUpdate = { userProfile = it }
                ) 
            }
            composable(Screen.Store.route) { 
                KarmaStoreScreen(
                    userProfile = userProfile,
                    onProfileUpdate = { userProfile = it }
                ) 
            }
            composable(Screen.Profile.route) { 
                ProfileEditScreen(
                    profile = userProfile,
                    onProfileUpdate = { userProfile = it },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Tiers.route) {
                TierListScreen(navController = navController, userProfile = userProfile)
            }
            composable(Screen.Smiles.route) {
                SmileWallScreen(donorName = userProfile.name)
            }
            composable(
                route = Screen.SendSmile.route,
                arguments = listOf(navArgument("receivalId") { type = NavType.StringType })
            ) { entry ->
                SendSmileScreen(
                    navController = navController,
                    receivalId = entry.arguments?.getString("receivalId") ?: ""
                )
            }
        }
    }
}

@Composable
fun WelcomeScreen(navController: NavController) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 32.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_karma_logo),
                    contentDescription = "KarmaKitchen Logo",
                    modifier = Modifier.size(120.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Welcome to",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                KarmaKitchenLogoText(
                    fontSize = 36.sp,
                    textColor = MaterialTheme.colorScheme.onBackground,
                    accentColor = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Share surplus food with people nearby. When it reaches them, you earn Karma Points.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }

        // How it works
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "How it works",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                val steps = listOf(
                    "Photograph the food" to "The app estimates servings and checks how fresh it looks, so there is nothing to type.",
                    "Choose a pickup spot" to "Your location fills in automatically, so a volunteer can find you.",
                    "Earn Karma Points" to "Points arrive once the NGO confirms it received the food.",
                    "Spend them in the store" to "Swap points for gift cards and coupons from partner brands."
                )

                steps.forEachIndexed { index, (title, desc) ->
                    if (index > 0) Spacer(modifier = Modifier.height(14.dp))
                    Text(text = title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = desc, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }

        // Good to know
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Good to know",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                listOf(
                    "Pack food in clean, sealed containers so it travels safely.",
                    "Post cooked meals within 2 hours of making them.",
                    "Check Who needs food on your home screen to see which shelters need help first."
                ).forEach { tip ->
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        // Start button
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = OnPrimaryGreen
                )
            ) {
                Text(
                    text = "Get started",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
fun KarmaKitchenLogoText(
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 38.sp,
    textColor: Color = SecondaryAmber,
    accentColor: Color = PrimaryGreen
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Karma",
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            color = textColor,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = "Kitchen",
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            color = accentColor,
            letterSpacing = (-0.5).sp
        )
    }
}

@Composable
fun KarmaInfinityLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.ic_karma_logo),
        contentDescription = "Karma Kitchen Logo",
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

@Composable
fun DonorDashboardScreen(navController: NavController, userProfile: UserProfile) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier.size(64.dp), // Fixed generous size for the container
                        contentAlignment = Alignment.Center
                    ) {
                        KarmaInfinityLogo(
                            // 70% of 64dp = ~45dp canvas, leaving 9.5dp padding inside the box on all sides
                            modifier = Modifier.fillMaxSize(0.70f)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = remember { greetingForHour(java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = userProfile.name.ifBlank { "Donor" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                val initials = userProfile.name.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
                
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .clickable { navController.navigate(Screen.Profile.route) },
                    contentAlignment = Alignment.Center
                ) {
                    ProfileAvatar(initials = initials)
                }
            }
        }

        item {
            val tier = tierStatus(userProfile.karmaPoints)
            val animatedKarma by animateIntAsState(userProfile.karmaPoints, tween(800), label = "karma")
            val animatedTierProgress by animateFloatAsState(tier.progress, tween(800), label = "tierProgress")
            Card(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.Tiers.route) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("%,d".format(animatedKarma), style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"), color = TextPrimary)
                            Text("Karma points", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("42", style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"), color = TextPrimary)
                            Text("Meals shared", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    LinearProgressIndicator(
                        progress = { animatedTierProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = PrimaryGreen,
                        trackColor = SurfaceVariantColor
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${tier.current} tier", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            if (tier.next != null) "${tier.pointsToNext} points to ${tier.next}" else "Top tier reached",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        item {
            SmileTeaserCard(onClick = { navController.goToTab(Screen.Smiles.route) })
        }

        item {
            FoodWasteFactBar()
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Who needs food", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                itemsIndexed(dummyNgos) { index, ngo ->
                    Box(Modifier.appearOnScreen(index)) { NgoNeedCard(ngo) }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Happening now", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
        }

        itemsIndexed(dummyFeed) { index, feedItem ->
            Box(Modifier.appearOnScreen(index)) { LiveFeedCard(feedItem, showDivider = index < dummyFeed.lastIndex) }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Recent donations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }

        itemsIndexed(recentDonations) { index, donation ->
            Box(Modifier.appearOnScreen(index)) { DonationItemCard(donation) }
        }
    }
}

data class NgoNeed(val name: String, val distance: String, val status: String, val statusColor: Color, val preferredItems: String)
val dummyNgos = listOf(
    NgoNeed("Vadodara Relief Kitchen", "2.5 km away", "Urgent Need", DangerColor, "Cooked Meals, Fresh Produce"),
    NgoNeed("Hope Shelter", "4.1 km away", "Accepting Donations", PrimaryGreen, "Packaged Food, Grains"),
    NgoNeed("City Orphanage", "5.8 km away", "Accepting Donations", PrimaryGreen, "Fruits, Dairy")
)

@Composable
fun NgoNeedCard(ngo: NgoNeed) {
    Card(
        modifier = Modifier.width(280.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("${ngo.name} • ${ngo.distance}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ngo.statusColor))
                Spacer(modifier = Modifier.width(8.dp))
                Text(ngo.status, style = MaterialTheme.typography.bodySmall, color = ngo.statusColor, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Looking for: ${ngo.preferredItems}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

data class LiveFeedItem(val message: String, val timeAgo: String)
val dummyFeed = listOf(
    LiveFeedItem("Riya donated 12 servings of veg biryani.", "4 min ago"),
    LiveFeedItem("Arjun saved 4 meals from his canteen.", "18 min ago"),
    LiveFeedItem("Annapurna Seva Trust received 30 chapatis from Meera.", "40 min ago"),
    LiveFeedItem("Kabir earned 150 Karma Points for an urgent delivery.", "1 hour ago"),
    LiveFeedItem("Hope Shelter reached its goal of 200 meals today.", "2 hours ago")
)

@Composable
fun LiveFeedCard(item: LiveFeedItem, showDivider: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text(item.message, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(item.timeAgo, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        }
        if (showDivider) HorizontalDivider(color = OutlineColor)
    }
}

@Composable
fun ImpactCard(modifier: Modifier = Modifier, title: String, value: String, icon: ImageVector) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

data class DonationItem(val title: String, val date: String, val status: String, val points: Int)

val recentDonations = listOf(
    DonationItem("Veg Biryani, 12 servings", "Today, 1:30 PM", "On the way", 300),
    DonationItem("Leftover Catering Sandwiches", "Yesterday, 7:45 PM", "Delivered", 150),
    DonationItem("Fresh Vegetable Basket", "29 Sep", "Delivered", 200),
    DonationItem("Bakery Surplus, 20 breads", "26 Sep", "Delivered", 100)
)

@Composable
fun DonationItemCard(item: DonationItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("${item.date} • ${item.status}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Text("+${item.points} KP", style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"), color = PrimaryGreen, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun DonationCreationScreen(navController: NavController, userProfile: UserProfile, onProfileUpdate: (UserProfile) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var showCamera by remember { mutableStateOf(false) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    var step by remember { mutableStateOf(1) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<FoodAnalysisResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var foodTitle by remember { mutableStateOf("") }
    var servings by remember { mutableStateOf("") }
    var isVeg by remember { mutableStateOf(true) }
    var shelfLife by remember { mutableStateOf("") }
    var qualityText by remember { mutableStateOf("") }
    var storageTip by remember { mutableStateOf("") }

    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
    )
    var currentAddress by remember { mutableStateOf("Fetching location...") }

    LaunchedEffect(step, locationPermissions.allPermissionsGranted) {
        if (step == 3) {
            if (!locationPermissions.allPermissionsGranted) {
                locationPermissions.launchMultiplePermissionRequest()
            } else {
                try {
                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                    @Suppress("MissingPermission")
                    val location: Location? = fusedLocationClient.lastLocation.await()
                    if (location != null) {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            currentAddress = address.getAddressLine(0) ?: "${location.latitude}, ${location.longitude}"
                        } else {
                            currentAddress = "${location.latitude}, ${location.longitude}"
                        }
                    } else {
                        currentAddress = if (userProfile.address.isNotBlank()) userProfile.address else "Location unavailable. Please set in Profile."
                    }
                } catch (e: Exception) {
                    currentAddress = if (userProfile.address.isNotBlank()) userProfile.address else "Location service failed."
                }
            }
        }
    }

    val calculatedPoints = remember(servings, qualityText) { calculateKarmaPoints(servings, qualityText) }

    fun analyzeUri(uri: Uri) {
        capturedImageUri = uri
        isAnalyzing = true
        errorMessage = null
        scope.launch {
            try {
                val base64 = uriToBase64(context, uri)
                val result = analyzeFoodWithGemini(base64)
                analysisResult = result
                foodTitle = result.title
                servings = result.quantity
                isVeg = result.isVeg
                qualityText = result.quality
                shelfLife = result.shelfLife
                storageTip = result.storageTip
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to analyze food with Gemini AI."
            } finally {
                isAnalyzing = false
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            analyzeUri(uri)
        }
    }

    if (showCamera) {
        if (cameraPermissionState.status.isGranted) {
            CameraCapture(
                onImageCaptured = { uri ->
                    showCamera = false
                    analyzeUri(uri)
                },
                onError = {
                    showCamera = false
                    errorMessage = "Camera error: ${it.message}"
                },
                onClose = {
                    showCamera = false
                }
            )
        } else {
            LaunchedEffect(Unit) {
                cameraPermissionState.launchPermissionRequest()
            }
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.PhotoCamera,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Camera access needed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "KarmaKitchen needs camera access to inspect food freshness and shelf life with Gemini AI.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { showCamera = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = { cameraPermissionState.launchPermissionRequest() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = OnPrimaryGreen)
                            ) {
                                Text("Allow")
                            }
                        }
                    }
                }
            }
        }
        return
    }

    if (step == 4) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_karma_logo),
                contentDescription = "KarmaKitchen Logo",
                modifier = Modifier.size(130.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Donation scheduled",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Your donation has been broadcast to nearby NGOs.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(18.dp))
            
            // Notice: Points Credited After Receipt
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariantColor),
                border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.LockClock, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "+$calculatedPoints KP Reserved",
                                style = MaterialTheme.typography.titleMedium,
                                color = PrimaryGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Waiting for the recipient to confirm",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "You will receive your +$calculatedPoints Karma Points as soon as the NGO or needy person receives the food.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(navController.graph.id) {
                            inclusive = true
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = OnPrimaryGreen)
            ) {
                Text("Back to home", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            }
        }
        
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                if (step > 1) {
                    step--
                } else {
                    navController.popBackStack()
                }
            }) {
                Icon(
                    if (step > 1) Icons.Filled.ArrowBack else Icons.Filled.Close,
                    contentDescription = if (step > 1) "Back" else "Close",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("New donation", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
        Spacer(modifier = Modifier.height(8.dp))
        val animatedStepProgress by animateFloatAsState(step / 3f, tween(400), label = "stepProgress")
        LinearProgressIndicator(
            progress = { animatedStepProgress },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
            color = PrimaryGreen,
            trackColor = SurfaceVariantColor
        )
        Spacer(modifier = Modifier.height(20.dp))

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState > initialState
                (fadeIn(tween(250)) + slideInHorizontally(tween(300)) { if (forward) it / 8 else -it / 8 }) togetherWith
                    (fadeOut(tween(150)) + slideOutHorizontally(tween(300)) { if (forward) -it / 8 else it / 8 })
            },
            label = "donationStep"
        ) { currentStep ->
        when (currentStep) {
            1 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                                    Column {
                                        Text(
                                            "Check the food",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary,
                                        )
                                        Text(
                                            "We estimate the quantity, freshness and shelf life from your photo.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                // Photo Display or Capture Box
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceVariantColor)
                                ) {
                                    Column {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(220.dp)
                                                .background(SurfaceVariantColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (capturedImageUri != null) {
                                                AsyncImage(
                                                    model = capturedImageUri,
                                                    contentDescription = "Captured Food",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }

                                            if (isAnalyzing) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(BackgroundColor.copy(alpha = 0.85f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    com.example.ui.ScannerAnimation(modifier = Modifier.fillMaxSize())
                                                }
                                            } else if (analysisResult != null) {
                                                val isFresh = analysisResult!!.isSafeToDonate
                                                Box(modifier = Modifier.fillMaxSize()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(12.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(if (isFresh) PrimaryGreen else DangerColor)
                                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                if (isFresh) Icons.Filled.Check else Icons.Filled.Block,
                                                                contentDescription = null,
                                                                tint = if (isFresh) OnPrimaryGreen else OnDanger,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                if (isFresh) analysisResult!!.quality else "Not safe to donate",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = if (isFresh) OnPrimaryGreen else OnDanger
                                                            )
                                                        }
                                                    }
                                                }
                                            } else if (capturedImageUri == null) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.padding(20.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.PhotoCamera,
                                                        contentDescription = "Upload",
                                                        modifier = Modifier.size(48.dp),
                                                        tint = PrimaryGreen
                                                    )
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    Text(
                                                        "Scan Food with Camera or Gallery",
                                                        color = TextPrimary,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        "Gemini AI will recognize the dish and inspect its quality automatically.",
                                                        color = TextSecondary,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }

                                        // Action buttons if no image yet or to rescan
                                        if (!isAnalyzing) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = { showCamera = true },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                                                ) {
                                                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(if (analysisResult == null) "Take photo" else "Retake")
                                                }
                                                OutlinedButton(
                                                    onClick = { galleryLauncher.launch("image/*") },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                                                ) {
                                                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Choose from gallery")
                                                }
                                            }
                                        }
                                    }
                                }

                                // Error Banner if any
                                if (errorMessage != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = DangerContainer),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, DangerColor.copy(alpha = 0.5f))
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Warning, contentDescription = null, tint = DangerColor)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                errorMessage!!,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = OnDangerContainer,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                // AI Analysis Breakdown
                                if (analysisResult != null) {
                                    val res = analysisResult!!
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Title & Dietary Tag
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            res.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (res.isVeg) PrimaryGreenLight else NonVegContainer)
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                if (res.isVeg) "Veg" else "Non-veg",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (res.isVeg) PrimaryGreen else NonVegColor
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Key Metrics: Quantity, Quality, Shelf Life
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Filled.Restaurant, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Quantity", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(res.quantity, style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"), fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                            }
                                        }

                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Outlined.Timer, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Shelf life", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(res.shelfLife, style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"), fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                            }
                                        }

                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = BorderStroke(1.dp, if (res.isSafeToDonate) OutlineColor else DangerColor),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        if (res.isSafeToDonate) Icons.Filled.CheckCircle else Icons.Filled.Dangerous,
                                                        contentDescription = null,
                                                        tint = if (res.isSafeToDonate) PrimaryGreen else DangerColor,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        "Quality",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (res.isSafeToDonate) TextSecondary else DangerColor
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    res.quality,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (res.isSafeToDonate) TextPrimary else OnDangerContainer
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (!res.isSafeToDonate) {
                                        // Inedible / Unsafe Safety Notice
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = DangerContainer),
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(1.5.dp, DangerColor)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Filled.Dangerous,
                                                        contentDescription = null,
                                                        tint = DangerColor,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        "Food Declared Inedible",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = OnDangerContainer
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    res.rejectionReason ?: "This food item is evaluated as spoiled, expired, or unsafe for consumption. In accordance with safety regulations, it cannot be accepted for donation.",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = TextPrimary
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    "For safety, this can't be donated. Please compost it or dispose of it safely.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = OnDangerContainer,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    } else {
                                        // Storage tip
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = SurfaceVariantColor),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Outlined.TipsAndUpdates, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text("Storage advice", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                                                    Text(res.storageTip, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    if (res.isSafeToDonate) {
                                        Button(
                                            onClick = { step = 2 },
                                            modifier = Modifier.fillMaxWidth().height(54.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = OnPrimaryGreen)
                                        ) {
                                            Icon(Icons.Outlined.VolunteerActivism, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Accept AI Verification & Continue", fontWeight = FontWeight.SemiBold)
                                        }
                                    } else {
                                        // Blocked from proceeding
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Button(
                                                onClick = { /* Blocked */ },
                                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    disabledContainerColor = DangerContainer,
                                                    disabledContentColor = DangerColor
                                                ),
                                                enabled = false
                                            ) {
                                                Icon(Icons.Filled.Block, contentDescription = null, tint = DangerColor, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Cannot Proceed: Inedible / Unsafe Food", fontWeight = FontWeight.SemiBold)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    capturedImageUri = null
                                                    analysisResult = null
                                                    showCamera = true
                                                },
                                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                                shape = RoundedCornerShape(14.dp),
                                                border = BorderStroke(1.dp, PrimaryGreen),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen)
                                            ) {
                                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Scan something else", fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Powered by Gemini", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                }
                            }
                        }
                    }
                    
                    item {
                        DonationChatbot()
                    }
                }
            }
            2 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text("Review the details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Gemini pre-filled these details from your photo. You can edit them if needed.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                OutlinedTextField(
                                    value = foodTitle,
                                    onValueChange = { foodTitle = it },
                                    label = { Text("Food name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = servings,
                                    onValueChange = { servings = it },
                                    label = { Text("Quantity & Servings (e.g. 4-6 servings)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = shelfLife,
                                    onValueChange = { shelfLife = it },
                                    label = { Text("Safe Shelf Life (e.g. 14 Hours)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Dietary Preference: ", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                                    Spacer(modifier = Modifier.weight(1f))
                                    FilterChip(
                                        selected = isVeg,
                                        onClick = { isVeg = true },
                                        label = { Text("Veg") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PrimaryGreenLight,
                                            selectedLabelColor = OnPrimaryGreenContainer,
                                            selectedLeadingIconColor = OnPrimaryGreenContainer
                                        ),
                                        leadingIcon = {
                                            if (isVeg) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    FilterChip(
                                        selected = !isVeg,
                                        onClick = { isVeg = false },
                                        label = { Text("Non-Veg") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NonVegContainer,
                                            selectedLabelColor = NonVegColor,
                                            selectedLeadingIconColor = NonVegColor
                                        ),
                                        leadingIcon = {
                                            if (!isVeg) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            OutlinedButton(
                                onClick = { step = 1 },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Back")
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Button(
                                onClick = { step = 3 },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = OnPrimaryGreen),
                                enabled = foodTitle.isNotBlank() && servings.isNotBlank()
                            ) {
                                Text("Next: Location", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            3 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text("Confirm the pickup spot", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Volunteers and NGO partners will collect the food from this address.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Pickup address", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(currentAddress, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Estimated Volunteer arrival: ~25 mins", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceVariantColor),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Points you will earn", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("+$calculatedPoints Karma Points", style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"), fontWeight = FontWeight.SemiBold, color = PrimaryGreen)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Based on quantity ($servings) and quality (${if(qualityText.isBlank()) "Standard" else qualityText}).", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.Info,
                                            contentDescription = null,
                                            tint = PrimaryGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Karma points will unlock as soon as the NGO or recipient receives the food.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimary,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            OutlinedButton(
                                onClick = { step = 2 },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Back")
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Button(
                                onClick = { step = 4 },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = OnPrimaryGreen)
                            ) {
                                Text("Confirm pickup", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

/**
 * A reward partner. [logoName] is the name of a drawable (res/drawable/<logoName>) holding its logo;
 * if that file is missing the tile shows the brand name instead. With [tintLogo] the logo is
 * painted in [logoColor] on a [tileColor] tile; without it the artwork is shown as-is on white.
 */
data class RewardItem(
    val id: String,
    val brand: String,
    val title: String,
    val points: Int,
    val logoName: String,
    val tileColor: Color,
    val logoColor: Color,
    val tintLogo: Boolean = true,
    val logoScale: Float = 0.62f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KarmaStoreScreen(userProfile: UserProfile, onProfileUpdate: (UserProfile) -> Unit) {
    val rewards = listOf(
        RewardItem("1", "McDonald's", "₹500 Gift Card", 1000, "logo_mcdonalds", BrandMcDonaldsRed, BrandMcDonaldsGold),
        RewardItem("2", "Swiggy", "₹500 Gift Card", 1000, "logo_swiggy", BrandSwiggyOrange, BrandWhite),
        RewardItem("3", "Spotify", "₹500 Gift Card", 1000, "logo_spotify", BrandSpotifyBlack, BrandSpotifyGreen),
        RewardItem("4", "Samsung", "10% Off Coupon", 2000, "logo_samsung", BrandSamsungBlue, BrandWhite, logoScale = 0.78f),
        RewardItem("5", "Puma", "₹1000 Gift Card", 2500, "logo_puma", BrandWhite, BrandBlack),
        RewardItem("6", "Nike", "₹2500 Gift Card", 5000, "logo_nike", BrandWhite, BrandBlack)
    )

    var cart by remember { mutableStateOf(listOf<RewardItem>()) }
    var showCart by remember { mutableStateOf(false) }
    var checkoutSuccess by remember { mutableStateOf(false) }

    val totalPoints = cart.sumOf { it.points }
    
    if (checkoutSuccess) {
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(100.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text("Purchase complete", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Your giftcard(s) will be sent to your email:\n${userProfile.email}",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { checkoutSuccess = false },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = OnPrimaryGreen)
            ) {
                Text("Back to store", fontWeight = FontWeight.SemiBold)
            }
        }
        return
    }

    if (showCart) {
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showCart = false }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Your cart", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            if (cart.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("Your cart is empty", color = TextSecondary)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(cart) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                BrandLogoTile(item, 40.dp)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.brand, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                                    Text(item.title, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                                Text("${item.points} KP", style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"), fontWeight = FontWeight.SemiBold, color = PrimaryGreen)
                                IconButton(onClick = { 
                                    val newCart = cart.toMutableList()
                                    newCart.remove(item)
                                    cart = newCart
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("$totalPoints KP", style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"), fontWeight = FontWeight.SemiBold, color = PrimaryGreen)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { 
                    if (userProfile.karmaPoints >= totalPoints) {
                        onProfileUpdate(userProfile.copy(karmaPoints = userProfile.karmaPoints - totalPoints))
                        cart = emptyList()
                        showCart = false
                        checkoutSuccess = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = OnPrimaryGreen),
                enabled = cart.isNotEmpty() && userProfile.karmaPoints >= totalPoints
            ) {
                Text(
                    if (userProfile.karmaPoints >= totalPoints) "Checkout" else "Not enough points", 
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Store", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                Text("You have %,d KP".format(userProfile.karmaPoints), style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"), color = PrimaryGreen, fontWeight = FontWeight.SemiBold)
            }
            
            BadgedBox(
                badge = {
                    if (cart.isNotEmpty()) {
                        Badge(containerColor = MaterialTheme.colorScheme.error) { Text("${cart.size}") }
                    }
                }
            ) {
                IconButton(onClick = { showCart = true }) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = "Cart", modifier = Modifier.size(28.dp))
                }
            }
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(rewards.chunked(2)) { rowItems ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    for (item in rowItems) {
                        val quantity = cart.count { it.id == item.id }
                        val canAdd = (totalPoints + item.points) <= userProfile.karmaPoints
                        RewardCard(
                            modifier = Modifier.weight(1f),
                            item = item,
                            quantity = quantity,
                            canAdd = canAdd,
                            onAddClick = {
                                if (canAdd) {
                                    cart = cart + item
                                }
                            },
                            onRemoveClick = {
                                val newCart = cart.toMutableList()
                                val index = newCart.indexOfFirst { it.id == item.id }
                                if (index != -1) {
                                    newCart.removeAt(index)
                                    cart = newCart
                                }
                            }
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun RewardCard(
    modifier: Modifier = Modifier,
    item: RewardItem,
    quantity: Int,
    canAdd: Boolean = true,
    onAddClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            BrandLogoTile(item, 64.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(item.brand, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Text(item.title, style = MaterialTheme.typography.bodySmall, color = TextSecondary, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(12.dp))
            
            if (quantity > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .background(SurfaceVariantColor, RoundedCornerShape(8.dp)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onRemoveClick, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Remove, contentDescription = "Remove", modifier = Modifier.size(16.dp), tint = TextPrimary)
                    }
                    Text(quantity.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = onAddClick, modifier = Modifier.size(36.dp), enabled = canAdd) {
                        Icon(Icons.Filled.Add, contentDescription = "Add", modifier = Modifier.size(16.dp), tint = if (canAdd) TextPrimary else TextSecondary.copy(alpha = 0.3f))
                    }
                }
            } else {
                Button(
                    onClick = onAddClick,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceVariantColor,
                        contentColor = TextPrimary,
                        disabledContainerColor = SurfaceVariantColor.copy(alpha = 0.3f),
                        disabledContentColor = TextSecondary.copy(alpha = 0.5f)
                    ),
                    contentPadding = PaddingValues(0.dp),
                    enabled = canAdd
                ) {
                    Text("${item.points} KP", style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}







@OptIn(ExperimentalMaterial3Api::class, com.google.accompanist.permissions.ExperimentalPermissionsApi::class)
@Composable
fun ProfileEditScreen(
    profile: UserProfile,
    onProfileUpdate: (UserProfile) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var email by remember { mutableStateOf(profile.email) }
    var phone by remember { mutableStateOf(profile.phone) }
    var address by remember { mutableStateOf(profile.address) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
    )
    
    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted && address.isBlank()) {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                val location: Location? = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        address = addresses[0].getAddressLine(0) ?: ""
                    }
                }
            } catch (e: Exception) {
                // Ignore failure
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("Edit profile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { locationPermissions.launchMultiplePermissionRequest() },
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Use my location", tint = PrimaryGreen)
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = {
                onProfileUpdate(profile.copy(name = name, email = email, phone = phone, address = address))
                onBack()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = OnPrimaryGreen)
        ) {
            Text("Save changes", fontWeight = FontWeight.SemiBold)
        }
    }
}



@Composable
fun TierListScreen(navController: NavController, userProfile: UserProfile) {
    val tiers = listOf(
        TierInfo("Bronze", 0, "Default 1x KP multiplier", TierBronze),
        TierInfo("Silver", 2000, "1.2x KP multiplier\n5% store discount", TierSilver),
        TierInfo("Gold", 5000, "1.5x KP multiplier\n10% store discount", TierGold),
        TierInfo("Platinum", 10000, "2.0x KP multiplier\n20% store discount\nFree shipping", TierPlatinum)
    )

    val currentTier = when {
        userProfile.karmaPoints >= 10000 -> tiers[3]
        userProfile.karmaPoints >= 5000 -> tiers[2]
        userProfile.karmaPoints >= 2000 -> tiers[1]
        else -> tiers[0]
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Impact tiers",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Your tier",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentTier.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = currentTier.color
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "%,d KP".format(userProfile.karmaPoints),
                            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                            color = TextPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "All tiers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(tiers.size) { index ->
                val tier = tiers[index]
                val nextTierKp = if (index < tiers.size - 1) tiers[index + 1].minKp else null
                val rangeText = if (nextTierKp != null) {
                    "%,d - %,d KP".format(tier.minKp, nextTierKp - 1)
                } else {
                    "%,d+ KP".format(tier.minKp)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (tier.name == currentTier.name) SurfaceVariantColor else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = tier.color,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = tier.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = tier.color
                                )
                                if (tier.name == currentTier.name) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(tier.color)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Current",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = BackgroundColor
                                        )
                                    }
                                }
                            }
                            Text(
                                text = rangeText,
                                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                            )
                            Text(
                                text = tier.benefits,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

data class TierInfo(
    val name: String,
    val minKp: Int,
    val benefits: String,
    val color: Color
)


@Composable
fun FoodWasteFactBar() {
    var facts by remember { mutableStateOf<com.example.api.FoodWasteFacts?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val factsContext = LocalContext.current

    LaunchedEffect(Unit) {
        isLoading = true
        // Default facts as a fallback immediately for UI responsiveness, then load actual
        facts = com.example.api.FoodWasteFacts(
            worldWaste = "1.05B Tonnes",
            indiaWaste = "78M Tonnes",
            gujaratWaste = "Loading...",
            indiaWasteKgPerSec = 2178.2,
            gujaratWasteKgPerSec = 112.5,
            positiveMessage = "Fetching latest impact data..."
        )
        
        try {
            val cached = loadCachedFacts(factsContext)
            val fetchedFacts = cached ?: com.example.api.fetchFoodWasteFacts()
            if (fetchedFacts != null) {
                if (cached == null) saveCachedFacts(factsContext, fetchedFacts)
                facts = fetchedFacts
            } else {
                facts = com.example.api.FoodWasteFacts(
                    worldWaste = "1.05 Billion Tonnes",
                    indiaWaste = "78 Million Tonnes",
                    gujaratWaste = "Thousands of Tonnes",
                    indiaWasteKgPerSec = 2178.2,
                    gujaratWasteKgPerSec = 112.5,
                    positiveMessage = "Every meal you donate helps someone today."
                )
            }
        } catch (e: Exception) {
             facts = com.example.api.FoodWasteFacts(
                worldWaste = "1.05 Billion Tonnes",
                indiaWaste = "78 Million Tonnes",
                gujaratWaste = "Thousands of Tonnes",
                indiaWasteKgPerSec = 2178.2,
                gujaratWasteKgPerSec = 112.5,
                positiveMessage = "Every meal you donate counts."
            )
        } finally {
            isLoading = false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Food waste, by the numbers", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading && facts?.worldWaste == "1.05B Tonnes") {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Ticking timer (global to app session)
            var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
            LaunchedEffect(Unit) {
                while(true) {
                    kotlinx.coroutines.delay(200)
                    currentTime = System.currentTimeMillis()
                }
            }
            
            val elapsedSeconds = (currentTime - AppStartTime) / 1000.0

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FactItem(title = "World, per year", value = facts?.worldWaste ?: "")
                
                val indiaLive = if (facts != null) "%,d kg".format((facts!!.indiaWasteKgPerSec * elapsedSeconds).toInt()) else ""
                FactItem(
                    title = "India, since you opened the app",
                    value = if (isLoading) (facts?.indiaWaste ?: "") else indiaLive
                )
                
                val gujaratLive = if (facts != null) "%,d kg".format((facts!!.gujaratWasteKgPerSec * elapsedSeconds).toInt()) else ""
                FactItem(
                    title = "Gujarat, since you opened the app",
                    value = if (isLoading) (facts?.gujaratWaste ?: "") else gujaratLive
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = facts?.positiveMessage ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun androidx.compose.foundation.layout.RowScope.FactItem(title: String, value: String) {
    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"), color = TextPrimary)
        Text(title, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
fun RoleSelectionScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VideoBackdrop)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo Video Animation
        val context = LocalContext.current
        val videoUri = Uri.parse("android.resource://${context.packageName}/${R.raw.logo_animation}")
        
        AndroidView(
            factory = { ctx ->
                android.widget.VideoView(ctx).apply {
                    setVideoURI(videoUri)
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        start()
                    }
                    setOnErrorListener { _, _, _ -> true } // Suppress error popup if dummy file
                }
            },
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Image(
            painter = painterResource(id = R.drawable.ic_karmakitchen_text),
            contentDescription = "KarmaKitchen Text Logo",
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(90.dp),
            contentScale = ContentScale.Fit
        )
        Text(
            text = "Surplus food, shared with people who need it",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
        
        Spacer(modifier = Modifier.height(64.dp))
        
        // Donor Button
        Button(
            onClick = { navController.navigate(Screen.Welcome.route) }, // Or Dashboard directly
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SuccessColor)
        ) {
            Text(
                "I'm donating food",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = OnPrimaryGreen
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // NGO Button
        Button(
            onClick = { navController.navigate(Screen.NgoDashboard.route) },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WarningColor)
        ) {
            Text(
                "I'm receiving food (NGO)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = OnSecondaryAmber
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NgoDashboardScreen(navController: NavController) {
    var isBroadcasting by remember { mutableStateOf(false) }
    var isAccepting by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<IntakeAnalysisResult?>(null) }
    var showResultDialog by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: android.graphics.Bitmap? ->
        if (bitmap != null) {
            isAnalyzing = true
            coroutineScope.launch {
                try {
                    val base64 = bitmapToBase64(bitmap)
                    analysisResult = verifyIntakeWithGemini(base64)
                    showResultDialog = true
                } catch (e: Exception) {
                    // Never fabricate a "verified" result: ask for a manual inspection instead.
                    analysisResult = null
                    showResultDialog = false
                    scanError = "AI check unavailable. Please inspect the food manually before accepting it."
                } finally {
                    isAnalyzing = false
                }
            }
        }
    }

    val mockDeliveries = listOf(
        Triple("20 Servings - Mixed Veg", "Driver ETA: 12 Mins", "1.2 km away"),
        Triple("50 Assorted Breads", "Driver ETA: 25 Mins", "3.4 km away")
    )
    
    val mockInventory = listOf(
        Triple("Whole Wheat Flour (Atta) - 10kg", "Expires in 3 months", "Fresh"),
        Triple("Fresh Tomatoes & Onions - 5kg", "Expires in 3 days", "Fresh"),
        Triple("Cooked Basmati Rice & Dal", "Expires in 4 hours", "Expiring Soon"),
        Triple("Catering Paneer Sabzi", "Expired 2 hours ago", "Expired")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_karma_logo),
                        contentDescription = "KarmaKitchen Logo",
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(remember { greetingForHour(java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) }, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text("Navrachana Community", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariantColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, contentDescription = "Profile", tint = TextPrimary)
                }
            }
        }

        // Metrics Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NgoMetricCard(title = "Meals today", value = "342", modifier = Modifier.weight(1f))
                NgoMetricCard(title = "Capacity", value = "85%", modifier = Modifier.weight(1f))
                NgoMetricCard(title = "Active", value = "3", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Action Center
        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text("Availability", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))
                
                // Broadcast Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceColor)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Urgent need broadcast", style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(if (isBroadcasting) "Broadcasting to local donors" else "Currently inactive", style = MaterialTheme.typography.bodyMedium, color = if (isBroadcasting) WarningColor else TextSecondary)
                    }
                    androidx.compose.material3.Switch(
                        checked = isBroadcasting,
                        onCheckedChange = { isBroadcasting = it },
                        colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = WarningColor)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Accepting Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceColor)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Accepting Donations", style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text("Manage warehouse capacity", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    androidx.compose.material3.Switch(
                        checked = isAccepting,
                        onCheckedChange = { isAccepting = it },
                        colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = SuccessColor)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Incoming Radar
        item {
            Text(
                "On the way",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        items(mockDeliveries.size) { index ->
            val delivery = mockDeliveries[index]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(delivery.first, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text("${delivery.second} • ${delivery.third}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
                // Actions inside card
                Button(
                    onClick = { 
                        if (cameraPermissionState.status.isGranted) {
                            cameraLauncher.launch(null) 
                        } else {
                            cameraPermissionState.launchPermissionRequest()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp, topStart = 0.dp, topEnd = 0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessColor)
                ) {
                    Text("Accept and log intake", color = OnPrimaryGreen, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Received donations",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Text(
                "Open a received donation to send the donor a photo of the people who enjoyed it.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(sampleReceivals) { receival ->
            ReceivalCard(
                receival = receival,
                onSendSmile = { navController.navigate(Screen.SendSmile.routeFor(receival.id)) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "In stock",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        items(mockInventory.size) { index ->
            val inventory = mockInventory[index]
            val statusColor = when(inventory.third) {
                "Fresh" -> SuccessColor
                "Expiring Soon" -> WarningColor
                else -> DangerColor // Red for expired
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceColor)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(inventory.first, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(inventory.second, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                
                // Status Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(inventory.third, color = statusColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (isAnalyzing) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { },
            title = { Text("Checking the food", color = TextPrimary) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = SuccessColor)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("AI is verifying food freshness...", color = TextSecondary)
                }
            },
            confirmButton = { },
            containerColor = SurfaceHighColor,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }

    if (scanError != null) {
        AlertDialog(
            onDismissRequest = { scanError = null },
            title = { Text("Could not verify food") },
            text = { Text(scanError ?: "") },
            confirmButton = { TextButton(onClick = { scanError = null }) { Text("OK") } }
        )
    }

    if (showResultDialog && analysisResult != null) {
        val res = analysisResult!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showResultDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (res.verifiedMatch) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = if (res.verifiedMatch) SuccessColor else WarningColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Intake recorded", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }
            },
            text = {
                Column {
                    Text("The AI has verified this donation against the donor's original listing.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Freshness: ${res.freshness}", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Expiration: ${res.estimatedExpiration}", color = WarningColor, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Storage: ${res.storageInstructions}", color = SuccessColor, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Automated Sorting:", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        res.dietaryTags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SuccessColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(tag, color = SuccessColor, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showResultDialog = false }) {
                    Text("Done", color = SuccessColor, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = SurfaceHighColor
        )
    }
}

@Composable
fun NgoMetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .padding(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"), color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}
