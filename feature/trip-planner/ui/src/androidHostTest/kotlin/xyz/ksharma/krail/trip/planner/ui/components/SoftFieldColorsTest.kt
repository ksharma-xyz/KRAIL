package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.components.TextFieldColors
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.contrastRatio
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The shared soft pill colours, which the Intro's stop field and Search Stop's pills use, are the same in every theme style:
 * a fill that is a real step from the page, and text and a placeholder that read on it, in light and dark mode. They are
 * built from taj's tokens, so no theme can be left out.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class SoftFieldColorsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var colors: TextFieldColors? = null
    private var page: Color = Color.Unspecified

    private fun read() {
        composeRule.setContent {
            PreviewTheme {
                colors = softFieldColors()
                page = KrailTheme.colors.surface
            }
        }
        composeRule.waitForIdle()
    }

    private fun assertReads() {
        val field = requireNotNull(colors)
        assertNotEquals(page, field.containerColor, "the field is the page surface")
        assertTrue(field.containerColor.contrastRatio(page) >= 1.1f, "the fill is not a visible step from the page")
        assertTrue(field.contentColor.contrastRatio(field.containerColor) >= 4.5f, "the text does not read on the fill")
        assertTrue(
            field.placeholderColor.contrastRatio(field.containerColor) >= 4.5f,
            "the placeholder does not read on the fill",
        )
        assertTrue(
            field.cursorColor.contrastRatio(field.containerColor) >= 4.5f,
            "the cursor does not show on the fill",
        )
    }

    @Test
    fun `in light mode the soft field has a fill and text that reads on it`() {
        read()

        assertReads()
    }

    @Test
    @Config(qualifiers = "+night")
    fun `in dark mode the soft field has a fill and text that reads on it`() {
        read()

        assertReads()
    }
}
