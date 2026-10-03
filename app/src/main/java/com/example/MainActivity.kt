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
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                welcomeSteps.forEachIndexed { index, step ->
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

/** What the Karma Store sells. The home screen shows the first few of these too. */
val storeRewards = listOf(
    RewardItem("1", "McDonald's", "₹500 Gift Card", 1000, "logo_mcdonalds", BrandMcDonaldsRed, BrandMcDonaldsGold),
    RewardItem("2", "Swiggy", "₹500 Gift Card", 1000, "logo_swiggy", BrandSwiggyOrange, BrandWhite),
    RewardItem("3", "Spotify", "₹500 Gift Card", 1000, "logo_spotify", BrandSpotifyBlack, BrandSpotifyGreen),
    RewardItem("4", "Samsung", "10% Off Coupon", 2000, "logo_samsung", BrandSamsungBlue, BrandWhite, logoScale = 0.78f),
    RewardItem("5", "Puma", "₹1000 Gift Card", 2500, "logo_puma", BrandWhite, BrandBlack),
    RewardItem("6", "Nike", "₹2500 Gift Card", 5000, "logo_nike", BrandWhite, BrandBlack)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KarmaStoreScreen(userProfile: UserProfile, onProfileUpdate: (UserProfile) -> Unit) {
    val rewards = storeRewards

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
                                KarmaAmount("${item.points}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
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
                KarmaAmount("$totalPoints", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("You have ", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                    KarmaAmount(formatKarma(userProfile.karmaPoints), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
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
                    KarmaAmount("${item.points}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
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
                            color = currentTier.color
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
