package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The spacing of the headline, spacing V2 of the approved mock: each line is 1.12 of its own font size tall, there are 2 dp between
 * two lines, and the hero line, only that one, has 6 dp more above it and below it. Read for real from the drawn text, for all five
 * screens at the three frames of the mock, and held against the arithmetic of the plan. The old spacing was a line of 1.286 of the
 * size and 6 dp between lines, with no room round the hero.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroHeadlineSpacingTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class Screen(val name: String, val lines: List<String>, val hero: Int, val spoken: String)

    private val screens = listOf(
        Screen("Park and Ride", PARK_RIDE_HEADLINE, PARK_RIDE_HERO_INDEX, PARK_RIDE_HEADING),
        Screen("Destination", DESTINATION_HEADLINE, STOP_HERO_INDEX, DESTINATION_HEADING),
        Screen("Origin", ORIGIN_HEADLINE, STOP_HERO_INDEX, ORIGIN_HEADING),
        Screen("Confirm", CONFIRM_HEADLINE, CONFIRM_HERO_INDEX, CONFIRM_HEADING),
        Screen("Theme", PICK_THEME_HEADLINE, PICK_THEME_HERO_INDEX, PICK_THEME_HEADING),
    )

    private val current = mutableStateOf(screens[0])
    private var density = 1f

    private fun start(room: Int, fontScale: Float) {
        RuntimeEnvironment.setFontScale(fontScale)
        composeRule.setContent {
            PreviewTheme {
                density = LocalDensity.current.density
                CompositionLocalProvider(LocalHeadlineRoom provides room.dp) {
                    Box(modifier = Modifier.width(room.dp)) {
                        val screen = current.value
                        IntroHeadline(
                            lines = screen.lines,
                            emphasisIndex = screen.hero,
                            emoji = "",
                            spoken = screen.spoken,
                            heroLayout = true,
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private class Row(val bounds: Rect, val layout: TextLayoutResult)

    // Every drawn row, top to bottom, with where its text is in the window and how it was laid out. The text measures to the height
    // its font has by nature and sits in the middle of the box the headline gives it, so a row's box is found from its middle.
    private fun rows(): List<Row> {
        val nodes = composeRule.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
            useUnmergedTree = true,
        ).fetchSemanticsNodes()
        return nodes.map { node ->
            val results = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
            Row(node.boundsInRoot, results.single())
        }.sortedBy { it.bounds.top }
    }

    // In the density the row was drawn in, which is how an Android phone turns sp to dp, not quite linearly above 100 percent.
    private fun Row.sizeDp(): Float = with(layout.layoutInput.density) { layout.layoutInput.style.fontSize.toDp().value }

    private fun Row.boxDp(): Float = with(layout.layoutInput.density) { layout.layoutInput.style.lineHeight.toDp().value }

    private fun Row.middleDp(density: Float): Float = (bounds.top + bounds.bottom) / 2f / density

    private fun headlineHeightDp(): Float {
        val heading = composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).fetchSemanticsNodes().single()
        return heading.boundsInRoot.height / density
    }

    private fun assertSpacing(screen: Screen, frame: String) {
        val rows = rows()
        val largest = rows.maxOf { it.sizeDp() }
        rows.zipWithNext().forEachIndexed { _, (above, below) ->
            // The hero is the largest, and its room is round the whole of it, so between two words of a stack there is none.
            val heroAbove = abs(above.sizeDp() - largest) < 0.01f
            val heroBelow = abs(below.sizeDp() - largest) < 0.01f
            val gap = if (heroAbove != heroBelow) 2f + 6f else 2f
            // From the middle of one box to the middle of the next: half of each box and the room between them.
            val expected = above.boxDp() / 2 + gap + below.boxDp() / 2
            assertEquals(
                expected,
                below.middleDp(density) - above.middleDp(density),
                PX_DP,
                "${screen.name}, $frame: \"${above.layout.layoutInput.text.text}\" to \"${below.layout.layoutInput.text.text}\"",
            )
        }
    }

    private fun assertHeight(screen: Screen, frame: String) {
        val rows = rows()
        val hero = rows.maxOf { it.sizeDp() }
        // Every box at 1.12 of its size, 2 dp between the units, 6 dp above and below the hero, and the stacked words' 2 dp between.
        val expected = rows.sumOf { it.boxDp().toDouble() }.toFloat() + (rows.size - 1) * 2f + 12f
        assertTrue(rows.all { abs(it.boxDp() - HEADLINE_LINE_HEIGHT_FACTOR * it.sizeDp()) < 0.01f }, "a box is 1.12 of its size")
        assertEquals(expected, headlineHeightDp(), PX_DP * rows.size, "${screen.name}, $frame: the whole headline")
        assertTrue(rows.any { abs(it.sizeDp() - hero) < 0.01f })
    }

    private fun check(room: Int, fontScale: Float, frame: String) {
        start(room, fontScale)
        screens.forEach { screen ->
            current.value = screen
            composeRule.waitForIdle()
            assertSpacing(screen, frame)
            assertHeight(screen, frame)
        }
    }

    @Test
    fun `at 411 dp and 100 percent every line is 1_12 of its size, 2 dp from the next, and the hero has 6 dp more above and below`() {
        check(room = 379, fontScale = 1.0f, frame = "411 dp, 100 percent")
    }

    @Test
    fun `at 411 dp and 150 percent the spacing is the same`() {
        check(room = 379, fontScale = 1.5f, frame = "411 dp, 150 percent")
    }

    @Test
    fun `at 320 dp and 200 percent the spacing is the same, with the stacked Theme hero 2 dp between its words`() {
        check(room = 288, fontScale = 2.0f, frame = "320 dp, 200 percent")
    }

    @Test
    fun `Park and Ride at 411 dp and 100 percent is 183 dp tall, 24 dp shorter than the old spacing, the hero room included`() {
        start(room = 379, fontScale = 1.0f)
        val rows = rows()

        val total = headlineHeightDp()
        assertEquals(183.3f, total, 1.5f)
        // Old: a line of 61.2 sp at 47.6, so 1.286 of its size, 6 dp between lines, no room round the hero.
        val old = rows.sumOf { (it.sizeDp() * OLD_LINE_HEIGHT).toDouble() }.toFloat() + 3 * OLD_GAP
        assertEquals(207.7f, old, 0.8f)
        assertEquals(24.4f, old - total, 1.5f)
    }

    @Test
    fun `a screen reader still hears one heading`() {
        start(room = 379, fontScale = 1.0f)

        screens.forEach { screen ->
            current.value = screen
            composeRule.waitForIdle()
            val headings = composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            assertEquals(1, headings.fetchSemanticsNodes().size, screen.name)
        }
    }

    @Test
    fun `the tokens are 2 dp between lines and 6 dp round the hero`() {
        composeRule.setContent {
            PreviewTheme {
                assertEquals(2.dp, headlineLineGap())
                assertEquals(6.dp, headlineHeroPad())
                assertEquals(KrailTheme.dimensions.spacingXXS, headlineLineGap())
                assertTrue(headlineHeroPad() == KrailTheme.dimensions.spacingS)
            }
        }
        composeRule.waitForIdle()
    }

    private companion object {
        // A pixel of rounding in dp at the test device's density of 2.625.
        const val PX_DP = 0.5f
        const val OLD_LINE_HEIGHT = 61.2f / 47.6f
        const val OLD_GAP = 6f
    }
}
