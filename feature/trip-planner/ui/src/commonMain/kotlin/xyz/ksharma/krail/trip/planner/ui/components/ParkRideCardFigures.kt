package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme

// The lines of one car park's reading on the Park & Ride card: the spots free, the share full and the time.

@Composable
internal fun ParkRideFigures(
    spots: String,
    percentFull: String,
    textWeight: FontWeight?,
    spotsBeat: Modifier = Modifier,
    percentBeat: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(dim.spacingML),
        verticalArrangement = Arrangement.spacedBy(dim.spacingXS),
    ) {
        ParkRideSpotsFigure(spots = spots, textWeight = textWeight, modifier = spotsBeat)
        ParkRidePercentFigure(percentFull = percentFull, textWeight = textWeight, modifier = percentBeat)
    }
}

@Composable
internal fun ParkRideSpotsFigure(spots: String, textWeight: FontWeight?, modifier: Modifier = Modifier) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(KrailTheme.dimensions.spacingXS)) {
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
}

@Composable
internal fun ParkRidePercentFigure(percentFull: String, textWeight: FontWeight?, modifier: Modifier = Modifier) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(KrailTheme.dimensions.spacingXS)) {
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

@Composable
internal fun ParkRideUpdatedAt(timeText: String, textWeight: FontWeight?, modifier: Modifier = Modifier) {
    Text(
        text = "Last updated at\u00A0$timeText",
        style = KrailTheme.typography.bodySmall.withWeight(textWeight),
        color = KrailTheme.colors.secondaryLabel,
        modifier = modifier,
    )
}
