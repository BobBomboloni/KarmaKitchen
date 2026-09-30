package com.example.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val tools: List<Map<String, Map<String, String>>>? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null
)

data class FoodAnalysisResult(
    val title: String,
    val quantity: String,
    val isVeg: Boolean,
    val quality: String,
    val shelfLife: String,
    val storageTip: String,
    val isSafeToDonate: Boolean = true,
    val rejectionReason: String? = null,
    val karmaScore: Int = 150,
    val rawResponse: String = ""
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}


data class ChatMessage(val isUser: Boolean, val text: String)

suspend fun chatWithGemini(history: List<ChatMessage>, newMessage: String): String = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.GEMINI_API_KEY
    if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
        return@withContext "Please configure your Gemini API Key in the settings."
    }
    
    val systemInstruction = "You are a helpful AI assistant for KarmaKitchen. Your MAIN priority is answering queries related to food donation. You also help with food storage and food safety. If a user asks about topics unrelated to food donation, storage, or safety, politely decline and steer the conversation back. Be concise, friendly, and practical. Do not use Markdown, just plain text if possible, or very simple formatting."
    
    val parts = mutableListOf<Part>()
    parts.add(Part(text = "System: " + systemInstruction))
    for (msg in history) {
        parts.add(Part(text = (if(msg.isUser) "User: " else "Assistant: ") + msg.text))
    }
    parts.add(Part(text = "User: " + newMessage))
    
    val request = GenerateContentRequest(
        contents = listOf(Content(parts = parts))
    )
    
    try {
        val response = retryWithBackoff { RetrofitClient.service.generateContent(apiKey, request) }
        val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: "Sorry, I couldn't generate a response."
        return@withContext responseText.trim()
    } catch (e: Exception) {
        Log.e("GeminiService", "Chat API failed", e)
        return@withContext "Sorry, I encountered an error. Please try again."
    }
}


suspend fun <T> retryWithBackoff(
    times: Int = 3,
    initialDelay: Long = 1000,
    maxDelay: Long = 5000,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            return block()
        } catch (e: Exception) {
            val isRetryable = e is retrofit2.HttpException && (e.code() == 503 || e.code() == 429)
            if (!isRetryable) {
                throw e
            }
            Log.w("GeminiService", "Retryable error ${e.code()} from Gemini, retrying in ${currentDelay}ms...", e)
            kotlinx.coroutines.delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
    }
    return block()
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

suspend fun uriToBase64(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    val maxDimension = 1024
    
    // Decode bounds first to avoid OutOfMemoryError
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    context.contentResolver.openInputStream(uri)?.use { 
        BitmapFactory.decodeStream(it, null, options)
    }
    
    options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
    options.inJustDecodeBounds = false
    
    val originalBitmap = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    } ?: throw IllegalArgumentException("Could not decode image from URI: $uri")

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
    Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
}

suspend fun analyzeFoodWithGemini(base64Image: String): FoodAnalysisResult = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.GEMINI_API_KEY
    if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
        throw IllegalStateException("Gemini API Key is not configured. Please add GEMINI_API_KEY in the Secrets panel.")
    }

    val prompt = """
        You are KarmaKitchen's AI Food Safety & Quality Inspector.
        Carefully analyze this food photograph for surplus food donation.
        
        Evaluate the following:
        1. Title: A clear, concise title of the dish or food items shown (e.g., 'Fresh Bakery Bagels', 'Vegetable Pasta', 'Catering Sandwich Platter', 'Fresh Fruit Basket').
        2. Quantity: An accurate estimate of the quantity and servings (e.g., '4-6 servings', '8 sandwiches (4 people)', '1 large bowl (3-4 servings)').
        3. Dietary: Is it Vegetarian/Vegan (true) or Non-Vegetarian containing meat/fish/poultry (false)?
        4. Quality & Freshness: Assess visible freshness ('Fresh & Excellent', 'Good Quality', 'Consume within today', 'Spoiled/Unsafe', 'Inedible/Rotten').
        5. Estimated Shelf Life: How long can this food safely last under proper storage? (e.g., '14 Hours', '24 Hours', '2 Days', '0 Hours / Expired').
        6. Storage & Handling Tip: A practical 1-sentence storage tip (e.g., 'Refrigerate immediately in an airtight container at 4°C.').
        7. Is it safe to donate / edible?: Set "safe": true ONLY if the food is fresh, edible, and safe for human consumption. Set "safe": false if the food is spoiled, expired, moldy, stale, rotting, half-eaten, contaminated, or unfit for human consumption.
        8. Rejection Reason: If safe is false, explain clearly why the food cannot be donated (e.g., 'Visible mold growth detected on surface', 'Food appears decayed and unsafe to consume'). If safe is true, leave as "".

        Return ONLY a raw JSON object with this exact structure (NO markdown wrappers, no backticks):
        {
          "title": "Fresh Fruit Bowl",
          "quantity": "4-6 servings",
          "isVeg": true,
          "quality": "Fresh & Verified",
          "shelfLife": "14 Hours",
          "storageTip": "Keep refrigerated in a sealed container",
          "safe": true,
          "rejectionReason": ""
        }
    """.trimIndent()

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
        val response = retryWithBackoff { RetrofitClient.service.generateContent(apiKey, request) }
        val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw Exception("No response returned from Gemini API.")

        // Strip any markdown code formatting if present
        val cleanJson = responseText
            .replace(Regex("^```json\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^```\\s*"), "")
            .replace(Regex("```$"), "")
            .trim()

        val jsonObject = JSONObject(cleanJson)
        val title = jsonObject.optString("title", "Food Item")
        val quantity = jsonObject.optString("quantity", "1-2 servings")
        val isVeg = jsonObject.optBoolean("isVeg", true)
        val quality = jsonObject.optString("quality", "Fresh & Verified")
        val shelfLife = jsonObject.optString("shelfLife", "12-24 Hours")
        val storageTip = jsonObject.optString("storageTip", "Keep chilled and covered.")
        val rawSafe = jsonObject.optBoolean("safe", true)
        val rejectionReason = jsonObject.optString("rejectionReason", "")

        val qualityLower = quality.lowercase()
        val isUnsafeKeywords = qualityLower.contains("spoil") ||
                qualityLower.contains("inedible") ||
                qualityLower.contains("unfit") ||
                qualityLower.contains("unsafe") ||
                qualityLower.contains("mold") ||
                qualityLower.contains("mould") ||
                qualityLower.contains("rot") ||
                qualityLower.contains("decay") ||
                qualityLower.contains("expired") ||
                qualityLower.contains("contaminat")

        val isSafe = rawSafe && !isUnsafeKeywords

        val finalRejectionReason = if (!isSafe) {
            if (rejectionReason.isNotBlank()) rejectionReason
            else "Food is evaluated as $quality and not fit for consumption."
        } else null

        FoodAnalysisResult(
            title = title,
            quantity = quantity,
            isVeg = isVeg,
            quality = quality,
            shelfLife = shelfLife,
            storageTip = storageTip,
            isSafeToDonate = isSafe,
            rejectionReason = finalRejectionReason,
            karmaScore = if (isSafe) 150 else 0,
            rawResponse = responseText
        )
    } catch (e: Exception) {
        Log.e("GeminiService", "Failed to analyze food image with Gemini", e)
        throw e
    }
}


