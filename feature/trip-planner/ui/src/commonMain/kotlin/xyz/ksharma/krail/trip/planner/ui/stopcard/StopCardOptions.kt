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
 *
 * [pickText] is the words of the primary action when the host knows what the rider is doing (Search Stop: Set as
 * destination, Set as starting point, Set as Home). Null keeps the words of [pickRole] (Go here, Start here, Select stop),
 * which the home map has always shown.
 */
@Stable
data class StopCardOptions(
    val pickRole: PickRole = PickRole.Choose,
    val labels: ImmutableList<StopLabel> = persistentListOf(),
    val onSaveAs: (label: StopLabel, stop: StopItem) -> Unit = { _, _ -> },
    val pickText: String? = null,
)
