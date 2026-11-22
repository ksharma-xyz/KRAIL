package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.runtime.saveable.Saver
import xyz.ksharma.krail.trip.planner.ui.components.ToFromField
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType

/**
 * The two states Search Stop has, rule 39 of `docs/MAP_HOME_UX.md`.
 *
 * - [Map] is how it opens: the map and one dark pill, no list and no keyboard.
 * - [List] is what tapping the bar raises: the keyboard and the recents (or the results).
 */
internal enum class SearchStopMode { Map, List }

/**
 * Where the rider is on Search Stop: which of the two states, which field the bar edits, and the
 * field the screen was opened on. The last is why back from a start field the rider switched to in
 * place returns to the field they came for, with the destination kept (rule 35).
 */
internal data class SearchStopFlow(
    val mode: SearchStopMode = SearchStopMode.Map,
    val editing: ToFromField = ToFromField.To,
    val openedOn: ToFromField = ToFromField.To,
)

/** What the rider did that moves between the two states. */
internal sealed interface SearchStopFlowEvent {
    /** The bar was tapped, so the keyboard is up. */
    data object BarTapped : SearchStopFlowEvent

    /** The other bar of the To and From block was tapped, so editing moves to it in place. */
    data class FieldTapped(val field: ToFromField) : SearchStopFlowEvent

    /** The map was tapped. */
    data object MapTapped : SearchStopFlowEvent

    /** System back, the iOS swipe or the arrow in the bar. */
    data object BackPressed : SearchStopFlowEvent
}

/** The next [flow], and whether the screen leaves instead (back from the map state goes home). */
internal data class SearchStopStep(val flow: SearchStopFlow, val exit: Boolean = false)

/**
 * The whole state machine, with no UI in it so one table pins every move.
 *
 * Back is a two step exit: the list state goes to the map state, and the map state goes home. A
 * tap on the map is the first step on its own. The text is the screen's own and none of these
 * touch it, so X (which only clears text) is not an event here and the recents stay put.
 */
internal fun reduceSearchStopFlow(flow: SearchStopFlow, event: SearchStopFlowEvent): SearchStopStep =
    when (event) {
        SearchStopFlowEvent.BarTapped -> SearchStopStep(flow.copy(mode = SearchStopMode.List))

        is SearchStopFlowEvent.FieldTapped ->
            SearchStopStep(flow.copy(mode = SearchStopMode.List, editing = event.field))

        SearchStopFlowEvent.MapTapped -> SearchStopStep(flow.copy(mode = SearchStopMode.Map))

        SearchStopFlowEvent.BackPressed -> when (flow.mode) {
            // Back from a start field switched to in place lands on the field the screen was opened on.
            SearchStopMode.List -> SearchStopStep(
                flow.copy(mode = SearchStopMode.Map, editing = flow.openedOn),
            )

            SearchStopMode.Map -> SearchStopStep(flow, exit = true)
        }
    }

/**
 * The flow a visit starts in. It always opens as the map state; the field it edits follows the
 * route, and a label or one leg of a trip is a single stop, so it edits the one bar (To).
 */
internal fun initialSearchStopFlow(fieldType: SearchStopFieldType, editTripLeg: Boolean): SearchStopFlow {
    val field = if (fieldType == SearchStopFieldType.FROM && !editTripLeg) ToFromField.From else ToFromField.To
    return SearchStopFlow(mode = SearchStopMode.Map, editing = field, openedOn = field)
}

/**
 * The flow the screen draws. With no map to open on, [mapsAvailable] false, the list state is the
 * only state there is, so the list shows and the keyboard comes up as it did before the map state
 * existed. The saved flow stays the map state, so back still leaves in one step.
 */
internal fun effectiveFlow(flow: SearchStopFlow, mapsAvailable: Boolean): SearchStopFlow =
    if (mapsAvailable) flow else flow.copy(mode = SearchStopMode.List)

private const val FLOW_SEPARATOR = "|"

/**
 * Saves the flow across rotation, theme and font changes as one string of three names, so nothing
 * is held that a recreated Activity cannot read back. A string that does not read back as three
 * known names restores the opening flow.
 */
internal val SearchStopFlowSaver: Saver<SearchStopFlow, String> = Saver(
    save = { listOf(it.mode.name, it.editing.name, it.openedOn.name).joinToString(FLOW_SEPARATOR) },
    restore = { saved ->
        val names = saved.split(FLOW_SEPARATOR)
        SearchStopFlow(
            mode = SearchStopMode.entries.firstOrNull { it.name == names.getOrNull(0) } ?: SearchStopMode.Map,
            editing = ToFromField.entries.firstOrNull { it.name == names.getOrNull(1) } ?: ToFromField.To,
            openedOn = ToFromField.entries.firstOrNull { it.name == names.getOrNull(2) } ?: ToFromField.To,
        )
    },
)
