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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalWindowInfo
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.isAppInDarkMode
import xyz.ksharma.krail.taj.themeColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Slowly morphing "cloud" gradient suitable as a full-screen background.
 *
 * Three large radial blobs — all derived from the user's theme color at
 * descending tint strengths — drift on coprime periods over a
 * [KrailTheme.colors.surface] base. Each blob also breathes its radius on a
 * separate slow cycle, so the *combined* envelope morphs (a downward triangle
 * collapsing into an arc, etc.) rather than reading as discrete circles.
 *
 * Why every centre sits in the top quarter of the screen with capped y-drift:
 * by request the cloud field stays in the top half only, so the bottom of the
 * screen reads as a clean [KrailTheme.colors.surface] wash. Centres are at
 * y ≤ 0.25 with small y-amplitude so the *visible* radial fade lands roughly
 * at the mid-line on a typical phone aspect ratio (h ≈ 2w).
 *
 * Why blob centres and radii are anchored to [LocalWindowInfo.containerSize]:
 * the window size stays constant when the IME shows, while a parent that uses
 * `imePadding()` will shrink the local draw area. Anchoring to window space
 * keeps the blobs visually still — only the visible bottom edge gets clipped
 * as the keyboard rises, so the gradient feels like a fixed sky behind the
 * content rather than something that lurches up with the keyboard.
 *
 * Why a 3-stop radial (peak → mid → transparent) and not a 2-stop:
 * a single peak→transparent stop falls off too fast and reads as a hard disc.
 * The mid-stop spreads the bulk of each blob's color over ~45% of its radius,
 * which is what lets the three blobs merge into one continuous field instead
 * of looking like three separate planets.
 *
 * Performance:
 *   - Single drawBehind. Only the lambda re-runs each frame.
 *   - Offscreen compositing so transparent blob edges blend cleanly into one
 *     layer instead of painting straight onto the destination.
 *   - No Modifier.blur — the multi-stop falloff already gives soft edges, and
 *     per-frame blur is significantly more expensive on both Android and Skia.
 *   - Brushes are allocated inside drawBehind (radial gradients are cheap);
 *     the brush has to change every frame anyway because center/radius animate.
 *
 * The driving InfiniteTransition pauses automatically when the host stops
 * producing frames (background, locked screen).
 */
@Composable
fun CloudGradientBackground(
    modifier: Modifier = Modifier,
    themeColor: Color = themeColor(),
    content: @Composable BoxScope.() -> Unit = {},
) {
    val surface = KrailTheme.colors.surface
    val darkMode = isAppInDarkMode()
    val peakAlpha = if (darkMode) DARK_PEAK_ALPHA else LIGHT_PEAK_ALPHA

    val tintA = lerp(themeColor, surface, BLOB_TINT_A_BLEND)
    val tintB = lerp(themeColor, surface, BLOB_TINT_B_BLEND)

    val windowSize = LocalWindowInfo.current.containerSize
    val refSize: Size? = remember(windowSize.width, windowSize.height) {
        if (windowSize.width > 0 && windowSize.height > 0) {
            Size(windowSize.width.toFloat(), windowSize.height.toFloat())
        } else {
            null
        }
    }

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
    // Separate "breathe" phase drives radius pulsing — independent of position
    // so the envelope shape morphs even when blob centres happen to align.
    val breathe by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(BREATHE_PERIOD_MS, easing = LinearEasing)),
        label = "cloud-breathe",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surface)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawBehind {
                val ref = refSize ?: size
                val pulse = sin(breathe * TWO_PI) * RADIUS_BREATHE_AMP
                val antiPulse = -pulse

                drawBlob(
                    referenceSize = ref,
                    color = themeColor,
                    peakAlpha = peakAlpha,
                    centerFrac = Offset(
                        x = BLOB_1_CENTER_X + BLOB_1_AMP_X * sin(t1 * TWO_PI),
                        y = BLOB_1_CENTER_Y + BLOB_1_AMP_Y * cos(t1 * TWO_PI),
                    ),
                    rFrac = BLOB_1_RADIUS_FRAC + pulse,
                )
                drawBlob(
                    referenceSize = ref,
                    color = tintA,
                    peakAlpha = peakAlpha,
                    centerFrac = Offset(
                        x = BLOB_2_CENTER_X + BLOB_2_AMP_X * cos(t2 * TWO_PI),
                        y = BLOB_2_CENTER_Y + BLOB_2_AMP_Y * sin(t2 * TWO_PI),
                    ),
                    rFrac = BLOB_2_RADIUS_FRAC + antiPulse,
                )
                drawBlob(
                    referenceSize = ref,
                    color = tintB,
                    peakAlpha = peakAlpha * BLOB_3_ALPHA_SCALE,
                    centerFrac = Offset(
                        x = BLOB_3_CENTER_X + BLOB_3_AMP_X * sin(t3 * TWO_PI),
                        y = BLOB_3_CENTER_Y + BLOB_3_AMP_Y * cos(t3 * TWO_PI),
                    ),
                    rFrac = BLOB_3_RADIUS_FRAC + pulse * BLOB_3_BREATHE_SCALE,
                )
            },
        content = content,
    )
}

