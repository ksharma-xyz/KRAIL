package xyz.ksharma.krail.trip.planner.ui.savers

import androidx.compose.runtime.saveable.Saver
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItem

/**
 * Convenience Saver for DateTimeSelectionItem.
 * Survives configuration changes (rotation, etc.)
 *
 * Usage:
 * ```kotlin
 * var dateTime by rememberSaveable(stateSaver = dateTimeSelectionSaver()) {
 *     mutableStateOf<DateTimeSelectionItem?>(null)
 * }
 * ```
 */
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
