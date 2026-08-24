package xyz.ksharma.krail.trip.planner.ui.search.ai.resolve

import kotlinx.coroutines.flow.first
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.math.sqrt

/**
 * The rider's own stop, when they are standing near one.
 *
 * A rider who says "get me home" while at work should leave from Work, not from whichever bus
 * stop happens to be eighty metres away. A labelled stop is a place they deliberately named,
 * which is far stronger evidence about where a journey starts than proximity alone: the nearest
 * stop to an office is often a shelter nobody uses, while the label is the platform they
 * actually walk to.
 *
 * Falls back to nothing rather than guessing. Outside the radius, or with no labels pinned, the
 * caller keeps its existing nearest-stop behaviour.
 *
 * ## Cost
 *
 * One indexed read of at most a handful of rows (a rider has single-digit labels), then about
 * ten float operations each. The GPS fix it needs was already being fetched for the
 * nearest-stop path, so this adds no location work at all.
 */
class LabelledStopLocator(
    private val sandook: Sandook,
) {

    /**
     * The stop pinned as Home, or null when the rider has not set one.
     *
     * Deliberately not distance-aware, because it is asked only when there is no location to
     * measure against: it is the last thing tried before leaving the field blank. Home is the
     * one label this app treats as permanent (it cannot be renamed, only reassigned or
     * cleared), which is what makes "is Home set" a question worth asking at all.
     *
     * [excludeStopId] is the destination, so "home to home" cannot be produced here either.
     */
    suspend fun homeStop(excludeStopId: String?): StopItem? =
        sandook.observeStopLabels().first()
            .firstOrNull { it.label.equals(HOME_STOP_LABEL, ignoreCase = true) }
            ?.let { label ->
                val id = label.stop_id ?: return@let null
                val name = label.stop_name ?: return@let null
                if (id == excludeStopId) null else StopItem(stopId = id, stopName = name)
            }

    /**
     * The nearest labelled stop within [RADIUS_KM], or null.
     *
     * [excludeStopId] is the destination. Standing at home and saying "get me home" must not
     * resolve to Home → Home, which is a journey of no distance and reads as the app not having
     * understood at all.
     */
    suspend fun nearestLabelledStop(
        latitude: Double,
        longitude: Double,
        excludeStopId: String?,
    ): StopItem? {
        val labelled = sandook.observeStopLabels().first()
            .mapNotNull { label ->
                val id = label.stop_id ?: return@mapNotNull null
                val name = label.stop_name ?: return@mapNotNull null
                if (id == excludeStopId) null else StopItem(stopId = id, stopName = name)
            }
        if (labelled.isEmpty()) return null

        val coordinates = sandook.selectStopCoordinatesBatch(labelled.map { it.stopId })

        return labelled
            .mapNotNull { stop ->
                val (stopLat, stopLon) = coordinates[stop.stopId] ?: return@mapNotNull null
                stop to distanceKm(latitude, longitude, stopLat, stopLon)
            }
            .filter { (_, distance) -> distance <= RADIUS_KM }
            .minByOrNull { (_, distance) -> distance }
            ?.first
    }

    /**
     * Whether the rider is already standing at [stopId].
     *
     * Asked about the DESTINATION. Someone at home saying "to home by 9pm" is already there,
     * and the honest answer is no journey at all — filling the origin with the next stop along
     * the road produces a trip from one bus stop to its neighbour, which is not a trip.
     */
    suspend fun isStandingAt(stopId: String, latitude: Double, longitude: Double): Boolean {
        val coordinates = sandook.selectStopCoordinatesBatch(listOf(stopId))[stopId] ?: return false
        return distanceKm(latitude, longitude, coordinates.first, coordinates.second) <= RADIUS_KM
    }

    private companion object {

        /**
         * Walking distance, roughly. Wide enough to cover a large campus or a station precinct
         * where the label sits on one entrance and the rider is at another; narrow enough that
         * it cannot reach the next suburb's labelled stop.
         */
        const val RADIUS_KM = 1.0

        // Equirectangular approximation, the same one NswStops.sq uses for its nearby query,
        // with the constants documented there: one degree of latitude is about 111km, and one
        // degree of longitude about 92km at Sydney's latitude. Exact enough by orders of
        // magnitude for a one kilometre threshold, and no trigonometry per candidate.
        const val KM_PER_DEGREE_LATITUDE = 111.0
        const val KM_PER_DEGREE_LONGITUDE_SYDNEY = 92.0

        fun distanceKm(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double): Double {
            val north = (toLat - fromLat) * KM_PER_DEGREE_LATITUDE
            val east = (toLon - fromLon) * KM_PER_DEGREE_LONGITUDE_SYDNEY
            return sqrt(north * north + east * east)
        }
    }
}
