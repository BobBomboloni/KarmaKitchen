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

/** Karma points for a donation: capped servings times a quality multiplier. */
fun calculateKarmaPoints(servingsText: String, quality: String): Int {
    val servings = (Regex("\\d+").find(servingsText)?.value?.toIntOrNull() ?: 1)
        .coerceIn(1, MAX_SERVINGS_FOR_POINTS)
    val multiplier = when {
        quality.contains("High", ignoreCase = true) ||
            quality.contains("Excellent", ignoreCase = true) ||
            quality.contains("Fresh", ignoreCase = true) -> 50
        quality.contains("Medium", ignoreCase = true) || quality.contains("Good", ignoreCase = true) -> 30
        else -> 20
    }
    return servings * multiplier
}
