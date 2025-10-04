package xyz.ksharma.krail.trip.planner.ui.themeselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.core.analytics.event.trackScreenViewEvent
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.taj.theme.DEFAULT_THEME_STYLE
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.trip.planner.ui.state.usualride.ThemeSelectionEvent
import xyz.ksharma.krail.trip.planner.ui.state.usualride.ThemeSelectionState

class ThemeSelectionViewModel(
    private val sandook: Sandook,
    private val analytics: Analytics,
    private val ioDispatcher: CoroutineDispatcher,
    private val preferences: SandookPreferences,
) : ViewModel() {

    private val _uiState: MutableStateFlow<ThemeSelectionState> =
        MutableStateFlow(ThemeSelectionState())
    val uiState: StateFlow<ThemeSelectionState> = _uiState
        .onStart {
            loadThemeStyle()
            analytics.trackScreenViewEvent(screen = AnalyticsScreen.ThemeSelection)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(ANR_TIMEOUT), ThemeSelectionState())

    private var transportSelectionJob: Job? = null

    fun onEvent(event: ThemeSelectionEvent) {
        when (event) {
            is ThemeSelectionEvent.ThemeSelected -> onThemeSelected(event.themeId)
            is ThemeSelectionEvent.ThemeModeSelected -> onThemeModeSelected(event.themeMode)
        }
    }

    private fun onThemeModeSelected(themeMode: Long) {
        preferences.setLong(SandookPreferences.KEY_THEME_MODE, themeMode)
    }

    private fun onThemeSelected(productClass: Int) {
        transportSelectionJob?.cancel()
        transportSelectionJob = viewModelScope.launch(ioDispatcher) {
            sandook.clearTheme() // Only one entry should exist at a time
            sandook.insertOrReplaceTheme(productClass.toLong())
            updateUiState {
                copy(themeSelected = true)
            }
            analytics.trackThemeSelectionEvent(themeId = productClass)
        }
    }

    private fun loadThemeStyle() {
        viewModelScope.launch(ioDispatcher) {
            // First app launch there will be no product class, so use default transport mode theme.
            val themeId = sandook.getProductClass()?.toInt() ?: DEFAULT_THEME_STYLE.id
            val themeStyle: KrailThemeStyle =
                KrailThemeStyle.entries.find { it.id == themeId } ?: DEFAULT_THEME_STYLE
            updateUiState {
                copy(selectedThemeStyle = themeStyle)
            }
        }
    }

    private inline fun updateUiState(block: ThemeSelectionState.() -> ThemeSelectionState) {
        _uiState.update(block)
    }

    companion object {
        const val ANR_TIMEOUT = 5000L
    }
}
