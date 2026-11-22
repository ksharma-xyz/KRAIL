package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
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
import xyz.ksharma.krail.taj.theme.KrailDimensions
import xyz.ksharma.krail.taj.theme.PreviewTheme
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The mode icon's sizes. The four that were there are as they were, including how they grow with the text size, and the
 * extra large one is twice the large one, with a bigger letter that is white like every other. The extra large one is a fixed
 * 64 dp at every text size, its letter is a fixed size in the heaviest weight, and its ring is twice as thick.
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
    private val fontScale = mutableStateOf(1f)
    private var contentSet = false

    private fun show(newSize: TransportModeIconSize, newMode: TransportMode, scale: Float = 1f) {
        size.value = newSize
        mode.value = newMode
        fontScale.value = scale
        if (!contentSet) {
            contentSet = true
            composeRule.setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = fontScale.value)) {
                    PreviewTheme {
                        TransportModeIcon(
                            transportMode = mode.value,
                            size = size.value,
                            modifier = Modifier.testTag("icon"),
                        )
                    }
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

    @Test
    fun `the extra large circle is a plain 64 dp and its letter a fixed size, at every text size`() {
        listOf(0.85f, 1f, 1.3f, 1.5f, 2f).forEach { scale ->
            show(TransportModeIconSize.XLarge, TransportMode.Metro, scale)

            val bounds = composeRule.onNodeWithTag("icon").getUnclippedBoundsInRoot()
            assertEquals(64.dp, bounds.width, "width at $scale")
            assertEquals(64.dp, bounds.height, "height at $scale")
            val layout = letter("M").layoutInput
            assertEquals(1f, layout.density.fontScale, "the letter follows the text size at $scale")
            assertEquals(28f, layout.style.fontSize.value, 0.01f, "the letter's size at $scale")
        }
    }

    @Test
    fun `the extra large letter is the heaviest weight and white, and fits whole inside the circle at 200 percent`() {
        show(TransportModeIconSize.XLarge, TransportMode.Ferry, scale = 2f)

        val result = letter("F")
        assertEquals(FontWeight.Black, result.layoutInput.style.fontWeight)
        assertEquals(Color.White, result.layoutInput.style.color)
        val circlePx = with(composeRule.density) { 64.dp.toPx() }
        assertTrue(result.size.width <= circlePx && result.size.height <= circlePx, "the letter is ${result.size} px in a $circlePx px circle")
    }

    @Test
    fun `the extra large ring is twice the ring of every other size, which is the medium stroke as it was`() {
        val dim = KrailDimensions()

        TransportModeIconSize.entries.filter { it != TransportModeIconSize.XLarge }.forEach {
            assertEquals(dim.strokeMedium, it.ringWidth(dim), "$it")
        }
        assertEquals(dim.strokeMedium * 2, TransportModeIconSize.XLarge.ringWidth(dim))
        assertEquals(6.dp, TransportModeIconSize.XLarge.ringWidth(dim))
    }

    @Test
    fun `the small and large sizes are unchanged, they grow with the text and keep the small title in its own weight`() {
        // Adaptive, as it was: the size times the text scale, held back to 0.8 of that above 1.5 times.
        listOf(
            TransportModeIconSize.XSmall to 20.dp,
            TransportModeIconSize.Small to 22.dp,
            TransportModeIconSize.Medium to 28.dp,
            TransportModeIconSize.Large to 32.dp,
        ).forEach { (iconSize, base) ->
            show(iconSize, TransportMode.Bus, scale = 1f)
            assertEquals(base.value, composeRule.onNodeWithTag("icon").getUnclippedBoundsInRoot().width.value, PIXEL_ROUNDING, "$iconSize at 1.0")

            show(iconSize, TransportMode.Bus, scale = 2f)
            assertEquals(base.value * 2f * 0.8f, composeRule.onNodeWithTag("icon").getUnclippedBoundsInRoot().width.value, PIXEL_ROUNDING, "$iconSize at 2.0")
            val style = letter("B").layoutInput
            assertEquals(2f, style.density.fontScale, "$iconSize letter follows the text size")
            assertEquals(14f, style.style.fontSize.value, 0.01f, "$iconSize letter size")
            assertTrue(style.style.fontWeight != FontWeight.Black, "$iconSize letter weight")
        }
    }

    private companion object {
        // A dp is not a whole number of pixels, so a size is within half a dp of what it is meant to be.
        const val PIXEL_ROUNDING = 0.5f
    }
}
