package xyz.ksharma.krail.park.ride.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import krail.feature.park_ride.ui.generated.resources.Res
import krail.feature.park_ride.ui.generated.resources.ic_car
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle

data class ParkRideFacilityInfo(
    val facilityName: String,
    val availableSpots: String,
    val fullPercentage: String,
)

@Composable
fun ParkRideInfoCard(
    parkRideInfo: ImmutableMap<String, ImmutableList<ParkRideFacilityInfo>>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ParkAndRideIcon()

            Column(modifier = Modifier.weight(1f)) {
                parkRideInfo.entries.forEachIndexed { index, (stopId, parkRideFacilityList) ->

                    parkRideFacilityList.forEach { parkRideFacilityInfo ->
                        ParkRideFacilityItem(
                            parkRideFacilityInfo = parkRideFacilityInfo,
                            modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                        )
                    }

                    if (index != parkRideInfo.size - 1) {
                        Divider(modifier = Modifier.padding(vertical = 4.dp).padding(end = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ParkAndRideIcon() {
    Box(
        modifier = Modifier
            .size(height = 44.dp, width = 32.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color = Color.White)
    ) {
        Text(
            text = "P",
            style = KrailTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = KrailThemeStyle.Metro.hexColorCode.hexToComposeColor(),
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        )

        Image(
            painter = painterResource(Res.drawable.ic_car),
            contentDescription = null,
            colorFilter = ColorFilter.tint(KrailThemeStyle.Metro.hexColorCode.hexToComposeColor()),
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.Center)
                .padding(start = 8.dp, top = 8.dp),
        )
    }
}

@Preview
@Composable
private fun ParkRideInfoCardPreview() {
    KrailTheme {
        Column(
            modifier = Modifier
                .background(
                    color = KrailThemeStyle.Metro.hexColorCode.hexToComposeColor(),
                ),
        ) {
            ParkRideInfoCard(
                parkRideInfo = mapOf(
                    "211" to listOf(
                        ParkRideFacilityInfo(
                            facilityName = "Tallawong P1",
                            availableSpots = "100",
                            fullPercentage = "50%",
                        ),
                        ParkRideFacilityInfo(
                            facilityName = "Tallawong P2",
                            availableSpots = "200",
                            fullPercentage = "75%",
                        ),
                    ).toImmutableList(),
                    "212" to listOf(
                        ParkRideFacilityInfo(
                            facilityName = "Hornsby P1",
                            availableSpots = "100",
                            fullPercentage = "50%",
                        ),
                        ParkRideFacilityInfo(
                            facilityName = "Hornsby P2",
                            availableSpots = "200",
                            fullPercentage = "75%",
                        ),
                    ).toImmutableList()
                ).toImmutableMap(),
            )
        }
    }
}

@Composable
fun ParkRideFacilityItem(
    parkRideFacilityInfo: ParkRideFacilityInfo,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
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
                number = parkRideFacilityInfo.fullPercentage,
                description = "full",
            )
        }

        Text(
            text = parkRideFacilityInfo.facilityName,
            style = KrailTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
        )
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
            style = KrailTheme.typography.labelMedium,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

@Preview
@Composable
private fun ParkRideFacilityItemPreview() {
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
