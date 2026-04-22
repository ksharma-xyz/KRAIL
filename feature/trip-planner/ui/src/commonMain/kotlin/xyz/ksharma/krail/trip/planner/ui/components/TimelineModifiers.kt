package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp

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
