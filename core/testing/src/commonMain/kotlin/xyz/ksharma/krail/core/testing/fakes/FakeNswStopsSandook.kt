package xyz.ksharma.krail.core.testing.fakes

import xyz.ksharma.krail.sandook.NswStopsSandook
import xyz.ksharma.krail.sandook.db.SelectProductClassesForStop
import xyz.ksharma.krail.sandook.db.SelectStopsNearby

/**
 * The stops table's nearby query as a test double: it returns the rows a test gives it in [nearbyRows], exactly as
 * given, and records the arguments of every call in [nearbyCalls]. The ordering by distance and the radius belong to
 * the SQL query, which has its own tests, so nothing here works them out and a test that needs a particular order
 * gives the rows in that order.
 *
 * The other operations are the empty table: nothing is found, and the writes change nothing.
 */
class FakeNswStopsSandook(
    var nearbyRows: List<SelectStopsNearby> = emptyList(),
) : NswStopsSandook {

    /** What one call to [selectStopsNearby] asked for. */
    data class NearbyCall(
        val centerLat: Double,
        val centerLon: Double,
        val minLat: Double,
        val maxLat: Double,
        val minLon: Double,
        val maxLon: Double,
        val radiusKm: Double,
        val filterByModes: Long,
        val productClasses: List<Long>,
        val maxResults: Long,
    )

    private val _nearbyCalls = mutableListOf<NearbyCall>()
    val nearbyCalls: List<NearbyCall> get() = _nearbyCalls

    /** Set to make the query fail the way a database that cannot be read does. */
    var nearbyFailure: Throwable? = null

    override fun selectStopsNearby(
        centerLat: Double,
        centerLon: Double,
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        radiusKm: Double,
        filterByModes: Long,
        productClasses: List<Long>,
        maxResults: Long,
    ): List<SelectStopsNearby> {
        _nearbyCalls += NearbyCall(
            centerLat, centerLon, minLat, maxLat, minLon, maxLon, radiusKm, filterByModes, productClasses, maxResults,
        )
        nearbyFailure?.let { throw it }
        return nearbyRows
    }

    override fun selectStopsCount(): Long = nearbyRows.size.toLong()

    override fun selectStopProductClassCount(): Long = 0L

    override fun selectProductClassesForStop(
        stopId: String,
        stopName: String,
    ): List<SelectProductClassesForStop> = emptyList()

    override fun insertStop(stopId: String, stopName: String, stopLat: Double, stopLon: Double, isParent: Long?) = Unit

    override fun insertStopProductClass(stopId: String, productClass: Long) = Unit

    override fun <R> transactionWithResult(block: () -> R): R = block()

    override fun clearNswStopsTable() = Unit

    override fun clearNswStopProductClassTable() = Unit
}
