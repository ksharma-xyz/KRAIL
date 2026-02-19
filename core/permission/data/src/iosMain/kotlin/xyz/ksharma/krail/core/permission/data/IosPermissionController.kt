package xyz.ksharma.krail.core.permission.data

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.*
import platform.Foundation.NSRunLoop
import platform.Foundation.NSURL
import platform.Foundation.performBlock
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
 */
internal class IosPermissionController : PermissionController {

    private val stateTracker = PermissionStateTracker()
    private val locationManager = CLLocationManager()
    private var authorizationDelegate: LocationAuthorizationDelegate? = null

    override suspend fun requestPermission(type: LocationPermissionType): PermissionResult {
        val currentStatus = locationManager.authorizationStatus
        val permissionStatus = currentStatus.toPermissionStatus(type)

        // Check if already granted
        if (permissionStatus is PermissionStatus.Granted) {
            stateTracker.markAsRequested(type)
            return PermissionResult.Granted
        }

        // Check if permanently denied
        if (permissionStatus is PermissionStatus.Denied.Permanent) {
            return PermissionResult.Denied(isPermanent = true)
        }

        // Mark as requested
        stateTracker.markAsRequested(type)

        // Request authorization
        return suspendCancellableCoroutine { continuation ->
            val delegate = LocationAuthorizationDelegate { newStatus ->
                val newPermissionStatus = newStatus.toPermissionStatus(type)

                when (newPermissionStatus) {
                    is PermissionStatus.Granted -> {
                        continuation.resume(PermissionResult.Granted)
                    }
                    is PermissionStatus.Denied.Permanent -> {
                        continuation.resume(PermissionResult.Denied(isPermanent = true))
                    }
                    is PermissionStatus.Denied.Temporary -> {
                        continuation.resume(PermissionResult.Denied(isPermanent = false))
                    }
                    is PermissionStatus.NotDetermined -> {
                        // Shouldn't happen, but treat as temporary denial
                        continuation.resume(PermissionResult.Denied(isPermanent = false))
                    }
                }
            }

            authorizationDelegate = delegate
            locationManager.delegate = delegate

            continuation.invokeOnCancellation {
                authorizationDelegate = null
                locationManager.delegate = null
            }

            // Request appropriate authorization
            when (type) {
                LocationPermissionType.LOCATION_WHEN_IN_USE,
                LocationPermissionType.COARSE_LOCATION -> {
                    locationManager.requestWhenInUseAuthorization()
                }
            }
        }
    }

    override suspend fun checkPermissionStatus(type: LocationPermissionType): PermissionStatus {
        val currentStatus = locationManager.authorizationStatus
        return currentStatus.toPermissionStatus(type)
    }

    override fun wasPermissionRequested(type: LocationPermissionType): Boolean {
        return stateTracker.wasRequested(type)
    }

    override fun openAppSettings() {
        val settingsUrl = UIApplicationOpenSettingsURLString
        UIApplication.sharedApplication.openURL(
            NSURL.URLWithString(settingsUrl)!!
        )
    }
}

/**
 * Delegate for receiving authorization status changes from CLLocationManager.
 */
private class LocationAuthorizationDelegate(
    private val onAuthorizationChange: (CLAuthorizationStatus) -> Unit
) : NSObject(), CLLocationManagerDelegateProtocol {

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        // Execute callback on main thread
        NSRunLoop.mainRunLoop.performBlock {
            onAuthorizationChange(manager.authorizationStatus)
        }
    }
}

