package xyz.ksharma.core.test.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.ksharma.krail.sandook.NswStops
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SavedTrip
import xyz.ksharma.krail.sandook.SelectProductClassesForStop
import xyz.ksharma.krail.sandook.SelectServiceAlertsByJourneyId

class FakeSandook : Sandook {

    private var productClass: Long? = null
    private val tripsFlow = MutableStateFlow<List<SavedTrip>>(emptyList())
    private val alerts = mutableMapOf<String, List<SelectServiceAlertsByJourneyId>>()
    private val stops = mutableListOf<NswStops>()
    private val stopProductClasses = mutableMapOf<String, MutableList<Int>>()

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
        stops.add(NswStops(stopId, stopName, stopLat, stopLon))
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
                productClasses = stopProductClasses[stop.stopId]?.joinToString(",") ?: ""
            )
        }
    }
}
