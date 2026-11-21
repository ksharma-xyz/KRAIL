package xyz.ksharma.krail.trip.planner.ui.testfakes

import xyz.ksharma.krail.trip.planner.ui.parkride.ParkRideAvailabilityLoader
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideMapping

/**
 * Records what a caller asked to refresh, so a test can assert the ViewModel went through the
 * rate-limited path without standing up a service and a cooldown behind it.
 *
 * [onRefresh] runs after the call is recorded, so a test can write the rows a real refresh would
 * have written, or throw to stand in for a refresh that failed.
 */
internal class FakeParkRideAvailabilityLoader : ParkRideAvailabilityLoader {

    val refreshedMappings = mutableListOf<List<ParkRideMapping>>()

    var onRefresh: suspend (List<ParkRideMapping>) -> Unit = {}

    override suspend fun refreshIfNeeded(mappings: List<ParkRideMapping>) {
        refreshedMappings += mappings
        onRefresh(mappings)
    }
}
