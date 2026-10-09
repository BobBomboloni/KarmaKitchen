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
        Account.start(context.applicationContext)
    }
}
