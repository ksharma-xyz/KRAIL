package xyz.ksharma.krail.io.gtfs.nswstops

interface StopsManager {

    /**
     * Reads and decodes the NSW stops from a protobuf file, then replaces the stored rows with
     * them in one transaction, if they are not already inserted. Readers see the old rows until the
     * transaction commits, and a failure leaves them untouched.
     *
     * Returns a failure carrying the cause rather than swallowing it. Cancellation is rethrown.
     */
    suspend fun insertStops(): Result<Unit>

    companion object {
        const val MINIMUM_REQUIRED_NSW_STOPS = 36_000
    }
}
