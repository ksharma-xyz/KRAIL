package xyz.ksharma.krail.trip.planner.ui.components.crowding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme

/**
 * The seat tag beside a departure time: "Seats at both ends", "Few seats", "Standing only".
 *
 * It arrives with a response the screen already loads, so it has no loading or error state:
 * present or absent. Neutral on purpose (design decision 1A): no traffic-light colours, so it
 * never clashes with the line colour beside it and never relies on colour to carry the level.
 * The fill is a low-alpha wash of the content colour rather than a new token, so it reads the
 * same on the surface, on a past-departure row and in every theme.
 */
@Composable
internal fun SeatTag(text: String, modifier: Modifier = Modifier) {
    val dim = KrailTheme.dimensions
    val shape = RoundedCornerShape(dim.radiusFull)
    Text(
        text = text,
        style = KrailTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
        color = KrailTheme.colors.onSurface,
        modifier = modifier
            .background(color = KrailTheme.colors.onSurface.copy(alpha = TAG_FILL_ALPHA), shape = shape)
            .padding(horizontal = dim.spacingS, vertical = dim.spacingXXS),
    )
}

private const val TAG_FILL_ALPHA = 0.08f

// region Previews

@ScreenshotTest
@PreviewComponent
@Composable
private fun Preview_SeatTag_Zone() {
    PreviewTheme {
        SeatTag(text = "Seats at both ends")
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun Preview_SeatTag_StandingOnly() {
    PreviewTheme {
        SeatTag(text = "Standing only")
    }
}

// endregion
