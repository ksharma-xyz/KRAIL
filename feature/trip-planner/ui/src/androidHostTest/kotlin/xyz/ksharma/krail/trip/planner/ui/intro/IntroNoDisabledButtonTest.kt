package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroSearchPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.TripDecision
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Intro never draws a disabled button. Every screen, in every state its bar can be in, is shown and its whole
 * semantics tree is read, merged and unmerged, so a button that is hidden and keeps its room is read too: none of them
 * carries the disabled flag. A button is live, or it is not shown, and a rider is never held on one that cannot be pressed.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroNoDisabledButtonTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val shown = mutableStateOf(ON_PARK_RIDE)
    private var contentSet = false

    private val picked = IntroState(
        step = IntroStep.CONFIRM,
        parkRideAnswer = ParkRideAnswer.NO,
        startStopId = "200060",
        startName = "Central Station",
        endStopId = "200080",
        endName = "Wynyard Station",
    )

    private val disabled = SemanticsMatcher.keyIsDefined(SemanticsProperties.Disabled)

    private fun show(state: IntroState) {
        shown.value = state
        if (!contentSet) {
            contentSet = true
            composeRule.setContent {
                PreviewTheme {
                    IntroFlowScreen(state = shown.value, suggestedStops = DEFAULT_SUGGESTED_STOPS, onEvent = { _, _ -> })
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun theme(decision: TripDecision?, saved: Boolean, themeId: Int?) = picked.copy(
        step = IntroStep.THEME,
        tripDecision = decision,
        saved = saved,
        selectedThemeId = themeId,
    )

    // One state for each shape of bar the model can give, with the stops data, which is not an input, left out on purpose.
    private val everyScreen: List<Pair<String, IntroState>> = listOf(
        "park and ride asking" to ON_PARK_RIDE,
        "park and ride locating" to PreviewLocatingState,
        "park and ride card loading" to PreviewCardLoadingState,
        "park and ride card with numbers" to PreviewCardContentState,
        "park and ride card unavailable" to PreviewCardUnavailableState,
        "park and ride no station near" to ON_PARK_RIDE.copy(
            parkRideAnswer = ParkRideAnswer.YES,
            parkRideCard = ParkRideCardPhase.NoStationNear,
        ),
        "destination, nothing selected" to IntroState(step = IntroStep.DESTINATION),
        "destination, a stop selected" to IntroState(step = IntroStep.DESTINATION, selectedStop = CENTRAL_STOP),
        "destination, no match" to IntroState(step = IntroStep.DESTINATION, search = IntroSearchPhase.NoMatch),
        "origin, nothing selected" to IntroState(step = IntroStep.ORIGIN),
        "origin, a stop selected" to IntroState(step = IntroStep.ORIGIN, selectedStop = CENTRAL_STOP),
        "confirm" to picked,
    ) + TripDecision.entries.flatMap { decision ->
        listOf(false, true).flatMap { saved ->
            listOf(null, KrailThemeStyle.Bus.id).map { themeId ->
                "theme, $decision, saved $saved, theme $themeId" to theme(decision, saved, themeId)
            }
        }
    } + ("theme, no answer recorded" to theme(decision = null, saved = false, themeId = KrailThemeStyle.Train.id))

    @Test
    fun `no button on any screen in any state is disabled, in the merged tree or the unmerged one`() {
        everyScreen.forEach { (name, state) ->
            show(state)

            composeRule.onAllNodes(disabled, useUnmergedTree = true).assertCountEquals(0, name)
            composeRule.onAllNodes(disabled).assertCountEquals(0, name)
        }
    }

    @Test
    fun `the screens that ask for a press do have buttons, so the check above cannot pass by looking at nothing`() {
        val buttonsOnScreen = everyScreen.associate { (name, state) ->
            show(state)
            val bar = hasTestTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION) or
                hasTestTag(TripPlannerTestTags.INTRO_SECONDARY_ACTION)
            name to composeRule.onAllNodes(hasClickAction() and bar).fetchSemanticsNodes().size
        }

        assertEquals(2, buttonsOnScreen["park and ride asking"])
        assertEquals(1, buttonsOnScreen["park and ride locating"], "only the left button, Add to home is not shown")
        assertEquals(2, buttonsOnScreen["park and ride card loading"])
        assertEquals(2, buttonsOnScreen["park and ride card with numbers"])
        assertEquals(1, buttonsOnScreen["park and ride no station near"], "the one OK, live")
        assertEquals(0, buttonsOnScreen["destination, nothing selected"], "no button until a stop is selected")
        assertEquals(1, buttonsOnScreen["destination, a stop selected"])
        assertEquals(2, buttonsOnScreen["confirm"])
        assertEquals(0, buttonsOnScreen["theme, SAVED, saved false, theme null"], "no button until a theme is selected")
        assertEquals(1, buttonsOnScreen["theme, SAVED, saved false, theme ${KrailThemeStyle.Bus.id}"])
        assertTrue(buttonsOnScreen.values.any { it > 0 })
    }
}

private fun SemanticsNodeInteractionCollection.assertCountEquals(expected: Int, message: String) {
    assertEquals(expected, fetchSemanticsNodes().size, message)
}
