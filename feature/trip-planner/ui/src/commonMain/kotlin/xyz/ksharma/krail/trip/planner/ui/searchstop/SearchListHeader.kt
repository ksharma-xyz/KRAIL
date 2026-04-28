package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.delay
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.themeColor
import xyz.ksharma.krail.trip.planner.ui.components.loading.AnimatedDots
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Compact "searching…" indicator. Renders as a small floating pill (onSurface
 * background, themeColor dots) so callers can stack it on the z-axis over their list
 * without the dots claiming a chunk of vertical layout space.
 *
 * Pass [isLoading] from your list state. The pill stays visible at least
 * [minVisibleMillis] to avoid flickering when searches return instantly.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun SearchingDotsHeader(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
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
        enter = fadeIn() + scaleIn(initialScale = 0.85f),
        exit = fadeOut() + scaleOut(targetScale = 0.85f),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(dim.radiusFull))
                .background(KrailTheme.colors.onSurface)
                .padding(horizontal = dim.spacingL, vertical = dim.spacingS),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedDots(color = themeColor())
        }
    }
}
