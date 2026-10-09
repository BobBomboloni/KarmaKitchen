package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import com.example.api.calculateInSampleSize
import com.example.cloud.Cloud
import com.example.cloud.CloudSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/** The NGO this phone represents when it is used in the receiver role. */
const val NGO_NAME = "Navrachana Community"

/** A food donation the NGO has received and can send a smile photo for. */
data class Receival(
    val id: String,
    val title: String,
    val receivedText: String,
    val servings: Int
)

/** Matches the donor's recent delivered donations so the demo loop stays consistent. */
val sampleReceivals = listOf(
    Receival("sandwiches", "Leftover Catering Sandwiches", "yesterday, 7:45 PM", 18),
    Receival("veg-basket", "Fresh Vegetable Basket", "29 Sep", 25),
    Receival("bakery", "Bakery Surplus, 20 breads", "26 Sep", 20)
)

/** A photo of people enjoying a donation, sent by an NGO to the donor. */
data class SmileEntry(
    val id: String,
    val donationId: String,
    val donationTitle: String,
    val ngoName: String,
    val message: String,
    val people: Int,
    val photoPath: String,
    val sentAt: Long,
    /** True for the built-in sample photos that show how the Smile Wall looks. */
    val isExample: Boolean = false
)

/**
 * True when the receiver's Smiles tab lists this photo under "Sent by you". The built-in example
 * photos count as well, so that list is not empty the first time the receiver opens it. With an
 * account, an NGO's list only holds the smiles it sent, so all of them count.
 */
fun SmileEntry.isSentByThisNgo(): Boolean = isExample || Cloud.enabled || ngoName == NGO_NAME

/**
 * Smile photos. Without an account, photos live in the app's private storage and the list is kept
 * in SharedPreferences, so the NGO side and the donor side of this phone share it. With an account,
 * the list holds the example photos plus the signed-in person's smiles from the cloud: the ones an
 * NGO sent, or the ones a donor received ([CloudSync] keeps them in step).
 */
object SmileStore {
    private const val PREFS = "karmakitchen_smiles"
    private const val KEY = "entries"
    private const val KEY_EXAMPLES_SEEDED = "examples_seeded"
    private const val DAY_MS = 24L * 60 * 60 * 1000
    private var loaded = false

    /** Newest first. Compose reads of this list update automatically. */
    val smiles = mutableStateListOf<SmileEntry>()

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY, null)?.let { raw ->
            // Skip entries whose photo file is gone (for example after clearing app data). With an
            // account only the examples are kept here; everything else comes from the cloud.
            smiles.addAll(smilesFromJson(raw).filter { File(it.photoPath).exists() && (it.isExample || !Cloud.enabled) })
        }
        // Add the sample photos once, so the wall is not empty the first time it is opened.
        if (!prefs.getBoolean(KEY_EXAMPLES_SEEDED, false)) {
            seedExamples(context)
            prefs.edit().putBoolean(KEY_EXAMPLES_SEEDED, true).apply()
        }
        // Smiles from the cloud may have arrived first.
        val sorted = smiles.sortedByDescending { it.sentAt }
        smiles.clear()
        smiles.addAll(sorted)
    }

    private class ExampleSpec(
        val id: String,
        val drawable: Int,
        val donationTitle: String,
        val ngoName: String,
        val message: String,
        val people: Int,
        val daysAgo: Int
    )

    private fun seedExamples(context: Context) {
        val specs = listOf(
            ExampleSpec(
                "example-1", R.drawable.smile_example_1, "Dal Khichdi, 30 servings",
                "Annapurna Seva Trust", "Our little ones finished every bite. Thank you!", 30, 2
            ),
            ExampleSpec(
                "example-2", R.drawable.smile_example_2, "Veg Pulao, 25 servings",
                "Hope Shelter", "Full plates and big smiles today. Thank you!", 25, 9
            )
        )
        val dir = File(context.filesDir, "smiles").apply { mkdirs() }
        val now = System.currentTimeMillis()
        val seeded = specs.mapNotNull { spec ->
            try {
                val file = File(dir, "${spec.id}.jpg")
                context.resources.openRawResource(spec.drawable).use { input ->
                    FileOutputStream(file).use { out -> input.copyTo(out) }
                }
                SmileEntry(
                    id = spec.id,
                    donationId = "example-${spec.id}",
                    donationTitle = spec.donationTitle,
                    ngoName = spec.ngoName,
                    message = spec.message,
                    people = spec.people,
                    photoPath = file.absolutePath,
                    sentAt = now - spec.daysAgo * DAY_MS,
                    isExample = true
                )
            } catch (e: Exception) {
                Log.w("SmileStore", "Could not add example photo ${spec.id}", e)
                null
            }
        }
        smiles.addAll(seeded.sortedByDescending { it.sentAt })
        save(context)
    }

    fun add(context: Context, entry: SmileEntry) {
        smiles.add(0, entry)
        save(context)
    }

    /** Swaps in the signed-in person's smiles from the cloud, keeping the example photos. */
    fun replaceCloud(entries: List<SmileEntry>) {
        val merged = (smiles.filter { it.isExample } + entries).sortedByDescending { it.sentAt }
        smiles.clear()
        smiles.addAll(merged)
    }

    fun remove(context: Context, id: String) {
        val entry = smiles.firstOrNull { it.id == id } ?: return
        // A smile from the cloud is only hidden from this donor's wall; the NGO still has it.
        if (Cloud.enabled && !entry.isExample) CloudSync.hideSmile(id)
        runCatching { File(entry.photoPath).delete() }
        smiles.remove(entry)
        save(context)
    }

    fun hasSmileFor(donationId: String): Boolean = smiles.any { it.donationId == donationId }

    private fun save(context: Context) {
        val kept = if (Cloud.enabled) smiles.filter { it.isExample } else smiles.toList()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, smilesToJson(kept))
            .apply()
    }
}

