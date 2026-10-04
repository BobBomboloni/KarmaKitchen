package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.api.FoodWasteFacts

private const val PREFS = "karmakitchen_prefs"
private const val FACTS_MAX_AGE_MS = 24L * 60 * 60 * 1000

/** Whether the dark palette is on. Light is the default; the choice is saved and switched in Profile. */
object ThemeSettings {
    var dark by mutableStateOf(false)
        private set

    fun load(context: Context) {
        dark = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("dark_theme", false)
    }

    fun set(context: Context, value: Boolean) {
        dark = value
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("dark_theme", value).apply()
    }
}

fun loadProfile(context: Context): UserProfile {
    val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    return UserProfile(
        name = p.getString("name", "") ?: "",
        email = p.getString("email", "") ?: "",
        phone = p.getString("phone", "") ?: "",
        address = p.getString("address", "") ?: "",
        karmaPoints = p.getInt("karma", UserProfile().karmaPoints)
    )
}

fun saveProfile(context: Context, profile: UserProfile) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putString("name", profile.name)
        .putString("email", profile.email)
        .putString("phone", profile.phone)
        .putString("address", profile.address)
        .putInt("karma", profile.karmaPoints)
        .apply()
}

/** Returns facts fetched within the last 24 hours, or null if missing or stale. */
fun loadCachedFacts(context: Context): FoodWasteFacts? {
    val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val at = p.getLong("facts_at", 0L)
    if (System.currentTimeMillis() - at > FACTS_MAX_AGE_MS) return null
    return FoodWasteFacts(
        worldWaste = p.getString("facts_world", null) ?: return null,
        indiaWaste = p.getString("facts_india", null) ?: return null,
        gujaratWaste = p.getString("facts_gujarat", null) ?: return null,
        indiaWasteKgPerSec = p.getFloat("facts_india_ps", 0f).toDouble(),
        gujaratWasteKgPerSec = p.getFloat("facts_gujarat_ps", 0f).toDouble(),
        positiveMessage = p.getString("facts_msg", null) ?: return null
    )
}

fun saveCachedFacts(context: Context, f: FoodWasteFacts) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putLong("facts_at", System.currentTimeMillis())
        .putString("facts_world", f.worldWaste)
        .putString("facts_india", f.indiaWaste)
        .putString("facts_gujarat", f.gujaratWaste)
        .putFloat("facts_india_ps", f.indiaWasteKgPerSec.toFloat())
        .putFloat("facts_gujarat_ps", f.gujaratWasteKgPerSec.toFloat())
        .putString("facts_msg", f.positiveMessage)
        .apply()
}
