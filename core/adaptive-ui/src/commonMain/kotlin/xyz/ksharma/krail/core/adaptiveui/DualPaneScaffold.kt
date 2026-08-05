package xyz.ksharma.krail.core.adaptiveui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.core.analytics.AnalyticsPane
import xyz.ksharma.krail.core.analytics.AnalyticsPaneTracker

/**
 * The one dual-pane split used by every screen (SavedTrips, SearchStop, …) so the two-pane
 * layout can never drift between screens again.
 *
 * Layout: a [Row] with a FIXED-width [listPane] on the left and a weight(1f) [rightPane]
 * filling the remainder on the right.
 *
 * Two invariants are baked in because violating either produced iOS-only blank-map bugs that
 * cost a long debugging saga:
 *
 *  1. **Fixed-width list pane.** The left pane is a hard [listPaneWidth], never a flexible
 *     `widthIn`. A flexible-width sibling next to the map's `weight(1f)` gives iOS UIKitView
 *     interop unstable, multi-pass constraints so the native MLNMapView frame never settles.
 *     Fixed width makes the map's weight slot deterministic in a single measure pass.
 *
 *  2. **The right pane is a SIBLING of the list pane, never a descendant.** Callers must NOT
 *     wrap both panes in a shared `CloudGradientBackground` (or any ancestor that uses
 *     `graphicsLayer { compositingStrategy = Offscreen }`). On iOS a UIKitView (MapLibre's
 *     MLNMapView) cannot composite into an offscreen GPU buffer and renders blank. Put any
 *     gradient/background INSIDE the [listPane] lambda only; the map in [rightPane] then has
 *     no offscreen ancestor.
 *
 * When [rightPane] is null the [listPane] fills the full width (no blank gap on the right).
 */
@Composable
fun DualPaneScaffold(
    listPane: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    listPaneWidth: Dp = DUAL_PANE_LIST_WIDTH,
    rightPane: (@Composable BoxScope.() -> Unit)? = null,
) {
    val isDualPane = rightPane != null

    // A single pane means the question "which pane" has no answer, so say so rather than
    // leaving a stale LIST/DETAIL behind from the last dual-pane screen.
    DisposableEffect(isDualPane) {
        if (!isDualPane) AnalyticsPaneTracker.set(AnalyticsPane.SINGLE)
        onDispose { AnalyticsPaneTracker.set(AnalyticsPane.SINGLE) }
    }

    Row(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .then(
                    if (isDualPane) Modifier.width(listPaneWidth) else Modifier.fillMaxWidth(),
                )
                .fillMaxHeight()
                .then(
                    if (isDualPane) Modifier.trackPaneTouches(AnalyticsPane.LIST) else Modifier,
                ),
        ) {
            listPane()
        }

        if (rightPane != null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .trackPaneTouches(AnalyticsPane.DETAIL),
            ) {
                rightPane()
            }
        }
    }
}

/**
 * Width of the list pane in a dual-pane layout. Keeps stop/trip rows at ≈ phone width so they
 * stay readable while the map fills the rest. See docs/TABLET_FOLDABLE_UX.md §2.
 */
val DUAL_PANE_LIST_WIDTH: Dp = 480.dp

/**
 * Records the pane a touch landed in, so every analytics event fired afterwards can say
 * where the rider was working.
 *
 * Observes on [PointerEventPass.Initial] and consumes nothing: the pane learns about the
 * touch before its content does, and the content still receives it untouched. Attribution
 * by touch rather than by "which screen is on top" is deliberate - in a dual-pane layout
 * both panes are visible, so topmost would credit the detail pane for list-pane taps.
 */
private fun Modifier.trackPaneTouches(pane: AnalyticsPane): Modifier = pointerInput(pane) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial)
            AnalyticsPaneTracker.set(pane)
        }
    }
}
