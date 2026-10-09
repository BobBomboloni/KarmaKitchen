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
import androidx.compose.animation.slideInVertically
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
        ThemeSettings.load(applicationContext)
        val barStyle = if (ThemeSettings.dark) {
            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
        // The window shows through for a moment before Compose draws, so match the saved theme.
        window.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(if (ThemeSettings.dark) 0xFF0E1410.toInt() else 0xFFF6F0E3.toInt())
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
    object NgoDashboard : Screen("ngo_dashboard", "Home", Icons.Filled.Home)
    object NgoOffers : Screen("ngo_offers", "Offers", Icons.Filled.Inbox)
    object NgoStock : Screen("ngo_stock", "Stock", Icons.Filled.Inventory2)
    object NgoSmiles : Screen("ngo_smiles", "Smiles", Icons.Filled.Mood)
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
    object TrackPickup : Screen("track/{side}/{donationId}", "Live pickup", Icons.Filled.Star) {
        fun routeFor(donationId: String, ngoSide: Boolean) = "track/${if (ngoSide) "ngo" else "donor"}/$donationId"
    }

}

private tailrec fun android.content.Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext?.findActivity()
    else -> null
}

@Composable
fun KarmaKitchenApp() {
    val navController = rememberNavController()
    val appContext = LocalContext.current
    var userProfile by remember { mutableStateOf(loadProfile(appContext)) }
    LaunchedEffect(userProfile) { saveProfile(appContext, userProfile) }
    remember(appContext) { SmileStore.load(appContext) }

    // Coins the NGO has confirmed (when it records a delivery) are added to the donor's balance.
    val pendingCredits = DonationLog.pendingCredits
    LaunchedEffect(pendingCredits.size) {
        if (pendingCredits.isNotEmpty()) {
            val earned = pendingCredits.sum()
            pendingCredits.clear()
            userProfile = userProfile.copy(karmaPoints = userProfile.karmaPoints + earned)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Status and navigation bar icons must contrast with the screen behind them: light icons in
    // dark mode, dark icons on the light palette.
    val barsDark = ThemeSettings.dark
    DisposableEffect(barsDark) {
        val barStyle = if (barsDark) {
            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        }
        (appContext.findActivity() as? ComponentActivity)?.enableEdgeToEdge(
            statusBarStyle = barStyle,
            navigationBarStyle = barStyle
        )
        onDispose { }
    }

    // The bar shows the donor tabs or the receiver tabs. It keeps the last set while it slides away.
    val tabHolder = remember { arrayOf<List<Screen>>(BottomTabs) }
    tabsForRoute(currentRoute)?.let { tabHolder[0] = it }
    val items = tabHolder[0]

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
                visible = currentRoute != Screen.Welcome.route && currentRoute != Screen.RoleSelection.route && currentRoute != Screen.SendSmile.route &&
                    currentRoute != Screen.TrackPickup.route,
                enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
                exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(200))
            ) {
                NavigationBar(containerColor = SurfaceColor, tonalElevation = 0.dp) {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                val waiting = if (screen.route == Screen.NgoOffers.route) NgoState.offers.size else 0
                                if (waiting > 0) {
                                    BadgedBox(
                                        badge = { Badge(containerColor = PrimaryGreen, contentColor = OnPrimaryGreen) { Text("$waiting") } }
                                    ) { Icon(screen.icon, contentDescription = screen.title) }
                                } else {
                                    Icon(screen.icon, contentDescription = screen.title)
                                }
                            },
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
            // The slide direction follows the tab order (see NavTransitions.kt), so going back to an
            // earlier tab slides the other way.
            enterTransition = { screenEnter(isPop = false) },
            exitTransition = { screenExit(isPop = false) },
            popEnterTransition = { screenEnter(isPop = true) },
            popExitTransition = { screenExit(isPop = true) }
        ) {
            composable(Screen.RoleSelection.route) { RoleSelectionScreen(navController) }
            composable(Screen.NgoDashboard.route) { NgoDashboardScreen(navController) }
            composable(Screen.NgoOffers.route) { NgoOffersScreen(navController) }
            composable(Screen.NgoStock.route) { NgoStockScreen(navController) }
            composable(Screen.NgoSmiles.route) { NgoSmilesScreen(navController) }
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
                    navController = navController,
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
            composable(
                route = Screen.TrackPickup.route,
                arguments = listOf(
                    navArgument("side") { type = NavType.StringType },
                    navArgument("donationId") { type = NavType.StringType }
                )
            ) { entry ->
                TrackPickupScreen(
                    navController = navController,
                    donationId = entry.arguments?.getString("donationId") ?: "",
                    ngoSide = entry.arguments?.getString("side") == "ngo"
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
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                welcomeSteps().forEachIndexed { index, step ->
                    if (index > 0) Spacer(modifier = Modifier.height(20.dp))
                    WelcomeStepRow(step = step, artOnLeft = index % 2 == 0)
                }
            }
        }

        // Good to know
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Good to know",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                listOf(
                    "Pack food in clean, sealed containers so it travels safely.",
                    "Post cooked meals within 2 hours of making them.",
                    "Check Needs help tonight on your home screen to see which shelters need food first."
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
            KarmaButton(
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
            fontFamily = Fraunces,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            color = textColor,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = "Kitchen",
            fontFamily = Fraunces,
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

@OptIn(ExperimentalPermissionsApi::class)
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
        
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceColor)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Dark mode", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text("Easier on the eyes at night", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Switch(
                checked = ThemeSettings.dark,
                onCheckedChange = { ThemeSettings.set(context, it) }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        
        KarmaButton(
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
        TierInfo("Bronze", 0, "Default 1x coin multiplier", TierBronze),
        TierInfo("Silver", 2000, "1.2x coin multiplier\n5% store discount", TierSilver),
        TierInfo("Gold", 5000, "1.5x coin multiplier\n10% store discount", TierGold),
        TierInfo("Platinum", 10000, "2.0x coin multiplier\n20% store discount\nFree shipping", TierPlatinum)
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
                        Image(
                            painter = painterResource(medalFor(currentTier.name)),
                            contentDescription = null,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your tier",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentTier.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = readableInk(currentTier.color)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        KarmaAmount(
                            amount = formatKarma(userProfile.karmaPoints),
                            style = MaterialTheme.typography.titleMedium,
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
                    "%,d - %,d".format(tier.minKp, nextTierKp - 1)
                } else {
                    "%,d+".format(tier.minKp)
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
                        Image(
                            painter = painterResource(medalFor(tier.name)),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
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
                                    color = readableInk(tier.color)
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
                                            color = OnTierColor
                                        )
                                    }
                                }
                            }
                            KarmaAmount(
                                amount = rangeText,
                                style = MaterialTheme.typography.bodySmall,
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
fun RoleSelectionScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedKarmaLogo(size = 132.dp)
        Spacer(modifier = Modifier.height(4.dp))
        KarmaKitchenLogoText(
            fontSize = 40.sp,
            textColor = TextPrimary,
            accentColor = PrimaryGreen
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Surplus food, shared with people who need it",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        RoleCard(
            title = "I'm donating food",
            subtitle = "Share surplus food and earn karma coins",
            art = R.drawable.illus_food_meal,
            tint = PrimaryGreen,
            onClick = { navController.navigate(Screen.Welcome.route) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        RoleCard(
            title = "I'm receiving food",
            subtitle = "For NGOs, shelters and community kitchens",
            art = R.drawable.illus_role_ngo,
            tint = SecondaryAmber,
            onClick = { navController.navigate(Screen.NgoDashboard.route) }
        )

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "You can switch roles any time.",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary
        )
    }
}
