package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.lifecycle.SavedStateHandle
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.test.Test
import kotlin.test.assertEquals

/** The theme screen as the first one of the flow, so the real default order is what this holds. */
class IntroFlowViewModelThemeStartTest {

    @Test
    fun `a rider who has not chosen yet starts on the theme screen with nothing selected`() = krailRunTest {
        val rig = IntroRig(this, handle = SavedStateHandle())

        assertEquals(IntroState(step = IntroStep.THEME), rig.state)
    }
}
