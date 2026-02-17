package xyz.ksharma.krail.core.location.data

import androidx.compose.runtime.Composable

/**
 * Remember a [LocationTracker] instance for the current composition.
 *
 * On Android, this creates a tracker using FusedLocationProviderClient
 * and binds it to the current activity lifecycle.
 *
 * On iOS, this creates a tracker using CLLocationManager.
 *
 * **Note**: Location permission must be granted before using the tracker.
 *
 * @return A platform-specific [LocationTracker] instance
 */
@Composable
expect fun rememberLocationTracker(): LocationTracker

