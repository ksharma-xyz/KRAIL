package xyz.ksharma.krail.trip.planner.ui.settings.story

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.core.analytics.event.trackScreenViewEvent
import xyz.ksharma.krail.core.remote_config.flag.Flag
import xyz.ksharma.krail.core.remote_config.flag.FlagKeys
import xyz.ksharma.krail.core.remote_config.flag.asString
import xyz.ksharma.krail.trip.planner.ui.state.settings.story.OurStoryState

class OurStoryViewModel(
    private val analytics: Analytics,
    private val flag: Flag,
) : ViewModel() {

    private val storyText: String by lazy {
        flag.getFlagValue(FlagKeys.OUR_STORY_TEXT.key).asString()
    }

    private val disclaimerText: String by lazy {
        flag.getFlagValue(FlagKeys.DISCLAIMER_TEXT.key).asString()
    }

    private val _uiState: MutableStateFlow<OurStoryState> = MutableStateFlow(OurStoryState())
    val uiState: StateFlow<OurStoryState> = _uiState
        .onStart {
            updateOurStoryState()
            analytics.trackScreenViewEvent(screen = AnalyticsScreen.OurStory)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OurStoryState())

    private fun updateOurStoryState() {
        if (storyText.isNotBlank() && disclaimerText.isNotBlank()) {
            updateUiState {
                copy(
                    story = storyText,
                    disclaimer = disclaimerText,
                    isLoading = false,
                )
            }
        }
    }

    private fun updateUiState(block: OurStoryState.() -> OurStoryState) {
        _uiState.update(block)
    }
}
