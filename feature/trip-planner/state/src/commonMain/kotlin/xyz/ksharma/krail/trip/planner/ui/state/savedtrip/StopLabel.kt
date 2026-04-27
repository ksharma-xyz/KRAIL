package xyz.ksharma.krail.trip.planner.ui.state.savedtrip

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

data class PendingNewLabel(val emoji: String, val name: String)

@Stable
data class StopLabel(
    val emoji: String,
    val label: String,
    val stopId: String? = null,
    val stopName: String? = null,
) {
    val isSet: Boolean get() = stopId != null && stopName != null

    fun toStopItem(): StopItem? = if (isSet) StopItem(stopId = stopId!!, stopName = stopName!!) else null

    companion object {
        val defaults: ImmutableList<StopLabel> = persistentListOf(
            StopLabel(emoji = "🏠", label = "Home"),
            StopLabel(emoji = "💼", label = "Work"),
        )
    }
}
