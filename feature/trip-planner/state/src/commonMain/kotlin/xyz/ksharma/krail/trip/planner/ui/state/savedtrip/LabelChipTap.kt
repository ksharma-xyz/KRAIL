package xyz.ksharma.krail.trip.planner.ui.state.savedtrip

import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * Where a tap on a Home or Work chip has got to. At most one is in flight, and it is held in the
 * ViewModel so a rotation in the middle of it loses nothing.
 */
sealed interface LabelChipTap {

    data object Idle : LabelChipTap

    /** A set chip was tapped and the screen is taking the one location fix the tap asked for. */
    data class Locating(val labelKey: String) : LabelChipTap

    /** Decided. The screen acts on [outcome] once, then reports it handled. */
    data class Decided(val outcome: LabelTapOutcome) : LabelChipTap
}

/** What a Home or Work chip tap does. */
sealed interface LabelTapOutcome {

    /** Plan a trip from the stop nearest the rider to the labelled stop, with no saved trip. */
    data class Plan(val from: StopItem, val to: StopItem) : LabelTapOutcome

    /** The existing search flow, with the labelled stop as the destination and From left empty. */
    data class Search(val to: StopItem) : LabelTapOutcome

    /** The label has no stop yet, so the stop search opens for the rider to choose one. */
    data class Assign(val labelKey: String) : LabelTapOutcome
}
