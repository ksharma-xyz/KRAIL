package xyz.ksharma.krail.core.maps.state

import kotlin.math.roundToInt

/**
 * Extension to format distance for display.
 * < 100m: show in meters (e.g., "50m")
 * >= 100m: show in km (e.g., "0.3km")
 */
@Suppress("MagicNumber")
fun Double.formatDistance(): String = when {
    this < 0.1 -> "${(this * 1000).roundToInt()}m"
    else -> {
        val km = (this * 10).roundToInt() / 10.0
        "${km}km"
    }
}
