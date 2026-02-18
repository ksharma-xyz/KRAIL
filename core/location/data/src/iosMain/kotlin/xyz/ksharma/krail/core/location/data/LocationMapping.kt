package xyz.ksharma.krail.core.location.data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.*
import platform.Foundation.timeIntervalSince1970
import xyz.ksharma.krail.core.location.Location
import xyz.ksharma.krail.core.location.LocationPriority

/**
 * Convert iOS CLLocation to common Location model.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun CLLocation.toCommonLocation(): Location {
    return Location(
        latitude = coordinate.useContents { latitude },
        longitude = coordinate.useContents { longitude },
        accuracy = if (horizontalAccuracy >= 0) horizontalAccuracy else null,
        altitude = altitude,
        altitudeAccuracy = if (verticalAccuracy >= 0) verticalAccuracy else null,
        speed = if (speed >= 0) speed else null,
        speedAccuracy = if (speedAccuracy >= 0) speedAccuracy else null,
        bearing = if (course >= 0) course else null,
        bearingAccuracy = if (courseAccuracy >= 0) courseAccuracy else null,
        timestamp = (timestamp.timeIntervalSince1970 * 1000).toLong()
    )
}

/**
 * Convert LocationPriority to iOS CLLocationAccuracy.
 */
internal fun LocationPriority.toiOSAccuracy(): CLLocationAccuracy {
    return when (this) {
        LocationPriority.HIGH_ACCURACY -> kCLLocationAccuracyBest
        LocationPriority.BALANCED_POWER -> kCLLocationAccuracyHundredMeters
        LocationPriority.LOW_POWER -> kCLLocationAccuracyKilometer
        LocationPriority.PASSIVE -> kCLLocationAccuracyThreeKilometers
    }
}

