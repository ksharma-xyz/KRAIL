package xyz.ksharma.krail.core.location.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import xyz.ksharma.krail.core.location.Location
import xyz.ksharma.krail.core.location.LocationConfig
import xyz.ksharma.krail.core.location.LocationError
import xyz.ksharma.krail.core.location.LocationPriority
import xyz.ksharma.krail.core.log.log
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import android.location.LocationManager as SystemLocationManager

/**
 * Android implementation of [LocationTracker] using FusedLocationProviderClient.
 *
 * Uses Google Play Services Location API for accurate and battery-efficient location tracking.
 */
internal class AndroidLocationTrackerImpl(
    private val context: Context,
) : LocationTracker {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null
    private var boundActivity: ComponentActivity? = null

    /**
     * Bind this tracker to a ComponentActivity for lifecycle management.
     */
    fun bind(activity: ComponentActivity) {
        if (boundActivity === activity) return // Already bound to this exact instance

        boundActivity = activity

        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                stopTracking()
                boundActivity = null
            }
        })
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(timeoutMs: Long): Location {
        if (!isLocationEnabled()) {
            throw LocationError.Unknown(Exception("Location services are disabled"))
        }

        return suspendCancellableCoroutine { continuation ->
            var isCompleted = false

            // Set timeout
            val timeoutRunnable = Runnable {
                if (!isCompleted) {
                    isCompleted = true
                    continuation.resumeWithException(LocationError.Unknown(Exception("Location request timed out")))
                }
            }
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed(timeoutRunnable, timeoutMs)

            // Use last known location as fallback, then request current
            val lastLocationTask = fusedLocationClient.lastLocation
            lastLocationTask.addOnSuccessListener { lastLocation: android.location.Location? ->
                if (!isCompleted && lastLocation != null) {
                    handler.removeCallbacks(timeoutRunnable)
                    isCompleted = true
                    continuation.resume(lastLocation.toCommonLocation())
                } else {
                    // Request current location if last location not available
                    val locationRequest =
                        createLocationRequest(LocationConfig(priority = LocationPriority.HIGH_ACCURACY))
                    val callback = object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            if (!isCompleted) {
                                handler.removeCallbacks(timeoutRunnable)
                                isCompleted = true
                                result.lastLocation?.let { location ->
                                    fusedLocationClient.removeLocationUpdates(this)
                                    continuation.resume(location.toCommonLocation())
                                } ?: run {
                                    fusedLocationClient.removeLocationUpdates(this)
                                    continuation.resumeWithException(
                                        LocationError.Unknown(IllegalStateException("Location is null")),
                                    )
                                }
                            }
                        }
                    }

                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        callback,
                        Looper.getMainLooper(),
                    ).addOnFailureListener { exception ->
                        if (!isCompleted) {
                            handler.removeCallbacks(timeoutRunnable)
                            isCompleted = true
                            continuation.resumeWithException(exception.toLocationError())
                        }
                    }

                    continuation.invokeOnCancellation {
                        if (!isCompleted) {
                            handler.removeCallbacks(timeoutRunnable)
                            fusedLocationClient.removeLocationUpdates(callback)
                        }
                    }
                }
            }.addOnFailureListener { exception ->
                if (!isCompleted) {
                    handler.removeCallbacks(timeoutRunnable)
                    isCompleted = true
                    continuation.resumeWithException(exception.toLocationError())
                }
            }

            continuation.invokeOnCancellation {
                handler.removeCallbacks(timeoutRunnable)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun startTracking(config: LocationConfig): Flow<Location> = callbackFlow {
        if (!isLocationEnabled()) {
            throw LocationError.Unknown(Exception("Location services are disabled"))
        }

        // Seed with last known location so the map shows instantly without waiting
        // for the first update interval to fire (same technique Google Maps uses).
        fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
            lastLocation?.let { trySend(it.toCommonLocation()) }
        }

        val locationRequest = createLocationRequest(config)

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(location.toCommonLocation())
                }
            }
        }

        locationCallback = callback

        log(
            "[USER_LOCATION] Android: FusedLocation requestLocationUpdates " +
                "(interval=${locationRequest.intervalMillis}ms)",
        )
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper(),
        ).addOnFailureListener { exception ->
            close(exception.toLocationError())
        }

        awaitClose {
            log("[USER_LOCATION] Android: FusedLocation removeLocationUpdates (flow cancelled)")
            fusedLocationClient.removeLocationUpdates(callback)
            locationCallback = null
        }
    }.flowOn(Dispatchers.Main)

    override fun stopTracking() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
            locationCallback = null
        }
    }

    override suspend fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? SystemLocationManager
            ?: return false

        return locationManager.isProviderEnabled(SystemLocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(SystemLocationManager.NETWORK_PROVIDER)
    }

    /**
     * Create a LocationRequest from config using the modern Builder API.
     */
    private fun createLocationRequest(config: LocationConfig): LocationRequest {
        return LocationRequest.Builder(config.updateIntervalMs)
            .setPriority(config.priority.toAndroidPriority())
            .setMinUpdateDistanceMeters(config.minDistanceMeters)
            .build()
    }
}
