package xyz.ksharma.krail.trip.planner.ui.intro

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The four theme badges as the real composable lays them out, in a window wide enough for every room of the table: the wave where it
 * fits, the zigzag where it does not and the column below that, for the window widths 280, 320, 352, 360, 411, 480, 600 and 840 dp at
 * 100 and 200 percent text. The content is at most 480 dp wide and the room is that less 32 dp of page padding, so a tablet and an open
 * fold use the same column as 480 dp. The badges are drawn at rest, and what is read is the real position of each against the pure
 * layout (`IntroThemeBadgeGridTest` holds that), that none is smaller than a 48 dp target or overlaps another, that a tap on each picks
 * that badge, that a screen reader meets them Train, Metro, Bus, Ferry, and that reduced motion draws them still.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = "w900dp-h1200dp-xxhdpi",
    manifest = Config.NONE,
)
class IntroThemeBadgeWaveTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val widths = listOf(280, 320, 352, 360, 411, 480, 600, 840)
    private val picks = mutableListOf<Int>()
    private val windowWidth = mutableStateOf(411)
    private val fontScale = mutableStateOf(1f)
    private var contentSet = false

    private fun show(window: Int, scale: Float = 1f, inspection: Boolean = true) {
        windowWidth.value = window
        fontScale.value = scale
        if (!contentSet) {
            contentSet = true
            composeRule.setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, fontScale = fontScale.value),
                    LocalInspectionMode provides inspection,
                ) {
                    PreviewTheme {
                        // The Intro's content is at most 480 dp wide, so the room of a wider window is the same as 480 dp's.
                        Box(
                            modifier = Modifier.requiredWidth(min(windowWidth.value, CONTENT_CAP).dp).testTag(ROOM),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            IntroThemeBadges(selectedThemeId = null, onPick = { picks += it })
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    @After
    fun restoreMotion() {
        Settings.Global.putFloat(
            ApplicationProvider.getApplicationContext<Context>().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
    }

    private fun bounds(): List<DpRect> = introThemeChoices().map { composeRule.onNodeWithTag(it.tag).getUnclippedBoundsInRoot() }

    private fun expected(window: Int) = themeBadgeGrid(
        count = 4,
        availableDp = (min(window, CONTENT_CAP) - PAGE_PADDING_TWICE).toFloat(),
        badgeDp = 64f,
    )

    @Test
    fun `at every window width the badges are where the pure layout puts them, at 100 and 200 percent text`() {
        widths.forEach { window ->
            listOf(1f, 2f).forEach { scale ->
                show(window, scale)
                val grid = expected(window)
                val b = bounds()
                val room = composeRule.onNodeWithTag(ROOM).getUnclippedBoundsInRoot()
                // The room is the content less the page padding on each side, and the grid is centred across it.
                val left = room.left + 16.dp + ((min(window, CONTENT_CAP) - PAGE_PADDING_TWICE).dp - grid.widthDp.dp) / 2f
                val where = "$window dp at $scale"

                b.forEachIndexed { i, rect ->
                    assertEquals(64f, rect.width.value, 1f, "$where: badge $i is not 64 dp")
                    assertEquals(left.value + grid.xsDp[i], rect.left.value, 1f, "$where: badge $i across")
                    assertEquals(room.top.value + grid.ysDp[i], rect.top.value, 1f, "$where: badge $i down")
                }
                assertEquals(grid.heightDp, (b.maxOf { it.bottom } - b.minOf { it.top }).value, 1f, "$where: the grid height")
            }
        }
    }

    @Test
    fun `the layout is the wave from 341 dp, the zigzag below it and the column in a very narrow window, as the table says`() {
        val layouts = widths.associateWith { expected(it).layout }

        assertEquals(
            mapOf(
                280 to ThemeBadgeLayout.Zigzag,
                320 to ThemeBadgeLayout.Zigzag,
                352 to ThemeBadgeLayout.Wave,
                360 to ThemeBadgeLayout.Wave,
                411 to ThemeBadgeLayout.Wave,
                480 to ThemeBadgeLayout.Wave,
                600 to ThemeBadgeLayout.Wave,
                840 to ThemeBadgeLayout.Wave,
            ),
            layouts,
        )
        assertEquals(ThemeBadgeLayout.Column, expected(200).layout)
    }

    @Test
    fun `a window wider than 480 dp is the same column as 480 dp, centred`() {
        show(480)
        val at480 = bounds().map { it.left.value - composeRule.onNodeWithTag(ROOM).getUnclippedBoundsInRoot().left.value }
        show(840)
        val at840 = bounds().map { it.left.value - composeRule.onNodeWithTag(ROOM).getUnclippedBoundsInRoot().left.value }

        assertEquals(at480.size, at840.size)
        at480.indices.forEach { assertEquals(at480[it], at840[it], 1f, "badge $it moved with the window") }
    }

    @Test
    fun `every badge is a target of at least 48 dp, whole inside its room, and no two overlap, at every width and text size`() {
        widths.forEach { window ->
            listOf(1f, 2f).forEach { scale ->
                show(window, scale)
                val b = bounds()
                val room = composeRule.onNodeWithTag(ROOM).getUnclippedBoundsInRoot()
                val where = "$window dp at $scale"

                b.forEachIndexed { i, rect ->
                    assertTrue(rect.width >= 48.dp - 1.dp && rect.height >= 48.dp - 1.dp, "badge $i is only $rect: $where")
                    assertTrue(rect.left >= room.left - 1.dp && rect.right <= room.right + 1.dp, "badge $i is outside the room: $rect $where")
                }
                b.forEachIndexed { i, a ->
                    b.drop(i + 1).forEachIndexed { k, other ->
                        // The badges are circles, so the space is the distance between the centres less a badge.
                        val centres = hypot(
                            (a.left + a.right - other.left - other.right).value / 2f,
                            (a.top + a.bottom - other.top - other.bottom).value / 2f,
                        )
                        assertTrue(centres - 64f >= 14f, "badges $i and ${i + 1 + k} are ${centres - 64f} dp apart: $where")
                    }
                }
            }
        }
    }

    @Test
    fun `a tap on each badge picks that badge and no other`() {
        widths.forEach { window ->
            show(window)
            introThemeChoices().forEach { choice ->
                picks.clear()

                composeRule.onNodeWithTag(choice.tag).performClick()
                composeRule.waitForIdle()

                assertEquals(listOf(choice.style.id), picks, "${choice.name} at $window dp")
            }
        }
    }

    @Test
    fun `a screen reader meets the badges Train, Metro, Bus, Ferry, one button each`() {
        widths.forEach { window ->
            show(window)

            val tags = introThemeChoices().map { it.tag }.toSet()
            val names = composeRule.onAllNodesWithTags(tags)
                .map { it.config[SemanticsProperties.ContentDescription].single() }

            assertEquals(listOf("Train", "Metro", "Bus", "Ferry"), names, "at $window dp")
        }
    }

    // The nodes under the theme tags, in the order the tree gives them, which is the order a screen reader reads.
    private fun ComposeContentTestRule.onAllNodesWithTags(tags: Set<String>) =
        onAllNodes(SemanticsMatcher("tag is one of $tags") { it.config.getOrNull(SemanticsProperties.TestTag) in tags })
            .fetchSemanticsNodes()

    @Test
    fun `with reduced motion the badges are drawn still, at their places, however long the clock runs`() {
        Settings.Global.putFloat(
            ApplicationProvider.getApplicationContext<Context>().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            0f,
        )
        composeRule.mainClock.autoAdvance = false
        // Not an inspection, which is still too: this is the real clock with the rider's reduced motion on.
        show(411, inspection = false)
        val grid = expected(411)
        val room = composeRule.onNodeWithTag(ROOM).getUnclippedBoundsInRoot()

        listOf(0L, 1_500L, 4_000L, 11_000L).forEach { ms ->
            composeRule.mainClock.advanceTimeBy(ms)
            bounds().forEachIndexed { i, rect ->
                assertEquals(room.top.value + grid.ysDp[i], rect.top.value, 0.6f, "badge $i moved down after $ms ms")
                assertEquals(rect.left.value, bounds()[i].left.value, 0.01f)
            }
        }
        assertTrue(bounds().all { abs(it.top.value - room.top.value) <= 48.5f }, "a badge is not on the wave")
    }

    private companion object {
        const val ROOM = "intro.theme.room"
        const val CONTENT_CAP = 480
        const val PAGE_PADDING_TWICE = 32
    }
}
