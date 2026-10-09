package com.example.cloud

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds use the App Check debug provider. On first launch it prints a debug token to
 * Logcat ("Enter this debug secret into the allow list..."); add it under App Check > Manage debug
 * tokens in the Firebase console before turning on enforcement.
 */
internal fun appCheckProviderFactory(): AppCheckProviderFactory = DebugAppCheckProviderFactory.getInstance()
