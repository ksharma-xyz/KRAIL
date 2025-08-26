package xyz.ksharma.krail.trip.planner.ui.savedtrips

import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.sandook.SandookPreferences.Companion.KEY_DISMISSED_INFO_TILES

/**
 * Checks if the info tile with the given [key] has been dismissed by the user.
 *
 * This function reads the stored comma-separated list of dismissed info tile keys from preferences,
 * splits it into a set, and checks if the given [key] is present.
 *
 * @param key The unique identifier for the info tile.
 * @return `true` if the info tile has been dismissed, `false` otherwise.
 */
fun SandookPreferences.isInfoTileDismissed(key: String): Boolean {
    val dismissed = getString(KEY_DISMISSED_INFO_TILES)
        ?.split(",")
        ?.filter { it.isNotBlank() }
        ?.toSet() ?: emptySet()
    return key in dismissed
}

/**
 * Marks the info tile with the given [key] as dismissed by the user.
 *
 * This function retrieves the current list of dismissed info tile keys, adds the new [key] to the set,
 * and saves the updated list back to preferences as a comma-separated string.
 *
 * @param key The unique identifier for the info tile.
 */
fun SandookPreferences.markInfoTileAsDismissed(key: String) {
    val dismissed = getString(KEY_DISMISSED_INFO_TILES)
        ?.split(",")
        ?.filter { it.isNotBlank() }
        ?.toMutableSet() ?: mutableSetOf()
    dismissed.add(key)
    setString(KEY_DISMISSED_INFO_TILES, dismissed.joinToString(","))
}
