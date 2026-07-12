package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_location_on
import krail.feature.trip_planner.ui.generated.resources.ic_walk
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.LocalContentAlpha
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme

@Composable
fun WalkingLeg(
    duration: String,
    modifier: Modifier = Modifier,
    originPinName: String? = null,
    destinationPinName: String? = null,
    // Off by default: the offset below is tuned to JourneyCard's LegView geometry
    // (card padding + inner column start padding). Only JourneyCard's call sites opt in;
    // TrackTripScreen uses its own timeline geometry and would misalign if this defaulted on.
    drawConnectorLine: Boolean = false,
) {
    val dim = KrailTheme.dimensions
    val density = LocalDensity.current
    // todo can be reusable logic for consistent icon size
    val iconSize = with(density) { 18.sp.toDp() }
    val contentAlpha = LocalContentAlpha.current

    // Aligns with LegView's own timeline x-position (card padding + inner column start
    // padding), minus this composable's own horizontal padding (applied by the caller).
    val lineOffset = dim.spacingL + dim.spacingXS - dim.spacingXXS

    // Gap from the line to the icon - matches LegView's StopInfo, which sits spacingXL after
    // its own timeline dot.
    val contentStart = lineOffset + dim.spacingXL

    // Walking is supplementary context, so its connector should remain quieter than the
    // mode-coloured transport timelines on either the regular or past-journey surface.
    val lineColor = KrailTheme.colors.walkingConnector
    val strokeWidth = dim.journeyLegStrokeWidth

    // Deliberately inherit the parent surface. A past JourneyCard uses
    // pastDepartureRowSurface; painting the normal surface here made walking and pin rows
    // look like a separate, non-past card inside it.
    Column(modifier = modifier.fillMaxWidth()) {
        if (originPinName != null) {
            PinRow(
                name = originPinName,
                iconSize = iconSize,
                lineOffset = lineOffset,
                textStart = contentStart,
            )
            Spacer(modifier = Modifier.height(PIN_ICON_GAP))
        }

        // The connector gap (above/below, when a pin is present) and the walk row are drawn
        // as ONE continuous dashed line rather than 2-3 separately-phased segments. Each
        // dashedTimeLine call restarts its dash pattern at phase 0, so stitching independent
        // segments together left a visible extra dot wherever two fresh phase-0 starts landed
        // close to each other at a segment boundary - a single drawBehind spanning the whole
        // block has no such seam.
        Column(
            modifier = if (drawConnectorLine) {
                Modifier.dashedTimeLine(lineColor, strokeWidth, xOffset = lineOffset)
            } else {
                Modifier
            },
        ) {
            if (originPinName != null) {
                Spacer(modifier = Modifier.height(CONNECTOR_GAP_HEIGHT))
            }

            Row(
                modifier = Modifier.padding(start = contentStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_walk),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(
                        color = KrailTheme.colors.onSurface.copy(alpha = contentAlpha),
                    ),
                    modifier = Modifier.size(iconSize),
                )
                Text("Walk $duration", style = KrailTheme.typography.bodyLarge)
            }

            if (destinationPinName != null) {
                Spacer(modifier = Modifier.height(CONNECTOR_GAP_HEIGHT))
            }
        }

        if (destinationPinName != null) {
            Spacer(modifier = Modifier.height(PIN_ICON_GAP))
            PinRow(
                name = destinationPinName,
                iconSize = iconSize,
                lineOffset = lineOffset,
                textStart = contentStart,
            )
        }
    }
}

/**
 * An address row whose pin icon sits exactly on the timeline (at [lineOffset]) - the icon *is*
 * the line's marker, the same way LegView's solid dot marks where its own line starts/ends. No
 * line is drawn through this row itself: see [PIN_ICON_GAP] and the surrounding blank [Spacer]s
 * for why the dashes stay entirely outside the icon's own bounding box.
 * The address text is offset further right (at [textStart]), matching LegView's StopInfo text.
 */
@Composable
private fun PinRow(name: String, iconSize: Dp, lineOffset: Dp, textStart: Dp) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(lineOffset - iconSize / 2))
        Image(
            painter = painterResource(Res.drawable.ic_location_on),
            contentDescription = null,
            colorFilter = ColorFilter.tint(color = KrailTheme.colors.onSurface),
            modifier = Modifier.size(iconSize),
        )
        Spacer(modifier = Modifier.width(textStart - lineOffset - iconSize / 2))
        Text(
            text = name,
            style = KrailTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = KrailTheme.colors.onSurface,
        )
    }
}

// Visible breathing room between the icon's own bounding box and the first/last dash.
// ic_location_on.xml's ink fills almost the whole box (path spans y=2..22 of a 24 viewBox),
// so a line endpoint anywhere *inside* iconSize/2 is fully hidden under the icon and produces
// no visible gap - the gap only appears once the line stops *past* the box edge.
private val PIN_ICON_GAP = 6.dp

// Height of the spacer between the "Walk" text row and a PinRow's stop name - dashedTimeLine
// on this spacer keeps the connector line continuous through the extra space.
private val CONNECTOR_GAP_HEIGHT = 20.dp

// @ScreenshotTest disabled: missing baseline (recording timed out, see README)
@Preview
@Composable
private fun PreviewWalkingLeg() {
    PreviewTheme {
        WalkingLeg("5 mins")
    }
}

@Preview
@Composable
private fun PreviewWalkingLegWithPins() {
    PreviewTheme {
        WalkingLeg(
            duration = "5 mins",
            originPinName = "123 Example St, Sydney",
            destinationPinName = "Sydney Opera House",
            drawConnectorLine = true,
        )
    }
}
