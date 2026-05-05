package xyz.ksharma.krail.core.maps.data.location

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import xyz.ksharma.dhruva.location.Location
import xyz.ksharma.dhruva.location.LocationConfig
import xyz.ksharma.dhruva.location.LocationError
import xyz.ksharma.dhruva.location.data.LocationTracker
import xyz.ksharma.aagya.permission.AppPermission
import xyz.ksharma.aagya.permission.PermissionResult
import xyz.ksharma.aagya.permission.PermissionStatus
import xyz.ksharma.aagya.permission.data.PermissionController
import xyz.ksharma.krail.coroutines.ext.suspendSafeResult

internal class UserLocationManagerImpl(
    private val permissionController: PermissionController,
    private val locationTracker: LocationTracker,
) : UserLocationManager {

    override suspend fun getCurrentLocation(): Result<Location> {
        return when (permissionController.checkPermissionStatus(AppPermission.Location.Fine)) {
            is PermissionStatus.Granted -> getLocation()
            is PermissionStatus.NotDetermined -> requestPermissionAndGetLocation()
            is PermissionStatus.Denied,
            PermissionStatus.Restricted -> Result.failure(LocationError.PermissionDenied())
        }
    }

    override fun locationUpdates(config: LocationConfig): Flow<Location> = flow {
        when (permissionController.checkPermissionStatus(AppPermission.Location.Fine)) {
            is PermissionStatus.Granted -> Unit
            is PermissionStatus.NotDetermined -> {
                val result = permissionController.requestPermission(AppPermission.Location.Fine)
                if (result !is PermissionResult.Granted) throw LocationError.PermissionDenied()
            }
            is PermissionStatus.Denied,
            PermissionStatus.Restricted -> throw LocationError.PermissionDenied()
        }
        emitAll(locationTracker.startTracking(config))
    }

    override suspend fun checkPermissionStatus(): PermissionStatus =
        permissionController.checkPermissionStatus(AppPermission.Location.Fine)

    override fun openAppSettings() = permissionController.openAppSettings()

    private suspend fun requestPermissionAndGetLocation(): Result<Location> =
        when (permissionController.requestPermission(AppPermission.Location.Fine)) {
            is PermissionResult.Granted -> getLocation()
            is PermissionResult.Denied,
            is PermissionResult.Cancelled,
            is PermissionResult.PolicyExhausted ->
                Result.failure(LocationError.PermissionDenied())
        }

    private suspend fun getLocation(): Result<Location> = suspendSafeResult(Dispatchers.IO) {
        Result.success(locationTracker.getCurrentLocation(timeoutMs = 10_000L))
    }.getOrElse { e ->
        if (e is LocationError) {
            Result.failure(e)
        } else {
            Result.failure(LocationError.Unknown(e))
        }
    }
}
