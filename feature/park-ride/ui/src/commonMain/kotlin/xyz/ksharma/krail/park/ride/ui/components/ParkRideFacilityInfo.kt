package xyz.ksharma.krail.park.ride.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme

data class ParkRideFacilityInfo(
    val facilityName: String,
    val availableSpots: String,
    val fullPercentage: String,
)

@Composable
fun ParkRideFacilityItem(
    parkRideFacilityInfo: ParkRideFacilityInfo,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ParkRideInfoText(
                number = parkRideFacilityInfo.availableSpots,
                description = "spots available",
            )

            ParkRideInfoText(
                number = parkRideFacilityInfo.availableSpots,
                description = "full",
            )
        }

        Text(text = parkRideFacilityInfo.facilityName, style = KrailTheme.typography.labelLarge)
    }
}

@Composable
fun ParkRideInfoText(
    number: String,
    description: String,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = number, style = KrailTheme.typography.headlineMedium,
            modifier = Modifier.alignByBaseline(),
        )
        Text(
            text = description,
            style = KrailTheme.typography.bodyLarge,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

@Preview
@Composable
private fun ParkRideInfoPreview() {
    KrailTheme {
        Column(modifier = Modifier.background(color = Color.White)) {
            ParkRideFacilityItem(
                parkRideFacilityInfo = ParkRideFacilityInfo(
                    facilityName = "Tallawong P1",
                    availableSpots = "100",
                    fullPercentage = "50%",
                )
            )
        }
    }
}
