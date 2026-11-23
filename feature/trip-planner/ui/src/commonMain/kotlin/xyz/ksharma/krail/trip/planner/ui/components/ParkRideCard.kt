package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableSet
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.cardBackground
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState

private val ParkRideCardVerticalPadding = 20.dp // no token equivalent

/**
 * The Park & Ride card.
 *
 * [keepReadingSpace] is for a surface that shows the card before its numbers have arrived or when
 * they never will: an open card then lists every car park by name, and one with no reading (spots
 * below zero) holds the lines its numbers will take, so the card keeps one height whether the
 * numbers are loading, shown or missing. It is off for the home card, which shows Loading until a
 * reading exists.
 *
 * A null [onClick] draws a card that is not tappable, for a surface where the card only shows and the
 * action is somewhere else, such as the Intro, whose bottom bar adds the station.
 *
 * [iconColor] is the fill of the `P` tile, for a surface drawn in a colour of its own, such as the Intro. It is the theme colour
 * when null, as it is on the home card. The tint of the card is always the theme's, so a surface that has its own colour gives
 * the card that colour as the theme (`LocalThemeColor`) and not as a parameter.
 *
 * [textWeight] is the weight of the lines that are Regular on the home card (the car park name, the words beside the figures and
 * `Last updated`), for a surface that reads them a step heavier, such as the Intro. The figures stay Bold, and every colour and size
 * stay as they are. It is null on the home card, which keeps the weights it has.
 */
@Composable
fun ParkRideCard(
    parkRideUiState: ParkRideUiState,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = {},
    keepReadingSpace: Boolean = false,
    iconColor: Color? = null,
    textWeight: FontWeight? = null,
) {
    val dim = KrailTheme.dimensions
    Row(
        modifier = modifier.fillMaxWidth()
            .cardBackground()
            .then(if (onClick != null) Modifier.klickable { onClick() } else Modifier)
            .animateContentSize()
            .padding(
                top = ParkRideCardVerticalPadding,
                start = dim.pageHorizontalPadding,
                end = dim.spacingL,
                bottom = ParkRideCardVerticalPadding,
            ),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingXL),
    ) {
        ParkRideIcon(modifier = Modifier.padding(top = dim.spacingXXS), backgroundColor = iconColor)

        Column(modifier = Modifier.fillMaxWidth()) {
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(spring(stiffness = Spring.StiffnessVeryLow)),
                exit = shrinkVertically() + fadeOut(spring(stiffness = Spring.StiffnessVeryLow)),
            ) {
                if (keepReadingSpace || parkRideUiState.facilities.any { it.spotsAvailable >= 0 }) {
                    Column(verticalArrangement = Arrangement.spacedBy(ParkRideCardVerticalPadding)) {
                        parkRideUiState.facilities.forEach { facility ->
                            Column(
                                verticalArrangement = Arrangement.spacedBy(dim.spacingXS),
                            ) {
                                // Facility Name
                                Text(
                                    text = facility.facilityName,
                                    style = KrailTheme.typography.displaySmall.copy(
                                        fontWeight = textWeight ?: FontWeight.Normal,
                                    ),
                                )

                                if (facility.spotsAvailable >= 0 || !keepReadingSpace) {
                                    ParkRideFigures(
                                        spots = "${facility.spotsAvailable}",
                                        percentFull = "${facility.percentageFull}%",
                                        textWeight = textWeight,
                                    )

                                    // Time Information
                                    if (facility.timeText.isNotBlank()) {
                                        ParkRideUpdatedAt(timeText = facility.timeText, textWeight = textWeight)
                                    }
                                } else {
                                    ParkRideReadingPlaceholder(textWeight = textWeight)
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Loading...",
                        style = KrailTheme.typography.bodyMedium.withWeight(textWeight),
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
                    style = KrailTheme.typography.displaySmall.copy(fontWeight = textWeight ?: FontWeight.Normal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ParkRideFigures(spots: String, percentFull: String, textWeight: FontWeight?) {
    val dim = KrailTheme.dimensions
    // Spots Information
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(dim.spacingML),
        verticalArrangement = Arrangement.spacedBy(dim.spacingXS),
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(dim.spacingXS)) {
            Text(
                text = spots,
                style = KrailTheme.typography.headlineMedium,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                text = "spots available",
                style = KrailTheme.typography.bodyMedium.withWeight(textWeight),
                modifier = Modifier.alignByBaseline(),
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dim.spacingXS),
        ) {
            Text(
                text = percentFull,
                style = KrailTheme.typography.headlineMedium,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                text = "full",
                style = KrailTheme.typography.bodyMedium.withWeight(textWeight),
                modifier = Modifier.alignByBaseline(),
            )
        }
    }
}

@Composable
private fun ParkRideUpdatedAt(timeText: String, textWeight: FontWeight?) {
    Text(
        text = "Last updated at\u00A0$timeText",
        style = KrailTheme.typography.bodySmall.withWeight(textWeight),
        color = KrailTheme.colors.secondaryLabel,
    )
}

/** The lines a reading takes, drawn with nothing in them and hidden from a screen reader. */
@Composable
private fun ParkRideReadingPlaceholder(textWeight: FontWeight?) {
    Column(
        modifier = Modifier.alpha(0f).clearAndSetSemantics {},
        verticalArrangement = Arrangement.spacedBy(KrailTheme.dimensions.spacingXS),
    ) {
        ParkRideFigures(spots = "000", percentFull = "00%", textWeight = textWeight)
        ParkRideUpdatedAt(timeText = "0:00\u00A0AM", textWeight = textWeight)
    }
}

@ScreenshotTest
@Preview
@Composable
private fun ParkRideCardPreview() {
    PreviewTheme(KrailThemeStyle.BarbiePink) {
        ParkRideCard(
            parkRideUiState = previewParkRideUiState,
            isExpanded = false,
            onClick = { },
        )
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
        ),
    ).toImmutableSet(),
    stopId = "12345",
    isLoading = false,
    error = null,
)
