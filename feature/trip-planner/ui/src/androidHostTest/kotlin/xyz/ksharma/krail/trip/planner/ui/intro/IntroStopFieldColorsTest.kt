package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.components.TextFieldColors
import xyz.ksharma.krail.taj.components.TextFieldDefaults
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.contrastRatio
import xyz.ksharma.krail.taj.theme.md_theme_dark_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_dark_onSurface_placeholder
import xyz.ksharma.krail.taj.theme.md_theme_dark_past_departure_row_surface
import xyz.ksharma.krail.taj.theme.md_theme_dark_surface
import xyz.ksharma.krail.taj.theme.md_theme_light_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_light_onSurface_placeholder
import xyz.ksharma.krail.taj.theme.md_theme_light_past_departure_row_surface
import xyz.ksharma.krail.taj.theme.md_theme_light_surface
import xyz.ksharma.krail.trip.planner.ui.components.softFieldColors
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The stop field has a visible fill in both modes. Its colours are the app's shared soft pill colours (`softFieldColors`),
 * from one place (`introStopFieldColors`): a grey fill a touch darker than the page in light mode and a touch lighter in dark
 * mode, with the page's text on it. Taj's default colours were the page surface itself, so the field had no fill on the white
 * Intro page, and the inverted set is the harsh black bar. The text and the placeholder clear 4.5 to 1 on the fill in both
 * modes, and the fill is a real step away from the page.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroStopFieldColorsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var colors: TextFieldColors? = null
    private var shared: TextFieldColors? = null
    private var defaults: TextFieldColors? = null
    private var inverted: TextFieldColors? = null
    private var page: Color = Color.Unspecified
    private var onSurface: Color = Color.Unspecified

    private fun read() {
        composeRule.setContent {
            PreviewTheme {
                colors = introStopFieldColors()
                shared = softFieldColors()
                defaults = TextFieldDefaults.colors()
                inverted = TextFieldDefaults.invertedColors()
                page = KrailTheme.colors.surface
                onSurface = KrailTheme.colors.onSurface
            }
        }
        composeRule.waitForIdle()
    }

    private fun assertContrast(field: TextFieldColors) {
        val text = field.contentColor.contrastRatio(field.containerColor)
        val placeholder = field.placeholderColor.contrastRatio(field.containerColor)
        assertTrue(text >= MIN_TEXT_CONTRAST, "the text is $text to 1 on the field")
        assertTrue(placeholder >= MIN_TEXT_CONTRAST, "the placeholder is $placeholder to 1 on the field")
    }

    private fun assertFillIsAStepFromThePage(field: TextFieldColors) {
        assertNotEquals(page, field.containerColor, "the field is the page surface, so it has no fill")
        val ratio = field.containerColor.contrastRatio(page)
        assertTrue(ratio >= MIN_FILL_STEP, "the fill is only $ratio to 1 against the page, which is not a visible step")
    }

    @Test
    fun `in light mode the field is a soft grey fill with dark text, the shared pill colours and not taj's default or the inverted set`() {
        read()

        val field = requireNotNull(colors)
        assertEquals(md_theme_light_past_departure_row_surface, field.containerColor)
        assertEquals(md_theme_light_onSurface, field.contentColor)
        assertEquals(md_theme_light_onSurface_placeholder, field.placeholderColor)
        assertEquals(md_theme_light_onSurface, field.cursorColor)
        assertEquals(md_theme_light_surface, page)
        assertEquals(shared, field, "the Intro's field is not the shared soft field colours")
        assertNotEquals(defaults, field)
        assertNotEquals(requireNotNull(inverted).containerColor, field.containerColor)
        assertNotEquals(onSurface, field.containerColor, "the field is the dark one of the inverted set")
        assertFillIsAStepFromThePage(field)
        assertContrast(field)
    }

    @Test
    @Config(qualifiers = "+night")
    fun `in dark mode the field is a soft lighter fill with light text, the shared pill colours and not taj's default or the inverted set`() {
        read()

        val field = requireNotNull(colors)
        assertEquals(md_theme_dark_past_departure_row_surface, field.containerColor)
        assertEquals(md_theme_dark_onSurface, field.contentColor)
        assertEquals(md_theme_dark_onSurface_placeholder, field.placeholderColor)
        assertEquals(md_theme_dark_onSurface, field.cursorColor)
        assertEquals(md_theme_dark_surface, page)
        assertEquals(shared, field, "the Intro's field is not the shared soft field colours")
        assertNotEquals(defaults, field)
        assertNotEquals(requireNotNull(inverted).containerColor, field.containerColor)
        assertNotEquals(onSurface, field.containerColor, "the field is the light one of the inverted set")
        assertFillIsAStepFromThePage(field)
        assertContrast(field)
    }

    @Test
    fun `the fill is darker than the page in light mode and lighter than it in dark mode`() {
        assertTrue(
            md_theme_light_past_departure_row_surface.luminance() < md_theme_light_surface.luminance(),
            "the light fill is not a touch darker than the page",
        )
        assertTrue(
            md_theme_dark_past_departure_row_surface.luminance() > md_theme_dark_surface.luminance(),
            "the dark fill is not a touch lighter than the page",
        )
    }

    private companion object {
        const val MIN_TEXT_CONTRAST = 4.5f

        // A fill about 1.15 to 1 from the page is what the soft pills are drawn with, so the step must be at least this.
        const val MIN_FILL_STEP = 1.1f
    }
}
