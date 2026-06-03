package xyz.ksharma.krail.trip.planner.ui.timetable

/**
 * Title + message for the empty timetable state.
 *
 * When [emptyDueToModeFilter] is true the API returned trips but the user's mode selection hid
 * them all, so we point them at the modes button instead of saying no route exists.
 *
 * Lives in its own file so the choice does not add to [TimeTableScreen]'s cyclomatic complexity
 * or file function count, both of which already sit at their detekt limit.
 */
internal fun timeTableEmptyResultMessage(emptyDueToModeFilter: Boolean): Pair<String, String> =
    if (emptyDueToModeFilter) {
        "Nothing right now" to
            "No trips running for the transport modes you picked. Tap the modes button to show more."
    } else {
        "No route found!" to "Search for another stop or check back later."
    }
