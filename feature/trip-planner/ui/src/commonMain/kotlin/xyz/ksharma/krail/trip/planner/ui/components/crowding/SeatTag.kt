package xyz.ksharma.krail.trip.planner.ui.components.crowding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.feature.crowding.CarriageLevels
import xyz.ksharma.krail.feature.crowding.SeatLevel
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme

/**
 * The seat tag beside a departure time: "Seats at both ends", "Few seats", "Standing only".
 *
 * Plain text, no pill, led by a small bar per car when the cars are known: a taller bar is a
 * fuller car, so the row reads at a glance without a key. Buses and trains without carriage
 * data show the words alone.
 *
 * It arrives with a response the screen already loads, so it has no loading or error state:
 * present or absent. Neutral on purpose (design decision 1A): no traffic-light colours, and the
 * words always carry the level, never the bars alone.
 */
@Composable
internal fun SeatTag(text: String, modifier: Modifier = Modifier, cars: CarriageLevels? = null) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(KrailTheme.dimensions.spacingS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (cars != null && cars.size > 1) MiniCars(levels = cars.levels, ink = KrailTheme.colors.onSurface)
        CrossfadeText(text = text) { words ->
            Text(
                text = words,
                style = KrailTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = KrailTheme.colors.onSurface,
            )
        }
    }
}

@Composable
private fun MiniCars(levels: List<SeatLevel>, ink: Color) {
    val width = MINI_BAR_WIDTH * levels.size + MINI_BAR_GAP * (levels.size - 1)
    val fills = animatedFills(levels)
    Canvas(
        modifier = Modifier
            .width(width)
            .height(MINI_HEIGHT)
            // The words beside it say the same thing.
            .clearAndSetSemantics { },
    ) {
        val bar = MINI_BAR_WIDTH.toPx()
        val step = bar + MINI_BAR_GAP.toPx()
        val radius = CornerRadius(MINI_BAR_CORNER.toPx())
        fills.forEachIndexed { index, fill ->
            val barHeight = size.height * fill
            drawRoundRect(
                color = ink,
                topLeft = Offset(index * step, size.height - barHeight),
                size = Size(bar, barHeight),
                cornerRadius = radius,
            )
        }
    }
}

private val MINI_HEIGHT: Dp = 12.dp
private val MINI_BAR_WIDTH: Dp = 4.dp
private val MINI_BAR_GAP: Dp = 1.5.dp
private val MINI_BAR_CORNER: Dp = 1.5.dp

/**
 * The tag where it can come and go on a refresh: opens top-down and fades in, and on the way
 * out keeps drawing its last words while it folds, so the row below never jumps.
 */
@Composable
internal fun AnimatedSeatTag(text: String?, cars: CarriageLevels?, modifier: Modifier = Modifier) {
    var last by remember { mutableStateOf(text) }
    var lastCars by remember { mutableStateOf(cars) }
    if (text != null) {
        last = text
        lastCars = cars
    }
    AnimatedVisibility(
        visible = text != null,
        modifier = modifier,
        enter = fadeIn(tween(TEXT_FADE_MS)) + expandVertically(tween(TEXT_FADE_MS), expandFrom = Alignment.Top),
        exit = fadeOut(tween(TEXT_FADE_MS)) + shrinkVertically(tween(TEXT_FADE_MS), shrinkTowards = Alignment.Top),
    ) {
        last?.let { SeatTag(text = it, cars = lastCars) }
    }
}

// region Previews

@ScreenshotTest
@PreviewComponent
@Composable
private fun Preview_SeatTag_Zone() {
    PreviewTheme {
        SeatTag(
            text = "Seats at both ends",
            cars = CarriageLevels(
                listOf(SeatLevel.SEATS_AVAILABLE) + List(6) { SeatLevel.FEW_SEATS } + SeatLevel.SEATS_AVAILABLE,
            ),
        )
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
