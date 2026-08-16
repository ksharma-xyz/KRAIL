package xyz.ksharma.krail.taj.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * An upward arrow: send, in the shape every message field on a phone uses for it.
 *
 * Authored as path data for the same reason [MicIcon] and [StopIcon] are, and stroked rather
 * than filled so it holds its weight next to them at the same size.
 *
 * The no-arrows rule this repo has is about arrows in *copy*, where a word carries the meaning
 * better. This is a glyph on a button whose whole job is a direction.
 */
val SendIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Send",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2.2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(12f, 19f)
            lineTo(12f, 5f)
            moveTo(5f, 12f)
            lineTo(12f, 5f)
            lineTo(19f, 12f)
        }
    }.build()
}
