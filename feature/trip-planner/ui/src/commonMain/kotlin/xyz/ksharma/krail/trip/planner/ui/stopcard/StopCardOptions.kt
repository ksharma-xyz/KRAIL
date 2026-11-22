package xyz.ksharma.krail.trip.planner.ui.stopcard

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * What a floating stop card offers for its stop besides showing it: what picking the stop does to the
 * trip, the labels the stop can be saved as, and what a tap on one of them does. The defaults offer the
 * stop's one plain action and no labels, which is what a host that has neither gets.
 */
@Stable
data class StopCardOptions(
    val pickRole: PickRole = PickRole.Choose,
    val labels: ImmutableList<StopLabel> = persistentListOf(),
    val onSaveAs: (label: StopLabel, stop: StopItem) -> Unit = { _, _ -> },
)
