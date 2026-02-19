package xyz.ksharma.krail.core.location.data.delegate

import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLErrorLocationUnknown
import platform.Foundation.NSError
import platform.darwin.NSObject

/**
 * Delegate for a single-shot location fix (used by [IosLocationTrackerImpl.getCurrentLocation]).
 *
 * Tracks completion internally to guard against CLLocationManager calling both
 * [locationManager(manager:didUpdateLocations:)] and
 * [locationManager(manager:didFailWithError:)] for the same request.
 */
internal class LocationSingleShotDelegate(
    private val onSuccess: (CLLocation) -> Unit,
    private val onError: (NSError) -> Unit,
) : NSObject(), CLLocationManagerDelegateProtocol {

    private var isCompleted = false

    override fun locationManager(
        manager: CLLocationManager,
        didUpdateLocations: List<*>,
    ) {
        if (isCompleted) return
        val location = didUpdateLocations.lastOrNull() as? CLLocation
        if (location != null) {
            isCompleted = true
            manager.stopUpdatingLocation()
            manager.delegate = null
            onSuccess(location)
        }
    }

    override fun locationManager(
        manager: CLLocationManager,
        didFailWithError: NSError,
    ) {
        if (isCompleted) return
        // kCLErrorLocationUnknown (code 0) is transient: CoreLocation cannot get a location
        // right now but will keep trying. Ignore it and wait for didUpdateLocations.
        // Treating it as fatal causes an "Already resumed" crash when didUpdateLocations
        // fires shortly after.
        if (didFailWithError.code == kCLErrorLocationUnknown) return
        isCompleted = true
        manager.stopUpdatingLocation()
        manager.delegate = null
        onError(didFailWithError)
    }

    /** Called from [kotlinx.coroutines.CancellableContinuation.invokeOnCancellation]. */
    fun cancel(manager: CLLocationManager) {
        isCompleted = true
        manager.stopUpdatingLocation()
        manager.delegate = null
    }
}
