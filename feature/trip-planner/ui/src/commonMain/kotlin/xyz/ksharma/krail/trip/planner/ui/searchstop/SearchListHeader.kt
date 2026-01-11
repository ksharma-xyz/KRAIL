package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import xyz.ksharma.krail.trip.planner.ui.components.loading.AnimatedDots
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Generic fixed-height header that always occupies `height` space and centers its content.
 * - `content` can be empty (acts as a spacer) or show any composable (e.g. loading dots).
 * - `visibleControl` is a helper boolean you pass (e.g. isLoading). Internally this composable
 *   ensures the content is visible for at least [minVisibleMillis] to avoid flicker.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun SearchListHeader(
    height: Dp = 32.dp,
    modifier: Modifier = Modifier,
    visibleControl: Boolean = false,
    minVisibleMillis: Long = 600L,
    content: @Composable () -> Unit = {}
) {
    // internalVisible enforces "min visible time" behaviour
    val internalVisibleState = remember { mutableStateOf(false) }
    val lastShownAt = remember { mutableStateOf(0L) }

    LaunchedEffect(visibleControl) {
        val now = Clock.System.now().toEpochMilliseconds()
        if (visibleControl) {
            internalVisibleState.value = true
            lastShownAt.value = now
        } else {
            // If we were shown, ensure we remain visible for at least minVisibleMillis
            if (internalVisibleState.value) {
                val elapsed = Clock.System.now().toEpochMilliseconds() - lastShownAt.value
                val remaining = minVisibleMillis - elapsed
                if (remaining > 0) delay(remaining)
                internalVisibleState.value = false
            } else {
                internalVisibleState.value = false
            }
        }
    }

    Box(
        modifier = modifier.height(height),
        contentAlignment = Alignment.Center
    ) {
        // center the provided content both vertically and horizontally
        AnimatedVisibility(
            visible = internalVisibleState.value,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                content()
            }
        }
    }
}

/**
 * Small convenience wrapper to show the animated dots inside the fixed header.
 * - Pass `isLoading` from your list state.
 * - Will ensure minVisibleMillis to avoid flicker when searches are fast.
 */
@Composable
fun SearchingDotsHeader(
    isLoading: Boolean,
    height: Dp = 32.dp,
    modifier: Modifier = Modifier,
    minVisibleMillis: Long = 600L,
) {
    SearchListHeader(
        height = height,
        modifier = modifier,
        visibleControl = isLoading,
        minVisibleMillis = minVisibleMillis,
    ) {
        AnimatedDots(modifier = Modifier.fillMaxWidth())
    }
}
