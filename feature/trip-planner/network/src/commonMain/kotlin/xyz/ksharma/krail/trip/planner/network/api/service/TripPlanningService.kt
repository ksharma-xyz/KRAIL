package xyz.ksharma.krail.trip.planner.network.api.service

import xyz.ksharma.krail.trip.planner.network.api.model.StopFinderResponse
import xyz.ksharma.krail.trip.planner.network.api.model.StopType
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse

/**
 * Swagger: https://opendata.transport.nsw.gov.au/dataset/trip-planner-apis/resource/917c66c3-8123-4a0f-b1b1-b4220f32585d
 */
interface TripPlanningService {

    suspend fun trip(
        originStopId: String,
        destinationStopId: String,
        depArr: DepArr = DepArr.DEP,

        /**
         * YYYYMMDD format.
         * E.g. 20160901 refers to 1 September 2016.
         */
        date: String? = null,

        /**
         * HHMM 24-hour format.
         * E.g. 0830 means 8:30am and 2030 means 8:30pm.
         */
        time: String? = null,

        /**
         * List of product classes to exclude from the response.
         * Only those journeys will be returned which are different modes of transport to the excluded
         * product classes.
         */
        excludeProductClassSet: Set<Int>,
    ): TripResponse

    suspend fun stopFinder(
        stopSearchQuery: String,
        stopType: StopType = StopType.STOP,
    ): StopFinderResponse
}


/**
 * This endpoint returns a list of departures for a given location based on the date and time
 * specified. This data can be used to display a "upcoming departures" board for a stop.
 *
 * Provides capability to provide NSW public transport departure information
 * from a stop, station or wharf including real-time.
 */
//("v1/tp/departure_mon")
//suspend fun departure()

/**
 * This endpoint returns a list of service alerts or additional information about travelling
 * on the public transport network. This list can be filtered by date, route type, route,
 * operator or stop.
 *
 * Provides capability to display all public transport service status and
 * incident information (as published from the Incident Capture System).
 */
//"v1/tp/add_info")
//suspend fun serviceAlert()

/**
 * When given a specific geographical location, this API finds public transport stops,
 * stations, wharfs and points of interest around that location.
 */
//"v1/tp/coord")
//suspend fun coordinateRequest()
