package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme

/**
 * The numbers of one car park: the free spots, the percent full and the time of the reading, in the
 * type the home card uses. Shared with the open Park & Ride tile on home, so a car
 * park reads the same wherever it is shown. Whole items wrap and nothing is squeezed.
 */
@Composable
internal fun CarParkFigures(
    spotsAvailable: Int,
    percentageFull: Int?,
    timeText: String?,
    modifier: Modifier = Modifier,
    // The time's colour: the card's own secondary text, or the fixed quieter light on a card that floats on the map.
    secondaryColor: Color = KrailTheme.colors.secondaryLabel,
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
                color = secondaryColor,
            )
        }
    }
}
