package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableSet
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState

@Composable
fun ParkRideCard(
    parkRideUiState: ParkRideUiState,
    modifier: Modifier = Modifier,
    onClick: (Boolean) -> Unit = {},
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(themeBackgroundColor())
            .klickable {
                isExpanded = !isExpanded
                onClick(isExpanded)
            }
            .animateContentSize()
            .padding(top = 20.dp, start = 16.dp, end = 12.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ParkRideIcon(modifier = Modifier.padding(top = 2.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(spring(stiffness = Spring.StiffnessVeryLow)),
                exit = shrinkVertically() + fadeOut(spring(stiffness = Spring.StiffnessVeryLow)),
            ) {
                if (parkRideUiState.facilities.any { it.spotsAvailable >= 0 }) {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        parkRideUiState.facilities.forEach { facility ->
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                // Facility Name
                                Text(
                                    text = facility.facilityName,
                                    style = KrailTheme.typography.displaySmall.copy(fontWeight = FontWeight.Normal),
                                )

                                // Spots Information
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "${facility.spotsAvailable}",
                                            style = KrailTheme.typography.headlineLarge,
                                            modifier = Modifier.alignByBaseline(),
                                        )
                                        Text(
                                            text = "spots available",
                                            style = KrailTheme.typography.bodyMedium,
                                            modifier = Modifier.alignByBaseline(),
                                        )
                                    }

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(
                                            text = "${facility.percentageFull}%",
                                            style = KrailTheme.typography.headlineLarge,
                                            modifier = Modifier.alignByBaseline(),
                                        )
                                        Text(
                                            text = "full",
                                            style = KrailTheme.typography.bodyMedium,
                                            modifier = Modifier.alignByBaseline(),
                                        )
                                    }
                                }

                                // Time Information
                                if (facility.timeText.isNotBlank()) {
                                    Text(
                                        text = "Last updated at\u00A0${facility.timeText}",
                                        style = KrailTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Loading...",
                        style = KrailTheme.typography.bodyMedium,
                    )
                }
            }

            AnimatedVisibility(
                visible = !isExpanded,
                enter = fadeIn(spring(stiffness = Spring.StiffnessHigh)),
                exit = fadeOut(spring(stiffness = Spring.StiffnessHigh)),
            ) {
                Text(
                    text = parkRideUiState.stopName,
                    style = KrailTheme.typography.displaySmall.copy(fontWeight = FontWeight.Normal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview
@Composable
private fun ParkRideCardPreview() {
    PreviewTheme(KrailThemeStyle.BarbiePink) {
        ParkRideCard(previewParkRideUiState)
    }
}

private val previewParkRideUiState = ParkRideUiState(
    stopName = "Central Station",
    facilities = listOf(
        ParkRideUiState.ParkRideFacilityDetail(
            facilityName = "Park & Ride A",
            spotsAvailable = 50,
            percentageFull = 20,
            totalSpots = 100,
            stopId = "111",
            timeText = "8:00 AM",
            facilityId = "10",
        ),
        ParkRideUiState.ParkRideFacilityDetail(
            facilityName = "Park & Ride B",
            spotsAvailable = 10,
            percentageFull = 90,
            stopId = "112",
            timeText = "9:00 AM",
            facilityId = "10",
            totalSpots = 190,
        )
    ).toImmutableSet(),
    stopId = "12345",
    isLoading = false,
    error = null,
)