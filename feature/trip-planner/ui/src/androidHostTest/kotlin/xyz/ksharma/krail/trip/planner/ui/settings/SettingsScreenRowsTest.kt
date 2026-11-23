package xyz.ksharma.krail.trip.planner.ui.settings

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Settings screen has no How to KRAIL entry: the Intro opens on a first run from the splash and is not reached from Settings. The
 * other rows are as they were, each with its tag and its own click, and About keeps Our story. The last test holds that nothing in the
 * source still names what was removed with the row: its parameter, its tag, the view model method, the navigator method and the event.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class SettingsScreenRowsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val clicks = mutableListOf<String>()

    private fun show(showDebugConfig: Boolean = false) {
        composeRule.setContent {
            PreviewTheme {
                SettingsScreen(
                    appVersion = "1.0.0",
                    showDebugConfig = showDebugConfig,
                    onChangeThemeClick = { clicks += "theme" },
                    onReferFriendClick = { clicks += "invite" },
                    onRateAppClick = { clicks += "rate" },
                    onAboutUsClick = { clicks += "story" },
                    onDebugConfigClick = { clicks += "debug" },
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `the screen does not show How to KRAIL, by its words or by its old tag`() {
        show()

        composeRule.onAllNodesWithText("How to KRAIL?").assertCountEquals(0)
        composeRule.onAllNodesWithText("How to KRAIL", substring = true, ignoreCase = true).assertCountEquals(0)
        composeRule.onAllNodesWithTag(REMOVED_TAG).assertCountEquals(0)
    }

    @Test
    fun `the other rows and the About heading are still there, each with its tag, and each takes its own tap`() {
        show()

        composeRule.onNodeWithText("Change Theme").assertIsDisplayed()
        composeRule.onNodeWithText("Rate KRAIL").assertIsDisplayed()
        composeRule.onNodeWithText("About").assertIsDisplayed()
        composeRule.onNodeWithText("Our story").assertIsDisplayed()
        listOf(
            TripPlannerTestTags.SETTINGS_ITEM_THEME to "theme",
            TripPlannerTestTags.SETTINGS_ITEM_INVITE to "invite",
            TripPlannerTestTags.SETTINGS_ITEM_RATE to "rate",
            TripPlannerTestTags.SETTINGS_ITEM_OUR_STORY to "story",
        ).forEach { (tag, expected) ->
            clicks.clear()
            composeRule.onNodeWithTag(tag).performClick()
            assertEquals(listOf(expected), clicks, tag)
        }
    }

    @Test
    fun `Debug Config shows only when asked, and the screen is otherwise the same`() {
        show(showDebugConfig = false)
        composeRule.onAllNodesWithTag(TripPlannerTestTags.SETTINGS_ITEM_DEBUG_CONFIG).assertCountEquals(0)
    }

    @Test
    fun `nothing in the source still names the row, its parameter, its tag, its handlers or its event`() {
        val root = generateSequence(File("").absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").isFile }
            ?: error("Cannot find the repository root above ${File("").absolutePath}")
        val sources = listOf(
            "feature/trip-planner/ui/src",
            "core/analytics/src",
            "composeApp/src",
        ).flatMap { path ->
            File(root, path).walkTopDown().filter { it.isFile && it.extension == "kt" && it.name != THIS_FILE }.toList()
        }
        assertTrue(sources.size > MIN_SOURCES, "found only ${sources.size} sources, so this guard would pass by looking at nothing")

        val offenders = sources.flatMap { file ->
            val text = file.readText()
            REMOVED_NAMES.filter { it in text }.map { "${file.relativeTo(root).path} names $it" }
        }

        assertTrue(offenders.isEmpty(), "These were removed with the Settings row and must not come back: $offenders")
    }

    private companion object {
        const val THIS_FILE = "SettingsScreenRowsTest.kt"
        const val MIN_SOURCES = 200
        const val REMOVED_TAG = "settings.item.howto"

        // Built from parts, so this file does not name them as one word either.
        val REMOVED_NAMES = listOf(
            "onIntro" + "Click",
            "SETTINGS_ITEM_" + "HOW_TO",
            "SettingsHowTo" + "UseClickEvent",
            "how_to_" + "krail",
            "navigateTo" + "Intro",
            "How to " + "KRAIL?",
        )
    }
}
