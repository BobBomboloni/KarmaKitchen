package com.example.cloud

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.NGO_NAME
import com.example.UserProfile
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

const val ROLE_DONOR = "donor"
const val ROLE_NGO = "ngo"

private const val USERS = "users"
private const val NGOS = "ngos"

/** The signed-in person's `users/{uid}` document, plus their NGO's details for NGO accounts. */
data class CloudUser(
    val uid: String,
    val role: String,
    val name: String,
    val email: String,
    val phone: String = "",
    val address: String = "",
    val coinsSpent: Int = 0,
    val ngoName: String = "",
    val ngoVerified: Boolean = false
)

sealed interface AccountStatus {
    /** Waiting for Firebase to say who is signed in. */
    data object Loading : AccountStatus

    data object SignedOut : AccountStatus

    /** Signed in for the first time: they still have to pick donor or NGO. */
    data class NeedsRole(val uid: String, val name: String, val email: String) : AccountStatus

    /** Signed in with a profile. [isNew] is true right after the profile was created. */
    data class Ready(val uid: String, val role: String, val isNew: Boolean) : AccountStatus

    /** The profile could not be loaded (most often Firestore is not set up or the rules block it). */
    data class Failed(val message: String) : AccountStatus
}

/** Who is signed in, kept as Compose state so screens follow along. */
object Account {
    var status by mutableStateOf<AccountStatus>(AccountStatus.Loading)
        private set
    var user by mutableStateOf<CloudUser?>(null)
        private set

    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()

    private var listeningUid: String? = null
    private var userListener: ListenerRegistration? = null
    private var ngoListener: ListenerRegistration? = null
    private var justCreated = false

    fun start(context: Context) {
        auth.addAuthStateListener { onAuthChanged(it.currentUser) }
    }

    private fun onAuthChanged(firebaseUser: FirebaseUser?) {
        if (firebaseUser?.uid == listeningUid && firebaseUser != null) return
        stopListening()
        if (firebaseUser == null) {
            user = null
            status = AccountStatus.SignedOut
            return
        }
        listen(firebaseUser)
    }

