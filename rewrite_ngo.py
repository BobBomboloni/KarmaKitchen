import sys
import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# I will just write a new NgoDashboardScreen and replace the old one

new_ngo = """@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NgoDashboardScreen(navController: NavController) {
    var isBroadcasting by remember { mutableStateOf(false) }
    var isAccepting by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<IntakeAnalysisResult?>(null) }
    var showResultDialog by remember { mutableStateOf(false) }
    
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
                    analysisResult = IntakeAnalysisResult(
                        verifiedMatch = true,
                        freshness = "Visually Fresh & Verified",
                        estimatedExpiration = "Consume within 24 Hours",
                        storageInstructions = "Refrigerate immediately at 4°C",
                        dietaryTags = listOf("Vegan", "High-Protein", "Gluten-Free")
                    )
                    showResultDialog = true
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
        Triple("Fresh Bakery Bagels", "Expires in 2 days", "Fresh"),
        Triple("Mixed Veggie Pasta", "Expires in 4 hours", "Expiring Soon"),
        Triple("Catering Salads", "Expired 2 hours ago", "Expired")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
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
        }

        // Metrics Row
        item {
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
        }

        // Action Center
        item {
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
        }

        // Incoming Radar
        item {
            Text(
                "Live Incoming Deliveries",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
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
                    .background(Color(0xFF1C1C1E))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759))
                ) {
                    Text("Accept & Log AI Intake", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Current Inventory",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        items(mockInventory.size) { index ->
            val inventory = mockInventory[index]
            val statusColor = when(inventory.third) {
                "Fresh" -> Color(0xFF34C759)
                "Expiring Soon" -> Color(0xFFFF9F0A)
                else -> Color(0xFFFF453A) // Red for expired
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1C1C1E))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Inventory, contentDescription = null, tint = statusColor)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(inventory.first, style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(inventory.second, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
                
                // Status Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(inventory.third, color = statusColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (isAnalyzing) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { },
            title = { Text("Smart Intake Scanner", color = Color.White) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = Color(0xFF34C759))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("AI is verifying food freshness...", color = Color.LightGray)
                }
            },
            confirmButton = { },
            containerColor = Color(0xFF1C1C1E),
            titleContentColor = Color.White,
            textContentColor = Color.LightGray
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
                        tint = if (res.verifiedMatch) Color(0xFF34C759) else Color(0xFFFF9F0A),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Intake Logged", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("The AI has verified this donation against the donor's original listing.", color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Freshness: ${res.freshness}", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Expiration: ${res.estimatedExpiration}", color = Color(0xFFFF9F0A), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Storage: ${res.storageInstructions}", color = Color(0xFF34C759), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Automated Sorting:", color = Color.LightGray, style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        res.dietaryTags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF34C759).copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(tag, color = Color(0xFF34C759), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showResultDialog = false }) {
                    Text("Complete Intake", color = Color(0xFF34C759), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF1C1C1E)
        )
    }
}"""

# Find the start and end of NgoDashboardScreen in content and replace it
start_idx = content.find("@OptIn(ExperimentalPermissionsApi::class)\n@Composable\nfun NgoDashboardScreen")
if start_idx == -1:
    print("Could not find start of NgoDashboardScreen")
    sys.exit(1)

# Find the end by looking for NgoMetricCard
end_idx = content.find("@Composable\nfun NgoMetricCard")
if end_idx == -1:
    print("Could not find end of NgoDashboardScreen")
    sys.exit(1)

content = content[:start_idx] + new_ngo + "\n\n" + content[end_idx:]

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
