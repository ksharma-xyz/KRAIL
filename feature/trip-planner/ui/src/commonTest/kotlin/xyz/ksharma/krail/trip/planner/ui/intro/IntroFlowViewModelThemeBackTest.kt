package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.lifecycle.SavedStateHandle
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Back from the screen after the theme screen, which needs the theme screen to be the first one of the flow, so
 * the real default order is what these hold.
 */
class IntroFlowViewModelThemeBackTest {

    private val metro = KrailThemeStyle.Metro.id
    private val ferry = KrailThemeStyle.Ferry.id

    private fun IntroRig.reported(): List<Any?> =
        analytics.getTrackedEvents("theme_selected").map { it.properties?.get("themeId") }

    @Test
    fun `a rider who has not chosen yet starts on the theme screen with nothing selected`() = krailRunTest {
        val rig = IntroRig(this, handle = SavedStateHandle())

        assertEquals(IntroState(step = IntroStep.THEME), rig.state)
    }

    @Test
    fun `going back and confirming the same theme again is not a second choice, and a different one is`() = krailRunTest {
        val rig = IntroRig(this, handle = SavedStateHandle())
        rig.send(IntroUiEvent.PickTheme(metro), IntroUiEvent.ConfirmTheme)

        rig.send(IntroUiEvent.Back)
        assertEquals(IntroStep.THEME, rig.state.step)
        assertEquals(metro, rig.state.selectedThemeId, "the selection is still shown")
        rig.send(IntroUiEvent.ConfirmTheme)
        assertEquals(listOf<Any?>(metro.toString()), rig.reported())
        assertEquals(IntroStep.PARK_RIDE, rig.state.step)

        rig.send(IntroUiEvent.Back, IntroUiEvent.PickTheme(ferry), IntroUiEvent.ConfirmTheme)
        assertEquals(listOf<Any?>(metro.toString(), ferry.toString()), rig.reported())
    }
}
