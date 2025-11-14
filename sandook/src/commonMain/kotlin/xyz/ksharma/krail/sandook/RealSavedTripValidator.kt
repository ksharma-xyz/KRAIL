package xyz.ksharma.krail.sandook

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import xyz.ksharma.krail.core.log.log

/**
 * Default implementation of SavedTripValidator that validates saved trips against the current
 * NSW stops database.
 */
class RealSavedTripValidator(
    private val sandook: Sandook,
    private val ioDispatcher: CoroutineDispatcher,
) : SavedTripValidator {

    override suspend fun validateAllSavedTrips() = withContext(ioDispatcher) {
        val allSavedTrips = sandook.selectAllTrips()

        if (allSavedTrips.isEmpty()) {
            log("SavedTripValidator: No saved trips to validate")
            return@withContext
        }

        log("SavedTripValidator: Validating ${allSavedTrips.size} saved trips")

        // Collect all unique stop IDs from all trips
        val allStopIds = allSavedTrips.flatMap { trip ->
            listOf(trip.fromStopId, trip.toStopId)
        }.toSet()

        log("SavedTripValidator: Checking ${allStopIds.size} unique stops")

        // Check existence for all unique stops once
        val existingStops: Map<String, Boolean> = allStopIds.associateWith { stopId ->
            sandook.checkStopExists(stopId)
        }

        // Validate each trip using the cached stop existence results
        allSavedTrips.forEach { savedTrip ->
            val fromStopExists = existingStops[savedTrip.fromStopId] ?: false
            val toStopExists = existingStops[savedTrip.toStopId] ?: false

            val currentFromStopValid = savedTrip.isFromStopValid == 1L
            val currentToStopValid = savedTrip.isToStopValid == 1L

            // Update trip validity if either stop validity has changed
            if (fromStopExists != currentFromStopValid || toStopExists != currentToStopValid) {
                sandook.updateStopValidity(
                    tripId = savedTrip.tripId,
                    isFromStopValid = fromStopExists,
                    isToStopValid = toStopExists,
                )

                val status = if (fromStopExists && toStopExists) "VALID" else "INVALID"
                log(
                    "SavedTripValidator: Trip ${savedTrip.tripId} is $status - " +
                        "fromStop: ${savedTrip.fromStopId} (exists: $fromStopExists), " +
                        "toStop: ${savedTrip.toStopId} (exists: $toStopExists)",
                )
            }
        }

        log("SavedTripValidator: Validation complete")
    }
}
