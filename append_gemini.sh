cat << 'INNER_EOF' >> ./app/src/main/java/com/example/api/GeminiService.kt

@JsonClass(generateAdapter = true)
data class DonorPlace(
    val name: String,
    val description: String,
    val type: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
)

suspend fun findNearbyDonors(location: String): List<DonorPlace> = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.GEMINI_API_KEY
    if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
        throw IllegalStateException("Gemini API Key is not configured.")
    }

    val prompt = """
        Find nearby NGOs, Gurudwaras, or institutes donating food in or near ${'$'}location.
        Provide at least 5 relevant locations.
        Return ONLY a raw JSON array of objects with this exact structure (NO markdown wrappers):
        [
          {
            "name": "Gurudwara Bangla Sahib",
            "description": "Langar serving thousands daily.",
            "type": "Gurudwara",
            "address": "Ashoka Road, New Delhi",
            "latitude": 28.6264,
            "longitude": 77.2089
          }
        ]
    """.trimIndent()

    val request = GenerateContentRequest(
        contents = listOf(Content(parts = listOf(Part(text = prompt)))),
        tools = listOf(mapOf("googleMaps" to emptyMap())) // Use Maps Grounding
    )

    try {
        val response = RetrofitClient.service.generateContent(apiKey, request)
        val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw Exception("No response returned from Gemini API.")
            
        val cleanJson = responseText
            .replace(Regex("^```json\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^```\\s*"), "")
            .replace(Regex("```$"), "")
            .trim()
            
        val jsonArray = org.json.JSONArray(cleanJson)
        val places = mutableListOf<DonorPlace>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            places.add(
                DonorPlace(
                    name = obj.optString("name", "Unknown Location"),
                    description = obj.optString("description", "Food Donation Center"),
                    type = obj.optString("type", "NGO"),
                    address = obj.optString("address", "Unknown Address"),
                    latitude = obj.optDouble("latitude", 0.0),
                    longitude = obj.optDouble("longitude", 0.0)
                )
            )
        }
        places
    } catch (e: Exception) {
        Log.e("GeminiService", "Failed to find nearby donors", e)
        throw e
    }
}
INNER_EOF
