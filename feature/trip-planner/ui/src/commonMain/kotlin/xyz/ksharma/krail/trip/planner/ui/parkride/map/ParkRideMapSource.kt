package xyz.ksharma.krail.trip.planner.ui.parkride.map

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.coroutines.ext.suspendSafeResult
import xyz.ksharma.krail.platform.ops.PlatformOps
import xyz.ksharma.krail.sandook.NswParkRideSandook
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.db.NSWParkRideFacilityDetail
import xyz.ksharma.krail.trip.planner.ui.parkride.ParkRideAvailabilityLoader
import xyz.ksharma.krail.trip.planner.ui.parkride.ParkRideCatalogue
import xyz.ksharma.krail.trip.planner.ui.savedtrips.toParkRideState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideMapping
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.StationPosition
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState.ParkRideFacilityDetail
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.ParkRideMapCardUi

/** What a stop map needs for Park & Ride: where the stations are, one station's parking card, and Directions. */
interface ParkRideMapSource {
    /**
     * The stations that have parking, as stops a map can mark, all of them and however far from its centre.
     * Reads the local stops database, so call it off the main thread.
     */
    fun stops(): ImmutableList<NearbyStopFeature>

    /**
     * The parking card of the station that has [stopId], while it is collected. The stored numbers come at once with
     * loading set, then whatever the one fetch brings, and loading is cleared when it ends, however it ends. Nothing
     * polls. Emits nothing for a stop that has no parking.
     */
    fun card(stopId: String): Flow<ParkRideMapCardUi>

    /** Opens the phone's maps app with directions to one car park. */
    fun openDirections(latitude: Double, longitude: Double, label: String)
}

/** A map with no parking on it, for a caller that has none to show. */
object NoParkRideMapSource : ParkRideMapSource {
    override fun stops(): ImmutableList<NearbyStopFeature> = persistentListOf()
    override fun card(stopId: String): Flow<ParkRideMapCardUi> = flow { }
    override fun openDirections(latitude: Double, longitude: Double, label: String) = Unit
}

/** The marker layer's own item for a station's `P`: what it draws and what a tap hands back. */
internal fun NearbyStopFeature.toParkRidePickerItem() = ParkRideStationPickerItem(
    stationId = stopId,
    stationName = stopName,
    stopName = stopName,
    carParkNames = emptyList(),
    mappings = emptyList(),
    position = StationPosition(position.latitude, position.longitude),
)

private const val NOT_READ = -1

/**
 * Every car park of [mappings], with the stored reading where there is one and a car park with no reading where there is
 * not, in a fixed order by name and then id, so a number landing never moves a car park.
 */
internal fun carParksOf(
    mappings: List<ParkRideMapping>,
    stored: List<NSWParkRideFacilityDetail>,
): ImmutableList<ParkRideFacilityDetail> {
    val byFacility = stored.associateBy { it.facilityId }
    return mappings
        .distinctBy { it.facilityId }
        .map { mapping ->
            byFacility[mapping.facilityId]?.toParkRideState()?.copy(facilityName = mapping.facilityName)
                ?: ParkRideFacilityDetail(
                    spotsAvailable = NOT_READ,
                    totalSpots = NOT_READ,
                    facilityName = mapping.facilityName,
                    percentageFull = NOT_READ,
                    stopId = mapping.stopId,
                    timeText = "",
                    facilityId = mapping.facilityId,
                )
        }
        .sortedWith(compareBy({ it.facilityName }, { it.facilityId }))
        .toImmutableList()
}

/**
 * [catalogue]'s stations, each named, placed and given its transport modes by the local stops database, as a nearby stop
 * is, so its marker and card read the same. A station with several stop ids takes the first the database knows, and one
 * it knows none of is left out. The card's numbers come through [availabilityLoader], with the cooldown every Park &
 * Ride surface shares.
 */
internal class RealParkRideMapSource(
    private val catalogue: ParkRideCatalogue,
    private val sandook: Sandook,
    private val parkRideSandook: NswParkRideSandook,
    private val availabilityLoader: ParkRideAvailabilityLoader,
    private val platformOps: PlatformOps,
) : ParkRideMapSource {

    override fun stops(): ImmutableList<NearbyStopFeature> {
        val stations = catalogue.stations()
        val stopIds = stations.flatMap { station -> station.mappings.map { it.stopId } }.distinct()
        val rows = sandook.selectStopsByIds(stopIds).associateBy { it.stopId }
        return stations.mapNotNull { station ->
            val row = station.mappings.firstNotNullOfOrNull { rows[it.stopId] } ?: return@mapNotNull null
            NearbyStopFeature(
                stopId = row.stopId,
                stopName = row.stopName,
                position = LatLng(row.stopLat, row.stopLon),
                transportModes = row.productClasses
                    .split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .mapNotNull { NswTransportConfig.modeFromProductClass(it) }
                    .toImmutableList(),
                hasParkAndRide = true,
            )
        }.toImmutableList()
    }

    override fun card(stopId: String): Flow<ParkRideMapCardUi> = flow {
        val station = catalogue.stations()
            .firstOrNull { station -> station.mappings.any { it.stopId == stopId } }
            ?: return@flow
        val loading = MutableStateFlow(true)
        coroutineScope {
            launch {
                // A failed fetch leaves the stored numbers, and the card stops saying it is loading.
                availabilityLoader.suspendSafeResult(Dispatchers.Default) { refreshIfNeeded(station.mappings) }
                loading.value = false
            }
            emitAll(
                combine(parkRideSandook.getAllParkRideFacilityDetail(), loading) { stored, isLoading ->
                    ParkRideMapCardUi(
                        stationId = stopId,
                        carParks = carParksOf(station.mappings, stored),
                        isLoading = isLoading,
                    )
                }.distinctUntilChanged(),
            )
        }
    }

    override fun openDirections(latitude: Double, longitude: Double, label: String) {
        platformOps.openMapDirections(latitude = latitude, longitude = longitude, label = label)
    }
}
