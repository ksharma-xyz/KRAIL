package xyz.ksharma.krail.sandook

/**
 * Interface for NSW Stops database operations.
 * Provides methods to query and manage stops and their product classes.
 */
interface NswStopsSandook {

    // region Query Operations

    /**
     * Get all stops within a radius of a center point.
     *
     * @param centerLat Center latitude
     * @param centerLon Center longitude
     * @param minLat Minimum latitude for bounding box
     * @param maxLat Maximum latitude for bounding box
     * @param minLon Minimum longitude for bounding box
     * @param maxLon Maximum longitude for bounding box
     * @param radiusKm Search radius in kilometers
     * @param filterByModes Whether to filter by transport modes (0 = no filter, 1 = filter)
     * @param productClasses List of product classes to filter by
     * @param maxResults Maximum number of results to return
     * @return List of nearby stops with their details
     */
    @Suppress("LongParameterList")
    fun selectStopsNearby(
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
    ): List<SelectStopsNearby>

    /**
     * Get the total count of stops in the database.
     */
    fun selectStopsCount(): Long

    /**
     * Get the total count of stop product class entries.
     */
    fun selectStopProductClassCount(): Long

    /**
     * Get stops by stop ID or name with their product classes.
     *
     * @param stopId Exact stop ID to match
     * @param stopName Stop name to partially match (case-insensitive)
     * @return List of stops with their product classes
     */
    fun selectProductClassesForStop(
        stopId: String,
        stopName: String,
    ): List<SelectProductClassesForStop>

    // endregion

    // region Insert Operations

    /**
     * Insert a stop into the database.
     *
     * @param stopId Unique stop identifier
     * @param stopName Stop name
     * @param stopLat Stop latitude
     * @param stopLon Stop longitude
     * @param isParent Whether the stop is a parent stop (NULL or 1) or child stop (0)
     */
    fun insertStop(
        stopId: String,
        stopName: String,
        stopLat: Double,
        stopLon: Double,
        isParent: Long?,
    )

    /**
     * Insert a product class for a stop.
     *
     * @param stopId Stop identifier
     * @param productClass Product class/transport mode code
     */
    fun insertStopProductClass(stopId: String, productClass: Long)

    /**
     * Execute operations within a transaction.
     */
    fun <R> transactionWithResult(block: () -> R): R

    // endregion

    // region Maintenance Operations

    /**
     * Clear all stops from the database.
     */
    fun clearNswStopsTable()

    /**
     * Clear all stop product class entries.
     */
    fun clearNswStopProductClassTable()

    // endregion
}

/**
 * Real implementation of [NswStopsSandook].
 * Delegates to the SQLDelight generated queries.
 */
internal class RealNswStopsSandook(
    private val factory: SandookDriverFactory,
) : NswStopsSandook {

    private val sandook = KrailSandook(factory.createDriver())
    private val nswStopsQueries = sandook.nswStopsQueries

    // region Query Operations

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
        return nswStopsQueries.selectStopsNearby(
            centerLat = centerLat,
            centerLon = centerLon,
            minLat = minLat,
            maxLat = maxLat,
            minLon = minLon,
            maxLon = maxLon,
            radiusKm = radiusKm,
            filterByModes = filterByModes,
            productClasses = productClasses,
            maxResults = maxResults,
        ).executeAsList()
    }

    override fun selectStopsCount(): Long {
        return nswStopsQueries.selectStopsCount().executeAsOne()
    }

    override fun selectStopProductClassCount(): Long {
        return nswStopsQueries.selectStopProductClassCount().executeAsOne()
    }

    override fun selectProductClassesForStop(
        stopId: String,
        stopName: String,
    ): List<SelectProductClassesForStop> {
        return nswStopsQueries.selectProductClassesForStop(
            stopId = stopId,
            stopName = stopName,
        ).executeAsList()
    }

    // endregion

    // region Insert Operations

    override fun insertStop(
        stopId: String,
        stopName: String,
        stopLat: Double,
        stopLon: Double,
        isParent: Long?,
    ) {
        nswStopsQueries.insertStop(
            stopId = stopId,
            stopName = stopName,
            stopLat = stopLat,
            stopLon = stopLon,
            isParent = isParent,
        )
    }

    override fun insertStopProductClass(stopId: String, productClass: Long) {
        nswStopsQueries.insertStopProductClass(stopId, productClass)
    }

    override fun <R> transactionWithResult(block: () -> R): R {
        return nswStopsQueries.transactionWithResult { block() }
    }

    // endregion

    // region Maintenance Operations

    override fun clearNswStopsTable() {
        nswStopsQueries.clearNswStopsTable()
    }

    override fun clearNswStopProductClassTable() {
        nswStopsQueries.clearNswStopProductClassTable()
    }

    // endregion
}
