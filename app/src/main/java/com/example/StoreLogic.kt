package com.example

import kotlin.random.Random

/** Store discount a tier gives, as shown on the Impact Tiers screen. */
fun tierDiscountPercent(tier: String): Int = when (tier) {
    "Silver" -> 5
    "Gold" -> 10
    "Platinum" -> 20
    else -> 0
}

/** Price after [percent] off, rounded to the nearest coin (halves round up). */
fun discountedPrice(price: Int, percent: Int): Int =
    (price * (100 - percent.coerceIn(0, 100)) + 50) / 100

const val MAX_PER_REWARD = 5

/** The cart maps a reward id to how many of it are in the cart. */
fun addToCart(cart: Map<String, Int>, id: String): Map<String, Int> {
    val count = cart[id] ?: 0
    return if (count >= MAX_PER_REWARD) cart else cart + (id to (count + 1))
}

fun removeFromCart(cart: Map<String, Int>, id: String): Map<String, Int> {
    val count = cart[id] ?: return cart
    return if (count <= 1) cart - id else cart + (id to (count - 1))
}

fun cartCount(cart: Map<String, Int>): Int = cart.values.sum()

/** Full price of everything in the cart, before any tier discount. */
fun cartSubtotal(cart: Map<String, Int>, rewards: List<RewardItem> = storeRewards): Int =
    rewards.sumOf { (cart[it.id] ?: 0) * it.points }

/** What the cart costs after the tier discount. */
fun cartTotal(cart: Map<String, Int>, discountPercent: Int, rewards: List<RewardItem> = storeRewards): Int =
    rewards.sumOf { (cart[it.id] ?: 0) * discountedPrice(it.points, discountPercent) }

/** How many more coins are needed to afford [price] with [balance] (0 when it is already affordable). */
fun coinsShort(price: Int, balance: Int): Int = (price - balance).coerceAtLeast(0)

/** The nearest partner reward the donor cannot afford yet, and how many coins away it is. */
data class NextGoal(val reward: RewardItem, val coinsToGo: Int)

fun nextGoal(balance: Int, discountPercent: Int, rewards: List<RewardItem> = storeRewards): NextGoal? =
    rewards
        .filter { it.kind != RewardKind.Cause }
        .map { it to discountedPrice(it.points, discountPercent) }
        .filter { (_, price) -> price > balance }
        .minByOrNull { (_, price) -> price }
        ?.let { (reward, price) -> NextGoal(reward, price - balance) }

private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

/** A demo voucher code such as "SWI-8K2F-91XQ" (no look-alike characters). */
fun voucherCode(brand: String, random: Random = Random.Default): String {
    val prefix = brand.filter { it.isLetter() }.take(3).uppercase().padEnd(3, 'X')
    fun group() = String(CharArray(4) { CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)] })
    return "$prefix-${group()}-${group()}"
}

const val DAY_MILLIS = 24L * 60 * 60 * 1000

/** When a voucher bought at [boughtAt] stops working, or null if it never does. */
fun validUntil(boughtAt: Long, validDays: Int): Long? =
    if (validDays <= 0) null else boughtAt + validDays * DAY_MILLIS
