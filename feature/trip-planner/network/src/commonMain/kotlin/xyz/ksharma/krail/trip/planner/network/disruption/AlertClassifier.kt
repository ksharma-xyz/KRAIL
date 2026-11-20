package xyz.ksharma.krail.trip.planner.network.disruption

import com.google.transit.realtime.Alert
import xyz.ksharma.krail.trip.planner.ui.state.disruption.Disruption
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionFeed
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionSeverity

/**
 * Keeps the alerts that change whether a rider can travel, and drops the rest.
 *
 * Measured on the live metro feed: its six alerts were four trackwork closures and two
 * "Station Update" facility notices with `UNKNOWN_EFFECT`. Priority cannot separate them, but
 * the effect can. Pure and free of I/O so the same fixtures can run in the BFF.
 */
internal fun classifyAlerts(feed: DisruptionFeed, alerts: List<Alert>): List<Disruption> =
    alerts.mapNotNull { alert -> alert.toDisruption(feed) }

private val SERVICE_EFFECTS = setOf(
    Alert.Effect.NO_SERVICE,
    Alert.Effect.REDUCED_SERVICE,
    Alert.Effect.SIGNIFICANT_DELAYS,
    Alert.Effect.DETOUR,
    Alert.Effect.MODIFIED_SERVICE,
)

// Wording NSW uses when a line is out, checked against the live feed. Lets a real stoppage
// through even if a fault alert arrives without an effect.
private val NOT_RUNNING_WORDING = listOf("do not run", "buses replace", "suspended", "no service", "not running")

// Lift and escalator outages are accessibility notices, not a change to the service.
private val FACILITY_WORDING = listOf("lift", "escalator")

private fun Alert.toDisruption(feed: DisruptionFeed): Disruption? {
    val headline = header_text?.translation?.firstOrNull()?.text?.trim().orEmpty()
    val period = active_period.firstOrNull()
    // An alert with no headline or no start has nothing to show or no window to match a search against.
    val start = period?.start
    val severity = headline.takeIf { it.isNotEmpty() }?.let { severityOf(it.lowercase(), effect) }
    return if (start == null || severity == null) {
        null
    } else {
        Disruption(
            id = "${feed.feedName}:$headline:$start",
            feed = feed,
            severity = severity,
            headline = headline,
            detail = description_text?.translation?.firstOrNull()?.text?.trim().orEmpty(),
            startEpochSec = start,
            endEpochSec = period.end?.takeIf { it > start },
            moreInfoUrl = url?.translation?.firstOrNull()?.text?.takeIf { it.isNotBlank() },
            replacedByBuses = "buses replace" in headline.lowercase(),
        )
    }
}

/** Null means the alert is not a service disruption and is dropped. */
private fun severityOf(loweredHeadline: String, effect: Alert.Effect?): DisruptionSeverity? {
    val wordingSaysNotRunning = NOT_RUNNING_WORDING.any { it in loweredHeadline }
    return when {
        FACILITY_WORDING.any { it in loweredHeadline } -> null
        wordingSaysNotRunning || effect == Alert.Effect.NO_SERVICE -> DisruptionSeverity.NOT_RUNNING
        effect in SERVICE_EFFECTS -> DisruptionSeverity.DELAYS_AND_CHANGES
        else -> null
    }
}
