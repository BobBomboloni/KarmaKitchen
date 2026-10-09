package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID

/** Where the NGO this phone represents is based. */
const val NGO_AREA = "Fatehgunj, Vadodara"

enum class OfferStage { Offered, OnTheWay, Received }

/** A donation as the NGO sees it: offered, on its way, or received. */
data class NgoDonation(
    val id: String,
    val donor: String,
    val title: String,
    val servings: Int,
    val isVeg: Boolean,
    val shelfLife: String,
    val distanceKm: Double,
    val pickup: String,
    val category: FoodCategory,
    val stage: OfferStage,
    val note: String = "",
    val volunteer: String? = null,
    val etaMinutes: Int? = null,
    val receivedText: String? = null,
    /** Photo the donor took, as a Uri string. */
    val photo: String? = null,
    /** True when it was posted from the Donate screen on this phone. */
    val fromDonor: Boolean = false
)

data class StockItem(
    val id: String,
    val name: String,
    val quantity: String,
    val hoursLeft: Int,
    val category: FoodCategory
)

enum class StockStatus(val label: String) {
    Fresh("Fresh"),
    Soon("Expiring soon"),
    Expired("Expired")
}

fun stockStatus(hoursLeft: Int): StockStatus = when {
    hoursLeft <= 0 -> StockStatus.Expired
    hoursLeft <= 24 -> StockStatus.Soon
    else -> StockStatus.Fresh
}

/** "Expires in 4 hours", "Expires in 3 days", "Expires in 3 months" or "Expired 2 hours ago". */
fun expiryLabel(hoursLeft: Int): String {
    fun unit(n: Int, name: String) = if (n == 1) "1 $name" else "$n ${name}s"
    val hours = kotlin.math.abs(hoursLeft)
    val amount = when {
        hours < 24 -> unit(maxOf(hours, 1), "hour")
        hours < 24 * 45 -> unit(hours / 24, "day")
        else -> unit(hours / (24 * 30), "month")
    }
    return if (hoursLeft > 0) "Expires in $amount" else "Expired $amount ago"
}

/** Reads text such as "Expires in 12 hours" or "Consume within 2 days" as a number of hours. */
fun hoursFromExpiryText(text: String, fallback: Int = 6): Int {
    val match = Regex("(\\d+)\\s*(hour|hr|day|week|month)", RegexOption.IGNORE_CASE).find(text) ?: return fallback
    val n = match.groupValues[1].toIntOrNull() ?: return fallback
    val perUnit = when (match.groupValues[2].lowercase()) {
        "hour", "hr" -> 1
        "day" -> 24
        "week" -> 24 * 7
        else -> 24 * 30
    }
    return n * perUnit
}

/** Letters for the last seven days ending today, from Calendar.DAY_OF_WEEK (1 = Sunday). */
fun lastSevenDayLetters(dayOfWeek: Int): List<String> {
    val letters = "SMTWTFS"
    val today = (dayOfWeek - 1).coerceIn(0, 6)
    return (0..6).map { letters[(today - 6 + it + 7) % 7].toString() }
}

val volunteerNames = listOf("Imran", "Hetal", "Karan", "Sana")

/** The same donation always gets the same volunteer, so the demo is repeatable. */
fun volunteerFor(id: String): String = volunteerNames[(id.hashCode() and Int.MAX_VALUE) % volunteerNames.size]

/** Rough pickup time: a few minutes to get going plus about 6 minutes per km. */
fun etaMinutes(distanceKm: Double): Int = (8 + distanceKm * 6).toInt()

/** What the receiver learned when it checked a delivery. [manual] means no AI check was used. */
data class IntakeSummary(
    val verified: Boolean,
    val freshness: String,
    val expiry: String,
    val storage: String,
    val tags: List<String>,
    val manual: Boolean = false
)

private fun seedDonations(): List<NgoDonation> = listOf(
    NgoDonation(
        "offer-1", "Hotel Saffron", "Paneer butter masala and naan", 30, true, "5 hours", 1.8, "As soon as possible",
        FoodCategory.Meals, OfferStage.Offered, note = "Back gate, ask for Ramesh"
    ),
    NgoDonation(
        "offer-2", "Meera Kapadia", "Fresh fruit tray", 15, true, "2 days", 3.2, "In about an hour",
        FoodCategory.Fruit, OfferStage.Offered
    ),
    NgoDonation(
        "incoming-1", "Aarav's Canteen", "20 Servings - Mixed Veg", 20, true, "6 hours", 1.2, "As soon as possible",
        FoodCategory.Meals, OfferStage.OnTheWay, volunteer = "Imran", etaMinutes = 12
    ),
    NgoDonation(
        "incoming-2", "Sunrise Bakery", "50 Assorted Breads", 50, true, "1 day", 3.4, "In about an hour",
        FoodCategory.Bakery, OfferStage.OnTheWay, volunteer = "Hetal", etaMinutes = 25
    )
) + sampleReceivals.map { r ->
    NgoDonation(
        r.id, "Local donor", r.title, r.servings, true, "", 2.0, "", guessCategory(r.title), OfferStage.Received,
        receivedText = r.receivedText
    )
}

