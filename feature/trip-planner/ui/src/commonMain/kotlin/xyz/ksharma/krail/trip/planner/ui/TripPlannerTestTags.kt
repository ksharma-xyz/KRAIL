package xyz.ksharma.krail.trip.planner.ui

/**
 * Stable selectors for end-to-end drivers (Maestro).
 *
 * Every tag here is applied with `Modifier.testTag`, which is semantics-only: it never
 * participates in layout or drawing, so tagging a node cannot move or repaint a pixel.
 *
 * Two rules keep these useful:
 *
 * 1. **Never derive a tag from visible copy.** A flow that selects on "Saved Trips" breaks the
 *    day the wording changes. These names describe the node's job, not its label.
 * 2. **Treat a tag as public API once a flow references it.** Renaming one is a breaking change
 *    to `.maestro/`; grep there before editing.
 *
 * On Android the tags surface as accessibility `resource-id`s because the app root opts in via
 * `exposeTestTagsToUiAutomation()`. On iOS, Compose Multiplatform publishes them as
 * `accessibilityIdentifier` with no opt-in needed, so a single tag serves both platforms.
 *
 * Per-item tags (one row of a list) are built by the functions at the bottom of this file.
 */
object TripPlannerTestTags {

    // First-run intro carousel. Every fresh install lands here, so an E2E run on a clean
    // device has to get past it before it can test anything else. One button advances the
    // carousel and finishes it, hence a single action tag rather than one per page.
    const val INTRO_SCREEN = "intro.screen"
    const val INTRO_PRIMARY_ACTION = "intro.action.primary"

    // Saved trips (home)
    const val SAVED_TRIPS_SCREEN = "savedtrips.screen"
    const val SAVED_TRIPS_LIST = "savedtrips.list"
    const val SAVED_TRIPS_ACTION_SETTINGS = "savedtrips.action.settings"
    const val SAVED_TRIPS_ACTION_DISCOVER = "savedtrips.action.discover"
    const val SAVED_TRIPS_ACTION_DONE = "savedtrips.action.done"

    // Search row (the bottom origin/destination bar on the home screen)
    const val SEARCH_ROW = "searchrow.root"
    const val SEARCH_ROW_FROM = "searchrow.from"
    const val SEARCH_ROW_TO = "searchrow.to"
    const val SEARCH_ROW_SEARCH = "searchrow.search"
    const val SEARCH_ROW_ASK_KRAIL = "searchrow.askkrail"
    const val SEARCH_ROW_COLLAPSED_PILL = "searchrow.collapsedpill"

    // Search stop
    const val SEARCH_STOP_QUERY_FIELD = "searchstop.query"
    const val SEARCH_STOP_BACK = "searchstop.back"
    const val SEARCH_STOP_RESULTS_LIST = "searchstop.results"
    const val SEARCH_STOP_RECENTS_LIST = "searchstop.recents"

    /**
     * The confirm button on the first-run map options sheet.
     *
     * Tagged so a flow can get past that sheet by name. It opens itself the first time a
     * rider ever reaches this screen and covers [SEARCH_STOP_QUERY_FIELD] while it is up,
     * which on a fresh device makes the search field unreachable rather than slow.
     */
    const val SEARCH_STOP_MAP_OPTIONS_DONE = "searchstop.mapoptions.done"

    // Timetable
    const val TIME_TABLE_SCREEN = "timetable.screen"
    const val TIME_TABLE_DATE_TIME_SELECTOR = "timetable.action.datetime"
    const val TIME_TABLE_MODE_FILTER = "timetable.action.modefilter"
    const val TIME_TABLE_REVERSE = "timetable.action.reverse"
    const val TIME_TABLE_SAVE_TRIP = "timetable.action.savetrip"

    /** The alerts button inside an expanded journey card. */
    const val JOURNEY_CARD_ALERTS = "journeycard.alerts"

    // Settings
    const val SETTINGS_SCREEN = "settings.screen"
    const val SETTINGS_ITEM_THEME = "settings.item.theme"
    const val SETTINGS_ITEM_INVITE = "settings.item.invite"
    const val SETTINGS_ITEM_HOW_TO = "settings.item.howto"
    const val SETTINGS_ITEM_OUR_STORY = "settings.item.ourstory"
    const val SETTINGS_ITEM_DEBUG_CONFIG = "settings.item.debugconfig"

    /** One saved trip row. Suffixed so a flow can target a known trip or glob the set. */
    fun savedTripCard(tripId: String): String = "savedtrips.card.$tripId"

    /** One search result row, keyed by the stop it would select. */
    fun searchStopResult(stopId: String): String = "searchstop.result.$stopId"

    /** One journey result card. */
    fun timeTableJourneyCard(journeyId: String): String = "timetable.journey.$journeyId"
}
