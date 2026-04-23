package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.pastDepartureColor
import xyz.ksharma.krail.trip.planner.ui.pastDepartureTextStyle

/**
 * Shared UI component that renders a departure or origin time, with an optional
 * deviation row when the service is delayed or early.
 *
 * **On time** (no deviation):
 * ```
 * 11:30 am                       ← [timeText] in [onTimeTextStyle]
 * ```
 *
 * **Delayed or early**:
 * ```
 * ~~11:28 am~~  Delayed 2 min    ← strikethrough [scheduledTimeText] + [deviationLabel]
 * 11:30 am                       ← [timeText] promoted to titleMedium
 * ```
 *
 * All colours are resolved through [pastDepartureColor], so past items read consistently
 * as neutral text against the `pastDepartureRowSurface` background.
 *
 * This composable is the single source of truth for scheduled-vs-actual time display,
 * used by both [DepartureRow] and JourneyCard's origin-time row.
 *
 * @param timeText          The real (possibly delayed / early) time to display.
 * @param isPast            Whether this item is a past departure or past journey.
 * @param scheduledTimeText Scheduled time shown with strikethrough when deviated; pass
 *                          `null` (or omit) to render the on-time layout.
 * @param deviationLabel    Pre-formatted deviation string, e.g. "Delayed 2 min" or
 *                          "Early 1 min". Pass `null` together with a null
 *                          [scheduledTimeText] for the on-time layout.
 * @param deviationColor    Colour applied to [deviationLabel]; unused when no deviation.
 * @param onTimeTextStyle   Typography for [timeText] in the on-time layout. Callers can
 *                          override (e.g. `bodyMedium` for departure rows where the
 *                          "in X mins" title already establishes the visual hierarchy).
 * @param modifier          Modifier applied to the root layout.
 */
@Composable
fun ScheduledTimeRow(
    timeText: String,
    isPast: Boolean,
    modifier: Modifier = Modifier,
    scheduledTimeText: String? = null,
    deviationLabel: String? = null,
    deviationColor: Color = Color.Unspecified,
    onTimeTextStyle: TextStyle = KrailTheme.typography.titleMedium,
) {
    val dim = KrailTheme.dimensions
    if (scheduledTimeText != null && deviationLabel != null) {
        // Deviation layout: strikethrough + label on first line, promoted actual time below
        Column(modifier = modifier) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(dim.spacingS),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append(scheduledTimeText)
                        }
                    },
                    style = KrailTheme.typography.bodyMedium,
                    color = pastDepartureColor(isPast, KrailTheme.colors.softLabel),
                )
                Text(
                    text = deviationLabel,
                    style = KrailTheme.typography.bodyMedium,
                    color = deviationColor,
                )
            }
            Text(
                text = timeText,
                style = pastDepartureTextStyle(isPast, KrailTheme.typography.titleMedium),
                color = KrailTheme.colors.label,
            )
        }
    } else {
        // On-time layout: single time text
        Text(
            text = timeText,
            style = pastDepartureTextStyle(isPast, onTimeTextStyle),
            color = KrailTheme.colors.label,
            modifier = modifier,
        )
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@PreviewComponent
@Composable
private fun ScheduledTimeRowOnTimePreview() {
    PreviewTheme(KrailThemeStyle.Train) {
        ScheduledTimeRow(
            timeText = "11:30 AM",
            isPast = false,
        )
    }
}

@Preview
@Composable
private fun ScheduledTimeRowDelayedPreview() {
    PreviewTheme(KrailThemeStyle.Bus) {
        ScheduledTimeRow(
            timeText = "11:32 AM",
            isPast = false,
            scheduledTimeText = "11:30 AM",
            deviationLabel = "Delayed 2 min",
            deviationColor = KrailTheme.colors.deviationLate,
        )
    }
}

@Preview
@Composable
private fun ScheduledTimeRowEarlyPreview() {
    PreviewTheme(KrailThemeStyle.Train) {
        ScheduledTimeRow(
            timeText = "11:28 AM",
            isPast = false,
            scheduledTimeText = "11:30 AM",
            deviationLabel = "Early 2 min",
            deviationColor = KrailTheme.colors.deviationEarly,
        )
    }
}

@PreviewComponent
@Composable
private fun ScheduledTimeRowPastOnTimePreview() {
    PreviewTheme(KrailThemeStyle.Train) {
        ScheduledTimeRow(
            timeText = "11:30 AM",
            isPast = true,
        )
    }
}

@Preview
@Composable
private fun ScheduledTimeRowPastDelayedPreview() {
    PreviewTheme(KrailThemeStyle.Bus) {
        ScheduledTimeRow(
            timeText = "11:32 AM",
            isPast = true,
            scheduledTimeText = "11:30 AM",
            deviationLabel = "Delayed 2 min",
            deviationColor = KrailTheme.colors.deviationLate,
        )
    }
}

@Preview
@Composable
private fun ScheduledTimeRowPastEarlyPreview() {
    PreviewTheme(KrailThemeStyle.Train) {
        ScheduledTimeRow(
            timeText = "11:28 AM",
            isPast = true,
            scheduledTimeText = "11:30 AM",
            deviationLabel = "Early 2 min",
            deviationColor = KrailTheme.colors.deviationEarly,
        )
    }
}
