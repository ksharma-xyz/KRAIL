package xyz.ksharma.krail.feature.crowding

/**
 * How full a service is. TfNSW publishes three levels and never a number, so neither does KRAIL.
 * See docs/investigations/TFNSW_OCCUPANCY_DATA.md.
 *
 * Declared quietest first, so [ordinal] order is "fuller" order and `maxOf` is the busiest.
 */
enum class SeatLevel {
    SEATS_AVAILABLE,
    FEW_SEATS,
    STANDING_ONLY,
    ;

    /** True when this is at least one full level fuller than [other]. */
    fun isBusierThan(other: SeatLevel): Boolean = ordinal > other.ordinal

    companion object {
        // GTFS-R VehiclePosition.OccupancyStatus values.
        private const val GTFS_EMPTY = 0
        private const val GTFS_MANY_SEATS = 1
        private const val GTFS_FEW_SEATS = 2
        private const val GTFS_STANDING = 3
        private const val GTFS_CRUSHED = 4
        private const val GTFS_FULL = 5

        /**
         * Parses the `occupancy` string on trip planner and departure monitor stops. Both the
         * EFA short form (`MANY_SEATS`) and the GTFS-R name (`MANY_SEATS_AVAILABLE`) are accepted,
         * because the two APIs are not documented to agree. Anything else is "no data".
         */
        fun fromApi(value: String?): SeatLevel? = when (value?.trim()?.uppercase()) {
            "EMPTY", "MANY_SEATS", "MANY_SEATS_AVAILABLE" -> SEATS_AVAILABLE
            "FEW_SEATS", "FEW_SEATS_AVAILABLE" -> FEW_SEATS
            "STANDING_ONLY", "STANDING_ROOM_ONLY", "CRUSHED_STANDING_ROOM_ONLY", "FULL" ->
                STANDING_ONLY
            else -> null
        }

        /**
         * Parses GTFS-R `OccupancyStatus`. NOT_ACCEPTING_PASSENGERS (6), NO_DATA_AVAILABLE (7)
         * and NOT_BOARDABLE (8) are not a seat level, so they read as "no data".
         */
        fun fromGtfs(value: Int?): SeatLevel? = when (value) {
            GTFS_EMPTY, GTFS_MANY_SEATS -> SEATS_AVAILABLE
            GTFS_FEW_SEATS -> FEW_SEATS
            GTFS_STANDING, GTFS_CRUSHED, GTFS_FULL -> STANDING_ONLY
            else -> null
        }
    }
}

/** Where a level came from, which decides the one certainty word in every line. */
enum class Certainty {
    /** A reading from the vehicle now: "has". */
    LIVE,

    /** A per-stop prediction: "expected" or "should". Never "will". */
    FORECAST,
}
