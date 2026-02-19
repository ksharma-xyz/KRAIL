package xyz.ksharma.krail.core.maps.state

/**
 * Configuration constants for user location tracking and camera behavior on the map.
 */
object UserLocationConfig {
    /** How often to request GPS updates from the device. */
    const val UPDATE_INTERVAL_MS = 2_000L

    /** Zoom level used when auto-centering on the user's first location fix. */
    const val AUTO_CENTER_ZOOM = 15.0

    /** Zoom level used when the user taps the re-center button. */
    const val RECENTER_ZOOM = 16.0

    /** Camera animation duration (ms) for the initial auto-center. */
    const val AUTO_CENTER_ANIMATION_MS = 1_500L

    /** Camera animation duration (ms) for manual re-center taps. */
    const val RECENTER_ANIMATION_MS = 1_000L
}
