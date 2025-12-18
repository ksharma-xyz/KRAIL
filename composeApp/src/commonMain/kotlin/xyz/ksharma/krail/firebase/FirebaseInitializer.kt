package xyz.ksharma.krail.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError

private var isInitialized = false

/**
 * Initialize Firebase.
 * - Android: Pass application context
 * - iOS: Pass null (Firebase initialized in AppDelegate)
 *
 * @param context Android Application context, or null for iOS
 * @return true if initialization succeeded, false otherwise
 */
fun initializeFirebase(context: Any?): Boolean {
    if (isInitialized) {
        return true
    }

    return try {
        if (context != null) {
            Firebase.initialize(context = context)
            isInitialized = true
            log("Firebase initialized successfully")
            true
        } else {
            // iOS - Firebase initialized in AppDelegate
            isInitialized = true
            true
        }
    } catch (e: Exception) {
        logError("Failed to initialize Firebase: ${e.message}", e)
        false
    }
}

