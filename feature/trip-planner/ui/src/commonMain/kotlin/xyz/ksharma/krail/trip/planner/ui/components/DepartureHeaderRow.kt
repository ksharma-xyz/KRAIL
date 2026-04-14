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
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.core.transport.nsw.NswTransportLine
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.ensureMinimumContrast
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
    val surface = KrailTheme.colors.surface
    val resolvedTimeColor = pastDepartureColor(
        isPast = isPast,
        activeColor = activeTimeColor.ensureMinimumContrast(surface),
    )
    val resolvedPlatformColor = pastDepartureColor(
        isPast = isPast,
        activeColor = activePlatformColor.ensureMinimumContrast(surface),
    )

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
                    color = resolvedTimeColor,
                )
            }
            modeContent()
        }

        platformText?.let {
            Text(
                text = it,
                textAlign = TextAlign.End,
                style = pastDepartureTextStyle(isPast, KrailTheme.typography.titleMedium),
                color = resolvedPlatformColor,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

// region Previews

// Values sourced from the enum / theme — no hardcoded hex strings.
// If the official brand colour changes, the preview automatically reflects it.
private val TRAIN_COLOR = NswTransportLine.NORTH_SHORE_WESTERN.hexColor  // T1
private val BUS_COLOR = KrailThemeStyle.Bus.hexColorCode

// Colors that are low-contrast on dark backgrounds — used for contrast-enforcement previews.
// Values are taken directly from NswTransportLine so that if the official brand colour ever
// changes in the enum the preview (and the snapshot regression) automatically reflects it.
//
//  T4 (#005AA3) → ~2.35:1 on dark surface — well below WCAG AA.  ensureMinimumContrast brightens it.
//  T8 (#00954C) → ~4.32:1 on dark surface — just below WCAG AA.  ensureMinimumContrast nudges it up.
private val T4_COLOR = NswTransportLine.EASTERN_SUBURBS_ILLAWARRA.hexColor
private val T8_COLOR = NswTransportLine.AIRPORT_SOUTH.hexColor

// ── DepartureRow context (single mode, neutral platform colour) ───────────────

@ScreenshotTest
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

@ScreenshotTest
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

@ScreenshotTest
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

@ScreenshotTest
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

@ScreenshotTest
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

@ScreenshotTest
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

// ── Contrast enforcement previews — low-contrast line colors on dark backgrounds ──

/**
 * T4 (#005AA3) is ~2.35:1 on the dark surface — well below WCAG AA.
 * [ensureMinimumContrast] brightens it until it reaches 4.5:1.
 * The dark-mode variant of this preview demonstrates the fix.
 */
@ScreenshotTest(description = "T4 dark navy line — contrast enforcement brightens color in dark mode")
@PreviewComponent
@Composable
private fun Preview_ContrastEnforcement_T4_LowContrast() {
    PreviewTheme(KrailThemeStyle.Train) {
        DepartureHeaderRow(
            relativeTimeText = "in 3 mins",
            isPast = false,
            activeTimeColor = T4_COLOR.hexToComposeColor(),
            activePlatformColor = T4_COLOR.hexToComposeColor(),
            platformText = "Platform 2",
        ) {
            TransportModeIcon(
                transportMode = TransportMode.Train,
                size = TransportModeIconSize.XSmall,
                displayBorder = false,
            )
            TransportModeBadge(badgeText = "T4", backgroundColor = T4_COLOR.hexToComposeColor())
        }
    }
}

/**
 * T8 (#00954C) is ~4.32:1 on dark surface — just below WCAG AA.
 * [ensureMinimumContrast] slightly brightens it to meet the 4.5:1 threshold.
 */
@ScreenshotTest(description = "T8 green line — contrast enforcement makes marginal color compliant")
@PreviewComponent
@Composable
private fun Preview_ContrastEnforcement_T8_MarginalContrast() {
    PreviewTheme(KrailThemeStyle.Train) {
        DepartureHeaderRow(
            relativeTimeText = "in 8 mins",
            isPast = false,
            activeTimeColor = T8_COLOR.hexToComposeColor(),
            activePlatformColor = T8_COLOR.hexToComposeColor(),
            platformText = "Platform 5",
        ) {
            TransportModeIcon(
                transportMode = TransportMode.Train,
                size = TransportModeIconSize.XSmall,
                displayBorder = false,
            )
            TransportModeBadge(badgeText = "T8", backgroundColor = T8_COLOR.hexToComposeColor())
        }
    }
}

// endregion
