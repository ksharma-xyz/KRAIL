package xyz.ksharma.krail.core.analytics

/**
 * Which pane the rider was last interacting with.
 *
 * [SINGLE] is the honest answer on a phone-shaped window: there is one pane, so the
 * question does not arise. It is also what a dual-pane layout reports before the rider has
 * touched either side.
 */
enum class AnalyticsPane {
    SINGLE,
    LIST,
    DETAIL,
}

/**
 * Tracks which pane the rider last touched, so every event can say where it came from.
 *
 * Attribution is by **last touch**, not by which screen sits on top. In a dual-pane
 * layout both panes are on screen at once, so "the topmost entry" would credit the detail
 * pane for taps that actually happened in the list - the kind of plausible-but-wrong
 * attribution that is worse than no attribution at all. A pointer landing in a pane is
 * unambiguous.
 *
 * Set from `DualPaneScaffold`, which is the single place every dual-pane screen builds its
 * two sides, and read by [RealAnalytics] when an event is sent. Events with no rider
 * interaction behind them (polling, screen views) inherit the last touched pane, which is
 * the right answer for "where was the rider working" and the only one available.
 */
object AnalyticsPaneTracker {

    const val PROP_PANE = "pane"

    private var current: AnalyticsPane = AnalyticsPane.SINGLE

    fun set(pane: AnalyticsPane) {
        current = pane
    }

    fun currentPane(): AnalyticsPane = current

    /**
     * Adds the pane to an event's parameters. Central so no call site has to thread pane
     * context through a ViewModel, and so events added later are attributed for free.
     */
    fun decorate(properties: Map<String, Any>?): Map<String, Any> =
        properties.orEmpty() + (PROP_PANE to current.name)
}
