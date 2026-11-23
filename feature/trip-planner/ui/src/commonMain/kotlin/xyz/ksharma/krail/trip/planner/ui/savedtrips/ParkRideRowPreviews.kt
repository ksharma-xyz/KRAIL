package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.toImmutableSet
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState.ParkRideFacilityDetail

// The Park & Ride row in every state the home stack can show it in. Each is a @PreviewScreen, so
// it is drawn in light and dark at normal and 200 percent text, the 320 dp frame included.

@PreviewScreen
@Composable
private fun PreviewParkRideRowClosed() = RowPreview(openStationId = null)

@PreviewScreen
@Composable
private fun PreviewParkRideRowOpenSeveralCarParks() = RowPreview(openStationId = TALLAWONG.stopId)

@PreviewScreen
@Composable
private fun PreviewParkRideRowOpenOneCarPark() = RowPreview(openStationId = ONE_CAR_PARK.stopId)

@PreviewScreen
@Composable
private fun PreviewParkRideRowOpenNoCoordinates() = RowPreview(openStationId = NO_COORDINATES.stopId)

@PreviewScreen
@Composable
private fun PreviewParkRideRowLoading() = RowPreview(stations = listOf(LOADING, ONE_CAR_PARK), openStationId = null)

@PreviewScreen
@Composable
private fun PreviewParkRideRowStale() = RowPreview(stations = listOf(STALE, ONE_CAR_PARK), openStationId = null)

@Composable
private fun RowPreview(
    openStationId: String?,
    stations: List<ParkRideUiState> = listOf(TALLAWONG, ONE_CAR_PARK, NO_COORDINATES, LOADING),
) {
    PreviewTheme {
        Column(
            modifier = Modifier.fillMaxSize().background(
                KrailTheme.colors.surface,
            ).padding(top = KrailTheme.dimensions.spacingXXXL),
        ) {
            ParkRideRow(
                tiles = stations.toTiles(),
                openStationId = openStationId,
                onTileClick = {},
                onDirectionsClick = {},
            )
        }
    }
}

/** One car park of a preview station, with the reading and the position it carries. */
private data class PreviewCar(
    val id: String,
    val name: String,
    val free: Int,
    val time: String,
    val readAt: Long,
    val position: Pair<Double, Double>? = null,
)

private fun station(id: String, name: String, vararg cars: PreviewCar) = ParkRideUiState(
    stopId = id,
    stopName = name,
    facilities = cars.map { car ->
        ParkRideFacilityDetail(
            spotsAvailable = car.free,
            totalSpots = if (car.free < 0) -1 else PREVIEW_CAPACITY,
            facilityName = car.name,
            percentageFull = if (car.free < 0) -1 else PERCENT_FULL - car.free / PREVIEW_SPOTS_PER_PERCENT,
            stopId = id,
            timeText = car.time,
            facilityId = car.id,
            latitude = car.position?.first,
            longitude = car.position?.second,
            readAtEpochSeconds = car.readAt,
        )
    }.toImmutableSet(),
)

private const val PREVIEW_CAPACITY = 500
private const val PERCENT_FULL = 100
private const val PREVIEW_SPOTS_PER_PERCENT = 5
private const val EIGHT_FIVE = "8:05\u00A0AM"
private const val EIGHT = "8:00\u00A0AM"
private const val TALLAWONG_STOP = "2155384"
private const val BELLA_VISTA_STOP = "2153478"
private const val KELLYVILLE_STOP = "2000001"
private const val LEPPINGTON_STOP = "2000002"
private const val HORNSBY_STOP = "2000003"

private val TALLAWONG = station(
    TALLAWONG_STOP,
    "Tallawong",
    PreviewCar("26", "Tallawong P1", free = 120, time = EIGHT_FIVE, readAt = 300L, position = -33.69 to 150.90),
    PreviewCar("27", "Tallawong P2", free = 30, time = EIGHT_FIVE, readAt = 300L, position = -33.69 to 150.91),
    PreviewCar("28", "Tallawong P3", free = 5, time = "7:50\u00A0AM", readAt = 100L),
)

private val ONE_CAR_PARK = station(
    BELLA_VISTA_STOP,
    "Bella Vista",
    PreviewCar("31", "Bella Vista Car Park", free = 124, time = EIGHT_FIVE, readAt = 300L, position = -33.73 to 150.94),
)

private val NO_COORDINATES = station(
    KELLYVILLE_STOP,
    "Kellyville",
    PreviewCar("71", "Kellyville at-grade", free = 60, time = EIGHT, readAt = 200L),
    PreviewCar("72", "Kellyville multi-level", free = 15, time = EIGHT, readAt = 200L),
)

private val LOADING = station(
    LEPPINGTON_STOP,
    "Leppington",
    PreviewCar("81", "Leppington Car Park", free = -1, time = "", readAt = 0L),
)

private val STALE = station(
    HORNSBY_STOP,
    "Hornsby",
    PreviewCar("91", "Hornsby Car Park", free = 12, time = "6:00\u00A0AM", readAt = 1L, position = -33.70 to 151.10),
)