private fun DrawScope.drawBlob(
    referenceSize: Size,
    color: Color,
    peakAlpha: Float,
    centerFrac: Offset,
    rFrac: Float,
) {
    val center = Offset(referenceSize.width * centerFrac.x, referenceSize.height * centerFrac.y)
    val radius = referenceSize.minDimension * rFrac
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to color.copy(alpha = peakAlpha),
                BLOB_MID_STOP to color.copy(alpha = peakAlpha * MID_ALPHA_RATIO),
                1f to Color.Transparent,
            ),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

private const val TWO_PI = (2 * PI).toFloat()

// Light mode tolerates a stronger peak because surface is white; dark mode
// needs much less alpha or the saturated theme colour reads as a hot spot.
private const val LIGHT_PEAK_ALPHA = 0.55f
private const val DARK_PEAK_ALPHA = 0.32f
private const val MID_ALPHA_RATIO = 0.45f
private const val BLOB_MID_STOP = 0.45f

private const val RADIUS_BREATHE_AMP = 0.10f
private const val BREATHE_PERIOD_MS = 23_000

private const val BLOB_TINT_A_BLEND = 0.35f
private const val BLOB_TINT_B_BLEND = 0.60f

// All four periods (incl. BREATHE_PERIOD_MS above) are pairwise coprime so
// blobs never sync into a visible pulse.
//
// Sizing rule of thumb: on a phone (h ≈ 2w, minDim = w), a blob's *visible*
// fade reaches roughly 0.7 × radius beyond its centre. With centres at
// y ≤ 0.25 and radii ≤ ~0.85, the visible field stays in the top 50%.
private const val BLOB_1_PERIOD_MS = 16_000
private const val BLOB_1_CENTER_X = 0.30f
private const val BLOB_1_CENTER_Y = 0.12f
private const val BLOB_1_AMP_X = 0.10f
private const val BLOB_1_AMP_Y = 0.05f
private const val BLOB_1_RADIUS_FRAC = 0.85f

private const val BLOB_2_PERIOD_MS = 19_000
private const val BLOB_2_CENTER_X = 0.70f
private const val BLOB_2_CENTER_Y = 0.12f
private const val BLOB_2_AMP_X = 0.10f
private const val BLOB_2_AMP_Y = 0.05f
private const val BLOB_2_RADIUS_FRAC = 0.85f

private const val BLOB_3_PERIOD_MS = 25_000
private const val BLOB_3_CENTER_X = 0.50f
private const val BLOB_3_CENTER_Y = 0.22f
private const val BLOB_3_AMP_X = 0.12f
private const val BLOB_3_AMP_Y = 0.05f
private const val BLOB_3_RADIUS_FRAC = 0.70f
private const val BLOB_3_ALPHA_SCALE = 0.85f
private const val BLOB_3_BREATHE_SCALE = 0.6f

@PreviewComponent
@Composable
private fun CloudGradientBackgroundTrainPreview() {
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
