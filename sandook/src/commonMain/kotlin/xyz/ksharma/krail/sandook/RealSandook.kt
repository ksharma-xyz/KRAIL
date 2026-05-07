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

    private val recentSearchStopsQueries = sandook.recentSearchStopsQueries

    private val stopLabelsQueries = sandook.stopLabelsQueries

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

    override fun updateSavedTripSortOrder(tripId: String, sortOrder: Long) {
        query.updateSavedTripSortOrder(sort_order = sortOrder, tripId = tripId)
    }

    override fun clearSavedTrips() {
        query.clearSavedTrips()
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
        isParent: Boolean?,
    ) {
        nswStopsQueries.insertStop(
            stopId = stopId,
            stopName = stopName,
            stopLat = stopLat,
            stopLon = stopLon,
            // Only store when explicitly false (child stop)
            // NULL and true both mean parent stop (default)
            isParent = if (isParent == false) 0L else null,
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

    override fun selectStopsByIds(stopIds: List<String>): List<SelectProductClassesForStop> {
        if (stopIds.isEmpty()) return emptyList()
        // SQLDelight generates a separate type per query even with identical SELECT shapes,
        // so we map back to the canonical SelectProductClassesForStop the rest of the app uses.
        return nswStopsQueries.selectProductClassesForStopsByIds(stopIds).executeAsList().map { row ->
            SelectProductClassesForStop(
                stopId = row.stopId,
                stopName = row.stopName,
                stopLat = row.stopLat,
                stopLon = row.stopLon,
                isParent = row.isParent,
                productClasses = row.productClasses,
            )
        }
    }

    override fun selectStopCoordinatesBatch(stopIds: List<String>): Map<String, Pair<Double, Double>> {
        if (stopIds.isEmpty()) return emptyMap()
        return nswStopsQueries.selectStopCoordinatesBatch(stopIds)
            .executeAsList()
            .associate { row -> row.stopId to (row.stopLat to row.stopLon) }
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

    // region StopLabels
    override fun observeStopLabels(): Flow<List<StopLabels>> {
        return stopLabelsQueries.selectAllStopLabels()
            .asFlow()
            .mapToList(ioDispatcher)
    }

    override fun upsertStopLabel(
        label: String,
        emoji: String,
        stopId: String?,
        stopName: String?,
        sortOrder: Long,
    ) {
        stopLabelsQueries.upsertStopLabel(
            label = label,
            emoji = emoji,
            stop_id = stopId,
            stop_name = stopName,
            sort_order = sortOrder,
        )
    }

    override fun updateStopLabelStop(label: String, stopId: String?, stopName: String?) {
        stopLabelsQueries.updateStopLabelStop(
            stop_id = stopId,
            stop_name = stopName,
            label = label,
        )
    }

    override fun deleteStopLabel(label: String) {
        stopLabelsQueries.deleteStopLabel(label)
    }

    override fun clearStopLabels() {
        stopLabelsQueries.clearStopLabels()
    }
    // endregion
}
