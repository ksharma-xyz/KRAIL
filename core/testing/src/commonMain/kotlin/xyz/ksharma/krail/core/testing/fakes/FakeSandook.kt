package xyz.ksharma.krail.core.testing.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.ksharma.krail.sandook.NswStops
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SavedTrip
import xyz.ksharma.krail.sandook.SelectProductClassesForStop
import xyz.ksharma.krail.sandook.SelectRecentSearchStops
import xyz.ksharma.krail.sandook.SelectServiceAlertsByJourneyId
import xyz.ksharma.krail.sandook.StopLabels

// Intrinsic to faking the production Sandook interface (33 methods). Not refactorable.
@Suppress("TooManyFunctions")
class FakeSandook : Sandook {

    private var productClass: Long? = null
    private val tripsFlow = MutableStateFlow<List<SavedTrip>>(emptyList())
    private val alerts = mutableMapOf<String, List<SelectServiceAlertsByJourneyId>>()
    private val stops = mutableListOf<NswStops>()
    private val stopProductClasses = mutableMapOf<String, MutableList<Int>>()
    private val recentSearchStops = mutableListOf<SelectRecentSearchStops>()
    private val stopLabelsFlow = MutableStateFlow<List<StopLabels>>(emptyList())

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
        val existingSortOrder = tripsFlow.value.find { it.tripId == tripId }?.sort_order ?: 0L
        val trip = SavedTrip(
            tripId,
            fromStopId,
            fromStopName,
            toStopId,
            toStopName,
            timestamp = null,
            sort_order = existingSortOrder,
        )
        val current = tripsFlow.value.toMutableList()
        current.removeAll { it.tripId == tripId }
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

    override fun observeAllTrips(): Flow<List<SavedTrip>> {
        return tripsFlow.asStateFlow()
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
        // Only store when explicitly false (child stop)
        // NULL means parent stop (default)
        stops.add(NswStops(stopId, stopName, stopLat, stopLon, isParent = if (isParent == false) 0L else null))
    }

    override fun stopsCount(): Int {
        return stops.size
    }

    override fun productClassCount(): Int {
        return stopProductClasses.size
    }

    override fun insertNswStopProductClass(stopId: String, productClass: Int) {
        val productClasses = stopProductClasses.getOrPut(stopId) { mutableListOf() }
        productClasses.add(productClass)
    }

    override fun <R> insertTransaction(block: () -> R): R {
        return block()
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

    override fun insertOrReplaceRecentSearchStop(stopId: String) {
        // Remove existing entry if it exists
        recentSearchStops.removeAll { it.stopId == stopId }

        // Find the corresponding stop details
        val stop = stops.find { it.stopId == stopId }
        if (stop != null) {
            // Create a new recent search stop entry
            val productClasses = stopProductClasses[stopId]?.joinToString(",") ?: ""
            val recentStop = SelectRecentSearchStops(
                stopId = stopId,
                timestamp = "2028-01-01 12:00:00", // Mock timestamp
                stopName = stop.stopName,
                productClasses = productClasses,
            )
            recentSearchStops.add(0, recentStop) // Add to beginning (most recent)

            // Keep only the most recent entries
            if (recentSearchStops.size > RECENT_SEARCH_STOPS_CAP) {
                recentSearchStops.removeAt(recentSearchStops.size - 1)
            }
        }
    }

    override fun selectRecentSearchStops(): List<SelectRecentSearchStops> {
        return recentSearchStops.toList()
    }

    override fun clearRecentSearchStops() {
        recentSearchStops.clear()
    }

    override fun cleanupOrphanedRecentSearchStops() {
        // Remove recent search stops that don't have corresponding stops
        val validStopIds = stops.map { it.stopId }.toSet()
        recentSearchStops.removeAll { it.stopId !in validStopIds }
    }

    override fun cleanupOldRecentSearchStops() {
        // Keep only the most recent entries
        if (recentSearchStops.size > RECENT_SEARCH_STOPS_CAP) {
            val toKeep = recentSearchStops.take(RECENT_SEARCH_STOPS_CAP)
            recentSearchStops.clear()
            recentSearchStops.addAll(toKeep)
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
