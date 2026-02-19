package xyz.ksharma.krail.core.location.data

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLLocationAccuracyBest
import xyz.ksharma.krail.core.location.Location
import xyz.ksharma.krail.core.location.LocationConfig
import xyz.ksharma.krail.core.location.LocationError
import xyz.ksharma.krail.core.location.data.delegate.LocationSingleShotDelegate
import xyz.ksharma.krail.core.location.data.delegate.LocationTrackingDelegate
import xyz.ksharma.krail.core.log.log
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
            throw LocationError.Unknown(Exception("Location services are disabled"))
        }

        return try {
            withTimeout(timeoutMs) {
                suspendCancellableCoroutine { continuation ->
                    val delegate = LocationSingleShotDelegate(
                        onSuccess = { location ->
                            continuation.resume(location.toCommonLocation())
                        },
                        onError = { error ->
                            continuation.resumeWithException(
                                LocationError.Unknown(Exception(error.localizedDescription)),
                            )
                        },
                    )

                    locationManager.delegate = delegate
                    locationManager.desiredAccuracy = kCLLocationAccuracyBest
                    locationManager.startUpdatingLocation()

                    continuation.invokeOnCancellation {
                        delegate.cancel(locationManager)
                    }
                }
            }
        } catch (_: TimeoutCancellationException) {
            throw LocationError.Unknown(Exception("Location request timed out"))
        }
    }

    override fun startTracking(config: LocationConfig): Flow<Location> = callbackFlow {
        if (!isLocationEnabled()) {
            throw LocationError.Unknown(Exception("Location services are disabled"))
        }

        val delegate = LocationTrackingDelegate(
            onLocationUpdate = { location ->
                trySend(location.toCommonLocation())
            },
            onError = { error ->
                close(LocationError.Unknown(Exception(error.localizedDescription)))
            },
        )

        trackingDelegate = delegate

        // Configure location manager
        locationManager.delegate = delegate
        locationManager.desiredAccuracy = config.priority.toIosAccuracy()
        locationManager.distanceFilter = config.minDistanceMeters.toDouble()

        // Seed with cached location so the map shows instantly without waiting
        // for the first delegate callback (same technique Google Maps uses).
        locationManager.location?.let { cached -> trySend(cached.toCommonLocation()) }

        // Start updating location
        log("[USER_LOCATION] iOS: CLLocationManager startUpdatingLocation")
        locationManager.startUpdatingLocation()

        awaitClose {
            log("[USER_LOCATION] iOS: CLLocationManager stopUpdatingLocation (flow cancelled)")
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
