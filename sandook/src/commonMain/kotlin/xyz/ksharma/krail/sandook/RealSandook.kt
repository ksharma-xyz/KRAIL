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

    override fun insertNswStopProductClass(stopId: String, productClass: Int) {
        nswStopsQueries.insertStopProductClass(stopId, productClass.toLong())
    }

    override fun selectStopsByPartialName(stopName: String): List<NswStops> {
        return nswStopsQueries.selectStopsByPartialName(stopName).executeAsList()
    }

    override fun selectStopsByNameAndProductClass(
        stopName: String,
        includeProductClassList: List<Int>,
    ): List<NswStops> {
        return nswStopsQueries.selectStopsByNameAndProductClass(
            stopName,
            includeProductClassList.map { it.toLong() }
        ).executeAsList()
    }

    override fun selectStopsByNameExcludingProductClass(
        stopName: String,
        excludeProductClassList: List<Int>
    ): List<NswStops> {
        return nswStopsQueries.selectStopsByNameExcludingProductClass(
            stopName,
            excludeProductClassList.map { it.toLong() }
        ).executeAsList()
    }

    /**
     * Combines exact stopId and partial [stopName] search logic while excluding stops
     * based on the given list of product classes.
     */
    override fun selectStopsByNameExcludingProductClassOrExactId(
        stopName: String,
        excludeProductClassList: List<Int>
    ): List<NswStops> {
        val stopId = stopName
        return nswStopsQueries.selectStopsByNameExcludingProductClassOrExactStopId(
            stopId,
            stopName,
            excludeProductClassList.map { it.toLong() }
        ).executeAsList()
    }

    // endregion NswStops
}
