package xyz.ksharma.krail.trip.planner.ui.components.crowding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.feature.crowding.CarriageLevels
import xyz.ksharma.krail.feature.crowding.CrowdingWording
import xyz.ksharma.krail.feature.crowding.SeatLevel
import xyz.ksharma.krail.feature.crowding.Zone
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.components.isLargeFontScale

/**
 * The carriage row (design decision 1A): one box per car in the text colour, filled to show
 * the level, and a sentence above saying where the seats are. No key: a fuller box reads as a
 * fuller car without one. Cars run left to right in feed order; which real end that is has not been
 * checked yet, so nothing here labels an end.
 *
 * Layout follows the width it is given and the font size, never the device: under
 * [MIN_CAR_WIDTH] per car, or at a large font size, the cars merge into three blocks (the
 * same zones the sentence is worked out from). Capped at [MAX_ROW_WIDTH] so a foldable does
 * not stretch it into a bar chart.
 */
@Composable
internal fun CarriageSection(
    cars: CarriageLevels,
    sentence: String?,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    // Text colour, not the line colour: the row sits on the line-coloured leg card, where the
    // line colour has too little contrast to read the fill.
    val ink = KrailTheme.colors.onSurface
    Column(
        modifier = modifier
            .widthIn(max = MAX_ROW_WIDTH)
            .semantics(mergeDescendants = true) { },
        verticalArrangement = Arrangement.spacedBy(dim.spacingS),
    ) {
        sentence?.let {
            Text(text = it, style = KrailTheme.typography.bodyMedium, color = KrailTheme.colors.onSurface)
        }
        CarriageRow(cars = cars, ink = ink)
    }
}

/**
 * Draws the cars, or three blocks when they do not fit. Screen readers hear the levels drawn,
 * since a card can show the row without a sentence.
 */
@Composable
internal fun CarriageRow(cars: CarriageLevels, ink: Color, modifier: Modifier = Modifier) {
    val largeFont = isLargeFontScale()
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val fits = maxWidth / cars.size >= MIN_CAR_WIDTH
        val drawn = if (fits && !largeFont) cars.levels else cars.asBlocks()
        val description = drawn.distinct().sorted().joinToString(prefix = "Carriages: ") {
            CrowdingWording.levelTag(it)
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(ROW_HEIGHT)
                .testTag(CARRIAGE_ROW_TAG)
                .clearAndSetSemantics { contentDescription = description },
        ) {
            val gap = CAR_GAP.toPx()
            val width = (size.width - gap * (drawn.size - 1)) / drawn.size
            drawn.forEachIndexed { index, level ->
                drawCar(Offset(index * (width + gap), 0f), Size(width, size.height), level, ink)
            }
        }
    }
}

private fun DrawScope.drawCar(topLeft: Offset, carSize: Size, level: SeatLevel, ink: Color) {
    val stroke = CAR_STROKE.toPx()
    val radius = CornerRadius(CAR_CORNER.toPx())
    val fillHeight = carSize.height * level.fillFraction()
    drawRoundRect(
        color = ink,
        topLeft = Offset(topLeft.x, topLeft.y + carSize.height - fillHeight),
        size = Size(carSize.width, fillHeight),
        cornerRadius = radius,
    )
    drawRoundRect(
        color = ink,
        topLeft = topLeft + Offset(stroke / 2, stroke / 2),
        size = Size(carSize.width - stroke, carSize.height - stroke),
        cornerRadius = radius,
        style = Stroke(width = stroke),
    )
}

// Fuller car, more fill: a third, two thirds, all of it. Shape, not colour, carries the level.
private fun SeatLevel.fillFraction(): Float = (ordinal + 1f) / SeatLevel.entries.size

private fun CarriageLevels.asBlocks(): List<SeatLevel> =
    zoneLevels()?.let { zones -> Zone.entries.map { zones.getValue(it) } } ?: levels

internal const val CARRIAGE_ROW_TAG = "crowding.carriageRow"
private val ROW_HEIGHT: Dp = 22.dp
private val CAR_GAP: Dp = 3.dp
private val CAR_STROKE: Dp = 1.5.dp
private val CAR_CORNER: Dp = 3.dp
private val MIN_CAR_WIDTH: Dp = 22.dp
private val MAX_ROW_WIDTH: Dp = 480.dp
