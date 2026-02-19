package xyz.ksharma.krail.core.location.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * iOS implementation of [rememberLocationTracker].
 *
 * Creates a tracker that uses CLLocationManager for location tracking.
 */
@Composable
actual fun rememberLocationTracker(): LocationTracker {
    return remember {
        IosLocationTrackerImpl()
    }
}
