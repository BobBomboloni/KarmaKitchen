package com.example.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.cloud.Cloud
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/**
 * Gemini is called through Firebase AI Logic (Gemini Developer API), so no API key ships inside
 * the APK; App Check vouches for the app instead. A busy Gemini (503 "overloaded") is retried for
 * about 15 seconds, first on [PRIMARY_MODEL] and then on [FALLBACK_MODEL]. Any other error goes
 * straight to the fallback model.
 */
private const val PRIMARY_MODEL = "gemini-3.6-flash"
private const val FALLBACK_MODEL = "gemini-flash-lite-latest"
private val RETRY_DELAYS_MS = listOf(1_000L, 2_000L, 4_000L)

/** Shown when Gemini stays overloaded after every retry. */
const val AI_BUSY_MESSAGE = "Our AI is busy right now. Tap Try again in a moment."
const val AI_NOT_SET_UP_MESSAGE =
    "The AI check needs the app to be connected to Firebase. See Setup in the README."
const val AI_FAILED_MESSAGE = "The AI couldn't answer this time. Check your internet and try again."

/**
 * A Gemini failure with a message that can be shown to the person as it is. Debug builds add the
 * underlying error, so a tester can see what went wrong without opening Logcat.
 */
class AiException(message: String, cause: Throwable? = null) : Exception(
    if (BuildConfig.DEBUG && cause != null) "$message\n\nDebug details: ${errorSummary(cause)}" else message,
    cause
)

internal fun errorSummary(e: Throwable): String = "${e::class.simpleName}: ${e.message}".take(300)

/** True when Gemini is overloaded for a moment (HTTP 503), so retrying the same model can help. */
internal fun isBusyError(e: Throwable): Boolean {
    val text = e.message ?: ""
    return listOf("503", "overloaded", "unavailable", "try again later").any { text.contains(it, ignoreCase = true) }
}

/** True when this model's request limit is used up (HTTP 429), so the fallback model is worth a try. */
internal fun isQuotaError(e: Throwable): Boolean {
    val text = e.message ?: ""
    return e::class.simpleName == "QuotaExceededException" ||
        listOf("429", "RESOURCE_EXHAUSTED").any { text.contains(it, ignoreCase = true) }
}

private fun model(
    name: String,
    systemInstruction: String? = null,
    tools: List<Tool>? = null
): GenerativeModel = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
    modelName = name,
    systemInstruction = systemInstruction?.let { content { text(it) } },
    tools = tools
)

/**
 * Sends [prompt] to Gemini and returns the reply text. Retries busy errors on the main model,
 * then on the fallback model, and throws [AiException] with a friendly message when all fail.
 */
private suspend fun generateText(
    prompt: List<Content>,
    systemInstruction: String? = null,
    tools: List<Tool>? = null
): String {
    if (!Cloud.enabled) throw AiException(AI_NOT_SET_UP_MESSAGE)
    var lastError: Exception? = null
    for (name in listOf(PRIMARY_MODEL, FALLBACK_MODEL)) {
        val generativeModel = model(name, systemInstruction, tools)
        for (attempt in 0..RETRY_DELAYS_MS.size) {
            try {
                // Passed as first + rest so it fits both the (vararg) and (first, vararg rest) signatures.
                val response = generativeModel.generateContent(prompt.first(), *prompt.drop(1).toTypedArray())
                val text: String? = response.text
                if (!text.isNullOrBlank()) return text
                throw AiException(AI_FAILED_MESSAGE)
            } catch (e: CancellationException) {
                throw e
            } catch (e: AiException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                if (!isBusyError(e)) {
                    // Other errors won't fix themselves on a retry, but the fallback model may still
                    // work (for example when this one is out of free requests or was retired).
                    Log.e("GeminiService", "Gemini call failed on $name", e)
                    break
                }
                Log.w("GeminiService", "Gemini $name busy (attempt ${attempt + 1})", e)
                RETRY_DELAYS_MS.getOrNull(attempt)?.let { delay(it) }
            }
        }
    }
    val busy = lastError != null && (isBusyError(lastError) || isQuotaError(lastError))
    throw AiException(if (busy) AI_BUSY_MESSAGE else AI_FAILED_MESSAGE, lastError)
}

