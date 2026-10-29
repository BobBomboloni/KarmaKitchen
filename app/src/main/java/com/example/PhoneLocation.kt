package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

/**
 * Where the phone is: a fresh fix when one comes within a few seconds, else the last known one.
 * Null without location permission or when the phone has no idea. Uses Google Play services, which
 * is free and needs no map key.
 */
@SuppressLint("MissingPermission")
suspend fun phonePlace(context: Context): Place? {
    return try {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cancel = CancellationTokenSource()
        val fresh = withTimeoutOrNull(8_000) {
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancel.token).await()
        }
        if (fresh == null) cancel.cancel()
        val location = fresh ?: client.lastLocation.await()
        location?.let { Place(it.latitude, it.longitude) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w("PhoneLocation", "Could not get the phone's location", e)
        null
    }
}

/** The street address at [place], or null when the phone cannot look it up. */
@Suppress("DEPRECATION")
suspend fun addressOf(context: Context, place: Place): String? = withContext(Dispatchers.IO) {
    try {
        Geocoder(context, Locale.getDefault()).getFromLocation(place.lat, place.lng, 1)?.firstOrNull()?.getAddressLine(0)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w("PhoneLocation", "Could not look up the address", e)
        null
    }
}

/** Where a typed [address] is, or null when the phone cannot find it. */
@Suppress("DEPRECATION")
suspend fun placeOfAddress(context: Context, address: String): Place? = withContext(Dispatchers.IO) {
    if (address.isBlank() || !Geocoder.isPresent()) return@withContext null
    try {
        Geocoder(context, Locale.getDefault()).getFromLocationName(address, 1)?.firstOrNull()
            ?.let { Place(it.latitude, it.longitude) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w("PhoneLocation", "Could not find that address", e)
        null
    }
}
