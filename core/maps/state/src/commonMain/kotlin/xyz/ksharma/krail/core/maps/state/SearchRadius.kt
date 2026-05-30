package xyz.ksharma.krail.core.maps.state

/**
 * Valid search-radius options for the nearby-stops map query.
 * Use [SearchRadius.entries] to iterate options in the UI and [DEFAULT] for initial state.
 */
enum class SearchRadius(val km: Double) {
    ONE(1.0),
    THREE(3.0),
    FIVE(5.0),
    ;

    val label: String get() = "${km.toInt()}km"

    companion object {
        val DEFAULT: SearchRadius = THREE
    }
}
