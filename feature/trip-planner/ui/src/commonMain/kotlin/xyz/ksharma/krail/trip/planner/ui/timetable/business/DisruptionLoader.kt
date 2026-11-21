package xyz.ksharma.krail.trip.planner.ui.timetable.business

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.trip.planner.ui.state.disruption.Disruption
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionBannerState
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionFeed
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionRepository
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState.JourneyCardInfo
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Loads service disruptions beside the results screen's own trip call and turns them into the
 * banner. Everything about it is optional: the results never wait for it, a failure leaves the
 * last banner as it was, and a trip that does not use a mode with an alerts feed costs no request.
 *
 * Origin and destination modes come from the bundled stop table, so deciding whether a trip is a
 * metro trip needs no network. [refresh] is called on every results refresh (every 30 seconds
 * while visible); the repository's cache makes nearly all of those free.
 */
@OptIn(ExperimentalTime::class)
internal class DisruptionLoader(
    private val repository: DisruptionRepository?,
    private val sandook: Sandook,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val nowEpochSec: () -> Long,
    /** Called when a feed could not be read, so the screen can report it. Never shown to the rider. */
    private val onUnavailable: (DisruptionFeed) -> Unit = {},
) {
    private var disruptions: List<Disruption> = emptyList()
    private var endpointClasses: Set<Int> = emptySet()
    private var endpointsFor: String? = null
    private var inFlight = false

    /** Calls [onChanged] only when the disruptions actually changed, so an unchanged feed redraws nothing. */
    fun refresh(trip: Trip, onChanged: () -> Unit) {
        val repository = repository ?: return
        if (inFlight) return
        inFlight = true
        scope.launch(dispatcher) {
            try {
                val classes = endpointClassesFor(trip)
                val feeds = DisruptionFeed.entries.filter { it.productClass in classes }
                val loaded = feeds.flatMap { feed ->
                    repository.load(feed)
                        .onFailure {
                            log("[DISRUPTION] ${feed.feedName} unavailable: ${it.message}")
                            onUnavailable(feed)
                        }
                        .getOrNull()
                        .orEmpty()
                }
                if (loaded != disruptions) {
                    disruptions = loaded
                    onChanged()
                }
            } finally {
                inFlight = false
            }
        }
    }

    /**
     * A replacement-bus leg in [journeys] is itself the evidence a line is closed, so it needs no alert
     * and no network call. [journeys] are already filtered by the rider's modes, so hiding the bus mode
     * hides the banner. See [replacementBusBanner].
     */
    fun bannerFor(journeys: List<JourneyCardInfo>, unselectedModes: Set<Int>): DisruptionBannerState {
        val hasReplacement = journeys.any { it.hasReplacementBusLeg() }
        val metroBanner = metroBannerFor(journeys, unselectedModes, hasReplacement)
        return replacementBusBanner(journeys, metroBanner)
    }

    private fun metroBannerFor(
        journeys: List<JourneyCardInfo>,
        unselectedModes: Set<Int>,
        hasReplacementBusLeg: Boolean,
    ): DisruptionBannerState {
        if (disruptions.isEmpty()) return DisruptionBannerState.None
        val now = nowEpochSec()
        val spanStart = journeys.minOfOrNull { it.originUtcDateTime.toEpochSec() } ?: now
        val spanEnd = journeys.maxOfOrNull { it.destinationUtcDateTime.toEpochSec() } ?: (now + NO_RESULTS_SPAN_SEC)
        return disruptionBanner(
            disruptions = disruptions,
            endpointProductClasses = endpointClasses - unselectedModes,
            resultProductClasses = journeys.flatMap { it.transportModeLines }
                .map { it.transportMode.productClass }
                .toSet(),
            hasReplacementBusLeg = hasReplacementBusLeg,
            spanStartEpochSec = spanStart,
            spanEndEpochSec = spanEnd,
        )
    }

    /** Modes that serve both ends. A mode at only one end does not make this a trip on that mode. */
    private fun endpointClassesFor(trip: Trip): Set<Int> {
        if (endpointsFor == trip.tripId) return endpointClasses
        val rows = sandook.selectStopsByIds(listOf(trip.fromStopId, trip.toStopId))
        val perStop = listOf(trip.fromStopId, trip.toStopId).map { id ->
            rows.firstOrNull { it.stopId == id }?.productClasses.orEmpty()
                .split(',')
                .mapNotNull { it.trim().toIntOrNull() }
                .toSet()
        }
        endpointClasses = perStop.reduce { a, b -> a intersect b }
        endpointsFor = trip.tripId
        return endpointClasses
    }

    private fun String.toEpochSec(): Long = Instant.parse(this).epochSeconds

    private companion object {
        /** With no results there is no time span to compare, so look two hours ahead. */
        const val NO_RESULTS_SPAN_SEC = 2 * 60 * 60L
    }
}
