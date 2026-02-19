package xyz.ksharma.krail.core.permission.data

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.NSObject
import xyz.ksharma.krail.core.permission.LocationPermissionType
import xyz.ksharma.krail.core.permission.PermissionResult
import xyz.ksharma.krail.core.permission.PermissionStatus
import kotlin.coroutines.resume

/**
 * iOS implementation of [PermissionController].
 *
 * Uses CLLocationManager to handle location permissions on iOS.
 *
 * Authorization status is cached and kept up to date via [locationManagerDidChangeAuthorization],
 * so [checkPermissionStatus] never reads [CLLocationManager.authorizationStatus] on the main
 * thread during a button tap — eliminating the iOS 17 "UI unresponsiveness" diagnostic.
 */
internal class IosPermissionController : PermissionController {

    private val stateTracker = PermissionStateTracker()
    private val locationManager = CLLocationManager()
    private var authorizationDelegate: LocationAuthorizationDelegate? = null

    // Cached status — read once at init, then kept current by the persistent delegate.
    // Avoids calling locationManager.authorizationStatus on the main thread repeatedly.
    private var cachedStatus: CLAuthorizationStatus = locationManager.authorizationStatus

    private val persistentDelegate = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
            cachedStatus = manager.authorizationStatus
        }
    }

    init {
        locationManager.delegate = persistentDelegate
    }

    override suspend fun requestPermission(type: LocationPermissionType): PermissionResult {
        resolveExistingStatus(type)?.let { return it }
        stateTracker.markAsRequested(type)
        return suspendCancellableCoroutine { continuation ->
            val delegate = LocationAuthorizationDelegate { newStatus ->
                cachedStatus = newStatus // keep cache current during the request
                val result = when (newStatus.toPermissionStatus()) {
                    is PermissionStatus.Granted -> PermissionResult.Granted
                    is PermissionStatus.Denied.Permanent -> PermissionResult.Denied(isPermanent = true)
                    else -> PermissionResult.Denied(isPermanent = false)
                }
                continuation.resume(result)
                // Restore persistent delegate so future status changes keep being cached
                locationManager.delegate = persistentDelegate
                authorizationDelegate = null
            }

            authorizationDelegate = delegate
            locationManager.delegate = delegate

            continuation.invokeOnCancellation {
                locationManager.delegate = persistentDelegate
                authorizationDelegate = null
            }

            when (type) {
                LocationPermissionType.LOCATION_WHEN_IN_USE,
                LocationPermissionType.COARSE_LOCATION,
                -> locationManager.requestWhenInUseAuthorization()
            }
        }
    }

    override suspend fun checkPermissionStatus(type: LocationPermissionType): PermissionStatus =
        cachedStatus.toPermissionStatus()

    override fun wasPermissionRequested(type: LocationPermissionType): Boolean =
        stateTracker.wasRequested(type)

    override fun openAppSettings() {
        UIApplication.sharedApplication.openURL(
            NSURL.URLWithString(UIApplicationOpenSettingsURLString)!!,
        )
    }

    /**
     * Checks the cached permission state before launching the OS dialog.
     * Returns a [PermissionResult] if the request can be short-circuited, or null
     * if the OS dialog should be shown.
     */
    private fun resolveExistingStatus(type: LocationPermissionType): PermissionResult? =
        when (cachedStatus.toPermissionStatus()) {
            is PermissionStatus.Granted -> {
                stateTracker.markAsRequested(type)
                PermissionResult.Granted
            }
            is PermissionStatus.Denied.Permanent -> PermissionResult.Denied(isPermanent = true)
            else -> null
        }
}

/**
 * Delegate for receiving authorization status changes from CLLocationManager.
 *
 * [locationManagerDidChangeAuthorization] is already dispatched on the main thread by iOS,
 * so reading [CLLocationManager.authorizationStatus] here is the correct Apple-recommended
 * pattern and does not trigger the "UI unresponsiveness" diagnostic.
 */
private class LocationAuthorizationDelegate(
    private val onAuthorizationChange: (CLAuthorizationStatus) -> Unit,
) : NSObject(), CLLocationManagerDelegateProtocol {

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        onAuthorizationChange(manager.authorizationStatus)
    }
}
