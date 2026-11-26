package xyz.ksharma.krail.core.maps.data.location

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import xyz.ksharma.aagya.permission.AppPermission
import xyz.ksharma.aagya.permission.PermissionResult
import xyz.ksharma.aagya.permission.PermissionStatus
import xyz.ksharma.aagya.permission.data.PermissionController
import xyz.ksharma.dhruva.location.Location
import xyz.ksharma.dhruva.location.LocationConfig
import xyz.ksharma.dhruva.location.LocationError
import xyz.ksharma.dhruva.location.data.LocationTracker
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.PermissionResultEvent
import xyz.ksharma.krail.coroutines.ext.suspendSafeResult

internal class UserLocationManagerImpl(
    private val permissionController: PermissionController,
    private val locationTracker: LocationTracker,
    private val recorder: LocationPermissionRecorder,
) : UserLocationManager {

    override suspend fun getCurrentLocation(source: PermissionResultEvent.Source): Result<Location> =
        if (ensureAllowed(source)) getLocation() else Result.failure(LocationError.PermissionDenied())

    override fun locationUpdates(
        config: LocationConfig,
        requestSource: PermissionResultEvent.Source?,
    ): Flow<Location> = flow {
        if (!ensureAllowed(requestSource)) throw LocationError.PermissionDenied()
        emitAll(locationTracker.startTracking(config))
    }

    override suspend fun checkPermissionStatus(): PermissionStatus {
        val status = permissionController.checkPermissionStatus(AppPermission.Location.Fine)
        // Noted for [reportedPermissionStatus] only.
        when (status) {
            is PermissionStatus.Granted -> recorder.wasRefused = false
            is PermissionStatus.Denied -> recorder.wasRefused = true
            PermissionStatus.NotDetermined, PermissionStatus.Restricted -> Unit
        }
        return status
    }

    override suspend fun reportedPermissionStatus(): PermissionStatus {
        val status = checkPermissionStatus()
        return if (status is PermissionStatus.NotDetermined && recorder.wasRefused) {
            PermissionStatus.Denied(canAskAgain = false)
        } else {
            status
        }
    }

    override fun openAppSettings() = permissionController.openAppSettings()

    /** True when location may be read. With no [requestSource] nothing is ever asked of the rider. */
    private suspend fun ensureAllowed(requestSource: PermissionResultEvent.Source?): Boolean =
        when (checkPermissionStatus()) {
            is PermissionStatus.Granted -> true
            is PermissionStatus.NotDetermined -> requestSource != null && request(requestSource)
            is PermissionStatus.Denied,
            PermissionStatus.Restricted,
            -> false
        }

    private suspend fun request(source: PermissionResultEvent.Source): Boolean {
        val result = permissionController.requestPermission(AppPermission.Location.Fine)
        // This app sets no cap of its own on requests, and a request that was never made has no answer to report.
        if (result is PermissionResult.PolicyExhausted) return false
        val allowed = result is PermissionResult.Granted
        recorder.wasRefused = !allowed
        recorder.onRequestAnswered(
            source = source,
            outcome = answeredOutcome(result),
            canAskAgain = (result as? PermissionResult.Denied)?.canAskAgain,
        )
        return allowed
    }

    /**
     * What the rider answered, for analytics only. The app asks for precise location, so an Android rider who picks
     * approximate comes back as not granted and the app treats them as not allowed. The report says which it was.
     */
    private suspend fun answeredOutcome(result: PermissionResult): PermissionResultEvent.Outcome = when {
        result is PermissionResult.Granted -> PermissionResultEvent.Outcome.GRANTED
        permissionController.isApproximateAllowed() -> PermissionResultEvent.Outcome.GRANTED_APPROXIMATE
        result is PermissionResult.Cancelled -> PermissionResultEvent.Outcome.CANCELLED
        else -> PermissionResultEvent.Outcome.DENIED
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

private suspend fun PermissionController.isApproximateAllowed(): Boolean =
    checkPermissionStatus(AppPermission.Location.Coarse) is PermissionStatus.Granted
