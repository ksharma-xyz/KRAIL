package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.LocalContentAlpha
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState

/**
 * Displays a coloured dot + label indicating whether a service is on time, late, or early.
 *
 * Mirrors the real-time deviation chip shown inside [JourneyCard].
 */
@Composable
fun DepartureDeviationIndicator(
    deviation: TimeTableState.JourneyCardInfo.DepartureDeviation,
    modifier: Modifier = Modifier,
) {
    val (dotColor, label) = when (deviation) {
        is TimeTableState.JourneyCardInfo.DepartureDeviation.Late ->
            KrailTheme.colors.deviationLate to deviation.text

        is TimeTableState.JourneyCardInfo.DepartureDeviation.Early ->
            KrailTheme.colors.deviationEarly to deviation.text

        TimeTableState.JourneyCardInfo.DepartureDeviation.OnTime ->
            KrailTheme.colors.deviationOnTime to "On time"
    }
    DepartureDeviationIndicator(dotColor = dotColor, label = label, modifier = modifier)
}

/**
 * Low-level overload — optional coloured dot + label text.
 *
 * @param showDot When false the dot is hidden, useful when an external badge
 *                already conveys the status (e.g. Live / Scheduled in [DepartureRow]).
 */
@Composable
fun DepartureDeviationIndicator(
    dotColor: Color,
    label: String,
    modifier: Modifier = Modifier,
    showDot: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showDot) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        color = dotColor.copy(alpha = LocalContentAlpha.current),
                        shape = CircleShape,
                    ),
            )
        }
        Text(
            text = label,
            style = KrailTheme.typography.bodyMedium,
            color = KrailTheme.colors.onSurface,
        )
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@PreviewComponent
@Composable
private fun DepartureDeviationIndicatorOnTimePreview() {
    PreviewTheme {
        DepartureDeviationIndicator(
            deviation = TimeTableState.JourneyCardInfo.DepartureDeviation.OnTime,
        )
    }
}

@Preview
@Composable
private fun DepartureDeviationIndicatorLatePreview() {
    PreviewTheme {
        DepartureDeviationIndicator(
            deviation = TimeTableState.JourneyCardInfo.DepartureDeviation.Late("3 mins late"),
        )
    }
}

@Preview
@Composable
private fun DepartureDeviationIndicatorEarlyPreview() {
    PreviewTheme {
        DepartureDeviationIndicator(
            deviation = TimeTableState.JourneyCardInfo.DepartureDeviation.Early("1 min early"),
        )
    }
}

@Preview
@Composable
private fun DepartureDeviationIndicatorNoDotPreview() {
    PreviewTheme {
        DepartureDeviationIndicator(
            dotColor = KrailTheme.colors.deviationOnTime,
            label = "in 3 mins",
            showDot = false,
        )
    }
}
