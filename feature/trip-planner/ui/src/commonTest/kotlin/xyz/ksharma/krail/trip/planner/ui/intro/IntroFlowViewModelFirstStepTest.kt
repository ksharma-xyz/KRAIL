package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.lifecycle.SavedStateHandle
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The Park & Ride screen as the first one of the flow, so the real default order is what this holds. */
class IntroFlowViewModelFirstStepTest {

    @Test
    fun `a rider who has not answered starts on the Park and Ride screen with nothing selected`() = krailRunTest {
        val rig = IntroRig(this, handle = SavedStateHandle())

        assertEquals(IntroState(step = IntroStep.PARK_RIDE), rig.state)
        assertNull(rig.state.selectedThemeId)
        assertNull(rig.state.tripDecision)
    }
}
