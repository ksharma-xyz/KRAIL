package xyz.ksharma.krail.core.location.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import xyz.ksharma.krail.core.location.Location
import xyz.ksharma.krail.core.location.LocationConfig

/**
 * Stub [LocationTracker] returned when a Composable is rendering inside
 * `LocalInspectionMode` (i.e. the IDE preview pane). Reports a fixed (0, 0)
 * location, never streams updates, and treats location services as enabled —
 * just enough for previews that consume the tracker without exercising the
 * real CoreLocation / FusedLocationProvider stack.
 *
 * Used in place of `AndroidLocationTrackerImpl` / iOS counterpart during
 * preview rendering only — production paths never see this object.
 */
internal object PreviewLocationTracker : LocationTracker {
    override suspend fun getCurrentLocation(timeoutMs: Long): Location =
        Location(latitude = 0.0, longitude = 0.0, timestamp = 0L)

    override fun startTracking(config: LocationConfig): Flow<Location> = emptyFlow()

    override fun stopTracking() = Unit

    override suspend fun isLocationEnabled(): Boolean = true
}
