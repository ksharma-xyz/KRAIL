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
    const val INTRO_SECONDARY_ACTION = "intro.action.secondary"

    // The four theme badges on the first Intro screen. One tag for each, because the flow has to tap a
    // specific one before the primary button comes on.
    const val INTRO_THEME_TRAIN = "intro.theme.train"
    const val INTRO_THEME_METRO = "intro.theme.metro"
    const val INTRO_THEME_BUS = "intro.theme.bus"
    const val INTRO_THEME_FERRY = "intro.theme.ferry"

    // The destination and origin screens: the search field, and one row for each stop listed.
    const val INTRO_STOP_FIELD = "intro.stop.field"

    // The confirm screen's trip tile.
    const val INTRO_CONFIRM_TILE = "intro.confirm.tile"

    // The finale layer over the app after the Intro. It is there for a few seconds and then gone.
    const val INTRO_FINALE = "intro.finale"

    // Saved trips (home)
    const val SAVED_TRIPS_SCREEN = "savedtrips.screen"
    const val SAVED_TRIPS_LIST = "savedtrips.list"
    const val SAVED_TRIPS_ACTION_SETTINGS = "savedtrips.action.settings"
    const val SAVED_TRIPS_ACTION_DISCOVER = "savedtrips.action.discover"
    const val SAVED_TRIPS_ACTION_DONE = "savedtrips.action.done"

    // The Home, Work and Park & Ride chips above the search row. The Park & Ride chip is a
    // constant, the Home and Work chips are per label, see [homeLabelChip].
    const val SAVED_TRIPS_CHIP_ROW = "savedtrips.chips"
    const val SAVED_TRIPS_CHIP_PARK_RIDE = "savedtrips.chip.parkride"

    // The Park & Ride row of tiles on home. The tiles and their car parks are per station and per
    // car park, see [parkRideTile], [parkRideCarPark] and [parkRideDirections].
    const val SAVED_TRIPS_PARK_RIDE_ROW = "savedtrips.parkride.row"

    // Search row (the bottom origin/destination bar on the home screen)
    const val SEARCH_ROW = "searchrow.root"
    const val SEARCH_ROW_FROM = "searchrow.from"
    const val SEARCH_ROW_TO = "searchrow.to"
    const val SEARCH_ROW_SEARCH = "searchrow.search"
    const val SEARCH_ROW_ASK_KRAIL = "searchrow.askkrail"
    const val SEARCH_ROW_REVERSE = "searchrow.reverse"
    const val SEARCH_ROW_COLLAPSED_PILL = "searchrow.collapsedpill"

    // The go circle itself (40 dp), which wraps the button that carries [SEARCH_ROW_SEARCH], and the
    // origin dot in the From bar's leading column.
    const val SEARCH_ROW_GO_CIRCLE = "searchrow.gocircle"
    const val SEARCH_ROW_ORIGIN_DOT = "searchrow.origindot"

    // Search stop
    const val SEARCH_STOP_QUERY_FIELD = "searchstop.query"
    const val SEARCH_STOP_BACK = "searchstop.back"
    const val SEARCH_STOP_RESULTS_LIST = "searchstop.results"
    const val SEARCH_STOP_RECENTS_LIST = "searchstop.recents"

    /** The X in the search bar. It clears the text and nothing else. */
    const val SEARCH_STOP_CLEAR = "searchstop.clear"

    /** The one status pill under the form while location permission is not granted. */
    const val SEARCH_STOP_LOCATION_PILL = "searchstop.locationpill"

    /** The row of labels at the top of the recents card. It scrolls sideways. */
    const val SEARCH_STOP_LABEL_ROW = "searchstop.labelrow"

    /** The mode filter row under the pill on Search Stop's map state. */
    const val SEARCH_STOP_MODE_CHIPS = "searchstop.modechips"

    /** The fade at the right edge of that row, drawn only while it has more to scroll to. */
    const val SEARCH_STOP_MODE_CHIPS_FADE = "searchstop.modechips.fade"

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
    const val SETTINGS_ITEM_RATE = "settings.item.rate"
    const val SETTINGS_ITEM_DEBUG_CONFIG = "settings.item.debugconfig"

    /** One saved trip row. Suffixed so a flow can target a known trip or glob the set. */
    fun savedTripCard(tripId: String): String = "savedtrips.card.$tripId"

    /** One closed Park & Ride tile, by its station's id (the smallest stop id of the group). */
    fun parkRideTile(stationId: String): String = "savedtrips.parkride.tile.$stationId"

    /** The one open Park & Ride tile, and its station name, which closes it. */
    fun parkRideTileOpen(stationId: String): String = "savedtrips.parkride.open.$stationId"
    fun parkRideTileName(stationId: String): String = "savedtrips.parkride.name.$stationId"

    /** One car park block of an open tile, and its Directions button when it has a position. */
    fun parkRideCarPark(facilityId: String): String = "savedtrips.parkride.carpark.$facilityId"
    fun parkRideDirections(facilityId: String): String = "savedtrips.parkride.directions.$facilityId"

    /** One Home or Work chip, keyed by the label's name so it is the same whether the label is set or not. */
    fun homeLabelChip(labelKey: String): String = "savedtrips.chip.label.$labelKey"

    /** One chip of that row, by the mode's product class. */
    fun searchStopModeChip(productClass: Int) = "searchstop.modechip.$productClass"

    /** One search result row, keyed by the stop it would select. */
    fun searchStopResult(stopId: String): String = "searchstop.result.$stopId"

    /** One row of the Intro's destination or origin list, keyed by the stop it would choose. */
    fun introStop(stopId: String): String = "intro.stop.$stopId"

    /** One journey result card. */
    fun timeTableJourneyCard(journeyId: String): String = "timetable.journey.$journeyId"
}
