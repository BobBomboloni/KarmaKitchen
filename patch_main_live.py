import sys

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Update the facts fallback
old_facts_init = """        facts = com.example.api.FoodWasteFacts(
            worldWaste = "1.3B Tonnes",
            indiaWaste = "68.7M Tonnes",
            gujaratWaste = "Loading...",
            positiveMessage = "Fetching latest impact data..."
        )"""

new_facts_init = """        facts = com.example.api.FoodWasteFacts(
            worldWaste = "1.3B Tonnes",
            indiaWaste = "68.7M Tonnes",
            gujaratWaste = "Loading...",
            indiaWasteKgPerSec = 2178.2,
            gujaratWasteKgPerSec = 112.5,
            positiveMessage = "Fetching latest impact data..."
        )"""

content = content.replace(old_facts_init, new_facts_init)

old_facts_fallback_1 = """                facts = com.example.api.FoodWasteFacts(
                    worldWaste = "1.3 Billion Tonnes",
                    indiaWaste = "68 Million Tonnes",
                    gujaratWaste = "Thousands of Tonnes",
                    positiveMessage = "Your donation creates a ripple of hope!"
                )"""

new_facts_fallback_1 = """                facts = com.example.api.FoodWasteFacts(
                    worldWaste = "1.3 Billion Tonnes",
                    indiaWaste = "68 Million Tonnes",
                    gujaratWaste = "Thousands of Tonnes",
                    indiaWasteKgPerSec = 2178.2,
                    gujaratWasteKgPerSec = 112.5,
                    positiveMessage = "Your donation creates a ripple of hope!"
                )"""

content = content.replace(old_facts_fallback_1, new_facts_fallback_1)

old_facts_fallback_2 = """             facts = com.example.api.FoodWasteFacts(
                worldWaste = "1.3 Billion Tonnes",
                indiaWaste = "68 Million Tonnes",
                gujaratWaste = "Thousands of Tonnes",
                positiveMessage = "Every meal you donate counts."
            )"""

new_facts_fallback_2 = """             facts = com.example.api.FoodWasteFacts(
                worldWaste = "1.3 Billion Tonnes",
                indiaWaste = "68 Million Tonnes",
                gujaratWaste = "Thousands of Tonnes",
                indiaWasteKgPerSec = 2178.2,
                gujaratWasteKgPerSec = 112.5,
                positiveMessage = "Every meal you donate counts."
            )"""

content = content.replace(old_facts_fallback_2, new_facts_fallback_2)

# Update the Composable

old_row = """            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FactItem(title = "World", value = facts?.worldWaste ?: "", icon = Icons.Filled.Public)
                FactItem(title = "India", value = facts?.indiaWaste ?: "", icon = Icons.Filled.Flag)
                FactItem(title = "Gujarat", value = facts?.gujaratWaste ?: "", icon = Icons.Filled.LocationCity)
            }"""

new_row = """            // Ticking timer
            var ticks by remember { mutableStateOf(0L) }
            LaunchedEffect(Unit) {
                while(true) {
                    kotlinx.coroutines.delay(200)
                    ticks += 1
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FactItem(title = "World (Yearly)", value = facts?.worldWaste ?: "", icon = Icons.Filled.Public)
                
                val indiaLive = if (facts != null) "%,d kg".format((facts!!.indiaWasteKgPerSec * (ticks * 0.2)).toInt()) else ""
                FactItem(
                    title = "India (Since you opened)",
                    value = if (isLoading) (facts?.indiaWaste ?: "") else indiaLive, 
                    icon = Icons.Filled.Flag
                )
                
                val gujaratLive = if (facts != null) "%,d kg".format((facts!!.gujaratWasteKgPerSec * (ticks * 0.2)).toInt()) else ""
                FactItem(
                    title = "Gujarat (Since you opened)",
                    value = if (isLoading) (facts?.gujaratWaste ?: "") else gujaratLive, 
                    icon = Icons.Filled.LocationCity
                )
            }"""
content = content.replace(old_row, new_row)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

