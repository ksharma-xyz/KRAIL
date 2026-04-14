package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.taj.components.SeparatorIcon
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.trip.planner.ui.pastDepartureColor
import xyz.ksharma.krail.trip.planner.ui.pastDepartureTextStyle
import xyz.ksharma.krail.taj.theme.DEFAULT_THEME_STYLE
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeLine

/**
 * The top section of a [JourneyCard] that shows the departure time, transport mode icons/badges,
 * and the platform text. Adapts its layout based on font scale and the number of transport modes:
 *
 * - **Compact layout** (default): time + mode icons inline in a single `FlowRow`; platform text
 *   pinned to the trailing edge of the same row.
 * - **Expanded layout** (large font scale OR > 2 modes with a platform): time text on its own line,
 *   mode icons on a second line so they don't crowd the platform label.
 *
 * Click handling is owned by the parent [JourneyCard] — this composable has no clickable modifier.
 */
@Composable
internal fun JourneyCardHeader(
    transportModeLineList: ImmutableList<TransportModeLine>,
    platformText: String?,
    timeToDeparture: String,
    modifier: Modifier = Modifier,
) {
    val isPast by remember(timeToDeparture) {
        mutableStateOf(timeToDeparture.contains(other = "ago", ignoreCase = true))
    }
    val firstLegTransportModeColor = pastDepartureColor(
        isPast = isPast,
        activeColor = transportModeLineList.firstOrNull()?.lineColorCode?.hexToComposeColor()
            ?: KrailTheme.colors.onSurface,
    )

    Column(
        modifier = modifier.padding(bottom = 4.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (displayAdaptiveTransportModeList(transportModeLineList, platformText)) {
                Text(
                    text = timeToDeparture,
                    style = pastDepartureTextStyle(isPast, KrailTheme.typography.titleMedium),
                    color = firstLegTransportModeColor,
                    modifier = Modifier.padding(end = 8.dp),
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = timeToDeparture,
                        style = pastDepartureTextStyle(isPast, KrailTheme.typography.titleMedium),
                        color = firstLegTransportModeColor,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    TransportModesRow(
                        transportModeLineList = transportModeLineList,
                        showBadge = { it.transportMode is TransportMode.Bus },
                    )
                }
            }

            platformText?.let { text ->
                Text(
                    text = text,
                    textAlign = TextAlign.Center,
                    style = pastDepartureTextStyle(isPast, KrailTheme.typography.titleMedium),
                    color = firstLegTransportModeColor,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }

        if (displayAdaptiveTransportModeList(transportModeLineList, platformText)) {
            // On large font scale or with many modes, show badges/icons on their own line
            TransportModesRow(
                transportModeLineList = transportModeLineList,
                showBadge = { it.transportMode is TransportMode.Bus },
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

// Returns true when the transport mode list should be shown below the time text on its own line,
// rather than inline. Triggers on large font scale or when there are many modes with a platform.
@Composable
internal fun displayAdaptiveTransportModeList(
    transportModeLineList: ImmutableList<TransportModeLine>,
    platformText: String?,
): Boolean = isLargeFontScale() || transportModeLineList.size > 2 && platformText != null

@Composable
internal fun TransportModesRow(
    transportModeLineList: ImmutableList<TransportModeLine>,
    showBadge: (TransportModeLine) -> Boolean,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        transportModeLineList.forEachIndexed { index, line ->
            if (showBadge(line)) {
                TransportModeBadge(
                    backgroundColor = line.lineColorCode.hexToComposeColor(),
                    badgeText = line.lineName,
                )
            }
            TransportModeIcon(
                transportMode = line.transportMode,
                size = TransportModeIconSize.Small,
            )
            if (index != transportModeLineList.lastIndex) {
                SeparatorIcon(modifier = Modifier.align(Alignment.CenterVertically))
            }
        }
    }
}

// region Previews

@PreviewComponent
@Composable
private fun Preview_JourneyCardHeader_Train_WithPlatform() {
    PreviewTheme(themeStyle = DEFAULT_THEME_STYLE) {
        JourneyCardHeader(
            timeToDeparture = "in 5 mins",
            platformText = "Platform 3",
            transportModeLineList = persistentListOf(
                TransportModeLine(transportMode = TransportMode.Train, lineName = "T1"),
            ),
        )
    }
}

@PreviewComponent
@Composable
private fun Preview_JourneyCardHeader_Bus_WithBadge() {
    PreviewTheme(themeStyle = DEFAULT_THEME_STYLE) {
        JourneyCardHeader(
            timeToDeparture = "in 2 mins",
            platformText = "Stand A",
            transportModeLineList = persistentListOf(
                TransportModeLine(transportMode = TransportMode.Bus, lineName = "700"),
            ),
        )
    }
}

@PreviewComponent
@Composable
private fun Preview_JourneyCardHeader_MultiMode_Inline() {
    PreviewTheme(themeStyle = DEFAULT_THEME_STYLE) {
        JourneyCardHeader(
            timeToDeparture = "in 7 mins",
            platformText = "Platform 1",
            transportModeLineList = persistentListOf(
                TransportModeLine(transportMode = TransportMode.Bus, lineName = "700"),
                TransportModeLine(transportMode = TransportMode.Train, lineName = "T1"),
            ),
        )
    }
}

@PreviewComponent
@Composable
private fun Preview_JourneyCardHeader_ManyModes_WrappedLayout() {
    PreviewTheme(themeStyle = DEFAULT_THEME_STYLE) {
        JourneyCardHeader(
            timeToDeparture = "in 3 mins",
            platformText = "Platform 5",
            transportModeLineList = persistentListOf(
                TransportModeLine(transportMode = TransportMode.Bus, lineName = "333"),
                TransportModeLine(transportMode = TransportMode.Train, lineName = "T1"),
                TransportModeLine(transportMode = TransportMode.Ferry, lineName = "F1"),
                TransportModeLine(transportMode = TransportMode.Metro, lineName = "M1"),
            ),
        )
    }
}

@PreviewComponent
@Composable
private fun Preview_JourneyCardHeader_NoPlatform() {
    PreviewTheme(themeStyle = DEFAULT_THEME_STYLE) {
        JourneyCardHeader(
            timeToDeparture = "Now",
            platformText = null,
            transportModeLineList = persistentListOf(
                TransportModeLine(transportMode = TransportMode.Ferry, lineName = "F2"),
            ),
        )
    }
}

@PreviewComponent
@Composable
private fun Preview_JourneyCardHeader_Past() {
    PreviewTheme(themeStyle = DEFAULT_THEME_STYLE) {
        JourneyCardHeader(
            timeToDeparture = "3 mins ago",
            platformText = "Platform 4",
            transportModeLineList = persistentListOf(
                TransportModeLine(transportMode = TransportMode.Train, lineName = "T2"),
            ),
        )
    }
}

// endregion
