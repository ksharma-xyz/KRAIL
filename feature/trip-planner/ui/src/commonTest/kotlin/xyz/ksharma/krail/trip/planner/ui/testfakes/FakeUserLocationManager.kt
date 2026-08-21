package xyz.ksharma.krail.trip.planner.ui.testfakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import xyz.ksharma.aagya.permission.PermissionStatus
import xyz.ksharma.dhruva.location.Location
import xyz.ksharma.dhruva.location.LocationConfig
import xyz.ksharma.krail.core.maps.data.location.UserLocationManager

/**
 * Canned permission status plus a call record. Lives feature-local rather than in
 * `:core:testing` because `UserLocationManager`'s signature pulls in aagya and dhruva, which
 * `:core:maps:data` keeps as `implementation` — promoting this fake would widen the shared fakes
 * module's API surface for a single consumer. Promote it the moment a second module needs it.
 */
internal class FakeUserLocationManager(
    private val permissionStatus: PermissionStatus = PermissionStatus.NotDetermined,
) : UserLocationManager {

    var permissionChecks = 0
        private set

    var openAppSettingsCount = 0
        private set

    override suspend fun getCurrentLocation(): Result<Location> =
        Result.failure(IllegalStateException("not used in these tests"))

    override fun locationUpdates(config: LocationConfig): Flow<Location> = emptyFlow()

    override suspend fun checkPermissionStatus(): PermissionStatus {
        permissionChecks++
        return permissionStatus
    }

    override fun openAppSettings() {
        openAppSettingsCount++
    }
}
