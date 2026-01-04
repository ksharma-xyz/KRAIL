package xyz.ksharma.core.test.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.ksharma.krail.sandook.NswBusRouteVariants
import xyz.ksharma.krail.sandook.NswBusTripOptions
import xyz.ksharma.krail.sandook.NswStops
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SavedTrip
import xyz.ksharma.krail.sandook.SelectProductClassesForStop
import xyz.ksharma.krail.sandook.SelectRecentSearchStops
import xyz.ksharma.krail.sandook.SelectServiceAlertsByJourneyId
import xyz.ksharma.krail.sandook.SelectStopsByTripId

class FakeSandook : Sandook {

    private var productClass: Long? = null
    private val tripsFlow = MutableStateFlow<List<SavedTrip>>(emptyList())
    private val alerts = mutableMapOf<String, List<SelectServiceAlertsByJourneyId>>()
    private val stops = mutableListOf<NswStops>()
    private val stopProductClasses = mutableMapOf<String, MutableList<Int>>()
    private val recentSearchStops = mutableListOf<SelectRecentSearchStops>()

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
        val trip = SavedTrip(
            tripId,
            fromStopId,
            fromStopName,
            toStopId,
            toStopName,
            timestamp = null,
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

    override fun insertNswStop(stopId: String, stopName: String, stopLat: Double, stopLon: Double) {
        stops.add(NswStops(stopId, stopName, stopLat, stopLon, parentStopId = null))
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
                parentStopId = null,
                productClasses = stopProductClasses[stop.stopId]?.joinToString(",") ?: ""
            )
        }
    }

    // region NswBusRoutes - Stub implementations
    override fun selectRouteByShortName(routeShortName: String): String? {
        // TODO: Implement when needed for tests
        return null
    }

    override fun selectRouteVariantsByShortName(routeShortName: String): List<NswBusRouteVariants> {
        // TODO: Implement when needed for tests
        return emptyList()
    }

    override fun selectTripsByRouteId(routeId: String): List<NswBusTripOptions> {
        // TODO: Implement when needed for tests
        return emptyList()
    }

    override fun selectStopsByTripId(tripId: String): List<SelectStopsByTripId> {
        // TODO: Implement when needed for tests
        return emptyList()
    }
    // endregion

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
                productClasses = productClasses
            )
            recentSearchStops.add(0, recentStop) // Add to beginning (most recent)

            // Keep only the 5 most recent entries
            if (recentSearchStops.size > 5) {
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
        // Keep only the 5 most recent entries
        if (recentSearchStops.size > 5) {
            val toKeep = recentSearchStops.take(5)
            recentSearchStops.clear()
            recentSearchStops.addAll(toKeep)
        }
    }
}
