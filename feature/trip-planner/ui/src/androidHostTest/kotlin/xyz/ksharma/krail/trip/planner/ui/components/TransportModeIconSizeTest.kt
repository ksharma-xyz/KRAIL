package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
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
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import kotlin.test.assertEquals

/**
 * The mode icon's sizes. The four that were there are as they were, and the extra large one is twice the large one, with a
 * bigger letter that is white like every other.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class TransportModeIconSizeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val size = mutableStateOf(TransportModeIconSize.Large)
    private val mode = mutableStateOf<TransportMode>(TransportMode.Train)
    private var contentSet = false

    private fun show(newSize: TransportModeIconSize, newMode: TransportMode) {
        size.value = newSize
        mode.value = newMode
        if (!contentSet) {
            contentSet = true
            composeRule.setContent {
                PreviewTheme {
                    TransportModeIcon(transportMode = mode.value, size = size.value, modifier = Modifier.testTag("icon"))
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun letter(letter: String): TextLayoutResult {
        val node = composeRule.onNode(hasText(letter), useUnmergedTree = true)
        val results = mutableListOf<TextLayoutResult>()
        node.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.single()
    }

    @Test
    fun `the sizes that were there are untouched and the extra large one is twice the large one`() {
        assertEquals(
            listOf(20.dp, 22.dp, 28.dp, 32.dp, 64.dp),
            TransportModeIconSize.entries.map { it.dpSize },
        )
        assertEquals(TransportModeIconSize.Large.dpSize * 2, TransportModeIconSize.XLarge.dpSize)
    }

    @Test
    fun `the large letter is still the small title in white`() {
        show(TransportModeIconSize.Large, TransportMode.Train)

        val style = letter("T").layoutInput.style
        assertEquals(14f, style.fontSize.value, 0.01f)
        assertEquals(Color.White, style.color)
    }

    @Test
    fun `at the extra large size every mode has a white letter in the larger style and a 64 dp circle`() {
        listOf(
            TransportMode.Train to "T",
            TransportMode.Metro to "M",
            TransportMode.Bus to "B",
            TransportMode.Ferry to "F",
        ).forEach { (transportMode, letter) ->
            show(TransportModeIconSize.XLarge, transportMode)

            val style = letter(letter).layoutInput.style
            assertEquals(Color.White, style.color, "$transportMode")
            assertEquals(28f, style.fontSize.value, 0.01f, "$transportMode")
            val bounds = composeRule.onNodeWithTag("icon").getUnclippedBoundsInRoot()
            assertEquals(64.dp, bounds.width, "$transportMode")
            assertEquals(64.dp, bounds.height, "$transportMode")
        }
    }
}
