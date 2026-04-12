package xyz.ksharma.krail.trip.planner.ui.departureboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.getForegroundColor

private val chipShape = RoundedCornerShape(8.dp)
private val chipMinHeight = 44.dp
private val chipMinWidth = 44.dp

/**
 * A tappable, selectable chip that displays a transport line number using [lineColorCode].
 *
 * Intended for per-line departure filtering (Option C, see [DepartureBoardFilterStrategy]).
 * Built today so the component is ready; the filter wiring will be added when Option C is enabled.
 *
 * Selected state:  solid [lineColorCode] background, accessible foreground text.
 * Unselected state: transparent background, [lineColorCode] border and text.
 *
 * Touch target is at minimum [chipMinHeight] × [chipMinWidth] (44 × 44 dp) to satisfy
 * accessibility guidelines.
 *
 * @param lineNumber   Short line identifier shown on the chip — "T1", "333", "F1".
 * @param lineColorCode Hex colour code for the line, e.g. "#F99D1C".
 * @param selected      Whether this chip is currently active.
 * @param onClick       Invoked when the chip is tapped.
 */
@Composable
fun LineBadgeChip(
    lineNumber: String,
    lineColorCode: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lineColor = remember(lineColorCode) { lineColorCode.hexToComposeColor() }

    val backgroundColor by animateColorAsState(
        targetValue = if (selected) lineColor else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "LineBadgeChip-bg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) Color.Transparent else lineColor,
        animationSpec = tween(durationMillis = 200),
        label = "LineBadgeChip-border",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) getForegroundColor(lineColor) else lineColor,
        animationSpec = tween(durationMillis = 200),
        label = "LineBadgeChip-text",
    )

    Box(
        modifier = modifier
            .heightIn(min = chipMinHeight)
            .widthIn(min = chipMinWidth)
            .clip(chipShape)
            .background(backgroundColor)
            .border(width = 2.dp, color = borderColor, shape = chipShape)
            .clickable(indication = null, interactionSource = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = lineNumber,
            color = textColor,
            style = KrailTheme.typography.titleMedium,
        )
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@PreviewComponent
@Composable
private fun LineBadgeChipSelectedPreview() {
    PreviewTheme {
        LineBadgeChip(
            lineNumber = "T1",
            lineColorCode = "#F99D1C",
            selected = true,
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun LineBadgeChipUnselectedPreview() {
    PreviewTheme {
        LineBadgeChip(
            lineNumber = "T1",
            lineColorCode = "#F99D1C",
            selected = false,
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun LineBadgeChipBusSelectedPreview() {
    PreviewTheme {
        LineBadgeChip(
            lineNumber = "333",
            lineColorCode = "#00B5EF",
            selected = true,
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun LineBadgeChipBusUnselectedPreview() {
    PreviewTheme {
        LineBadgeChip(
            lineNumber = "333",
            lineColorCode = "#00B5EF",
            selected = false,
            onClick = {},
        )
    }
}
