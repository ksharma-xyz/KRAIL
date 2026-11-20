package xyz.ksharma.krail.trip.planner.ui.state.disruption

/**
 * Modes that publish a dedicated service-alerts feed and can be matched to a rider's trip.
 *
 * Only metro is read today. Metro is one line, so an alert on the mode is an alert on the line.
 * Train alerts name routes (`T6_1a`, `BNK_1a`) that the trip planner does not use, so a train
 * banner needs a route to line mapping first; without it every train rider would see every
 * train alert. See `docs/features/service-disruptions/ERD.md`.
 */
enum class DisruptionFeed(val feedName: String, val productClass: Int) {
    METRO(feedName = "metro", productClass = 2),
}

enum class DisruptionSeverity {
    /** The line is not running, or buses replace it, for part of the network. */
    NOT_RUNNING,

    /** Reduced, changed or delayed service. Kept for later; the banner does not show it yet. */
    DELAYS_AND_CHANGES,
}

/**
 * A service-impacting alert, already separated from standing notices and facility notices.
 * Times are epoch seconds so the type has no date-library dependency.
 */
data class Disruption(
    val id: String,
    val feed: DisruptionFeed,
    val severity: DisruptionSeverity,
    val headline: String,
    val detail: String,
    val startEpochSec: Long,
    /** Null when the alert has no end, which NSW uses for faults with no estimate. */
    val endEpochSec: Long?,
    val moreInfoUrl: String?,
    /** The headline says buses replace the service, so the planner returns bus legs for it. */
    val replacedByBuses: Boolean,
)

sealed interface DisruptionBannerState {
    data object None : DisruptionBannerState

    data class Shown(val disruption: Disruption) : DisruptionBannerState
}
