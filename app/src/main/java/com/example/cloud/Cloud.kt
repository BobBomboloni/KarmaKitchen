package com.example.cloud

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck

/**
 * Whether the app talks to Firebase. It does when `app/google-services.json` was present at build
 * time. Without it the app runs as before: one phone plays both roles and nothing leaves it.
 */
object Cloud {
    var enabled = false
        private set

    fun init(context: Context) {
        if (enabled) return
        val app = FirebaseApp.getApps(context).firstOrNull() ?: FirebaseApp.initializeApp(context)
        if (app == null) {
            Log.i("Cloud", "No google-services.json, running in single-phone demo mode")
            return
        }
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(appCheckProviderFactory())
        enabled = true
        CloudSync.init(context)
    }

    private var started = false

    /**
     * Starts signing in and syncing (who is signed in, then their donations, offers and smiles).
     * MainActivity calls this once the startup animation's logo has formed: starting at launch put
     * this work on the main thread right while the seed fell and the K grew, so it stuttered.
     */
    fun start(context: Context) {
        if (!enabled || started) return
        started = true
        Account.start(context.applicationContext)
    }
}
