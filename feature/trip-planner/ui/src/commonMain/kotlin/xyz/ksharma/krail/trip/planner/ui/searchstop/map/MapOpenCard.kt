package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState.ParkRideFacilityDetail
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.ParkRideMapCardUi
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent
import xyz.ksharma.krail.trip.planner.ui.stopcard.ParkRideStationCard
import xyz.ksharma.krail.trip.planner.ui.stopcard.nearbyStopFor

/** The one card a stop map has open: a station's own card, or the parking card its `P` opens. */
internal data class MapOpenCard(
    val stop: NearbyStopFeature,
    val isParking: Boolean,
)

/** The stops a map draws station markers for, and the card it has open. One card is open at a time. */
internal class MapCards(
    val stationStops: ImmutableList<NearbyStopFeature>,
    val open: MapOpenCard?,
    private val askedStation: MutableState<String?>,
    private val askedParking: MutableState<String?>,
) {
    /** The stop whose own card was asked for, which is what that card's saved state is keyed on. */
    val stationStopId: String? = askedStation.value
    val isOpen: Boolean = open != null
    val isStationCardOpen: Boolean = open != null && !open.isParking

    fun openStationCard(stopId: String) = ask(station = stopId, parking = null)

    /**
     * Asks for [stopId]'s parking card, and says whether that is a new open: false when it is the one already asked
     * for. It looks at what is asked for as it is called, as [closeIfAsked] does.
     */
    fun openParkingCard(stopId: String): Boolean {
        val isNew = askedParking.value != stopId
        ask(station = null, parking = stopId)
        return isNew
    }

    fun close() = ask(station = null, parking = null)

    /**
     * Closes the card that was asked for, and says whether there was one. It looks at what is asked for as it is called
     * and not as this was made, because the map keeps the tap handler it was first given.
     */
    fun closeIfAsked(): Boolean {
        val asked = askedStation.value != null || askedParking.value != null
        if (asked) close()
        return asked
    }

    private fun ask(station: String?, parking: String?) {
        askedStation.value = station
        askedParking.value = parking
    }
}

/**
 * Which card is open is held as a stop id because that is the part worth saving: the feature itself is map rendering
 * data the ViewModel still holds after the Activity is recreated, so it is looked up again rather than serialised. Held
 * in a plain `remember` this closed the card on every rotation, theme switch and font size change. A stop that has since
 * left the markers leaves its card closed.
 *
 * While the parking card is open the ViewModel is told through [onEvent], which is what loads its numbers.
 */
@Composable
internal fun rememberMapCards(
    nearby: List<NearbyStopFeature>,
    parkRide: List<NearbyStopFeature>,
    selectedProductClasses: Set<Int>,
    onEvent: (SearchStopUiEvent) -> Unit,
): MapCards {
    val askedStation = rememberSaveable { mutableStateOf<String?>(null) }
    val askedParking = rememberSaveable { mutableStateOf<String?>(null) }
    // A Park & Ride station has its marker even outside the nearby set, so its card is looked for there too.
    val stationStops = remember(nearby, parkRide, selectedProductClasses) {
        stationMarkerStops(nearby, parkRide, selectedProductClasses)
    }
    val station = remember(askedStation.value, stationStops) { nearbyStopFor(askedStation.value, stationStops) }
    val parking = remember(askedParking.value, parkRide) { nearbyStopFor(askedParking.value, parkRide) }
    ParkRideCardEffect(stopId = parking?.stopId, onEvent = onEvent)
    return MapCards(
        stationStops = stationStops,
        open = openCardFor(station = station, parking = parking),
        askedStation = askedStation,
        askedParking = askedParking,
    )
}

private fun openCardFor(station: NearbyStopFeature?, parking: NearbyStopFeature?): MapOpenCard? = when {
    parking != null -> MapOpenCard(parking, isParking = true)
    station != null -> MapOpenCard(station, isParking = false)
    else -> null
}

/**
 * The stops the map draws a station marker for: the [nearby] ones, and every Park & Ride station of [parkRide] that is not
 * among them and has a mode the chips still show, however far away, so that each `P` has its station beside it.
 */
internal fun stationMarkerStops(
    nearby: List<NearbyStopFeature>,
    parkRide: List<NearbyStopFeature>,
    selectedProductClasses: Set<Int>,
): ImmutableList<NearbyStopFeature> {
    val nearbyIds = nearby.mapTo(HashSet()) { it.stopId }
    val farStations = parkRide.filter { station ->
        station.stopId !in nearbyIds &&
            station.transportModes.any { NswTransportConfig.productClassFor(it) in selectedProductClasses }
    }
    return (nearby + farStations).toImmutableList()
}

/** The card to draw: the [open] one, or the one that was, so that a dismissed card slides away still showing it. */
@Composable
internal fun rememberShownCard(open: MapOpenCard?): MapOpenCard? {
    var last by remember { mutableStateOf<MapOpenCard?>(null) }
    LaunchedEffect(open) { if (open != null) last = open }
    return open ?: last
}

/** The parking card of [stop], with the numbers in [live]. They are held through the card's exit, as the card itself is. */
@Composable
internal fun ShownParkingCard(
    stop: NearbyStopFeature,
    live: ParkRideMapCardUi?,
    onEvent: (SearchStopUiEvent) -> Unit,
    onClose: () -> Unit,
    // The rider tapped directions to this car park.
    onDirections: (facilityId: String) -> Unit = {},
) {
    var last by remember(stop.stopId) { mutableStateOf<ParkRideMapCardUi?>(null) }
    LaunchedEffect(live) { if (live != null) last = live }
    val shown = listOfNotNull(live, last).firstOrNull { it.stationId == stop.stopId }
    ParkRideStationCard(
        stationName = stop.stopName,
        carParks = shown?.carParks ?: persistentListOf(),
        isLoading = shown?.isLoading ?: true,
        onDirectionsClick = { carPark ->
            carPark.directionsEvent()?.let { event ->
                onDirections(carPark.facilityId)
                onEvent(event)
            }
        },
        onClose = onClose,
    )
}

private fun ParkRideFacilityDetail.directionsEvent(): SearchStopUiEvent.ParkRideDirectionsClicked? {
    val toLatitude = latitude ?: return null
    val toLongitude = longitude ?: return null
    return SearchStopUiEvent.ParkRideDirectionsClicked(toLatitude, toLongitude, facilityName)
}

/**
 * Tells the ViewModel while a parking card is on screen, so it loads that station's numbers, and again when it has gone,
 * whether it was closed, replaced or left with the screen.
 */
@Composable
private fun ParkRideCardEffect(stopId: String?, onEvent: (SearchStopUiEvent) -> Unit) {
    val currentOnEvent by rememberUpdatedState(onEvent)
    DisposableEffect(stopId) {
        if (stopId != null) currentOnEvent(SearchStopUiEvent.ParkRideCardOpened(stopId))
        onDispose { if (stopId != null) currentOnEvent(SearchStopUiEvent.ParkRideCardClosed) }
    }
}
