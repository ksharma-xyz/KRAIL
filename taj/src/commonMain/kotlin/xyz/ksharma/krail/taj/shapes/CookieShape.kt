package xyz.ksharma.krail.taj.shapes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.theme.KrailTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Reusable cookie (scalloped) shape.
class CookieShape(
    private val bumps: Int = 9,
    private val depthFraction: Float = 0.12f, // how deep each scallop is
    private val smooth: Boolean = true,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = buildCookiePath(size, bumps, depthFraction, smooth)
        return Outline.Generic(path)
    }
}

internal fun buildCookiePath(
    size: Size,
    bumps: Int,
    depthFraction: Float,
    smooth: Boolean
): Path {
    val w = size.width
    val h = size.height
    val rBase = min(w, h) / 2f
    val depth = rBase * depthFraction
    val steps = bumps * 40 // more = smoother
    val cx = w / 2f
    val cy = h / 2f
    val path = Path()
    var prevX = 0f
    var prevY = 0f
    for (i in 0..steps) {
        val t = i.toFloat() / steps
        val theta = t * 2f * PI.toFloat()
        val radius = rBase - depth + depth * (1f + sin(bumps * theta)) / 2f
        val x = cx + radius * cos(theta)
        val y = cy + radius * sin(theta)
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            if (smooth) {
                // simple quadratic smoothing using midpoint control
                val mx = (prevX + x) / 2f
                val my = (prevY + y) / 2f
                path.quadraticTo(prevX, prevY, mx, my)
            } else {
                path.lineTo(x, y)
            }
        }
        prevX = x
        prevY = y
    }
    path.close()
    return path
}

