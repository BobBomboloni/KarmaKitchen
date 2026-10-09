package com.example

import androidx.compose.runtime.mutableStateMapOf
import kotlin.math.ceil
import kotlin.math.hypot

/*
 * "Karma Relay", the delivery demo. Who carries a donation, in order:
 *  1. a runner from the NGO that accepts it,
 *  2. a Karma Rider (a verified local student volunteer) when no NGO takes it in time,
 *  3. a backup courier, paid for by a sponsor, when nobody else is free.
 *
 * Nothing here talks to a server. The waiting times are shortened and the trip on the map is sped
 * up so the whole story fits in a one-minute demo. The maths is plain Kotlin so it can be tested.
 */

enum class CarrierKind(val label: String, val about: String) {
    NgoRunner("NGO runner", "Sent by the NGO that accepted the food"),
    KarmaRider("Karma Rider", "A verified student volunteer from the area"),
    Courier("Backup courier", "Booked automatically so the food is not wasted")
}

data class Carrier(val name: String, val kind: CarrierKind, val vehicle: String, val rating: Double, val trips: Int)

enum class RelayPhase { AskingNgos, AskingRiders, Dispatched }

/** Seconds the demo gives nearby NGOs before it asks Karma Riders. */
const val DEMO_NGO_WINDOW_S = 15L

/** Seconds after that until a Karma Rider says yes. */
const val DEMO_RIDER_WAIT_S = 8L

/** The trip on the map runs this many times faster than real time. */
const val DEMO_TRIP_SPEED = 10

/** Cooked food is best eaten within about two hours of cooking. */
const val EAT_WITHIN_MS = 2 * 60 * 60 * 1000L

/** Which rung of the relay a donation that nobody has taken yet is on, [elapsedMs] after posting. */
fun relayPhase(elapsedMs: Long): RelayPhase = when {
    elapsedMs < DEMO_NGO_WINDOW_S * 1000 -> RelayPhase.AskingNgos
    elapsedMs < (DEMO_NGO_WINDOW_S + DEMO_RIDER_WAIT_S) * 1000 -> RelayPhase.AskingRiders
    else -> RelayPhase.Dispatched
}

/** Seconds left on the current rung, for the countdown next to it. */
fun relaySecondsLeft(elapsedMs: Long): Long {
    val seconds = elapsedMs / 1000
    val left = when (relayPhase(elapsedMs)) {
        RelayPhase.AskingNgos -> DEMO_NGO_WINDOW_S - seconds
        RelayPhase.AskingRiders -> DEMO_NGO_WINDOW_S + DEMO_RIDER_WAIT_S - seconds
        RelayPhase.Dispatched -> 0L
    }
    return left.coerceAtLeast(0L)
}

/** How far along the route the carrier is (0..1), [elapsedMs] after setting off on an [etaMinutes] trip. */
fun tripProgress(elapsedMs: Long, etaMinutes: Int): Float {
    val tripMs = etaMinutes.coerceAtLeast(1) * 60_000L / DEMO_TRIP_SPEED
    return (elapsedMs.toFloat() / tripMs).coerceIn(0f, 1f)
}

/** Minutes left on the trip, rounded up so it only reads 0 on arrival. */
fun minutesLeft(progress: Float, etaMinutes: Int): Int =
    ceil(etaMinutes * (1.0 - progress.coerceIn(0f, 1f))).toInt().coerceAtLeast(0)

/** A four-digit code the donor gives the carrier at pickup. The same donation always gets the same code. */
fun handoverCode(id: String): String = (1000 + (id.hashCode() and Int.MAX_VALUE) % 9000).toString()

/** "1h 40m", "25m" or "Past its best", for the eat-by countdown. */
fun eatByLabel(msLeft: Long): String {
    if (msLeft <= 0L) return "Past its best"
    val minutes = (msLeft + 59_999L) / 60_000L
    val hours = minutes / 60L
    return if (hours > 0) "${hours}h ${minutes % 60L}m" else "${minutes}m"
}

