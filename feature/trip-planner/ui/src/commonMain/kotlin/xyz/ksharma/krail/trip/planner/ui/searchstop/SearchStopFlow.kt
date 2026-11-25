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
 *
 * [editing] is null once both bars hold a stop: nothing is being edited, there is no keyboard (so the
 * state is always the map state) and both bars are the light button with the go circle showing.
 */
internal data class SearchStopFlow(
    val mode: SearchStopMode = SearchStopMode.Map,
    val editing: ToFromField? = ToFromField.To,
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

    /**
     * System back, the iOS swipe or the arrow in the bar. [filled] is the bars holding a stop, so back
     * from the list state lands on a bar that still needs one. [mapsAvailable] false is a screen with no
     * map, where back must not take a step that would draw the same list again.
     */
    data class BackPressed(
        val filled: Set<ToFromField> = emptySet(),
        val mapsAvailable: Boolean = true,
    ) : SearchStopFlowEvent

    /**
     * The go circle left with a trip. The trip results open on top of this screen and back from them
     * returns to it, so it is saved as the map state with nothing edited and no keyboard, both bars set.
     */
    data object TripSent : SearchStopFlowEvent
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

        SearchStopFlowEvent.TripSent -> SearchStopStep(flow.copy(mode = SearchStopMode.Map, editing = null))

        is SearchStopFlowEvent.BackPressed -> when (flow.mode) {
            SearchStopMode.List -> backFromList(flow, event)
            SearchStopMode.Map -> SearchStopStep(flow, exit = true)
        }
    }

/**
 * Back from a start field switched to in place lands on the field the screen was opened on, or on the
 * other bar when that one already holds a stop, or on nothing when both do.
 *
 * With no map, that landing draws as the list whenever it still edits a bar (see [effectiveFlow]), which
 * is the screen the rider is already on, so back leaves instead. This is reached because the list takes
 * focus by itself and focus is recorded as [SearchStopFlowEvent.BarTapped].
 */
private fun backFromList(flow: SearchStopFlow, event: SearchStopFlowEvent.BackPressed): SearchStopStep {
    val landed = flow.copy(
        mode = SearchStopMode.Map,
        editing = barToEditAfterBack(flow.openedOn, event.filled),
    )
    val drawsAsListAgain = effectiveFlow(landed, event.mapsAvailable).mode == SearchStopMode.List
    return if (drawsAsListAgain) SearchStopStep(flow, exit = true) else SearchStopStep(landed)
}

/** The bar back from the list state lands on: [openedOn] if it is empty, else the other if empty, else none. */
internal fun barToEditAfterBack(openedOn: ToFromField, filled: Set<ToFromField>): ToFromField? =
    listOf(openedOn, openedOn.other()).firstOrNull { it !in filled }

/**
 * The flow a visit starts in. It always opens as the map state; the field it edits follows the
 * route, and a label or one leg of a trip is a single stop, so it edits the one bar (To). A To and
 * From form opened with a stop already held ([preset], from a pin card, a label chip or Ask KRAIL)
 * edits the bar that is still empty, or nothing when both are held.
 */
internal fun initialSearchStopFlow(
    fieldType: SearchStopFieldType,
    editTripLeg: Boolean,
    preset: SearchStopTrip = SearchStopTrip(),
): SearchStopFlow {
    val field = if (fieldType == SearchStopFieldType.FROM && !editTripLeg) ToFromField.From else ToFromField.To
    val shape = formShapeFor(fieldType, editTripLeg)
    val editing = if (shape == FormShape.ToAndFrom) barToEditAfterBack(field, preset.filled()) else field
    return SearchStopFlow(mode = SearchStopMode.Map, editing = editing, openedOn = editing ?: field)
}

/**
 * The flow the screen draws. With no map to open on, [mapsAvailable] false, the list state is the
 * only state there is, so the list shows and the keyboard comes up as it did before the map state
 * existed. This leaves the saved flow alone, and back leaves in one step whether or not the list's
 * focus has since moved the saved flow to the list (see [reduceSearchStopFlow]). With nothing being
 * edited there is no list to show, so the state stays as it is.
 */
internal fun effectiveFlow(flow: SearchStopFlow, mapsAvailable: Boolean): SearchStopFlow =
    if (mapsAvailable || flow.editing == null) flow else flow.copy(mode = SearchStopMode.List)

private const val FLOW_SEPARATOR = "|"
private const val NOTHING_EDITED = "None"

/**
 * Saves the flow across rotation, theme and font changes as one string of three names, so nothing
 * is held that a recreated Activity cannot read back. A string that does not read back as three
 * known names restores the opening flow. Nothing being edited is saved as its own name.
 */
internal val SearchStopFlowSaver: Saver<SearchStopFlow, String> = Saver(
    save = {
        listOf(it.mode.name, it.editing?.name ?: NOTHING_EDITED, it.openedOn.name).joinToString(FLOW_SEPARATOR)
    },
    restore = { saved ->
        val names = saved.split(FLOW_SEPARATOR)
        SearchStopFlow(
            mode = SearchStopMode.entries.firstOrNull { it.name == names.getOrNull(0) } ?: SearchStopMode.Map,
            editing = if (names.getOrNull(1) == NOTHING_EDITED) {
                null
            } else {
                ToFromField.entries.firstOrNull { it.name == names.getOrNull(1) } ?: ToFromField.To
            },
            openedOn = ToFromField.entries.firstOrNull { it.name == names.getOrNull(2) } ?: ToFromField.To,
        )
    },
)
