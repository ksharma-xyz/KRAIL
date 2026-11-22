package xyz.ksharma.krail.core.testing.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import xyz.ksharma.krail.core.testing.helpers.CallLog
import xyz.ksharma.krail.sandook.RecentSearchLocation
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.db.NswStops
import xyz.ksharma.krail.sandook.db.RecentSearchLocations
import xyz.ksharma.krail.sandook.db.SavedTrip
import xyz.ksharma.krail.sandook.db.SelectProductClassesForStop
import xyz.ksharma.krail.sandook.db.SelectServiceAlertsByJourneyId
import xyz.ksharma.krail.sandook.db.StopLabels

// Intrinsic to faking the production Sandook interface (33 methods). Not refactorable.
@Suppress("TooManyFunctions")
class FakeSandook(private val callLog: CallLog? = null) : Sandook {

    private var productClass: Long? = null
    private val tripsFlow = MutableStateFlow<List<SavedTrip>>(emptyList())
    private val alerts = mutableMapOf<String, List<SelectServiceAlertsByJourneyId>>()
    private val stops = mutableListOf<NswStops>()
    private val stopProductClasses = mutableMapOf<String, MutableList<Int>>()
    private val recentSearchLocations = mutableListOf<RecentSearchLocations>()
    private val stopLabelsFlow = MutableStateFlow<List<StopLabels>>(emptyList())

    /** Makes [insertNswStop] throw for this stop id, to fail an insert part way through. */
    var failInsertNswStopId: String? = null

    /** Makes [stopsCount] throw, as a database that cannot be read would. */
    var failStopsCount = false

    // region StopLabels
    override fun observeStopLabels(): Flow<List<StopLabels>> = stopLabelsFlow.asStateFlow()

    override fun upsertStopLabel(
        label: String,
        emoji: String,
        stopId: String?,
        stopName: String?,
        sortOrder: Long,
    ) {
        val updated = stopLabelsFlow.value.toMutableList()
        updated.removeAll { it.label == label }
        updated.add(
            StopLabels(
                label = label,
                emoji = emoji,
                stop_id = stopId,
                stop_name = stopName,
                sort_order = sortOrder,
            ),
        )
        stopLabelsFlow.value = updated.sortedWith(
            compareBy({ it.stop_id == null }, { it.sort_order }, { it.label }),
        )
    }

    override fun updateStopLabelStop(label: String, stopId: String?, stopName: String?) {
        val updated = stopLabelsFlow.value.map { row ->
            if (row.label == label) row.copy(stop_id = stopId, stop_name = stopName) else row
        }
        stopLabelsFlow.value = updated.sortedWith(
            compareBy({ it.stop_id == null }, { it.sort_order }, { it.label }),
        )
    }

    override fun renameStopLabel(label: String, newLabel: String) {
        val updated = stopLabelsFlow.value.map { row ->
            if (row.label == label) row.copy(label = newLabel) else row
        }
        stopLabelsFlow.value = updated.sortedWith(
            compareBy({ it.stop_id == null }, { it.sort_order }, { it.label }),
        )
    }

    override fun deleteStopLabel(label: String) {
        stopLabelsFlow.value = stopLabelsFlow.value.filterNot { it.label == label }
    }

    override fun clearStopLabels() {
        stopLabelsFlow.value = emptyList()
    }
    // endregion

    // region Theme
    override fun insertOrReplaceTheme(productClass: Long) {
        this.productClass = productClass
    }

    override fun getProductClass(): Long? {
        return productClass
    }

    override fun clearTheme() {
        productClass = null
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
        callLog?.record("trip:$fromStopId->$toStopId")
        val canonicalTripId = "$fromStopId->$toStopId"
        val existingSortOrder = tripsFlow.value
            .find {
                it.tripId == canonicalTripId ||
                    (it.fromStopId == fromStopId && it.toStopId == toStopId)
            }
            ?.sort_order
            ?: 0L
        val trip = SavedTrip(
            canonicalTripId,
            fromStopId,
            fromStopName,
            toStopId,
            toStopName,
            timestamp = null,
            sort_order = existingSortOrder,
        )
        val current = tripsFlow.value.toMutableList()
        current.removeAll {
            it.tripId == canonicalTripId ||
                (it.fromStopId == fromStopId && it.toStopId == toStopId)
        }
        current.add(trip)
        tripsFlow.value = current
    }

    override fun deleteTrip(tripId: String) {
        val current = tripsFlow.value.toMutableList()
        current.removeAll { it.tripId == tripId }
        tripsFlow.value = current
    }

    override fun selectAllTrips(): List<SavedTrip> {
        return tripsFlow.value
    }

    // Ordered by sort_order as the real query is, and stable, so trips that were never moved keep the
    // order they were saved in.
    override fun observeAllTrips(): Flow<List<SavedTrip>> {
        return tripsFlow.asStateFlow().map { trips -> trips.sortedBy { it.sort_order } }
    }

