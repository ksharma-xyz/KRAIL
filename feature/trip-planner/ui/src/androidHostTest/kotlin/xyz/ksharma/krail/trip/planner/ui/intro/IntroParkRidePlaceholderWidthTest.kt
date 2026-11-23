package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCarPark
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardStation
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The room the Park & Ride figures take before they arrive is the widest reading a car park can have, so a four digit count such as
 * 1398 or a full car park (100 percent) never wraps or pushes the layout when it arrives, whatever the width and the text size. Drawn
 * through the real Intro slot at 411 dp with text at 100 percent and at 130 percent, and at 320 dp with text at 200 percent: the card
 * is as high with the numbers as without, the clear room is as wide as every reading the card can show, and a line wraps only where
 * its own width forces it to.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroParkRidePlaceholderWidthTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val shown = mutableStateOf<IntroCardSlot>(IntroCardSlot.Skeleton)
    private lateinit var measurer: TextMeasurer
    private lateinit var figureStyle: TextStyle
    private lateinit var updatedStyle: TextStyle
    private var gapSmallPx = 0f
    private var gapMediumPx = 0f
    private var endPaddingPx = 0f

    private fun station(name: String, vararg carParks: ParkRideCarPark) = IntroCardSlot.Station(
        ParkRideCardStation(
            stationId = "2153478",
            stationName = name,
            carParks = persistentListOf(*carParks),
        ).toParkRideUiState(),
    )

    private fun loading(name: String = "Bella Vista") = station(name, ParkRideCarPark("31", name))

    private fun reading(spots: Int, percent: Int, time: String?, name: String = "Bella Vista") =
        station(name, ParkRideCarPark("31", name, spots, percent, time))

    private fun start(widthDp: Int, fontScale: Float) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                PreviewTheme {
                    val dim = KrailTheme.dimensions
                    measurer = rememberTextMeasurer()
                    figureStyle = KrailTheme.typography.headlineMedium
                    updatedStyle = KrailTheme.typography.bodySmall.copy(fontWeight = IntroCardTextWeight)
                    with(LocalDensity.current) {
                        gapSmallPx = dim.spacingXS.toPx()
                        gapMediumPx = dim.spacingML.toPx()
                        endPaddingPx = dim.spacingL.toPx()
                    }
                    Box(Modifier.width(widthDp.dp)) { IntroParkRideCardSlot(shown.value) }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun show(slot: IntroCardSlot) {
        shown.value = slot
        composeRule.waitForIdle()
    }

    private fun heightPx() = composeRule.onNodeWithTag(INTRO_CARD_SLOT_TAG).fetchSemanticsNode().size.height

    private fun nodes(text: String, substring: Boolean = false): List<SemanticsNode> =
        composeRule.onAllNodes(hasText(text, substring = substring), useUnmergedTree = true).fetchSemanticsNodes()

    // The node a rider reads, and not the clear room that holds its place.
    private fun real(text: String, substring: Boolean = false): SemanticsNode =
        nodes(text, substring).single { node -> generateSequence(node) { it.parent }.none { it.config.isClearingSemantics } }

    // The clear room's own copy of the same words.
    private fun room(text: String, substring: Boolean = false): SemanticsNode =
        nodes(text, substring).single { node -> generateSequence(node) { it.parent }.any { it.config.isClearingSemantics } }

    private fun assertSameHeightLoadedAndLoading(loadedCards: List<Pair<String, IntroCardSlot>>, loadingCard: IntroCardSlot, where: String) {
        show(loadingCard)
        val loadingHeight = heightPx()
        loadedCards.forEach { (what, card) ->
            show(card)
            assertEquals(loadingHeight, heightPx(), "the card is a different height with $what than without numbers, $where")
            show(loadingCard)
            assertEquals(loadingHeight, heightPx(), "the card does not return to its height after $what, $where")
        }
    }

    private fun heightChecks(widthDp: Int, fontScale: Float) {
        start(widthDp, fontScale)
        val where = "at $widthDp dp with text at ${(fontScale * 100).toInt()} percent"
        assertSameHeightLoadedAndLoading(
            loadedCards = listOf(
                "1398 spots, 100% full and the longest time" to reading(1398, 100, "12:59\u00A0PM"),
                "9999 spots and a full car park" to reading(9999, 100, "10:00\u00A0AM"),
                "a small reading" to reading(48, 7, "8:05\u00A0AM"),
                "a reading with no time" to reading(1398, 100, null),
            ),
            loadingCard = loading(),
            where = where,
        )
        val longName = "Bella Vista Commuter Car Park Level Three North"
        assertSameHeightLoadedAndLoading(
            loadedCards = listOf("a long name" to reading(1398, 100, "12:59\u00A0PM", name = longName)),
            loadingCard = loading(longName),
            where = where,
        )
        val names = (1..3).map { ParkRideCarPark("2$it", "Tallawong P$it") }
        val readings = (1..3).map { ParkRideCarPark("2$it", "Tallawong P$it", 1000 + it, 100, "12:59\u00A0PM") }
        assertSameHeightLoadedAndLoading(
            loadedCards = listOf("three car parks" to station("Tallawong", *readings.toTypedArray())),
            loadingCard = station("Tallawong", *names.toTypedArray()),
            where = where,
        )
    }

    @Test
    fun `the card is as high with the widest readings as without on a phone`() = heightChecks(411, 1f)

    @Test
    fun `the card is as high with the widest readings as without at 130 percent text`() = heightChecks(411, 1.3f)

    @Test
    fun `the card is as high with the widest readings as without at 320 dp and 200 percent text`() = heightChecks(320, 2f)

    private fun width(style: TextStyle, text: String) = measurer.measure(text, style).size.width

    private fun widthChecks(widthDp: Int, fontScale: Float) {
        start(widthDp, fontScale)
        show(loading())
        val where = "at $widthDp dp with text at ${(fontScale * 100).toInt()} percent"
        val spots = room("0000").size.width
        val percent = room("100%").size.width
        val updatedRoom = room("Last updated at", substring = true)
        val name = real("Bella Vista").boundsInRoot
        val card = composeRule.onNodeWithTag(INTRO_CARD_SLOT_TAG).fetchSemanticsNode().boundsInRoot
        val available = (card.right - endPaddingPx - name.left).toInt()

        val counts = (0..9).map { "$it$it$it$it" } + listOf("1398", "1234", "9876", "2468", "8080")
        counts.forEach { assertTrue(width(figureStyle, it) <= spots, "the room for the spots is narrower than $it, $where") }
        (0..100).map { "$it%" }.forEach { assertTrue(width(figureStyle, it) <= percent, "the room for the share is narrower than $it, $where") }
        // The time is a line of words that wraps in a narrow room, so the room is compared with a reading in the same room, line for line.
        val roomLines = lineCount(updatedRoom)
        val times = (1..12).flatMap { hour ->
            (0..59).flatMap { minute -> listOf("AM", "PM").map { "$hour:${minute.toString().padStart(2, '0')}\u00A0$it" } }
        }
        times.forEach {
            val lines = measurer.measure("Last updated at\u00A0$it", updatedStyle, constraints = Constraints(maxWidth = available)).lineCount
            assertTrue(lines <= roomLines, "$it takes $lines lines and the room for it $roomLines, $where")
            if (roomLines == 1) {
                assertTrue(width(updatedStyle, "Last updated at\u00A0$it") <= updatedRoom.size.width, "the room for the time is narrower than $it, $where")
            }
        }
        // The room is the widest reading of its kind, and not a width wider than the line it holds.
        assertTrue(spots >= width(figureStyle, "0000") - 1, "the room for the spots is not four digits wide, $where")
        assertTrue(percent >= width(figureStyle, "100%") - 1, "the room for the share is not 100% wide, $where")
    }

    private fun lineCount(node: SemanticsNode): Int {
        val layouts = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        return layouts.single().lineCount
    }

    @Test
    fun `the clear room is as wide as every count, share and time on a phone`() = widthChecks(411, 1f)

    @Test
    fun `the clear room is as wide as every count, share and time at 130 percent text`() = widthChecks(411, 1.3f)

    @Test
    fun `the clear room is as wide as every count, share and time at 320 dp and 200 percent text`() = widthChecks(320, 2f)

    // Whether b sits to the right of a on the same line, as opposed to under it.
    private fun besides(a: Rect, b: Rect) = b.left >= a.right - HALF_PIXEL

    // The one line a pair of words takes, from the left of the first to the right of the second, as a share of the room it is given.
    private fun span(a: Rect, b: Rect) = maxOf(a.right, b.right) - minOf(a.left, b.left)

    private class Figures(val spots: Rect, val spotsLabel: Rect, val percent: Rect, val percentLabel: Rect, val room: Float)

    private fun figuresOf(pick: (String, Boolean) -> SemanticsNode, count: String, share: String): Figures {
        val name = real("Bella Vista").boundsInRoot
        val card = composeRule.onNodeWithTag(INTRO_CARD_SLOT_TAG).fetchSemanticsNode().boundsInRoot
        return Figures(
            spots = pick(count, false).boundsInRoot,
            spotsLabel = pick("spots available", false).boundsInRoot,
            percent = pick(share, false).boundsInRoot,
            percentLabel = pick("full", false).boundsInRoot,
            room = card.right - endPaddingPx - name.left,
        )
    }

    private fun wrapChecks(widthDp: Int, fontScale: Float, fitsOnOneLine: Boolean) {
        start(widthDp, fontScale)
        val where = "at $widthDp dp with text at ${(fontScale * 100).toInt()} percent"
        show(loading())
        val held = figuresOf({ text, sub -> room(text, sub) }, "0000", "100%")
        show(reading(1398, 100, "12:59\u00A0PM"))
        val figures = figuresOf({ text, sub -> real(text, sub) }, "1398", "100%")

        // A count and its words are on one line when their widths and the gap between them fit the room, and wrap when they do not.
        val spotsFit = figures.spots.width + gapSmallPx + figures.spotsLabel.width <= figures.room
        val percentFit = figures.percent.width + gapSmallPx + figures.percentLabel.width <= figures.room
        assertEquals(spotsFit, besides(figures.spots, figures.spotsLabel), "1398 and its words wrap where they fit, or stay where they do not, $where")
        assertEquals(percentFit, besides(figures.percent, figures.percentLabel), "100% and its word wrap where they fit, or stay where they do not, $where")
        val bothFit = span(figures.spots, figures.spotsLabel) + gapMediumPx + span(figures.percent, figures.percentLabel) <= figures.room
        if (spotsFit && percentFit) {
            assertEquals(bothFit, besides(figures.spotsLabel, figures.percent), "the two figures wrap where they fit, or stay where they do not, $where")
        }
        if (fitsOnOneLine) {
            assertTrue(spotsFit && percentFit && bothFit && besides(figures.spotsLabel, figures.percent), "1398 spots available 100% full wraps $where")
        }

        // The held room wraps exactly where the reading does, so nothing moves when the reading arrives.
        assertEquals(besides(held.spots, held.spotsLabel), besides(figures.spots, figures.spotsLabel), "the room and 1398 wrap differently, $where")
        assertEquals(besides(held.percent, held.percentLabel), besides(figures.percent, figures.percentLabel), "the room and 100% wrap differently, $where")
        assertEquals(besides(held.spotsLabel, held.percent), besides(figures.spotsLabel, figures.percent), "the room and the two figures wrap differently, $where")
    }

    @Test
    fun `1398 spots available and 100 percent full are one line on a phone`() = wrapChecks(411, 1f, fitsOnOneLine = true)

    @Test
    fun `1398 spots available and 100 percent full wrap only where they must at 130 percent text`() = wrapChecks(411, 1.3f, fitsOnOneLine = false)

    @Test
    fun `1398 spots available and 100 percent full wrap only where they must at 320 dp and 200 percent text`() =
        wrapChecks(320, 2f, fitsOnOneLine = false)

    private companion object {
        const val HALF_PIXEL = 0.5f
    }
}
