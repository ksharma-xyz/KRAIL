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
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.asString
import xyz.ksharma.krail.trip.planner.ui.MoleculeViewModel
import xyz.ksharma.krail.trip.planner.ui.state.settings.story.OurStoryEvent
import xyz.ksharma.krail.trip.planner.ui.state.settings.story.OurStoryState

class OurStoryViewModel(
    private val analytics: Analytics,
    private val flag: Flag,
) : MoleculeViewModel<OurStoryEvent, OurStoryState>() {

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
