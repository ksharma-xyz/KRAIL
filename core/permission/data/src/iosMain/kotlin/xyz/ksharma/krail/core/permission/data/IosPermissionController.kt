package xyz.ksharma.krail.core.permission.data

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.*
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
        val permissionStatus = cachedStatus.toPermissionStatus(type)

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

        // Request authorization — swap to an authorization delegate, restore persistent
        // delegate once the result is known so status changes keep being cached.
        return suspendCancellableCoroutine { continuation ->
            val delegate = LocationAuthorizationDelegate { newStatus ->
                cachedStatus = newStatus  // keep cache current during the request
                val newPermissionStatus = newStatus.toPermissionStatus(type)

                when (newPermissionStatus) {
                    is PermissionStatus.Granted ->
                        continuation.resume(PermissionResult.Granted)
                    is PermissionStatus.Denied.Permanent ->
                        continuation.resume(PermissionResult.Denied(isPermanent = true))
                    is PermissionStatus.Denied.Temporary ->
                        continuation.resume(PermissionResult.Denied(isPermanent = false))
                    is PermissionStatus.NotDetermined ->
                        continuation.resume(PermissionResult.Denied(isPermanent = false))
                }

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

            // Request appropriate authorization
            when (type) {
                LocationPermissionType.LOCATION_WHEN_IN_USE,
                LocationPermissionType.COARSE_LOCATION ->
                    locationManager.requestWhenInUseAuthorization()
            }
        }
    }

    override suspend fun checkPermissionStatus(type: LocationPermissionType): PermissionStatus {
        // Reads from cache — no authorizationStatus call on the main thread
        return cachedStatus.toPermissionStatus(type)
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
 *
 * [locationManagerDidChangeAuthorization] is already dispatched on the main thread by iOS,
 * so reading [CLLocationManager.authorizationStatus] here is the correct Apple-recommended
 * pattern and does not trigger the "UI unresponsiveness" diagnostic.
 */
private class LocationAuthorizationDelegate(
    private val onAuthorizationChange: (CLAuthorizationStatus) -> Unit
) : NSObject(), CLLocationManagerDelegateProtocol {

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        onAuthorizationChange(manager.authorizationStatus)
    }
}
