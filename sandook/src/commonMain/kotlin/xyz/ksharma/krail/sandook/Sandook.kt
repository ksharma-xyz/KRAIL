package xyz.ksharma.krail.sandook

import kotlinx.coroutines.flow.Flow

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

    fun observeAllTrips(): Flow<List<SavedTrip>>

    fun selectTripById(tripId: String): SavedTrip?
    fun clearSavedTrips()
    // endregion

    // region Alerts

    fun getAlerts(journeyId: String): List<SelectServiceAlertsByJourneyId>

    fun clearAlerts()

    fun insertAlerts(journeyId: String, alerts: List<SelectServiceAlertsByJourneyId>)

    // endregion

    // region NswStops
    fun insertNswStop(
        stopId: String,
        stopName: String,
        stopLat: Double,
        stopLon: Double,
        isParent: Boolean?,
    )

    fun stopsCount(): Int

    fun productClassCount(): Int

    fun insertNswStopProductClass(stopId: String, productClass: Int)

    /**
     * Inserts a list of stops in a single transaction.
     */
    fun <R> insertTransaction(block: () -> R): R

    fun clearNswStopsTable()

    fun clearNswProductClassTable()

    /**
     * Retrieves stops by matching an exact stop \id\ or partially matching a stop \name\.
     * Excludes stops having product classes in the given \excludeProductClassList\.
     */
    fun selectStops(
        stopName: String,
        excludeProductClassList: List<Int>,
    ): List<SelectProductClassesForStop>
    // endregion


    // region RecentSearchStops
    fun insertOrReplaceRecentSearchStop(stopId: String)

    fun selectRecentSearchStops(): List<SelectRecentSearchStops>

    fun clearRecentSearchStops()

    fun cleanupOrphanedRecentSearchStops()

    fun cleanupOldRecentSearchStops()
    // endregion
}
