package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
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
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The decision bar on its own, at the widths and text sizes that decide its layout: always two taj buttons,
 * one row when two whole buttons fit and otherwise one above the other with the primary on top, never
 * squeezed, and the Park & Ride answers Yes and No at every text size with the longer phrase kept for a screen reader.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroDecisionBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<IntroUiEvent>()
    private val width = mutableStateOf(PHONE_WIDTH)
    private val fontScale = mutableStateOf(1f)
    private val rightShown = mutableStateOf(true)
    private val accepting = mutableStateOf(true)
    private val single = mutableStateOf(false)
    private val useCardBar = mutableStateOf(false)
    private val barShown = mutableStateOf(true)
    private var contentSet = false

    private val askBar = requireNotNull(introBar(PreviewAskState))
    private val cardBar = requireNotNull(introBar(PreviewCardContentState))
    private val themeBar = requireNotNull(
        introBar(IntroState(step = IntroStep.THEME, selectedThemeId = 1)),
    )

    private fun show(
        widthDp: Int,
        scale: Float,
        onlyRight: Boolean = false,
        shown: Boolean = true,
        card: Boolean = false,
        showRight: Boolean = true,
        acceptPresses: Boolean = true,
    ) {
        useCardBar.value = card
        width.value = widthDp
        barShown.value = shown
        single.value = onlyRight
        fontScale.value = scale
        rightShown.value = showRight
        accepting.value = acceptPresses
        if (!contentSet) {
            contentSet = true
            composeRule.setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = fontScale.value)) {
                    PreviewTheme {
                        Box(modifier = Modifier.width(width.value.dp)) {
                            val bar = if (useCardBar.value) cardBar else askBar
                            IntroDecisionBar(
                                left = bar.left.takeUnless { single.value },
                                right = if (single.value) themeBar.right else bar.right,
                                onEvent = { events += it },
                                shown = barShown.value,
                                rightShown = rightShown.value,
                                acceptsPresses = accepting.value,
                            )
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun primary() = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION)

    private fun secondary() = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_SECONDARY_ACTION)

    private fun SemanticsNodeInteraction.bounds() = getUnclippedBoundsInRoot()

    @Test
    fun `a bar that is not shown yet keeps the height of its button, and the button then appears in that room`() {
        listOf(1f, 2f).forEach { scale ->
            show(NARROW_WIDTH, scale, onlyRight = true, shown = false)
            val reserved = composeRule.onRoot().getUnclippedBoundsInRoot().height

            primary().assertDoesNotExist()
            composeRule.onNodeWithContentDescription(THEME_CONFIRM).assertDoesNotExist()
            composeRule.onAllNodes(hasClickAction()).assertCountEquals(0)
            assertTrue(reserved > 0.dp, "no room is kept at $scale")

            show(NARROW_WIDTH, scale, onlyRight = true, shown = true)

            primary().assertIsEnabled()
            assertEquals(reserved, composeRule.onRoot().getUnclippedBoundsInRoot().height, "the bar grew at $scale")
        }
    }

    @Test
    fun `a two button bar that is not shown keeps its room and has nothing to tap`() {
        show(PHONE_WIDTH, 1f, shown = true)
        val room = composeRule.onRoot().getUnclippedBoundsInRoot().height

        show(PHONE_WIDTH, 1f, shown = false)

        primary().assertDoesNotExist()
        secondary().assertDoesNotExist()
        assertEquals(room, composeRule.onRoot().getUnclippedBoundsInRoot().height)
    }

    @Test
    fun `at 100 percent the answers are Yes and No, in one row or stacked with the primary on top`() {
        show(PHONE_WIDTH, 1f)

        primary().assertTextEquals(PARK_RIDE_YES)
        secondary().assertTextEquals(PARK_RIDE_NO)
        val yes = primary().bounds()
        val no = secondary().bounds()
        val oneRow = no.top == yes.top && no.right <= yes.left
        val stacked = yes.top < no.top && yes.bottom <= no.top && yes.left == no.left && yes.width == no.width
        assertTrue(oneRow || stacked, "neither one row nor stacked, with Yes at $yes and No at $no")
    }

    @Test
    fun `when two whole buttons cannot fit the bar stacks with the primary on top at the same left and width`() {
        show(NARROW_WIDTH, 2f, card = true)

        primary().assertTextEquals(PARK_RIDE_ADD)
        secondary().assertTextEquals(PARK_RIDE_NOT_NOW)
        val add = primary().bounds()
        val notNow = secondary().bounds()
        assertTrue(
            add.bottom <= notNow.top,
            "Add to home ends at ${add.bottom} and Not now starts at ${notNow.top}, so they are not stacked",
        )
        assertEquals(add.left, notNow.left)
        assertEquals(add.width, notNow.width)
        assertEquals(NARROW_WIDTH.dp, add.width, "stacked buttons use the whole width")
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(2)
    }

    @Test
    fun `at every text size the answers are the one words Yes and No, and the narrowest window at 200 percent keeps one row`() {
        listOf(1f, 1.3f, 1.31f, 2f).forEach { scale ->
            show(NARROW_WIDTH, scale)

            primary().assertTextEquals(PARK_RIDE_YES)
            secondary().assertTextEquals(PARK_RIDE_NO)
            val yes = primary().bounds()
            val no = secondary().bounds()
            assertEquals(no.top, yes.top, "one row at $scale")
            assertTrue(no.right <= yes.left, "No ends at ${no.right}, past where Yes starts at ${yes.left}, at $scale")
            assertTrue(yes.right <= NARROW_WIDTH.dp, "Yes ends at ${yes.right}, outside the ${NARROW_WIDTH}dp bar")
        }
    }

    @Test
    fun `the longer phrase is the description at every text size, so a screen reader hears it and voice control still finds the word`() {
        listOf(1f, 1.3f, 1.31f, 2f).forEach { scale ->
            show(PHONE_WIDTH, scale)

            composeRule.onNodeWithContentDescription(PARK_RIDE_YES_SPOKEN).assertExists()
            composeRule.onNodeWithContentDescription(PARK_RIDE_NO_SPOKEN).assertExists()
            assertTrue(PARK_RIDE_YES_SPOKEN.startsWith(PARK_RIDE_YES), "the word shown is the start of the phrase said")
            assertTrue(PARK_RIDE_NO_SPOKEN.startsWith(PARK_RIDE_NO), "the word shown is the start of the phrase said")
        }
    }

    @Test
    fun `no button is ever disabled, and each button sends its own event`() {
        show(PHONE_WIDTH, 1f)

        secondary().assertIsEnabled()
        primary().assertIsEnabled()
        primary().performClick()
        secondary().performClick()

        assertEquals<List<IntroUiEvent>>(
            listOf(IntroUiEvent.AnswerParkRide(yes = true), IntroUiEvent.AnswerParkRide(yes = false)),
            events,
        )
    }

    @Test
    fun `a right button that is not shown keeps its room, takes no press and leaves the left button live`() {
        show(NARROW_WIDTH, 1f, card = true)
        val withBoth = composeRule.onRoot().getUnclippedBoundsInRoot()
        val addLeft = primary().bounds().left

        show(NARROW_WIDTH, 1f, card = true, showRight = false)

        assertEquals(withBoth, composeRule.onRoot().getUnclippedBoundsInRoot(), "the bar changed size without Add to home")
        primary().assertDoesNotExist()
        secondary().assertIsEnabled().assertTextEquals(PARK_RIDE_NOT_NOW)
        // A touch where the hidden button is takes nothing, and is not a click on something else.
        composeRule.onRoot().performTouchInput { click(Offset(addLeft.toPx() + 8f, centerY)) }
        composeRule.waitForIdle()
        assertEquals<List<IntroUiEvent>>(emptyList(), events)

        secondary().performClick()
        assertEquals<List<IntroUiEvent>>(listOf(IntroUiEvent.DeclineParkRide), events)

        show(NARROW_WIDTH, 1f, card = true, showRight = true)
        primary().assertIsEnabled().performClick()
        assertEquals(IntroUiEvent.AddParkRideAndContinue, events.last())
    }

    @Test
    fun `while the screen is still moving the bar takes no press, and is drawn live and not disabled`() {
        show(PHONE_WIDTH, 1f, acceptPresses = false)

        primary().assertIsEnabled()
        secondary().assertIsEnabled()
        composeRule.onRoot().performTouchInput { click(Offset(width - 20f, centerY)) }
        composeRule.onRoot().performTouchInput { click(Offset(20f, centerY)) }
        composeRule.waitForIdle()
        assertEquals<List<IntroUiEvent>>(emptyList(), events, "a press went through while the screen was moving")

        show(PHONE_WIDTH, 1f, acceptPresses = true)
        composeRule.onRoot().performTouchInput { click(Offset(width - 20f, centerY)) }
        composeRule.waitForIdle()
        assertEquals<List<IntroUiEvent>>(listOf(IntroUiEvent.AnswerParkRide(yes = true)), events)
    }

    @Test
    fun `a label is never squeezed onto two lines in a row`() {
        listOf(PHONE_WIDTH, WIDE_WIDTH).forEach { widthDp ->
            show(widthDp, 1f)

            val yes = primary().bounds()
            val no = secondary().bounds()
            if (no.top == yes.top) {
                assertTrue(yes.height < ONE_LINE_BUTTON_MAX, "Yes is ${yes.height} high in a row, so its label wrapped")
                assertTrue(no.height < ONE_LINE_BUTTON_MAX, "No is ${no.height} high in a row, so its label wrapped")
            }
        }
    }

    @Test
    fun `with no left button the bar is the one primary across the whole width, as tall as the two button bar`() {
        show(PHONE_WIDTH, 1f)
        val rowHeight = primary().bounds().height

        show(PHONE_WIDTH, 1f, onlyRight = true)

        secondary().assertDoesNotExist()
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(1)
        val only = primary().bounds()
        assertTrue(only.width >= PHONE_WIDTH.dp - ROUNDING, "the one button is not the full width: $only")
        assertEquals(0.dp, only.left)
        assertEquals(rowHeight, only.height, "the one button is not as tall as the two button bar")
    }

    @Test
    fun `the one button keeps its whole label and the full width at the narrowest window at 200 percent, and is live`() {
        show(NARROW_WIDTH, 2f, onlyRight = true)

        primary().assertTextEquals(themeBar.right.label).assertIsEnabled()
        assertTrue(primary().bounds().width >= NARROW_WIDTH.dp - ROUNDING, "not the full width: ${primary().bounds()}")
        primary().performClick()
        assertEquals<List<IntroUiEvent>>(listOf(IntroUiEvent.ConfirmTheme), events)
    }

    private companion object {
        const val PHONE_WIDTH = 379
        const val WIDE_WIDTH = 480
        const val NARROW_WIDTH = 288
        val ROUNDING = 1.dp
        val ONE_LINE_BUTTON_MAX = 64.dp
    }
}
