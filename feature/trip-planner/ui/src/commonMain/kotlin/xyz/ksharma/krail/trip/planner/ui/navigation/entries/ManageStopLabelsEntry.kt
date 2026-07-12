package xyz.ksharma.krail.trip.planner.ui.navigation.entries

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.trip.planner.ui.components.ConfirmLabelActionSheet
import xyz.ksharma.krail.trip.planner.ui.managestoplabels.ManageStopLabelsScreen
import xyz.ksharma.krail.trip.planner.ui.navigation.ManageStopLabelsRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.TripPlannerNavigator
import xyz.ksharma.krail.trip.planner.ui.searchstop.SearchStopViewModel
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent

/**
 * A real nav destination, not a sheet — see `STOP_LABEL_UX_REDESIGN_PROPOSAL.md`.
 * Reuses `SearchStopViewModel` for its label event handlers (Rename/ClearLabelStop/
 * DeleteLabel/MoveLabelToIndex already live there and observe Sandook reactively) —
 * a dedicated ManageStopLabelsViewModel would just duplicate that same observer.
 */
@Composable
internal fun EntryProviderScope<NavKey>.ManageStopLabelsEntry(
    tripPlannerNavigator: TripPlannerNavigator,
) {
    entry<ManageStopLabelsRoute> {
        val viewModel: SearchStopViewModel = koinViewModel()
        val searchStopState by viewModel.uiState.collectAsStateWithLifecycle()

        // Delete needs a confirm (J7); Remove assignment fires instantly.
        var pendingDeleteLabel by rememberSaveable(stateSaver = StopLabelSaver) {
            mutableStateOf<StopLabel?>(null)
        }

        ManageStopLabelsScreen(
            stopLabels = searchStopState.stopLabels,
            onBackClick = { tripPlannerNavigator.goBack() },
            onRename = { label, newName ->
                viewModel.onEvent(SearchStopUiEvent.RenameLabel(labelKey = label.label, newName = newName))
            },
            onRemoveAssignment = { label ->
                viewModel.onEvent(SearchStopUiEvent.ClearLabelStop(label.label))
            },
            onDeleteLabel = { label -> pendingDeleteLabel = label },
            onMove = { labelKey, targetLabelKey ->
                viewModel.onEvent(
                    SearchStopUiEvent.MoveLabelToIndex(labelKey = labelKey, targetLabelKey = targetLabelKey),
                )
            },
        )

        val deleteTarget = pendingDeleteLabel
        if (deleteTarget != null) {
            ConfirmLabelActionSheet(
                title = "Delete \"${deleteTarget.label}\" label?",
                message = "This action will permanently remove the label.",
                primaryText = "Delete",
                primaryIsDestructive = true,
                onPrimary = {
                    viewModel.onEvent(SearchStopUiEvent.DeleteLabel(deleteTarget.label))
                    pendingDeleteLabel = null
                },
                onCancel = { pendingDeleteLabel = null },
            )
        }
    }
}

private val StopLabelSaver: Saver<StopLabel?, Any> = Saver(
    save = { label ->
        label?.let {
            mapOf<String, Any?>(
                "emoji" to it.emoji,
                "label" to it.label,
                "stopId" to it.stopId,
                "stopName" to it.stopName,
            )
        }
    },
    restore = { saved ->
        @Suppress("UNCHECKED_CAST")
        val map = saved as Map<String, Any?>
        StopLabel(
            emoji = map["emoji"] as String,
            label = map["label"] as String,
            stopId = map["stopId"] as String?,
            stopName = map["stopName"] as String?,
        )
    },
)
