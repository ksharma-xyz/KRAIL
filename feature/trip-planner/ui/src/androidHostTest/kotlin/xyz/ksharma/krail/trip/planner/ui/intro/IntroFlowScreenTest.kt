package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.test.assertEquals

/**
 * The shell of the Intro: the bottom button, the back button and system back. The screens are left
 * empty here, because the shell is what is under test.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroFlowScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<IntroUiEvent>()
    private val ready = StopsReadiness.Ready(stopCount = 100)
    private val stepTwo = IntroState(step = IntroStep.DESTINATION)

    private val shownState = mutableStateOf(IntroState())
    private val shownReadiness = mutableStateOf<StopsReadiness>(ready)
    private var contentSet = false

    /** Sets the content once, and from then on changes what it shows, as a test may only set it once. */
    private fun show(state: IntroState, readiness: StopsReadiness = ready) {
        shownState.value = state
        shownReadiness.value = readiness
        if (!contentSet) {
            contentSet = true
            composeRule.setContent {
                PreviewTheme {
                    IntroFlowScreen(
                        state = shownState.value,
                        suggestedStops = DEFAULT_SUGGESTED_STOPS,
                        readiness = shownReadiness.value,
                        onEvent = { events += it },
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun button() = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION)

    @Test
    fun `step 1 shows an enabled Next button under the test tag`() {
        show(IntroState())

        button().assertTextEquals("Next").assertIsEnabled()
    }

    @Test
    fun `the button is disabled and reads Getting ready while the stops load`() {
        show(stepTwo, StopsReadiness.Loading)

        button().assertTextEquals("Getting ready").assertIsNotEnabled()
    }

    @Test
    fun `the button is disabled and reads Getting ready before the stops have started`() {
        show(stepTwo, StopsReadiness.Unknown)

        button().assertTextEquals("Getting ready").assertIsNotEnabled()
    }

    @Test
    fun `the button reads Save and start but is disabled with only one stop picked`() {
        show(stepTwo.copy(startStopId = "200060", startName = "Central Station"))

        button().assertTextEquals("Save and start").assertIsNotEnabled()
    }

    @Test
    fun `the button is enabled for each action that finishes the Intro`() {
        val both = stepTwo.copy(
            startStopId = "200060",
            startName = "Central Station",
            endStopId = "200080",
            endName = "Wynyard Station",
        )
        val finishing = listOf(
            "Skip for now" to stepTwo,
            "Save and start" to both,
            "Let's KRAIL" to both.copy(saved = true),
        )

        finishing.forEach { (words, state) ->
            show(state)
            button().assertTextEquals(words).assertIsEnabled()
        }
    }

    @Test
    fun `failed stops do not hold the button back`() {
        show(stepTwo, StopsReadiness.Failed(attempt = 1, reason = "disk full"))

        button().assertTextEquals("Skip for now").assertIsEnabled()
    }

    @Test
    fun `pressing the enabled button tells the view model to finish`() {
        show(IntroState())

        button().performClick()

        assertEquals<List<IntroUiEvent>>(listOf(IntroUiEvent.FinishPressed), events)
    }

    @Test
    fun `there are no progress dots on any screen`() {
        IntroStep.entries.forEach { step ->
            show(IntroState(step = step))

            composeRule.onNodeWithContentDescription("Step 1 of 2").assertDoesNotExist()
            composeRule.onNodeWithContentDescription("Step 2 of 2").assertDoesNotExist()
        }
    }

    @Test
    fun `step 1 has no back button`() {
        show(IntroState())

        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
    }

    @Test
    fun `the back button on step 2 sends Back`() {
        show(stepTwo)

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals<List<IntroUiEvent>>(listOf(IntroUiEvent.Back), events)
    }

    @Test
    fun `system back on step 2 sends Back`() {
        show(stepTwo)

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        assertEquals<List<IntroUiEvent>>(listOf(IntroUiEvent.Back), events)
    }

    @Test
    fun `system back on step 1 is left to the platform`() {
        show(IntroState())

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        assertEquals<List<IntroUiEvent>>(emptyList(), events)
    }
}
