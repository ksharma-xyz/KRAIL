package xyz.ksharma.krail.trip.planner.ui.testfakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import xyz.ksharma.aagya.permission.PermissionStatus
import xyz.ksharma.dhruva.location.Location
import xyz.ksharma.dhruva.location.LocationConfig
import xyz.ksharma.krail.core.maps.data.location.UserLocationManager

/**
 * Canned permission status and fix, plus a call record. Lives feature-local rather than in
 * `:core:testing` because `UserLocationManager`'s signature pulls in aagya and dhruva, which
 * `:core:maps:data` keeps as `implementation` — promoting this fake would widen the shared fakes
 * module's API surface for a single consumer. Promote it the moment a second module needs it.
 */
internal class FakeUserLocationManager(
    private val permissionStatus: PermissionStatus = PermissionStatus.NotDetermined,
    /** What one call to [getCurrentLocation] returns. It may suspend, to stand in for a dialog or a slow fix. */
    currentLocation: suspend () -> Result<Location> = {
        Result.failure(IllegalStateException("not used in these tests"))
    },
) : UserLocationManager {

    private var answer: suspend () -> Result<Location> = currentLocation

    var currentLocationCalls = 0
        private set

    var permissionChecks = 0
        private set

    var openAppSettingsCount = 0
        private set

    override suspend fun getCurrentLocation(): Result<Location> {
        currentLocationCalls++
        return answer()
    }

    /** Make every later call to [getCurrentLocation] answer with [result], in place of the constructor's answer. */
    fun answerWith(result: Result<Location>) {
        answer = { result }
    }

    override fun locationUpdates(config: LocationConfig): Flow<Location> = emptyFlow()

    override suspend fun checkPermissionStatus(): PermissionStatus {
        permissionChecks++
        return permissionStatus
    }

    override fun openAppSettings() {
        openAppSettingsCount++
    }
}
