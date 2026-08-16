package xyz.ksharma.krail.taj.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * A circle with an exclamation in it: something needs the rider's attention.
 *
 * Stroked rather than filled, and authored as path data for the same reason [MicIcon],
 * [StopIcon] and [SendIcon] are, so it carries the same weight as the rest of the set at the
 * same size.
 *
 * A triangle would read as danger. Nothing this marks is dangerous — a stop that did not
 * resolve is a thing to correct, not a warning — so the calmer of the two shapes is right.
 */
val AlertIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Alert",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            // The circle, as four arcs.
            moveTo(12f, 3f)
            arcToRelative(9f, 9f, 0f, true, true, -0.01f, 0f)
            close()

            // The stem, stopping short of the dot.
            moveTo(12f, 7.5f)
            lineTo(12f, 13f)

            // The dot.
            moveTo(12f, 16.5f)
            lineTo(12f, 16.6f)
        }
    }.build()
}
