package xyz.ksharma.krail.io.gtfs.nswbusroutes

import xyz.ksharma.krail.sandook.NswBusRoutesSandook
import xyz.ksharma.krail.sandook.db.NswBusRouteVariants
import xyz.ksharma.krail.sandook.db.NswBusTripOptions
import xyz.ksharma.krail.sandook.db.SelectStopsByTripId

/**
 * Keeps only the route group names, which is all the manager reads back. Like the real driver, a
 * transaction whose block throws leaves the groups as they were before it. Kept beside the one
 * test that uses it; promote it to `:core:testing` when a second module needs it.
 */
internal class FakeNswBusRoutesSandook : NswBusRoutesSandook {

    private var groups: List<String> = emptyList()

    /** Makes [insertBusRouteGroup] throw for this group, to fail an insert part way through. */
    var failInsertGroup: String? = null

    fun groupNames(): List<String> = groups

    override fun busRouteGroupsCount(): Int = groups.size

    override fun insertBusRouteGroup(routeShortName: String) {
        check(routeShortName != failInsertGroup) { "insert of route $routeShortName failed" }
        groups = groups + routeShortName
    }

    override fun clearNswBusRoutesData() {
        groups = emptyList()
    }

    override fun insertTransaction(body: () -> Unit) {
        val before = groups
        var committed = false
        try {
            body()
            committed = true
        } finally {
            if (!committed) groups = before
        }
    }

    override fun selectRouteByShortName(routeShortName: String): String? =
        groups.firstOrNull { it == routeShortName }

    override fun selectRouteVariantsByShortName(routeShortName: String): List<NswBusRouteVariants> = emptyList()
    override fun selectTripsByRouteId(routeId: String): List<NswBusTripOptions> = emptyList()
    override fun selectTripsByRouteIds(routeIds: List<String>): List<NswBusTripOptions> = emptyList()
    override fun selectStopsByTripId(tripId: String): List<SelectStopsByTripId> = emptyList()
    override fun insertBusRouteVariant(routeId: String, routeShortName: String, routeName: String) = Unit
    override fun insertBusTripOption(tripId: String, routeId: String, headsign: String) = Unit
    override fun insertBusTripStop(tripId: String, stopId: String, stopSequence: Int) = Unit
}