private fun seedStock(): List<StockItem> = listOf(
    StockItem("stock-1", "Whole Wheat Flour (Atta) - 10kg", "10 kg", 24 * 90, FoodCategory.Packaged),
    StockItem("stock-2", "Fresh Tomatoes & Onions - 5kg", "5 kg", 72, FoodCategory.Vegetables),
    StockItem("stock-3", "Cooked Basmati Rice & Dal", "40 servings", 4, FoodCategory.Meals),
    StockItem("stock-4", "Catering Paneer Sabzi", "15 servings", -2, FoodCategory.Meals)
)

/**
 * Everything the receiver side shows. It lives in memory for this session. Donations posted from
 * the Donate screen arrive here as offers, and accepting or receiving them updates the donor's copy
 * in [DonationLog], so one phone can play both roles.
 */
object NgoState {
    val donations = mutableStateListOf<NgoDonation>()
    val stock = mutableStateListOf<StockItem>()
    val declined = mutableStateListOf<String>()

    var accepting by mutableStateOf(true)
    var broadcasting by mutableStateOf(false)
    var needs by mutableStateOf<Set<FoodCategory>>(setOf(FoodCategory.Meals))
    var mealsToday by mutableStateOf(342)
    var peopleServed by mutableStateOf(1280)

    init {
        reset()
    }

    /** Back to the built-in sample data. */
    fun reset() {
        donations.clear()
        donations.addAll(seedDonations())
        stock.clear()
        stock.addAll(seedStock())
        declined.clear()
        accepting = true
        broadcasting = false
        needs = setOf(FoodCategory.Meals)
        mealsToday = 342
        peopleServed = 1280
    }

    val offers: List<NgoDonation> get() = donations.filter { it.stage == OfferStage.Offered && it.id !in declined }
    val onTheWay: List<NgoDonation> get() = donations.filter { it.stage == OfferStage.OnTheWay }
    val received: List<NgoDonation> get() = donations.filter { it.stage == OfferStage.Received }

    /** Stock that is expired or about to expire. */
    val needAttention: Int get() = stock.count { stockStatus(it.hoursLeft) != StockStatus.Fresh }

    val awaitingSmile: List<NgoDonation> get() = received.filter { !SmileStore.hasSmileFor(it.id) }

    /** How full the store room is, from the amount of stock. */
    val capacityPercent: Int get() = (40 + 11 * stock.size).coerceAtMost(100)

    fun receival(id: String): Receival? = donations.firstOrNull { it.id == id && it.stage == OfferStage.Received }?.let {
        Receival(it.id, it.title, it.receivedText ?: "just now", it.servings)
    }

    fun post(donation: NgoDonation) {
        donations.add(0, donation)
    }

    fun accept(id: String) {
        val donation = donations.firstOrNull { it.id == id && it.stage == OfferStage.Offered } ?: return
        val volunteer = volunteerFor(id)
        val eta = etaMinutes(donation.distanceKm)
        replace(id) { it.copy(stage = OfferStage.OnTheWay, volunteer = volunteer, etaMinutes = eta) }
        DonationLog.update(id) {
            it.copy(ngo = NGO_NAME, status = STATUS_ON_THE_WAY, stage = 1, volunteer = volunteer, etaMinutes = eta)
        }
    }

    fun decline(id: String) {
        if (id !in declined) declined.add(id)
    }

    /** The food has arrived: record it, add it to stock and let the donor know. */
    fun markReceived(id: String, intake: IntakeSummary?) {
        val donation = donations.firstOrNull { it.id == id && it.stage == OfferStage.OnTheWay } ?: return
        replace(id) { it.copy(stage = OfferStage.Received, etaMinutes = null, receivedText = "just now") }
        val hours = hoursFromExpiryText(intake?.expiry?.takeIf { it.isNotBlank() } ?: donation.shelfLife, fallback = 6)
        stock.add(0, StockItem(UUID.randomUUID().toString(), donation.title, "${donation.servings} servings", hours, donation.category))
        mealsToday += donation.servings
        DonationLog.update(id) { it.copy(status = STATUS_DELIVERED, stage = 2, etaMinutes = null) }
        if (donation.fromDonor) DonationLog.credit(id)
    }

    /** Stock handed out to people (or thrown away when it has expired). */
    fun removeStock(id: String, distributed: Boolean) {
        val item = stock.firstOrNull { it.id == id } ?: return
        stock.remove(item)
        if (distributed) peopleServed += servingsCount(item.quantity)
    }

    /** The NGO's current urgent request as the donor home would list it, or null when not broadcasting. */
    fun broadcastRequest(): NgoRequest? {
        if (!broadcasting || needs.isEmpty()) return null
        val goal = 200
        val pledged = donations.filter { it.stage != OfferStage.Offered }.sumOf { it.servings }.coerceAtMost(goal)
        val wanted = needs.joinToString(" and ") { it.label.lowercase() }
        return NgoRequest(
            id = "navrachana-urgent", name = NGO_NAME, area = "Fatehgunj", distanceKm = 0.8, urgent = true,
            need = "Urgent: $wanted needed today", pledged = pledged, goal = goal, closes = "Closes at 9 PM",
            accepts = needs, icon = needs.first()
        )
    }

    private fun replace(id: String, change: (NgoDonation) -> NgoDonation) {
        val index = donations.indexOfFirst { it.id == id }
        if (index >= 0) donations[index] = change(donations[index])
    }
}
