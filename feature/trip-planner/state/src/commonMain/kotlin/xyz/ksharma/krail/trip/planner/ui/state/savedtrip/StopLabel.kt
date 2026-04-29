package xyz.ksharma.krail.trip.planner.ui.state.savedtrip

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

@Stable
data class StopLabel(
    val emoji: String,
    val label: String,
    val stopId: String? = null,
    val stopName: String? = null,
) {
    val isSet: Boolean get() = stopId != null && stopName != null

    /**
     * Home is the always-on shortcut: it's seeded on first install and the user can't
     * delete it. They can still clear/reassign its stop. Other labels (including Work)
     * can be deleted freely and added back via "+ Add".
     */
    val isProtected: Boolean get() = label.equals(PROTECTED_LABEL, ignoreCase = true)

    fun toStopItem(): StopItem? = if (isSet) StopItem(stopId = stopId!!, stopName = stopName!!) else null

    companion object {
        const val PROTECTED_LABEL = "Home"

        val defaults: ImmutableList<StopLabel> = persistentListOf(
            StopLabel(emoji = "🏠", label = PROTECTED_LABEL),
            StopLabel(emoji = "💼", label = "Work"),
        )
    }
}
