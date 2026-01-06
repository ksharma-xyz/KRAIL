package xyz.ksharma.krail.sandook

/**
 * Real implementation of [NswBusRoutesSandook].
 * Delegates to the SQLDelight generated queries.
 */
@Suppress("TooManyFunctions")
class RealNswBusRoutesSandook(
    private val factory: SandookDriverFactory,
) : NswBusRoutesSandook {

    private val sandook = KrailSandook(factory.createDriver())
    private val nswBusRoutesQueries = sandook.nswBusRoutesQueries

    // region Query Operations
    override fun selectRouteByShortName(routeShortName: String): String? {
        return nswBusRoutesQueries.selectRouteByShortName(routeShortName)
            .executeAsOneOrNull()
    }

    override fun selectRouteVariantsByShortName(routeShortName: String): List<NswBusRouteVariants> {
        return nswBusRoutesQueries.selectRouteVariantsByShortName(routeShortName).executeAsList()
    }

    override fun selectTripsByRouteId(routeId: String): List<NswBusTripOptions> {
        return nswBusRoutesQueries.selectTripsByRouteId(routeId).executeAsList()
    }

    override fun selectTripsByRouteIds(routeIds: List<String>): List<NswBusTripOptions> {
        return nswBusRoutesQueries.selectTripsByRouteIds(routeIds).executeAsList()
    }

    override fun selectStopsByTripId(tripId: String): List<SelectStopsByTripId> {
        return nswBusRoutesQueries.selectStopsByTripId(tripId).executeAsList()
    }

    override fun busRouteGroupsCount(): Int {
        return nswBusRoutesQueries.selectRouteGroupsCount().executeAsOne().toInt()
    }
    // endregion

    // region Insert Operations
    override fun insertBusRouteGroup(routeShortName: String) {
        nswBusRoutesQueries.insertRouteGroup(routeShortName)
    }

    override fun insertBusRouteVariant(routeId: String, routeShortName: String, routeName: String) {
        nswBusRoutesQueries.insertRouteVariant(routeId, routeShortName, routeName)
    }

    override fun insertBusTripOption(tripId: String, routeId: String, headsign: String) {
        nswBusRoutesQueries.insertTripOption(tripId, routeId, headsign)
    }

    override fun insertBusTripStop(tripId: String, stopId: String, stopSequence: Int) {
        nswBusRoutesQueries.insertTripStop(tripId, stopId, stopSequence.toLong())
    }
    // endregion

    // region Maintenance Operations
    override fun clearNswBusRoutesData() {
        nswBusRoutesQueries.clearNswBusTripStopsTable()
        nswBusRoutesQueries.clearNswBusTripOptionsTable()
        nswBusRoutesQueries.clearNswBusRouteVariantsTable()
        nswBusRoutesQueries.clearNswBusRouteGroupsTable()
    }

    override fun insertTransaction(body: () -> Unit) {
        nswBusRoutesQueries.transaction {
            body()
        }
    }
    // endregion
}
