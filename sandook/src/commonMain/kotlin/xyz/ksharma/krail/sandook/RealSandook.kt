package xyz.ksharma.krail.sandook

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.sandook.db.KrailSandook
import xyz.ksharma.krail.sandook.db.RecentSearchLocations
import xyz.ksharma.krail.sandook.db.SavedTrip
import xyz.ksharma.krail.sandook.db.SelectProductClassesForStop
import xyz.ksharma.krail.sandook.db.SelectServiceAlertsByJourneyId
import xyz.ksharma.krail.sandook.db.StopLabels

internal class RealSandook(
    sandook: KrailSandook,
    private val ioDispatcher: CoroutineDispatcher,
) : Sandook {

    private val query = sandook.krailSandookQueries

    private val nswStopsQueries = sandook.nswStopsQueries

    private val recentSearchLocationsQueries = sandook.recentSearchLocationsQueries

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
        // This boundary owns saved-trip identity. Keep it aligned with Trip.tripId and the
        // SavedTrip CHECK constraint instead of trusting a caller-provided serialization.
        val canonicalTripId = "$fromStopId->$toStopId"
        if (tripId != canonicalTripId) {
            // Temporary migration diagnostic; safe to remove after legacy-ID callers have
            // aged out. Keep the canonicalization above as a permanent storage boundary.
            log("Normalizing saved trip ID from $tripId to $canonicalTripId")
        }
        query.insertOrReplaceTrip(
            canonicalTripId,
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

    // region RecentSearchLocations
    override fun upsertRecentSearchLocation(location: RecentSearchLocation) {
        recentSearchLocationsQueries.upsertRecentSearchLocation(
            locationId = location.locationId,
            displayName = location.displayName,
            kind = location.kind,
            addressType = location.addressType,
            productClasses = location.productClasses,
        )
        // Automatically cleanup old entries to maintain max 5 items
        recentSearchLocationsQueries.cleanupOldRecentSearchLocations()
    }

    override fun selectRecentSearchLocations(): List<RecentSearchLocations> {
        return recentSearchLocationsQueries.selectRecentSearchLocations().executeAsList()
    }

    override fun clearRecentSearchLocations() {
        recentSearchLocationsQueries.clearRecentSearchLocations()
    }

    override fun cleanupOldRecentSearchLocations() {
        recentSearchLocationsQueries.cleanupOldRecentSearchLocations()
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

    override fun renameStopLabel(label: String, newLabel: String) {
        stopLabelsQueries.renameStopLabel(label = newLabel, label_ = label)
    }

    override fun deleteStopLabel(label: String) {
        stopLabelsQueries.deleteStopLabel(label)
    }

    override fun clearStopLabels() {
        stopLabelsQueries.clearStopLabels()
    }
    // endregion
}
