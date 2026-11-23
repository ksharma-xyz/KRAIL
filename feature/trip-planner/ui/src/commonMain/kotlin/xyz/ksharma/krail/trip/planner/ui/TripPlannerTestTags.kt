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

    // The destination and origin screens: the search field, the first row of the list (a tag that does not depend on
    // which stop it is, for a flow that only needs to select something), and one row for each stop listed.
    const val INTRO_STOP_FIELD = "intro.stop.field"
    const val INTRO_STOP_FIRST = "intro.stop.first"

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

    // The classic home's Set Home and Set Work pills, the first row of the list. Each pill carries [homeLabelChip].
    const val SAVED_TRIPS_LABEL_PILLS = "savedtrips.pills"

    // The theme coloured sheet at the bottom of the classic home, the one that holds the search bar.
    const val SAVED_TRIPS_BOTTOM_SHEET = "savedtrips.sheet"

    // The Park & Ride row of tiles on home. The tiles and their car parks are per station and per
    // car park, see [parkRideTile], [parkRideCarPark] and [parkRideDirections].
    const val SAVED_TRIPS_PARK_RIDE_ROW = "savedtrips.parkride.row"

    // The map home (docs/MAP_HOME_UX.md rules 1, 12, 20 and 24): the row of notices under the top row, the
    // Map offline chip in it, the top row and the bottom block. The search pill keeps the search row's own
    // tags, so a flow written for it still reads.
    const val HOME_MAP_OFFLINE_CHIP = "homemap.offline"
    const val HOME_MAP_TOP_ROW = "homemap.top"
    const val HOME_MAP_LOGO = "homemap.logo"
    const val HOME_MAP_BOTTOM = "homemap.bottom"

    // Reorder mode (docs/MAP_HOME_UX.md rule 31): the top row that holds Done, and the Park & Ride
    // row's outlined block. The grips and delete circles are per card, see [Reorder].
    const val SAVED_TRIPS_REORDER_TOP_ROW = "savedtrips.reorder.top"
    const val SAVED_TRIPS_PARK_RIDE_BLOCK = "savedtrips.reorder.parkride.block"

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
    const val SEARCH_ROW_DESTINATION_PIN = "searchrow.destinationpin"

    // The Search Stop card's label icon, drawn in place of the dot or pin, and the bare swap glyph. The swap's 48 dp target
    // carries [SEARCH_ROW_REVERSE].
    const val SEARCH_ROW_LABEL_ICON = "searchrow.labelicon"
    const val SEARCH_ROW_SWAP_GLYPH = "searchrow.swapglyph"

    // The 1 dp divider between the card's two rows.
    const val SEARCH_ROW_DIVIDER = "searchrow.divider"

    // Search stop
    const val SEARCH_STOP_QUERY_FIELD = "searchstop.query"
    const val SEARCH_STOP_BACK = "searchstop.back"
    const val SEARCH_STOP_RESULTS_LIST = "searchstop.results"
    const val SEARCH_STOP_RECENTS_LIST = "searchstop.recents"

    /** The X in the search bar. It clears the text and nothing else. */
    const val SEARCH_STOP_CLEAR = "searchstop.clear"

    /** The bare cross inside the X in the search bar. There is no circle round it. */
    const val SEARCH_STOP_CLEAR_GLYPH = "searchstop.clear.glyph"

    /** The one rounded box at the top of Search Stop, holding the To and From pills. */
    const val SEARCH_STOP_GROUP_CARD = "searchstop.groupcard"

    /** The one status pill under the form while location permission is not granted. */
    const val SEARCH_STOP_LOCATION_PILL = "searchstop.locationpill"

    /** The pin at the start of the location notice's title. */
    const val SEARCH_STOP_LOCATION_PIN = "searchstop.locationpin"

    /** The row of labels at the top of the list. It scrolls sideways. */
    const val SEARCH_STOP_LABEL_ROW = "searchstop.labelrow"

    /** The cloud gradient that covers the map in Search Stop's list state. */
    const val SEARCH_STOP_LIST_BACKGROUND = "searchstop.listbackground"

    /** The Select on map row of the list state, which returns to the map state. */
    const val SEARCH_STOP_SELECT_ON_MAP = "searchstop.selectonmap"

    /** The mode filter block at the bottom of Search Stop's map state, drawn only while it is open. */
    const val SEARCH_STOP_MODE_CHIPS = "searchstop.modechips"

    /** The Options button on the bottom row of the map state that opens and shuts that block. */
    const val SEARCH_STOP_MODE_BUTTON = "searchstop.modebutton"
    const val SEARCH_STOP_MODE_PILL = "searchstop.modepill"

    // The map's location control at the bottom end of the map state.
    const val SEARCH_STOP_LOCATION_BUTTON = "searchstop.locationbutton"

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

    /** Reorder mode's per card tags, kept in their own object so this one stays under its limit. */
    object Reorder {
        /** The grip on a card, by the card's key: a trip's id or the Park & Ride row's key. */
        fun grip(key: String): String = "savedtrips.reorder.grip.$key"

        /** The delete circle on a trip card. */
        fun delete(tripId: String): String = "savedtrips.reorder.delete.$tripId"
    }

    /** The floating stop card's tags, kept in their own object so this one stays under its limit. */
    object StopCard {
        const val CARD = "stopcard.card"
        const val HEADER = "stopcard.header"
        const val TITLE = "stopcard.title"
        const val BODY = "stopcard.body"
        const val FOOTER = "stopcard.footer"
        const val CLOSE = "stopcard.close"
        const val CLOSE_CIRCLE = "stopcard.close.circle"
        const val MODES = "stopcard.modes"
        const val BOARD_HEADING = "stopcard.board.heading"
        const val CHEVRON = "stopcard.board.chevron"
        const val BOARD_BODY = "stopcard.board.body"
        const val ACTIONS = "stopcard.actions"
        const val PARK_RIDE_TOTAL = "stopcard.parkride.total"
        const val SAVE_AS = "stopcard.saveas"
        const val PICK = "stopcard.pick"
        const val HOME_ACTION = "stopcard.homeaction"

        /** One Save as pill, by the label's name, the same whether the stop carries it or not. */
        fun saveAsPill(labelKey: String): String = "stopcard.saveas.$labelKey"
    }

    /** The one-time Park & Ride suggestion card on the map home, kept in its own object so this one stays under its limit. */
    object ParkRideSuggestion {
        const val CARD = "parkridesuggestion.card"
        const val FIGURES = "parkridesuggestion.figures"
        const val ADD_TO_HOME = "parkridesuggestion.add"
        const val NOT_NOW = "parkridesuggestion.notnow"
    }

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
