package xyz.ksharma.krail.trip.planner.ui.stopcard

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.core.adaptiveui.rememberAdaptiveLayoutInfo
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.motion.isReduceMotionEnabled
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.ParkRideCarPark
import xyz.ksharma.krail.trip.planner.ui.parkride.map.rememberParkRideMarkerPainter
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState.ParkRideFacilityDetail

/**
 * The card a station's `P` opens on a stop map: the station's parking and nothing else. It is the station card's shell, in
 * the app's mode, with the `P` where the station card has its mode badges, and each car park laid out as on the home card,
 * with its Directions beside it. The station's departures and its pick action are on the station's own card.
 *
 * In a short window, such as a phone on its side, the `P` is left out of the header: there the card has little height,
 * and its line would take the room of the first car park's numbers.
 */
@Composable
internal fun ParkRideStationCard(
    stationName: String,
    carParks: ImmutableList<ParkRideFacilityDetail>,
    isLoading: Boolean,
    onDirectionsClick: (ParkRideFacilityDetail) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val showsLogo = !rememberAdaptiveLayoutInfo().isCompactHeight
    val logo: @Composable ColumnScope.() -> Unit = {
        Image(
            painter = rememberParkRideMarkerPainter(),
            contentDescription = "Park & Ride",
            modifier = Modifier.size(dim.iconL),
        )
    }
    StopCardFrame(
        announcement = "$stationName parking",
        closeLabel = "Close $stationName parking",
        onClose = onClose,
        modifier = modifier,
        heading = {
            Text(
                text = stationName,
                style = StopCardType.title,
                modifier = Modifier.testTag(TripPlannerTestTags.StopCard.TITLE).semantics { heading() },
            )
        },
        headerContent = logo.takeIf { showsLogo },
    ) {
        Column(
            // Smooth, so that room held for a reading that never came does not go with a jump.
            modifier = Modifier
                .fillMaxWidth()
                .cardInset()
                .then(if (isReduceMotionEnabled()) Modifier else Modifier.animateContentSize()),
            verticalArrangement = Arrangement.spacedBy(dim.spacingXL),
        ) {
            carParks.forEach { carPark ->
                ParkRideCarPark(facility = carPark, isLoading = isLoading, onDirectionsClick = onDirectionsClick)
            }
        }
    }
}

// region Previews

private val PreviewCarParks = persistentListOf(
    ParkRideFacilityDetail(
        spotsAvailable = 60,
        totalSpots = 123,
        facilityName = "Tallawong P1",
        percentageFull = 51,
        stopId = "2155384",
        timeText = "8:12 PM",
        facilityId = "1",
        latitude = -33.69,
        longitude = 150.9,
    ),
    ParkRideFacilityDetail(
        spotsAvailable = -1,
        totalSpots = -1,
        facilityName = "Tallawong P2",
        percentageFull = -1,
        stopId = "2155384",
        timeText = "",
        facilityId = "2",
    ),
)

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewParkRideStationCard() {
    PreviewTheme {
        ParkRideStationCard(
            stationName = "Tallawong Station",
            carParks = PreviewCarParks,
            isLoading = true,
            onDirectionsClick = {},
            onClose = {},
        )
    }
}

// endregion
