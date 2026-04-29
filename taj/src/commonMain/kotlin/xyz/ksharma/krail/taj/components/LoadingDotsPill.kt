package xyz.ksharma.krail.taj.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeColor
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Compact rounded pill containing [AnimatedDots]. Designed to float on the z-axis above
 * other content so the loading state doesn't claim a chunk of vertical layout space.
 *
 * @param isLoading external "is request in flight" boolean. The pill stays visible at
 *   least [minVisibleMillis] after this drops to false to avoid flicker on fast results.
 * @param backgroundColor pill background; defaults to the inverse-of-surface
 *   ([KrailColors.onSurface]) so the pill always pops against the screen background.
 * @param dotsColor colour of the bouncing dots; defaults to [themeColor] so the pill
 *   visually inherits the user's transport theme.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun LoadingDotsPill(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    backgroundColor: Color = KrailTheme.colors.onSurface,
    dotsColor: Color = themeColor(),
    minVisibleMillis: Long = 600L,
) {
    val internalVisibleState = remember { mutableStateOf(false) }
    val lastShownAt = remember { mutableLongStateOf(0L) }

    LaunchedEffect(isLoading) {
        val now = Clock.System.now().toEpochMilliseconds()
        if (isLoading) {
            internalVisibleState.value = true
            lastShownAt.value = now
        } else if (internalVisibleState.value) {
            val elapsed = Clock.System.now().toEpochMilliseconds() - lastShownAt.value
            val remaining = minVisibleMillis - elapsed
            if (remaining > 0) delay(remaining)
            internalVisibleState.value = false
        }
    }

    val dim = KrailTheme.dimensions
    AnimatedVisibility(
        visible = internalVisibleState.value,
        enter = fadeIn() + scaleIn(initialScale = ENTER_EXIT_SCALE),
        exit = fadeOut() + scaleOut(targetScale = ENTER_EXIT_SCALE),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(dim.radiusFull))
                .background(backgroundColor)
                .padding(horizontal = dim.spacingM, vertical = dim.spacingS),
            contentAlignment = Alignment.Center,
        ) {
            // Canvas is sized for AnimatedDots' default geometry: width ≥ 2*spacing
            // + 2*radius (= 36 dp) and height ≥ 2*(radius + bounceHeight) (= 28 dp),
            // with a couple of dp of margin so the bounce never touches the pill
            // border. Canvas has no intrinsic size, so this dp lock is required for
            // the dots to render.
            AnimatedDots(
                color = dotsColor,
                modifier = Modifier.size(width = DotsCanvasWidth, height = DotsCanvasHeight),
            )
        }
    }
}

private val DotsCanvasWidth = 44.dp
private val DotsCanvasHeight = 32.dp

// Scale-in/out target keeps the pill from popping at full size on appear; 0.85 is
// large enough to feel intentional, small enough to read as "growing in".
private const val ENTER_EXIT_SCALE = 0.85f

// region Previews

@Preview
@Composable
private fun PreviewLoadingDotsPill_Visible() {
    PreviewTheme {
        LoadingDotsPill(isLoading = true)
    }
}

// endregion
