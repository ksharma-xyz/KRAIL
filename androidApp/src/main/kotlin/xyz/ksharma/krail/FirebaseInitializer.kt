package xyz.ksharma.krail

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.initialize

private var isInitialized = false

/**
 * Initialize Firebase.
 * - Android: Pass application context
 * - iOS: Pass null (Firebase initialized in AppDelegate)
 *
 * @param context Android Application context, or null for iOS
 * @return true if initialization succeeded, false otherwise
 */
fun initializeFirebase(context: Context?): Boolean {
    if (isInitialized) {
        return true
    }

    return try {
        if (context != null) {
            Firebase.initialize(context = context)
            isInitialized = true
            Log.d("FirebaseInit","Firebase initialized successfully")
            true
        } else {
            // iOS - Firebase initialized in AppDelegate
            isInitialized = true
            true
        }
    } catch (e: Exception) {
        Log.e("FirebaseInit","Failed to initialize Firebase: ${e.message}", e)
        false
    }
}
