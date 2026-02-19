package xyz.ksharma.krail.core.location.data

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Android implementation of [rememberLocationTracker].
 *
 * The tracker is retained across rotations via [remember] without an activity key.
 * [DisposableEffect] rebinds it to the new activity after a configuration change.
 */
@Composable
actual fun rememberLocationTracker(): LocationTracker {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
        ?: error("LocationTracker requires ComponentActivity.")

    val tracker = remember {
        AndroidLocationTrackerImpl(context.applicationContext)
    }

    DisposableEffect(activity) {
        tracker.bind(activity)
        onDispose { }
    }

    return tracker
}
