package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.KrailDimensions
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStop
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The decision bar is laid over the bottom of the screen and the scrolling content goes full height beneath it. With no
 * bar shown nothing is kept for it, so the last row scrolls to the bottom of the window. With the bar shown the content
 * gets room at its end, so the last row scrolls fully above the bar. The bar appearing moves nothing that is on screen.
 * Each class is one window and one text size, drawn as a preview is, with no motion.
 */
abstract class IntroBarOverlayTestBase(private val windowWidth: Dp, private val fontScale: Float) {

    @get:Rule
    val composeRule = createComposeRule()

    private val dim = KrailDimensions()
    private val rhythm = introRhythm(dim)
    private val shown = mutableStateOf(PreviewDestinationSuggestions)
    private val events = mutableListOf<IntroUiEvent>()
    private var contentSet = false

    private val lastEndId get() = DEFAULT_SUGGESTED_STOPS.end.last().stopId
    private val themeSelected = IntroState(step = IntroStep.THEME, selectedThemeId = KrailThemeStyle.Train.id)

    private fun show(state: IntroState) {
        shown.value = state
        if (!contentSet) {
            contentSet = true
            composeRule.setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, fontScale = fontScale),
                    LocalInspectionMode provides true,
                ) {
                    PreviewTheme {
                        IntroFlowScreen(
                            state = shown.value,
                            suggestedStops = DEFAULT_SUGGESTED_STOPS,
                            onEvent = { event, _ -> events += event },
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun window(): DpRect = composeRule.onRoot().getUnclippedBoundsInRoot()

    private fun row(stopId: String) = composeRule.onNodeWithTag(TripPlannerTestTags.introStop(stopId))

    // The list is lazy, so a row below the window is not drawn: it is either not there or it is below this line.
    private fun rowEnds(stopId: String): Dp? {
        val nodes = composeRule.onAllNodes(hasTestTag(TripPlannerTestTags.introStop(stopId)))
        return if (nodes.fetchSemanticsNodes().isEmpty()) null else bounds(nodes[0]).bottom
    }

    // Each suggested stop that is drawn now, with its bounds, so a row that is off the window is not read.
    private fun drawnRows(): Map<String, DpRect> = DEFAULT_SUGGESTED_STOPS.end.mapNotNull { stop ->
        val nodes = composeRule.onAllNodes(hasTestTag(TripPlannerTestTags.introStop(stop.stopId)))
        if (nodes.fetchSemanticsNodes().isEmpty()) null else stop.stopId to bounds(nodes[0])
    }.toMap()

    private fun primary() = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION)

    private fun bounds(node: SemanticsNodeInteraction) = node.getUnclippedBoundsInRoot()

    // The one tall scrolling column that holds the headline and the middle zone, and not the field's own scroll.
    private fun scroller(): SemanticsNodeInteraction {
        val all = composeRule.onAllNodes(hasScrollAction())
        val tallest = all.fetchSemanticsNodes().withIndex().maxBy { it.value.size.height }.index
        return all[tallest]
    }

    private fun scrollBy(px: Float) {
        scroller().performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, px) }
        composeRule.waitForIdle()
    }

    private fun scrollToEnd() = scrollBy(END_OF_SCROLL)

    // The bottom of the bar: the lower of its two buttons, as the two stack when they do not fit a row.
    private fun barBottom(): Dp {
        val secondary = composeRule.onAllNodes(hasTestTag(TripPlannerTestTags.INTRO_SECONDARY_ACTION))
        val buttons = listOf(bounds(primary())) + secondary.fetchSemanticsNodes().indices.map { bounds(secondary[it]) }
        return buttons.maxOf { it.bottom }
    }

