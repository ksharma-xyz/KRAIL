package xyz.ksharma.krail.trip.planner.network.api.service.trip

internal object TripRequestParams {

    /**
     * Used to set the response data type. This documentation only covers responses that use
     * the JSON format. Setting the outputFormat value to rapidJSON is required to enable JSON output.
     */
    const val outputFormat: String = "outputFormat"

    /**
     * This specifies the format the coordinates are returned in. While other variations are
     * available, the EPSG:4326 format will return the widely-used format.
     */
    const val coordOutputFormat: String = "coordOutputFormat"

    /**
     * This value anchors the requested date time. If set to dep, then trips departing after
     * the specified date/time at the specified location are included.
     *
     * If set to arr, then trips arriving before the specified time at its destination stop are included.
     * Works in conjunctions with the [itdDate] and [itdTime] values.
     *
     * "dep" or "arr"
     */
    const val depArrMacro: String = "depArrMacro"

    /**
     * The reference date used when searching trips, in YYYYMMDD format.
     * For instance, 20160901 refers to 1 September 2016. Works in conjunction with the
     * [itdTime] and [depArrMacro] values. If not specified, the current server date is used.
     *
     * Optional, default to null if not provided
     */
    const val itdDate = "itdDate"

    /**
     * The reference time used when searching trips, in HHMM 24-hour format.
     * For instance, 2215 refers to 10:15 PM.
     * Works in conjunction with the [itdDate] and [depArrMacro] values.
     *
     * If not specified, the current server time is used.
     *
     * Optional, default to null if not provided
     */
    const val itdTime = "itdTime"

    /**
     * This is the type of data specified in the name_origin field. The origin indicates the
     * starting point when searching for journeys.
     * The best way to use the trip planner is to use use any for this field then specify a
     * valid location ID in type_origin, or to use coord
     * in this field and a correctly formatted coordinate in type_origin.
     *
     * Available values : any, coord
     *
     * Default value : any
     */
    const val typeOrigin: String = "type_origin"

    /**
     * This value is used to indicate the starting point when searching for journeys.
     * This value can be one of three things:
     * A valid location/stop ID (for example, 10101100 indicates Central Station - this can be
     * determined using stop_finder).
     *
     * A valid global stop ID (for example, 200060 indicates Central Station - this can be
     * determined using stop_finder) Coordinates in the format LONGITUDE:LATITUDE:EPSG:4326
     * (Note that longitude is first).
     *
     * Default value : 10101331
     */
    const val nameOrigin = "name_origin"

    /**
     * This is the type of data specified in the name_destination field. The origin indicates
     * the finishing point when searching for journeys. The best way to use the trip planner
     * is to use use any for this field then specify a valid location ID in type_destination,
     * or to use coord in this field and a correctly formatted coordinate in type_destination.
     *
     * Available values : any, coord
     *
     * Default value : any
     */
    const val typeDestination: String = "type_destination"

    /**
     * his value is used to indicate the finishing point when searching for journeys.
     * This value can be one of three things:
     * A valid location/stop ID (for example, 10101100 indicates Central Station - this can be
     * determined using [stopFinder]).
     *
     * A valid global stop ID (for example, 200060 indicates Central Station - this can be
     * determined using [stopFinder])
     * Coordinates in the format LONGITUDE:LATITUDE:EPSG:4326 (Note that longitude is first).
     *
     * Default value : 10102027
     */
    const val nameDestination = "name_destination"

    /**
     * This parameter indicates the maximum number of trips to returned. Fewer trips may be returned anyway,
     * depending on the available public transport services.
     *
     * Default value : 6
     */
    const val calcNumberOfTrips = "calcNumberOfTrips"

    /**
     * Including this parameter (regardless of its value) ensures that only wheelchair-accessible
     * options are returned.
     *
     * Available values : on
     */
    const val wheelchair: String = "wheelchair" // Optional, used for wheelchair accessible trips

