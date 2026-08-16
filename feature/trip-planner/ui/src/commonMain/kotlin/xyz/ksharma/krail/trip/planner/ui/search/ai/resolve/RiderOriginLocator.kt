package xyz.ksharma.krail.trip.planner.ui.search.ai.resolve

import xyz.ksharma.dhruva.location.Location
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
        val location = resolveCurrentLocation() ?: return null

        // Already there. "To home by 9pm" while standing at home used to fill the origin with
        // the next stop along the road, which is a journey from one bus stop to its neighbour.
        // Leaving it blank says nothing untrue and leaves the field editable.
        val alreadyThere = excludeStopId != null && labelledStopLocator?.isStandingAt(
            stopId = excludeStopId,
            latitude = location.latitude,
            longitude = location.longitude,
        ) == true
        if (alreadyThere) return null

        return labelledStopLocator?.nearestLabelledStop(
            latitude = location.latitude,
            longitude = location.longitude,
            excludeStopId = excludeStopId,
        ) ?: nearestStop(location, excludeStopId)
    }

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
