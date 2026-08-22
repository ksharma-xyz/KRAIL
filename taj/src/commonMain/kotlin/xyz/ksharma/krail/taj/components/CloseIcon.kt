package xyz.ksharma.krail.taj.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * A cross: close, dismiss, put this away.
 *
 * Authored as path data and stroked at the same weight as [SendIcon], [MicIcon] and [StopIcon]
 * so it sits beside them without looking lighter or heavier than the row it is in.
 *
 * It exists because a surface that can only be left by a gesture cannot be left at all by
 * someone who does not know the gesture. The Ask KRAIL card is a Compose dialog, and on iOS a
 * Compose dialog has no back press and no swipe: with nothing drawn to tap, the only way out
 * was a sentence the model happened to understand.
 */
val CloseIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Close",
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
            moveTo(6f, 6f)
            lineTo(18f, 18f)
            moveTo(18f, 6f)
            lineTo(6f, 18f)
        }
    }.build()
}
