@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.withTimeoutOrNull
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.components.SetLabelPill
import xyz.ksharma.krail.trip.planner.ui.components.UnsetLabelPill
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

@Suppress("LongParameterList")
@Composable
internal fun LazyItemScope.LabelShortcutPill(
    label: StopLabel,
    isAssigning: Boolean,
    editing: Boolean,
    reorderState: ReorderableLazyListState,
    onSetLabelClick: (StopItem) -> Unit,
    onUnsetLabelClick: (StopLabel) -> Unit,
    onEnterEditing: () -> Unit,
    onDeleteLabel: (StopLabel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val haptic = LocalHapticFeedback.current
    ReorderableItem(reorderState, key = label.label, modifier = modifier) { isDragging ->
        val rotation = rememberWiggleRotation(
            active = editing && !isDragging,
            seed = label.label.hashCode(),
        )
        // Scale up the pill when it's the assigning target — same visual
        // language as the departure-board chips. We don't tint the pill
        // here because the screen background is already themeColor and a
        // colour change would blend the pill in.
        val targetScale = when {
            isDragging -> PILL_DRAG_SCALE
            isAssigning -> PILL_ASSIGNING_SCALE
            else -> PILL_RESTING_SCALE
        }
        val scale by animateFloatAsState(
            targetValue = targetScale,
            label = "pill-scale",
        )
        Box(
            modifier = Modifier.graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
            },
        ) {
            // clip BEFORE the gesture detector so the ripple is contained inside
            // the rounded shape. longPressDraggableHandle (active only while
            // editing) handles long-press + drag for reordering. The custom
            // awaitEachGesture below distinguishes tap (release inside long-press
            // timeout) from long-press (timeout reached) without competing with
            // the drag handle for events the way combinedClickable did.
            val pillModifier = Modifier
                .clip(RoundedCornerShape(dim.radiusFull))
                .longPressDraggableHandle(
                    enabled = editing,
                    onDragStarted = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                )
                .labelPillTapGestures(
                    label = label,
                    editing = editing,
                    haptic = haptic,
                    onSetLabelClick = onSetLabelClick,
                    onUnsetLabelClick = onUnsetLabelClick,
                    onEnterEditing = onEnterEditing,
                )

            if (label.isSet) {
                SetLabelPill(label = label, modifier = pillModifier)
            } else {
                UnsetLabelPill(
                    label = label,
                    isAssigning = isAssigning,
                    modifier = pillModifier,
                )
            }

            if (editing && !label.isProtected) {
                DeleteOverlay(
                    onClick = { onDeleteLabel(label) },
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
    }
}

@Suppress("LongParameterList")
private fun Modifier.labelPillTapGestures(
    label: StopLabel,
    editing: Boolean,
    haptic: HapticFeedback,
    onSetLabelClick: (StopItem) -> Unit,
    onUnsetLabelClick: (StopLabel) -> Unit,
    onEnterEditing: () -> Unit,
): Modifier = pointerInput(label.label, editing) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        val tappedQuickly = withTimeoutOrNull(
            viewConfiguration.longPressTimeoutMillis,
        ) {
            waitForUpOrCancellation() != null
        }
        when (tappedQuickly) {
            true -> {
                // Released within the long-press window → tap.
                // In edit mode, taps are suppressed entirely so an
                // accidental brush against a pill mid-reorder can't
                // navigate away or enter assigning mode. Only drag
                // and the ✕ delete chip work while editing.
                if (!editing) {
                    if (label.isSet) {
                        label.toStopItem()?.let(onSetLabelClick)
                    } else {
                        onUnsetLabelClick(label)
                    }
                }
            }
            null -> {
                // Long-press timeout reached without release.
                if (!editing) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEnterEditing()
                }
                // Wait for release; null return means another
                // detector (the drag handle) took over.
                waitForUpOrCancellation()
            }
            false -> {
                // Cancellation inside timeout (e.g. drag detector
                // consumed the events) — let the drag handle run.
            }
        }
    }
}
