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
import xyz.ksharma.krail.taj.theme.KrailTheme

/**
 * Three dots that bob up and down in sequence — used as a generic loading indicator.
 *
 * The dots are drawn into a Canvas, so size is governed by the [modifier] you pass.
 * For consistent visuals across screens prefer wrapping with [LoadingDotsPill].
 */
@Composable
fun AnimatedDots(
    modifier: Modifier = Modifier,
    color: Color = KrailTheme.colors.onSurface,
) {
    val infiniteTransition = rememberInfiniteTransition()

    val dot1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -30f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    val dot2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -30f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing, delayMillis = 200),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    val dot3Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -30f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing, delayMillis = 400),
            repeatMode = RepeatMode.Reverse,
        ),
    )

    Canvas(modifier = modifier) {
        val dotRadius = 10f
        val centerY = size.height / 2
        val centerX = size.width / 2
        val spacing = 40f

        drawCircle(
            color = color,
            radius = dotRadius,
            center = Offset(centerX - spacing, centerY + dot1Offset),
        )
        drawCircle(
            color = color,
            radius = dotRadius,
            center = Offset(centerX, centerY + dot2Offset),
        )
        drawCircle(
            color = color,
            radius = dotRadius,
            center = Offset(centerX + spacing, centerY + dot3Offset),
        )
    }
}
