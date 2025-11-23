package xyz.ksharma.krail.sandook

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import xyz.ksharma.krail.core.log.log

internal class RealSandook(
    factory: SandookDriverFactory,
    private val ioDispatcher: CoroutineDispatcher,
) : Sandook {

    private val sandook = KrailSandook(factory.createDriver())
    private val query = sandook.krailSandookQueries

    private val nswStopsQueries = sandook.nswStopsQueries

    private val nswParkRideQueries = sandook.nswParkRideQueries

    private val recentSearchStopsQueries = sandook.recentSearchStopsQueries

    // region Theme
    override fun insertOrReplaceTheme(productClass: Long) {
        query.insertOrReplaceProductClass(productClass)
    }

    override fun getProductClass(): Long? {
        return query.selectProductClass().executeAsOneOrNull()
    }

    override fun clearTheme() {
        query.clearTheme()
    }

    // endregion

    // region SavedTrip
    override fun insertOrReplaceTrip(
        tripId: String,
        fromStopId: String,
        fromStopName: String,
        toStopId: String,
        toStopName: String,
    ) {
        query.insertOrReplaceTrip(
            tripId,
            fromStopId,
            fromStopName,
            toStopId,
            toStopName,
        )
    }

    override fun deleteTrip(tripId: String) {
        query.deleteTrip(tripId)
    }

    override fun selectAllTrips(): List<SavedTrip> {
        return query.selectAllTrips().executeAsList()
    }

    override fun observeAllTrips(): Flow<List<SavedTrip>> {
        return query.selectAllTrips()
            .asFlow()
            .mapToList(ioDispatcher)
    }

    override fun selectTripById(tripId: String): SavedTrip? {
        return query.selectTripById(tripId).executeAsOneOrNull()
    }

    override fun clearSavedTrips() {
        query.clearSavedTrips()
    }

    override fun updateStopValidity(
        tripId: String,
        isFromStopValid: Boolean,
        isToStopValid: Boolean,
    ) {
        query.updateStopValidity(
            isFromStopValid = if (isFromStopValid) 1L else 0L,
            isToStopValid = if (isToStopValid) 1L else 0L,
            tripId = tripId,
        )
    }

    override fun checkStopExists(stopId: String): Boolean {
        return nswStopsQueries.checkStopExists(stopId).executeAsOne() > 0
    }

    // endregion

    // region Alerts

    override fun getAlerts(journeyId: String): List<SelectServiceAlertsByJourneyId> {
        val alerts = query.selectServiceAlertsByJourneyId(journeyId).executeAsList()
        log("Alerts: $alerts")
        return alerts
    }

    override fun clearAlerts() {
        query.clearAllServiceAlerts()
    }

    override fun insertAlerts(journeyId: String, alerts: List<SelectServiceAlertsByJourneyId>) {
        alerts.forEach {
            query.insertServiceAlert(
                journeyId = journeyId,
                heading = it.heading,
                message = it.message,
            )
        }
    }

    // endregion

    // region NswStops

    override fun insertNswStop(
        stopId: String,
        stopName: String,
        stopLat: Double,
        stopLon: Double,
    ) {
        nswStopsQueries.insertStop(
            stopId = stopId,
            stopName = stopName,
            stopLat = stopLat,
            stopLon = stopLon,
        )
    }

    override fun stopsCount(): Int {
        return nswStopsQueries.selectStopsCount().executeAsOne().toInt()
    }

    override fun productClassCount(): Int {
        return nswStopsQueries.selectStopProductClassCount().executeAsOne().toInt()
    }

    override fun insertNswStopProductClass(stopId: String, productClass: Int) {
        nswStopsQueries.insertStopProductClass(stopId, productClass.toLong())
    }

    override fun <R> insertTransaction(block: () -> R): R {
        return nswStopsQueries.transactionWithResult { block() }
    }

    override fun clearNswStopsTable() {
        nswStopsQueries.clearNswStopsTable()
    }

    override fun clearNswProductClassTable() {
        nswStopsQueries.clearNswStopProductClassTable()
    }

    override fun selectStops(
        stopName: String,
        excludeProductClassList: List<Int>,
    ): List<SelectProductClassesForStop> {
        return nswStopsQueries.selectProductClassesForStop(
            stopId = stopName,
            stopName = stopName,
        ).executeAsList()
    }

    // endregion NswStops

    // region RecentSearchStops
    override fun insertOrReplaceRecentSearchStop(stopId: String) {
        recentSearchStopsQueries.insertOrReplaceRecentSearchStop(stopId)
        // Automatically cleanup old entries to maintain max 5 items
        recentSearchStopsQueries.cleanupOldRecentSearchStops()
    }

    override fun selectRecentSearchStops(): List<SelectRecentSearchStops> {
        return recentSearchStopsQueries.selectRecentSearchStops().executeAsList()
    }

    override fun clearRecentSearchStops() {
        recentSearchStopsQueries.clearRecentSearchStops()
    }

    override fun cleanupOrphanedRecentSearchStops() {
        recentSearchStopsQueries.cleanupOrphanedRecentSearchStops()
    }

    override fun cleanupOldRecentSearchStops() {
        recentSearchStopsQueries.cleanupOldRecentSearchStops()
    }
    // endregion
}
