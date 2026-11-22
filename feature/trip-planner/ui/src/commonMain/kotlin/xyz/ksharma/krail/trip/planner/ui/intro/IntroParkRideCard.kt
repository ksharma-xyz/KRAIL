package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.components.ParkRideCard
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardStation
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState.ParkRideFacilityDetail

// The reading a car park has before one arrives, which the shared card reads as no numbers yet.
private const val NO_READING = -1

// Holds a line open where no text is yet, so the card has the height it will have with text.
private const val BLANK = "\u00A0"

/** Tags the slot the card states share, so a test can read the height they all keep. */
internal const val INTRO_CARD_SLOT_TAG = "intro.parkride.slot"

/**
 * What the middle of the Park & Ride screen holds. One of these for every [ParkRideCardPhase]. The card
 * has no action of its own: adding the station is the right button of the decision bar.
 */
internal sealed interface IntroCardSlot {
    /** Nothing, because the question is not answered or the answer gave no card. */
    data object Empty : IntroCardSlot

    /** A card with no text yet, while the location and the stops are awaited. */
    data object Skeleton : IntroCardSlot

    /** The nearest station, with the numbers when it has them. */
    data class Station(val card: ParkRideUiState) : IntroCardSlot
}

/**
 * No station near is [IntroCardSlot.Empty]: that answer leaves nothing to decide, so the screen is
 * passed over and says nothing. Added is the same card, because the bar and not the card carries the action.
 */
internal fun introCardSlot(state: IntroState): IntroCardSlot = when (val phase = state.parkRideCard) {
    ParkRideCardPhase.None, ParkRideCardPhase.NoStationNear -> IntroCardSlot.Empty
    is ParkRideCardPhase.Locating -> IntroCardSlot.Skeleton
    is ParkRideCardPhase.Card -> IntroCardSlot.Station(phase.station.toParkRideUiState())
}

/**
 * The station as the shared Park & Ride card draws it. A car park with no reading carries the
 * card's own no reading mark, so it shows as a name and holds its place for the numbers. A station
 * with no car park name at all is drawn as one car park named after the station, never as nothing.
 */
internal fun ParkRideCardStation.toParkRideUiState(): ParkRideUiState = ParkRideUiState(
    stopId = stationId,
    stopName = stationName,
    facilities = carParks
        .map { carPark ->
            ParkRideFacilityDetail(
                spotsAvailable = carPark.spotsAvailable ?: NO_READING,
                totalSpots = NO_READING,
                facilityName = carPark.name,
                percentageFull = carPark.percentageFull ?: NO_READING,
                stopId = stationId,
                timeText = carPark.timeText.orEmpty(),
                facilityId = carPark.facilityId,
            )
        }
        .ifEmpty { listOf(unnamedFacility(stationId, stationName)) }
        .toImmutableSet(),
)

private fun unnamedFacility(stationId: String, name: String) = ParkRideFacilityDetail(
    spotsAvailable = NO_READING,
    totalSpots = NO_READING,
    facilityName = name,
    percentageFull = NO_READING,
    stopId = stationId,
    timeText = "",
    facilityId = stationId,
)

private val SkeletonCard = ParkRideUiState(
    stopId = "",
    stopName = BLANK,
    facilities = persistentSetOf(
        ParkRideFacilityDetail(
            spotsAvailable = NO_READING,
            totalSpots = NO_READING,
            facilityName = BLANK,
            percentageFull = NO_READING,
            stopId = "",
            timeText = "",
            facilityId = "",
        ),
    ),
)

/**
 * The card in the middle of the screen. Every state that has a card keeps the same lines, a name and the
 * numbers, so the card does not change height when the numbers arrive or fail.
 */
@Composable
internal fun IntroParkRideCardSlot(slot: IntroCardSlot, modifier: Modifier = Modifier) {
    Box(modifier = modifier.testTag(INTRO_CARD_SLOT_TAG)) {
        when (slot) {
            IntroCardSlot.Empty -> Unit
            IntroCardSlot.Skeleton -> IntroParkRideCard(SkeletonCard, standIn = true)
            is IntroCardSlot.Station -> IntroParkRideCard(slot.card)
        }
    }
}

/** The shared card. A [standIn] is a picture of a card, so it names nothing and does nothing. */
@Composable
private fun IntroParkRideCard(card: ParkRideUiState, modifier: Modifier = Modifier, standIn: Boolean = false) {
    ParkRideCard(
        parkRideUiState = card,
        isExpanded = true,
        keepReadingSpace = true,
        onClick = null,
        modifier = modifier
            .padding(horizontal = KrailTheme.dimensions.pageHorizontalPadding)
            .then(if (standIn) Modifier.clearAndSetSemantics {} else Modifier),
    )
}
