package com.example.cloud

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.DonationLog
import com.example.IntakeSummary
import com.example.NgoState
import com.example.SmileEntry
import com.example.SmileStore
import com.example.Voucher
import com.example.VoucherStore
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val SMILES = "smiles"
private const val VOUCHERS = "vouchers"

/**
 * Keeps [DonationLog] (the donor's side) and [NgoState] (the NGO's side) in step with the
 * `donations` collection, the signed-in person's smiles with [SmileStore] and a donor's rewards
 * with [VoucherStore], and writes the donor's and NGO's actions back.
 */
object CloudSync {
    private val db get() = FirebaseFirestore.getInstance()
    private val listeners = mutableListOf<ListenerRegistration>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var smileJob: Job? = null
    private lateinit var appContext: Context

    private var offers: List<DonationDoc> = emptyList()
    private var accepted: List<DonationDoc> = emptyList()

    /** Set when the NGO cannot see offers yet, usually because it is waiting to be verified. */
    var ngoProblem by mutableStateOf<String?>(null)
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
    }

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
            listenToSmiles("ngoId", uid, forDonor = false)
        } else {
            DonationLog.clear()
            listeners += db.collection(DONATIONS).whereEqualTo("donorId", uid)
                .addSnapshotListener { snap, error ->
                    if (error != null) {
                        Log.w("CloudSync", "Donations listener failed", error)
                        return@addSnapshotListener
                    }
                    val docs = snap.toDocs().sortedByDescending { it.createdAtMs }
                    val now = System.currentTimeMillis()
                    DonationLog.replaceAll(docs.map { it.toDonationItem(now) })
                }
            listenToSmiles("donorId", uid, forDonor = true)
            listeners += db.collection(USERS).document(uid).collection(VOUCHERS)
                .addSnapshotListener { snap, error ->
                    if (error != null) {
                        Log.w("CloudSync", "Rewards listener failed", error)
                        return@addSnapshotListener
                    }
                    val vouchers = snap?.documents?.mapNotNull { it.toVoucher() } ?: emptyList()
                    VoucherStore.replaceAll(vouchers.sortedByDescending { it.boughtAt })
                }
        }
    }

    /** Smiles this NGO sent, or smiles this donor received (minus the ones they took off their wall). */
    private fun listenToSmiles(field: String, uid: String, forDonor: Boolean) {
        listeners += db.collection(SMILES).whereEqualTo(field, uid)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w("CloudSync", "Smiles listener failed", error)
                    return@addSnapshotListener
                }
                val docs = snap?.documents ?: return@addSnapshotListener
                smileJob?.cancel()
                smileJob = scope.launch {
                    val entries = withContext(Dispatchers.IO) { docs.mapNotNull { it.toSmileEntry(forDonor, appContext.cacheDir) } }
                    SmileStore.replaceCloud(entries)
                }
            }
    }

    fun stop() {
        listeners.forEach { it.remove() }
        listeners.clear()
        smileJob?.cancel()
        offers = emptyList()
        accepted = emptyList()
        ngoProblem = null
        DonationLog.clear()
        NgoState.replaceDonations(emptyList())
        SmileStore.replaceCloud(emptyList())
        VoucherStore.replaceAll(emptyList())
    }

    private fun publishNgo() {
        val now = System.currentTimeMillis()
        val all = (accepted + offers).distinctBy { it.id }.sortedByDescending { it.createdAtMs }
        NgoState.replaceDonations(all.map { it.toNgoDonation(now) })
    }

    /**
     * The donor posts a donation, with a small copy of their [photo] of the food when there is one.
     * Firestore shows it straight away and sends it when online.
     */
    fun postDonation(doc: DonationDoc, photo: Uri?) {
        scope.launch {
            val jpeg = photo?.let { uri ->
                try {
                    cloudJpeg(appContext, uri)
                } catch (e: Exception) {
                    Log.w("CloudSync", "Could not shrink the photo for ${doc.id}; posting without it", e)
                    null
                }
            }
            val ref = db.collection(DONATIONS).document(doc.id)
            val batch = db.batch()
            batch.set(
                ref,
                doc.copy(hasPhoto = jpeg != null).toCreateMap() + mapOf(
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (jpeg != null) {
                CloudPhotos.remember(doc.id, jpeg)
                batch.set(
                    ref.collection(PHOTOS).document(FOOD_PHOTO),
                    mapOf("jpeg" to Blob.fromBytes(jpeg), "createdAt" to FieldValue.serverTimestamp())
                )
            }
            batch.commit().addOnFailureListener { Log.e("CloudSync", "Could not post donation ${doc.id}", it) }
        }
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

    /**
     * The NGO has the food. The same write adds the donation's coins to the donor's balance; the
     * security rules only accept the two together.
     */
    fun markReceived(id: String, intake: IntakeSummary?) {
        val donation = accepted.firstOrNull { it.id == id }
        if (donation == null) {
            Log.e("CloudSync", "Donation $id is not one this NGO accepted")
            return
        }
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
        val batch = db.batch()
        batch.update(
            db.collection(DONATIONS).document(id),
            mapOf(
                "status" to STATE_RECEIVED,
                "intake" to intakeMap,
                "receivedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            db.collection(USERS).document(donation.donorId),
            mapOf("coinsEarned" to FieldValue.increment(donation.coins.toLong()), "lastCreditFor" to id)
        )
        batch.commit().addOnFailureListener { Log.e("CloudSync", "Could not mark donation $id received", it) }
    }

    /**
     * The NGO sends a smile photo ([jpeg], already shrunk) to the donor of a donation it received.
     * Returns the smile to show on this phone, or null when that donation is not one of this NGO's.
     */
    suspend fun sendSmile(
        id: String,
        donationId: String,
        donationTitle: String,
        message: String,
        people: Int,
        jpeg: ByteArray
    ): SmileEntry? {
        val uid = Account.user?.uid ?: return null
        val donation = accepted.firstOrNull { it.id == donationId && it.status == STATE_RECEIVED } ?: return null
        val file = smileFile(appContext.cacheDir, id)
        withContext(Dispatchers.IO) { file.writeBytes(jpeg) }
        val ngoName = currentNgoName()
        db.collection(SMILES).document(id).set(
            mapOf(
                "donationId" to donationId,
                "donationTitle" to donationTitle,
                "donorId" to donation.donorId,
                "ngoId" to uid,
                "ngoName" to ngoName,
                "message" to message,
                "people" to people,
                "consent" to true,
                "hiddenByDonor" to false,
                "jpeg" to Blob.fromBytes(jpeg),
                "sentAt" to FieldValue.serverTimestamp()
            )
        ).addOnFailureListener { Log.e("CloudSync", "Could not send smile $id", it) }
        return SmileEntry(
            id = id,
            donationId = donationId,
            donationTitle = donationTitle,
            ngoName = ngoName,
            message = message,
            people = people,
            photoPath = file.absolutePath,
            sentAt = System.currentTimeMillis()
        )
    }

    /** The donor takes a smile off their wall. The NGO keeps its copy. */
    fun hideSmile(id: String) {
        db.collection(SMILES).document(id).update("hiddenByDonor", true)
            .addOnFailureListener { Log.e("CloudSync", "Could not hide smile $id", it) }
    }

    /**
     * The donor buys rewards for [total] coins. The rewards and the coins spent are saved together,
     * and the security rules refuse it if it would spend more than the donor has earned.
     */
    fun purchase(bought: List<Voucher>, total: Int) {
        val uid = Account.user?.uid ?: return
        val user = db.collection(USERS).document(uid)
        val batch = db.batch()
        batch.update(user, "coinsSpent", FieldValue.increment(total.toLong()))
        bought.forEach { v ->
            batch.set(
                user.collection(VOUCHERS).document(v.id),
                mapOf(
                    "rewardId" to v.rewardId,
                    "brand" to v.brand,
                    "title" to v.title,
                    "code" to v.code,
                    "boughtAt" to v.boughtAt,
                    "paid" to v.paid,
                    "validDays" to v.validDays
                )
            )
        }
        batch.commit().addOnFailureListener { Log.e("CloudSync", "Could not save the purchase", it) }
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

private fun DocumentSnapshot.toVoucher(): Voucher? {
    val rewardId = getString("rewardId") ?: return null
    return Voucher(
        id = id,
        rewardId = rewardId,
        brand = getString("brand") ?: "",
        title = getString("title") ?: "",
        code = getString("code") ?: "",
        boughtAt = getLong("boughtAt") ?: 0L,
        paid = getLong("paid")?.toInt() ?: 0,
        validDays = getLong("validDays")?.toInt() ?: 0
    )
}

/**
 * Reads a smile and saves its photo to the cache folder, where the smile screens load it from.
 * Returns null for smiles the donor has taken off their wall.
 */
private fun DocumentSnapshot.toSmileEntry(forDonor: Boolean, cacheDir: File): SmileEntry? {
    if (forDonor && getBoolean("hiddenByDonor") == true) return null
    val file = smileFile(cacheDir, id)
    if (!file.exists()) {
        val jpeg = getBlob("jpeg")?.toBytes() ?: return null
        file.writeBytes(jpeg)
    }
    return SmileEntry(
        id = id,
        donationId = getString("donationId") ?: "",
        donationTitle = getString("donationTitle") ?: "",
        ngoName = getString("ngoName") ?: "",
        message = getString("message") ?: "",
        people = getLong("people")?.toInt() ?: 0,
        photoPath = file.absolutePath,
        sentAt = getTimestamp("sentAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
            ?: System.currentTimeMillis()
    )
}

/** Where a smile's photo from the cloud is kept on this phone. */
private fun smileFile(cacheDir: File, id: String): File =
    File(File(cacheDir, "cloud-smiles").apply { mkdirs() }, "$id.jpg")
