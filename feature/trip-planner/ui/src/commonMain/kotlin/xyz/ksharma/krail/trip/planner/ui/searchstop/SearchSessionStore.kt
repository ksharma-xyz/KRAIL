package xyz.ksharma.krail.trip.planner.ui.searchstop

/**
 * Carries a `searchSessionId` from the stop selection that produced it to the
 * `load_timetable_click` that follows, so the search funnel can be measured all the way
 * to "the rider actually saw departure times" rather than stopping at "a stop was picked".
 *
 * The two events fire from different ViewModels on different screens (SearchStop and
 * SavedTrips) and the search ViewModel is gone by the time the trip is loaded, so the id
 * has to live somewhere both can reach. This is that somewhere: a process-scoped single,
 * deliberately tiny, holding at most one pending selection.
 *
 * Attribution rules, both chosen so a session id is never attached to a trip the search
 * did not actually lead to:
 *  - **Consume-once.** The id is cleared when read. Loading the same trip a second time
 *    is a repeat view, not a second search converting.
 *  - **Stop must match.** The id is only returned when the loaded trip still contains the
 *    stop that was selected in that session. Searching and then tapping an unrelated
 *    saved trip card attributes nothing.
 *
 * Selections with no live query (recents, map picks) record a null id, which also clears
 * any earlier pending one - those selections are genuinely unattributable and must not
 * inherit the previous search's id.
 */
interface SearchSessionStore {

    /**
     * Records the session that produced a stop selection.
     *
     * @param searchSessionId id of the settled query behind the selection, or null when
     *                        the selection came from recents, an empty-state stop or the
     *                        map.
     * @param stopId          stop the rider selected; used to verify the trip that gets
     *                        loaded is the one this search led to.
     */
    fun recordSelection(searchSessionId: String?, stopId: String)

    /**
     * Returns the pending session id when either endpoint of the loaded trip is the stop
     * it was recorded against, else null. Always clears the pending selection.
     */
    fun consumeFor(fromStopId: String, toStopId: String): String?
}

internal class RealSearchSessionStore : SearchSessionStore {

    private var pending: Selection? = null

    override fun recordSelection(searchSessionId: String?, stopId: String) {
        pending = searchSessionId?.let { Selection(searchSessionId = it, stopId = stopId) }
    }

    override fun consumeFor(fromStopId: String, toStopId: String): String? {
        val selection = pending
        pending = null
        return selection?.searchSessionId
            ?.takeIf { selection.stopId == fromStopId || selection.stopId == toStopId }
    }

    private data class Selection(val searchSessionId: String, val stopId: String)
}
