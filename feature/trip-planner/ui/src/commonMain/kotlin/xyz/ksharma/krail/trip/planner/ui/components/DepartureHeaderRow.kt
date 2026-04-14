package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.pastDepartureColor
import xyz.ksharma.krail.trip.planner.ui.pastDepartureTextStyle
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeLine

/**
 * The shared header row used by both [DepartureRow] and [JourneyCardHeader].
 *
 * Renders:
 * ```
 * [relativeTimeText]  [modeContent …]  |  [platformText]
 * ```
 *
 * All past-item styling (`pastDepartureTextStyle`, `pastDepartureColor`) is applied here
 * so that a change in one place propagates to every departure surface.
 *
 * @param relativeTimeText       "in 5 mins", "5 mins ago", "Now", etc.
 * @param isPast                 Whether this departure has already passed.
 * @param activeTimeColor        Colour for the time text when not past (e.g. line colour);
 *                               [pastDepartureColor] is applied internally.
 * @param activePlatformColor    Colour for the platform label when not past;
 *                               [pastDepartureColor] is applied internally.
 *                               Defaults to [KrailTheme.colors.label] (neutral), but
 *                               [JourneyCardHeader] overrides this with the line colour.
 * @param platformText           Optional platform/stand/wharf label at the trailing edge.
 * @param modifier               Modifier applied to the outer [Row].
 * @param modeContent            Slot for transport mode icons/badges. Receives a [RowScope]
 *                               so callers can place a single icon+badge ([DepartureRow])
 *                               or delegate to [TransportModesRow] ([JourneyCardHeader]).
 */
@Composable
internal fun DepartureHeaderRow(
    relativeTimeText: String,
    isPast: Boolean,
    activeTimeColor: Color,
    platformText: String?,
    modifier: Modifier = Modifier,
    activePlatformColor: Color = KrailTheme.colors.label,
    modeContent: @Composable RowScope.() -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (relativeTimeText.isNotBlank()) {
                Text(
                    text = relativeTimeText,
                    style = pastDepartureTextStyle(isPast, KrailTheme.typography.titleMedium),
                    color = pastDepartureColor(isPast, activeTimeColor),
                )
            }
            modeContent()
        }

        platformText?.let {
            Text(
                text = it,
                textAlign = TextAlign.End,
                style = pastDepartureTextStyle(isPast, KrailTheme.typography.titleMedium),
                color = pastDepartureColor(isPast, activePlatformColor),
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

// region Previews

private const val TRAIN_COLOR = "#F99D1C"
private const val BUS_COLOR = "#00B5EF"

// ── DepartureRow context (single mode, neutral platform colour) ───────────────

@PreviewComponent
@Composable
private fun Preview_Upcoming_SingleMode_WithPlatform() {
    PreviewTheme(KrailThemeStyle.Train) {
        DepartureHeaderRow(
            relativeTimeText = "in 5 mins",
            isPast = false,
            activeTimeColor = TRAIN_COLOR.hexToComposeColor(),
            platformText = "Platform 4",
        ) {
            TransportModeIcon(
                transportMode = TransportMode.Train,
                size = TransportModeIconSize.XSmall,
                displayBorder = false,
            )
            TransportModeBadge(badgeText = "T1", backgroundColor = TRAIN_COLOR.hexToComposeColor())
        }
    }
}

@PreviewComponent
@Composable
private fun Preview_Past_SingleMode_WithPlatform() {
    PreviewTheme(KrailThemeStyle.Train) {
        DepartureHeaderRow(
            relativeTimeText = "8 mins ago",
            isPast = true,
            activeTimeColor = TRAIN_COLOR.hexToComposeColor(),
            platformText = "Platform 4",
        ) {
            TransportModeIcon(
                transportMode = TransportMode.Train,
                size = TransportModeIconSize.XSmall,
                displayBorder = false,
            )
            TransportModeBadge(badgeText = "T1", backgroundColor = TRAIN_COLOR.hexToComposeColor())
        }
    }
}

@PreviewComponent
@Composable
private fun Preview_Upcoming_Bus_NoPlatform() {
    PreviewTheme(KrailThemeStyle.Bus) {
        DepartureHeaderRow(
            relativeTimeText = "Now",
            isPast = false,
            activeTimeColor = BUS_COLOR.hexToComposeColor(),
            platformText = null,
        ) {
            TransportModeIcon(
                transportMode = TransportMode.Bus,
                size = TransportModeIconSize.XSmall,
                displayBorder = false,
            )
            TransportModeBadge(badgeText = "700", backgroundColor = BUS_COLOR.hexToComposeColor())
        }
    }
}

@PreviewComponent
@Composable
private fun Preview_Past_Bus_NoPlatform() {
    PreviewTheme(KrailThemeStyle.Bus) {
        DepartureHeaderRow(
            relativeTimeText = "3 mins ago",
            isPast = true,
            activeTimeColor = BUS_COLOR.hexToComposeColor(),
            platformText = null,
        ) {
            TransportModeIcon(
                transportMode = TransportMode.Bus,
                size = TransportModeIconSize.XSmall,
                displayBorder = false,
            )
            TransportModeBadge(badgeText = "700", backgroundColor = BUS_COLOR.hexToComposeColor())
        }
    }
}

// ── JourneyCardHeader context (multi-mode, line colour platform) ──────────────

@PreviewComponent
@Composable
private fun Preview_Upcoming_MultiMode_LineColorPlatform() {
    PreviewTheme(KrailThemeStyle.Train) {
        DepartureHeaderRow(
            relativeTimeText = "in 7 mins",
            isPast = false,
            activeTimeColor = TRAIN_COLOR.hexToComposeColor(),
            activePlatformColor = TRAIN_COLOR.hexToComposeColor(),
            platformText = "Platform 3",
        ) {
            TransportModesRow(
                transportModeLineList = persistentListOf(
                    TransportModeLine(transportMode = TransportMode.Train, lineName = "T1"),
                    TransportModeLine(transportMode = TransportMode.Bus, lineName = "700"),
                ),
                showBadge = { it.transportMode is TransportMode.Bus },
            )
        }
    }
}

@PreviewComponent
@Composable
private fun Preview_Past_MultiMode_LineColorPlatform() {
    PreviewTheme(KrailThemeStyle.Train) {
        DepartureHeaderRow(
            relativeTimeText = "12 mins ago",
            isPast = true,
            activeTimeColor = TRAIN_COLOR.hexToComposeColor(),
            activePlatformColor = TRAIN_COLOR.hexToComposeColor(),
            platformText = "Platform 3",
        ) {
            TransportModesRow(
                transportModeLineList = persistentListOf(
                    TransportModeLine(transportMode = TransportMode.Train, lineName = "T1"),
                    TransportModeLine(transportMode = TransportMode.Bus, lineName = "700"),
                ),
                showBadge = { it.transportMode is TransportMode.Bus },
            )
        }
    }
}

// endregion
