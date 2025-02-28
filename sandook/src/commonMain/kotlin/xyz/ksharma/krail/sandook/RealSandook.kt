package xyz.ksharma.krail.sandook

import xyz.ksharma.krail.core.log.log

internal class RealSandook(factory: SandookDriverFactory) : Sandook {

    private val sandook = KrailSandook(factory.createDriver())
    private val query = sandook.krailSandookQueries

    private val nswStopsQueries = sandook.nswStopsQueries

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

    override fun selectTripById(tripId: String): SavedTrip? {
        return query.selectTripById(tripId).executeAsOneOrNull()
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

    override fun insertNswStopProductClass(stopId: String, productClass: Int) {
        nswStopsQueries.insertStopProductClass(stopId, productClass.toLong())
    }

    override fun insertTransaction(block: () -> Unit) {
        nswStopsQueries.transaction { block() }
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
            stopName,
            stopName,
            productClass = excludeProductClassList.map { it.toLong() },
        ).executeAsList()
    }

    // endregion NswStops
}
