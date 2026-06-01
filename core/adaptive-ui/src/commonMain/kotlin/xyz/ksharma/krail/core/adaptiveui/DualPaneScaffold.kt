package xyz.ksharma.krail.core.adaptiveui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.core.log.log

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
 *
 * @param logTag temporary diagnostic label (e.g. "SavedTrips" / "SearchStop"). When non-null,
 *   the right pane logs its size + window position under the `[PANE_DIAG]` tag so two screens
 *   can be compared. Remove call-site tags once the layout is trusted.
 */
@Composable
fun DualPaneScaffold(
    listPane: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    listPaneWidth: Dp = DUAL_PANE_LIST_WIDTH,
    logTag: String? = null,
    rightPane: (@Composable BoxScope.() -> Unit)? = null,
) {
    Row(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .then(
                    if (rightPane != null) Modifier.width(listPaneWidth) else Modifier.fillMaxWidth(),
                )
                .fillMaxHeight(),
        ) {
            listPane()
        }

        if (rightPane != null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (logTag != null) {
                            Modifier
                                .onSizeChanged {
                                    log("[PANE_DIAG] $logTag rightPane size=${it.width}x${it.height}")
                                }
                                .onGloballyPositioned {
                                    log(
                                        "[PANE_DIAG] $logTag rightPane " +
                                            "pos=${it.positionInWindow()} size=${it.size}",
                                    )
                                }
                        } else {
                            Modifier
                        },
                    ),
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
