package com.example.cloud

import com.example.DELIVERY_DROP_OFF
import com.example.DELIVERY_PICKUP
import com.example.DROP_OFF_BONUS
import com.example.DonationItem
import com.example.FoodCategory
import com.example.NgoDonation
import com.example.OfferStage
import com.example.Place
import com.example.STATUS_ACCEPTED
import com.example.STATUS_CANCELLED
import com.example.STATUS_DELIVERED
import com.example.STATUS_EXPIRED
import com.example.STATUS_ON_THE_WAY
import com.example.STATUS_POSTED
import com.example.distanceKm
import com.example.geohash
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

const val DONATIONS = "donations"

/**
 * A donation's status as stored in Firestore. The security rules only allow these moves: offered to
 * accepted to on the way (picked up) to received, back from accepted to offered when the pickup falls
 * through, and to cancelled by the donor before pickup. "Expired" is not stored: it is an offer whose
 * eat-by time has passed.
 */
const val STATE_OFFERED = "offered"
const val STATE_ACCEPTED = "accepted"
const val STATE_ON_THE_WAY = "on_the_way"
const val STATE_RECEIVED = "received"
const val STATE_CANCELLED = "cancelled"

/**
 * One document in the `donations` collection. Timestamps are plain milliseconds here; the Firestore
 * layer converts them, so this class can be tested without Firebase.
 */
data class DonationDoc(
    val id: String,
    val donorId: String,
    val donorName: String,
    /** Only on donations from before slice 3. Newer ones keep the number in a private document. */
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
    val receivedAtMs: Long? = null,
    /** Where to collect it, when the donor's phone knew. */
    val place: Place? = null,
    /** When the food has to be eaten by. */
    val eatByMs: Long? = null,
    /** [DELIVERY_PICKUP] (the NGO collects it) or [DELIVERY_DROP_OFF] (the donor brings it to [dropOffNgoId]). */
    val deliveryType: String = DELIVERY_PICKUP,
    val dropOffNgoId: String? = null,
    val dropOffNgoName: String? = null,
    val acceptedAtMs: Long? = null
) {
    val isDropOff: Boolean get() = deliveryType == DELIVERY_DROP_OFF

    /** What the donor earns once it is received: a bonus for bringing it themselves. */
    val totalCoins: Int get() = coins + if (isDropOff) DROP_OFF_BONUS else 0

    fun isExpired(nowMs: Long): Boolean = status == STATE_OFFERED && eatByMs != null && nowMs >= eatByMs

    /**
     * Fields written when the donor posts the donation (timestamps are added by the caller). The
     * phone number is left out: it goes in a private document only the accepting NGO can read.
     */
    fun toCreateMap(): Map<String, Any?> = mapOf(
        "donorId" to donorId,
        "donorName" to donorName,
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
        "donorAcknowledged" to false,
        "lat" to place?.lat,
        "lng" to place?.lng,
        "geohash" to (place?.let { geohash(it) } ?: ""),
        "deliveryType" to deliveryType,
        "dropOffNgoId" to dropOffNgoId,
        "dropOffNgoName" to dropOffNgoName
    ) + (eatByMs?.let { mapOf("eatBy" to Date(it)) } ?: emptyMap())

    /** How the donor's home screen shows it. */
    fun toDonationItem(nowMs: Long, zone: TimeZone = TimeZone.getDefault()): DonationItem = DonationItem(
        title = title,
        ngo = ngoName ?: dropOffNgoName ?: "Nearby NGOs",
        date = friendlyDate(createdAtMs, nowMs, zone),
        status = when (status) {
            STATE_RECEIVED -> STATUS_DELIVERED
            STATE_ON_THE_WAY -> STATUS_ON_THE_WAY
            STATE_ACCEPTED -> STATUS_ACCEPTED
            STATE_CANCELLED -> STATUS_CANCELLED
            else -> if (isExpired(nowMs)) STATUS_EXPIRED else STATUS_POSTED
        },
        points = totalCoins,
        category = category,
        volunteer = volunteer,
        etaMinutes = etaMinutes,
        stage = when (status) {
            STATE_RECEIVED -> 3
            STATE_ON_THE_WAY -> 2
            STATE_ACCEPTED -> 1
            else -> 0
        },
        id = id,
        acknowledged = donorAcknowledged,
        servings = servings,
        address = address,
        dropOff = isDropOff,
        dropOffNgoId = dropOffNgoId,
        dropOffNgoName = dropOffNgoName,
        ngoId = ngoId,
        createdAtMs = createdAtMs,
        acceptedAtMs = acceptedAtMs,
        eatByMs = eatByMs,
        place = place
    )

    /** How the NGO's Offers and Home screens show it. [here] is the NGO's own spot, for the distance. */
    fun toNgoDonation(nowMs: Long, zone: TimeZone = TimeZone.getDefault(), here: Place? = null): NgoDonation = NgoDonation(
        id = id,
        donor = donorName.ifBlank { "A donor" },
        title = title,
        servings = servings,
        isVeg = isVeg,
        shelfLife = shelfLife,
        distanceKm = if (place != null && here != null) distanceKm(here, place) else -1.0,
        pickup = pickupWindow,
        category = category,
        stage = when (status) {
            STATE_RECEIVED -> OfferStage.Received
            STATE_ON_THE_WAY -> OfferStage.OnTheWay
            STATE_ACCEPTED -> OfferStage.Accepted
            else -> OfferStage.Offered
        },
        note = note,
        volunteer = volunteer,
        etaMinutes = etaMinutes,
        receivedText = receivedAtMs?.let { friendlyDate(it, nowMs, zone) },
        fromDonor = true,
        address = address,
        donorPhone = donorPhone,
        hasCloudPhoto = hasPhoto,
        place = place,
        eatByMs = eatByMs,
        dropOff = isDropOff,
        dropOffNgoId = dropOffNgoId,
        acceptedAtMs = acceptedAtMs
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
                createdAtMs = millis(data["createdAt"]) ?: 0L,
                receivedAtMs = millis(data["receivedAt"]),
                place = placeOf(data["lat"], data["lng"]),
                eatByMs = millis(data["eatBy"]),
                deliveryType = if (data["deliveryType"] == DELIVERY_DROP_OFF) DELIVERY_DROP_OFF else DELIVERY_PICKUP,
                dropOffNgoId = data["dropOffNgoId"] as? String,
                dropOffNgoName = data["dropOffNgoName"] as? String,
                acceptedAtMs = millis(data["acceptedAt"])
            )
        }

        /** Milliseconds from a number (the Firestore layer converts timestamps) or a [Date]. */
        private fun millis(value: Any?): Long? = when (value) {
            is Number -> value.toLong()
            is Date -> value.time
            else -> null
        }

        private fun placeOf(lat: Any?, lng: Any?): Place? {
            val a = (lat as? Number)?.toDouble() ?: return null
            val b = (lng as? Number)?.toDouble() ?: return null
            return Place(a, b)
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
