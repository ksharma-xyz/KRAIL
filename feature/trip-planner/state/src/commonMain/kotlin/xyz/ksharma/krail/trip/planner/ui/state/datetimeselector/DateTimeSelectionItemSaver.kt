package xyz.ksharma.krail.trip.planner.ui.state.datetimeselector

import androidx.compose.runtime.saveable.Saver

/**
 * Custom Saver for DateTimeSelectionItem to enable rememberSaveable support.
 *
 * Uses JSON serialization (toJsonString/fromJsonString) to convert the object
 * to/from a String that can be saved across configuration changes.
 *
 * This is necessary because DateTimeSelectionItem contains LocalDate which is
 * not Parcelable by default.
 */
val DateTimeSelectionItemSaver: Saver<DateTimeSelectionItem?, String> = Saver(
    save = { item ->
        item?.toJsonString() ?: ""
    },
    restore = { json ->
        if (json.isEmpty()) null
        else DateTimeSelectionItem.fromJsonString(json)
    }
)
