package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.components.FIGURE_RISE_DP
import xyz.ksharma.krail.trip.planner.ui.components.FIGURE_TOTAL_MS
import xyz.ksharma.krail.trip.planner.ui.components.ParkRideCard
import xyz.ksharma.krail.trip.planner.ui.components.figureFrame
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCarPark
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardStation
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The Park & Ride figures arrive as three staggered fades, read from the drawn card on a clock that is stepped by hand: the opacity
 * from the pixels, the rise from where the node is drawn, the height from the layout. The name and the tile are there as they always
 * were, so the numbers are the second beat, and the home card, which does not stage its numbers, shows them as it did. The curve and
 * the table of times are held by `ParkRideFigureRevealTest`.
 *
 * Time zero is the frame the numbers are composed in, which draws every line clear. The animation clock takes its start time a frame
 * later, as every Compose animation does, so a line is [START_LAG_MS] behind the table, and a reading may land a frame either side.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroParkRideFiguresFadeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val shown = mutableStateOf(loadingCard())
    private val generation = mutableIntStateOf(0)
    private var measurePasses = 0

    private fun station(vararg carParks: ParkRideCarPark) = ParkRideCardStation(
        stationId = "2153478",
        stationName = "Bella Vista",
        carParks = persistentListOf(*carParks),
    ).toParkRideUiState()

    private fun loadingCard() = station(ParkRideCarPark("31", "Bella Vista"))

    private fun readingCard(spots: Int = 120, percent: Int = 40, time: String = "8:05\u00A0AM") =
        station(ParkRideCarPark("31", "Bella Vista", spots, percent, time))

    private fun start(widthDp: Int = 411, fontScale: Float = 1f, card: @Composable (ParkRideUiState, Modifier) -> Unit) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                PreviewTheme {
                    Box(Modifier.width(widthDp.dp)) {
                        // The generation recreates the card from nothing, which is how one test sees the first arrival twice.
                        key(generation.intValue) {
                            card(
                                shown.value,
                                Modifier.testTag(INTRO_CARD_SLOT_TAG).layout { measurable, constraints ->
                                    measurePasses++
                                    val placeable = measurable.measure(constraints)
                                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                                },
                            )
                        }
                    }
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(SETTLE_MS)
    }

    // The card as the Intro draws it, through the real slot, so what is tested is what the rider gets.
    private fun startIntro(widthDp: Int = 411, fontScale: Float = 1f) = start(widthDp, fontScale) { state, modifier ->
        IntroParkRideCardSlot(slot = IntroCardSlot.Station(state), modifier = modifier)
    }

    // The shared card with the Intro's switches one at a time.
    private fun startCard(staged: Boolean, keepReadingSpace: Boolean, reduceMotion: Boolean? = null) = start { state, modifier ->
        ParkRideCard(
            parkRideUiState = state,
            isExpanded = true,
            onClick = null,
            keepReadingSpace = keepReadingSpace,
            stagedReading = staged,
            reduceMotion = reduceMotion,
            modifier = modifier,
        )
    }

    // The home card with exactly the arguments Saved Trips gives it and nothing else, so what is tested is what it does by default.
    private fun startHome() = start { state, modifier ->
        ParkRideCard(parkRideUiState = state, isExpanded = true, onClick = {}, modifier = modifier)
    }

    private fun frame() = composeRule.mainClock.advanceTimeBy(FRAME_MS)

    private fun now() = composeRule.mainClock.currentTime

    private fun framesUntil(zero: Long, elapsedMs: Long) {
        while (now() - zero < elapsedMs) frame()
    }

    // A card made from nothing with [card] as the first thing it draws.
    private fun fresh(card: ParkRideUiState) {
        shown.value = card
        generation.intValue++
        Snapshot.sendApplyNotifications()
        frame()
        frame()
    }

    /** The numbers arrive, and the frame they are composed in is drawn. Returns the clock of that frame, which is time zero. */
    private fun arrive(card: ParkRideUiState = readingCard()): Long {
        shown.value = card
        Snapshot.sendApplyNotifications()
        frame()
        return now()
    }

    private fun real(text: String, substring: Boolean = false): SemanticsNode =
        composeRule.onAllNodes(hasText(text, substring = substring), useUnmergedTree = true).fetchSemanticsNodes()
            .filter { node -> generateSequence(node) { it.parent }.none { it.config.isClearingSemantics } }
            .single()

    // The three lines of the reading, as drawn now: the bounds include the rise.
    private fun lines(): List<SemanticsNode> = listOf(real("120"), real("40%"), real("Last updated at", substring = true))

    private fun tops(): List<Float> = lines().map { it.boundsInRoot.top }

    private fun heightDp(): Float =
        with(composeRule.density) { composeRule.onNodeWithTag(INTRO_CARD_SLOT_TAG).fetchSemanticsNode().size.height.toDp().value }

    private fun picture(): BufferedImage = IntroPixels(composeRule).of(INTRO_CARD_SLOT_TAG)

    private fun origin() = composeRule.onNodeWithTag(INTRO_CARD_SLOT_TAG).fetchSemanticsNode().boundsInRoot

    /** The strongest difference from the card's own tint inside [bounds]. 0 is a line that is not drawn, and a line at rest is at full strength. */
    private fun ink(picture: BufferedImage, bounds: Rect): Float {
        val origin = origin()
        val gap = with(composeRule.density) { 8.dp.toPx() }
        val tint = picture.colourAt(bounds.left - origin.left - gap, bounds.center.y - origin.top)
        var strongest = 0f
        for (y in (bounds.top - origin.top).toInt() until (bounds.bottom - origin.top).toInt()) {
            for (x in (bounds.left - origin.left).toInt() until (bounds.right - origin.left).toInt()) {
                val colour = picture.colourAt(x.toFloat(), y.toFloat())
                strongest = maxOf(strongest, abs(colour.red - tint.red), abs(colour.green - tint.green), abs(colour.blue - tint.blue))
            }
        }
        return strongest
    }

    // What a reading at rest looks like for the numbers used here: the strength and the top of each line, and the card's height.
    private class Rest(val ink: List<Float>, val tops: List<Float>, val heightDp: Float)

    private fun rest(): Rest {
        fresh(readingCard())
        frame()
        return Rest(picture().let { picture -> lines().map { ink(picture, it.boundsInRoot) } }, tops(), heightDp())
    }

    /** How drawn each line is now, 0 for clear and 1 for at rest. */
    private fun alphas(rest: Rest): List<Float> {
        val picture = picture()
        return lines().mapIndexed { index, line -> (ink(picture, line.boundsInRoot) / rest.ink[index]).coerceIn(0f, 1f) }
    }

    /** How much of the rise each line still has to go, 1 for not started and 0 for in place. */
    private fun rises(rest: Rest): List<Float> {
        val risePx = with(composeRule.density) { FIGURE_RISE_DP.dp.toPx() }
        return tops().mapIndexed { index, top -> (top - rest.tops[index]) / risePx }
    }

    // Where the table puts line [beat] [elapsed] ms after the numbers landed, for the animation clock that starts a frame later, as the
    // two ends of a one frame window: the opacity from the earlier to the later end, and the rise left from the later to the earlier.
    private fun sinceStart(elapsed: Long, shift: Long) = (elapsed - START_LAG_MS + shift).coerceAtLeast(0L).toFloat()

    private fun opacityWindow(elapsed: Long, beat: Int) =
        figureFrame(sinceStart(elapsed, -FRAME_MS), beat, false).alpha..figureFrame(sinceStart(elapsed, FRAME_MS), beat, false).alpha

    private fun riseWindow(elapsed: Long, beat: Int) =
        figureFrame(sinceStart(elapsed, FRAME_MS), beat, false).rise..figureFrame(sinceStart(elapsed, -FRAME_MS), beat, false).rise

    private fun assertInside(what: String, actual: Float, window: ClosedFloatingPointRange<Float>, slack: Float) {
        assertTrue(
            actual >= window.start - slack && actual <= window.endInclusive + slack,
            "$what is $actual, outside ${window.start} to ${window.endInclusive}",
        )
    }

    @Test
    fun `the numbers arrive clear in the frame they are composed in, so nothing pops in at full strength`() {
        startIntro()
        val rest = rest()
        fresh(loadingCard())

        arrive()

        assertEquals(3, lines().size, "the three lines are composed as soon as the numbers arrive")
        alphas(rest).forEach { assertTrue(it < CLEAR, "a line is drawn at $it in the frame the numbers arrive") }
        rises(rest).forEach { assertTrue(it > 0.9f, "a line is already in its place in the frame the numbers arrive: $it") }
    }

    @Test
    fun `at 0, 100, 200 and 300 ms the lines are where the three beats put them, in reading order`() {
        startIntro()
        val rest = rest()
        fresh(loadingCard())
        val zero = arrive()

        listOf(0L, 100L, 200L, 300L).forEach { target ->
            framesUntil(zero, target)
            val elapsed = now() - zero
            val alphas = alphas(rest)
            val rises = rises(rest)
            repeat(3) { beat ->
                assertInside("the opacity of line ${beat + 1} at $elapsed ms", alphas[beat], opacityWindow(elapsed, beat), PIXEL_SLACK)
                assertInside("the rise left of line ${beat + 1} at $elapsed ms", rises[beat], riseWindow(elapsed, beat), OFFSET_SLACK)
            }
            assertTrue(
                alphas[0] + PIXEL_SLACK >= alphas[1] && alphas[1] + PIXEL_SLACK >= alphas[2],
                "the lines are not in reading order at $elapsed ms: $alphas",
            )
        }
    }

    @Test
    fun `at 100 ms the spots are well on their way and the time of the reading has not begun, which is staggered`() {
        startIntro()
        val rest = rest()
        fresh(loadingCard())
        val zero = arrive()

        framesUntil(zero, 100)
        val alphas = alphas(rest)
        val rises = rises(rest)

        assertTrue(alphas[0] > 0.2f && rises[0] < 0.9f, "the spots have not started at 100 ms: $alphas $rises")
        assertTrue(alphas[2] < CLEAR && rises[2] > 0.9f, "the time of the reading has started at 100 ms: $alphas $rises")
    }

    @Test
    fun `the card is complete 410 ms after the animation starts and the last line is still on its way at 300 ms`() {
        startIntro()
        val rest = rest()
        fresh(loadingCard())
        val zero = arrive()

        framesUntil(zero, 300)
        assertTrue(alphas(rest)[2] < COMPLETE, "the last line is complete at 300 ms")

        framesUntil(zero, START_LAG_MS + FIGURE_TOTAL_MS + FRAME_MS * 2)
        assertTrue(alphas(rest).all { it > COMPLETE }, "a line is still fading after the whole fade")
        assertTrue(rises(rest).all { abs(it) < OFFSET_SLACK }, "a line has not reached its place after the whole fade")
    }

    private fun assertCardKeepsItsHeight(widthDp: Int, fontScale: Float) {
        startIntro(widthDp, fontScale)
        val heights = mutableListOf(heightDp())
        val zero = arrive()
        heights += heightDp()
        while (now() - zero < FIGURE_TOTAL_MS + SETTLE_MS) {
            frame()
            heights += heightDp()
        }

        assertTrue(
            heights.all { abs(it - heights.first()) < HEIGHT_TOLERANCE_DP },
            "the card changes height as the numbers fade in at $widthDp dp and $fontScale: ${heights.distinct()}",
        )
    }

    @Test
    fun `the card is as high before, during and after the fade on a phone`() = assertCardKeepsItsHeight(widthDp = 411, fontScale = 1f)

    @Test
    fun `the card is as high before, during and after the fade at 320 dp with text at 200 percent`() =
        assertCardKeepsItsHeight(widthDp = 320, fontScale = 2f)

    @Test
    fun `the name and the tile are as they always were, drawn in full and in place in every frame of the fade`() {
        startIntro()
        val tile = composeRule.onAllNodes(hasText("P"), useUnmergedTree = true).fetchSemanticsNodes().single().boundsInRoot
        val name = real("Bella Vista").boundsInRoot
        val before = picture()
        val nameInk = ink(before, name)
        val tileFill = before.colourAt(tile.left - origin().left + 2f, tile.top - origin().top + 2f)

        val zero = arrive()
        while (now() - zero < FIGURE_TOTAL_MS + SETTLE_MS) {
            val at = now() - zero
            assertEquals(tile, composeRule.onAllNodes(hasText("P"), useUnmergedTree = true).fetchSemanticsNodes().single().boundsInRoot, "the tile moved at $at ms")
            assertEquals(name, real("Bella Vista").boundsInRoot, "the name moved at $at ms")
            val picture = picture()
            assertTrue(abs(ink(picture, name) - nameInk) < INK_TOLERANCE, "the name is not drawn in full at $at ms")
            assertEquals(tileFill, picture.colourAt(tile.left - origin().left + 2f, tile.top - origin().top + 2f), "the tile changed colour at $at ms")
            frame()
        }
    }

    @Test
    fun `nothing is measured again while the lines fade, so it is all draw phase`() {
        startCard(staged = true, keepReadingSpace = true)
        val zero = arrive()
        frame()
        frame()
        val before = measurePasses

        while (now() - zero < FIGURE_TOTAL_MS + SETTLE_MS) frame()

        assertEquals(before, measurePasses, "the card was measured ${measurePasses - before} more times during the fade")
    }

    @Test
    fun `only the first arrival fades, a refreshed reading replaces the text and is drawn in full in the next frame`() {
        startIntro()
        val rest = rest()
        fresh(loadingCard())
        val zero = arrive()
        framesUntil(zero, START_LAG_MS + FIGURE_TOTAL_MS + SETTLE_MS)

        arrive(readingCard(spots = 119, percent = 41, time = "8:10\u00A0AM"))

        val picture = picture()
        listOf("119", "41%", "Last updated at\u00A08:10\u00A0AM").forEachIndexed { index, text ->
            val node = real(text, substring = index == 2)
            assertTrue(ink(picture, node.boundsInRoot) / rest.ink[index] > COMPLETE, "$text fades again when it is refreshed")
            assertEquals(rest.tops[index], node.boundsInRoot.top, "$text rises again when it is refreshed")
        }
    }

    @Test
    fun `a card first drawn with its numbers is complete at once, as a restored one is`() {
        startIntro()
        val rest = rest()

        fresh(readingCard())

        assertTrue(alphas(rest).all { it > COMPLETE }, "a card drawn with its numbers is not complete in its first frames")
        assertEquals(rest.tops, tops(), "a card drawn with its numbers is not in place")
        val tops = tops()
        framesUntil(now(), FIGURE_TOTAL_MS.toLong())
        assertEquals(tops, tops(), "a restored card moves")
    }

    @Test
    fun `with reduced motion the three lines are one plain 150 ms fade together, with no rise and no stagger`() {
        startCard(staged = true, keepReadingSpace = true, reduceMotion = true)
        val rest = rest()
        fresh(loadingCard())
        val zero = arrive()

        val samples = mutableListOf<Triple<Long, List<Float>, List<Float>>>()
        while (now() - zero < FIGURE_TOTAL_MS) {
            samples += Triple(now() - zero, alphas(rest), rises(rest))
            frame()
        }

        samples.forEach { (at, alphas, rises) ->
            assertTrue(alphas.max() - alphas.min() < PIXEL_SLACK, "the lines are staggered under reduced motion at $at ms: $alphas")
            assertTrue(rises.all { abs(it) < OFFSET_SLACK }, "a line rises under reduced motion at $at ms: $rises")
        }
        assertTrue(samples.first { it.first >= 64 }.second.all { it in 0.1f..0.95f }, "the one fade is not under way 64 ms in")
        assertTrue(samples.first { it.first >= 200 }.second.all { it > COMPLETE }, "the one fade is not over at 200 ms")
    }

    @Test
    fun `the home card shows its numbers as it always did, with no rise, no live region and nothing merged by the card`() {
        startHome()
        val rest = rest()
        fresh(loadingCard())
        val zero = arrive()

        assertEquals(rest.tops, tops(), "a line of the home card is not in place the frame the numbers arrive")
        val picture = picture()
        assertTrue(ink(picture, real("120").boundsInRoot) > 0.2f, "the home card's figures are faded as they arrive")
        framesUntil(zero, FIGURE_TOTAL_MS.toLong())
        assertEquals(rest.tops, tops(), "a line of the home card moves as the numbers arrive")
        assertEquals(0, composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion)).fetchSemanticsNodes().size)
    }

    @Test
    fun `a card that keeps its room but does not stage its numbers shows them in one frame`() {
        startCard(staged = false, keepReadingSpace = true)
        val rest = rest()
        fresh(loadingCard())

        arrive()

        assertEquals(rest.tops, tops(), "the lines rise though they are not staged")
        assertTrue(alphas(rest).all { it > COMPLETE }, "the lines fade though they are not staged")
    }

    @Test
    fun `every car park of a station fades in on the same three beats`() {
        val three = station(
            ParkRideCarPark("26", "Tallawong P1", 120, 40, "8:05\u00A0AM"),
            ParkRideCarPark("27", "Tallawong P2", 48, 76, "8:05\u00A0AM"),
            ParkRideCarPark("28", "Tallawong P3", 210, 12, "8:05\u00A0AM"),
        )
        val names = station(
            ParkRideCarPark("26", "Tallawong P1"),
            ParkRideCarPark("27", "Tallawong P2"),
            ParkRideCarPark("28", "Tallawong P3"),
        )
        startIntro()
        fresh(three)
        val resting = listOf("120", "48", "210").map { real(it).boundsInRoot.top }
        fresh(names)
        val zero = arrive(three)

        framesUntil(zero, 100)
        val left = listOf("120", "48", "210").mapIndexed { index, text -> real(text).boundsInRoot.top - resting[index] }

        assertTrue(left.all { it > 0f }, "a car park's spots have no rise left to go, or none to start: $left")
        assertTrue(left.max() - left.min() < 0.5f, "the car parks do not arrive together: $left")
    }

    // The card's one merged node, the one that carries the live region and the words.
    private fun cardNode(): SemanticsNode = composeRule.onAllNodes(hasText("Bella Vista")).fetchSemanticsNodes().single()

    private fun <T> SemanticsNode.prop(key: SemanticsPropertyKey<T>): T? = if (config.contains(key)) config[key] else null

    private fun words(node: SemanticsNode): List<String> = node.prop(SemanticsProperties.Text).orEmpty().map { it.text }

    @Test
    fun `a screen reader gets the card as one node, polite once the numbers are in, and its words change once`() {
        startIntro()
        val live = SemanticsProperties.LiveRegion

        val before = cardNode()
        assertNull(before.prop(live), "the card is a live region before the numbers are in")
        assertEquals(listOf("Bella Vista"), words(before))

        val zero = arrive()
        val seen = mutableListOf(words(cardNode()))
        while (now() - zero < FIGURE_TOTAL_MS + SETTLE_MS) {
            frame()
            seen += words(cardNode())
        }

        val card = cardNode()
        assertEquals(LiveRegionMode.Polite, card.prop(live))
        assertEquals(listOf("Bella Vista", "120", "spots available", "40%", "full", "Last updated at\u00A08:05\u00A0AM"), words(card))
        assertEquals(1, seen.distinct().size, "the card's words change as the lines fade: ${seen.distinct()}")
        assertEquals(1, composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(live)).fetchSemanticsNodes().size, "more than one live region")
        assertEquals(1, composeRule.onAllNodes(hasText("120")).fetchSemanticsNodes().size, "the figures are a node of their own")
    }

    @Test
    fun `a card with no numbers is one node with the name and no live region`() {
        startIntro()
        frame()
        frame()

        assertEquals(listOf("Bella Vista"), words(cardNode()))
        assertEquals(0, composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion)).fetchSemanticsNodes().size)
    }

    private companion object {
        const val FRAME_MS = 16L
        const val SETTLE_MS = 500L

        // The animation clock takes its start time a frame after the frame the numbers are composed in.
        const val START_LAG_MS = 16L

        // A line counts as clear below this share of its strength, and as drawn above the other.
        const val CLEAR = 0.1f
        const val COMPLETE = 0.9f

        // Text is anti-aliased, so a share read from pixels is not exact.
        const val PIXEL_SLACK = 0.12f
        const val OFFSET_SLACK = 0.06f
        const val INK_TOLERANCE = 0.04f
        const val HEIGHT_TOLERANCE_DP = 0.5f
    }
}
