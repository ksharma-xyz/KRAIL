package xyz.ksharma.krail.core.location.data

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import xyz.ksharma.krail.core.location.Location

/**
 * Android implementation of [rememberLocationTracker].
 *
 * The tracker is retained across rotations via [remember] without an activity key.
 * [DisposableEffect] rebinds it to the new activity after a configuration change.
 */
@Composable
actual fun rememberLocationTracker(): LocationTracker {
    if (LocalInspectionMode.current) {
        return remember {
            object : LocationTracker {
                override suspend fun getCurrentLocation(timeoutMs: Long): Location =
                    Location(latitude = 0.0, longitude = 0.0, timestamp = 0L)

                override fun startTracking(config: xyz.ksharma.krail.core.location.LocationConfig): Flow<Location> =
                    emptyFlow()

                override fun stopTracking() {}

                override suspend fun isLocationEnabled(): Boolean = true
            }
        }
    }

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
