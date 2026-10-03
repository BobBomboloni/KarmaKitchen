package com.example

private val UNSAFE_WORDS = listOf(
    "spoil", "inedible", "unfit", "unsafe", "mold", "mould", "decay", "expired", "contaminat"
)
private val ROT_REGEX = Regex("\\brot(ten|ting|s)?\\b")

/** True when the AI's quality text indicates the food must not be donated. */
fun isUnsafeQuality(quality: String): Boolean {
    val q = quality.lowercase()
    return UNSAFE_WORDS.any { q.contains(it) } || ROT_REGEX.containsMatchIn(q)
}

const val MAX_SERVINGS_FOR_POINTS = 50

/** Coins earned per serving: more for fresh, high-quality food. */
fun karmaPerServing(quality: String): Int = when {
    quality.contains("High", ignoreCase = true) ||
        quality.contains("Excellent", ignoreCase = true) ||
        quality.contains("Fresh", ignoreCase = true) -> 50
    quality.contains("Medium", ignoreCase = true) || quality.contains("Good", ignoreCase = true) -> 30
    else -> 20
}

/** The first number in text like "4-6 servings" (1 when there is none). */
fun servingsCount(servingsText: String): Int =
    Regex("\\d+").find(servingsText)?.value?.toIntOrNull() ?: 1

/** Servings that count towards coins: at least 1, at most [MAX_SERVINGS_FOR_POINTS]. */
fun countedServings(servingsText: String): Int = servingsCount(servingsText).coerceIn(1, MAX_SERVINGS_FOR_POINTS)

/** Karma coins for a donation: capped servings times a quality multiplier. */
fun calculateKarmaPoints(servingsText: String, quality: String): Int =
    countedServings(servingsText) * karmaPerServing(quality)

/** Used by the + and - buttons: "4-6 servings" plus one becomes "5 servings". */
fun adjustServings(servingsText: String, delta: Int): String {
    val match = Regex("\\d+").find(servingsText)
    val next = ((match?.value?.toIntOrNull() ?: 0) + delta).coerceIn(1, 99)
    return if (next == 1) "1 serving" else "$next servings"
}
