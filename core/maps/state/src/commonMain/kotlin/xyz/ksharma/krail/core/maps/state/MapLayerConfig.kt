package xyz.ksharma.krail.core.maps.state

/**
 * Visual and interaction sizing constants for map circle layers.
 * Shared across all map screens so stop circles and hit targets stay consistent.
 */
object MapLayerConfig {

    /** Radius of the visible stop circle on SearchStopMap. */
    const val NEARBY_STOP_CIRCLE_RADIUS_DP = 8

    /** Radius of the visible stop circle on JourneyMap (regular/intermediate stops). */
    const val JOURNEY_STOP_CIRCLE_RADIUS_DP = 6

    /**
     * Radius of the invisible hit-target layer placed on top of every stop circle.
     * Larger than the visible circle to make small dots easier to tap without
     * changing the visual appearance. Applied identically on all map screens.
     */
    const val STOP_HIT_TARGET_RADIUS_DP = 20
}
