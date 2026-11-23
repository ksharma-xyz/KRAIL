package xyz.ksharma.krail.trip.planner.ui.stopcard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideMapping
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

// Save as, Go here and Start here on the floating cards, over a flat stand-in for the map. Each is a
// @PreviewScreen, so it is drawn in light and dark at normal and 200 percent text, the 320 dp frame included.
// The board is closed, as a pin opens it.

private val CENTRAL = NearbyStopFeature(
    stopId = "200060",
    stopName = "Central Station",
    position = LatLng(-33.8830, 151.2070),
    transportModes = persistentListOf(TransportMode.Train, TransportMode.Bus, TransportMode.LightRail),
)

private val HOME = StopLabel(emoji = "🏠", label = "Home")
private val WORK = StopLabel(emoji = "💼", label = "Work")
private val GYM = StopLabel(emoji = "🏋", label = "Gym")

private const val TALLAWONG_STOP = "2155384"

@PreviewScreen
@Composable
private fun PreviewStopCardSaveAsAndGoHere() =
    StopCardWithActions(options = StopCardOptions(PickRole.Destination, persistentListOf(HOME, WORK, GYM)))

@PreviewScreen
@Composable
private fun PreviewStopCardStartHere() =
    StopCardWithActions(options = StopCardOptions(PickRole.Start, persistentListOf(HOME, WORK)))

@PreviewScreen
@Composable
private fun PreviewStopCardSavedAsHome() = StopCardWithActions(
    options = StopCardOptions(
        PickRole.Destination,
        persistentListOf(HOME.copy(stopId = CENTRAL.stopId, stopName = CENTRAL.stopName), WORK),
    ),
)

@PreviewScreen
@Composable
private fun PreviewStopCardChoosingAStopForALabel() =
    StopCardWithActions(options = StopCardOptions(PickRole.Choose, persistentListOf(HOME, WORK)))

@PreviewScreen
@Composable
private fun PreviewParkRidePinCardGoHere() = ParkRideCardWithActions(
    goHereStopId = TALLAWONG_STOP,
    role = PickRole.Destination,
)

@PreviewScreen
@Composable
private fun PreviewParkRidePinCardStartHere() = ParkRideCardWithActions(
    goHereStopId = TALLAWONG_STOP,
    role = PickRole.Start,
)

@PreviewScreen
@Composable
private fun PreviewParkRidePinCardNoStop() = ParkRideCardWithActions(goHereStopId = null, role = PickRole.Destination)

@Composable
private fun StopCardWithActions(options: StopCardOptions) {
    PreviewTheme {
        Box(modifier = Modifier.fillMaxSize().background(KrailTheme.colors.onSurface.copy(alpha = 0.08f))) {
            StopCardPlacement(topReserve = 96.dp, onHeight = {}) {
                StopCard(
                    stop = CENTRAL,
                    departures = DeparturesState(),
                    onEvent = {},
                    onClose = {},
                    actions = {
                        StopCardPickActions(
                            stop = StopItem(stopId = CENTRAL.stopId, stopName = CENTRAL.stopName),
                            options = options,
                            onPick = {},
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ParkRideCardWithActions(goHereStopId: String?, role: PickRole) {
    val station = ParkRideStationPickerItem(
        stationId = TALLAWONG_STOP,
        stationName = "Tallawong",
        stopName = "Tallawong Station",
        carParkNames = listOf("Tallawong P1"),
        mappings = listOf(ParkRideMapping(TALLAWONG_STOP, "26", "Tallawong P1")),
    )
    PreviewTheme {
        Box(modifier = Modifier.fillMaxSize().background(KrailTheme.colors.onSurface.copy(alpha = 0.08f))) {
            StopCardPlacement(topReserve = 0.dp, onHeight = {}) {
                ParkRidePinCard(
                    tile = station.toPinCardTile(emptyList()),
                    isOnHome = true,
                    onToggleHome = {},
                    onDirectionsClick = {},
                    onClose = {},
                    pickAction = parkRidePickActionFor(station.stationName, goHereStopId, role),
                )
            }
        }
    }
}
