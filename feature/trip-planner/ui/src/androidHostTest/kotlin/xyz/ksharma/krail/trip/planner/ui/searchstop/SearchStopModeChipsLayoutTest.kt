package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.transport.ModeSelectionSurface
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The mode filter block of Search Stop's map state: whole chips that wrap at every text size and never scroll
 * sideways or fade at an edge, with nothing shrunk, clipped or split to make it fit. Each test runs in a window
 * of its own width (density 1, so a dp is a pixel) and reads where the real chips landed.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [MODE_CHIPS_TEST_SDK], manifest = Config.NONE)
class SearchStopModeChipsLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val modes = NswTransportConfig.modesFor(ModeSelectionSurface.NEARBY_STOPS)
    private val allOn: ImmutableSet<Int> =
        modes.map { NswTransportConfig.productClassFor(it) }.toImmutableSet()

    @Test
    @Config(qualifiers = NARROW)
    fun `at normal text every mode is a whole chip and the block wraps onto more lines`() {
        setChips()

        val chips = chipBounds()
        assertEquals(modes.size, chips.size, "One chip for every mode the map can show.")
        assertTrue(chips.map { it.top }.distinct().size > 1, "A 320 dp block of ${modes.size} chips wraps.")
        assertAllInsideTheBlock(chips)
        assertNoChipSqueezed(chips)
    }

    @Test
    @Config(qualifiers = NARROW)
    fun `at exactly 130 percent the chips still wrap and stay inside the block`() {
        setChips(fontScale = 1.3f)

        val chips = chipBounds()
        assertTrue(chips.map { it.top }.distinct().size > 1)
        assertAllInsideTheBlock(chips)
        assertNoChipSqueezed(chips)
    }

    @Test
    @Config(qualifiers = NARROW)
    fun `at 200 percent the chips wrap onto more lines and none runs past the edge`() {
        setChips(fontScale = 2f)

        val chips = chipBounds()
        assertTrue(chips.map { it.top }.distinct().size > 1, "Larger text wraps whole chips, never a scrolling row.")
        assertAllInsideTheBlock(chips)
        assertNoChipSqueezed(chips)
    }

    @Test
    @Config(qualifiers = NARROW)
    fun `the block is a card with the card corner and padding and has no sideways scroll or fade`() {
        setChips(fontScale = 2f)

        val block = composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS).getUnclippedBoundsInRoot()
        val gaps = searchStopGaps()
        val chips = chipBounds()
        assertEquals(gaps.cardPadding, chips.minOf { it.left } - block.left, "Chips start one card padding in.")
        assertEquals(gaps.cardPadding, chips.minOf { it.top } - block.top, "Chips start one card padding down.")
        assertTrue(chips.maxOf { it.right } <= block.right - gaps.cardPadding + 1.dp, "No chip passes the padding.")
        composeRule.onAllNodes(hasScrollAction()).assertCountEquals(0)
    }

    @Test
    @Config(qualifiers = NARROW)
    fun `a chip is on or off the way a toggle says, and a tap reports its mode`() {
        val toggled = mutableListOf<TransportMode>()
        val train = modes.first { it == TransportMode.Train }
        val ferry = modes.first { it == TransportMode.Ferry }
        setChips(selected = persistentSetOf(NswTransportConfig.productClassFor(train)), onToggle = { toggled += it })

        composeRule.onNodeWithTag(chipTag(train)).assertIsOn()
        composeRule.onNodeWithTag(chipTag(ferry)).assertIsOff()
        composeRule.onNodeWithTag(chipTag(ferry)).performClick()
        composeRule.onNodeWithTag(chipTag(train)).performClick()

        assertEquals(listOf(ferry, train), toggled)
    }

    // region harness

    private fun chipTag(mode: TransportMode) =
        TripPlannerTestTags.searchStopModeChip(NswTransportConfig.productClassFor(mode))

    /** A chip that was squeezed, or had its text broken onto a second line, would be taller than the rest. */
    private fun assertNoChipSqueezed(chips: List<DpRect>) {
        assertEquals(1, chips.map { it.height }.distinct().size, "Every chip is as tall as the rest: $chips")
    }

    private fun assertAllInsideTheBlock(chips: List<DpRect>) {
        val block = composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS).getUnclippedBoundsInRoot()
        for (chip in chips) {
            assertTrue(chip.left >= block.left && chip.right <= block.right, "A chip $chip stays in the block $block")
        }
    }

    private fun chipBounds(): List<DpRect> = modes.map { mode ->
        composeRule.onNodeWithTag(chipTag(mode), useUnmergedTree = true).getUnclippedBoundsInRoot()
    }

    private fun setChips(
        fontScale: Float = 1f,
        selected: ImmutableSet<Int> = allOn,
        onToggle: (TransportMode) -> Unit = {},
    ) {
        composeRule.setContent {
            WithFontScale(fontScale) {
                PreviewTheme {
                    Box(modifier = Modifier.width(NARROW_WIDTH.dp)) {
                        SearchStopModeBlock(selectedModes = selected, onToggle = onToggle)
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Composable
    private fun WithFontScale(fontScale: Float, content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalDensity provides Density(LocalDensity.current.density, fontScale),
            content = content,
        )
    }

    private companion object {
        const val NARROW_WIDTH = 320
        const val NARROW = "w${NARROW_WIDTH}dp-h640dp"
    }

    // endregion
}

private const val MODE_CHIPS_TEST_SDK = 34
