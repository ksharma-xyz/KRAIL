package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
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
 * squeezed, and the short Yes and No above 130 percent text with the whole phrase kept for a screen reader.
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
    private val rightEnabled = mutableStateOf(true)
    private val single = mutableStateOf(false)
    private val barShown = mutableStateOf(true)
    private var contentSet = false

    private val bar = requireNotNull(introBar(PreviewAskState, PreviewReady))
    private val themeBar = requireNotNull(
        introBar(IntroState(step = IntroStep.THEME, selectedThemeId = 1), PreviewReady),
    )

    private fun show(
        widthDp: Int,
        scale: Float,
        enabled: Boolean = true,
        onlyRight: Boolean = false,
        shown: Boolean = true,
    ) {
        width.value = widthDp
        barShown.value = shown
        single.value = onlyRight
        fontScale.value = scale
        rightEnabled.value = enabled
        if (!contentSet) {
            contentSet = true
            composeRule.setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = fontScale.value)) {
                    PreviewTheme {
                        Box(modifier = Modifier.width(width.value.dp)) {
                            IntroDecisionBar(
                                left = bar.left.takeUnless { single.value },
                                right = if (single.value) themeBar.right else bar.right,
                                onEvent = { events += it },
                                rightEnabled = rightEnabled.value,
                                shown = barShown.value,
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
    fun `at 100 percent the labels are whole and the buttons are one row or stacked with the primary on top`() {
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
        show(NARROW_WIDTH, LARGE_BUT_NOT_SHORTENED)

        primary().assertTextEquals(PARK_RIDE_YES)
        secondary().assertTextEquals(PARK_RIDE_NO)
        val yes = primary().bounds()
        val no = secondary().bounds()
        assertTrue(
            yes.bottom <= no.top,
            "Yes ends at ${yes.bottom} and No starts at ${no.top}, so they are not stacked",
        )
        assertEquals(yes.left, no.left)
        assertEquals(yes.width, no.width)
        assertEquals(NARROW_WIDTH.dp, yes.width, "stacked buttons use the whole width")
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(2)
    }

    @Test
    fun `above 130 percent the short Yes and No sit in one row and a screen reader still gets the whole phrases`() {
        show(NARROW_WIDTH, 2f)

        primary().assertTextEquals(PARK_RIDE_YES_COMPACT)
        secondary().assertTextEquals(PARK_RIDE_NO_COMPACT)
        composeRule.onNodeWithContentDescription(PARK_RIDE_YES).assertExists()
        composeRule.onNodeWithContentDescription(PARK_RIDE_NO).assertExists()
        val yes = primary().bounds()
        val no = secondary().bounds()
        assertEquals(no.top, yes.top)
        assertTrue(no.right <= yes.left, "No ends at ${no.right}, past where Yes starts at ${yes.left}")
        assertTrue(yes.right <= NARROW_WIDTH.dp, "Yes ends at ${yes.right}, outside the ${NARROW_WIDTH}dp bar")
    }

    @Test
    fun `the whole phrase is the description at every text size, so the screen reader never hears the short form`() {
        listOf(1f, 1.3f, 1.31f, 2f).forEach { scale ->
            show(PHONE_WIDTH, scale)

            composeRule.onNodeWithContentDescription(PARK_RIDE_YES).assertExists()
            composeRule.onNodeWithContentDescription(PARK_RIDE_NO).assertExists()
        }
    }

    @Test
    fun `130 percent still shows the whole words and just above it shows the short ones`() {
        show(PHONE_WIDTH, 1.3f)
        primary().assertTextEquals(PARK_RIDE_YES)

        show(PHONE_WIDTH, 1.31f)
        primary().assertTextEquals(PARK_RIDE_YES_COMPACT)
        secondary().assertTextEquals(PARK_RIDE_NO_COMPACT)
    }

    @Test
    fun `only the right button is ever disabled, and a disabled one sends nothing`() {
        show(PHONE_WIDTH, 1f, enabled = false)

        secondary().assertIsEnabled()
        primary().assertIsNotEnabled()
        primary().performClick()
        assertEquals<List<IntroUiEvent>>(emptyList(), events)

        secondary().performClick()
        assertEquals<List<IntroUiEvent>>(listOf(IntroUiEvent.AnswerParkRide(yes = false)), events)
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
    fun `the one button keeps its whole label and the full width at the narrowest window at 200 percent, and stays off until enabled`() {
        show(NARROW_WIDTH, 2f, enabled = false, onlyRight = true)

        primary().assertTextEquals(themeBar.right.label).assertIsNotEnabled()
        assertTrue(primary().bounds().width >= NARROW_WIDTH.dp - ROUNDING, "not the full width: ${primary().bounds()}")
        primary().performClick()
        assertEquals<List<IntroUiEvent>>(emptyList(), events)

        show(NARROW_WIDTH, 2f, enabled = true, onlyRight = true)
        primary().performClick()
        assertEquals<List<IntroUiEvent>>(listOf(IntroUiEvent.ConfirmTheme), events)
    }

    private companion object {
        const val PHONE_WIDTH = 379
        const val WIDE_WIDTH = 480
        const val NARROW_WIDTH = 288
        const val LARGE_BUT_NOT_SHORTENED = 1.3f
        val ROUNDING = 1.dp
        val ONE_LINE_BUTTON_MAX = 64.dp
    }
}