@JsonClass(generateAdapter = true)
data class FoodWasteFacts(
    val worldWaste: String,
    val indiaWaste: String,
    val gujaratWaste: String,
    val indiaWasteKgPerSec: Double,
    val gujaratWasteKgPerSec: Double,
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
        Calculate the estimated amount of food wasted PER SECOND in kilograms for India and Gujarat based on the annual data (1 Tonne = 1000 kg).
        Also provide a short, highly encouraging 1-sentence positive message (max 10 words) about how donating food helps.
        
        Return ONLY a raw JSON object (NO markdown wrappers, no backticks) with exactly these keys:
        {
          "worldWaste": "...",
          "indiaWaste": "...",
          "gujaratWaste": "...",
          "indiaWasteKgPerSec": 2178.5,
          "gujaratWasteKgPerSec": 105.2,
          "positiveMessage": "..."
        }
    """.trimIndent()
    
    val request = GenerateContentRequest(
        contents = listOf(Content(parts = listOf(Part(text = prompt)))),
        tools = listOf(mapOf("googleSearch" to emptyMap()))
    )
    
    try {
        val response = retryWithBackoff { RetrofitClient.service.generateContent(apiKey, request) }
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
            indiaWasteKgPerSec = jsonObject.optDouble("indiaWasteKgPerSec", 2178.2),
            gujaratWasteKgPerSec = jsonObject.optDouble("gujaratWasteKgPerSec", 112.5),
            positiveMessage = jsonObject.optString("positiveMessage", "Your donation makes a real difference!")
        )
    } catch (e: Exception) {
        Log.w("GeminiService", "Failed to fetch food waste facts, falling back.")
        null
    }
}


data class IntakeAnalysisResult(
    val verifiedMatch: Boolean,
    val freshness: String,
    val estimatedExpiration: String,
    val storageInstructions: String,
    val dietaryTags: List<String>,
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

    val prompt = """
        You are KarmaKitchen's NGO Intake AI.
        Analyze this photograph of received food for intake logging.
        
        Evaluate the following:
        1. Verified Match: Does this look like typical donated food (e.g. prepared meals, fresh produce, packaged goods)? (true/false)
        2. Freshness: Give a short 2-5 word assessment (e.g. "Looks fresh and safe", "Slightly bruised, but edible").
        3. Estimated Expiration: Give a countdown based on visual freshness (e.g. "Expires in 12 hours", "Consume within 2 days").
        4. Storage Instructions: A short instruction for the NGO (e.g. "Refrigerate immediately", "Store in a cool dry place").
        5. Dietary Tags: Based on the visual analysis, assign a JSON array of up to 3 applicable tags (e.g. ["Vegan", "Gluten-Free", "High-Protein", "Nut-Free"]).
        
        Return ONLY a raw JSON object with this exact structure (NO markdown wrappers):
        {
          "verifiedMatch": true,
          "freshness": "...",
          "estimatedExpiration": "...",
          "storageInstructions": "...",
          "dietaryTags": ["Vegan", "High-Protein"]
        }
    """.trimIndent()

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
        val response = retryWithBackoff { RetrofitClient.service.generateContent(apiKey, request) }
        val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw Exception("No response returned from Gemini API.")
            
        val cleanJson = responseText
            .replace(Regex("^```json\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^```\\s*"), "")
            .replace(Regex("```$"), "")
            .trim()
            
        val jsonObject = org.json.JSONObject(cleanJson)
        
        val tagsArray = jsonObject.optJSONArray("dietaryTags")
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
        )
    } catch (e: Exception) {
        Log.e("GeminiService", "Failed to verify intake", e)
        throw e
    }
}

fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height: Int, width: Int) = options.outHeight to options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}
