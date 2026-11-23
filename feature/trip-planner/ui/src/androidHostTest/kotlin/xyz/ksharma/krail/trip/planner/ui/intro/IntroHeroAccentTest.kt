package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.ThemeController
import xyz.ksharma.krail.taj.theme.ThemeMode
import xyz.ksharma.krail.taj.theme.contrastRatio
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The colours the five headlines are drawn in, read for real from their text layouts in light and in dark mode: the hero line,
 * with its question mark when it has one, is the screen's accent, and every other line, a whisper whose question mark stays with
 * it, is the page's ink. The accent is at least 3 to 1 on the page the headline is drawn on. A screen reader hears one heading and
 * no colour changes what is said.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroHeroAccentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class Screen(val step: IntroStep, val lines: List<String>, val hero: Int, val spoken: String)

    private val screens = listOf(
        Screen(IntroStep.PARK_RIDE, PARK_RIDE_HEADLINE, PARK_RIDE_HERO_INDEX, PARK_RIDE_HEADING),
        Screen(IntroStep.DESTINATION, DESTINATION_HEADLINE, STOP_HERO_INDEX, DESTINATION_HEADING),
        Screen(IntroStep.ORIGIN, ORIGIN_HEADLINE, STOP_HERO_INDEX, ORIGIN_HEADING),
        Screen(IntroStep.CONFIRM, CONFIRM_HEADLINE, CONFIRM_HERO_INDEX, CONFIRM_HEADING),
        Screen(IntroStep.THEME, PICK_THEME_HEADLINE, PICK_THEME_HERO_INDEX, PICK_THEME_HEADING),
    )

    private val current = mutableStateOf(screens[0])
    private var accentSeen = Color.Unspecified
    private var inkSeen = Color.Unspecified
    private var pageSeen = Color.Unspecified

    private fun start(mode: ThemeMode) {
        composeRule.setContent {
            KrailTheme(themeController = ThemeController(currentMode = mode, setThemeMode = {})) {
                CompositionLocalProvider(LocalHeadlineRoom provides ROOM.dp) {
                    Box(modifier = Modifier.width(ROOM.dp)) {
                        val screen = current.value
                        accentSeen = introAccentColor(screen.step)
                        inkSeen = KrailTheme.colors.onSurface
                        pageSeen = KrailTheme.colors.surface
                        IntroHeadline(
                            lines = screen.lines,
                            emphasisIndex = screen.hero,
                            emoji = "",
                            spoken = screen.spoken,
                            heroLayout = true,
                            accent = accentSeen,
                        )
                    }
                }
            }
        }
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

    private fun assertColours(dark: Boolean) {
        screens.forEach { screen ->
            current.value = screen
            composeRule.waitForIdle()
            val expectedAccent = introAccentInk(introAccentTheme(screen.step), pageSeen, dark)
            assertEquals(expectedAccent, accentSeen, "${screen.step} accent, dark $dark")
            assertTrue(
                accentSeen.contrastRatio(pageSeen) >= 3.0f,
                "${screen.step} accent is under 3 to 1 on the page, dark $dark",
            )
            drawn().forEachIndexed { index, layout ->
                val text = layout.layoutInput.text.text
                val colour = layout.layoutInput.style.color
                if (index == screen.hero) {
                    assertEquals(
                        expectedAccent,
                        colour,
                        "${screen.step}: the hero \"$text\" is the accent, with its question mark",
                    )
                } else {
                    assertEquals(
                        inkSeen,
                        colour,
                        "${screen.step}: the whisper \"$text\" is the ink, with its question mark",
                    )
                }
            }
        }
    }

    @Test
    fun `in light mode the hero of every screen is its accent and every whisper is the ink`() {
        start(ThemeMode.LIGHT)

        assertColours(dark = false)
    }

    @Test
    fun `in dark mode the hero of every screen is its accent and every whisper is the ink`() {
        start(ThemeMode.DARK)

        assertColours(dark = true)
    }

    @Test
    fun `the question mark of a hero takes the hero's colour and one that ends a whisper stays the ink`() {
        start(ThemeMode.LIGHT)

        current.value = screens[1]
        composeRule.waitForIdle()
        assertEquals(accentSeen, drawn().last().layoutInput.style.color, "GOING? is all the accent")
        current.value = screens[3]
        composeRule.waitForIdle()
        assertEquals(inkSeen, drawn().last().layoutInput.style.color, "THIS TRIP? is all the ink")
        assertEquals(listOf("SAVE", "THIS TRIP?"), drawn().map { it.layoutInput.text.text })
    }

    @Test
    fun `a screen reader hears one heading with the whole question whatever the colours are`() {
        start(ThemeMode.LIGHT)

        screens.forEach { screen ->
            current.value = screen
            composeRule.waitForIdle()
            val headings = composeRule.onAllNodes(
                SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.Heading),
            )
            assertEquals(1, headings.fetchSemanticsNodes().size)
            assertEquals(
                listOf(screen.spoken),
                headings[0].fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].map { it.text },
            )
        }
    }

    private companion object {
        const val ROOM = 379
    }
}
