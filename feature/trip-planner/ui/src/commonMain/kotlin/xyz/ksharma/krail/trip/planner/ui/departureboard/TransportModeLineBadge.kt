@file:Suppress("MagicNumber")

package xyz.ksharma.krail.trip.planner.ui.departureboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.LocalContentAlpha
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.tokens.ContentAlphaTokens

/**
 * A single pill-shaped badge that shows both the transport mode initial and the line number.
 *
 * Visual layout:
 * ```
 * ┌──────────────┐
 * │  ●T  │  T1  │
 * └──────────────┘
 * ```
 * The left circle uses a semi-transparent dark overlay on [lineColorCode] to differentiate
 * the mode initial from the line number while keeping a single colour palette.
 *
 * This component replaces the plain [TransportModeBadge] inside [DepartureRow] so that
 * the transport mode is always visible alongside the line number without extra layout space.
 *
 * @param transportModeInitial Single uppercase letter for the mode — "T", "B", "M", "F", "L".
 *                             Derive via [String.toTransportModeInitial].
 * @param lineNumber           Short line identifier — "T1", "333", "F1", "M".
 * @param lineColorCode        Hex colour code for the badge background, e.g. "#F99D1C".
 */
@Composable
fun TransportModeLineBadge(
    transportModeInitial: String,
    lineNumber: String,
    lineColorCode: String,
    modifier: Modifier = Modifier,
) {
    val lineColor = remember(lineColorCode) { lineColorCode.hexToComposeColor() }

    CompositionLocalProvider(LocalContentAlpha provides ContentAlphaTokens.EnabledContentAlpha) {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(percent = 20))
                .background(lineColor),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Mode initial in a darkened overlay circle
            Box(
                modifier = Modifier
                    .padding(3.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = transportModeInitial,
                    color = Color.White,
                    style = KrailTheme.typography.titleSmall,
                )
            }
            // Line number
            Text(
                text = lineNumber,
                color = Color.White,
                style = KrailTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 4.dp, end = 6.dp),
            )
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@PreviewComponent
@Composable
private fun TransportModeLineBadgeTrainPreview() {
    PreviewTheme {
        TransportModeLineBadge(
            transportModeInitial = "T",
            lineNumber = "T1",
            lineColorCode = "#F99D1C",
        )
    }
}

@Preview
@Composable
private fun TransportModeLineBadgeBusPreview() {
    PreviewTheme {
        TransportModeLineBadge(
            transportModeInitial = "B",
            lineNumber = "333",
            lineColorCode = "#00B5EF",
        )
    }
}

@Preview
@Composable
private fun TransportModeLineBadgeFerryPreview() {
    PreviewTheme {
        TransportModeLineBadge(
            transportModeInitial = "F",
            lineNumber = "F1",
            lineColorCode = "#00774B",
        )
    }
}

@Preview
@Composable
private fun TransportModeLineBadgeMetroPreview() {
    PreviewTheme {
        TransportModeLineBadge(
            transportModeInitial = "M",
            lineNumber = "M",
            lineColorCode = "#009B77",
        )
    }
}
