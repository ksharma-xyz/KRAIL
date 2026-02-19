package xyz.ksharma.krail.core.maps.data.location

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import xyz.ksharma.krail.core.location.Location
import xyz.ksharma.krail.core.location.LocationConfig
import xyz.ksharma.krail.core.location.LocationError
import xyz.ksharma.krail.core.location.data.LocationTracker
import xyz.ksharma.krail.core.permission.LocationPermissionType
import xyz.ksharma.krail.core.permission.data.PermissionController
import xyz.ksharma.krail.core.permission.PermissionResult
import xyz.ksharma.krail.core.permission.PermissionStatus

internal class UserLocationManagerImpl(
    private val permissionController: PermissionController,
    private val locationTracker: LocationTracker,
) : UserLocationManager {

    override suspend fun getCurrentLocation(): Result<Location> {
        return when (permissionController.checkPermissionStatus(LocationPermissionType.LOCATION_WHEN_IN_USE)) {
            is PermissionStatus.Granted -> getLocation()
            is PermissionStatus.NotDetermined,
            is PermissionStatus.Denied.Temporary -> requestPermissionAndGetLocation()
            is PermissionStatus.Denied.Permanent -> Result.failure(LocationError.PermissionDenied)
        }
    }

    override fun locationUpdates(config: LocationConfig): Flow<Location> = flow {
        when (permissionController.checkPermissionStatus(LocationPermissionType.LOCATION_WHEN_IN_USE)) {
            is PermissionStatus.Granted -> Unit
            is PermissionStatus.NotDetermined,
            is PermissionStatus.Denied.Temporary -> {
                val result = permissionController.requestPermission(LocationPermissionType.LOCATION_WHEN_IN_USE)
                if (result !is PermissionResult.Granted) throw LocationError.PermissionDenied
            }
            is PermissionStatus.Denied.Permanent -> throw LocationError.PermissionDenied
        }
        emitAll(locationTracker.startTracking(config))
    }

    override suspend fun checkPermissionStatus(): PermissionStatus =
        permissionController.checkPermissionStatus(LocationPermissionType.LOCATION_WHEN_IN_USE)

    override fun openAppSettings() = permissionController.openAppSettings()

    private suspend fun requestPermissionAndGetLocation(): Result<Location> =
        when (permissionController.requestPermission(LocationPermissionType.LOCATION_WHEN_IN_USE)) {
            is PermissionResult.Granted -> getLocation()
            is PermissionResult.Denied, is PermissionResult.Cancelled ->
                Result.failure(LocationError.PermissionDenied)
        }

    private suspend fun getLocation(): Result<Location> =
        try {
            Result.success(locationTracker.getCurrentLocation(timeoutMs = 10_000L))
        } catch (e: LocationError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(LocationError.Unknown(e))
        }
}
