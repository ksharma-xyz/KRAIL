package xyz.ksharma.krail.trip.planner.ui.state.settings

data class SettingsState(
    val appVersion: String = "",
    /**
     * `true` when the running build is a debug build. Drives whether the
     * "Debug Config" tile is shown in the Settings screen. Release builds
     * never see the tile.
     */
    val isDebug: Boolean = false,
    val isProActive: Boolean = false,
)
