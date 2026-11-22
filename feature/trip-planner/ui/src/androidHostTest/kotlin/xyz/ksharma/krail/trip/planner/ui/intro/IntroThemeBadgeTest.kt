package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
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
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.taj.theme.PreviewTheme
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The four theme badges as drawn, read for real: each is the app's `TransportModeIcon`, so its letter is white in light
 * and in dark, and it keeps the 48 dp touch target, the tag, the spoken name and the selected state. The letter is read
 * from the drawn text. The fill is the icon's own `NswTransportConfig.colorFor(mode)`, and `IntroThemeChoicesTest` holds
 * that it is the theme colour. The badges are drawn at rest, with nothing passed to the icon that picks a colour.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroThemeBadgeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun showAllAtRest(selected: Int? = null) {
        composeRule.setContent {
            PreviewTheme {
                Row {
                    introThemeChoices().forEachIndexed { index, choice ->
                        IntroThemeBadge(
                            choice = choice,
                            selected = index == selected,
                            frame = { RestFrame },
                            onClick = {},
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun letterColor(choice: IntroThemeChoice): Color {
        val letter = NswTransportConfig.nameFor(choice.mode).first().toString()
        val node = composeRule.onNode(
            hasText(letter) and hasAnyAncestor(hasTestTag(choice.tag)),
            useUnmergedTree = true,
        )
        val results = mutableListOf<TextLayoutResult>()
        node.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.single().layoutInput.style.color
    }

    private fun assertWhiteLettersOnTheModeIcons() {
        introThemeChoices().forEach { choice ->
            assertEquals(Color.White, letterColor(choice), "${choice.name}'s letter is not white")
        }
    }

    @Test
    fun `in light every badge has a white letter, drawn by the mode icon`() {
        showAllAtRest()

        assertWhiteLettersOnTheModeIcons()
    }

    @Test
    @Config(qualifiers = "+night")
    fun `in dark every badge has a white letter, drawn by the mode icon`() {
        showAllAtRest()

        assertWhiteLettersOnTheModeIcons()
    }

    @Test
    fun `a chosen badge keeps a white letter, because selecting never changes the colours`() {
        val chosen = mutableStateOf(0)
        composeRule.setContent {
            PreviewTheme {
                IntroThemeBadge(
                    choice = introThemeChoices()[chosen.value],
                    selected = true,
                    frame = { settledFrame(selected = true) },
                    onClick = {},
                )
            }
        }

        introThemeChoices().indices.forEach { index ->
            chosen.value = index
            composeRule.waitForIdle()

            assertEquals(Color.White, letterColor(introThemeChoices()[index]))
        }
    }

    @Test
    fun `each badge is a 48 dp touch target with its tag, a name for a screen reader and its selected state`() {
        showAllAtRest(selected = 2)

        introThemeChoices().forEachIndexed { index, choice ->
            val node = composeRule.onNodeWithTag(choice.tag).assertHasClickAction()
            val bounds = node.getUnclippedBoundsInRoot()
            assertTrue(
                bounds.width >= TOUCH - ROUNDING && bounds.height >= TOUCH - ROUNDING,
                "${choice.name} is $bounds",
            )
            val config = node.fetchSemanticsNode().config
            assertEquals(listOf(choice.name), config[SemanticsProperties.ContentDescription])
            assertEquals(index == 2, config[SemanticsProperties.Selected], choice.name)
        }
    }

    private companion object {
        val TOUCH = 48.dp
        val ROUNDING = 1.dp
    }
}
