package com.example

import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** How a donation gets to the NGO: the NGO sends someone, or the donor brings it. */
const val DELIVERY_PICKUP = "ngo_pickup"
const val DELIVERY_DROP_OFF = "donor_dropoff"

/** Extra coins for bringing the food to the NGO yourself. The security rules credit the same amount. */
const val DROP_OFF_BONUS = 25

/** Small donations get the "drop it off yourself" suggestion. */
const val DROP_OFF_SUGGESTED_SERVINGS = 5

/** After this long with no NGO saying yes, the donor is shown other ways to get the food there. */
const val NO_TAKERS_MS = 30 * 60_000L

/** How late the NGO's runner can be before the donor may put the food back up for other NGOs. */
const val LATE_GRACE_MS = 30 * 60_000L

/** Used when nobody knows how far the runner has to go. */
const val DEFAULT_ETA_MINUTES = 30

/** A spot on the map. */
data class Place(val lat: Double, val lng: Double)

/** Straight-line distance in km. Roads add a little, so the app says "about". */
fun distanceKm(a: Place, b: Place): Double {
    val dLat = Math.toRadians(b.lat - a.lat)
    val dLng = Math.toRadians(b.lng - a.lng)
    val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLng / 2).pow(2)
    return 2 * 6371.0 * asin(sqrt(h.coerceIn(0.0, 1.0)))
}

/** "850 m", "2.4 km" or "14 km". */
fun distanceText(km: Double): String = when {
    km < 1 -> "${((km * 1000) / 50).roundToInt().coerceAtLeast(1) * 50} m"
    km < 10 -> String.format(Locale.ENGLISH, "%.1f km", km)
    else -> "${km.roundToInt()} km"
}

private const val GEOHASH_BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"

/**
 * The standard geohash of [place]. Nine characters pin it to about 5 m. It is saved with donations
 * and NGOs so a "nearby" search can later be a simple range query.
 */
fun geohash(place: Place, precision: Int = 9): String {
    var latLo = -90.0
    var latHi = 90.0
    var lngLo = -180.0
    var lngHi = 180.0
    val hash = StringBuilder()
    var bits = 0
    var index = 0
    var useLng = true
    while (hash.length < precision) {
        if (useLng) {
            val mid = (lngLo + lngHi) / 2
            index = if (place.lng >= mid) (index shl 1) or 1 else index shl 1
            if (place.lng >= mid) lngLo = mid else lngHi = mid
        } else {
            val mid = (latLo + latHi) / 2
            index = if (place.lat >= mid) (index shl 1) or 1 else index shl 1
            if (place.lat >= mid) latLo = mid else latHi = mid
        }
        useLng = !useLng
        if (++bits == 5) {
            hash.append(GEOHASH_BASE32[index])
            bits = 0
            index = 0
        }
    }
    return hash.toString()
}

/** Google Maps directions to [place], or to [address] when the spot is not known. Opens the Maps app when installed. */
fun directionsUrl(place: Place?, address: String): String? = when {
    place != null -> "https://www.google.com/maps/dir/?api=1&destination=${place.lat},${place.lng}"
    address.isNotBlank() -> "https://www.google.com/maps/dir/?api=1&destination=${encode(address)}"
    else -> null
}

/** The number the way WhatsApp wants it: digits with the country code. 10-digit numbers are taken as Indian. */
fun whatsAppNumber(phone: String): String? {
    val digits = phone.filter { it.isDigit() }
    return when {
        digits.length == 10 -> "91$digits"
        digits.length == 11 && digits.startsWith("0") -> "91${digits.drop(1)}"
        digits.length in 11..15 -> digits
        else -> null
    }
}

/** Opens a WhatsApp chat with [phone], with [text] typed in. Null when the number cannot be used. */
fun whatsAppUrl(phone: String, text: String = ""): String? {
    val number = whatsAppNumber(phone) ?: return null
    return if (text.isBlank()) "https://wa.me/$number" else "https://wa.me/$number?text=${encode(text)}"
}

private fun encode(text: String): String = URLEncoder.encode(text, "UTF-8").replace("+", "%20")

/**
 * When food posted at [postedAtMs] should be eaten by, from the shelf life the donor or the AI scan
 * gave ("4 hours", "2 days"). At least 1 hour, and at most a week so old offers do not linger.
 */
fun eatByFor(shelfLife: String, postedAtMs: Long): Long {
    val hours = hoursFromExpiryText(shelfLife, fallback = 4).coerceIn(1, 24 * 7)
    return postedAtMs + hours * 3_600_000L
}

/** "9:30 PM" today, or "11 Oct, 9:30 PM" on another day. */
fun clockText(timeMs: Long, nowMs: Long, zone: TimeZone = TimeZone.getDefault()): String {
    val time = Calendar.getInstance(zone).apply { timeInMillis = timeMs }
    val now = Calendar.getInstance(zone).apply { timeInMillis = nowMs }
    val sameDay = time.get(Calendar.YEAR) == now.get(Calendar.YEAR) && time.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
    val pattern = if (sameDay) "h:mm a" else "d MMM, h:mm a"
    return SimpleDateFormat(pattern, Locale.ENGLISH).apply { timeZone = zone }.format(time.time)
}

/** "45 min", "2 h 10 min" or "3 days" until [untilMs]; "0 min" once it has passed. */
fun timeLeftText(untilMs: Long, nowMs: Long): String {
    val minutes = ((untilMs - nowMs) / 60_000).coerceAtLeast(0)
    return when {
        minutes < 60 -> "$minutes min"
        minutes < 48 * 60 -> if (minutes % 60 == 0L) "${minutes / 60} h" else "${minutes / 60} h ${minutes % 60} min"
        else -> "${minutes / (24 * 60)} days"
    }
}

/** Minutes until the runner should arrive, counted from when the NGO accepted. Null when there is no estimate. */
fun minutesUntilArrival(acceptedAtMs: Long?, etaMinutes: Int?, nowMs: Long): Int? {
    if (etaMinutes == null) return null
    if (acceptedAtMs == null || acceptedAtMs <= 0) return etaMinutes
    val passed = ((nowMs - acceptedAtMs) / 60_000).toInt()
    return (etaMinutes - passed).coerceAtLeast(0)
}

/** True once the runner is more than [LATE_GRACE_MS] past their estimate, so the donor may offer the food to others. */
fun runnerIsLate(acceptedAtMs: Long?, etaMinutes: Int?, nowMs: Long): Boolean {
    if (acceptedAtMs == null || acceptedAtMs <= 0) return false
    val expected = acceptedAtMs + (etaMinutes ?: DEFAULT_ETA_MINUTES) * 60_000L
    return nowMs > expected + LATE_GRACE_MS
}

/** The pickup estimate the NGO starts from: by distance when known, else [DEFAULT_ETA_MINUTES]. */
fun suggestedEta(distanceKm: Double): Int = if (distanceKm >= 0) etaMinutes(distanceKm) else DEFAULT_ETA_MINUTES

/** The text the donor shares to a WhatsApp group of local NGOs when nobody has picked the food up yet. */
fun shareOfferText(title: String, servings: Int, address: String, eatByMs: Long?, nowMs: Long): String {
    val amount = if (servings > 1) "$servings servings of " else ""
    val until = eatByMs?.let { ", good until ${clockText(it, nowMs)}" } ?: ""
    val pickup = if (address.isNotBlank()) " Pickup: ${address.trim()}." else ""
    return "Free food for an NGO: $amount${title.trim()}$until.$pickup Accept it in the KarmaKitchen app."
}
