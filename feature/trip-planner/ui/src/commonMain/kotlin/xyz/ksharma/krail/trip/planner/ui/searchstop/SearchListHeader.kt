package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import xyz.ksharma.krail.taj.components.LoadingDotsPill

/**
 * Thin alias kept for the existing call sites — the actual rendering, visibility-state
 * machine and pill styling live in [LoadingDotsPill] in taj so other features can
 * reuse the same indicator.
 */
@Composable
fun SearchingDotsHeader(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    minVisibleMillis: Long = 600L,
) {
    LoadingDotsPill(
        isLoading = isLoading,
        modifier = modifier,
        minVisibleMillis = minVisibleMillis,
    )
}
