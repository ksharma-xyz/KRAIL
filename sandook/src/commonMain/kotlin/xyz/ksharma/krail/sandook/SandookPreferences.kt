package xyz.ksharma.krail.sandook

interface SandookPreferences {

    // Integer preferences
    fun getLong(key: String): Long?
    fun setLong(key: String, value: Long)

    // String preferences
    fun getString(key: String): String?
    fun setString(key: String, value: String)

    // Boolean preferences
    fun getBoolean(key: String): Boolean?
    fun setBoolean(key: String, value: Boolean)

    // Double preferences
    fun getDouble(key: String): Double?
    fun setDouble(key: String, value: Double)

    // Delete preference
    fun deletePreference(key: String)

    companion object {
        // Increment this when bundling new stops data
        const val NSW_STOPS_VERSION = 91L

        // Increment this when bundling new bus routes data
        const val NSW_BUS_ROUTES_VERSION = 64L

        const val KEY_NSW_STOPS_VERSION = "KEY_NSW_STOPS_VERSION"
        const val KEY_HAS_SEEN_INTRO = "KEY_HAS_SEEN_INTRO"

        /**
         * Set once the Intro has asked, or the rider has passed, the Park & Ride question, so home
         * never asks it a second time. Stored like [KEY_HAS_SEEN_INTRO].
         */
        const val KEY_PARK_RIDE_SUGGESTION_SHOWN = "KEY_PARK_RIDE_SUGGESTION_SHOWN"
        const val KEY_DISCOVER_CLICKED_BEFORE = "KEY_DISCOVER_CLICKED_BEFORE"
        const val KEY_DISMISSED_INFO_TILES = "KEY_DISMISSED_INFO_TILES"
        const val KEY_THEME_MODE = "KEY_THEME_MODE"
        const val KEY_HAS_SEEN_INVITE_FRIENDS_TILE = "KEY_HAS_SEEN_INVITE_FRIENDS_TILE"

        const val KEY_NSW_BUS_ROUTES_VERSION = "KEY_NSW_BUS_ROUTES_VERSION"

        const val KEY_LOCATION_PERMISSION_EVER_REQUESTED = "KEY_LOCATION_PERMISSION_EVER_REQUESTED"

        const val KEY_HAS_SEEN_SAVED_TRIP_CARD_REORDER_TIP = "KEY_HAS_SEEN_SAVED_TRIP_CARD_REORDER_TIP"

        /**
         * How many saved trips come before the Park & Ride row on home, once the rider has dragged
         * it there. Absent until then, which puts the row after the second trip.
         */
        const val KEY_PARK_RIDE_ROW_INDEX = "KEY_PARK_RIDE_ROW_INDEX"

        /**
         * Prefix for per-trip "Save this trip?" prompt dismissal counters.
         * Full key is this prefix + tripId; value is the number of times the
         * user dismissed the prompt for that origin-destination pair.
         */
        const val KEY_SAVE_TRIP_PROMPT_DISMISSALS_PREFIX = "KEY_SAVE_TRIP_PROMPT_DISMISSALS_"
    }
}
