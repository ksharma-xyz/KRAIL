package xyz.ksharma.krail.trip.planner.ui.testfakes

import xyz.ksharma.krail.trip.planner.ui.parkride.ParkRideAvailabilityLoader
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideMapping

/**
 * Records what a caller asked to refresh, so a test can assert the ViewModel went through the
 * rate-limited path without standing up a service and a cooldown behind it.
 */
class FakeParkRideAvailabilityLoader : ParkRideAvailabilityLoader {

    val refreshedMappings = mutableListOf<List<ParkRideMapping>>()

    override suspend fun refreshIfNeeded(mappings: List<ParkRideMapping>) {
        refreshedMappings += mappings
    }
}
