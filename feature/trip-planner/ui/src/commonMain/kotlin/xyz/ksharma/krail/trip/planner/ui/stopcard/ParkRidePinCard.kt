package xyz.ksharma.krail.trip.planner.ui.stopcard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.savedtrips.CarParkBlock
import xyz.ksharma.krail.trip.planner.ui.savedtrips.CarParkBlockContent
import xyz.ksharma.krail.trip.planner.ui.savedtrips.ParkRideTile
import xyz.ksharma.krail.trip.planner.ui.savedtrips.ReservedLine
import xyz.ksharma.krail.trip.planner.ui.savedtrips.SpotsLine
import xyz.ksharma.krail.trip.planner.ui.savedtrips.StationLine
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

private const val NO_NUMBER_YET = 0
private const val NO_TIME_YET = "12:00 AM"

/**
 * The floating card a Park & Ride pin opens, in the same shell as the stop card
 * (`docs/MAP_HOME_UX.md`, Stop card and rule 40): the station's name with the close button, then, for
 * a station with several car parks, its total and the time of the oldest reading where the closed
 * home tile shows them, then every car park stacked at equal weight with its own free spots, percent
 * full, reading time and Directions, then the action.
 *
 * Directions go to a car park's own position and a car park without one has no Directions button, so
 * the card never promises a trip it cannot start. A car park with no reading yet shows its name alone,
 * and the total and the time keep their line boxes while they wait, so the card is the same height
 * whether its numbers are loading, loaded or failed (rule 22).
 *
 * The actions are Go here (Start here in a start search) when the station has a stop in the local stops
 * table, and always "Add to home" or "Remove from home". A station with no stop has no Go here, so the
 * card never promises a trip it cannot start (rule 40). [pickAction] is decided by
 * [parkRidePickActionFor], and the Add Park & Ride picker, which is not choosing a trip, gives none.
 */
@Composable
internal fun ParkRidePinCard(
    tile: ParkRideTile,
    isOnHome: Boolean,
    onToggleHome: () -> Unit,
    onDirectionsClick: (CarParkBlock) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    pickAction: PickAction? = null,
    onPick: (StopItem) -> Unit = {},
) {
    val layout = stopCardLayout(StopCardKind.ParkRide, modeCount = 0, carParkCount = tile.carParks.size)
    key(stopCardStateKey(StopCardKind.ParkRide, tile.stationId)) {
        StopCardFrame(
            announcement = tile.stationName,
            closeLabel = "Close ${tile.stationName}",
            onClose = onClose,
            modifier = modifier,
            heading = { StationLine(name = tile.stationName, large = true) },
        ) {
            if (layout.showsTotal) StationTotal(tile = tile)
            CarParks(tile = tile, onDirectionsClick = onDirectionsClick)
            if (layout.showsActionSlot) {
                ActionSlot {
                    ParkRideActions(
                        homeText = parkRidePinActionLabel(isOnHome),
                        onToggleHome = onToggleHome,
                        pickAction = pickAction,
                        onPick = onPick,
                    )
                }
            }
        }
    }
}

/**
 * Go here above Add to home or Remove from home, one whole button under another at every width. With
 * Go here the primary is Go here and Add to home is the outlined one; alone, Add to home is the primary.
 */
@Composable
private fun ParkRideActions(
    homeText: String,
    onToggleHome: () -> Unit,
    pickAction: PickAction?,
    onPick: (StopItem) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KrailTheme.dimensions.spacingM),
    ) {
        if (pickAction == null) {
            StopCardAction(
                text = homeText,
                onClick = onToggleHome,
                modifier = Modifier.testTag(TripPlannerTestTags.StopCard.HOME_ACTION),
            )
        } else {
            StopCardSecondaryAction(
                text = homeText,
                onClick = onToggleHome,
                modifier = Modifier.testTag(TripPlannerTestTags.StopCard.HOME_ACTION),
            )
            StopCardAction(
                text = pickAction.text,
                onClick = { onPick(pickAction.stop) },
                modifier = Modifier.testTag(TripPlannerTestTags.StopCard.PICK),
            )
        }
    }
}

/** The station's total and the time of its oldest reading, in the closed tile's own parts. */
@Composable
private fun StationTotal(tile: ParkRideTile) {
    Column(
        modifier = Modifier.testTag(TripPlannerTestTags.StopCard.PARK_RIDE_TOTAL).fillMaxWidth().cardInset(),
        verticalArrangement = Arrangement.spacedBy(KrailTheme.dimensions.spacingM),
    ) {
        ReservedLine(present = tile.totalFreeSpots != null) {
            SpotsLine(spots = tile.totalFreeSpots ?: NO_NUMBER_YET)
        }
        ReservedLine(present = tile.timeText != null) {
            Text(
                text = "Last updated at ${tile.timeText ?: NO_TIME_YET}",
                style = KrailTheme.typography.bodySmall,
                color = KrailTheme.colors.secondaryLabel,
            )
        }
    }
}

/** Every car park of the station, one block under another at every width and text size. */
@Composable
private fun CarParks(tile: ParkRideTile, onDirectionsClick: (CarParkBlock) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().cardInset(),
        verticalArrangement = Arrangement.spacedBy(KrailTheme.dimensions.spacingXL),
    ) {
        tile.carParks.forEach { carPark ->
            key(carPark.facilityId) {
                CarParkBlockContent(
                    carPark = carPark,
                    onDirectionsClick = { onDirectionsClick(carPark) },
                    holdsHeightWhileLoading = true,
                )
            }
        }
    }
}
