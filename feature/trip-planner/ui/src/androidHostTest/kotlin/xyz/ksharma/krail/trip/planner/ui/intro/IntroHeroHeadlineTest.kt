package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import xyz.ksharma.krail.taj.theme.PreviewTheme
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The five headlines as drawn, hero and whispers, read for real from their text layouts at the three frames of the approved mock:
 * 411 dp at 100 percent text, 411 dp at 150 percent and 320 dp at 200 percent. The room is the window less the 16 dp each side
 * the Intro's frame leaves, so 411 dp is 379 and 320 dp is 288. Every line is on one row, no line is wider than its room, no word
 * is split, the hero is the largest line and never above the ordinary headline size, every whisper is 0.70 of the hero, a screen
 * reader hears one heading of the whole question, and the Theme hero stacks word by word at 320 dp and 200 percent.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroHeroHeadlineTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class Screen(val name: String, val lines: List<String>, val hero: Int, val spoken: String)

    private val screens = listOf(
        Screen("Park and Ride", PARK_RIDE_HEADLINE, PARK_RIDE_HERO_INDEX, "Show parking spots at your nearest station?"),
        Screen("Destination", DESTINATION_HEADLINE, STOP_HERO_INDEX, "Where are you going?"),
        Screen("Origin", ORIGIN_HEADLINE, STOP_HERO_INDEX, "Where are you leaving from?"),
        Screen("Confirm", CONFIRM_HEADLINE, CONFIRM_HERO_INDEX, "Save this trip?"),
        Screen("Theme", PICK_THEME_HEADLINE, PICK_THEME_HERO_INDEX, "What's your favourite transport mode in Sydney?"),
    )

    private val current = mutableStateOf(screens[0])
    private var started = false

    private fun start(room: Int, fontScale: Float) {
        RuntimeEnvironment.setFontScale(fontScale)
        composeRule.setContent {
            PreviewTheme {
                CompositionLocalProvider(LocalHeadlineRoom provides room.dp) {
                    Box(modifier = Modifier.width(room.dp)) {
                        val screen = current.value
                        IntroHeadline(
                            lines = screen.lines,
                            emphasisIndex = screen.hero,
                            emoji = "",
                            spoken = screen.spoken,
                            heroLayout = true,
                            accent = Color.Unspecified,
                        )
                    }
                }
            }
        }
        started = true
        composeRule.waitForIdle()
    }

    private fun show(screen: Screen) {
        check(started)
        current.value = screen
        composeRule.waitForIdle()
    }

    private fun drawn(): List<TextLayoutResult> {
        val nodes = composeRule.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
            useUnmergedTree = true,
        ).fetchSemanticsNodes()
        return nodes.map { node ->
            val results = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
            results.single()
        }
    }

    private fun TextLayoutResult.sizeSp(): Float = layoutInput.style.fontSize.value * layoutInput.density.fontScale

    // The test font is a little different from the mock's Roboto Black, so the sizes are held to a point of the mock's.
    private fun assertDrawn(screen: Screen, room: Int, expected: List<Float>) {
        val layouts = drawn()
        assertEquals(expected.size, layouts.size, "${screen.name} at $room dp: ${layouts.map { it.layoutInput.text.text }}")
        layouts.zip(expected).forEachIndexed { index, (layout, size) ->
            assertTrue(
                kotlin.math.abs(layout.sizeSp() - size) <= SIZE_TOLERANCE_SP,
                "${screen.name} at $room dp: line ${index + 1} is ${layout.sizeSp()} sp, the mock has $size",
            )
        }
    }

    private fun assertWhole(screen: Screen, room: Int) {
        val layouts = drawn()
        layouts.forEach { layout ->
            val text = layout.layoutInput.text.text
            assertEquals(1, layout.lineCount, "\"$text\" wraps in $room dp")
            assertTrue(!layout.didOverflowWidth, "\"$text\" overflows in $room dp")
            assertTrue(layout.size.width <= layout.layoutInput.constraints.maxWidth, "\"$text\" is wider than its room")
            assertTrue(layout.getLineRight(0) <= room * layout.layoutInput.density.density + 1f, "\"$text\" is cut in $room dp")
        }
        assertEquals(
            screen.lines.joinToString(" ") { introHeadlineDisplay(it) },
            layouts.joinToString(" ") { it.layoutInput.text.text },
            "every word of the ${screen.name} headline is drawn, in capitals and in order",
        )
    }

    @Test
    fun `at 411 dp and 100 percent the hero is 47_6 sp and the whispers 33_3 sp, Theme's hero 42_6 sp and its whispers 29_8`() {
        start(room = PHONE_ROOM, fontScale = 1.0f)
        val expected = mapOf(
            screens[0] to listOf(33.3f, 47.6f, 33.3f, 33.3f),
            screens[1] to listOf(33.3f, 47.6f),
            screens[2] to listOf(33.3f, 47.6f),
            screens[3] to listOf(47.6f, 33.3f),
            screens[4] to listOf(29.8f, 42.6f, 29.8f),
        )
        screens.forEach { screen ->
            show(screen)
            assertDrawn(screen, PHONE_ROOM, expected.getValue(screen))
            assertWhole(screen, PHONE_ROOM)
        }
    }

    @Test
    fun `at 411 dp and 150 percent the headline is held at 1_05 and every line is whole`() {
        start(room = PHONE_ROOM, fontScale = 1.5f)
        val normal = 1.05f * 47.6f
        val expected = mapOf(
            screens[0] to listOf(0.7f * 49.3f, 49.3f, 0.7f * 49.3f, 0.7f * 49.3f),
            screens[1] to listOf(0.7f * normal, normal),
            screens[2] to listOf(0.7f * 49.5f, 49.5f),
            screens[3] to listOf(normal, 0.7f * normal),
            screens[4] to listOf(29.8f, 42.6f, 29.8f),
        )
        screens.forEach { screen ->
            show(screen)
            assertDrawn(screen, PHONE_ROOM, expected.getValue(screen))
            assertWhole(screen, PHONE_ROOM)
        }
    }

    @Test
    fun `at 320 dp and 200 percent every line is whole, Park about 0_85 of the normal size, and the Theme hero stacks word by word`() {
        start(room = NARROW_ROOM, fontScale = 2.0f)
        val normal = headlineFontScale(2.0f, NARROW_ROOM.dp) * 47.6f
        val expected = mapOf(
            screens[0] to listOf(0.7f * 37.6f, 37.6f, 0.7f * 37.6f, 0.7f * 37.6f),
            screens[1] to listOf(0.7f * normal, normal),
            screens[2] to listOf(0.7f * 37.8f, 37.8f),
            screens[3] to listOf(normal, 0.7f * normal),
            screens[4] to listOf(0.7f * normal, normal, normal, 0.7f * normal),
        )
        screens.forEach { screen ->
            show(screen)
            assertDrawn(screen, NARROW_ROOM, expected.getValue(screen))
            assertWhole(screen, NARROW_ROOM)
        }
        show(screens[4])
        assertEquals(
            listOf("YOUR FAVOURITE", "TRANSPORT", "MODE", "IN SYDNEY?"),
            drawn().map { it.layoutInput.text.text },
            "the hero stacks word by word, and no word is split",
        )
        show(screens[0])
        assertTrue(drawn()[1].sizeSp() / normal in 0.8f..0.9f, "PARKING SPOTS is about 0.85 of the normal size at 320 dp and 200 percent")
    }

    @Test
    fun `the hero is the largest line, never above the ordinary headline size, and every whisper is 0_70 of it`() {
        start(room = PHONE_ROOM, fontScale = 1.0f)
        screens.forEach { screen ->
            show(screen)
            val layouts = drawn()
            val ordinary = headlineFontScale(1.0f, PHONE_ROOM.dp) * 47.6f
            val hero = layouts[screen.hero].sizeSp()
            assertTrue(hero <= ordinary + 0.01f, "${screen.name}: the hero is $hero sp over the ordinary $ordinary")
            layouts.forEachIndexed { index, layout ->
                assertTrue(layout.sizeSp() <= hero + 0.01f, "${screen.name}: line ${index + 1} is larger than the hero")
                if (index != screen.hero) assertEquals(0.7f * hero, layout.sizeSp(), 0.3f, "${screen.name}: line ${index + 1}")
            }
        }
    }

    @Test
    fun `a screen reader hears the one heading of the whole question, never the drawn lines, in every frame`() {
        start(room = PHONE_ROOM, fontScale = 1.0f)
        screens.forEach { screen ->
            show(screen)
            val headings = composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            assertEquals(1, headings.fetchSemanticsNodes().size, "${screen.name} is one heading")
            assertEquals(listOf(screen.spoken), headings[0].fetchSemanticsNode().config[SemanticsProperties.Text].map { it.text })
        }
    }

    private companion object {
        const val PHONE_ROOM = 379
        const val NARROW_ROOM = 288
        const val SIZE_TOLERANCE_SP = 1.0f
    }
}
