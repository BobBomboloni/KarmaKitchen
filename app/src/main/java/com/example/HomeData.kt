package com.example

/*
 * Sample content and small helpers for the donor home screen. Everything here is plain Kotlin
 * (no Compose, no resources) so it can be unit-tested. The names, places and numbers are made-up
 * demo data for the prototype; a real release would load them from a server.
 */

/** Kinds of food a donor can give. Home shows them as a row and uses them to filter NGOs. */
enum class FoodCategory(val label: String) {
    Meals("Cooked meals"),
    Bakery("Bakery"),
    Fruit("Fruit"),
    Vegetables("Vegetables"),
    Packaged("Packaged"),
    Dairy("Dairy")
}

/** A shelter or kitchen asking for food right now, shown as a card on the home screen. */
data class NgoRequest(
    val id: String,
    val name: String,
    val area: String,
    val distanceKm: Double,
    val urgent: Boolean,
    val need: String,
    val pledged: Int,
    val goal: Int,
    val closes: String,
    val accepts: Set<FoodCategory>,
    /** Drives the picture on the card. */
    val icon: FoodCategory
) {
    /** 0..1 share of the goal that donors have already pledged. */
    val progress: Float get() = if (goal <= 0) 0f else (pledged.toFloat() / goal).coerceIn(0f, 1f)
}

val sampleNgoRequests = listOf(
    NgoRequest(
        id = "relief-kitchen", name = "Vadodara Relief Kitchen", area = "Alkapuri", distanceKm = 2.5,
        urgent = true, need = "Dinner for 60 people tonight", pledged = 38, goal = 60,
        closes = "Closes at 8:30 PM", accepts = setOf(FoodCategory.Meals, FoodCategory.Vegetables),
        icon = FoodCategory.Meals
    ),
    NgoRequest(
        id = "hope-shelter", name = "Hope Shelter", area = "Fatehgunj", distanceKm = 4.1,
        urgent = false, need = "Packaged food and cooked meals for the week", pledged = 120, goal = 200,
        closes = "Open until 9 PM", accepts = setOf(FoodCategory.Packaged, FoodCategory.Meals),
        icon = FoodCategory.Packaged
    ),
    NgoRequest(
        id = "city-orphanage", name = "City Orphanage", area = "Manjalpur", distanceKm = 5.8,
        urgent = false, need = "Fruit and milk for 35 children", pledged = 12, goal = 35,
        closes = "Open until 7 PM", accepts = setOf(FoodCategory.Fruit, FoodCategory.Dairy),
        icon = FoodCategory.Fruit
    ),
    NgoRequest(
        id = "annapurna", name = "Annapurna Seva Trust", area = "Gotri", distanceKm = 6.3,
        urgent = false, need = "Bread and bakery items for tomorrow's breakfast", pledged = 20, goal = 50,
        closes = "Closes at 10 PM", accepts = setOf(FoodCategory.Bakery, FoodCategory.Meals),
        icon = FoodCategory.Bakery
    )
)

/** NGOs that accept [category] (all of them when it is null), urgent first, then nearest first. */
fun ngosAccepting(category: FoodCategory?, all: List<NgoRequest> = sampleNgoRequests): List<NgoRequest> =
    all.filter { category == null || category in it.accepts }
        .sortedWith(compareByDescending<NgoRequest> { it.urgent }.thenBy { it.distanceKm })

const val STATUS_ON_THE_WAY = "On the way"
const val STATUS_POSTED = "Waiting for pickup"
const val STATUS_DELIVERED = "Delivered"

data class DonationItem(
    val title: String,
    val ngo: String,
    val date: String,
    val status: String,
    val points: Int,
    val category: FoodCategory,
    val volunteer: String? = null,
    val etaMinutes: Int? = null,
    /** 0 = posted and waiting for a volunteer, 1 = picked up and on the way, 2 = delivered. */
    val stage: Int = 1,
    /** Links a donation made on this phone to the NGO's copy of it. */
    val id: String = java.util.UUID.randomUUID().toString()
) {
    val inTransit: Boolean get() = status != STATUS_DELIVERED
}

