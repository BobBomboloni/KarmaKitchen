import sys

with open("app/src/main/java/com/example/api/GeminiService.kt", "r") as f:
    content = f.read()

# Update data class
old_dataclass = """data class IntakeAnalysisResult(
    val verifiedMatch: Boolean,
    val freshness: String,
    val estimatedExpiration: String,
    val storageInstructions: String,
    val rawResponse: String = ""
)"""
new_dataclass = """data class IntakeAnalysisResult(
    val verifiedMatch: Boolean,
    val freshness: String,
    val estimatedExpiration: String,
    val storageInstructions: String,
    val dietaryTags: List<String>,
    val rawResponse: String = ""
)"""
content = content.replace(old_dataclass, new_dataclass)

# Update prompt
old_prompt = """        3. Estimated Expiration: Give a countdown based on visual freshness (e.g. "Expires in 12 hours", "Consume within 2 days").
        4. Storage Instructions: A short instruction for the NGO (e.g. "Refrigerate immediately", "Store in a cool dry place").
        
        Return ONLY a raw JSON object with this exact structure (NO markdown wrappers):
        {
          "verifiedMatch": true,
          "freshness": "...",
          "estimatedExpiration": "...",
          "storageInstructions": "..."
        }"""
new_prompt = """        3. Estimated Expiration: Give a countdown based on visual freshness (e.g. "Expires in 12 hours", "Consume within 2 days").
        4. Storage Instructions: A short instruction for the NGO (e.g. "Refrigerate immediately", "Store in a cool dry place").
        5. Dietary Tags: Based on the visual analysis, assign a JSON array of up to 3 applicable tags (e.g. ["Vegan", "Gluten-Free", "High-Protein", "Nut-Free"]).
        
        Return ONLY a raw JSON object with this exact structure (NO markdown wrappers):
        {
          "verifiedMatch": true,
          "freshness": "...",
          "estimatedExpiration": "...",
          "storageInstructions": "...",
          "dietaryTags": ["Vegan", "High-Protein"]
        }"""
content = content.replace(old_prompt, new_prompt)

# Update JSON parsing
old_parsing = """        IntakeAnalysisResult(
            verifiedMatch = jsonObject.optBoolean("verifiedMatch", true),
            freshness = jsonObject.optString("freshness", "Visually Fresh"),
            estimatedExpiration = jsonObject.optString("estimatedExpiration", "12-24 Hours"),
            storageInstructions = jsonObject.optString("storageInstructions", "Store safely per guidelines"),
            rawResponse = responseText
        )"""
new_parsing = """        val tagsArray = jsonObject.optJSONArray("dietaryTags")
        val parsedTags = mutableListOf<String>()
        if (tagsArray != null) {
            for (i in 0 until tagsArray.length()) {
                parsedTags.add(tagsArray.getString(i))
            }
        }
        if (parsedTags.isEmpty()) parsedTags.add("Standard Diet")

        IntakeAnalysisResult(
            verifiedMatch = jsonObject.optBoolean("verifiedMatch", true),
            freshness = jsonObject.optString("freshness", "Visually Fresh"),
            estimatedExpiration = jsonObject.optString("estimatedExpiration", "12-24 Hours"),
            storageInstructions = jsonObject.optString("storageInstructions", "Store safely per guidelines"),
            dietaryTags = parsedTags,
            rawResponse = responseText
        )"""
content = content.replace(old_parsing, new_parsing)

with open("app/src/main/java/com/example/api/GeminiService.kt", "w") as f:
    f.write(content)


with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content2 = f.read()

# Add horizontalScroll import
if "import androidx.compose.foundation.horizontalScroll" not in content2:
    content2 = content2.replace("import androidx.compose.foundation.layout.Box", "import androidx.compose.foundation.layout.Box\nimport androidx.compose.foundation.horizontalScroll\nimport androidx.compose.foundation.rememberScrollState")

# Update fallback result
old_fallback = """                    analysisResult = IntakeAnalysisResult(
                        verifiedMatch = true,
                        freshness = "Visually Fresh & Verified",
                        estimatedExpiration = "Consume within 24 Hours",
                        storageInstructions = "Refrigerate immediately at 4°C"
                    )"""
new_fallback = """                    analysisResult = IntakeAnalysisResult(
                        verifiedMatch = true,
                        freshness = "Visually Fresh & Verified",
                        estimatedExpiration = "Consume within 24 Hours",
                        storageInstructions = "Refrigerate immediately at 4°C",
                        dietaryTags = listOf("Vegan", "High-Protein", "Gluten-Free")
                    )"""
content2 = content2.replace(old_fallback, new_fallback)

# Update dialog UI
old_dialog = """                    Text("Expiration: ${res.estimatedExpiration}", color = Color(0xFFFF9F0A), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Storage: ${res.storageInstructions}", color = Color(0xFF34C759), fontWeight = FontWeight.Bold)
                }
            },"""
new_dialog = """                    Text("Expiration: ${res.estimatedExpiration}", color = Color(0xFFFF9F0A), fontWeight = FontWeight.Bold)
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
            },"""
content2 = content2.replace(old_dialog, new_dialog)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content2)

