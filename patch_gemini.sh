cat << 'INNER_EOF' >> ./app/src/main/java/com/example/api/GeminiService.kt

@JsonClass(generateAdapter = true)
data class FoodWasteFacts(
    val worldWaste: String,
    val indiaWaste: String,
    val gujaratWaste: String,
    val positiveMessage: String
)

suspend fun fetchFoodWasteFacts(): FoodWasteFacts? = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.GEMINI_API_KEY
    if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
        return@withContext null
    }
    
    val prompt = """
        Using Google Search, find the latest statistics on how much food is wasted annually in:
        1. The World
        2. India
        3. Gujarat
        Format the statistics to be short and impactful strings (e.g., '1.3 Billion Tonnes', '68 Million Tonnes', 'Thousands of Tonnes').
        Also provide a short, highly encouraging 1-sentence positive message (max 10 words) about how donating food helps.
        
        Return ONLY a raw JSON object (NO markdown wrappers, no backticks) with exactly these keys:
        {
          "worldWaste": "...",
          "indiaWaste": "...",
          "gujaratWaste": "...",
          "positiveMessage": "..."
        }
    """.trimIndent()
    
    val request = GenerateContentRequest(
        contents = listOf(Content(parts = listOf(Part(text = prompt)))),
        tools = listOf(mapOf("googleSearch" to emptyMap()))
    )
    
    try {
        val response = RetrofitClient.service.generateContent(apiKey, request)
        val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw Exception("No response")
            
        val cleanJson = responseText
            .replace(Regex("^```json\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^```\\s*"), "")
            .replace(Regex("```$"), "")
            .trim()
            
        val jsonObject = org.json.JSONObject(cleanJson)
        FoodWasteFacts(
            worldWaste = jsonObject.optString("worldWaste", "1.3 Billion Tonnes"),
            indiaWaste = jsonObject.optString("indiaWaste", "68.7 Million Tonnes"),
            gujaratWaste = jsonObject.optString("gujaratWaste", "Thousands of Tonnes"),
            positiveMessage = jsonObject.optString("positiveMessage", "Your donation makes a real difference!")
        )
    } catch (e: Exception) {
        Log.e("GeminiService", "Failed to fetch food waste facts", e)
        null
    }
}
INNER_EOF
