package xyz.ksharma.krail.taj.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.theme.KrailTheme

/**
 * Three dots that bob up and down in sequence — used as a generic loading indicator.
 *
 * Sizes are taken in dp and converted to pixels per current density, so the dots
 * render consistently across phones (the previous pixel-fixed layout looked bigger
 * on low-density and got clipped at the top of small canvases on high-density).
 *
 * The dots are drawn into a Canvas, so the overall area is governed by [modifier].
 * Make sure the Canvas is at least:
 *   width  >= 2 * [dotSpacing] + 2 * [dotRadius]
 *   height >= 2 * ([dotRadius] + [bounceHeight])
 *
 * For consistent visuals across screens prefer wrapping with [LoadingDotsPill].
 */
@Composable
fun AnimatedDots(
    modifier: Modifier = Modifier,
    color: Color = KrailTheme.colors.onSurface,
    dotRadius: Dp = 4.dp,
    dotSpacing: Dp = 14.dp,
    bounceHeight: Dp = 10.dp,
) {
    val infiniteTransition = rememberInfiniteTransition()

    // Animations are now unit-less fractions in [-1, 0] that we scale to bounceHeight
    // in pixels at draw time — so changing bounceHeight scales the bounce smoothly
    // rather than coupling animation timing to the current screen density.
    val dot1Frac by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    val dot2Frac by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing, delayMillis = 200),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    val dot3Frac by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing, delayMillis = 400),
            repeatMode = RepeatMode.Reverse,
        ),
    )

    Canvas(modifier = modifier) {
        val radiusPx = dotRadius.toPx()
        val spacingPx = dotSpacing.toPx()
        val bouncePx = bounceHeight.toPx()
        val centerY = size.height / 2
        val centerX = size.width / 2

        drawCircle(
            color = color,
            radius = radiusPx,
            center = Offset(centerX - spacingPx, centerY + dot1Frac * bouncePx),
        )
        drawCircle(
            color = color,
            radius = radiusPx,
            center = Offset(centerX, centerY + dot2Frac * bouncePx),
        )
        drawCircle(
            color = color,
            radius = radiusPx,
            center = Offset(centerX + spacingPx, centerY + dot3Frac * bouncePx),
        )
    }
}
