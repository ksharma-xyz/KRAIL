package xyz.ksharma.krail.feature.debug.settings.ui

/**
 * Stable selectors for end-to-end drivers (Maestro) on the debug config screen.
 *
 * Same contract as `TripPlannerTestTags`: semantics-only, never derived from visible copy, and
 * public API once a flow in `.maestro/` references one.
 */
object DebugSettingsTestTags {

    const val DEBUG_CONFIG_SCREEN = "debugconfig.screen"
    const val DEBUG_CONFIG_LIST = "debugconfig.list"
    const val DEBUG_CONFIG_TILE_NETWORK = "debugconfig.tile.network"
    const val DEBUG_CONFIG_TILE_RESET_REVIEW = "debugconfig.tile.resetreview"

    /**
     * One feature-flag toggle row. [flagId] is the flag's own identifier, so the tag stays
     * correct when the row's label is reworded.
     */
    fun toggleTile(flagId: String): String = "debugconfig.toggle.$flagId"
}
