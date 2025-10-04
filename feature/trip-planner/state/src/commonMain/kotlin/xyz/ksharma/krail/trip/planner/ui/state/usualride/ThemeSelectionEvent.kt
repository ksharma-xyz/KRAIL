package xyz.ksharma.krail.trip.planner.ui.state.usualride

sealed interface ThemeSelectionEvent {
    data class ThemeSelected(val themeId: Int) : ThemeSelectionEvent
    data class ThemeModeSelected(val themeMode: Long) : ThemeSelectionEvent
}
