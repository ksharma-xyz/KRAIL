package xyz.ksharma.krail.trip.planner.ui.intro.v2

import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideAvailabilityState
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideCarPark
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideCardPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideCardStation

// Real station and car park names with plain sample numbers, never a rider's data. Shared by the phone, the
// 320dp and the tablet previews of step 1, and by the layout tests that draw the same states.

private const val BELLA_VISTA = "Bella Vista"
private const val SAMPLE_TIME = "8:05\u00A0AM"

private val BellaVistaNames = persistentListOf(ParkRideCarPark(facilityId = "31", name = BELLA_VISTA))

private val BellaVistaReading = persistentListOf(
    ParkRideCarPark("31", BELLA_VISTA, spotsAvailable = 120, percentageFull = 40, timeText = SAMPLE_TIME),
)

private val TallawongNames = persistentListOf(
    ParkRideCarPark(facilityId = "26", name = "Tallawong P1"),
    ParkRideCarPark(facilityId = "27", name = "Tallawong P2"),
    ParkRideCarPark(facilityId = "28", name = "Tallawong P3"),
)

private val TallawongReadings = persistentListOf(
    ParkRideCarPark("26", "Tallawong P1", spotsAvailable = 120, percentageFull = 40, timeText = SAMPLE_TIME),
    ParkRideCarPark("27", "Tallawong P2", spotsAvailable = 48, percentageFull = 76, timeText = SAMPLE_TIME),
    ParkRideCarPark("28", "Tallawong P3", spotsAvailable = 210, percentageFull = 12, timeText = SAMPLE_TIME),
)

private fun asked(card: ParkRideCardPhase, added: Boolean = false) = IntroState(
    parkRideAnswer = ParkRideAnswer.YES,
    locationResult = LocationResult.GRANTED,
    parkRideCard = card,
    addedParkRide = added,
)

private fun bellaVista(availability: ParkRideAvailabilityState) = ParkRideCardPhase.Card(
    ParkRideCardStation(
        stationId = "2153478",
        stationName = BELLA_VISTA,
        carParks = if (availability == ParkRideAvailabilityState.Content) BellaVistaReading else BellaVistaNames,
        availability = availability,
    ),
)

private fun tallawong(availability: ParkRideAvailabilityState) = ParkRideCardPhase.Card(
    ParkRideCardStation(
        stationId = "2155384",
        stationName = "Tallawong",
        carParks = if (availability == ParkRideAvailabilityState.Content) TallawongReadings else TallawongNames,
        availability = availability,
    ),
)

internal val PreviewAskState = IntroState()
internal val PreviewLocatingState = asked(ParkRideCardPhase.Locating())
internal val PreviewNoStationNearState = asked(ParkRideCardPhase.NoStationNear)
internal val PreviewCardLoadingState = asked(bellaVista(ParkRideAvailabilityState.Loading))
internal val PreviewCardContentState = asked(bellaVista(ParkRideAvailabilityState.Content))
internal val PreviewCardUnavailableState = asked(bellaVista(ParkRideAvailabilityState.Unavailable))
internal val PreviewAddedState = asked(bellaVista(ParkRideAvailabilityState.Content), added = true)

// The tallest card the default catalogue gives, three car parks, for the narrow and wide frames.
internal val PreviewTallCardContentState = asked(tallawong(ParkRideAvailabilityState.Content))
internal val PreviewTallAddedState = asked(tallawong(ParkRideAvailabilityState.Content), added = true)
