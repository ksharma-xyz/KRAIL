package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.FROM_PLACEHOLDER
import xyz.ksharma.krail.trip.planner.ui.components.TO_PLACEHOLDER
import xyz.ksharma.krail.trip.planner.ui.components.ToFromField
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The From and To rows of the Search Stop card, read from the real card (`docs/MAP_HOME_UX.md`, The box): a 20 dp marker column
 * at x16 beside the first line of the name, the text column at x44 at every size and width, a long name wrapping by whole words
 * onto lines that start at x44 (a hanging indent), and a row that is 56 dp or its lines plus 24 dp. A line that holds only a
 * marker must never exist, whatever the text size, the width or the name.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-notnight-xxhdpi", manifest = Config.NONE)
class SearchStopCardRowsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val gaps = searchStopGaps()

    private var fromStop by mutableStateOf<StopItem?>(null)
    private var toStop by mutableStateOf<StopItem?>(null)
    private var fromLabel by mutableStateOf<String?>(null)
    private var toLabel by mutableStateOf<String?>(null)
    private var editing by mutableStateOf<ToFromField?>(null)

    private var fontScaleState by mutableStateOf(1f)

    private fun setCard(fontScale: Float = 1f) {
        fontScaleState = fontScale
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScaleState)) {
                PreviewTheme {
                    Box(modifier = Modifier.fillMaxSize().background(KrailTheme.colors.surface).padding(gaps.edge)) {
                        SearchStopGroupCard(
                            form = SearchStopFormModel(
                                shape = FormShape.ToAndFrom,
                                editing = editing,
                                toPlaceholder = toStop?.stopName ?: TO_PLACEHOLDER,
                                fromPlaceholder = fromStop?.stopName ?: FROM_PLACEHOLDER,
                                toStop = toStop,
                                fromStop = fromStop,
                                fieldState = rememberTextFieldState(),
                                focusRequester = remember { FocusRequester() },
                                onFlowEvent = {},
                                onGoClick = {},
                                onTextChange = {},
                                fromLabel = fromLabel,
                                toLabel = toLabel,
                            ),
                            onBack = {},
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun boundsOf(tag: String): DpRect = composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun DpRect.cy() = top + height / 2f
    private fun DpRect.w() = right - left

    private fun assertNear(what: String, expected: Dp, actual: Dp, within: Float = 0.75f) {
        assertTrue(abs((expected - actual).value) <= within, "$what: expected $expected, got $actual")
    }

    private fun SemanticsNode.textLayouts(): List<Pair<SemanticsNode, TextLayoutResult>> {
        val own = mutableListOf<TextLayoutResult>().also { config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(it) }
        return own.map { this to it } + children.flatMap { it.textLayouts() }
    }

    private fun rowLines(tag: String): Int =
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().textLayouts().first().second.lineCount

    private fun px2dp(px: Float): Dp = with(composeRule.density) { px.toDp() }

    /**
     * The one rule of the card's rows, asserted for [tag] with its [markerTag]: the marker is in its 20 dp column at x16, the text
     * starts at x44, the marker is centred on the first line of the text (so there is a word on the line it stands on, and no line
     * is above it), every line of the text starts at x44, and the row is 56 dp or 24 dp plus its lines, whichever is taller.
     * Returns the row's height and its number of lines.
     */
    private fun assertRow(label: String, tag: String, markerTag: String): Pair<Dp, Int> {
        val card = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        val row = boundsOf(tag)
        val marker = boundsOf(markerTag)
        val rowNode = composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
        val (textNode, layout) = rowNode.textLayouts().first()
        val textBounds = with(composeRule.density) { textNode.boundsInRoot.let { DpRect(it.left.toDp(), it.top.toDp(), it.right.toDp(), it.bottom.toDp()) } }

        val lines = layout.lineCount
        val firstLine = px2dp(layout.getLineBottom(0) - layout.getLineTop(0))
        val block = px2dp(layout.getLineBottom(lines - 1) - layout.getLineTop(0))

        assertNear("$label, the text starts at x44", 44.dp, textBounds.left - card.left)
        for (line in 0 until lines) {
            assertNear("$label, line ${line + 1} starts at the text start, never under the marker", 0.dp, px2dp(layout.getLineLeft(line)))
        }
        assertTrue(layout.getLineRight(0) > layout.getLineLeft(0), "$label, the first line holds words: a marker never stands on a line of its own")
        assertNear("$label, the text block is centred in its row", row.top + (row.height - block) / 2f, textBounds.top, within = 1f)
        assertNear("$label, the marker is centred on the first line", textBounds.top + firstLine / 2f, marker.cy())
        assertTrue(marker.cy() in (textBounds.top)..(textBounds.top + firstLine), "$label, the marker is on the first line, not above or below it")
        assertTrue(marker.right <= card.left + 36.dp + 0.5.dp, "$label, the marker stays in its column, x16 to x36")
        assertTrue(marker.left >= card.left + 16.dp - 0.5.dp, "$label, the marker starts at x16 or later")
        assertNear("$label, the row is 56 dp or 24 dp plus its lines", maxOf(56.dp, 24.dp + block), row.height, within = 1f)
        return row.height to lines
    }

    /** The card is the two rows and the 1 dp divider, whatever the rows are. */
    private fun assertCardIsItsRows(label: String) {
        val from = boundsOf(TripPlannerTestTags.SEARCH_ROW_FROM).height
        val to = boundsOf(TripPlannerTestTags.SEARCH_ROW_TO).height
        assertNear("$label, the card is the rows and the 1 dp divider", from + to + 1.dp, boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD).height)
    }

    private fun trip() {
        fromStop = LONG_NAME
        toStop = TOWN_HALL
    }

    @Test
    fun `at 411 dp and 100 percent the rows are 56 and 56 and the card is 113, each name on one line with its marker`() {
        trip()
        setCard()

        assertNear("From row", 56.dp, boundsOf(TripPlannerTestTags.SEARCH_ROW_FROM).height)
        assertNear("To row", 56.dp, boundsOf(TripPlannerTestTags.SEARCH_ROW_TO).height)
        assertNear("the card is 113 dp, the approved picture's", 113.dp, boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD).height)
        val (_, fromLines) = assertRow("From", TripPlannerTestTags.SEARCH_ROW_FROM, TripPlannerTestTags.SEARCH_ROW_ORIGIN_DOT)
        val (_, toLines) = assertRow("To", TripPlannerTestTags.SEARCH_ROW_TO, TripPlannerTestTags.SEARCH_ROW_DESTINATION_PIN)
        assertEquals(1, fromLines)
        assertEquals(1, toLines)
    }

    @Test
    fun `at 411 dp and 130 percent the long name wraps onto a second line at x44 and the card grows with it`() {
        trip()
        setCard(fontScale = 1.3f)

        assertCardIsItsRows("411 dp, 130 percent")
        val (_, fromLines) = assertRow("From", TripPlannerTestTags.SEARCH_ROW_FROM, TripPlannerTestTags.SEARCH_ROW_ORIGIN_DOT)
        assertRow("To", TripPlannerTestTags.SEARCH_ROW_TO, TripPlannerTestTags.SEARCH_ROW_DESTINATION_PIN)
        assertEquals(2, fromLines, "The long name wraps by whole words onto two lines.")
    }

    @Test
    fun `at 411 dp and 150 percent the long name is two lines, the To row one, and the card is its rows`() {
        trip()
        setCard(fontScale = 1.5f)

        assertCardIsItsRows("411 dp, 150 percent")
        assertEquals(2, rowLines(TripPlannerTestTags.SEARCH_ROW_FROM), "The long name is on two lines and the To row on one.")
        assertEquals(1, rowLines(TripPlannerTestTags.SEARCH_ROW_TO))
        assertRow("From", TripPlannerTestTags.SEARCH_ROW_FROM, TripPlannerTestTags.SEARCH_ROW_ORIGIN_DOT)
        assertRow("To", TripPlannerTestTags.SEARCH_ROW_TO, TripPlannerTestTags.SEARCH_ROW_DESTINATION_PIN)
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-notnight-xxhdpi")
    fun `at 320 dp and 200 percent the long names are 3 and 2 lines, the card is its rows and no line holds only a marker`() {
        trip()
        setCard(fontScale = 2f)

        assertCardIsItsRows("320 dp, 200 percent")
        val (_, fromLines) = assertRow("From", TripPlannerTestTags.SEARCH_ROW_FROM, TripPlannerTestTags.SEARCH_ROW_ORIGIN_DOT)
        val (_, toLines) = assertRow("To", TripPlannerTestTags.SEARCH_ROW_TO, TripPlannerTestTags.SEARCH_ROW_DESTINATION_PIN)
        assertEquals(3, fromLines)
        assertEquals(2, toLines)
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-notnight-xxhdpi")
    fun `with one stop chosen the To prompt wraps and keeps its marker on the first line`() {
        fromStop = LONG_NAME
        toStop = null
        setCard(fontScale = 2f)

        assertRow("From", TripPlannerTestTags.SEARCH_ROW_FROM, TripPlannerTestTags.SEARCH_ROW_ORIGIN_DOT)
        assertRow("To, a prompt", TripPlannerTestTags.SEARCH_ROW_TO, TripPlannerTestTags.SEARCH_ROW_DESTINATION_PIN)
    }

    @Test
    fun `every name of the matrix keeps its marker on the first line at every size and width`() {
        trip()
        setCard()
        for ((name, scale) in listOf(
            LONG_NAME to 1f, LONG_NAME to 1.3f, LONG_NAME to 1.5f, TOWN_HALL to 1f, TOWN_HALL to 1.5f,
            SEVEN_HILLS to 1.3f, SEVEN_HILLS to 1.5f,
        )) {
            composeRule.runOnIdle {
                fromStop = name
                toStop = TOWN_HALL
                fontScaleState = scale
            }
            composeRule.waitForIdle()
            assertRow("From ${name.stopName} at $scale", TripPlannerTestTags.SEARCH_ROW_FROM, TripPlannerTestTags.SEARCH_ROW_ORIGIN_DOT)
            assertRow("To at $scale", TripPlannerTestTags.SEARCH_ROW_TO, TripPlannerTestTags.SEARCH_ROW_DESTINATION_PIN)
        }
    }

    @Test
    fun `the prompts keep their marker on the first line too, at rest and while editing`() {
        fromStop = null
        toStop = null
        setCard(fontScale = 1.5f)
        for (field in listOf<ToFromField?>(null, ToFromField.From, ToFromField.To)) {
            composeRule.runOnIdle { editing = field }
            composeRule.waitForIdle()
            assertRow("From prompt, editing $field", TripPlannerTestTags.SEARCH_ROW_FROM, TripPlannerTestTags.SEARCH_ROW_ORIGIN_DOT)
            assertRow("To prompt, editing $field", TripPlannerTestTags.SEARCH_ROW_TO, TripPlannerTestTags.SEARCH_ROW_DESTINATION_PIN)
        }
    }

    @Test
    fun `a labelled stop reads the label in bold then the stop in brackets, its icon on the first line and the brackets on a hanging line`() {
        fromStop = LONG_NAME
        fromLabel = "Home"
        toStop = TOWN_HALL
        toLabel = "Work"
        setCard(fontScale = 1.5f)

        val card = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        val from = boundsOf(TripPlannerTestTags.SEARCH_ROW_FROM)
        val icons = composeRule.onAllNodesIcon()
        assertEquals(2, icons.size, "Both labelled rows draw their label's own icon.")
        val homeText = composeRule.onNodeWithText("Home", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val stopText = composeRule.onNodeWithText("(${LONG_NAME.stopName})", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val fromIcon = icons.minBy { it.top }
        assertNear("the label starts at x44", 44.dp, homeText.left - card.left)
        assertNear("the icon is centred on the label's line, the first", homeText.cy(), fromIcon.cy())
        assertTrue(stopText.left - card.left >= 44.dp - 0.5.dp, "the brackets never go under the icon: ${stopText.left - card.left}")
        assertTrue(stopText.top >= homeText.top - 0.5.dp, "the brackets are never above the label")
        assertTrue(fromIcon.cy() < from.top + from.height / 2f + 0.5.dp)
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesIcon(): List<DpRect> =
        onAllNodes(androidx.compose.ui.test.hasTestTag(TripPlannerTestTags.SEARCH_ROW_LABEL_ICON), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .map { node -> with(density) { node.boundsInRoot.let { DpRect(it.left.toDp(), it.top.toDp(), it.right.toDp(), it.bottom.toDp()) } } }

    @Test
    fun `the card is as tall with none, one or two stops chosen, at 100 and 150 percent`() {
        fromStop = null
        toStop = null
        setCard()
        for (scale in listOf(1f, 1.5f)) {
            composeRule.runOnIdle {
                fontScaleState = scale
                fromStop = null
                toStop = null
            }
            composeRule.waitForIdle()
            val none = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD).height

            composeRule.runOnIdle { fromStop = CENTRAL }
            composeRule.waitForIdle()
            val one = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD).height

            composeRule.runOnIdle { toStop = TOWN_HALL }
            composeRule.waitForIdle()
            val two = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD).height

            assertEquals(none, one, "At $scale one stop chosen does not change the card's height.")
            assertEquals(none, two, "At $scale two stops chosen does not change the card's height.")
        }
    }

    private companion object {
        val LONG_NAME = StopItem(stopName = "Blacktown Rd before Metella Rd", stopId = "200100")
        val TOWN_HALL = StopItem(stopName = "Town Hall Station", stopId = "200070")
        val CENTRAL = StopItem(stopName = "Central Station", stopId = "200060")
        val SEVEN_HILLS = StopItem(stopName = "Seven Hills Station", stopId = "200200")
    }
}
