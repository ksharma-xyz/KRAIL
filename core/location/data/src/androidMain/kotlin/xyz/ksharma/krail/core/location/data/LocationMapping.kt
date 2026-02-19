package xyz.ksharma.krail.core.location.data

import android.os.Build
import com.google.android.gms.location.Priority
import xyz.ksharma.krail.core.location.Location
import xyz.ksharma.krail.core.location.LocationError
import xyz.ksharma.krail.core.location.LocationPriority
import android.location.Location as AndroidLocation

/**
 * Convert Android Location to common Location model.
 */
internal fun AndroidLocation.toCommonLocation(): Location {
    return Location(
        latitude = latitude,
        longitude = longitude,
        accuracy = if (hasAccuracy()) accuracy.toDouble() else null,
        altitude = if (hasAltitude()) altitude else null,
        altitudeAccuracy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hasVerticalAccuracy()) {
            verticalAccuracyMeters.toDouble()
        } else {
            null
        },
        speed = if (hasSpeed()) speed.toDouble() else null,
        speedAccuracy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hasSpeedAccuracy()) {
            speedAccuracyMetersPerSecond.toDouble()
        } else {
            null
        },
        bearing = if (hasBearing()) bearing.toDouble() else null,
        bearingAccuracy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hasBearingAccuracy()) {
            bearingAccuracyDegrees.toDouble()
        } else {
            null
        },
        timestamp = time,
    )
}

/**
 * Convert LocationPriority to Android Priority.
 */
internal fun LocationPriority.toAndroidPriority(): Int {
    return when (this) {
        LocationPriority.HIGH_ACCURACY -> Priority.PRIORITY_HIGH_ACCURACY
        LocationPriority.BALANCED_POWER -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
        LocationPriority.LOW_POWER -> Priority.PRIORITY_LOW_POWER
        LocationPriority.PASSIVE -> Priority.PRIORITY_PASSIVE
    }
}

/**
 * Convert exceptions to [LocationError].
 *
 * Uses type-based matching rather than message string inspection.
 */
internal fun Exception.toLocationError(): LocationError = when (this) {
    is SecurityException -> LocationError.PermissionDenied()
    else -> LocationError.Unknown(this)
}
