package xyz.ksharma.krail.core.location.data.delegate

import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLErrorLocationUnknown
import platform.Foundation.NSError
import platform.darwin.NSObject

/**
 * Delegate for continuous location updates (used by [IosLocationTrackerImpl.startTracking]).
 */
internal class LocationTrackingDelegate(
    private val onLocationUpdate: (CLLocation) -> Unit,
    private val onError: (NSError) -> Unit,
) : NSObject(), CLLocationManagerDelegateProtocol {

    override fun locationManager(
        manager: CLLocationManager,
        didUpdateLocations: List<*>,
    ) {
        val location = didUpdateLocations.lastOrNull() as? CLLocation
        if (location != null) {
            onLocationUpdate(location)
        }
    }

    override fun locationManager(
        manager: CLLocationManager,
        didFailWithError: NSError,
    ) {
        // kCLErrorLocationUnknown (code 0) is transient — CoreLocation cannot get a location
        // right now but will keep trying. Ignore it so the flow stays alive.
        if (didFailWithError.code == kCLErrorLocationUnknown) return
        onError(didFailWithError)
    }
}
