package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState.ParkRideFacilityDetail

/**
 * What one Park & Ride tile on home shows, decided from a saved station's state, with no layout.
 *
 * Every rule here is a plain function so a table test can drive it at its boundary, including the
 * boundaries a single real reading cannot reach: a station with a car park that has no number yet,
 * a car park with no position, a reading with no time. `docs/MAP_HOME_UX.md` rules 22, 29 and 40.
 *
 * @property stationId the stop id the saved station is keyed on, which is also what the expanded
 * state is saved under.
 * @property stopIds every stop id the station's car parks sit behind, in order, so the Go here
 * branch can pick one without reading the state again.
 * @property totalFreeSpots the free spots across the car parks that have a reading, or null when
 * none has one yet.
 * @property timeText the time of the oldest reading the total is made of, or null with no reading.
 */
@Stable
internal data class ParkRideTile(
    val stationId: String,
    val stationName: String,
    val stopIds: ImmutableList<String>,
    val totalFreeSpots: Int?,
    val timeText: String?,
    val carParks: ImmutableList<CarParkBlock>,
)

/**
 * One car park of a station, at equal weight with its neighbours. Numbers are null while it has
 * no reading, and [directionsTo] is null when the car park has no position of its own.
 */
@Stable
internal data class CarParkBlock(
    val facilityId: String,
    val name: String,
    val freeSpots: Int?,
    val percentFull: Int?,
    val timeText: String?,
    val directionsTo: LatLng?,
)

/** The width a tile has at 100 percent text, in dp. It scales with the font. */
internal val PARK_RIDE_TILE_BASE_WIDTH: Dp = 148.dp

/** Room kept clear at the end of the row, so the next tile always shows its edge. */
internal val PARK_RIDE_TILE_PEEK_ROOM: Dp = 60.dp

/**
 * The width of a closed tile in a column [columnWidth] wide at [fontScale]: 148 dp at normal text,
 * growing with the text, and never wider than the column less 60 dp, so the next tile peeks.
 *
 * A column with no room, or a negative width a measure pass can briefly hand over, gets a tile
 * with no width rather than a negative one.
 */
internal fun parkRideTileWidth(columnWidth: Dp, fontScale: Float): Dp =
    minOf(PARK_RIDE_TILE_BASE_WIDTH * fontScale, columnWidth - PARK_RIDE_TILE_PEEK_ROOM)
        .coerceAtLeast(0.dp)

/**
 * A car park has a number only once a fetch has landed. A row that has never been fetched is
 * stored with -1 in every number, which is the "no reading yet" sentinel and never a real count.
 */
internal fun ParkRideFacilityDetail.hasReading(): Boolean = spotsAvailable >= 0 && totalSpots >= 0

/**
 * The free spots across the car parks of one station, for the closed tile.
 *
 * Only car parks with a reading count. A station with none gives null, so the tile shows no number
 * rather than a 0 that would read as full. A station with some readings and some not adds the ones
 * it has, which can only understate the total, never promise a space that is not there.
 */
internal fun totalFreeSpots(facilities: Collection<ParkRideFacilityDetail>): Int? =
    facilities.filter { it.hasReading() }.takeIf { it.isNotEmpty() }?.sumOf { it.spotsAvailable }

/**
 * The time to show for a station as a whole: the time of its oldest reading, because the total is
 * only as fresh as the oldest number in it. Null when no reading has a time.
 */
internal fun oldestReadingTime(facilities: Collection<ParkRideFacilityDetail>): String? =
    facilities
        .filter { it.hasReading() && it.timeText.isNotBlank() }
        .minByOrNull { it.readAtEpochSeconds }
        ?.timeText

/**
 * Where Directions go for a car park: its own position, or nothing.
 *
 * A car park never borrows the station's position. A missing position is stored as 0.0 and 0.0,
 * which is a point in the Atlantic and not a car park, so that counts as missing too, as does a
 * position outside the globe. With nothing to go to the car park has no Directions button at all,
 * because a card never promises a trip it cannot start.
 */
internal fun directionsTarget(latitude: Double?, longitude: Double?): LatLng? {
    if (latitude == null || longitude == null) return null
    val atTheOrigin = latitude == 0.0 && longitude == 0.0
    val onTheGlobe = latitude in LATITUDE_RANGE && longitude in LONGITUDE_RANGE
    return if (atTheOrigin || !onTheGlobe) null else LatLng(latitude, longitude)
}

private val LATITUDE_RANGE = -90.0..90.0
private val LONGITUDE_RANGE = -180.0..180.0

/**
 * The tile for one saved station.
 *
 * The car parks keep a fixed order, by name and then id, so a number changing never moves a row.
 * Every car park of the station is listed whether or not it has a reading yet, each with its own
 * time, at equal weight. A car park with no name of its own is called by the station's.
 */
internal fun ParkRideUiState.toTile(): ParkRideTile {
    val ordered = facilities.sortedWith(compareBy({ it.facilityName.lowercase() }, { it.facilityId }))
    return ParkRideTile(
        stationId = stopId,
        stationName = stopName,
        stopIds = ordered.map { it.stopId }.distinct().sorted().ifEmpty { listOf(stopId) }.toImmutableList(),
        totalFreeSpots = totalFreeSpots(ordered),
        timeText = oldestReadingTime(ordered),
        carParks = ordered.map { it.toBlock(fallbackName = stopName) }.toImmutableList(),
    )
}

private fun ParkRideFacilityDetail.toBlock(fallbackName: String): CarParkBlock {
    val read = hasReading()
    return CarParkBlock(
        facilityId = facilityId,
        name = facilityName.toDisplayFacilityName().ifBlank { fallbackName },
        freeSpots = spotsAvailable.takeIf { read },
        percentFull = percentageFull.takeIf { read && it >= 0 },
        timeText = timeText.takeIf { read && it.isNotBlank() },
        directionsTo = directionsTarget(latitude, longitude),
    )
}

/**
 * The tiles of the saved stations, with a station that has no car park left out: a tile with
 * nothing to show would be a card with no purpose.
 */
internal fun List<ParkRideUiState>.toTiles(): ImmutableList<ParkRideTile> =
    filter { it.facilities.isNotEmpty() }.map { it.toTile() }.toImmutableList()

/**
 * Which tile is open after [tapped] is tapped, one at a time. Tapping the open tile closes it, and
 * tapping any other opens that one and closes the first.
 */
internal fun openTilesAfterTap(open: Set<String>, tapped: String): Set<String> =
    if (tapped in open) emptySet() else setOf(tapped)
