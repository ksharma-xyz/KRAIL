package xyz.ksharma.krail.trip.planner.ui.savedtrips

import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.sandook.SandookPreferences.Companion.KEY_DISMISSED_INFO_TILES

fun SandookPreferences.isInfoTileDismissed(key: String): Boolean {
    val dismissed = getString(KEY_DISMISSED_INFO_TILES)
        ?.split(",")
        ?.filter { it.isNotBlank() }
        ?.toSet() ?: emptySet()
    return key in dismissed
}

fun SandookPreferences.markInfoTileAsDismissed(key: String) {
    val dismissed = getString(KEY_DISMISSED_INFO_TILES)
        ?.split(",")
        ?.filter { it.isNotBlank() }
        ?.toMutableSet() ?: mutableSetOf()
    dismissed.add(key)
    setString(KEY_DISMISSED_INFO_TILES, dismissed.joinToString(","))
}
