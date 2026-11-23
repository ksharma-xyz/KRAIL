package xyz.ksharma.krail.trip.planner.ui.stopcard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideMapping
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.StationPosition
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState.ParkRideFacilityDetail

// The Park & Ride pin card in every state a P pin can open it in, over a flat stand-in for the map.
// Each is a @PreviewScreen, so it is drawn in light and dark at normal and 200 percent text, the
// 320 dp frame included.

private const val TALLAWONG_STOP = "2155384"
private const val BELLA_VISTA_STOP = "2153478"
private const val TALLAWONG_NAME = "Tallawong"
private const val TOTAL_SPOTS = 250
private const val CAR_PARK_LATITUDE = -33.69
private const val CAR_PARK_LONGITUDE = 150.90

private fun mapping(id: String, name: String, stop: String) =
    ParkRideMapping(stopId = stop, facilityId = id, facilityName = name)

private fun station(stop: String, name: String, vararg mappings: ParkRideMapping) = ParkRideStationPickerItem(
    stationId = stop,
    stationName = name,
    stopName = "$name Station",
    carParkNames = mappings.map { it.facilityName },
    mappings = mappings.toList(),
    isUserAdded = true,
    position = StationPosition(CAR_PARK_LATITUDE, CAR_PARK_LONGITUDE),
)

// One reading with a position of its own. A car park's id is also its age, the oldest first.
private fun reading(id: String, name: String, free: Int, percent: Int, time: String) = ParkRideFacilityDetail(
    spotsAvailable = free,
    totalSpots = TOTAL_SPOTS,
    facilityName = name,
    percentageFull = percent,
    stopId = TALLAWONG_STOP,
    timeText = time,
    facilityId = id,
    latitude = CAR_PARK_LATITUDE,
    longitude = CAR_PARK_LONGITUDE,
    readAtEpochSeconds = id.toLong(),
)

private val TALLAWONG = station(
    TALLAWONG_STOP,
    TALLAWONG_NAME,
    mapping("26", "Tallawong P1", TALLAWONG_STOP),
    mapping("27", "Tallawong P2", TALLAWONG_STOP),
    mapping("28", "Tallawong P3", TALLAWONG_STOP),
)

private val TALLAWONG_READINGS = listOf(
    reading("26", "Tallawong P1", free = 50, percent = 80, time = "8:05 AM"),
    reading("27", "Tallawong P2", free = 7, percent = 97, time = "7:50 AM"),
    // The third car park's reading carried no position, so it has no Directions.
    reading("28", "Tallawong P3", free = 98, percent = 61, time = "8:00 AM").copy(latitude = null, longitude = null),
)

private val BELLA_VISTA = station(
    BELLA_VISTA_STOP,
    "Bella Vista",
    mapping("31", "Bella Vista Car Park", BELLA_VISTA_STOP),
)

@PreviewScreen
@Composable
private fun PreviewParkRidePinCardSeveralCarParks() =
    PinCardOverMap(TALLAWONG, TALLAWONG_READINGS, isOnHome = true)

@PreviewScreen
@Composable
private fun PreviewParkRidePinCardOneCarPark() = PinCardOverMap(
    BELLA_VISTA,
    listOf(
        reading("31", "Bella Vista Car Park", free = 120, percent = 52, time = "8:05 AM")
            .copy(stopId = BELLA_VISTA_STOP),
    ),
    isOnHome = false,
)

@PreviewScreen
@Composable
private fun PreviewParkRidePinCardNoReadingsYet() = PinCardOverMap(TALLAWONG, emptyList(), isOnHome = false)

@Composable
private fun PinCardOverMap(
    station: ParkRideStationPickerItem,
    readings: List<ParkRideFacilityDetail>,
    isOnHome: Boolean,
) {
    PreviewTheme {
        Box(modifier = Modifier.fillMaxSize().background(KrailTheme.colors.onSurface.copy(alpha = 0.08f))) {
            StopCardPlacement(topReserve = 0.dp, onHeight = {}) {
                ParkRidePinCard(
                    tile = station.toPinCardTile(readings),
                    isOnHome = isOnHome,
                    onToggleHome = {},
                    onDirectionsClick = {},
                    onClose = {},
                )
            }
        }
    }
}
