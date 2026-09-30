import sys

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Add to Screen
screen_target = """sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Welcome : Screen("welcome", "Welcome", Icons.Filled.Star)"""
screen_replacement = """sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object RoleSelection : Screen("role_selection", "Role Selection", Icons.Filled.Star)
    object NgoDashboard : Screen("ngo_dashboard", "NGO Dashboard", Icons.Filled.Home)
    object Welcome : Screen("welcome", "Welcome", Icons.Filled.Star)"""
content = content.replace(screen_target, screen_replacement)

# Update NavHost
nav_target = """        NavHost(
            navController = navController,
            startDestination = Screen.Welcome.route,
            modifier = Modifier.padding(innerPadding)
        ) {"""
nav_replacement = """        NavHost(
            navController = navController,
            startDestination = Screen.RoleSelection.route,
            modifier = Modifier.padding(innerPadding)
        ) {"""
content = content.replace(nav_target, nav_replacement)

# Update routes inside NavHost
routes_target = """        ) {
            composable(Screen.Welcome.route) { WelcomeScreen(navController) }
            composable(Screen.Dashboard.route) { DonorDashboardScreen(navController, userProfile) }"""
routes_replacement = """        ) {
            composable(Screen.RoleSelection.route) { RoleSelectionScreen(navController) }
            composable(Screen.NgoDashboard.route) { NgoDashboardScreen(navController) }
            composable(Screen.Welcome.route) { WelcomeScreen(navController) }
            composable(Screen.Dashboard.route) { DonorDashboardScreen(navController, userProfile) }"""
content = content.replace(routes_target, routes_replacement)

# Add UI components at the end
ngo_code = """
@Composable
fun RoleSelectionScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Mock Logo / Title
        Icon(
            Icons.Filled.Favorite, 
            contentDescription = "KarmaKitchen Logo",
            tint = Color(0xFF34C759),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "KarmaKitchen",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Text(
            text = "Unified Donation Platform",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(64.dp))
        
        // Donor Button
        Button(
            onClick = { navController.navigate(Screen.Welcome.route) }, // Or Dashboard directly
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759))
        ) {
            Text(
                "I want to Donate Food",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
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
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9F0A))
        ) {
            Text(
                "I am an NGO / Receiver",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

@Composable
fun NgoDashboardScreen(navController: NavController) {
    var isBroadcasting by remember { mutableStateOf(false) }
    var isAccepting by remember { mutableStateOf(true) }

    val mockDeliveries = listOf(
        Triple("20 Servings - Mixed Veg", "Driver ETA: 12 Mins", "1.2 km away"),
        Triple("50 Assorted Breads", "Driver ETA: 25 Mins", "3.4 km away"),
        Triple("1 Large Catering Tray", "Driver ETA: 45 Mins", "5.1 km away")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Operational Command", style = MaterialTheme.typography.labelMedium, color = Color(0xFFFF9F0A), fontWeight = FontWeight.Bold)
                Text("Navrachana Community", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Black)
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1C1C1E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, contentDescription = "Profile", tint = Color.White)
            }
        }

        // Metrics Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NgoMetricCard(title = "Meals Today", value = "342", modifier = Modifier.weight(1f))
            NgoMetricCard(title = "Capacity", value = "85%", modifier = Modifier.weight(1f))
            NgoMetricCard(title = "Active", value = "3", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Center
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text("Action Center", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            
            // Broadcast Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1C1C1E))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Urgent Need Broadcast", style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(if (isBroadcasting) "Broadcasting to local donors" else "Currently inactive", style = MaterialTheme.typography.bodyMedium, color = if (isBroadcasting) Color(0xFFFF9F0A) else Color.Gray)
                }
                androidx.compose.material3.Switch(
                    checked = isBroadcasting,
                    onCheckedChange = { isBroadcasting = it },
                    colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = Color(0xFFFF9F0A))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Accepting Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1C1C1E))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Accepting Donations", style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Manage warehouse capacity", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
                androidx.compose.material3.Switch(
                    checked = isAccepting,
                    onCheckedChange = { isAccepting = it },
                    colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = Color(0xFF34C759))
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Incoming Radar
        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Text(
                "Live Incoming Deliveries",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(mockDeliveries.size) { index ->
                    val delivery = mockDeliveries[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1C1C1E))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF34C759).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = Color(0xFF34C759))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(delivery.first, style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("${delivery.second} • ${delivery.third}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    }
                    // Actions inside card
                    Button(
                        onClick = { /* Log Intake */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp, topStart = 4.dp, topEnd = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759))
                    ) {
                        Text("Accept & Log AI Intake", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun NgoMetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1C1C1E))
            .padding(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}
"""

content += ngo_code

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
