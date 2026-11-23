package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStop
import kotlin.test.assertTrue

/**
 * A focus the host hands to the Intro stays on the Intro's root and never lands on a control (F3).
 *
 * The host here does what the pane scaffold of the list and detail scene does in a window wide enough for two panes: the
 * Intro is inside a focus group with a focus requester, and the focus is requested for it each time the content is
 * composed, which is what happens again after a turn of the screen. Before the fix that focus fell to the first control
 * of the screen, the search field on the destination and origin screens, which opened the keyboard without a tap.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroFocusHomeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val restorationTester by lazy { StateRestorationTester(composeRule) }

    // Whether the focus the host asked for is somewhere inside the Intro. Set again by every composition of the host.
    private var focusIsInside = false

    private val central = IntroStop(stopId = "200060", name = "Central Station")

    private val shown = mutableStateOf(IntroState())

    private fun show(state: IntroState) {
        shown.value = state
        restorationTester.setContent {
            PreviewTheme {
                val pane = remember { FocusRequester() }
                LaunchedEffect(pane) { pane.requestFocus() }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onFocusChanged { focusIsInside = it.hasFocus }
                        .focusRequester(pane)
                        .focusGroup(),
                ) {
                    IntroFlowScreen(
                        state = shown.value,
                        suggestedStops = DEFAULT_SUGGESTED_STOPS,
                        onEvent = { _, _ -> },
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun field() = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_STOP_FIELD)

    private fun assertNoControlHasTheFocus() {
        assertTrue(focusIsInside, "The host's focus request was not taken, so the test proves nothing")
        composeRule.onAllNodes(isFocused()).assertCountEquals(0)
    }

    @Test
    fun `a handed focus lands on no control on any of the five screens`() {
        show(IntroState(step = IntroStep.PARK_RIDE))
        assertNoControlHasTheFocus()

        // The Intro moves on a screen at a time, and the focus stays where it was put.
        IntroStep.entries.forEach { step ->
            shown.value = shown.value.copy(step = step)
            composeRule.waitForIdle()
            assertNoControlHasTheFocus()
        }
    }

    @Test
    fun `the search field does not take a handed focus with nothing selected`() {
        show(IntroState(step = IntroStep.DESTINATION))

        field().assertIsNotFocused()
        assertNoControlHasTheFocus()
    }

    @Test
    fun `the search field does not take a handed focus with a stop selected`() {
        show(IntroState(step = IntroStep.DESTINATION, selectedStop = central))

        composeRule.onNodeWithTag(TripPlannerTestTags.introStop(central.stopId)).assertIsSelected()
        field().assertIsNotFocused()
        assertNoControlHasTheFocus()
    }

    @Test
    fun `the origin screen's field does not take a handed focus with a stop selected`() {
        show(IntroState(step = IntroStep.ORIGIN, selectedStop = central))

        field().assertIsNotFocused()
        assertNoControlHasTheFocus()
    }

    @Test
    fun `a turn of the screen with a stop selected leaves the field without the focus`() {
        show(IntroState(step = IntroStep.DESTINATION, selectedStop = central))

        // The content is composed again from saved state, and the host asks for the focus again, as after a rotation.
        focusIsInside = false
        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        field().assertIsNotFocused()
        assertNoControlHasTheFocus()
    }

    @Test
    fun `a tap on the field still gives it the focus`() {
        show(IntroState(step = IntroStep.DESTINATION, selectedStop = central))

        field().performClick()
        composeRule.waitForIdle()

        field().assertIsFocused()
    }
}
