package xyz.ksharma.core.test.viewmodels

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.test.runTest
import xyz.ksharma.core.test.fakes.FakeAnalytics
import xyz.ksharma.core.test.fakes.FakeFlag
import xyz.ksharma.krail.trip.planner.ui.settings.story.OurStoryViewModel
import xyz.ksharma.krail.trip.planner.ui.state.settings.story.OurStoryEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class OurStoryViewModelTest {

    private val analytics = FakeAnalytics()
    private val flag = FakeFlag()

    @Test
    fun `models emits correct state on init`() = runTest {
        val viewModel = OurStoryViewModel(analytics, flag)
        val events = MutableSharedFlow<OurStoryEvent>()

        moleculeFlow(RecompositionMode.Immediate) { viewModel.models(events) }.distinctUntilChanged()
            .test {
                val state = awaitItem()
                println("Initial state: $state")
                assertEquals("Story Text", state.story)
                assertEquals("Disclaimer Text", state.disclaimer)
                assertFalse(state.isLoading)
                cancelAndIgnoreRemainingEvents()
            }
    }
}
