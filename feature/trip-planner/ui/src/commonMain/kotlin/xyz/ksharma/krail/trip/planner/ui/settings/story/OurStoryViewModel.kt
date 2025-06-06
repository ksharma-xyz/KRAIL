package xyz.ksharma.krail.trip.planner.ui.settings.story

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.Flow
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.core.analytics.event.trackScreenViewEvent
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.remote_config.flag.Flag
import xyz.ksharma.krail.core.remote_config.flag.FlagKeys
import xyz.ksharma.krail.core.remote_config.flag.asString
import xyz.ksharma.krail.trip.planner.ui.MoleculeViewModel
import xyz.ksharma.krail.trip.planner.ui.state.settings.story.OurStoryEvent
import xyz.ksharma.krail.trip.planner.ui.state.settings.story.OurStoryState

class OurStoryViewModel(
    private val analytics: Analytics,
    private val flag: Flag,
) : MoleculeViewModel<OurStoryEvent, OurStoryState>() {

    /*
        private val _uiState: MutableStateFlow<OurStoryState> = MutableStateFlow(OurStoryState())
        val uiState: StateFlow<OurStoryState> = _uiState
            .onStart {
                updateOurStoryState()
                analytics.trackScreenViewEvent(screen = AnalyticsScreen.OurStory)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OurStoryState())
     */

    /*
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
     */

    /*
        private fun updateUiState(block: OurStoryState.() -> OurStoryState) {
            _uiState.update(block)
        }
     */

    @Composable
    override fun models(events: Flow<OurStoryEvent>): OurStoryState {
        return ourStoryPresenter(events)
    }

    @Composable
    private fun ourStoryPresenter(events: Flow<OurStoryEvent>): OurStoryState {
        val storyText: String by remember {
            mutableStateOf(flag.getFlagValue(FlagKeys.OUR_STORY_TEXT.key).asString())
        }

        val disclaimerText: String by remember {
            mutableStateOf(flag.getFlagValue(FlagKeys.DISCLAIMER_TEXT.key).asString())
        }

        LaunchedEffect(Unit) {
            log("OurStoryViewModel-  Our Story Text: ${storyText.take(10)}")
            analytics.trackScreenViewEvent(screen = AnalyticsScreen.OurStory)
        }

        val ourStoryState = if (storyText.isNotBlank() && disclaimerText.isNotBlank()) {
            OurStoryState(
                story = storyText,
                disclaimer = disclaimerText,
                isLoading = false,
            )
        } else {
            OurStoryState(
                story = "",
                disclaimer = "",
                isLoading = true,
            )
        }

        return ourStoryState
    }
}
