package xyz.ksharma.krail.core.permission.data

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.NSObject
import xyz.ksharma.krail.core.permission.AppPermission
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

    override suspend fun requestPermission(permission: AppPermission): PermissionResult {
        resolveExistingStatus(permission)?.let { return it }
        stateTracker.markAsRequested(permission)
        return suspendCancellableCoroutine { continuation ->
            val delegate = LocationAuthorizationDelegate { newStatus ->
                cachedStatus = newStatus // keep cache current during the request
                val result = when (newStatus.toPermissionStatus()) {
                    PermissionStatus.Granted -> PermissionResult.Granted
                    else -> PermissionResult.Denied
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

            when (permission) {
                is AppPermission.Location -> locationManager.requestWhenInUseAuthorization()
            }
        }
    }

    override suspend fun checkPermissionStatus(permission: AppPermission): PermissionStatus =
        when (permission) {
            is AppPermission.Location -> cachedStatus.toPermissionStatus()
        }

    override fun wasPermissionRequested(permission: AppPermission): Boolean =
        stateTracker.wasRequested(permission)

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
    private fun resolveExistingStatus(permission: AppPermission): PermissionResult? =
        when (cachedStatus.toPermissionStatus()) {
            is PermissionStatus.Granted -> {
                stateTracker.markAsRequested(permission)
                PermissionResult.Granted
            }
            is PermissionStatus.Denied -> PermissionResult.Denied
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
