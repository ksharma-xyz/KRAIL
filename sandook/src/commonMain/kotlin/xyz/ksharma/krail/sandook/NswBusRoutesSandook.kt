package xyz.ksharma.krail.sandook

/**
 * Interface for NSW Bus Routes database operations.
 * Standalone interface similar to DiscoverCardSeenPreferences.
 */
@Suppress("ComplexInterface", "TooManyFunctions")
interface NswBusRoutesSandook {

    // region Query Operations
    /**
     * Checks if a route exists by exact routeShortName match.
     * Returns the routeShortName if found, null otherwise.
     */
    fun selectRouteByShortName(routeShortName: String): String?

    /**
     * Retrieves all variants for a given route short name.
     */
    fun selectRouteVariantsByShortName(routeShortName: String): List<NswBusRouteVariants>

    /**
     * Retrieves all trips for a given route variant.
     */
    fun selectTripsByRouteId(routeId: String): List<NswBusTripOptions>

    /**
     * Retrieves all stops for a given trip with stop details.
     */
    fun selectStopsByTripId(tripId: String): List<SelectStopsByTripId>

    /**
     * Returns count of bus route groups
     */
    fun busRouteGroupsCount(): Int
    // endregion

    // region Insert Operations
    /**
     * Inserts a bus route group (e.g., route short name "702")
     */
    fun insertBusRouteGroup(routeShortName: String)

    /**
     * Inserts a bus route variant (e.g., specific route like "2504_702")
     */
    fun insertBusRouteVariant(routeId: String, routeShortName: String, routeName: String)

    /**
     * Inserts a trip option for a route variant
     */
    fun insertBusTripOption(tripId: String, routeId: String, headsign: String)

    /**
     * Inserts a stop for a trip with its sequence number
     */
    fun insertBusTripStop(tripId: String, stopId: String, stopSequence: Int)
    // endregion

    // region Maintenance Operations
    /**
     * Clears all NSW Bus Routes data
     */
    fun clearNswBusRoutesData()

    /**
     * Execute multiple database operations in a single transaction for better performance
     */
    fun insertTransaction(body: () -> Unit)
    // endregion
}
