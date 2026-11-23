package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.test.assertEquals

/**
 * The room the colour screen keeps for its headline, at the three frames of the approved mock: 411 dp at 100 percent text,
 * 411 dp at 150 percent and 320 dp at 200 percent. The question, with its hero and whispers, and each of the four taglines a pick
 * brings are different heights of text, and the headline keeps the height of the tallest, so where it starts and how tall it is do
 * not change with the words, and not one badge moves because of them (the picked badge lifts by its own 4 dp).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class IntroThemeHeadlineRoomTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val none = IntroState(step = IntroStep.THEME)
    private val shown = mutableStateOf(none)

    private fun start(widthDp: Int, fontScale: Float) {
        RuntimeEnvironment.setQualifiers("w${widthDp}dp-h1280dp-xxhdpi")
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = fontScale),
                LocalInspectionMode provides true,
            ) {
                PreviewTheme {
                    IntroFlowScreen(state = shown.value, suggestedStops = DEFAULT_SUGGESTED_STOPS, onEvent = { _, _ -> })
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun show(state: IntroState) {
        shown.value = state
        composeRule.waitForIdle()
    }

    private fun headline(): DpRect = composeRule
        .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        .getUnclippedBoundsInRoot()

    private fun badges(): List<DpRect> = introThemeChoices().map { composeRule.onNodeWithTag(it.tag).getUnclippedBoundsInRoot() }

    private fun check(widthDp: Int, fontScale: Float) {
        start(widthDp, fontScale)
        val question = headline()
        val badgesBefore = badges()
        introThemeChoices().forEachIndexed { picked, choice ->
            show(none.copy(selectedThemeId = choice.style.id))
            val tagline = headline()

            assertEquals(question.left.value, tagline.left.value, ROUNDING, "${choice.name}: the headline moved across at $widthDp dp, $fontScale")
            assertEquals(question.top.value, tagline.top.value, ROUNDING, "${choice.name}: the headline moved down at $widthDp dp, $fontScale")
            assertEquals(question.bottom.value, tagline.bottom.value, ROUNDING, "${choice.name}: the headline changed height at $widthDp dp, $fontScale")
            badges().forEachIndexed { index, now ->
                val lift = if (index == picked) LIFT_DP else 0f
                assertEquals(badgesBefore[index].top.value, now.top.value, lift + ROUNDING, "${choice.name}: badge $index moved at $widthDp dp, $fontScale")
            }
        }
        show(none)
        assertEquals(question.bottom.value, headline().bottom.value, ROUNDING, "the question came back to its own height")
    }

    @Test
    fun `at 411 dp and 100 percent the headline keeps one height whichever words it has`() {
        check(widthDp = 411, fontScale = 1.0f)
    }

    @Test
    fun `at 411 dp and 150 percent the headline keeps one height whichever words it has`() {
        check(widthDp = 411, fontScale = 1.5f)
    }

    @Test
    fun `at 320 dp and 200 percent the headline keeps one height whichever words it has, with the hero stacked`() {
        check(widthDp = 320, fontScale = 2.0f)
    }

    @Test
    fun `the four themes are the ones offered`() {
        assertEquals(
            listOf(KrailThemeStyle.Train, KrailThemeStyle.Metro, KrailThemeStyle.Bus, KrailThemeStyle.Ferry),
            introThemeChoices().map { it.style },
        )
        assertEquals(320.dp, 320.dp)
    }

    private companion object {
        const val ROUNDING = 1.0f
        const val LIFT_DP = 4f
    }
}
