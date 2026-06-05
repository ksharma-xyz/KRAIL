package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.ImmutableList
import xyz.ksharma.krail.trip.planner.ui.components.AddLabelBottomSheet
import xyz.ksharma.krail.trip.planner.ui.components.LabelConflictSheet
import xyz.ksharma.krail.trip.planner.ui.components.SaveStopAsLabelSheet
import xyz.ksharma.krail.trip.planner.ui.components.labelNamesMatch
import xyz.ksharma.krail.trip.planner.ui.components.normaliseLabelName
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

// The label-management modals that hang off SearchStopScreen, split into one composable per
// sheet so each flow reads on its own. Each is self-guarding (renders nothing unless its state
// is active), so the screen calls all three unconditionally. State is hoisted in the caller;
// these read it and report changes back through the on*Change callbacks. They live in this file
// (not SearchStopScreen.kt) so their branching does not count toward the screen's complexity.

/** "Create label" sheet. Shown while [show] is true. */
@Composable
internal fun SearchStopAddLabelDialog(
    show: Boolean,
    stopLabels: ImmutableList<StopLabel>,
    pendingStopForNewLabel: StopItem?,
    onEvent: (SearchStopUiEvent) -> Unit,
    onShowAddLabelSheetChange: (Boolean) -> Unit,
    onStopBeingSavedChange: (StopItem?) -> Unit,
    onPendingStopForNewLabelChange: (StopItem?) -> Unit,
    onAssigningLabelChange: (StopLabel?) -> Unit,
) {
    if (!show) return
    AddLabelBottomSheet(
        stopName = null,
        existingLabels = stopLabels,
        onDismiss = {
            onShowAddLabelSheetChange(false)
            // If the user backed out without saving, hand the stop back to the
            // save-sheet flow so they're not stranded.
            onStopBeingSavedChange(pendingStopForNewLabel)
            onPendingStopForNewLabelChange(null)
        },
        onSave = { emoji, name ->
            onShowAddLabelSheetChange(false)
            val cleaned = normaliseLabelName(name)
            if (cleaned.isNotBlank() &&
                stopLabels.none { labelNamesMatch(it.label, cleaned) }
            ) {
                onEvent(SearchStopUiEvent.CreateLabel(name = cleaned, emoji = emoji))
                val pending = pendingStopForNewLabel
                onPendingStopForNewLabelChange(null)
                if (pending != null) {
                    // Came from "+ New label" inside SaveStopAsLabelSheet —
                    // auto-attach the stop to the freshly created label so the
                    // user's original intent ("save this stop as a new label")
                    // completes in one go.
                    onEvent(SearchStopUiEvent.AssignLabelStop(cleaned, pending))
                } else {
                    // Came from the standalone "+ Add" pill — drop into assigning
                    // mode so the user knows what to do next.
                    onAssigningLabelChange(StopLabel(emoji = emoji, label = cleaned))
                }
            } else {
                // Save was blocked (blank or duplicate). Restore the save-sheet
                // flow so the user can pick a different option.
                onStopBeingSavedChange(pendingStopForNewLabel)
                onPendingStopForNewLabelChange(null)
            }
        },
    )
}

/** "Save stop as label" sheet. Shown while [stopBeingSaved] is non-null. */
@Composable
internal fun SearchStopSaveLabelDialog(
    stopBeingSaved: StopItem?,
    stopLabels: ImmutableList<StopLabel>,
    onEvent: (SearchStopUiEvent) -> Unit,
    onShowAddLabelSheetChange: (Boolean) -> Unit,
    onStopBeingSavedChange: (StopItem?) -> Unit,
    onPendingConflictChange: (LabelConflict?) -> Unit,
    onPendingStopForNewLabelChange: (StopItem?) -> Unit,
) {
    val stop = stopBeingSaved ?: return
    SaveStopAsLabelSheet(
        stopName = stop.stopName,
        labels = stopLabels,
        onLabelClick = { label ->
            val stopOnAnotherLabel = stopLabels.firstOrNull { other ->
                other.stopId == stop.stopId && other.label != label.label
            }
            val labelHasDifferentStop = label.takeIf {
                it.isSet && it.stopId != stop.stopId
            }
            when {
                stopOnAnotherLabel != null -> {
                    onPendingConflictChange(
                        LabelConflict.StopAlreadyOnAnotherLabel(
                            target = label,
                            stop = stop,
                            existingLabel = stopOnAnotherLabel,
                        ),
                    )
                    onStopBeingSavedChange(null)
                }
                labelHasDifferentStop != null -> {
                    onPendingConflictChange(
                        LabelConflict.LabelHasDifferentStop(
                            target = label,
                            stop = stop,
                            existingStopName = labelHasDifferentStop.stopName.orEmpty(),
                        ),
                    )
                    onStopBeingSavedChange(null)
                }
                else -> {
                    onEvent(SearchStopUiEvent.AssignLabelStop(label.label, stop))
                    onStopBeingSavedChange(null)
                }
            }
        },
        onCreateNewLabel = {
            // Carry the stop into the AddLabel flow so that creating the label
            // can auto-assign it on save, instead of dropping the user back at
            // step one.
            onPendingStopForNewLabelChange(stop)
            onStopBeingSavedChange(null)
            onShowAddLabelSheetChange(true)
        },
        onDismiss = { onStopBeingSavedChange(null) },
    )
}

/** 1:1 conflict-resolution sheet. Shown while [pendingConflict] is non-null. */
@Composable
internal fun SearchStopConflictDialog(
    pendingConflict: LabelConflict?,
    onEvent: (SearchStopUiEvent) -> Unit,
    onStopBeingSavedChange: (StopItem?) -> Unit,
    onPendingConflictChange: (LabelConflict?) -> Unit,
) {
    val conflict = pendingConflict ?: return
    // Cancelling a conflict warning hands the stop back to SaveStopAsLabelSheet so
    // the user can pick a different label instead of being stranded on the screen.
    val onConflictCancel = {
        onStopBeingSavedChange(conflict.stop)
        onPendingConflictChange(null)
    }
    when (conflict) {
        is LabelConflict.StopAlreadyOnAnotherLabel -> LabelConflictSheet(
            title = "Already saved",
            message = "${conflict.stop.stopName} is currently saved as " +
                "${conflict.existingLabel.label}. Move it to ${conflict.target.label}?",
            confirmLabel = "Move",
            onConfirm = {
                onEvent(SearchStopUiEvent.ClearLabelStop(conflict.existingLabel.label))
                onEvent(SearchStopUiEvent.AssignLabelStop(conflict.target.label, conflict.stop))
                onPendingConflictChange(null)
            },
            onCancel = onConflictCancel,
        )
        is LabelConflict.LabelHasDifferentStop -> LabelConflictSheet(
            title = "Already in use",
            message = "${conflict.target.label} is currently saved as " +
                "${conflict.existingStopName}. Replace with ${conflict.stop.stopName}?",
            confirmLabel = "Replace",
            onConfirm = {
                onEvent(SearchStopUiEvent.AssignLabelStop(conflict.target.label, conflict.stop))
                onPendingConflictChange(null)
            },
            onCancel = onConflictCancel,
        )
    }
}
