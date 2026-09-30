import sys

with open("./app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Add imports
imports = """
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
"""

if "import androidx.compose.material.icons.filled.Public" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.Favorite", "import androidx.compose.material.icons.filled.Favorite\n" + imports)

# Find the spot to insert the FoodWasteFactBar in DonorDashboardScreen
target_spot = """        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Who Needs Food","""

injection = """        item {
            FoodWasteFactBar()
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Who Needs Food","""

content = content.replace(target_spot, injection)

# Add the Composable itself at the end of the file
composable_code = """
@Composable
fun FoodWasteFactBar() {
    var facts by remember { mutableStateOf<com.example.api.FoodWasteFacts?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isLoading = true
        // Default facts as a fallback immediately for UI responsiveness, then load actual
        facts = com.example.api.FoodWasteFacts(
            worldWaste = "1.3B Tonnes",
            indiaWaste = "68.7M Tonnes",
            gujaratWaste = "Loading...",
            positiveMessage = "Fetching latest impact data..."
        )
        
        try {
            val fetchedFacts = com.example.api.fetchFoodWasteFacts()
            if (fetchedFacts != null) {
                facts = fetchedFacts
            } else {
                facts = com.example.api.FoodWasteFacts(
                    worldWaste = "1.3 Billion Tonnes",
                    indiaWaste = "68 Million Tonnes",
                    gujaratWaste = "Thousands of Tonnes",
                    positiveMessage = "Your donation creates a ripple of hope!"
                )
            }
        } catch (e: Exception) {
             facts = com.example.api.FoodWasteFacts(
                worldWaste = "1.3 Billion Tonnes",
                indiaWaste = "68 Million Tonnes",
                gujaratWaste = "Thousands of Tonnes",
                positiveMessage = "Every meal you donate counts."
            )
        } finally {
            isLoading = false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Public, contentDescription = "World", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("The Scale of the Problem", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading && facts?.worldWaste == "1.3B Tonnes") {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FactItem(title = "World", value = facts?.worldWaste ?: "", icon = Icons.Filled.Public)
                FactItem(title = "India", value = facts?.indiaWaste ?: "", icon = Icons.Filled.Flag)
                FactItem(title = "Gujarat", value = facts?.gujaratWaste ?: "", icon = Icons.Filled.LocationCity)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Heart", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = facts?.positiveMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun FactItem(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
        Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, textAlign = TextAlign.Center)
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
    }
}
"""

if "fun FoodWasteFactBar" not in content:
    content += "\n" + composable_code

with open("./app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

