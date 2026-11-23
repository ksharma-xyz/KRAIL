package xyz.ksharma.krail.trip.planner.ui.savedtrips

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.sandook.db.StopLabels
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel

/** The stored label rows as the labels the screens read, in the order the table gives them. */
internal fun List<StopLabels>.toStopLabels(): ImmutableList<StopLabel> = map { row ->
    StopLabel(
        emoji = row.emoji,
        label = row.label,
        stopId = row.stop_id,
        stopName = row.stop_name,
    )
}.toImmutableList()
