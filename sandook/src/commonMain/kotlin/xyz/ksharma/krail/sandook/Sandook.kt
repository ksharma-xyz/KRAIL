package xyz.ksharma.krail.sandook

interface Sandook {

    // region Theme
    fun insertOrReplaceTheme(productClass: Long)
    fun getProductClass(): Long?
    fun clearTheme()
    // endregion

    // region SavedTrip
    fun insertOrReplaceTrip(
        tripId: String,
        fromStopId: String,
        fromStopName: String,
        toStopId: String,
        toStopName: String,
    )

    fun deleteTrip(tripId: String)
    fun selectAllTrips(): List<SavedTrip>
    fun selectTripById(tripId: String): SavedTrip?
    fun clearSavedTrips()
    // endregion

    // region Alerts

    fun getAlerts(journeyId: String): List<SelectServiceAlertsByJourneyId>

    fun clearAlerts()

    fun insertAlerts(journeyId: String, alerts: List<SelectServiceAlertsByJourneyId>)

    // endregion

    // region NswStops
    fun insertNswStop(stopId: String, stopName: String, stopLat: Double, stopLon: Double)

    fun insertNswStopProductClass(stopId: String, productClass: Int)

    fun selectStopsByPartialName(stopName: String): List<NswStops>

    /**
     * Select stops by name and product class. This is useful for selecting stops that are of a certain product class.
     * Use with care, because it may also include those stops which are of multiple product classes,
     * that are not included in the include list.
     */
    fun selectStopsByNameAndProductClass(
        stopName: String,
        includeProductClassList: List<Int>,
    ): List<NswStops>

    /**
     * Select stops by name excluding product classes. This is useful for selecting stops that are
     * not of a certain product class.
     */
    fun selectStopsByNameExcludingProductClass(
        stopName: String,
        excludeProductClassList: List<Int> = emptyList(),
    ): List<NswStops>

    /**
     * Retrieves stops by matching an exact stop \id\ or partially matching a stop \name\.
     * Excludes stops having product classes in the given \excludeProductClassList\.
     * \param stopId Exact stop \id\ to match.
     * \param stopName Partial stop \name\ to match.
     * \param excludeProductClassList Product class IDs to exclude.
     * \return List of matching NswStops.
     */
    fun selectStopsByNameExcludingProductClassOrExactId(
        stopName: String,
        excludeProductClassList: List<Int> = emptyList(),
    ): List<NswStops>

    // endregion
}