    private fun heading() = composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).onFirst()

    @Test
    fun `the window is the width the checks assume`() {
        show(PreviewDestinationSuggestions)

        assertEquals(windowWidth, window().width)
    }

    @Test
    fun `the scrolling zone reaches the bottom of the window whether the bar is shown or not`() {
        listOf(PreviewDestinationSuggestions, PreviewDestinationSelected, themeSelected, IntroState(step = IntroStep.THEME))
            .forEach { state ->
                show(state)

                val zone = bounds(scroller())
                assertTrue(zone.bottom >= window().bottom - ROUNDING, "$state: the zone ends at ${zone.bottom}, the window at ${window().bottom}")
                assertTrue(zone.top <= ROUNDING, "$state: the zone starts at ${zone.top}")
            }
    }

    @Test
    fun `with no bar shown the last row scrolls whole to the bottom of the window and nothing is kept for a bar`() {
        show(PreviewDestinationSuggestions)
        primary().assertDoesNotExist()
        assertTrue((rowEnds(lastEndId) ?: Dp.Infinity) > window().bottom, "the list fits, so the scroll is not being tested")

        scrollToEnd()

        row(lastEndId).assertIsDisplayed()
        val hiddenGap = window().bottom - bounds(row(lastEndId)).bottom
        // What is under the last row is the middle zone's own 16 dp and the row's own padding, and no bar.
        assertTrue(hiddenGap < dim.spacingXXXXL + ROUNDING, "the last row ends $hiddenGap above the bottom of the window")

        // With the bar shown the end of the scroll is exactly the bar's height further down, and no more.
        show(PreviewDestinationSelected)
        scrollToEnd()
        val shownGap = window().bottom - bounds(row(lastEndId)).bottom
        val bar = bounds(primary()).height + dim.spacingL * 2
        assertEquals((hiddenGap + bar).value, shownGap.value, ROUNDING.value, "the room at the end is not the bar's $bar")
    }

    @Test
    fun `with the bar shown the last row scrolls fully above the bar`() {
        show(PreviewDestinationSelected)
        assertTrue((rowEnds(lastEndId) ?: Dp.Infinity) > bounds(primary()).top, "the list fits above the bar, so the scroll is not being tested")

        scrollToEnd()

        row(lastEndId).assertIsDisplayed()
        val barTop = bounds(primary()).top - dim.spacingL
        assertTrue(bounds(row(lastEndId)).bottom <= barTop + ROUNDING, "the last row ends at ${bounds(row(lastEndId)).bottom}, under the bar at $barTop")
        assertTrue(window().bottom - bounds(primary()).bottom <= dim.spacingL + ROUNDING, "the bar is not at the bottom of the window")
    }

    @Test
    fun `the bar appearing moves no row, no field and no headline`() {
        fun snapshot() = listOf(
            bounds(heading()),
            bounds(composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_STOP_FIELD)),
            bounds(row(DEFAULT_SUGGESTED_STOPS.end.first().stopId)),
            bounds(row(DEFAULT_SUGGESTED_STOPS.end[1].stopId)),
        )
        show(PreviewDestinationSuggestions)
        val before = snapshot()

        show(PreviewDestinationSelected)
        primary().assertIsDisplayed()

        assertEquals(before, snapshot())
    }

    @Test
    fun `the bar appearing does not move the rows of a list that is already scrolled`() {
        show(PreviewDestinationSuggestions)
        scrollBy(SCROLLED_PX)
        val before = drawnRows()
        assertTrue(before.isNotEmpty(), "no row is drawn, so nothing is being compared")

        show(PreviewDestinationSelected)
        primary().assertIsDisplayed()

        assertEquals(before, drawnRows().filterKeys { it in before })
    }

    @Test
    fun `the field has the page padding at the start and at the end`() {
        show(PreviewDestinationSuggestions)

        val field = bounds(composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_STOP_FIELD))

        assertEquals(dim.pageHorizontalPadding, field.left, "the start inset")
        assertEquals(dim.pageHorizontalPadding, window().width - field.right, "the end inset")
    }

    @Test
    fun `the field keeps the same inset at both ends when it has the focus and a long search typed in`() {
        show(PreviewDestinationSuggestions)
        val field = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_STOP_FIELD)

        field.performClick()
        field.performTextInput("Chatswood Station, a long search that fills the whole field and runs past it")
        composeRule.waitForIdle()

        val bounds = bounds(field)
        assertEquals(dim.pageHorizontalPadding, bounds.left, "the start inset")
        assertEquals(dim.pageHorizontalPadding, window().width - bounds.right, "the end inset")
    }

    @Test
    fun `a tap on a row that is under where the bar would be reaches the row while the bar is hidden`() {
        show(PreviewDestinationSuggestions)
        scrollToEnd()

        row(lastEndId).performClick()

        assertEquals(
            listOf(lastEndId),
            events.filterIsInstance<IntroUiEvent.SelectStop>().map { it.stop.stopId },
        )
    }

    @Test
    fun `the theme badges and the headline do not move when the bar appears, and the badges scroll clear of it`() {
        fun snapshot() = listOf(bounds(heading())) + introThemeChoices().map { bounds(composeRule.onNodeWithTag(it.tag)) }
        show(IntroState(step = IntroStep.THEME))
        val before = snapshot()

        show(themeSelected)
        primary().assertIsDisplayed()
        val after = snapshot()
        // The badge that was picked sits lifted by its own 4 dp, as it always has. Nothing else moves.
        assertEquals(before[0], after[0], "the headline moved")
        before.zip(after).drop(1).forEachIndexed { i, (was, now) ->
            val picked = introThemeChoices()[i].style.id == KrailThemeStyle.Train.id
            if (picked) {
                assertEquals(was.top.value, now.top.value, 4f + ROUNDING.value, "the picked badge")
            } else {
                assertEquals(was, now, "${introThemeChoices()[i].name} moved")
            }
        }

        scrollToEnd()
        val barTop = bounds(primary()).top - dim.spacingL
        val lowest = introThemeChoices().maxOf { bounds(composeRule.onNodeWithTag(it.tag)).bottom }
        assertTrue(lowest <= barTop + ROUNDING, "the lowest badge ends at $lowest, under the bar at $barTop")
    }

    @Test
    fun `every screen keeps its headline at the top, its bar at the bottom, and both whole inside the window`() {
        val states = listOf(
            ON_PARK_RIDE,
            PreviewDestinationSelected,
            PreviewOriginCurated.copy(selectedStop = DEFAULT_SUGGESTED_STOPS.start.first().let { IntroStop(it.stopId, it.name) }),
            IntroState(
                step = IntroStep.CONFIRM,
                startStopId = "200060",
                startName = "Central Station",
                endStopId = "200080",
                endName = "Wynyard Station",
            ),
            themeSelected,
        )
        states.forEach { state ->
            show(state)

            val title = bounds(heading())
            val bar = bounds(primary())
            assertTrue(title.top >= rhythm.top - ROUNDING, "${state.step}: the headline starts at ${title.top}")
            assertTrue(title.left >= 0.dp && title.right <= window().width, "${state.step}: the headline is outside the window: $title")
            assertTrue(bar.left >= 0.dp && bar.right <= window().width, "${state.step}: the bar is outside the window: $bar")
            assertTrue(title.bottom <= bar.top, "${state.step}: the headline ends at ${title.bottom}, under the bar at ${bar.top}")
            assertTrue(window().bottom - barBottom() <= dim.spacingL + ROUNDING, "${state.step}: the bar is not at the bottom")
        }
    }

    private companion object {
        val ROUNDING = 1.dp
        const val SCROLLED_PX = 150f
        const val END_OF_SCROLL = 1_000_000f
    }
}

/** A 411 dp phone at 100 percent text. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h560dp-xxhdpi", manifest = Config.NONE)
class IntroBarOverlayPhoneTest : IntroBarOverlayTestBase(windowWidth = 411.dp, fontScale = 1f)

/** A 411 dp phone at 150 percent text. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h560dp-xxhdpi", manifest = Config.NONE)
class IntroBarOverlayPhoneLargeTextTest : IntroBarOverlayTestBase(windowWidth = 411.dp, fontScale = 1.5f)

/** The narrowest window, 320 dp, at 200 percent text. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w320dp-h560dp-xxhdpi", manifest = Config.NONE)
class IntroBarOverlayNarrowTest : IntroBarOverlayTestBase(windowWidth = 320.dp, fontScale = 2f)