    /**
     * This parameter which means of transport to exclude from the trip plan.
     * To exclude one means,
     * select one of the following: 1 = train, 2 = metro, 4 = light rail, 5 = bus, 7 = coach,
     * 9 = ferry, 11 = school bus.
     * `checkbox` allows you to exclude more than one means of transport when used in conjunction
     * with the exclMOT_<ID> parameters.
     *
     * Available values : checkbox, 1, 2, 4, 5, 7, 9, 11
     */
    const val excludedMeans: String =
        "excludedMeans" // Optional, to exclude specific transport modes

    /**
     * Excludes train services from the trip plan. Must be used in conjunction with
     * [excludedMeans]=checkbox
     *
     * Available values : 1
     */
    const val exclMOT1 = "exclMOT_1" // Optional, to exclude trains

    /**
     * Excludes metro services from the trip plan. Must be used in conjunction with
     * [excludedMeans]=checkbox
     *
     * Available values : 1
     */
    const val exclMOT2 = "exclMOT_2" // Optional, to exclude metro

    /**
     * Excludes light rail services from the trip plan. Must be used in conjunction with
     * [excludedMeans]=checkbox
     *
     * Available values : 1
     */
    const val exclMOT4 = "exclMOT_4" // Optional, to exclude light rail

    /**
     * Excludes bus services from the trip plan. Must be used in conjunction with
     * [excludedMeans]=checkbox
     *
     * Available values : 1
     */
    const val exclMOT5 = "exclMOT_5" // Optional, to exclude bus

    /**
     * Excludes coach services from the trip plan. Must be used in conjunction with
     * [excludedMeans]=checkbox
     *
     * Available values : 1
     */
    const val exclMOT7: String = "exclMOT_7" // Optional, to exclude bus

    /**
     * Excludes ferry services from the trip plan. Must be used in conjunction with
     * [excludedMeans]=checkbox
     *
     * Available values : 1
     */
    const val exclMOT9 = "exclMOT_9" // Optional, to exclude bus

    /**
     * Excludes school bus services from the trip plan. Must be used in conjunction with
     * [excludedMeans]=checkbox
     *
     * Available values : 1
     */
    const val exclMOT11 = "exclMOT_11"

    /**
     * Including this parameter enables a number of options that result in this API call
     * operating in the same way as the Transport for NSW Trip Planner web site,
     * including enabling real-time data.
     *
     * Available values : true
     *
     * Default value : true
     */
    const val tfNSWTR = "TfNSWTR"

    /**
     * Indicates which version of the API the caller is expecting for both request and response
     * data. Note that if this version differs from the version listed above then the returned
     * data may not be as expected.
     *
     * Default value : 10.2.1.42
     */
    const val version = "version"

    /**
     * This parameter activates the options for individual transport. If the parameter is
     * disabled, the parameters concerning individual transport will not be taken into account.
     * possible values are 0 and 1
     *
     * Default value : 1
     */
    const val itOptionsActive = "itOptionsActive" // Activates individual transport options

    /**
     * Activates the calculation of a monomodal trip, i.e., a trip that takes place exclusively
     * with the means of transport , e.g., with bicycle.
     *
     * Note 1: In order to use this parameter, the options for individual transport must be
     * activated with itOptionsActive=1.
     *
     * Note 2: If no monomodal trip with the means of transport is calculated despite the
     * parameter, the maximum time is often set too low. The parameter MaxITTime applies to
     * all means of transport, the parameter MaxITTimeto the means of transport (e.g., MaxITTime107).
     * These parameters are located in the [Parameters] section or are added to it.
     * The configuration can be alternatively overridden by the [maxTime] parameter.
     *
     */
    const val computeMonomodalTripBicycle = "computeMonomodalTripBicycle"

    const val cycleSpeed = "cycleSpeed"

    const val useElevationData = "useElevationData"

    const val elevFac = "elevFac"
}
