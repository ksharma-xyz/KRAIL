package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import xyz.ksharma.krail.park.ride.ui.components.ParkRideFacilityRow
import xyz.ksharma.krail.taj.components.Text
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

internal const val NO_STATION_NEAR_TEXT = "No car parks near you"
internal const val ADD_TO_HOME_TEXT = "Add to home"
internal const val ADDED_TO_HOME_TEXT = "Added to home"

/** Tags the slot the card states share, so a test can read the height they all keep. */
internal const val INTRO_CARD_SLOT_TAG = "intro.parkride.slot"

/** What the space below the answers holds. One of these for every [ParkRideCardPhase]. */
internal sealed interface IntroCardSlot {
    /** Nothing, because the question is not answered or the answer gave no card. */
    data object Empty : IntroCardSlot

    /** A card with no text yet, while the location and the stops are awaited. */
    data object Skeleton : IntroCardSlot

    /** One plain line, in the room the card would have taken. */
    data object NoStationNear : IntroCardSlot

    /** The nearest station, with the numbers when it has them, and the Add to home row. */
    data class Station(val card: ParkRideUiState, val added: Boolean) : IntroCardSlot
}

/** Added is the same card with another row, so it is [IntroState.addedParkRide] and not a phase. */
internal fun introCardSlot(state: IntroState): IntroCardSlot = when (val phase = state.parkRideCard) {
    ParkRideCardPhase.None -> IntroCardSlot.Empty
    is ParkRideCardPhase.Locating -> IntroCardSlot.Skeleton
    ParkRideCardPhase.NoStationNear -> IntroCardSlot.NoStationNear
    is ParkRideCardPhase.Card -> IntroCardSlot.Station(phase.station.toParkRideUiState(), state.addedParkRide)
}

internal fun addToHomeText(added: Boolean): String = if (added) ADDED_TO_HOME_TEXT else ADD_TO_HOME_TEXT

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
 * The room under the answers. Every state that has a card keeps the same lines, a name, the
 * numbers and the row, so the card does not change height when the numbers arrive or fail.
 */
@Composable
internal fun IntroParkRideCardSlot(slot: IntroCardSlot, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.testTag(INTRO_CARD_SLOT_TAG)) {
        when (slot) {
            IntroCardSlot.Empty -> Unit
            // A card with no text, and no row until there is a station to add.
            IntroCardSlot.Skeleton -> IntroParkRideCard(SkeletonCard, added = false, standIn = true, showRow = false)
            IntroCardSlot.NoStationNear -> {
                // The same card, drawn invisibly, holds the room, so the line takes the card's height.
                IntroParkRideCard(SkeletonCard, added = false, standIn = true, showCard = false, showRow = false)
                Text(
                    text = NO_STATION_NEAR_TEXT,
                    style = KrailTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = KrailTheme.dimensions.pageHorizontalPadding),
                )
            }
            is IntroCardSlot.Station -> IntroParkRideCard(slot.card, slot.added, onToggle = onToggle)
        }
    }
}

/**
 * The shared card and the shared Add row. A [standIn] is a picture of a card, so it names nothing
 * and does nothing, and what it does not [showCard] or [showRow] still takes its room.
 */
@Composable
private fun IntroParkRideCard(
    card: ParkRideUiState,
    added: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit = {},
    standIn: Boolean = false,
    showCard: Boolean = true,
    showRow: Boolean = true,
) {
    val dim = KrailTheme.dimensions
    Column(modifier = if (standIn) modifier.clearAndSetSemantics {} else modifier) {
        ParkRideCard(
            parkRideUiState = card,
            isExpanded = true,
            keepReadingSpace = true,
            onClick = onToggle,
            modifier = Modifier
                .padding(horizontal = dim.pageHorizontalPadding)
                .alpha(if (showCard) 1f else 0f),
        )
        ParkRideFacilityRow(
            facilityName = addToHomeText(added),
            added = added,
            onClick = onToggle,
            icon = {},
            showDivider = false,
            modifier = Modifier.alpha(if (showRow) 1f else 0f),
        )
    }
}
