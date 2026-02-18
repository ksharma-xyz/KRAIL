package xyz.ksharma.krail.core.maps.data.location

import xyz.ksharma.krail.core.location.Location
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
