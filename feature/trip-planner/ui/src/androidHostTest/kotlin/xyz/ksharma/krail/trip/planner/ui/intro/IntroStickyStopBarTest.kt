package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.KrailDimensions
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.md_theme_light_surface
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroSearchPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStop
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The search bar of the destination and origin screens is a sticky header over one list that scrolls as a whole. The
 * emoji and the headline scroll away and the bar stays at the top of the screen, opaque, with the rows sliding under it.
 * Focusing the bar scrolls it to the top once and typing does not move it. Each class is one window and one text size,
 * drawn as a preview is, with no motion.
 */
abstract class IntroStickyStopBarTestBase(private val windowWidth: Dp, private val fontScale: Float) {

    @get:Rule
    val composeRule = createComposeRule()

    private val dim = KrailDimensions()
    private val shown = mutableStateOf(PreviewDestinationSuggestions)
    private var contentSet = false

    // Far more rows than any window holds, so the list always has room to scroll the bar to the top.
    private val manyRows = (1..MANY).map { IntroStop("9000$it", "Test Stop $it") }
    private val manyResults = IntroState(
        step = IntroStep.DESTINATION,
        query = "test",
        search = IntroSearchPhase.Results(manyRows.toImmutableList()),
    )

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
                            readiness = PreviewReady,
                            onEvent = {},
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun window(): DpRect = composeRule.onRoot().getUnclippedBoundsInRoot()

