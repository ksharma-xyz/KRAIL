package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.ThemeController
import xyz.ksharma.krail.taj.theme.ThemeMode
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Everything that floats on the map is one fixed dark container with fixed light content, the same in light and
 * dark app modes, because the map is always light. The pair is taj's `stopLabelSurface` and `onStopLabelSurface`,
 * and every colour [FloatingDark] hands to a taj component's `colors` parameter is built from it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class FloatingDarkTest {

    @get:Rule
    val composeRule = createComposeRule()

    private data class Read(
        val container: Color,
        val content: Color,
        val soft: Color,
        val buttonFill: Color,
        val buttonInk: Color,
        val outlinedFill: Color,
        val outlinedInk: Color,
        val fieldFill: Color,
        val fieldInk: Color,
        val fieldPlaceholder: Color,
    )

    private var appMode by mutableStateOf(ThemeMode.LIGHT)
    private lateinit var out: Read

    @Before
    fun setContent() {
        composeRule.setContent {
            KrailTheme(themeController = ThemeController(currentMode = appMode, setThemeMode = {})) {
                val button = FloatingDark.buttonColors()
                val outlined = FloatingDark.outlinedButtonColors()
                val field = FloatingDark.fieldColors()
                out = Read(
                    container = FloatingDark.container,
                    content = FloatingDark.content,
                    soft = FloatingDark.softContent,
                    buttonFill = button.containerColor,
                    buttonInk = button.contentColor,
                    outlinedFill = outlined.containerColor,
                    outlinedInk = outlined.contentColor,
                    fieldFill = field.containerColor,
                    fieldInk = field.contentColor,
                    fieldPlaceholder = field.placeholderColor,
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun read(mode: ThemeMode): Read {
        appMode = mode
        composeRule.waitForIdle()
        return out
    }

    @Test
    fun `the container is dark and the content is light`() {
        val light = read(ThemeMode.LIGHT)

        assertTrue(light.container.luminance() < DARK_BELOW, "The container ${light.container} is dark.")
        assertTrue(light.content.luminance() > LIGHT_ABOVE, "The content ${light.content} is light.")
    }

    @Test
    fun `the pair is the same in light and dark app modes`() {
        val light = read(ThemeMode.LIGHT)
        val dark = read(ThemeMode.DARK)

        assertEquals(light.container, dark.container)
        assertEquals(light.content, dark.content)
        assertEquals(light.soft, dark.soft)
    }

    @Test
    fun `the colours handed to taj's buttons and field are the pair and nothing else`() {
        for (mode in ThemeMode.entries.filter { it != ThemeMode.SYSTEM }) {
            val read = read(mode)

            assertEquals(read.container, read.buttonFill, "button fill in $mode")
            assertEquals(read.content, read.buttonInk, "button ink in $mode")
            assertEquals(Color.Transparent, read.outlinedFill, "outlined fill in $mode")
            assertEquals(read.content, read.outlinedInk, "outlined ink in $mode")
            assertEquals(read.container, read.fieldFill, "field fill in $mode")
            assertEquals(read.content, read.fieldInk, "field ink in $mode")
            assertEquals(read.soft, read.fieldPlaceholder, "field placeholder in $mode")
            assertNotEquals(read.content, read.soft, "The quieter light is a step down from the content")
        }
    }

    private companion object {
        const val DARK_BELOW = 0.2f
        const val LIGHT_ABOVE = 0.6f
    }
}
