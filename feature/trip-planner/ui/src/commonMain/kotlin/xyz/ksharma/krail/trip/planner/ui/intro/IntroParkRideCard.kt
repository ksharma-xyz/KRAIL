package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.components.ParkRideCard
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
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

    /** No station within the wider distance either, said in words in the card's room. The bar's OK moves on. */
    data object NoStation : IntroCardSlot
}

/** What the card's room says when no station is near. It is the whole content of the room, and the bar answers it with OK. */
internal const val PARK_RIDE_NO_STATION = "No Park & Ride stations near you"

/**
 * No station near is [IntroCardSlot.NoStation], the message the rider answers with OK. No card with nothing to say, a
 * location that could not be used, is [IntroCardSlot.Empty] and the screen is passed over. Added is the same card, because
 * the bar and not the card carries the action.
 */
internal fun introCardSlot(state: IntroState): IntroCardSlot = when (val phase = state.parkRideCard) {
    ParkRideCardPhase.None -> IntroCardSlot.Empty
    ParkRideCardPhase.NoStationNear -> IntroCardSlot.NoStation
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
            IntroCardSlot.NoStation -> IntroParkRideNoStation()
        }
    }
}

// The message in the card's room, in the type and colour of the stop screens' no match line, centred, and read out when it
// appears because the card the rider was watching is what it replaced. The card's own room is drawn clear under it, so the
// screen keeps the height it had while the card was looked for and nothing moves when the message replaces it.
@Composable
private fun IntroParkRideNoStation() {
    Box(contentAlignment = Alignment.Center) {
        IntroParkRideCard(SkeletonCard, standIn = true, modifier = Modifier.alpha(0f))
        Text(
            text = PARK_RIDE_NO_STATION,
            style = KrailTheme.typography.titleLarge,
            color = KrailTheme.colors.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KrailTheme.dimensions.pageHorizontalPadding)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
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
        // The tile is the screen's button fill, because the white letter is not legible on the raw theme colour. The tint of
        // the card is the screen's colour family, which the screen provides as its theme (IntroScreenColours).
        iconColor = introPrimaryFill(IntroStep.PARK_RIDE),
        modifier = modifier
            .padding(horizontal = KrailTheme.dimensions.pageHorizontalPadding)
            .then(if (standIn) Modifier.clearAndSetSemantics {} else Modifier),
    )
}
