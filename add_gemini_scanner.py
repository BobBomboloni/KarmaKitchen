import sys

with open("app/src/main/java/com/example/api/GeminiService.kt", "r") as f:
    content = f.read()

injection = """
data class IntakeAnalysisResult(
    val verifiedMatch: Boolean,
    val freshness: String,
    val estimatedExpiration: String,
    val storageInstructions: String,
    val rawResponse: String = ""
)

fun bitmapToBase64(originalBitmap: Bitmap): String {
    val maxDimension = 1024
    val scaledBitmap = if (originalBitmap.width > maxDimension || originalBitmap.height > maxDimension) {
        val aspectRatio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
        val targetWidth = if (aspectRatio >= 1) maxDimension else (maxDimension * aspectRatio).toInt()
        val targetHeight = if (aspectRatio >= 1) (maxDimension / aspectRatio).toInt() else maxDimension
        Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
    } else {
        originalBitmap
    }
    val outputStream = ByteArrayOutputStream()
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
}

suspend fun verifyIntakeWithGemini(base64Image: String): IntakeAnalysisResult = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.GEMINI_API_KEY
    if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
        throw IllegalStateException("Gemini API Key is not configured.")
    }

    val prompt = \"\"\"
        You are KarmaKitchen's NGO Intake AI.
        Analyze this photograph of received food for intake logging.
        
        Evaluate the following:
        1. Verified Match: Does this look like typical donated food (e.g. prepared meals, fresh produce, packaged goods)? (true/false)
        2. Freshness: Give a short 2-5 word assessment (e.g. "Looks fresh and safe", "Slightly bruised, but edible").
        3. Estimated Expiration: Give a countdown based on visual freshness (e.g. "Expires in 12 hours", "Consume within 2 days").
        4. Storage Instructions: A short instruction for the NGO (e.g. "Refrigerate immediately", "Store in a cool dry place").
        
        Return ONLY a raw JSON object with this exact structure (NO markdown wrappers):
        {
          "verifiedMatch": true,
          "freshness": "...",
          "estimatedExpiration": "...",
          "storageInstructions": "..."
        }
    \"\"\".trimIndent()

    val request = GenerateContentRequest(
        contents = listOf(
            Content(
                parts = listOf(
                    Part(text = prompt),
                    Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                )
            )
        )
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
            
        val jsonObject = org.json.JSONObject(cleanJson)
        
        IntakeAnalysisResult(
            verifiedMatch = jsonObject.optBoolean("verifiedMatch", true),
            freshness = jsonObject.optString("freshness", "Visually Fresh"),
            estimatedExpiration = jsonObject.optString("estimatedExpiration", "12-24 Hours"),
            storageInstructions = jsonObject.optString("storageInstructions", "Store safely per guidelines"),
            rawResponse = responseText
        )
    } catch (e: Exception) {
        Log.e("GeminiService", "Failed to verify intake", e)
        throw e
    }
}
"""

if "data class IntakeAnalysisResult" not in content:
    content += "\n" + injection
    with open("app/src/main/java/com/example/api/GeminiService.kt", "w") as f:
        f.write(content)

