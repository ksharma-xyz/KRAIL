package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.taj.components.SeparatorIcon
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.preview.PreviewComponent
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
    isSchoolBus: Boolean = false,
) {
    val isPast by remember(timeToDeparture) {
        mutableStateOf(timeToDeparture.contains(other = "ago", ignoreCase = true))
    }
    val activeLineColor = transportModeLineList.firstOrNull()?.lineColorCode?.hexToComposeColor()
        ?: KrailTheme.colors.onSurface
    val isAdaptive = displayAdaptiveTransportModeList(transportModeLineList, platformText)

    val dim = KrailTheme.dimensions
    Column(
        modifier = modifier.padding(bottom = dim.journeyCardLegSpacing),
    ) {
        DepartureHeaderRow(
            relativeTimeText = timeToDeparture,
            isPast = isPast,
            activeTimeColor = activeLineColor,
            // Platform uses the line colour (e.g. orange "Platform 1") rather than the neutral label
            activePlatformColor = activeLineColor,
            platformText = platformText,
        ) {
            // When adaptive layout is active, modes move to their own row below; show nothing here.
            if (!isAdaptive) {
                TransportModesRow(
                    transportModeLineList = transportModeLineList,
                    showBadge = { it.transportMode is TransportMode.Bus },
                    isSchoolBus = isSchoolBus,
                )
            }
        }

        if (isAdaptive) {
            // Large font scale or many modes: badges/icons on their own line so they don't
            // crowd the platform label.
            TransportModesRow(
                transportModeLineList = transportModeLineList,
                showBadge = { it.transportMode is TransportMode.Bus },
                isSchoolBus = isSchoolBus,
                modifier = Modifier.padding(top = dim.journeyCardLegSpacing),
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
    isSchoolBus: Boolean = false,
) {
    val dim = KrailTheme.dimensions
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dim.spacingS),
        verticalArrangement = Arrangement.spacedBy(dim.spacingXS),
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
        if (isSchoolBus) {
            Text(
                text = "School Bus",
                style = KrailTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = KrailTheme.colors.onSurface,
            )
        }
    }
}

// region Previews

@ScreenshotTest
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

@ScreenshotTest
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

@ScreenshotTest
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

@ScreenshotTest
@PreviewComponent
@Composable
private fun Preview_JourneyCardHeader_ManyModes_WrappedLayout() {
    PreviewTheme(themeStyle = DEFAULT_THEME_STYLE) {
        JourneyCardHeader(
            timeToDeparture = "Tomorrow 10:10 am Okay",
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

@ScreenshotTest
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

@ScreenshotTest
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
