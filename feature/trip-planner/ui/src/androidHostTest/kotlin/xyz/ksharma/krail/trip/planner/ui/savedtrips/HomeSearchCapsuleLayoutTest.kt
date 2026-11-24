package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.ThemeController
import xyz.ksharma.krail.taj.theme.ThemeMode
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.getVisibleBoundsInRoot
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent

/**
 * The home search capsule on its own, in a window of its own size (density 1, so a dp is a pixel): one line that ends in an
 * ellipsis at any text size, a height that is one text line plus a constant 15 dp above and below and never under 48 dp, the
 * search glyph always and the microphone only when it is available, and a tap that opens Search Stop. The fill and the missing
 * border are read as pixels by `HomeSearchCapsuleLookTest`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE, qualifiers = PHONE)
class HomeSearchCapsuleLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var clicks = 0
    private val aiEvents = mutableListOf<AiSearchInputEvent>()

    // region one line

    @Test
    @Config(qualifiers = NARROW)
    fun `at 320 dp and 200 percent the question is one line that ends in an ellipsis, with the microphone on`() {
        setCapsule(fontScale = 2f, ai = true)

        val layout = questionLayout()
        assertEquals(1, layout.lineCount, "The question is one line at 200 percent text.")
        assertTrue(layout.isLineEllipsized(0), "The question ends in an ellipsis when it does not fit.")
        assertWhole()
    }

    @Test
    @Config(qualifiers = NARROW)
    fun `at 320 dp and 200 percent the question is one line that ends in an ellipsis, with the microphone off`() {
        setCapsule(fontScale = 2f, ai = false)

        val layout = questionLayout()
        assertEquals(1, layout.lineCount, "The question is one line at 200 percent text.")
        assertTrue(layout.isLineEllipsized(0), "The question ends in an ellipsis when it does not fit.")
        assertWhole()
    }

    @Test
    fun `at 100 percent the whole question fits on one line and is not cut`() {
        setCapsule(fontScale = 1f, ai = true)

        val layout = questionLayout()
        assertEquals(1, layout.lineCount)
        assertTrue(!layout.isLineEllipsized(0), "Nothing is cut at 100 percent.")
    }

    /** The capsule, the glyph and the text are whole inside the window, so nothing is clipped to make a line fit. */
    private fun assertWhole() {
        val capsule = bounds(TripPlannerTestTags.SEARCH_ROW_TO)
        val glyph = bounds(TripPlannerTestTags.HOME_SEARCH_GLYPH)
        val text = textBounds()
        assertTrue(capsule.left >= 0.dp && capsule.right <= NARROW_WIDTH, "The capsule $capsule is inside the window.")
        assertTrue(glyph.left >= capsule.left && glyph.right <= text.left, "The glyph $glyph is before the text $text.")
        assertTrue(text.top >= capsule.top && text.bottom <= capsule.bottom, "The text $text is inside the capsule $capsule.")
    }

    // endregion

    // region height

    @Test
    fun `the height is one line of content plus 15 dp above and below at every text size, so it grows with the text`() {
        val fontScale = mutableFloatStateOf(1f)
        setCapsule(fontScaleState = fontScale, ai = true)

        val heights = listOf(1f, 1.3f, 2f).map { scale ->
            fontScale.floatValue = scale
            composeRule.waitForIdle()
            val capsule = bounds(TripPlannerTestTags.SEARCH_ROW_TO)
            // The line is the taller of the text and the glyph beside it: the text outgrows the glyph as the text size grows.
            val line = maxOf(textBounds().height, bounds(TripPlannerTestTags.HOME_SEARCH_GLYPH).height)
            assertNear("padding above and below the line at $scale (capsule ${capsule.height}, line $line)", 30.dp, capsule.height - line)
            capsule.height
        }

        assertTrue(heights[0] < heights[1] && heights[1] < heights[2], "The capsule grows with the text: $heights")
    }

    @Test
    fun `the height is never under 48 dp, however small the text`() {
        setCapsule(fontScale = 0.5f, ai = false)

        assertNear("height at 50 percent text", 48.dp, bounds(TripPlannerTestTags.SEARCH_ROW_TO).height)
    }

    @Test
    fun `the height is the same with the microphone on and off`() {
        val ai = mutableStateOf(true)
        setCapsuleOnce(aiState = ai)
        val withMic = bounds(TripPlannerTestTags.SEARCH_ROW_TO).height

        ai.value = false
        composeRule.waitForIdle()

        assertNear("height with the microphone off", withMic, bounds(TripPlannerTestTags.SEARCH_ROW_TO).height)
    }

    // endregion

    // region glyph and microphone

    @Test
    fun `the search glyph is at the start with the microphone on`() {
        setCapsule(fontScale = 1f, ai = true)

        assertGlyphAtTheStart()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_ASK_KRAIL, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `the search glyph is at the start with the microphone off, and there is no microphone`() {
        setCapsule(fontScale = 1f, ai = false)

        assertGlyphAtTheStart()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_ASK_KRAIL).assertDoesNotExist()
    }

    private fun assertGlyphAtTheStart() {
        val capsule = bounds(TripPlannerTestTags.SEARCH_ROW_TO)
        val glyph = bounds(TripPlannerTestTags.HOME_SEARCH_GLYPH)
        val text = textBounds()
        composeRule.onNodeWithTag(TripPlannerTestTags.HOME_SEARCH_GLYPH, useUnmergedTree = true).assertIsDisplayed()
        assertNear("the glyph's left edge is 16 dp in", 16.dp, glyph.left - capsule.left)
        assertNear("the glyph's width", 24.dp, glyph.width)
        assertNear("the text starts 52 dp in", 52.dp, text.left - capsule.left)
        assertNear("the glyph is centred on the capsule", capsule.top + capsule.height / 2, glyph.top + glyph.height / 2)
    }

    @Test
    fun `the microphone is 48 dp square, 4 dp in from the end, and the text stops 16 dp short of it`() {
        setCapsule(fontScale = 1f, ai = true)

        val capsule = bounds(TripPlannerTestTags.SEARCH_ROW_TO)
        val mic = bounds(TripPlannerTestTags.SEARCH_ROW_ASK_KRAIL)
        assertNear("microphone width", 48.dp, mic.width)
        assertNear("microphone height", 48.dp, mic.height)
        assertNear("microphone is 4 dp in from the end", capsule.right - 4.dp, mic.right)
        assertNear("microphone is centred on the capsule", capsule.top + capsule.height / 2, mic.top + mic.height / 2)
        assertTrue(textBounds().right <= mic.left - 16.dp + 1.dp, "The text stops 16 dp short of the microphone.")
    }

    @Test
    fun `with the microphone off nothing else moves, the glyph, the text start and the height are the same`() {
        val ai = mutableStateOf(true)
        setCapsuleOnce(aiState = ai)
        val capsuleOn = bounds(TripPlannerTestTags.SEARCH_ROW_TO)
        val glyphOn = bounds(TripPlannerTestTags.HOME_SEARCH_GLYPH)
        val textOn = textBounds()

        ai.value = false
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_ASK_KRAIL).assertDoesNotExist()
        assertEquals(capsuleOn, bounds(TripPlannerTestTags.SEARCH_ROW_TO), "The capsule did not move.")
        assertEquals(glyphOn, bounds(TripPlannerTestTags.HOME_SEARCH_GLYPH), "The glyph did not move.")
        assertEquals(textOn.left, textBounds().left, "The text starts in the same place.")
    }

    // endregion

    // region taps

    @Test
    fun `a tap on the capsule opens Search Stop and is not a tap on the microphone`() {
        setCapsule(fontScale = 1f, ai = true)

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_TO, useUnmergedTree = true).performClick()

        assertEquals(1, clicks)
        assertTrue(aiEvents.isEmpty())
    }

    @Test
    fun `a tap on the microphone opens Ask KRAIL and never Search Stop`() {
        setCapsule(fontScale = 1f, ai = true)

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_ASK_KRAIL, useUnmergedTree = true).performClick()

        assertEquals(listOf<AiSearchInputEvent>(AiSearchInputEvent.OpenInput), aiEvents)
        assertEquals(0, clicks)
    }

    @Test
    fun `a screen reader hears the whole question as a button, and the microphone as Ask KRAIL`() {
        setCapsule(fontScale = 2f, ai = true)

        val capsule = composeRule.onNode(hasContentDescription(HOME_PILL_PLACEHOLDER), useUnmergedTree = true).fetchSemanticsNode()
        assertEquals(Role.Button, capsule.config.getOrNull(SemanticsProperties.Role), "The capsule is a button.")
        assertTrue(capsule.config.contains(SemanticsActions.OnClick), "The capsule can be clicked.")
        val mic = composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_ASK_KRAIL, useUnmergedTree = true)
            .fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
        assertEquals(listOf("Ask KRAIL"), mic)
    }

    // endregion

    // region width

    @Test
    @Config(qualifiers = "w700dp-h900dp")
    fun `the capsule is capped at 480 dp and centred, with equal margins`() {
        setCapsule(fontScale = 1f, ai = true)

        val capsule = bounds(TripPlannerTestTags.SEARCH_ROW_TO)
        assertNear("the capsule's width", 480.dp, capsule.width)
        assertNear("the margin on each side is the same", capsule.left, 700.dp - capsule.right)
    }

    @Test
    fun `on a phone the capsule fills the window less 16 dp at each side`() {
        setCapsule(fontScale = 1f, ai = true)

        val capsule = bounds(TripPlannerTestTags.SEARCH_ROW_TO)
        assertNear("left margin", 16.dp, capsule.left)
        assertNear("right margin", 16.dp, PHONE_WIDTH - capsule.right)
    }

    // endregion

    // region harness

    private val aiState = mutableStateOf(true)

    private fun setCapsule(
        fontScale: Float = 1f,
        ai: Boolean = true,
        fontScaleState: State<Float>? = null,
    ) {
        aiState.value = ai
        setCapsuleOnce(aiState = aiState, fontScale = fontScale, fontScaleState = fontScaleState)
    }

    private fun setCapsuleOnce(
        aiState: State<Boolean>,
        fontScale: Float = 1f,
        fontScaleState: State<Float>? = null,
    ) {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(LocalDensity.current.density, fontScaleState?.value ?: fontScale),
            ) {
                KrailTheme(themeController = ThemeController(currentMode = ThemeMode.LIGHT, setThemeMode = {})) {
                    CompositionLocalProvider(LocalThemeColor provides remember { mutableStateOf(KrailThemeStyle.Train.hexColorCode) }) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                            HomeSearchCapsule(
                                onClick = { clicks++ },
                                isAiAvailable = aiState.value,
                                onAiEvent = { aiEvents += it },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun questionLayout(): TextLayoutResult {
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNode(hasText(HOME_PILL_PLACEHOLDER), useUnmergedTree = true)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsActions.GetTextLayoutResult)
            ?.action
            ?.invoke(layouts)
        return layouts.first()
    }

    private fun bounds(tag: String): DpRect =
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).getVisibleBoundsInRoot()

    private fun textBounds(): DpRect =
        composeRule.onNode(hasText(HOME_PILL_PLACEHOLDER), useUnmergedTree = true).getVisibleBoundsInRoot()

    private fun assertNear(what: String, expected: Dp, actual: Dp) {
        assertTrue(
            abs(expected.value - actual.value) <= TOLERANCE_DP,
            "$what should be at $expected but is at $actual.",
        )
    }

    // endregion

    private companion object {
        const val TOLERANCE_DP = 1.5f
        val PHONE_WIDTH = 411.dp
        val NARROW_WIDTH = 320.dp
    }
}

private const val PHONE = "w411dp-h891dp"
private const val NARROW = "w320dp-h1280dp"