private val riderNames = listOf("Dev", "Riya", "Aditya", "Zoya")

/** Who carries donation [id]. [name] is the carrier already on the donation, when there is one. */
fun carrierFor(id: String, kind: CarrierKind, name: String? = null): Carrier {
    val n = id.hashCode() and Int.MAX_VALUE
    return when (kind) {
        CarrierKind.NgoRunner -> Carrier(name ?: volunteerFor(id), kind, "Scooter", 4.8, 60 + n % 90)
        CarrierKind.KarmaRider -> Carrier(name ?: riderNames[n % riderNames.size], kind, "Bicycle", 4.9, 20 + n % 140)
        CarrierKind.Courier -> Carrier(name ?: "Ramesh", kind, "Two-wheeler with an insulated box", 4.7, 400 + n % 600)
    }
}

/** The route on the drawn map, from the donor (first point) to the NGO (last), in 0..1 map units. */
val demoRoute: List<Pair<Float, Float>> = listOf(
    0.16f to 0.82f, 0.16f to 0.56f, 0.50f to 0.56f, 0.50f to 0.30f, 0.84f to 0.30f, 0.84f to 0.16f
)

/** The point a share [t] (0..1) of the way along [route]. */
fun pointAlong(route: List<Pair<Float, Float>>, t: Float): Pair<Float, Float> = routeUpTo(route, t).last()

/** The corners of [route] already passed at share [t], ending with the current point. */
fun routeUpTo(route: List<Pair<Float, Float>>, t: Float): List<Pair<Float, Float>> {
    if (route.size < 2) return route.ifEmpty { listOf(0f to 0f) }
    val lengths = route.zipWithNext { a, b -> hypot(b.first - a.first, b.second - a.second) }
    var remaining = t.coerceIn(0f, 1f) * lengths.sum()
    val passed = mutableListOf(route.first())
    for (i in lengths.indices) {
        val a = route[i]
        val b = route[i + 1]
        if (remaining <= lengths[i] || i == lengths.lastIndex) {
            val f = if (lengths[i] == 0f) 1f else (remaining / lengths[i]).coerceIn(0f, 1f)
            passed.add((a.first + (b.first - a.first) * f) to (a.second + (b.second - a.second) * f))
            return passed
        }
        remaining -= lengths[i]
        passed.add(b)
    }
    return passed
}

/** Remembers, for this session, when each step of a demo delivery started and who carries it. */
object DeliveryDemo {
    private val searchStarted = mutableMapOf<String, Long>()
    private val tripStarted = mutableMapOf<String, Long>()
    private val firstSeen = mutableMapOf<String, Long>()
    private val kinds = mutableStateMapOf<String, CarrierKind>()

    /** When the relay started looking for a carrier for donation [id]. */
    fun searchStart(id: String, now: Long): Long = searchStarted.getOrPut(id) { now }

    /** When the carrier set off with donation [id]. */
    fun tripStart(id: String, now: Long): Long = tripStarted.getOrPut(id) { now }

    /** When the food was cooked, for the eat-by countdown. The demo assumes 20 minutes before tracking began. */
    fun cookedAt(id: String, now: Long): Long = firstSeen.getOrPut(id) { now - 20 * 60_000L }

    fun kindOf(id: String): CarrierKind = kinds[id] ?: CarrierKind.NgoRunner

    /** No NGO took donation [id] in time, so a Karma Rider or the backup courier carries it instead. */
    fun dispatch(id: String, kind: CarrierKind) {
        if (kind == CarrierKind.NgoRunner) return
        val waiting = NgoState.donations.any { it.id == id && it.stage == OfferStage.Offered } ||
            DonationLog.submitted.any { it.id == id && it.stage == 0 }
        if (!waiting) return
        kinds[id] = kind
        NgoState.dispatch(id, carrierFor(id, kind).name, if (kind == CarrierKind.Courier) 9 else 12)
    }

    fun reset() {
        searchStarted.clear()
        tripStarted.clear()
        firstSeen.clear()
        kinds.clear()
    }
}
