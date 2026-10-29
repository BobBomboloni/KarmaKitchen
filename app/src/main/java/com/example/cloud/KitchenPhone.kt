package com.example.cloud

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat

private const val PREFS = "karmakitchen_prefs"
private const val KEY_KITCHEN_PHONE = "kitchen_phone"

/**
 * The "kitchen phone": an NGO can let one phone ring for every new food offer, even while the app
 * is in the background. It runs [OfferAlertService], which listens to Firestore from the phone
 * itself. That keeps it free: a push sent while the app is closed would need Cloud Functions, and
 * Cloud Functions needs a billing card on the Firebase project.
 *
 * The cost is a permanent notification while it is on, and some battery. After the phone restarts
 * it stays off until someone opens the app again.
 */
object KitchenPhone {
    /** The saved choice, kept as Compose state for the switch on the NGO dashboard. */
    var on by mutableStateOf(false)
        private set

    fun load(context: Context) {
        on = prefs(context).getBoolean(KEY_KITCHEN_PHONE, false)
    }

    /** Saves the choice and starts listening. Call it while the app is on screen. */
    fun turnOn(context: Context) {
        save(context, true)
        start(context)
    }

    fun turnOff(context: Context) {
        save(context, false)
        stop(context)
    }

    /** Starts listening again if it was switched on. Call it while the app is on screen and an NGO is signed in. */
    fun resumeIfOn(context: Context) {
        load(context)
        if (on) start(context)
    }

    /** Stops listening but keeps the saved choice, for sign-out. */
    fun stop(context: Context) {
        context.stopService(Intent(context, OfferAlertService::class.java))
    }

    /** True on Android 13 and later, where the person has to allow notifications first. */
    val needsNotificationPermission: Boolean
        get() = Build.VERSION.SDK_INT >= 33

    fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun start(context: Context) {
        try {
            ContextCompat.startForegroundService(context, Intent(context, OfferAlertService::class.java))
        } catch (e: IllegalStateException) {
            // Android 12 and later refuse to start it while the app is in the background.
            Log.w("KitchenPhone", "Could not start the kitchen phone", e)
        }
    }

    private fun save(context: Context, value: Boolean) {
        on = value
        prefs(context).edit().putBoolean(KEY_KITCHEN_PHONE, value).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
