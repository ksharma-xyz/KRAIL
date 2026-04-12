package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.departures.ui.state.model.StopDeparture
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme

/**
 * Horizontally scrollable row of unique line badges for a stop, used as a departure filter.
 *
 * Each badge represents one service line (e.g. T1, T2, 333) derived from [departures].
 * Tapping a badge selects it as the active filter (highlighted with a thick onSurface border).
 * Tapping the currently selected badge deselects it (clears the filter).
 *
 * All badges use [BadgeSize.Large] so they meet accessibility touch-target guidelines and
 * are easy to distinguish from the small badges shown inside departure rows.
 *
 * This composable is **stateless** — callers drive [selectedLine] and [onLineSelect].
 * Typically the filter state lives in the nearest enclosing composable that owns the
 * departure list (e.g. [DepartureBoardCard] or `DepartureBoardStopContent`).
 *
 * @param departures    Full (unfiltered) departure list — only used to derive unique lines.
 * @param selectedLine  The currently active filter line number, or `null` for "show all".
 * @param onLineSelect Called with the tapped line number, or `null` when toggling off.
 */
@Composable
internal fun LinesServedRow(
    departures: ImmutableList<StopDeparture>,
    selectedLine: String?,
    onLineSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uniqueLines = remember(departures) {
        departures.distinctBy { it.lineNumber }
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.padding(bottom = 12.dp),
    ) {
        items(uniqueLines, key = { it.lineNumber }) { departure ->
            val isSelected = selectedLine == departure.lineNumber
            TransportModeBadge(
                badgeText = departure.lineNumber,
                backgroundColor = departure.lineColorCode.hexToComposeColor(),
                size = BadgeSize.Large,
                selected = isSelected,
                onClick = {
                    onLineSelect(if (isSelected) null else departure.lineNumber)
                },
            )
        }
    }
}

// region Previews

private const val TRANSPORT_MODE_TRAIN = "Train"

private val previewDepartures: ImmutableList<StopDeparture> = persistentListOf(
    StopDeparture(
        lineNumber = "T1",
        lineColorCode = "#F99D1C",
        transportModeName = TRANSPORT_MODE_TRAIN,
        destinationName = "Liverpool",
        departureTimeText = "11:30 AM",
        departureUtcDateTime = "2026-04-11T01:30:00Z",
    ),
    StopDeparture(
        lineNumber = "T2",
        lineColorCode = "#0098CD",
        transportModeName = TRANSPORT_MODE_TRAIN,
        destinationName = "Bondi Junction",
        departureTimeText = "11:33 AM",
        departureUtcDateTime = "2026-04-11T01:33:00Z",
    ),
    StopDeparture(
        lineNumber = "T4",
        lineColorCode = "#005AA3",
        transportModeName = TRANSPORT_MODE_TRAIN,
        destinationName = "Illawarra",
        departureTimeText = "11:36 AM",
        departureUtcDateTime = "2026-04-11T01:36:00Z",
    ),
)

@Preview(name = "No filter active")
@Composable
private fun LinesServedRowNoFilterPreview() {
    PreviewTheme(KrailThemeStyle.Train) {
        LinesServedRow(
            departures = previewDepartures,
            selectedLine = null,
            onLineSelect = {},
        )
    }
}

@Preview(name = "T2 selected")
@Composable
private fun LinesServedRowSelectedPreview() {
    PreviewTheme(KrailThemeStyle.Train) {
        LinesServedRow(
            departures = previewDepartures,
            selectedLine = "T2",
            onLineSelect = {},
        )
    }
}

// endregion
