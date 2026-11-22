package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.isAppInDarkMode
import xyz.ksharma.krail.taj.themeSolidBackgroundColor
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The ground the wash grows into, `introGroundColour`, is the ground the screen settles on, which is taj's
 * `themeSolidBackgroundColor()`. The pure function exists so the wash can be made for a theme that is not the app's yet, and this
 * holds the two equal for the four themes the Intro offers in the scheme the test runs in.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel6, manifest = Config.NONE)
class IntroThemeGroundTest {

    @get:Rule
    val composeRule = createComposeRule()

    // A colour keeps 8 bits a channel and taj's tint is a colour with an alpha, so the two can differ by one step of 8 bits.
    private val tolerance = 0.006f

    private fun assertSame(expected: Color, actual: Color, what: String) {
        assertEquals(expected.red, actual.red, tolerance, "$what red")
        assertEquals(expected.green, actual.green, tolerance, "$what green")
        assertEquals(expected.blue, actual.blue, tolerance, "$what blue")
        assertEquals(1f, actual.alpha, "$what is opaque")
    }

    @Test
    fun `the ground of the wash is the ground the app gives the theme, for each of the four themes`() {
        val seen = mutableMapOf<String, Pair<Color, Color>>()
        composeRule.setContent {
            introThemeChoices().forEach { choice ->
                PreviewTheme(themeStyle = choice.style) {
                    val app = themeSolidBackgroundColor()
                    val mine = introGroundColour(choice.style.hexColorCode.hexToComposeColor(), KrailTheme.colors.surface, isAppInDarkMode())
                    SideEffect { seen[choice.name] = app to mine }
                }
            }
        }
        composeRule.waitForIdle()

        assertEquals(4, seen.size)
        seen.forEach { (name, colours) -> assertSame(colours.first, colours.second, name) }
    }

    @Test
    fun `a tinted ground is between the surface and the theme colour and is not the surface`() {
        val surface = Color.White
        val theme = Color(0xFF00B5EF)

        val light = introGroundColour(theme, surface, dark = false)
        val dark = introGroundColour(theme, surface, dark = true)

        assertTrue(light.red in theme.red..surface.red && light.green in theme.green..surface.green, "$light")
        assertTrue(dark.red < light.red, "the dark tint is the stronger: $dark against $light")
        assertEquals(1f, light.alpha)
        assertTrue(light != surface, "a chosen theme tints the ground")
    }
}
