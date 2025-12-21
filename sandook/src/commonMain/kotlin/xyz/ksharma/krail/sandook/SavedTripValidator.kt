package xyz.ksharma.krail.sandook

/**
 * Interface for validating saved trips against the current NSW stops database.
 *
 * When new stops are inserted into the database, this validator checks if any saved trips
 * reference stops that no longer exist. Invalid trips are marked in the database so they
 * can be displayed with a warning to the user.
 */
interface SavedTripValidator {
    /**
     * Validates all saved trips against the current stops in the database.
     * Marks trips as invalid if either the fromStopId or toStopId does not exist in NswStops.
     * Marks trips as valid if both stops exist.
     *
     * Invalid trips are logged for debugging. The database is updated and changes will be
     * reflected when observing saved trips.
     */
    suspend fun validateAllSavedTrips()
}
