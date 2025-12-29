package xyz.ksharma.krail.trip.planner.ui.navigation.savers

import androidx.compose.runtime.saveable.Saver
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItem

fun dateTimeSelectionSaver(): Saver<DateTimeSelectionItem?, String> = Saver(
    save = { item -> item?.toJsonString() ?: "" },
    restore = { json ->
        if (json.isEmpty()) {
            null
        } else {
            DateTimeSelectionItem.fromJsonString(json)
        }
    },
)
