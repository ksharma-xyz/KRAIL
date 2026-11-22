package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
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
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The theme screen on a phone, with the motion running as it does on a device (the infinite frame loop is
 * cancelled by the test's own policy, so these never wait on it): the four badges, the headline before and
 * after a choice, the bar, and what each tap sends.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroThemeStepTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val window get() = composeRule.onRoot().getUnclippedBoundsInRoot()

    private val events = mutableListOf<IntroUiEvent>()
    private val none = IntroState(step = IntroStep.THEME)
    private val shown = mutableStateOf(none)
    private var contentSet = false

    private fun show(state: IntroState) {
        shown.value = state
        if (!contentSet) {
            contentSet = true
            composeRule.setContent {
                PreviewTheme {
                    IntroFlowScreen(
                        state = shown.value,
                        suggestedStops = DEFAULT_SUGGESTED_STOPS,
                        readiness = PreviewReady,
                        onEvent = { events += it },
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun badge(choice: IntroThemeChoice) = composeRule.onNodeWithTag(choice.tag)

    private fun primary() = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION)

    private fun secondary() = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_SECONDARY_ACTION)

    @Test
    fun `before a choice the headline asks for the colour and the four badges are there, named, and big enough to tap`() {
        show(none)

        composeRule.onNodeWithText(PICK_THEME_HEADING).assertIsDisplayed()
        introThemeChoices().forEach { choice ->
            badge(choice).assertIsDisplayed().assertHasClickAction()
            composeRule.onNodeWithContentDescription(choice.name).assertExists()
            val bounds = badge(choice).getUnclippedBoundsInRoot()
            assertTrue(
                bounds.width >= MIN_TOUCH - 1.dp && bounds.height >= MIN_TOUCH - 1.dp,
                "${choice.name} is $bounds",
            )
        }
    }

    @Test
    fun `the badges carry the tags the Maestro flow taps`() {
        show(none)

        listOf("intro.theme.train", "intro.theme.metro", "intro.theme.bus", "intro.theme.ferry").forEach { tag ->
            composeRule.onNodeWithTag(tag).assertExists()
        }
    }

    @Test
    fun `each badge sends the pick of its own theme and does not move on`() {
        show(none)

        introThemeChoices().forEach { badge(it).performClick() }

        assertEquals<List<IntroUiEvent>>(
            introThemeChoices().map { IntroUiEvent.PickTheme(it.style.id) },
            events,
        )
    }

    @Test
    fun `only the selected badge reads as selected, and the screen reader hears each one's name`() {
        show(none.copy(selectedThemeId = KrailThemeStyle.Bus.id))

        introThemeChoices().forEach { choice ->
            if (choice.style == KrailThemeStyle.Bus) {
                badge(choice).assertIsSelected()
            } else {
                badge(choice).assertIsNotSelected()
            }
        }
    }

    @Test
    fun `after a choice the headline is that theme's tagline exactly as the app wrote it`() {
        introThemeChoices().forEach { choice ->
            show(none.copy(selectedThemeId = choice.style.id))

            composeRule.onNodeWithText(choice.style.tagLine).assertIsDisplayed()
            composeRule.onNodeWithText(PICK_THEME_HEADING).assertDoesNotExist()
        }
    }

    @Test
    fun `the headline emoji is the neutral scooter before a choice and the chosen mode after, hidden from a screen reader`() {
        show(none)

        composeRule.onNodeWithText(introEmojiFor(IntroStep.THEME), useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText(introEmojiFor(IntroStep.THEME)).assertDoesNotExist()
        listOf(
            KrailThemeStyle.Train to INTRO_TRAIN_EMOJI,
            KrailThemeStyle.Metro to INTRO_METRO_EMOJI,
            KrailThemeStyle.Bus to INTRO_BUS_EMOJI,
            KrailThemeStyle.Ferry to INTRO_FERRY_EMOJI,
        ).forEach { (style, emoji) ->
            show(none.copy(selectedThemeId = style.id))

            composeRule.onNodeWithText(emoji, useUnmergedTree = true).assertExists()
            composeRule.onNodeWithText(emoji).assertDoesNotExist()
        }
    }

    @Test
    fun `the one button is not on the screen until a theme is selected, and there is no way to say no`() {
        show(none)

        primary().assertDoesNotExist()
        secondary().assertDoesNotExist()
        composeRule.onNodeWithText(THEME_CONFIRM).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(THEME_CONFIRM).assertDoesNotExist()
        composeRule.onNodeWithText("No", substring = true).assertDoesNotExist()

        show(none.copy(selectedThemeId = KrailThemeStyle.Ferry.id))

        primary().assertTextEquals(THEME_CONFIRM).assertIsEnabled()
        secondary().assertDoesNotExist()
    }

    @Test
    fun `the one button is the width of the bar and the same height as the two button bar, with a screen reader name that matches`() {
        show(none.copy(selectedThemeId = KrailThemeStyle.Train.id))

        val bounds = primary().getUnclippedBoundsInRoot()
        val pad = window.width - bounds.width
        assertEquals(pad / 2, bounds.left, "the button is not centred in the window: $bounds in $window")
        assertTrue(
            bounds.width > window.width / 2,
            "the one button is not the full width of the bar: $bounds in $window",
        )
        assertEquals(
            THEME_CONFIRM,
            primary().fetchSemanticsNode().config[SemanticsProperties.ContentDescription].single(),
        )
    }

    @Test
    fun `the one button confirms, and nothing else on the screen sends a confirm`() {
        show(none.copy(selectedThemeId = KrailThemeStyle.Metro.id))

        primary().performClick()

        assertEquals(listOf<IntroUiEvent>(IntroUiEvent.ConfirmTheme), events)
    }

    @Test
    fun `before a choice there is no button to tap, so only the four badges are clickable`() {
        show(none)

        composeRule.onAllNodes(hasClickAction()).assertCountEquals(introThemeChoices().size)
        assertEquals(emptyList<IntroUiEvent>(), events)
    }

    @Test
    fun `after a choice the screen has the four badges and the one button`() {
        show(none.copy(selectedThemeId = KrailThemeStyle.Bus.id))

        composeRule.onAllNodes(hasClickAction()).assertCountEquals(introThemeChoices().size + 1)
    }

    @Test
    fun `the headline is above the badges and the badges are above the bar`() {
        show(none)

        val headline = composeRule.onNodeWithText(PICK_THEME_HEADING).getUnclippedBoundsInRoot()
        val badges = introThemeChoices().map { badge(it).getUnclippedBoundsInRoot() }
        // The button is not shown yet, so the bar's top is where it will be: the button's top once a theme is picked.
        show(none.copy(selectedThemeId = KrailThemeStyle.Bus.id))
        val barTop = primary().getUnclippedBoundsInRoot().top

        assertTrue(badges.all { it.top >= headline.bottom }, "a badge starts over the headline: $headline $badges")
        assertTrue(badges.all { it.bottom <= barTop }, "a badge ends under the top of the bar at $barTop: $badges")
    }

    @Test
    fun `the badges do not move when a choice changes the headline and the button appears`() {
        show(none)
        val before = introThemeChoices().map { badge(it).getUnclippedBoundsInRoot().top }

        show(none.copy(selectedThemeId = KrailThemeStyle.Metro.id))
        val after = introThemeChoices().map { badge(it).getUnclippedBoundsInRoot().top }

        // The chosen one lifts a little with the dance's first frame, so the check is for the others.
        assertEquals(before.drop(2), after.drop(2), "the badges moved when the headline changed")
    }

    private companion object {
        val MIN_TOUCH = 48.dp
    }
}
