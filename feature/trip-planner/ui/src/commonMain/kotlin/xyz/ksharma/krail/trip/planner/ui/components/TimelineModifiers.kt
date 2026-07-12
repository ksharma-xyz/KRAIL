package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws a vertical line and a circle at the top start of the composable,
 * representing the top of a timeline element.
 *
 * @param color The color of the line and circle.
 * @param strokeWidth The width of the line stroke.
 * @param circleRadius The radius of the circle.
 * @return A [Modifier] that draws the line and circle.
 */
internal fun Modifier.timeLineTop(
    color: Color,
    strokeWidth: Dp,
    circleRadius: Dp,
    circleColor: Color = color,
): Modifier {
    return this.drawBehind {
        drawCircle(
            color = circleColor,
            radius = circleRadius.toPx(),
            center = Offset(x = 0f, y = this.size.height / 2),
        )
        drawLine(
            color = color,
            start = Offset(x = 0f, y = this.size.height / 2),
            end = Offset(x = 0f, y = this.size.height),
            strokeWidth = strokeWidth.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

/**
 * Draws a vertical line through the center start of the composable,
 * representing a continuous timeline element.
 *
 * @param color The color of the line.
 * @param strokeWidth The width of the line stroke.
 * @return A [Modifier] that draws the line.
 */
internal fun Modifier.timeLineCenter(color: Color, strokeWidth: Dp): Modifier {
    return this.drawBehind {
        drawLine(
            color = color,
            start = Offset(x = 0f, y = 0f),
            end = Offset(x = 0f, y = this.size.height),
            strokeWidth = strokeWidth.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

/**
 * Draws a split vertical line: [completedColor] for the top [fraction] of height,
 * [pendingColor] for the remainder. Used to show approximate vehicle progress on a segment.
 */
internal fun Modifier.timeLineCenterSplit(
    completedColor: Color,
    pendingColor: Color,
    strokeWidth: Dp,
    fraction: Float,
): Modifier {
    return this.drawBehind {
        val splitY = size.height * fraction.coerceIn(0f, 1f)
        if (splitY > 0f) {
            drawLine(
                color = completedColor,
                start = Offset(x = 0f, y = 0f),
                end = Offset(x = 0f, y = splitY),
                strokeWidth = strokeWidth.toPx(),
                cap = StrokeCap.Round,
            )
        }
        if (splitY < size.height) {
            drawLine(
                color = pendingColor,
                start = Offset(x = 0f, y = splitY),
                end = Offset(x = 0f, y = size.height),
                strokeWidth = strokeWidth.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

internal fun Modifier.timeLineCenterWithStop(
    color: Color,
    strokeWidth: Dp,
    circleRadius: Dp,
    circleColor: Color = color,
): Modifier {
    return this.drawBehind {
        drawLine(
            color = color,
            start = Offset(x = 0f, y = 0f),
            end = Offset(x = 0f, y = this.size.height),
            strokeWidth = strokeWidth.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = circleColor,
            radius = circleRadius.toPx(),
            center = Offset(0f, this.size.height / 2),
        )
    }
}

// A near-zero line segment plus a round cap produces a true dot. The wide gap makes walking
// connectors recede behind scheduled-service timelines instead of appearing as a heavy dash.
private val DASH_LENGTH = 0.5.dp
private val DASH_GAP = 10.dp

private fun DrawScope.dashEffect(): PathEffect = PathEffect.dashPathEffect(
    intervals = floatArrayOf(DASH_LENGTH.toPx(), DASH_GAP.toPx()),
    phase = 0f,
)

/**
 * Dashed vertical line through the full height of the composable - the "informal segment"
 * equivalent of [timeLineCenter], used to connect a walking leg row to the LegView cards
 * above/below it (or to bridge the gap between two rows of the same walking leg). Dashed (vs
 * the solid, mode-colored line used between scheduled stops) signals "you're walking, not
 * riding a scheduled service", matching the convention used by Google Maps/Citymapper.
 *
 * @param xOffset Horizontal offset of the line from this composable's own left edge - use this
 * (rather than a nested `.padding(start = ...)`) when the composable draws the full-height line
 * itself instead of relying on an ancestor's padding to position it, as [LegView]'s stop rows do.
 */
internal fun Modifier.dashedTimeLine(color: Color, strokeWidth: Dp, xOffset: Dp = 0.dp): Modifier {
    return this.drawBehind {
        val x = xOffset.toPx()
        drawLine(
            color = color,
            start = Offset(x = x, y = 0f),
            end = Offset(x = x, y = this.size.height),
            strokeWidth = strokeWidth.toPx(),
            cap = StrokeCap.Round,
            pathEffect = dashEffect(),
        )
    }
}

/**
 * Draws a vertical line and a circle at the bottom start of the composable,
 * representing the bottom end of a timeline element.
 *
 * @param color The color of the line and circle.
 * @param strokeWidth The width of the line stroke.
 * @param circleRadius The radius of the circle.
 * @return A [Modifier] that draws the line and circle.
 */
internal fun Modifier.timeLineBottom(
    color: Color,
    strokeWidth: Dp,
    circleRadius: Dp,
    circleColor: Color = color,
): Modifier {
    return this.drawBehind {
        drawLine(
            color = color,
            start = Offset(x = 0f, y = 0f),
            end = Offset(x = 0f, y = this.size.height / 2),
            strokeWidth = strokeWidth.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = circleColor,
            radius = circleRadius.toPx(),
            center = Offset(0f, this.size.height / 2),
        )
    }
}