/** Best guess of what kind of food a dish name describes, for the picture next to a donation. */
fun guessCategory(title: String): FoodCategory {
    val t = title.lowercase()
    fun has(vararg words: String) = words.any { t.contains(it) }
    return when {
        has("bread", "bun", "cake", "pastry", "croissant", "muffin", "cookie", "bakery", "pav") -> FoodCategory.Bakery
        has("fruit", "apple", "banana", "mango", "orange", "grape", "papaya", "melon") -> FoodCategory.Fruit
        has("milk", "curd", "yogurt", "yoghurt", "paneer", "cheese", "butter", "ghee") -> FoodCategory.Dairy
        has("packet", "packaged", "sealed", "canned", "chips", "noodles", "flour", "grain") -> FoodCategory.Packaged
        has("biryani", "rice", "dal", "curry", "khichdi", "pulao", "sandwich", "meal", "thali", "pizza", "pasta", "roti") -> FoodCategory.Meals
        has("vegetable", "veggie", "salad", "carrot", "tomato", "potato", "spinach", "cabbage") -> FoodCategory.Vegetables
        else -> FoodCategory.Meals
    }
}

/** The delivered ones match what the NGO side of the app has received, so the demo stays consistent. */
val recentDonations = listOf(
    DonationItem(
        "Veg Biryani, 12 servings", "Vadodara Relief Kitchen", "Today, 1:30 PM", STATUS_ON_THE_WAY, 300,
        FoodCategory.Meals, volunteer = "Imran", etaMinutes = 8
    ),
    DonationItem("Leftover Catering Sandwiches", NGO_NAME, "Yesterday, 7:45 PM", STATUS_DELIVERED, 150, FoodCategory.Meals),
    DonationItem("Fresh Vegetable Basket", NGO_NAME, "29 Sep", STATUS_DELIVERED, 200, FoodCategory.Vegetables),
    DonationItem("Bakery Surplus, 20 breads", NGO_NAME, "26 Sep", STATUS_DELIVERED, 100, FoodCategory.Bakery)
)

data class LiveFeedItem(val message: String, val timeAgo: String, val category: FoodCategory)

val dummyFeed = listOf(
    LiveFeedItem("Riya donated 12 servings of veg biryani.", "4 min ago", FoodCategory.Meals),
    LiveFeedItem("Arjun saved 4 meals from his canteen.", "18 min ago", FoodCategory.Vegetables),
    LiveFeedItem("Annapurna Seva Trust received 30 chapatis from Meera.", "40 min ago", FoodCategory.Bakery),
    LiveFeedItem("Kabir earned 150 Karma Points for an urgent delivery.", "1 hour ago", FoodCategory.Dairy),
    LiveFeedItem("Hope Shelter reached its goal of 200 meals today.", "2 hours ago", FoodCategory.Meals)
)

data class TopDonor(val name: String, val meals: Int)

val topDonorsThisWeek = listOf(
    TopDonor("Hotel Saffron", 320),
    TopDonor("Meera Kapadia", 214),
    TopDonor("Aarav's Canteen", 187)
)

/** Demo numbers for the community card and the donor's own impact. */
const val COMMUNITY_MEALS_THIS_MONTH = 12_480
const val COMMUNITY_MEALS_GOAL = 15_000
const val DEMO_MEALS_SHARED = 42
const val DEMO_WEEKLY_RANK = 14

fun communityProgress(shared: Int = COMMUNITY_MEALS_THIS_MONTH, goal: Int = COMMUNITY_MEALS_GOAL): Float =
    if (goal <= 0) 0f else (shared.toFloat() / goal).coerceIn(0f, 1f)

const val DEFAULT_PICKUP_AREA = "Alkapuri, Vadodara"

/** Text for the pickup location at the top of the home screen. */
fun pickupLabel(address: String): String = address.trim().ifBlank { DEFAULT_PICKUP_AREA }

/** "Just now", "12 min ago", "3 hours ago", "Yesterday", "5 days ago", "2 weeks ago". */
fun timeAgoLabel(then: Long, now: Long = System.currentTimeMillis()): String {
    val minutes = ((now - then) / 60_000L).coerceAtLeast(0L)
    val hours = minutes / 60L
    val days = hours / 24L
    return when {
        minutes < 1L -> "Just now"
        minutes < 60L -> "$minutes min ago"
        hours < 24L -> if (hours == 1L) "1 hour ago" else "$hours hours ago"
        days < 7L -> if (days == 1L) "Yesterday" else "$days days ago"
        else -> {
            val weeks = days / 7L
            if (weeks == 1L) "1 week ago" else "$weeks weeks ago"
        }
    }
}
