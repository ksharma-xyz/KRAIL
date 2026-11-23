package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
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
import androidx.compose.ui.text.TextLayoutResult
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
 * The decision bar on its own, at the widths and text sizes that decide its layout: always two taj buttons, one row when two whole
 * buttons fit on one line each and otherwise one above the other, the negative on top and the positive under it, both the full width,
 * never squeezed and never with a label on two lines. The Park & Ride answers are No thanks and Yes please at every text size, with
 * the longer phrase kept for a screen reader, and the choice of row or stack is the same for every state of a screen.
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
    private val useConfirmBar = mutableStateOf(false)
    private val barShown = mutableStateOf(true)
    private var contentSet = false

    private val askBar = requireNotNull(introBar(PreviewAskState))
    private val cardBar = requireNotNull(introBar(PreviewCardContentState))
    private val confirmBar = requireNotNull(introBar(PreviewConfirm))
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
        confirm: Boolean = false,
    ) {
        useCardBar.value = card
        useConfirmBar.value = confirm
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
                            val bar = if (useConfirmBar.value) confirmBar else if (useCardBar.value) cardBar else askBar
                            IntroDecisionBar(
                                left = bar.left.takeUnless { single.value },
                                right = if (single.value) themeBar.right else bar.right,
                                onEvent = { events += it },
                                shown = barShown.value,
                                rightShown = rightShown.value,
                                acceptsPresses = accepting.value,
                                labelPairs = bar.labelPairs,
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
            composeRule.onNodeWithContentDescription(THEME_CONFIRM_SPOKEN).assertDoesNotExist()
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
    fun `at 411 dp and 100 percent the answers are No thanks and Yes please in one row, the negative on the left`() {
        show(PHONE_WIDTH, 1f)

        primary().assertTextEquals(PARK_RIDE_YES)
        secondary().assertTextEquals(PARK_RIDE_NO)
        val yes = primary().bounds()
        val no = secondary().bounds()
        assertEquals(no.top, yes.top, "one row")
        assertTrue(no.right <= yes.left, "No thanks ends at ${no.right}, past where Yes please starts at ${yes.left}")
    }

    @Test
    fun `at 320 dp and 200 percent the bar is vertical, No thanks on top and Yes please under it, both the full width`() {
        show(NARROW_WIDTH, 2f)

        primary().assertTextEquals(PARK_RIDE_YES)
        secondary().assertTextEquals(PARK_RIDE_NO)
        val yes = primary().bounds()
        val no = secondary().bounds()
        assertTrue(no.bottom <= yes.top, "No thanks ends at ${no.bottom} and Yes please starts at ${yes.top}, so they are not stacked")
        assertEquals(no.left, yes.left)
        assertEquals(no.width, yes.width, "the two are the same width")
        assertEquals(NARROW_WIDTH.dp, yes.width, "stacked buttons use the whole width")
        assertEquals(12f, (yes.top - no.bottom).value, 0.5f, "the usual gap between them")
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(2)
    }

    @Test
    fun `when two whole buttons cannot fit the bar stacks with the negative on top at the same left and width`() {
        show(NARROW_WIDTH, 2f, card = true)

        primary().assertTextEquals(PARK_RIDE_ADD)
        secondary().assertTextEquals(PARK_RIDE_NOT_NOW)
        val add = primary().bounds()
        val notNow = secondary().bounds()
        assertTrue(
            notNow.bottom <= add.top,
            "Not now ends at ${notNow.bottom} and Add to home starts at ${add.top}, so they are not stacked, or not in this order",
        )
        assertEquals(add.left, notNow.left)
        assertEquals(add.width, notNow.width)
        assertEquals(NARROW_WIDTH.dp, add.width, "stacked buttons use the whole width")
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(2)
    }

    // Every label is drawn once, whole, and on one line: the layout of each text is read for real.
    private fun labelLayouts(): List<TextLayoutResult> = composeRule.onAllNodes(
        SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
        useUnmergedTree = true,
    ).fetchSemanticsNodes().map { node ->
        val results = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        results.single()
    }

    @Test
    fun `no label is ever on two lines, whatever the width, the text size or the state, and both buttons are inside the window`() {
        listOf(false, true).forEach { card ->
            listOf(NARROW_WIDTH, 320, 360, PHONE_WIDTH, 440, WIDE_WIDTH).forEach { widthDp ->
                listOf(1f, 1.3f, 1.5f, 1.8f, 2f).forEach { scale ->
                    show(widthDp, scale, card = card)
                    val where = "card $card, $widthDp dp, $scale"

                    labelLayouts().forEach { layout ->
                        assertEquals(1, layout.lineCount, "\"${layout.layoutInput.text.text}\" is on two lines at $where")
                    }
                    listOf(primary(), secondary()).forEach { button ->
                        val bounds = button.bounds()
                        assertTrue(bounds.left >= 0.dp && bounds.right <= widthDp.dp + ROUNDING, "a button is outside the window at $where: $bounds")
                        assertTrue(bounds.height >= MIN_TOUCH_HEIGHT - ROUNDING, "a button is only ${bounds.height} tall at $where")
                    }
                }
            }
        }
    }

    @Test
    fun `the confirm bar never wraps a label either, is a row at 411 dp and stacked at 320 dp with 200 percent text, negative on top`() {
        listOf(NARROW_WIDTH, 320, 360, PHONE_WIDTH, 440, WIDE_WIDTH).forEach { widthDp ->
            listOf(1f, 1.3f, 1.5f, 1.8f, 2f).forEach { scale ->
                show(widthDp, scale, confirm = true)
                val where = "$widthDp dp, $scale"

                secondary().assertTextEquals(CONFIRM_NOT_NOW)
                primary().assertTextEquals(CONFIRM_SAVE)
                labelLayouts().forEach { assertEquals(1, it.lineCount, "\"${it.layoutInput.text.text}\" is on two lines at $where") }
                val no = secondary().bounds()
                val yes = primary().bounds()
                assertTrue(no.left >= 0.dp && yes.right <= widthDp.dp + ROUNDING, "a button is outside the window at $where")
                assertTrue(no.height >= MIN_TOUCH_HEIGHT - ROUNDING && yes.height >= MIN_TOUCH_HEIGHT - ROUNDING, "a button is under 48 dp at $where")
                if (no.top != yes.top) assertTrue(no.bottom <= yes.top, "the negative button is not on top at $where")
            }
        }
        show(PHONE_WIDTH, 1f, confirm = true)
        assertEquals(secondary().bounds().top, primary().bounds().top, "one row at 411 dp and 100 percent")
        show(NARROW_WIDTH, 2f, confirm = true)
        assertTrue(secondary().bounds().bottom <= primary().bounds().top, "stacked at 320 dp and 200 percent")
    }

    @Test
    fun `the bar is a row or a stack for the whole screen, so it is the same for the question and for the card`() {
        listOf(NARROW_WIDTH, 320, 360, PHONE_WIDTH, 440, WIDE_WIDTH).forEach { widthDp ->
            listOf(1f, 1.3f, 1.5f, 1.8f, 2f).forEach { scale ->
                val stacked = listOf(false, true).map { card ->
                    show(widthDp, scale, card = card)
                    val first = secondary().bounds()
                    val second = primary().bounds()
                    first.top != second.top
                }
                assertEquals(stacked[0], stacked[1], "the question and the card differ at $widthDp dp, $scale")
            }
        }
    }

    @Test
    fun `the room the bar takes does not change between the question and the card, so nothing above it moves`() {
        listOf(NARROW_WIDTH, PHONE_WIDTH, WIDE_WIDTH).forEach { widthDp ->
            listOf(1f, 1.5f, 2f).forEach { scale ->
                show(widthDp, scale, card = false)
                val question = composeRule.onRoot().getUnclippedBoundsInRoot().height
                show(widthDp, scale, card = true)
                assertEquals(question, composeRule.onRoot().getUnclippedBoundsInRoot().height, "the bar changed height at $widthDp dp, $scale")
            }
        }
    }

    @Test
    fun `the reading order is the negative button and then the positive one, in a row and stacked`() {
        listOf(PHONE_WIDTH to 1f, NARROW_WIDTH to 2f).forEach { (widthDp, scale) ->
            show(widthDp, scale)

            val order = composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().map { it.config[SemanticsProperties.ContentDescription].single() }
            assertEquals(listOf(PARK_RIDE_NO_SPOKEN, PARK_RIDE_YES_SPOKEN), order, "at $widthDp dp, $scale")
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
        val MIN_TOUCH_HEIGHT = 48.dp
    }
}
