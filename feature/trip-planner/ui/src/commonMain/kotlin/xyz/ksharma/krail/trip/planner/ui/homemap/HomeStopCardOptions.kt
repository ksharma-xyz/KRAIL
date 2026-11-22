package xyz.ksharma.krail.trip.planner.ui.homemap

import kotlinx.collections.immutable.ImmutableList
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.stopcard.PickRole
import xyz.ksharma.krail.trip.planner.ui.stopcard.StopCardOptions

/**
 * What a floating stop card offers on home: a pin picks the destination, so it reads Go here, and Save as
 * gives the stop a label through the same event the Home and Work chips use when a stop is chosen for them,
 * which refuses a stop another label already holds. Home has no start search, so it never reads Start here.
 */
internal fun homeStopCardOptions(
    labels: ImmutableList<StopLabel>,
    onEvent: (SavedTripUiEvent) -> Unit,
): StopCardOptions = StopCardOptions(
    pickRole = PickRole.Destination,
    labels = labels,
    onSaveAs = { label, stop ->
        onEvent(
            SavedTripUiEvent.LabelStopAssigned(labelKey = label.label, stopId = stop.stopId, stopName = stop.stopName),
        )
    },
)
