package com.example

/** Where a donor stands on the Impact Tiers ladder. */
data class TierStatus(
    val current: String,
    val next: String?,
    val pointsToNext: Int,
    /** 0..1 progress from the current tier towards the next one. */
    val progress: Float
)

private val tierLadder = listOf("Bronze" to 0, "Silver" to 2000, "Gold" to 5000, "Platinum" to 10000)

fun tierStatus(points: Int): TierStatus {
    val index = tierLadder.indexOfLast { points >= it.second }.coerceAtLeast(0)
    val current = tierLadder[index]
    val next = tierLadder.getOrNull(index + 1) ?: return TierStatus(current.first, null, 0, 1f)
    val span = (next.second - current.second).toFloat()
    return TierStatus(
        current = current.first,
        next = next.first,
        pointsToNext = (next.second - points).coerceAtLeast(0),
        progress = ((points - current.second) / span).coerceIn(0f, 1f)
    )
}
