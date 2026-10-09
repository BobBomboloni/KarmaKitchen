package com.example.cloud

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.util.LruCache
import com.example.loadUprightBitmap
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** Subcollection of a donation that holds the donor's photo of the food, in a document named [FOOD_PHOTO]. */
const val PHOTOS = "photos"
const val FOOD_PHOTO = "food"

private const val CLOUD_PHOTO_SIDE = 960
private const val CLOUD_PHOTO_BYTES = 200 * 1024

/**
 * Photos travel inside Firestore documents, because Cloud Storage needs a billing card. Each one is
 * shrunk first so it stays well under the 1 MB document limit (the rules allow up to 512 KB).
 */
suspend fun cloudJpeg(context: Context, source: Uri): ByteArray = withContext(Dispatchers.IO) {
    compressForCloud(loadUprightBitmap(context, source, CLOUD_PHOTO_SIDE))
}

/** JPEG bytes at the best quality that fits in about 200 KB. */
internal fun compressForCloud(bitmap: Bitmap): ByteArray {
    var bytes = ByteArray(0)
    for (quality in listOf(80, 70, 60, 50, 40)) {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        bytes = out.toByteArray()
        if (bytes.size <= CLOUD_PHOTO_BYTES) break
    }
    return bytes
}

/** Fetches donors' food photos for the NGO screens, keeping recent ones in memory. */
object CloudPhotos {
    private val cache = object : LruCache<String, ByteArray>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ByteArray) = value.size
    }

    fun cached(donationId: String): ByteArray? = cache.get(donationId)

    fun remember(donationId: String, jpeg: ByteArray) {
        cache.put(donationId, jpeg)
    }

    /** The photo saved with donation [donationId], or null when it has none or cannot be read. */
    suspend fun foodPhoto(donationId: String): ByteArray? {
        cache.get(donationId)?.let { return it }
        return try {
            val snap = FirebaseFirestore.getInstance().collection(DONATIONS).document(donationId)
                .collection(PHOTOS).document(FOOD_PHOTO).get().await()
            snap.getBlob("jpeg")?.toBytes()?.also { cache.put(donationId, it) }
        } catch (e: Exception) {
            Log.w("CloudPhotos", "Could not load the photo for $donationId", e)
            null
        }
    }
}
