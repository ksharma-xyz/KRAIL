package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import kotlinx.collections.immutable.ImmutableList
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.CardShape
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags

/**
 * The Park & Ride row on home, `docs/MAP_HOME_UX.md` rules 29 and 45: one block the full width of
 * the stack, with one tile per saved station.
 *
 * Closed, it is one line of tiles that scrolls sideways, with the 16 dp inset at the start and at
 * the end of the scroll, so the next tile shows its edge. Every tile is as tall as the tallest, so
 * the row does not change height when a number arrives or a fetch fails (rule 22). Open, one tile
 * takes the whole width of the row and the others wait behind it, so the row never holds two open
 * tiles. Tapping a tile opens it and tapping the open tile's name closes it.
 *
 * @param openStationId the station whose tile is open, or null with every tile closed. Held by the
 * screen, saved across a rotation, so this draws and never decides.
 */
@Composable
internal fun ParkRideRow(
    tiles: ImmutableList<ParkRideTile>,
    openStationId: String?,
    onTileClick: (ParkRideTile) -> Unit,
    onDirectionsClick: (CarParkBlock) -> Unit,
    modifier: Modifier = Modifier,
) {
    val open = tiles.firstOrNull { it.stationId == openStationId }
    if (open != null) {
        OpenParkRideTile(
            tile = open,
            onNameClick = { onTileClick(open) },
            onDirectionsClick = onDirectionsClick,
            modifier = modifier.padding(horizontal = KrailTheme.dimensions.pageHorizontalPadding),
        )
    } else {
        ClosedParkRideRow(tiles = tiles, onTileClick = onTileClick, modifier = modifier)
    }
}

@Composable
private fun ClosedParkRideRow(
    tiles: ImmutableList<ParkRideTile>,
    onTileClick: (ParkRideTile) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val tileWidth = parkRideTileWidth(columnWidth = maxWidth, fontScale = LocalDensity.current.fontScale)
        Row(
            modifier = Modifier
                .testTag(TripPlannerTestTags.SAVED_TRIPS_PARK_RIDE_ROW)
                // The viewport is the whole column, so a row of one tile is as wide as one of six.
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                // The tallest tile sets the height and every other tile stretches to it.
                .height(IntrinsicSize.Max)
                .padding(horizontal = dim.pageHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
        ) {
            tiles.forEach { tile ->
                key(tile.stationId) {
                    ClosedParkRideTile(tile = tile, width = tileWidth, onClick = { onTileClick(tile) })
                }
            }
        }
    }
}

/**
 * One closed tile: the free spots across the station's car parks as a big number with "spots" small
 * beside it, then the car icon and the station name, then the time of the oldest reading. The card
 * shell is the saved trip card's: the theme wash, 16 dp padding and corners.
 *
 * With no reading yet, or a failed first fetch, the number and the time keep their line boxes
 * invisible, so the tile is the same height whether its numbers are loading, loaded or failed and
 * shows the station name alone, never an error.
 */
@Composable
private fun ClosedParkRideTile(
    tile: ParkRideTile,
    width: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    Column(
        modifier = modifier
            .testTag(TripPlannerTestTags.parkRideTile(tile.stationId))
            .width(width)
            .fillMaxHeight()
            .clip(CardShape)
            .background(color = themeBackgroundColor(), shape = CardShape)
            .klickable(onClick = onClick)
            .semantics { onClick(label = "Show car parks at ${tile.stationName}", action = null) }
            .padding(dim.spacingXL),
        verticalArrangement = Arrangement.spacedBy(dim.spacingM, Alignment.CenterVertically),
    ) {
        ReservedLine(present = tile.totalFreeSpots != null) {
            SpotsLine(spots = tile.totalFreeSpots ?: PLACEHOLDER_SPOTS)
        }
        StationLine(name = tile.stationName)
        ReservedLine(present = tile.timeText != null) {
            Text(
                text = "Last updated at ${tile.timeText ?: PLACEHOLDER_TIME}",
                style = KrailTheme.typography.bodySmall,
                color = KrailTheme.colors.secondaryLabel,
            )
        }
    }
}

/**
 * The open tile: the station name, which closes it, and every car park of the station stacked
 * as its own block at equal weight, each with its own time and, when it has a position, its own
 * Directions. The blocks stack at every width and text size and never sit beside each other.
 */
@Composable
private fun OpenParkRideTile(
    tile: ParkRideTile,
    onNameClick: () -> Unit,
    onDirectionsClick: (CarParkBlock) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    Column(
        modifier = modifier
            .testTag(TripPlannerTestTags.parkRideTileOpen(tile.stationId))
            .fillMaxWidth()
            .clip(CardShape)
            .background(color = themeBackgroundColor(), shape = CardShape)
            .padding(dim.spacingXL),
        verticalArrangement = Arrangement.spacedBy(dim.spacingXL),
    ) {
        StationLine(
            name = tile.stationName,
            large = true,
            modifier = Modifier
                .testTag(TripPlannerTestTags.parkRideTileName(tile.stationId))
                .fillMaxWidth()
                .klickable(onClick = onNameClick)
                .semantics { onClick(label = "Close ${tile.stationName}", action = null) },
        )
        tile.carParks.forEach { carPark ->
            key(carPark.facilityId) {
                CarParkBlockContent(carPark = carPark, onDirectionsClick = { onDirectionsClick(carPark) })
            }
        }
    }
}

private const val PLACEHOLDER_SPOTS = 0
private const val PLACEHOLDER_TIME = "12:00 AM"
