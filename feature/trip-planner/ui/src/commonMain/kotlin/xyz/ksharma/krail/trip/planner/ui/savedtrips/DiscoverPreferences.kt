package xyz.ksharma.krail.trip.planner.ui.savedtrips

import xyz.ksharma.krail.sandook.SandookPreferences

fun SandookPreferences.hasDiscoverBeenClicked(): Boolean {
    return getBoolean(SandookPreferences.KEY_DISCOVER_CLICKED_BEFORE) ?: false
}

fun SandookPreferences.markDiscoverAsClicked() {
    setBoolean(SandookPreferences.KEY_DISCOVER_CLICKED_BEFORE, true)
}
