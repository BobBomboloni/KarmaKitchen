package com.example.cloud

import com.example.DonationItem
import com.example.FoodCategory
import com.example.NgoDonation
import com.example.OfferStage
import com.example.STATUS_DELIVERED
import com.example.STATUS_ON_THE_WAY
import com.example.STATUS_POSTED
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

const val DONATIONS = "donations"

/** A donation's status as stored in Firestore. Security rules only allow these moves, in order. */
const val STATE_OFFERED = "offered"
const val STATE_ON_THE_WAY = "on_the_way"
const val STATE_RECEIVED = "received"

/**
 * One document in the `donations` collection. Timestamps are plain milliseconds here; the Firestore
 * layer converts them, so this class can be tested without Firebase.
 */
data class DonationDoc(
    val id: String,
    val donorId: String,
    val donorName: String,
    val donorPhone: String = "",
    val title: String,
    val servings: Int,
    val isVeg: Boolean,
    val shelfLife: String = "",
    val quality: String = "",
    val category: FoodCategory = FoodCategory.Meals,
    val coins: Int,
    val pickupWindow: String = "",
    val note: String = "",
    val address: String = "",
    /** True when the donor's photo of the food is saved with it (see [CloudPhotos]). */
    val hasPhoto: Boolean = false,
    val status: String = STATE_OFFERED,
    val ngoId: String? = null,
    val ngoName: String? = null,
    val volunteer: String? = null,
    val etaMinutes: Int? = null,
    val donorAcknowledged: Boolean = false,
    val createdAtMs: Long = 0L,
    val receivedAtMs: Long? = null
) {
    /** Fields written when the donor posts the donation (timestamps are added by the caller). */
    fun toCreateMap(): Map<String, Any?> = mapOf(
        "donorId" to donorId,
        "donorName" to donorName,
        "donorPhone" to donorPhone,
        "title" to title,
        "servings" to servings,
        "isVeg" to isVeg,
        "shelfLife" to shelfLife,
        "quality" to quality,
        "category" to category.name,
        "coins" to coins,
        "pickupWindow" to pickupWindow,
        "note" to note,
        "address" to address,
        "hasPhoto" to hasPhoto,
        "status" to STATE_OFFERED,
        "ngoId" to null,
        "donorAcknowledged" to false
    )

    /** How the donor's home screen shows it. */
    fun toDonationItem(nowMs: Long, zone: TimeZone = TimeZone.getDefault()): DonationItem = DonationItem(
        title = title,
        ngo = ngoName ?: "Nearby NGOs",
        date = friendlyDate(createdAtMs, nowMs, zone),
        status = when (status) {
            STATE_RECEIVED -> STATUS_DELIVERED
            STATE_ON_THE_WAY -> STATUS_ON_THE_WAY
            else -> STATUS_POSTED
        },
        points = coins,
        category = category,
        volunteer = volunteer,
        etaMinutes = etaMinutes,
        stage = when (status) {
            STATE_RECEIVED -> 2
            STATE_ON_THE_WAY -> 1
            else -> 0
        },
        id = id,
        acknowledged = donorAcknowledged
    )

    /** How the NGO's Offers and Home screens show it. */
    fun toNgoDonation(nowMs: Long, zone: TimeZone = TimeZone.getDefault()): NgoDonation = NgoDonation(
        id = id,
        donor = donorName.ifBlank { "A donor" },
        title = title,
        servings = servings,
        isVeg = isVeg,
        shelfLife = shelfLife,
        distanceKm = -1.0,
        pickup = pickupWindow,
        category = category,
        stage = when (status) {
            STATE_RECEIVED -> OfferStage.Received
            STATE_ON_THE_WAY -> OfferStage.OnTheWay
            else -> OfferStage.Offered
        },
        note = note,
        volunteer = volunteer,
        etaMinutes = etaMinutes,
        receivedText = receivedAtMs?.let { friendlyDate(it, nowMs, zone) },
        fromDonor = true,
        address = address,
        donorPhone = donorPhone,
        hasCloudPhoto = hasPhoto
    )

    companion object {
        /** Reads a document's fields, or returns null when required ones are missing. */
        fun fromMap(id: String, data: Map<String, Any?>): DonationDoc? {
            val donorId = data["donorId"] as? String ?: return null
            val title = data["title"] as? String ?: return null
            return DonationDoc(
                id = id,
                donorId = donorId,
                donorName = data["donorName"] as? String ?: "",
                donorPhone = data["donorPhone"] as? String ?: "",
                title = title,
                servings = (data["servings"] as? Number)?.toInt() ?: 1,
                isVeg = data["isVeg"] as? Boolean ?: true,
                shelfLife = data["shelfLife"] as? String ?: "",
                quality = data["quality"] as? String ?: "",
                category = (data["category"] as? String)
                    ?.let { name -> FoodCategory.entries.firstOrNull { it.name == name } }
                    ?: FoodCategory.Meals,
                coins = (data["coins"] as? Number)?.toInt() ?: 0,
                pickupWindow = data["pickupWindow"] as? String ?: "",
                note = data["note"] as? String ?: "",
                address = data["address"] as? String ?: "",
                hasPhoto = data["hasPhoto"] as? Boolean ?: false,
                status = data["status"] as? String ?: STATE_OFFERED,
                ngoId = data["ngoId"] as? String,
                ngoName = data["ngoName"] as? String,
                volunteer = data["volunteer"] as? String,
                etaMinutes = (data["etaMinutes"] as? Number)?.toInt(),
                donorAcknowledged = data["donorAcknowledged"] as? Boolean ?: false,
                createdAtMs = (data["createdAt"] as? Number)?.toLong() ?: 0L,
                receivedAtMs = (data["receivedAt"] as? Number)?.toLong()
            )
        }
    }
}

/** "Just now", "12 min ago", "Today, 1:30 PM", "Yesterday, 7:45 PM" or "29 Sep". */
fun friendlyDate(timeMs: Long, nowMs: Long, zone: TimeZone = TimeZone.getDefault()): String {
    if (timeMs <= 0L) return "Just now"
    val minutes = (nowMs - timeMs) / 60_000
    if (minutes < 1) return "Just now"
    if (minutes < 60) return "$minutes min ago"
    val time = Calendar.getInstance(zone).apply { timeInMillis = timeMs }
    val today = Calendar.getInstance(zone).apply { timeInMillis = nowMs }
    val clock = SimpleDateFormat("h:mm a", Locale.ENGLISH).apply { timeZone = zone }.format(time.time)
    val days = dayNumber(today) - dayNumber(time)
    return when (days) {
        0L -> "Today, $clock"
        1L -> "Yesterday, $clock"
        else -> SimpleDateFormat("d MMM", Locale.ENGLISH).apply { timeZone = zone }.format(time.time)
    }
}

/** Days since 1970 in the calendar's own time zone. */
private fun dayNumber(c: Calendar): Long =
    Math.floorDiv(c.timeInMillis + c.get(Calendar.ZONE_OFFSET) + c.get(Calendar.DST_OFFSET), 86_400_000L)
