package xyz.ksharma.krail.io.gtfs.nswstops

interface ProtoParser {
    /**
     * Reads and decodes the NSW stops from a protobuf file, then inserts the stops into the database.
     */
    suspend fun parseAndInsertStops()
}
