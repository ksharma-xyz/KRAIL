package xyz.ksharma.krail.taj.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * A triangle with an exclamation: a service the rider planned on is not running.
 *
 * Stroked path data like [AlertIcon], so it carries the same weight at the same size. Drawn in
 * white and tinted by the caller.
 */
val WarningIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "WarningIcon",
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
            // The triangle.
            moveTo(12f, 3.5f)
            lineTo(21.5f, 20f)
            lineTo(2.5f, 20f)
            close()

            // The stem, stopping short of the dot.
            moveTo(12f, 9.5f)
            lineTo(12f, 14f)

            // The dot.
            moveTo(12f, 17f)
            lineTo(12f, 17.1f)
        }
    }.build()
}
