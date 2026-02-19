package xyz.ksharma.krail.core.location.data

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import platform.CoreLocation.*
import platform.CoreLocation.kCLErrorLocationUnknown
import platform.Foundation.NSError
import platform.darwin.NSObject
import xyz.ksharma.krail.core.location.Location
import xyz.ksharma.krail.core.location.LocationConfig
import xyz.ksharma.krail.core.location.LocationError
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * iOS implementation of [LocationTracker] using CLLocationManager.
 */
internal class IosLocationTrackerImpl : LocationTracker {

    private val locationManager = CLLocationManager()
    private var trackingDelegate: LocationTrackingDelegate? = null

    override suspend fun getCurrentLocation(timeoutMs: Long): Location {
        if (!isLocationEnabled()) {
            throw LocationError.LocationDisabled
        }

        return withTimeout(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                // Guard against CLLocationManager resuming the same request twice.
                var isCompleted = false

                val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                    override fun locationManager(
                        manager: CLLocationManager,
                        didUpdateLocations: List<*>
                    ) {
                        if (isCompleted) return
                        val location = didUpdateLocations.lastOrNull() as? CLLocation
                        if (location != null) {
                            isCompleted = true
                            manager.stopUpdatingLocation()
                            manager.delegate = null
                            continuation.resume(location.toCommonLocation())
                        }
                    }

                    override fun locationManager(
                        manager: CLLocationManager,
                        didFailWithError: NSError
                    ) {
                        if (isCompleted) return
                        // kCLErrorLocationUnknown (code 0) is transient: CoreLocation cannot get
                        // a location right now but will keep trying. Ignore it and wait for
                        // didUpdateLocations. Treating it as fatal causes an "Already resumed"
                        // crash when didUpdateLocations fires shortly after.
                        if (didFailWithError.code == kCLErrorLocationUnknown) return
                        isCompleted = true
                        manager.stopUpdatingLocation()
                        manager.delegate = null
                        continuation.resumeWithException(
                            LocationError.Unknown(Exception(didFailWithError.localizedDescription))
                        )
                    }
                }

                locationManager.delegate = delegate
                locationManager.desiredAccuracy = kCLLocationAccuracyBest
                locationManager.startUpdatingLocation()

                continuation.invokeOnCancellation {
                    isCompleted = true
                    locationManager.stopUpdatingLocation()
                    locationManager.delegate = null
                }
            }
        }
    }

    override fun startTracking(config: LocationConfig): Flow<Location> = callbackFlow {
        if (!isLocationEnabled()) {
            throw LocationError.LocationDisabled
        }

        val delegate = LocationTrackingDelegate(
            onLocationUpdate = { location ->
                trySend(location.toCommonLocation())
            },
            onError = { error ->
                close(LocationError.Unknown(Exception(error.localizedDescription)))
            }
        )

        trackingDelegate = delegate

        // Configure location manager
        locationManager.delegate = delegate
        locationManager.desiredAccuracy = config.priority.toiOSAccuracy()
        locationManager.distanceFilter = config.minDistanceMeters.toDouble()

        // Start updating location
        locationManager.startUpdatingLocation()

        awaitClose {
            locationManager.stopUpdatingLocation()
            locationManager.delegate = null
            trackingDelegate = null
        }
    }

    override fun stopTracking() {
        locationManager.stopUpdatingLocation()
        locationManager.delegate = null
        trackingDelegate = null
    }

    override suspend fun isLocationEnabled(): Boolean {
        // Check if location services are enabled system-wide
        return CLLocationManager.locationServicesEnabled()
    }
}

/**
 * Delegate for receiving location updates from CLLocationManager.
 */
private class LocationTrackingDelegate(
    private val onLocationUpdate: (CLLocation) -> Unit,
    private val onError: (NSError) -> Unit
) : NSObject(), CLLocationManagerDelegateProtocol {

    override fun locationManager(
        manager: CLLocationManager,
        didUpdateLocations: List<*>
    ) {
        val location = didUpdateLocations.lastOrNull() as? CLLocation
        if (location != null) {
            onLocationUpdate(location)
        }
    }

    override fun locationManager(
        manager: CLLocationManager,
        didFailWithError: NSError
    ) {
        onError(didFailWithError)
    }
}
