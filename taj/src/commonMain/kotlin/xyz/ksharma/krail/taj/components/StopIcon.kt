package xyz.ksharma.krail.taj.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * A rounded square: the universal "stop" glyph, authored as path data for the same reason
 * [MicIcon] is (material-icons-core does not carry it, and pulling in the extended artifact
 * for one shape is not worth it).
 *
 * Corners are rounded to match the app's own radius language rather than the sharp square
 * Material draws, so it sits next to KRAIL's pill buttons without looking borrowed.
 */
val StopIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Stop",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(8f, 6f)
            lineTo(16f, 6f)
            curveTo(17.1f, 6f, 18f, 6.9f, 18f, 8f)
            lineTo(18f, 16f)
            curveTo(18f, 17.1f, 17.1f, 18f, 16f, 18f)
            lineTo(8f, 18f)
            curveTo(6.9f, 18f, 6f, 17.1f, 6f, 16f)
            lineTo(6f, 8f)
            curveTo(6f, 6.9f, 6.9f, 6f, 8f, 6f)
            close()
        }
    }.build()
}
