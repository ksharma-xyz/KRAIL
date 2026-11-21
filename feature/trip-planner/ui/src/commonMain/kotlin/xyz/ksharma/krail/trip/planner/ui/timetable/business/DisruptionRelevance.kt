package xyz.ksharma.krail.trip.planner.ui.timetable.business

import xyz.ksharma.krail.trip.planner.ui.state.alerts.ServiceAlert
import xyz.ksharma.krail.trip.planner.ui.state.disruption.Disruption
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionBannerState
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionReason
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionSeverity
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState.JourneyCardInfo

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
 *    contain a replacement-bus leg (a leg NSW labels temporary bus, not any bus).
 *
 * Alerts name no stops, so (5) cannot tell which stretch the rider uses. The banner therefore
 * prints the alert's own headline so the rider can judge.
 */
@Suppress("LongParameterList")
internal fun disruptionBanner(
    disruptions: List<Disruption>,
    endpointProductClasses: Set<Int>,
    resultProductClasses: Set<Int>,
    hasReplacementBusLeg: Boolean,
    spanStartEpochSec: Long,
    spanEndEpochSec: Long,
): DisruptionBannerState {
    val shown = disruptions
        .asSequence()
        .filter { it.severity == DisruptionSeverity.NOT_RUNNING }
        .filter { it.feed.productClass in endpointProductClasses }
        .filter { it.overlaps(spanStartEpochSec, spanEndEpochSec) }
        .mapNotNull { disruption ->
            disruption.displacementReason(
                resultProductClasses,
                hasReplacementBusLeg,
            )?.let { reason -> disruption to reason }
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

private fun Disruption.displacementReason(
    resultProductClasses: Set<Int>,
    hasReplacementBusLeg: Boolean,
): DisruptionReason? = when {
    feed.productClass !in resultProductClasses -> DisruptionReason.MODE_MISSING
    replacedByBuses && hasReplacementBusLeg -> DisruptionReason.REPLACEMENT_BUS
    else -> null
}

/**
 * Puts the replacement-bus banner over the metro alert banner when a leg in [journeys] is a replacement
 * bus, which is NSW itself labelling a leg a temporary bus. Otherwise the metro banner stands.
 *
 * A metro alert only supplies the expanded detail when it also says buses replace the line. The
 * planner's own line alert is used when there is none. Neither is ever a trigger: a line alert
 * covers a whole route, so it would show for riders far from the closed stretch.
 */
internal fun replacementBusBanner(
    journeys: List<JourneyCardInfo>,
    metroBanner: DisruptionBannerState,
): DisruptionBannerState {
    val replacementJourneys = journeys.filter { it.hasReplacementBusLeg() }
    return if (replacementJourneys.isEmpty()) {
        metroBanner
    } else {
        DisruptionBannerState.ReplacementBuses(
            // Whatever the reason: when every result is a replacement bus the metro is missing from
            // the results (MODE_MISSING), and the alert that says buses replace it is still the detail.
            disruption = (metroBanner as? DisruptionBannerState.Shown)
                ?.disruption
                ?.takeIf { it.replacedByBuses },
            lineAlert = replacementJourneys.firstLineAlert(),
        )
    }
}

internal fun JourneyCardInfo.hasReplacementBusLeg(): Boolean =
    legs.any { it is JourneyCardInfo.Leg.TransportLeg && it.isReplacementBus }

private fun List<JourneyCardInfo>.firstLineAlert(): ServiceAlert? = asSequence()
    .flatMap { it.legs.asSequence() }
    .filterIsInstance<JourneyCardInfo.Leg.TransportLeg>()
    .flatMap { it.serviceAlertList.orEmpty().asSequence() }
    .firstOrNull { it.isLineAlert }
