package xyz.ksharma.krail.core.permission.data

import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import xyz.ksharma.krail.core.permission.PermissionStatus

/**
 * Map iOS CLAuthorizationStatus to [xyz.ksharma.krail.core.permission.PermissionStatus].
 */
internal fun CLAuthorizationStatus.toPermissionStatus(): PermissionStatus {
    return when (this) {
        kCLAuthorizationStatusAuthorizedAlways -> PermissionStatus.Granted

        kCLAuthorizationStatusAuthorizedWhenInUse -> {
            // When In Use is sufficient for LOCATION_WHEN_IN_USE and COARSE_LOCATION
            PermissionStatus.Granted
        }

        kCLAuthorizationStatusNotDetermined -> PermissionStatus.NotDetermined

        kCLAuthorizationStatusDenied,
        kCLAuthorizationStatusRestricted,
        -> PermissionStatus.Denied.Permanent

        else -> PermissionStatus.NotDetermined
    }
}