internal fun smilesToJson(list: List<SmileEntry>): String {
    val array = JSONArray()
    list.forEach { s ->
        array.put(
            JSONObject()
                .put("id", s.id)
                .put("donationId", s.donationId)
                .put("donationTitle", s.donationTitle)
                .put("ngoName", s.ngoName)
                .put("message", s.message)
                .put("people", s.people)
                .put("photoPath", s.photoPath)
                .put("sentAt", s.sentAt)
                .put("isExample", s.isExample)
        )
    }
    return array.toString()
}

internal fun smilesFromJson(raw: String): List<SmileEntry> = try {
    val array = JSONArray(raw)
    (0 until array.length()).map { i ->
        val o = array.getJSONObject(i)
        SmileEntry(
            id = o.getString("id"),
            donationId = o.getString("donationId"),
            donationTitle = o.getString("donationTitle"),
            ngoName = o.getString("ngoName"),
            message = o.getString("message"),
            people = o.getInt("people"),
            photoPath = o.getString("photoPath"),
            sentAt = o.getLong("sentAt"),
            isExample = o.optBoolean("isExample", false)
        )
    }
} catch (e: Exception) {
    Log.w("SmileStore", "Could not read saved smiles", e)
    emptyList()
}

/**
 * Copies a picked or captured photo into private storage. The photo is rotated upright, shrunk
 * to at most 1280 px and re-encoded, which also removes EXIF data such as the GPS location.
 * Returns the saved file's path.
 */
suspend fun saveSmilePhoto(context: Context, source: Uri, id: String): String = withContext(Dispatchers.IO) {
    val finalBitmap = loadUprightBitmap(context, source, maxSide = 1280)
    val dir = File(context.filesDir, "smiles").apply { mkdirs() }
    val out = File(dir, "$id.jpg")
    FileOutputStream(out).use { finalBitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
    out.absolutePath
}

/** Reads a photo, turns it upright from its EXIF orientation and shrinks it to at most [maxSide] px. */
internal fun loadUprightBitmap(context: Context, source: Uri, maxSide: Int): Bitmap {
    val resolver = context.contentResolver

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    bounds.inSampleSize = calculateInSampleSize(bounds, maxSide, maxSide)
    bounds.inJustDecodeBounds = false

    val decoded = resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        ?: throw IllegalArgumentException("Could not read the photo")

    val rotation = resolver.openInputStream(source)?.use { stream ->
        when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    } ?: 0f

    val upright = if (rotation != 0f) {
        val matrix = Matrix().apply { postRotate(rotation) }
        Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    } else {
        decoded
    }

    val scale = minOf(1f, maxSide.toFloat() / maxOf(upright.width, upright.height))
    return if (scale < 1f) {
        Bitmap.createScaledBitmap(upright, (upright.width * scale).toInt(), (upright.height * scale).toInt(), true)
    } else {
        upright
    }
}
