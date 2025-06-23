package xyz.ksharma.krail.io.gtfs.nswstops

interface StopsManager {

    /**
     * Reads and decodes the NSW stops from a protobuf file, then inserts the stops
     * into the database, if they are not already inserted.
     *
     */
    suspend fun insertStops()

    companion object {
        const val MINIMUM_REQUIRED_NSW_STOPS = 36_000
    }
}
