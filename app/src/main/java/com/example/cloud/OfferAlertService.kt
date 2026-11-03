package com.example.cloud

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.clockText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Date
import java.util.TimeZone

private const val TAG = "OfferAlertService"
private const val CHANNEL_WATCHING = "kitchen_phone_watching"
private const val CHANNEL_OFFERS = "kitchen_phone_offers"
private const val WATCHING_NOTIFICATION_ID = 4101

/** Offer notifications use this tag, so their ids never clash with the "watching" notification. */
private const val OFFER_TAG = "offer"

/** The donation id a tapped offer notification opens the app with. */
const val EXTRA_OFFER_ID = "com.example.extra.OFFER_ID"

/**
 * The kitchen phone (see [KitchenPhone]). While it runs it listens to open offers and rings for
 * each new one this NGO can take. Offers that were already open when it started do not ring.
 */
class OfferAlertService : Service() {
    private var offersListener: ListenerRegistration? = null
    private var authListener: FirebaseAuth.AuthStateListener? = null
    private var inForeground = false
    private var primed = false

    /** Offers that must not ring: the ones open at the start, and the ones that already rang. */
    private val known = HashSet<String>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Cloud.init(applicationContext)
        createChannels()
        inForeground = goForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (offersListener != null) return START_STICKY
        val uid = if (Cloud.enabled) FirebaseAuth.getInstance().currentUser?.uid else null
        if (!inForeground || uid == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        watch(uid)
        return START_STICKY
    }

    override fun onDestroy() {
        offersListener?.remove()
        offersListener = null
        authListener?.let { FirebaseAuth.getInstance().removeAuthStateListener(it) }
        authListener = null
        super.onDestroy()
    }

    private fun watch(uid: String) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            // Someone else signed in, or the NGO signed out: this phone no longer watches for them.
            if (auth.currentUser?.uid != uid) stopSelf()
        }
        FirebaseAuth.getInstance().addAuthStateListener(listener)
        authListener = listener

        offersListener = FirebaseFirestore.getInstance().collection(DONATIONS)
            .whereEqualTo("status", STATE_OFFERED)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    // A failed listener gets no more updates (most often the NGO is not verified yet).
                    Log.w(TAG, "Offers listener failed, stopping", error)
                    stopSelf()
                    return@addSnapshotListener
                }
                if (snap == null) return@addSnapshotListener
                if (!primed) {
                    snap.documents.forEach { known += it.id }
                    primed = true
                    return@addSnapshotListener
                }
                val now = System.currentTimeMillis()
                for (change in snap.documentChanges) {
                    val id = change.document.id
                    if (change.type == DocumentChange.Type.REMOVED) {
                        // Taken by an NGO or withdrawn: take its notification away too.
                        NotificationManagerCompat.from(this).cancel(OFFER_TAG, id.hashCode())
                        continue
                    }
                    if (id in known) continue
                    // MODIFIED counts too, so a drop-off another NGO declined rings once it is open to all.
                    val data = change.document.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (shouldAlert(data, uid, now)) {
                        known += id
                        showOffer(id, data, uid, now)
                    }
                }
            }
    }

    private fun createChannels() {
        val manager = NotificationManagerCompat.from(this)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_WATCHING, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName("Watching for offers")
                .setDescription("Shown while this phone is the kitchen phone")
                .setSound(null, null)
                .setShowBadge(false)
                .build()
        )
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_OFFERS, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName("New food offers")
                .setDescription("Rings when a donor nearby offers food")
                .build()
        )
    }

    /** Returns false when Android refuses, for example when it restarts the service while the app is closed. */
    private fun goForeground(): Boolean {
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        return try {
            ServiceCompat.startForeground(this, WATCHING_NOTIFICATION_ID, watchingNotification(), type)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Could not start watching for offers", e)
            false
        }
    }

    private fun watchingNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_WATCHING)
            .setSmallIcon(R.drawable.ic_notification_offer)
            .setContentTitle("KarmaKitchen is watching for offers")
            .setContentText("This phone rings when a donor nearby offers food")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setShowWhen(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openApp(null))
            .build()

    private fun showOffer(id: String, data: Map<String, Any?>, myUid: String, nowMs: Long) {
        // Android 13 and later need the person's yes before showing notifications.
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val manager = NotificationManagerCompat.from(this)
        if (!manager.areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(this, CHANNEL_OFFERS)
            .setSmallIcon(R.drawable.ic_notification_offer)
            .setContentTitle(offerAlertTitle(data))
            .setContentText(offerAlertText(data, myUid, nowMs))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(openApp(id))
            .build()
        manager.notify(OFFER_TAG, id.hashCode(), notification)
    }

    private fun openApp(offerId: String?): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (offerId != null) intent.putExtra(EXTRA_OFFER_ID, offerId)
        return PendingIntent.getActivity(
            this,
            offerId?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}

/**
 * Whether a donation should ring this NGO's kitchen phone: it is open, it is not a drop-off meant
 * for another NGO, and its eat-by time has not passed.
 */
internal fun shouldAlert(data: Map<String, Any?>, myUid: String, nowMs: Long): Boolean {
    if (data["status"] != STATE_OFFERED) return false
    val dropOffNgoId = data["dropOffNgoId"] as? String
    if (dropOffNgoId != null && dropOffNgoId != myUid) return false
    val eatBy = timeMs(data["eatBy"])
    if (eatBy != null && eatBy <= nowMs) return false
    return true
}

/** "New food offer: Veg biryani". */
internal fun offerAlertTitle(data: Map<String, Any?>): String {
    val title = (data["title"] as? String)?.trim().orEmpty()
    return if (title.isEmpty()) "New food offer" else "New food offer: $title"
}

/** "12 servings · eat by 9:30 PM", plus "the donor brings it to you" for a drop-off to this NGO. */
internal fun offerAlertText(
    data: Map<String, Any?>,
    myUid: String,
    nowMs: Long,
    zone: TimeZone = TimeZone.getDefault()
): String {
    val parts = mutableListOf<String>()
    val servings = (data["servings"] as? Number)?.toInt()
    if (servings != null && servings > 0) parts += if (servings == 1) "1 serving" else "$servings servings"
    timeMs(data["eatBy"])?.let { parts += "eat by " + clockText(it, nowMs, zone) }
    if (data["dropOffNgoId"] == myUid) parts += "the donor brings it to you"
    if (parts.isEmpty()) return "Open KarmaKitchen to see it"
    return parts.joinToString(" · ").replaceFirstChar { it.uppercase() }
}

/** Milliseconds from a Firestore timestamp, or from a field that already holds milliseconds. */
private fun timeMs(value: Any?): Long? = when (value) {
    is Timestamp -> value.toDate().time
    is Date -> value.time
    is Number -> value.toLong()
    else -> null
}
