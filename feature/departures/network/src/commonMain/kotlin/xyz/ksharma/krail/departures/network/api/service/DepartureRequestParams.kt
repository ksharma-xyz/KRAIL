package xyz.ksharma.krail.departures.network.api.service

internal object DepartureRequestParams {

    /**
     * Used to set the response data type. Must be set to "rapidJSON" for JSON output.
     */
    const val OUTPUT_FORMAT = "outputFormat"

    /**
     * Specifies the coordinate format. Use "EPSG:4326" for standard lat/lon.
     */
    const val COORD_OUTPUT_FORMAT = "coordOutputFormat"

    /**
     * When set to "direct", bypasses the stop verification step and returns departures
     * directly. Use this when the stop ID is already known.
     */
    const val MODE = "mode"

    /**
     * Specifies the type of the location given in [NAME_DM].
     * Use "stop" when providing a stop ID.
     *
     * Available values: any, coord, poi, singlehouse, stop, platform, street, locality, suburb
     */
    const val TYPE_DM = "type_dm"

    /**
     * The stop ID or search term identifying the departure location.
     * When [TYPE_DM] is "stop", this must be a valid NSW Transport stop ID.
     */
    const val NAME_DM = "name_dm"

    /**
     * Reference date for the departure search in YYYYMMDD format.
     * Defaults to the current server date when omitted.
     */
    const val ITD_DATE = "itdDate"

    /**
     * Reference time for the departure search in HHMM 24-hour format.
     * Defaults to the current server time when omitted.
     */
    const val ITD_TIME = "itdTime"

    /**
     * Enables the departure monitor macro — mirrors the behaviour of the
     * Transport for NSW Trip Planner website. Recommended to be set to "true"
     * together with [TF_NSW_DM].
     */
    const val DEPARTURE_MONITOR_MACRO = "departureMonitorMacro"

    /**
     * Enables Transport for NSW specific departure monitor options including
     * real-time data. Recommended to be set to "true" together with [DEPARTURE_MONITOR_MACRO].
     */
    const val TF_NSW_DM = "TfNSWDM"

    /**
     * Indicates the API version expected by the caller.
     */
    const val VERSION = "version"

    // -- Transport mode exclusion -------------------------------------------------

    /**
     * Controls which transport modes are excluded.
     * Set to "checkbox" to enable multi-mode exclusion via [EXCL_MOT_*] params.
     */
    const val EXCLUDED_MEANS = "excludedMeans"

    /** Exclude trains (product class 1). Must pair with [EXCLUDED_MEANS] = "checkbox". */
    const val EXCL_MOT_1 = "exclMOT_1"

    /** Exclude metro (product class 2). Must pair with [EXCLUDED_MEANS] = "checkbox". */
    const val EXCL_MOT_2 = "exclMOT_2"

    /** Exclude light rail (product class 4). Must pair with [EXCLUDED_MEANS] = "checkbox". */
    const val EXCL_MOT_4 = "exclMOT_4"

    /** Exclude buses (product class 5). Must pair with [EXCLUDED_MEANS] = "checkbox". */
    const val EXCL_MOT_5 = "exclMOT_5"

    /** Exclude coaches (product class 7). Must pair with [EXCLUDED_MEANS] = "checkbox". */
    const val EXCL_MOT_7 = "exclMOT_7"

    /** Exclude ferries (product class 9). Must pair with [EXCLUDED_MEANS] = "checkbox". */
    const val EXCL_MOT_9 = "exclMOT_9"

    /** Exclude school buses (product class 11). Must pair with [EXCLUDED_MEANS] = "checkbox". */
    const val EXCL_MOT_11 = "exclMOT_11"
}
