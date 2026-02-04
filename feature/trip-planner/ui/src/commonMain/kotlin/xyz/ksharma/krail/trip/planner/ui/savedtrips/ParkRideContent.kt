package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.taj.themeColor
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState.ParkRideFacilityDetail

@Composable
fun ParkRideLoadedContent(
    parkRideFacilityDetail: ParkRideFacilityDetail,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color = themeBackgroundColor())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Facility Name
            Text(
                text = parkRideFacilityDetail.facilityName,
                style = KrailTheme.typography.displayMedium.copy(fontWeight = FontWeight.Normal),
                color = getForegroundColor(themeBackgroundColor()),
                modifier = Modifier,
            )

            // Spots Information
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${parkRideFacilityDetail.spotsAvailable}",
                        style = KrailTheme.typography.headlineLarge,
                        color = getForegroundColor(themeBackgroundColor()),
                        modifier = Modifier.alignByBaseline(),
                    )
                    Text(
                        text = "spots empty",
                        style = KrailTheme.typography.bodyMedium,
                        color = getForegroundColor(themeBackgroundColor()),
                        modifier = Modifier.alignByBaseline(),
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "${parkRideFacilityDetail.percentageFull}%",
                        style = KrailTheme.typography.headlineLarge,
                        color = getForegroundColor(themeBackgroundColor()),
                        modifier = Modifier.alignByBaseline(),
                    )
                    Text(
                        text = "full",
                        style = KrailTheme.typography.bodyMedium,
                        color = getForegroundColor(themeBackgroundColor()),
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }

            if (parkRideFacilityDetail.timeText.isNotBlank()) {
                Text(
                    text = "Last updated at\u00A0${parkRideFacilityDetail.timeText}",
                    style = KrailTheme.typography.bodySmall,
                    color = getForegroundColor(themeColor()),
                )
            }
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
    PreviewTheme(KrailThemeStyle.BarbiePink, darkTheme = true) {
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
