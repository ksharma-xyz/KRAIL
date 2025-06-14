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
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.taj.themeColor
import xyz.ksharma.krail.trip.planner.ui.components.ParkRideIcon
import xyz.ksharma.krail.trip.planner.ui.components.ParkRideIconContainer
import xyz.ksharma.krail.trip.planner.ui.state.parkride.ParkRideState

@Composable
fun ParkRideLoadedContent(
    parkRideState: ParkRideState,
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
        if (parkRideState.displayParkRideIcon) {
            ParkRideIcon()
        } else {
            ParkRideIconContainer(backgroundColor = themeBackgroundColor(), content = {})
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Facility Name
            Text(
                text = parkRideState.facilityName,
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
                        text = "${parkRideState.spotsAvailable}",
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
                        text = "${parkRideState.percentageFull}%",
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

            if (parkRideState.timeText.isNotBlank()) {
                Text(
                    text = "Last updated at\u00A0${parkRideState.timeText}",
                    style = KrailTheme.typography.bodySmall,
                    color = getForegroundColor(themeColor()),
                )
            }
        }
    }
}

// region Previews

val parkRideStatePreview = ParkRideState(
    facilityName = "Tallawong P1",
    spotsAvailable = 120,
    percentageFull = 60,
    timeText = "2 min ago",
    totalSpots = 800,
    stopId = "212000",
    displayParkRideIcon = true,
)

@Preview
@Composable
fun ParkRideLoadedContentPreview_Default() {
    PreviewTheme(KrailThemeStyle.Train) {
        ParkRideLoadedContent(
            parkRideState = parkRideStatePreview,
        )
    }
}

@Preview
@Composable
fun ParkRideLoadedContentPreview_Full() {
    PreviewTheme(KrailThemeStyle.Metro) {
        ParkRideLoadedContent(
            parkRideState = parkRideStatePreview.copy(
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
            parkRideState = parkRideStatePreview.copy(
                spotsAvailable = 130,
                percentageFull = 10,
                timeText = "Just now"
            ),
        )
    }
}

// endregion
