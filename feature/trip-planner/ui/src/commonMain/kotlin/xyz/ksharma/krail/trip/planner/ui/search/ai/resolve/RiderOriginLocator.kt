package xyz.ksharma.krail.trip.planner.ui.search.ai.resolve

import xyz.ksharma.dhruva.location.Location
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.data.repository.NearbyStopsRepository
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * Where a journey starts when the rider did not say.
 *
 * "Get me home" names one end. This answers the other, in the order that respects what the
 * rider has actually told the app:
 *
 * 1. **A stop they named**, if they are standing within walking distance of one. A labelled stop
 *    is a place they deliberately pinned, which is far stronger evidence than proximity: the
 *    nearest stop to an office is often a shelter nobody uses, while the label is the platform
 *    they walk to every day.
 * 2. **The nearest stop**, as before.
 * 3. **Nothing**, leaving the field pencil-editable.
 *
 * Gathered into one collaborator because it is one question. It was three constructor
 * parameters on the ViewModel — a location lambda, a nearby repository and a label locator —
 * which is three things to pass around and no name for what they are collectively for.
 */
class RiderOriginLocator(
    private val resolveCurrentLocation: suspend () -> Location? = { null },
    private val labelledStopLocator: LabelledStopLocator? = null,
    private val nearbyStopsRepository: NearbyStopsRepository? = null,
) {

    /**
     * @param excludeStopId the destination. Standing at home and asking for home must not
     * resolve Home to Home, which is a journey of no distance and reads as the app not having
     * understood at all.
     */
    suspend fun originStop(excludeStopId: String?): StopItem? {
        val location = resolveCurrentLocation()
        return if (location == null) {
            homeAsALastGuess(excludeStopId)
        } else {
            whereTheRiderIsStanding(location = location, excludeStopId = excludeStopId)
        }
    }

    /**
     * No location at all: denied, restricted, or the fix timed out. All three arrive as the same
     * null and all three mean the same thing here, which is that nothing is known about where
     * the rider is right now.
     *
     * Home is a guess about habit rather than a fact about now, which is exactly why it sits
     * below location and never overrides it: a rider at work asking for the airport is served
     * correctly by where they are and wrongly by where they usually start. With no location
     * there is nothing else to go on, the destination is already understood, and so filling
     * this in answers a question the rider asked rather than inventing one. One tap changes it.
     */
    private suspend fun homeAsALastGuess(excludeStopId: String?): StopItem? =
        labelledStopLocator?.homeStop(excludeStopId).also { home ->
            log(
                "$ORIGIN_TAG no location; " +
                    if (home == null) "no Home set, origin left blank" else "using Home",
            )
        }

    /**
     * Location is known, so the guess is never reached from here. Two of these outcomes are
     * deliberately blank: both are things the app knows about the rider right now, and Home
     * would contradict them.
     */
    private suspend fun whereTheRiderIsStanding(
        location: Location,
        excludeStopId: String?,
    ): StopItem? = when {
        // Already there. "To home by 9pm" while standing at home used to fill the origin with
        // the next stop along the road, which is a journey from one bus stop to its neighbour.
        standingAt(location, excludeStopId) ->
            null.also { log("$ORIGIN_TAG standing at the destination, origin left blank") }

        else -> {
            val labelled = labelledStopLocator?.nearestLabelledStop(
                latitude = location.latitude,
                longitude = location.longitude,
                excludeStopId = excludeStopId,
            )
            val stop = labelled ?: nearestStop(location, excludeStopId)
            log(
                "$ORIGIN_TAG " + when {
                    labelled != null -> "using a labelled stop within walking distance"
                    stop != null -> "using the nearest stop"
                    else -> "no stop near the rider, origin left blank"
                },
            )
            stop
        }
    }

    private suspend fun standingAt(location: Location, excludeStopId: String?): Boolean =
        excludeStopId != null && labelledStopLocator?.isStandingAt(
            stopId = excludeStopId,
            latitude = location.latitude,
            longitude = location.longitude,
        ) == true

    private suspend fun nearestStop(location: Location, excludeStopId: String?): StopItem? =
        nearbyStopsRepository?.getStopsNearby(
            centerLat = location.latitude,
            centerLon = location.longitude,
            radiusKm = NEARBY_STOP_RADIUS_KM,
            // Two, so the destination can be skipped without a second query when the rider is
            // standing on top of the stop they asked to travel to.
            maxResults = 2,
        )
            ?.firstOrNull { it.stopId != excludeStopId }
            ?.let { StopItem(stopName = it.stopName, stopId = it.stopId) }

    private companion object {
        const val NEARBY_STOP_RADIUS_KM = 1.0
    }
}

// One greppable tag for every way a journey's start is decided, because every one of them ends
// as either a filled field or a blank one and the rider cannot tell which happened or why.
private const val ORIGIN_TAG = "[AI_ORIGIN]"
