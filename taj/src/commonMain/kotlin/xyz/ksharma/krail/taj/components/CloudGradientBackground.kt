package xyz.ksharma.krail.taj.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Slowly drifting "cloud" gradient suitable as a full-screen background.
 *
 * Three large radial blobs feather to transparent and translate on independent
 * non-harmonic sine paths over a [KrailTheme.colors.surface] base. Periods are
 * deliberately coprime so the blobs never sync into a visible pulse.
 *
 * Why it stays cheap:
 *   - Single drawBehind — only the lambda re-runs each frame, no recomposition.
 *   - Offscreen compositing is required so the blobs' transparent edges blend
 *     into a single layer instead of painting straight onto the destination
 *     (without it, soft edges look chalky against the surface wash).
 *   - No Modifier.blur — the radial feather already gives soft edges, and per-
 *     frame blur is significantly more expensive on both Android and iOS Skia.
 *
 * The driving InfiniteTransition is paused automatically when the host activity
 * stops producing frames (background, locked screen), so there is no separate
 * lifecycle plumbing to do here.
 */
@Composable
fun CloudGradientBackground(
    modifier: Modifier = Modifier,
    themeColor: Color = themeColor(),
    content: @Composable BoxScope.() -> Unit = {},
) {
    val transition = rememberInfiniteTransition(label = "cloud-gradient")
    val t1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(BLOB_1_PERIOD_MS, easing = LinearEasing)),
        label = "cloud-blob-1",
    )
    val t2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(BLOB_2_PERIOD_MS, easing = LinearEasing)),
        label = "cloud-blob-2",
    )
    val t3 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(BLOB_3_PERIOD_MS, easing = LinearEasing)),
        label = "cloud-blob-3",
    )

    val surface = KrailTheme.colors.surface
    val tintA = lerp(themeColor, surface, BLOB_TINT_A_BLEND)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surface)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawBehind {
                drawBlob(
                    centerFrac = Offset(
                        x = BLOB_1_CENTER_X + BLOB_1_AMP_X * sin(t1 * TWO_PI),
                        y = BLOB_1_CENTER_Y + BLOB_1_AMP_Y * cos(t1 * TWO_PI),
                    ),
                    color = themeColor,
                    radiusFrac = BLOB_1_RADIUS_FRAC,
                )
                drawBlob(
                    centerFrac = Offset(
                        x = BLOB_2_CENTER_X + BLOB_2_AMP_X * cos(t2 * TWO_PI),
                        y = BLOB_2_CENTER_Y + BLOB_2_AMP_Y * sin(t2 * TWO_PI),
                    ),
                    color = tintA,
                    radiusFrac = BLOB_2_RADIUS_FRAC,
                )
                drawBlob(
                    centerFrac = Offset(
                        x = BLOB_3_CENTER_X + BLOB_3_AMP_X * sin(t3 * TWO_PI),
                        y = BLOB_3_CENTER_Y + BLOB_3_AMP_Y * cos(t3 * TWO_PI),
                    ),
                    color = surface,
                    radiusFrac = BLOB_3_RADIUS_FRAC,
                )
            },
        content = content,
    )
}

private fun DrawScope.drawBlob(
    centerFrac: Offset,
    color: Color,
    radiusFrac: Float,
) {
    val center = Offset(size.width * centerFrac.x, size.height * centerFrac.y)
    val radius = size.minDimension * radiusFrac
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = BLOB_PEAK_ALPHA), Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

private const val TWO_PI = (2 * PI).toFloat()

// Coprime periods avoid a perceived pulse when blobs would otherwise re-align.
private const val BLOB_1_PERIOD_MS = 18_000
private const val BLOB_2_PERIOD_MS = 23_000
private const val BLOB_3_PERIOD_MS = 31_000

private const val BLOB_PEAK_ALPHA = 0.55f
private const val BLOB_TINT_A_BLEND = 0.4f

private const val BLOB_1_CENTER_X = 0.20f
private const val BLOB_1_CENTER_Y = 0.25f
private const val BLOB_1_AMP_X = 0.15f
private const val BLOB_1_AMP_Y = 0.10f
private const val BLOB_1_RADIUS_FRAC = 0.75f

private const val BLOB_2_CENTER_X = 0.80f
private const val BLOB_2_CENTER_Y = 0.40f
private const val BLOB_2_AMP_X = 0.12f
private const val BLOB_2_AMP_Y = 0.18f
private const val BLOB_2_RADIUS_FRAC = 0.65f

private const val BLOB_3_CENTER_X = 0.50f
private const val BLOB_3_CENTER_Y = 0.85f
private const val BLOB_3_AMP_X = 0.20f
private const val BLOB_3_AMP_Y = 0.08f
private const val BLOB_3_RADIUS_FRAC = 0.90f

@PreviewComponent
@Composable
private fun CloudGradientBackgroundPreview() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        CloudGradientBackground(modifier = Modifier.fillMaxSize())
    }
}

@PreviewComponent
@Composable
private fun CloudGradientBackgroundMetroPreview() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        CloudGradientBackground(modifier = Modifier.fillMaxSize())
    }
}
