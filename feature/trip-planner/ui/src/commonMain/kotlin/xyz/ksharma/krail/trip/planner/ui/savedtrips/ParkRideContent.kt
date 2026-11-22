package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.cardBackground
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState.ParkRideFacilityDetail

/**
 * The availability block for one car park.
 *
 * Colours and type scale deliberately mirror [ParkRideCard]: the same facility shows on the
 * home card and in the map sheet, and recolouring the text against the container made the two
 * look like different features reporting the same numbers.
 */
@Composable
fun ParkRideLoadedContent(
    parkRideFacilityDetail: ParkRideFacilityDetail,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    Row(
        modifier = modifier
            .fillMaxWidth()
            .cardBackground()
            .padding(horizontal = dim.spacingXL, vertical = dim.spacingL),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingXL),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dim.spacingXS),
        ) {
            // Facility Name
            Text(
                text = parkRideFacilityDetail.facilityName,
                style = KrailTheme.typography.displaySmall.copy(fontWeight = FontWeight.Normal),
            )

            CarParkFigures(
                spotsAvailable = parkRideFacilityDetail.spotsAvailable,
                percentageFull = parkRideFacilityDetail.percentageFull,
                timeText = parkRideFacilityDetail.timeText,
            )
        }
    }
}

/**
 * The numbers of one car park: the free spots, the percent full and the time of the reading, in the
 * type the card and the map sheet both use. Shared with the open Park & Ride tile on home, so a car
 * park reads the same wherever it is shown. Whole items wrap and nothing is squeezed.
 */
@Composable
internal fun CarParkFigures(
    spotsAvailable: Int,
    percentageFull: Int?,
    timeText: String?,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(dim.spacingXS)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dim.spacingML),
            verticalArrangement = Arrangement.spacedBy(dim.spacingXS),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(dim.spacingXS)) {
                Text(
                    text = "$spotsAvailable",
                    style = KrailTheme.typography.headlineMedium,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    text = "spots available",
                    style = KrailTheme.typography.bodyMedium,
                    modifier = Modifier.alignByBaseline(),
                )
            }

            if (percentageFull != null) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(dim.spacingXS)) {
                    Text(
                        text = "$percentageFull%",
                        style = KrailTheme.typography.headlineMedium,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Text(
                        text = "full",
                        style = KrailTheme.typography.bodyMedium,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
        }

        if (!timeText.isNullOrBlank()) {
            Text(
                text = "Last updated at\u00A0$timeText",
                style = KrailTheme.typography.bodySmall,
                color = KrailTheme.colors.secondaryLabel,
            )
        }
    }
}

// region Previews

val parkRideFacilityDetailPreview = ParkRideFacilityDetail(
    facilityName = "Tallawong P1",
    spotsAvailable = 120,
    percentageFull = 60,
    timeText = "2 min ago",
    totalSpots = 800,
    stopId = "212000",
    facilityId = "1234567890",
)

@Preview
@Composable
fun ParkRideLoadedContentPreview_Default() {
    PreviewTheme(KrailThemeStyle.Train) {
        ParkRideLoadedContent(
            parkRideFacilityDetail = parkRideFacilityDetailPreview,
        )
    }
}

@Preview
@Composable
fun ParkRideLoadedContentPreview_Full() {
    PreviewTheme(KrailThemeStyle.Metro) {
        ParkRideLoadedContent(
            parkRideFacilityDetail = parkRideFacilityDetailPreview.copy(
                spotsAvailable = 0,
                percentageFull = 100,
                timeText = "Just now",
            ),
        )
    }
}

@Preview
@Composable
fun ParkRideLoadedContentPreview_Barbie() {
    PreviewTheme(KrailThemeStyle.BarbiePink) {
        ParkRideLoadedContent(
            parkRideFacilityDetail = parkRideFacilityDetailPreview.copy(
                spotsAvailable = 130,
                percentageFull = 10,
                timeText = "Just now",
            ),
        )
    }
}

// endregion