    private fun field() = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_STOP_FIELD)

    private fun fieldBounds() = field().getUnclippedBoundsInRoot()

    private fun headings() = composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))

    // The one list that scrolls: the tallest node with a scroll action, and not the field's own scroll.
    private fun scroller(): SemanticsNodeInteraction {
        val all = composeRule.onAllNodes(hasScrollAction())
        val tallest = all.fetchSemanticsNodes().withIndex().maxBy { it.value.size.height }.index
        return all[tallest]
    }

    private fun listTop(): Dp = scroller().getUnclippedBoundsInRoot().top

    private fun scrollBy(px: Float) {
        scroller().performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, px) }
        composeRule.waitForIdle()
    }

    // To the item that is the far end of the list or the first one, which a scroll of any size would reach.
    private fun scrollToIndex(index: Int) {
        scroller().performScrollToIndex(index)
        composeRule.waitForIdle()
    }

    // Each of the rows that is drawn now, with its bounds. A row off the window is not drawn.
    private fun drawnRows(stopIds: List<String> = manyRows.map { it.stopId }): List<DpRect> = stopIds.mapNotNull { stopId ->
        val nodes = composeRule.onAllNodes(hasTestTag(TripPlannerTestTags.introStop(stopId)))
        if (nodes.fetchSemanticsNodes().isEmpty()) null else nodes[0].getUnclippedBoundsInRoot()
    }

    private fun assertBarAtTop(what: String) {
        assertEquals(listTop().value, fieldBounds().top.value, ROUNDING.value, "$what: the bar is at ${fieldBounds().top}, the list starts at ${listTop()}")
    }

    @Test
    fun `the window is the width the checks assume`() {
        show(manyResults)

        assertEquals(windowWidth, window().width)
    }

    @Test
    fun `the bar starts under the headline and the headline and emoji scroll off, leaving the bar at the top`() {
        show(manyResults)
        headings().assertCountEquals(1)
        assertTrue(fieldBounds().top > listTop() + ROUNDING, "the bar starts at ${fieldBounds().top}, which is not under the headline")

        scrollToIndex(LAST_OF_MANY)

        headings().assertCountEquals(0)
        assertBarAtTop("after a large scroll")
    }

    @Test
    fun `the bar stays at the top through every scroll once the headline is gone, and keeps its page padding at both ends`() {
        show(manyResults)

        val scrolls = listOf<() -> Unit>(
            { scrollToIndex(LAST_OF_MANY) },
            { scrollBy(-SMALL_SCROLL) },
            { scrollBy(SMALL_SCROLL) },
            { scrollToIndex(LAST_OF_MANY) },
        )
        scrolls.forEachIndexed { index, scroll ->
            scroll()
            if (headings().fetchSemanticsNodes().isEmpty()) {
                assertBarAtTop("after scroll $index")
                assertEquals(dim.pageHorizontalPadding, fieldBounds().left, "the start inset")
                assertEquals(dim.pageHorizontalPadding, window().width - fieldBounds().right, "the end inset")
            }
        }
    }

    @Test
    fun `rows pass under the bar and are never drawn over it, the room under the bar is the window's own ground`() {
        show(manyResults)
        scrollToIndex(LAST_OF_MANY)
        val stripTop = fieldBounds().bottom
        val stripBottom = stripTop + introRhythm(dim).fieldToList
        var passedUnder = 0
        var checked = 0

        repeat(STEPS) {
            scrollBy(-STEP_PX)
            val under = drawnRows().filter { it.top < fieldBounds().bottom && it.bottom > fieldBounds().top }
            if (under.isNotEmpty()) passedUnder++
            // Any row that reaches into the strip under the field would show through, if the bar were not opaque.
            val reachesStrip = drawnRows().any { it.top < stripBottom && it.bottom > stripTop }
            if (reachesStrip) {
                checked++
                assertStripIsGround(stripTop, stripBottom)
            }
        }

        assertTrue(passedUnder > 0, "no row was ever partly under the bar, so nothing is being compared")
        assertTrue(checked > 0, "no row ever reached the strip under the bar, so its colour was not read")
    }

    // The window as it is drawn, read off the compose view itself, which is the view the rows and the bar are drawn in.
    private fun drawnWindow(): Bitmap {
        val view = composeRule.onRoot().fetchSemanticsNode().root as View
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return bitmap
    }

    private fun assertStripIsGround(top: Dp, bottom: Dp) {
        val pixels = drawnWindow()
        val density = composeRule.density
        val from = with(density) { top.roundToPx() }
        val to = with(density) { bottom.roundToPx() }
        val ground = md_theme_light_surface.toArgb()
        for (y in from until to) {
            for (x in 0 until pixels.width) {
                assertEquals(ground, pixels.getPixel(x, y), "a row shows through the bar at $x, $y")
            }
        }
    }

    @Test
    fun `focusing the bar scrolls it to the top, with a list short enough that it could not scroll before`() {
        show(PreviewDestinationSuggestions)
        assertTrue(fieldBounds().top > listTop() + ROUNDING, "the bar is already at the top")

        field().performClick()
        composeRule.waitForIdle()

        headings().assertCountEquals(0)
        assertBarAtTop("after focusing")
    }

    @Test
    fun `typing does not move the bar, with the same rows or with far fewer`() {
        show(PreviewDestinationSuggestions)
        field().performClick()
        composeRule.waitForIdle()
        assertBarAtTop("after focusing")

        field().performTextInput("chats")
        composeRule.waitForIdle()
        assertBarAtTop("after typing")

        // The search then finds two rows, where the list had five, which would have brought the headline back.
        show(PreviewDestinationTyped)
        assertBarAtTop("after the rows shrank")
        headings().assertCountEquals(0)
    }

    @Test
    fun `focusing scrolls once, so a rider who scrolls back to the headline is not scrolled away again by typing`() {
        show(PreviewDestinationSuggestions)
        field().performClick()
        composeRule.waitForIdle()
        scrollToIndex(0)
        headings().assertCountEquals(1)
        val before = fieldBounds()

        field().performTextInput("c")
        composeRule.waitForIdle()

        headings().assertCountEquals(1)
        assertEquals(before, fieldBounds(), "typing moved the bar")
    }

    @Test
    fun `without the focus the list keeps no extra room at its end`() {
        show(PreviewDestinationSuggestions)

        scrollToIndex(LAST_OF_SUGGESTIONS)

        // The list is no taller than its content, so the last row ends at the bottom of the window and no room is left past it.
        val bottom = drawnRows(DEFAULT_SUGGESTED_STOPS.end.map { it.stopId }).maxOf { it.bottom }
        assertTrue(bottom <= window().bottom + ROUNDING, "the last row ends at $bottom, past the window")
        assertTrue(window().bottom - bottom < dim.spacingXXXXL + ROUNDING, "the last row ends $bottom, far above the window's bottom")
    }

    private companion object {
        const val MANY = 30
        // The headline, the bar, the rows and the end room: the last item of each list.
        const val LAST_OF_MANY = MANY + 2
        const val LAST_OF_SUGGESTIONS = 5 + 2
        const val SMALL_SCROLL = 150f
        const val STEP_PX = 12f
        const val STEPS = 40
        val ROUNDING = 1.dp
    }
}

/** A 411 dp phone at 100 percent text. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h560dp-xxhdpi", manifest = Config.NONE)
class IntroStickyStopBarPhoneTest : IntroStickyStopBarTestBase(windowWidth = 411.dp, fontScale = 1f)

/** A 411 dp phone at 150 percent text. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h560dp-xxhdpi", manifest = Config.NONE)
class IntroStickyStopBarPhoneLargeTextTest : IntroStickyStopBarTestBase(windowWidth = 411.dp, fontScale = 1.5f)

/** The narrowest window, 320 dp, at 200 percent text. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w320dp-h560dp-xxhdpi", manifest = Config.NONE)
class IntroStickyStopBarNarrowTest : IntroStickyStopBarTestBase(windowWidth = 320.dp, fontScale = 2f)