    private fun listen(firebaseUser: FirebaseUser) {
        val uid = firebaseUser.uid
        listeningUid = uid
        status = AccountStatus.Loading
        userListener = db.collection(USERS).document(uid).addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e("Account", "Could not load the profile", error)
                status = AccountStatus.Failed(firestoreErrorMessage(error))
                return@addSnapshotListener
            }
            if (snap == null || !snap.exists()) {
                user = null
                status = AccountStatus.NeedsRole(uid, firebaseUser.displayName ?: "", firebaseUser.email ?: "")
                return@addSnapshotListener
            }
            val role = snap.getString("role") ?: ROLE_DONOR
            val loaded = CloudUser(
                uid = uid,
                role = role,
                name = snap.getString("name") ?: "",
                email = snap.getString("email") ?: firebaseUser.email ?: "",
                phone = snap.getString("phone") ?: "",
                address = snap.getString("address") ?: "",
                coinsSpent = snap.getLong("coinsSpent")?.toInt() ?: 0,
                ngoName = user?.ngoName ?: "",
                ngoVerified = user?.ngoVerified ?: false
            )
            user = loaded
            if (status !is AccountStatus.Ready) {
                status = AccountStatus.Ready(uid, role, isNew = justCreated)
                justCreated = false
                if (role == ROLE_NGO) listenToNgo(uid)
                CloudSync.start(uid, role)
            }
        }
    }

    /** Tries loading the profile again after [AccountStatus.Failed]. */
    fun retry() {
        val firebaseUser = auth.currentUser
        stopListening()
        if (firebaseUser == null) status = AccountStatus.SignedOut else listen(firebaseUser)
    }

    private fun listenToNgo(uid: String) {
        ngoListener = db.collection(NGOS).document(uid).addSnapshotListener { snap, error ->
            if (error != null || snap == null) return@addSnapshotListener
            user = user?.copy(
                ngoName = snap.getString("name") ?: "",
                ngoVerified = snap.getBoolean("verified") ?: false
            )
        }
    }

    private fun stopListening() {
        userListener?.remove()
        ngoListener?.remove()
        userListener = null
        ngoListener = null
        listeningUid = null
        CloudSync.stop()
    }

    /** Signs in, or creates an account when [create] is true. Throws with a readable message. */
    suspend fun signInWithEmail(email: String, password: String, create: Boolean) {
        try {
            if (create) {
                auth.createUserWithEmailAndPassword(email.trim(), password).await()
            } else {
                auth.signInWithEmailAndPassword(email.trim(), password).await()
            }
        } catch (e: Exception) {
            throw SignInException(authErrorMessage(e), e)
        }
    }

    /**
     * Shows the Google account picker and signs in with the chosen account. Returns quietly when
     * the person closes the picker.
     */
    suspend fun signInWithGoogle(activityContext: Context) {
        val clientId = webClientId(activityContext)
            ?: throw SignInException(
                "Google sign-in is not switched on yet. Turn it on in the Firebase console, then download google-services.json again."
            )
        try {
            val option = GetSignInWithGoogleOption.Builder(clientId).build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
            if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                throw SignInException("That account type is not supported. Please choose a Google account.")
            }
            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        } catch (e: GetCredentialCancellationException) {
            return
        } catch (e: NoCredentialException) {
            throw SignInException("There is no Google account on this phone. Add one in Settings, or use email.", e)
        } catch (e: SignInException) {
            throw e
        } catch (e: Exception) {
            throw SignInException(authErrorMessage(e), e)
        }
    }

    /** First sign-in: saves the role (it cannot be changed later) and, for NGOs, the NGO itself. */
    suspend fun createProfile(role: String, name: String, phone: String, ngoName: String) {
        val firebaseUser = auth.currentUser ?: throw SignInException("You are signed out. Please sign in again.")
        val uid = firebaseUser.uid
        val batch = db.batch()
        batch.set(
            db.collection(USERS).document(uid),
            mapOf(
                "role" to role,
                "name" to name.trim(),
                "email" to (firebaseUser.email ?: ""),
                "phone" to phone.trim(),
                "address" to "",
                "coinsSpent" to 0,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        if (role == ROLE_NGO) {
            batch.set(
                db.collection(NGOS).document(uid),
                mapOf(
                    "name" to ngoName.trim(),
                    "area" to "",
                    "verified" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }
        justCreated = true
        try {
            batch.commit().await()
        } catch (e: Exception) {
            justCreated = false
            throw SignInException(
                if (e is FirebaseFirestoreException) firestoreErrorMessage(e) else authErrorMessage(e),
                e
            )
        }
    }

    /**
     * Saves profile edits. [spent] is how many coins were just spent in the store; the total only
     * ever goes up, and the balance shown is coins earned minus coins spent.
     */
    fun saveProfile(profile: UserProfile, spent: Int) {
        val current = user ?: return
        user = current.copy(
            name = profile.name,
            email = profile.email,
            phone = profile.phone,
            address = profile.address,
            coinsSpent = current.coinsSpent + spent.coerceAtLeast(0)
        )
        val changes = mutableMapOf<String, Any>(
            "name" to profile.name,
            "email" to profile.email,
            "phone" to profile.phone,
            "address" to profile.address
        )
        if (spent > 0) changes["coinsSpent"] = FieldValue.increment(spent.toLong())
        db.collection(USERS).document(current.uid).update(changes)
            .addOnFailureListener { Log.e("Account", "Could not save the profile", it) }
    }

    suspend fun signOut(context: Context) {
        auth.signOut()
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w("Account", "Could not clear the saved Google account", e)
        }
    }

    /** The OAuth client the google-services plugin writes into resources once Google sign-in is on. */
    private fun webClientId(context: Context): String? {
        val res = context.resources
        val id = res.getIdentifier("default_web_client_id", "string", context.packageName)
            .takeIf { it != 0 }
            ?: res.getIdentifier("default_web_client_id", "string", "com.example")
        return if (id != 0) res.getString(id).takeIf { it.isNotBlank() } else null
    }
}

class SignInException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** The NGO name to show and to stamp on accepted donations. */
fun currentNgoName(): String = Account.user?.ngoName?.takeIf { it.isNotBlank() } ?: NGO_NAME

private fun authErrorMessage(e: Exception): String = when (e) {
    is FirebaseAuthWeakPasswordException -> "Please use a password with at least 6 characters."
    is FirebaseAuthUserCollisionException -> "There is already an account with this email. Sign in instead."
    is FirebaseAuthInvalidUserException -> "There is no account with this email yet. Create one first."
    is FirebaseAuthInvalidCredentialsException -> "That email and password do not match."
    is FirebaseNetworkException -> "No internet connection. Please try again."
    else -> e.message ?: "Something went wrong. Please try again."
}

private fun firestoreErrorMessage(e: FirebaseFirestoreException): String = when (e.code) {
    FirebaseFirestoreException.Code.PERMISSION_DENIED ->
        "The app is not allowed to read your profile. Check that the Firestore rules from firestore.rules are published."
    FirebaseFirestoreException.Code.NOT_FOUND ->
        "The Firestore database has not been created yet. Create it in the Firebase console."
    FirebaseFirestoreException.Code.UNAVAILABLE -> "No internet connection. Please try again."
    else -> e.message ?: "Could not load your profile."
}
