package xyz.ksharma.krail.departures.network.api.service

import xyz.ksharma.krail.departures.network.api.model.DepartureMonitorResponse

/**
 * Provides real-time departure information for a given stop.
 *
 * Swagger: https://opendata.transport.nsw.gov.au/dataset/trip-planner-apis
 * Endpoint: GET /v1/tp/departure_mon
 *
 * This endpoint returns a list of upcoming departures for a stop, station or wharf,
 * including real-time data when available. Use it to build a "departure board" for
 * any NSW public transport stop.
 */
interface DeparturesService {

    /**
     * Returns upcoming departures for [stopId].
     *
     * @param stopId The NSW Transport stop ID (e.g. "10111010" for Town Hall station).
     *               Obtained from the stop_finder API.
     * @param date Reference date in YYYYMMDD format (e.g. "20260401").
     *             Defaults to the current server date when null.
     * @param time Reference time in HHMM 24-hour format (e.g. "1430" for 2:30 PM).
     *             Defaults to the current server time when null.
     */
    suspend fun departures(
        stopId: String,
        date: String? = null,
        time: String? = null,
    ): DepartureMonitorResponse
}
