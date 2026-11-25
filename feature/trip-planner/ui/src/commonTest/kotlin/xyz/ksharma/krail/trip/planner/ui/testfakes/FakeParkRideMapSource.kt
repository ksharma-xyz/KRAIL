package xyz.ksharma.krail.trip.planner.ui.testfakes

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.trip.planner.ui.parkride.map.ParkRideMapSource
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.ParkRideMapCardUi

/** Stations a ViewModel test names itself, and a card it moves by hand. */
internal class FakeParkRideMapSource : ParkRideMapSource {

    val station = NearbyStopFeature(
        stopId = "2155384",
        stopName = "Tallawong Station",
        position = LatLng(-33.69, 150.9),
        transportModes = persistentListOf(),
        hasParkAndRide = true,
    )

    /** How many times the stations were read. */
    var stationReads = 0
        private set

    /** The stop ids a card was asked for, in order. */
    val cardsAskedFor = mutableListOf<String>()

    val card = MutableStateFlow<ParkRideMapCardUi?>(null)

    var lastDirections: Triple<Double, Double, String>? = null
        private set

    override fun stops(): ImmutableList<NearbyStopFeature> {
        stationReads++
        return listOf(station).toImmutableList()
    }

    override fun card(stopId: String): Flow<ParkRideMapCardUi> {
        cardsAskedFor += stopId
        return card.filterNotNull()
    }

    override fun openDirections(latitude: Double, longitude: Double, label: String) {
        lastDirections = Triple(latitude, longitude, label)
    }
}

/** A card with no car parks, which is all a ViewModel test needs to see it arrive. */
internal fun parkRideCardOf(stopId: String, isLoading: Boolean = false) =
    ParkRideMapCardUi(stationId = stopId, carParks = persistentListOf(), isLoading = isLoading)
