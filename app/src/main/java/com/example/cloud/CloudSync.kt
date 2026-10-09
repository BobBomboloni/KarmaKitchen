package com.example.cloud

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.DonationLog
import com.example.IntakeSummary
import com.example.NgoState
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot

/**
 * Keeps [DonationLog] (the donor's side) and [NgoState] (the NGO's side) in step with the
 * `donations` collection, and writes the donor's and NGO's actions back to it.
 */
object CloudSync {
    private val db get() = FirebaseFirestore.getInstance()
    private val listeners = mutableListOf<ListenerRegistration>()

    private var offers: List<DonationDoc> = emptyList()
    private var accepted: List<DonationDoc> = emptyList()

    /** The signed-in donor's donations, newest first. */
    var donorDonations by mutableStateOf<List<DonationDoc>>(emptyList())
        private set

    /** Set when the NGO cannot see offers yet, usually because it is waiting to be verified. */
    var ngoProblem by mutableStateOf<String?>(null)
        private set

    fun start(uid: String, role: String) {
        stop()
        if (role == ROLE_NGO) {
            NgoState.replaceDonations(emptyList())
            listeners += db.collection(DONATIONS).whereEqualTo("status", STATE_OFFERED)
                .addSnapshotListener { snap, error ->
                    if (error != null) {
                        Log.w("CloudSync", "Offers listener failed", error)
                        ngoProblem = if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            "Your NGO is waiting to be verified. Once KarmaKitchen verifies it, offers from donors show up here."
                        } else {
                            "Could not load offers. Check your internet connection."
                        }
                        return@addSnapshotListener
                    }
                    ngoProblem = null
                    offers = snap.toDocs()
                    publishNgo()
                }
            listeners += db.collection(DONATIONS).whereEqualTo("ngoId", uid)
                .addSnapshotListener { snap, error ->
                    if (error != null) {
                        Log.w("CloudSync", "Accepted donations listener failed", error)
                        return@addSnapshotListener
                    }
                    accepted = snap.toDocs()
                    publishNgo()
                }
        } else {
            DonationLog.clear()
            listeners += db.collection(DONATIONS).whereEqualTo("donorId", uid)
                .addSnapshotListener { snap, error ->
                    if (error != null) {
                        Log.w("CloudSync", "Donations listener failed", error)
                        return@addSnapshotListener
                    }
                    val docs = snap.toDocs().sortedByDescending { it.createdAtMs }
                    donorDonations = docs
                    val now = System.currentTimeMillis()
                    DonationLog.replaceAll(docs.map { it.toDonationItem(now) })
                }
        }
    }

    fun stop() {
        listeners.forEach { it.remove() }
        listeners.clear()
        offers = emptyList()
        accepted = emptyList()
        donorDonations = emptyList()
        ngoProblem = null
        DonationLog.clear()
        NgoState.replaceDonations(emptyList())
    }

    private fun publishNgo() {
        val now = System.currentTimeMillis()
        val all = (accepted + offers).distinctBy { it.id }.sortedByDescending { it.createdAtMs }
        NgoState.replaceDonations(all.map { it.toNgoDonation(now) })
    }

    /** The donor posts a donation. Firestore shows it straight away and sends it when online. */
    fun postDonation(doc: DonationDoc) {
        val data = doc.toCreateMap() + mapOf(
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection(DONATIONS).document(doc.id).set(data)
            .addOnFailureListener { Log.e("CloudSync", "Could not post donation ${doc.id}", it) }
    }

    /** The NGO accepts an offer and will collect it. */
    fun accept(id: String, ngoName: String, etaMinutes: Int?) {
        val uid = Account.user?.uid ?: return
        db.collection(DONATIONS).document(id).update(
            mapOf(
                "status" to STATE_ON_THE_WAY,
                "ngoId" to uid,
                "ngoName" to ngoName,
                "volunteer" to ngoName,
                "etaMinutes" to etaMinutes,
                "acceptedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).addOnFailureListener { Log.e("CloudSync", "Could not accept donation $id", it) }
    }

    /** The NGO has the food. This is what unlocks the donor's coins. */
    fun markReceived(id: String, intake: IntakeSummary?) {
        val intakeMap = intake?.let {
            mapOf(
                "verified" to it.verified,
                "freshness" to it.freshness,
                "expiry" to it.expiry,
                "storage" to it.storage,
                "tags" to it.tags,
                "manual" to it.manual
            )
        }
        db.collection(DONATIONS).document(id).update(
            mapOf(
                "status" to STATE_RECEIVED,
                "intake" to intakeMap,
                "receivedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).addOnFailureListener { Log.e("CloudSync", "Could not mark donation $id received", it) }
    }

    /** The donor has seen the "Delivered" card. */
    fun acknowledge(id: String) {
        db.collection(DONATIONS).document(id).update("donorAcknowledged", true)
            .addOnFailureListener { Log.e("CloudSync", "Could not save acknowledgement for $id", it) }
    }
}

private fun QuerySnapshot?.toDocs(): List<DonationDoc> =
    this?.documents?.mapNotNull { it.toDonationDoc() } ?: emptyList()

/** Reads a snapshot, using the phone's estimate for timestamps the server has not set yet. */
private fun DocumentSnapshot.toDonationDoc(): DonationDoc? {
    val raw = getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return null
    val plain = raw.mapValues { (_, value) -> if (value is Timestamp) value.toDate().time else value }
    return DonationDoc.fromMap(id, plain)
}