/** Strips a ```json fence if the model added one anyway. */
private fun stripJsonFence(responseText: String): String = responseText.trim()
    .replace(Regex("^```json\\s*", RegexOption.IGNORE_CASE), "")
    .replace(Regex("^```\\s*"), "")
    .replace(Regex("```$"), "")
    .trim()

private fun imagePrompt(prompt: String, base64Image: String): List<Content> = listOf(
    content {
        text(prompt)
        inlineData(Base64.decode(base64Image, Base64.NO_WRAP), "image/jpeg")
    }
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

data class ChatMessage(val isUser: Boolean, val text: String)

suspend fun chatWithGemini(history: List<ChatMessage>, newMessage: String): String = withContext(Dispatchers.IO) {
    val systemInstruction = "You are a helpful AI assistant for KarmaKitchen. Your MAIN priority is answering queries related to food donation. You also help with food storage and food safety. If a user asks about topics unrelated to food donation, storage, or safety, politely decline and steer the conversation back. Be concise, friendly, and practical. Do not use Markdown, just plain text if possible, or very simple formatting."

    val contents = history.map { content(role = if (it.isUser) "user" else "model") { text(it.text) } } +
        content(role = "user") { text(newMessage) }

    try {
        generateText(contents, systemInstruction = systemInstruction).trim()
    } catch (e: AiException) {
        e.message ?: "Sorry, I encountered an error. Please try again."
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

/** Throws [AiException] with a message that can be shown on screen as it is. */
suspend fun analyzeFoodWithGemini(base64Image: String): FoodAnalysisResult = withContext(Dispatchers.IO) {

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

    val responseText = generateText(imagePrompt(prompt, base64Image))
    try {
        val cleanJson = stripJsonFence(responseText)

        val jsonObject = JSONObject(cleanJson)
        val title = jsonObject.optString("title", "Food Item")
        val quantity = jsonObject.optString("quantity", "1-2 servings")
        val isVeg = jsonObject.optBoolean("isVeg", true)
        val quality = jsonObject.optString("quality", "Unverified")
        val shelfLife = jsonObject.optString("shelfLife", "Unknown")
        val storageTip = jsonObject.optString("storageTip", "Keep chilled and covered.")
        val rawSafe = jsonObject.optBoolean("safe", false) // fail closed
        val rejectionReason = jsonObject.optString("rejectionReason", "")

        val isUnsafeKeywords = com.example.isUnsafeQuality(quality)

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
        Log.e("GeminiService", "Could not read Gemini's food analysis: $responseText", e)
        throw AiException(AI_FAILED_MESSAGE, e)
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
    if (!Cloud.enabled) return@withContext null
    
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
    
    try {
        val responseText = generateText(listOf(content { text(prompt) }), tools = listOf(Tool.googleSearch()))
        val cleanJson = stripJsonFence(responseText)

        val jsonObject = org.json.JSONObject(cleanJson)
        FoodWasteFacts(
            worldWaste = jsonObject.optString("worldWaste", "1.05 Billion Tonnes"),
            indiaWaste = jsonObject.optString("indiaWaste", "78 Million Tonnes"),
            gujaratWaste = jsonObject.optString("gujaratWaste", "Thousands of Tonnes"),
            indiaWasteKgPerSec = jsonObject.optDouble("indiaWasteKgPerSec", 2178.2),
            gujaratWasteKgPerSec = jsonObject.optDouble("gujaratWasteKgPerSec", 112.5),
            positiveMessage = jsonObject.optString("positiveMessage", "Your donation makes a real difference!")
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w("GeminiService", "Failed to fetch food waste facts, falling back.", e)
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

    val responseText = generateText(imagePrompt(prompt, base64Image))
    try {
        val cleanJson = stripJsonFence(responseText)

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
        Log.e("GeminiService", "Could not read Gemini's intake check: $responseText", e)
        throw AiException(AI_FAILED_MESSAGE, e)
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