    override fun selectTripById(tripId: String): SavedTrip? {
        return tripsFlow.value.find { it.tripId == tripId }
    }

    override fun updateSavedTripSortOrder(tripId: String, sortOrder: Long) {
        tripsFlow.value = tripsFlow.value.map { trip ->
            if (trip.tripId == tripId) trip.copy(sort_order = sortOrder) else trip
        }
    }

    override fun clearSavedTrips() {
        tripsFlow.value = emptyList()
    }

    // endregion

    // region Service Alerts

    override fun getAlerts(journeyId: String): List<SelectServiceAlertsByJourneyId> {
        return alerts[journeyId] ?: emptyList()
    }

    override fun clearAlerts() {
        alerts.clear()
    }

    override fun insertAlerts(journeyId: String, alerts: List<SelectServiceAlertsByJourneyId>) {
        this.alerts[journeyId] = alerts
    }
    // endregion

    override fun insertNswStop(
        stopId: String,
        stopName: String,
        stopLat: Double,
        stopLon: Double,
        isParent: Boolean?,
    ) {
        check(stopId != failInsertNswStopId) { "FakeSandook: insert of stop $stopId failed" }
        // Only store when explicitly false (child stop)
        // NULL means parent stop (default)
        stops.add(NswStops(stopId, stopName, stopLat, stopLon, isParent = if (isParent == false) 0L else null))
    }

    override fun stopsCount(): Int {
        check(!failStopsCount) { "FakeSandook: stopsCount failed" }
        return stops.size
    }

    override fun productClassCount(): Int {
        return stopProductClasses.size
    }

    override fun insertNswStopProductClass(stopId: String, productClass: Int) {
        val productClasses = stopProductClasses.getOrPut(stopId) { mutableListOf() }
        productClasses.add(productClass)
    }

    /** Like the real driver, a block that throws leaves the stops tables as they were before it. */
    override fun <R> insertTransaction(block: () -> R): R {
        val stopsBefore = stops.toList()
        val productClassesBefore = stopProductClasses.mapValues { it.value.toMutableList() }
        var committed = false
        try {
            return block().also { committed = true }
        } finally {
            if (!committed) {
                stops.clear()
                stops.addAll(stopsBefore)
                stopProductClasses.clear()
                stopProductClasses.putAll(productClassesBefore)
            }
        }
    }

    override fun clearNswStopsTable() {
        stops.clear()
    }

    override fun clearNswProductClassTable() {
        stopProductClasses.clear()
    }

    override fun selectStops(
        stopName: String,
        excludeProductClassList: List<Int>,
    ): List<SelectProductClassesForStop> {
        return stops.map { stop ->
            SelectProductClassesForStop(
                stop.stopId,
                stop.stopName,
                stop.stopLat,
                stop.stopLon,
                isParent = stop.isParent,
                productClasses = stopProductClasses[stop.stopId]?.joinToString(",") ?: "",
            )
        }
    }

    override fun selectStopsByIds(stopIds: List<String>): List<SelectProductClassesForStop> {
        if (stopIds.isEmpty()) return emptyList()
        val idSet = stopIds.toSet()
        return stops.filter { it.stopId in idSet }.map { stop ->
            SelectProductClassesForStop(
                stop.stopId,
                stop.stopName,
                stop.stopLat,
                stop.stopLon,
                isParent = stop.isParent,
                productClasses = stopProductClasses[stop.stopId]?.joinToString(",") ?: "",
            )
        }
    }

    override fun upsertRecentSearchLocation(location: RecentSearchLocation) {
        recentSearchLocations.removeAll { it.locationId == location.locationId }
        recentSearchLocations.add(
            0,
            RecentSearchLocations(
                locationId = location.locationId,
                displayName = location.displayName,
                kind = location.kind,
                addressType = location.addressType,
                productClasses = location.productClasses,
                timestamp = "2028-01-01 12:00:00",
            ),
        )
        cleanupOldRecentSearchLocations()
    }

    override fun selectRecentSearchLocations(): List<RecentSearchLocations> = recentSearchLocations.toList()

    override fun clearRecentSearchLocations() {
        recentSearchLocations.clear()
    }

    override fun cleanupOldRecentSearchLocations() {
        // Keep only the most recent entries
        if (recentSearchLocations.size > RECENT_SEARCH_STOPS_CAP) {
            val toKeep = recentSearchLocations.take(RECENT_SEARCH_STOPS_CAP)
            recentSearchLocations.clear()
            recentSearchLocations.addAll(toKeep)
        }
    }

    override fun selectStopCoordinatesBatch(stopIds: List<String>): Map<String, Pair<Double, Double>> {
        return stops
            .filter { it.stopId in stopIds }
            .associate { stop -> stop.stopId to Pair(stop.stopLat, stop.stopLon) }
    }

    private companion object {
        // Match the production cap RealSandook applies on recent searches.
        const val RECENT_SEARCH_STOPS_CAP = 5
    }
}
