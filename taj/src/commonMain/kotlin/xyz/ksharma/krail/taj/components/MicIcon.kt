package xyz.ksharma.krail.taj.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The standard Material Design "mic" glyph, authored directly as path data rather than
 * pulling in material-icons-extended for one icon (material-icons-core, this project's
 * only icon dependency, doesn't include it — same gap as the missing thumbs-down in
 * AlertFeedbackVote). Same path data every Material mic icon in the wild uses, just not
 * routed through the extended-icons artifact.
 */
val MicIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Mic",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 14f)
            curveTo(13.66f, 14f, 15f, 12.66f, 15f, 11f)
            lineTo(15f, 5f)
            curveTo(15f, 3.34f, 13.66f, 2f, 12f, 2f)
            curveTo(10.34f, 2f, 9f, 3.34f, 9f, 5f)
            lineTo(9f, 11f)
            curveTo(9f, 12.66f, 10.34f, 14f, 12f, 14f)
            close()
            moveTo(17f, 11f)
            curveTo(17f, 13.76f, 14.76f, 16f, 12f, 16f)
            curveTo(9.24f, 16f, 7f, 13.76f, 7f, 11f)
            lineTo(5f, 11f)
            curveTo(5f, 14.53f, 7.61f, 17.43f, 11f, 17.92f)
            lineTo(11f, 21f)
            lineTo(13f, 21f)
            lineTo(13f, 17.92f)
            curveTo(16.39f, 17.43f, 19f, 14.53f, 19f, 11f)
            lineTo(17f, 11f)
            close()
        }
    }.build()
}
