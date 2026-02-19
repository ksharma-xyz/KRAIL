package xyz.ksharma.krail.core.location

/**
 * Physical constants describing the Earth (WGS84).
 * Used for coordinate validation and geographic distance calculations.
 */
object EarthConstants {
    /** Mean radius of the Earth in meters. Used by the Haversine formula. */
    const val RADIUS_METERS = 6_371_000.0

    /** Valid latitude range. */
    const val MIN_LATITUDE = -90.0
    const val MAX_LATITUDE = 90.0

    /** Valid longitude range. */
    const val MIN_LONGITUDE = -180.0
    const val MAX_LONGITUDE = 180.0
}
