package xyz.ksharma.krail.trip.planner.ui.timetable.business

import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.disruption.Disruption
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionBannerState
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionReason
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionSeverity

/**
 * Decides whether a disruption deserves a banner above these results.
 *
 * The trip planner routes around a closed line without saying so, so the closed line simply
 * does not appear. A banner is shown only when all of these hold, which keeps it from firing for
 * riders it does not concern:
 *
 * 1. The line is out of service ([DisruptionSeverity.NOT_RUNNING]).
 * 2. Both the origin and the destination are served by that mode. A trip that only touches the
 *    mode at one end (a train from a station that also has a metro) is not a metro trip.
 * 3. The rider has not filtered that mode out themselves.
 * 4. The alert's window overlaps the times of the results on screen.
 * 5. The mode is missing from the results, or the alert says buses replace it and the results
 *    contain a bus leg.
 *
 * Alerts name no stops, so (5) cannot tell which stretch the rider uses. The banner therefore
 * prints the alert's own headline so the rider can judge.
 */
internal fun disruptionBanner(
    disruptions: List<Disruption>,
    endpointProductClasses: Set<Int>,
    resultProductClasses: Set<Int>,
    spanStartEpochSec: Long,
    spanEndEpochSec: Long,
): DisruptionBannerState {
    val shown = disruptions
        .asSequence()
        .filter { it.severity == DisruptionSeverity.NOT_RUNNING }
        .filter { it.feed.productClass in endpointProductClasses }
        .filter { it.overlaps(spanStartEpochSec, spanEndEpochSec) }
        .mapNotNull { disruption ->
            disruption.displacementReason(resultProductClasses)?.let { reason -> disruption to reason }
        }
        .minByOrNull { (disruption, _) -> disruption.startEpochSec }
    return if (shown == null) {
        DisruptionBannerState.None
    } else {
        DisruptionBannerState.Shown(disruption = shown.first, reason = shown.second)
    }
}

private fun Disruption.overlaps(spanStart: Long, spanEnd: Long): Boolean {
    val end = endEpochSec
    return startEpochSec <= spanEnd && (end == null || end >= spanStart)
}

private fun Disruption.displacementReason(resultProductClasses: Set<Int>): DisruptionReason? = when {
    feed.productClass !in resultProductClasses -> DisruptionReason.MODE_MISSING
    replacedByBuses && TransportMode.Bus.productClass in resultProductClasses -> DisruptionReason.REPLACEMENT_BUS
    else -> null
}
