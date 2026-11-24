package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
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
import androidx.compose.ui.text.rememberTextMeasurer
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
import xyz.ksharma.krail.trip.planner.ui.components.cardColumnInUse
import xyz.ksharma.krail.trip.planner.ui.components.cardGrid
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The From and To rows of the Search Stop card, read from the real card (`docs/MAP_HOME_UX.md`, The box): a 20 dp marker column
 * at x16 beside the first line of the name, the text column at x44 at every size and width, a long name wrapping by whole words
 * onto lines that start at x44 (a hanging indent), and a row that is 56 dp or its lines plus 24 dp. A line that holds only a
 * marker must never exist, whatever the text size, the width or the name. The room the right column takes follows what the card
 * draws: with no swap and no go circle the text and the divider run to the card's right padding, 16 dp, and with them they stop 8 dp
 * before the column, 64 dp from the edge.
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

    private var screenOffersSwap by mutableStateOf(false)
    private lateinit var typed: TextFieldState
    private var nameWidthOnOneLine: (String) -> Dp = { 0.dp }

    private fun setCard(fontScale: Float = 1f) {
        fontScaleState = fontScale
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScaleState)) {
                PreviewTheme {
                    val measurer = rememberTextMeasurer()
                    val style = KrailTheme.typography.titleMedium
                    // The width of a name on one line in the row's own style, measured in the composition so the font
                    // scale is the card's.
                    nameWidthOnOneLine = { name ->
                        with(density) { measurer.measure(name, style, softWrap = false).size.width.toDp() }
                    }
                    typed = rememberTextFieldState()
                    Box(modifier = Modifier.fillMaxSize().background(KrailTheme.colors.surface).padding(gaps.edge)) {
                        SearchStopGroupCard(
                            form = SearchStopFormModel(
                                shape = FormShape.ToAndFrom,
                                editing = editing,
                                toPlaceholder = toStop?.stopName ?: TO_PLACEHOLDER,
                                fromPlaceholder = fromStop?.stopName ?: FROM_PLACEHOLDER,
                                toStop = toStop,
                                fromStop = fromStop,
                                fieldState = typed,
                                focusRequester = remember { FocusRequester() },
                                onFlowEvent = {},
                                onGoClick = {},
                                onTextChange = {},
                                fromLabel = fromLabel,
                                toLabel = toLabel,
                                // The screen offers the swap once the rider has picked a stop (`swapShown`).
                                onSwap = if (screenOffersSwap) ({}) else null,
                            ),
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun boundsOf(tag: String): DpRect = composeRule.onNodeWithTag(
        tag,
        useUnmergedTree = true,
    ).getUnclippedBoundsInRoot()

    private fun DpRect.cy() = top + height / 2f
    private fun DpRect.w() = right - left

    private fun assertNear(what: String, expected: Dp, actual: Dp, within: Float = 0.75f) {
        assertTrue(abs((expected - actual).value) <= within, "$what: expected $expected, got $actual")
    }

    private fun SemanticsNode.textLayouts(): List<Pair<SemanticsNode, TextLayoutResult>> {
        val own = mutableListOf<TextLayoutResult>().also {
            config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(it)
        }
        return own.map { this to it } + children.flatMap { it.textLayouts() }
    }

    private fun rowLines(tag: String): Int =
        composeRule.onNodeWithTag(
            tag,
            useUnmergedTree = true,
        ).fetchSemanticsNode().textLayouts().first().second.lineCount

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
        val textBounds = with(composeRule.density) {
            textNode.boundsInRoot.let { DpRect(it.left.toDp(), it.top.toDp(), it.right.toDp(), it.bottom.toDp()) }
        }

        val lines = layout.lineCount
        val firstLine = px2dp(layout.getLineBottom(0) - layout.getLineTop(0))
        val block = px2dp(layout.getLineBottom(lines - 1) - layout.getLineTop(0))

        assertNear("$label, the text starts at x44", 44.dp, textBounds.left - card.left)
        for (line in 0 until lines) {
            assertNear(
                "$label, line ${line + 1} starts at the text start, never under the marker",
                0.dp,
                px2dp(layout.getLineLeft(line)),
            )
        }
        assertTrue(
            layout.getLineRight(0) > layout.getLineLeft(0),
            "$label, the first line holds words: a marker never stands on a line of its own",
        )
        assertNear(
            "$label, the text block is centred in its row",
            row.top + (row.height - block) / 2f,
            textBounds.top,
            within = 1f,
        )
        assertNear("$label, the marker is centred on the first line", textBounds.top + firstLine / 2f, marker.cy())
        assertTrue(
            marker.cy() in (textBounds.top)..(textBounds.top + firstLine),
            "$label, the marker is on the first line, not above or below it",
        )
        assertTrue(marker.right <= card.left + 36.dp + 0.5.dp, "$label, the marker stays in its column, x16 to x36")
        assertTrue(marker.left >= card.left + 16.dp - 0.5.dp, "$label, the marker starts at x16 or later")
        assertNear(
            "$label, the row is 56 dp or 24 dp plus its lines",
            maxOf(56.dp, 24.dp + block),
            row.height,
            within = 1f,
        )
        return row.height to lines
    }

    /** The card is the two rows and the 1 dp divider, whatever the rows are. */
    private fun assertCardIsItsRows(label: String) {
        val from = boundsOf(TripPlannerTestTags.SEARCH_ROW_FROM).height
        val to = boundsOf(TripPlannerTestTags.SEARCH_ROW_TO).height
        assertNear(
            "$label, the card is the rows and the 1 dp divider",
            from + to + 1.dp,
            boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD).height,
        )
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
        assertNear(
            "the card is 113 dp, the approved picture's",
            113.dp,
            boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD).height,
        )
        val (_, fromLines) = assertRow(
            "From",
            TripPlannerTestTags.SEARCH_ROW_FROM,
            TripPlannerTestTags.SEARCH_ROW_ORIGIN_DOT,
        )
        val (_, toLines) = assertRow(
            "To",
            TripPlannerTestTags.SEARCH_ROW_TO,
            TripPlannerTestTags.SEARCH_ROW_DESTINATION_PIN,
        )
        assertEquals(1, fromLines)
        assertEquals(1, toLines)
    }

    @Test
    fun `at 411 dp and 130 percent the long name wraps onto a second line at x44 and the card grows with it`() {
        trip()
        setCard(fontScale = 1.3f)

        assertCardIsItsRows("411 dp, 130 percent")
        val (_, fromLines) = assertRow(
            "From",
            TripPlannerTestTags.SEARCH_ROW_FROM,
            TripPlannerTestTags.SEARCH_ROW_ORIGIN_DOT,
        )
        assertRow("To", TripPlannerTestTags.SEARCH_ROW_TO, TripPlannerTestTags.SEARCH_ROW_DESTINATION_PIN)
        assertEquals(2, fromLines, "The long name wraps by whole words onto two lines.")
    }

    @Test
    fun `at 411 dp and 150 percent the long name is two lines, the To row one, and the card is its rows`() {
        trip()
        setCard(fontScale = 1.5f)

        assertCardIsItsRows("411 dp, 150 percent")
        assertEquals(
            2,
            rowLines(TripPlannerTestTags.SEARCH_ROW_FROM),
            "The long name is on two lines and the To row on one.",
        )
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
        val (_, fromLines) = assertRow(
            "From",
            TripPlannerTestTags.SEARCH_ROW_FROM,
            TripPlannerTestTags.SEARCH_ROW_ORIGIN_DOT,
        )
        val (_, toLines) = assertRow(
            "To",
            TripPlannerTestTags.SEARCH_ROW_TO,
            TripPlannerTestTags.SEARCH_ROW_DESTINATION_PIN,
        )
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
            LONG_NAME to 1f,
            LONG_NAME to 1.3f,
            LONG_NAME to 1.5f,
            TOWN_HALL to 1f,
            TOWN_HALL to 1.5f,
            SEVEN_HILLS to 1.3f,
            SEVEN_HILLS to 1.5f,
        )) {
            composeRule.runOnIdle {
                fromStop = name
                toStop = TOWN_HALL
                fontScaleState = scale
            }
            composeRule.waitForIdle()
            assertRow(
                "From ${name.stopName} at $scale",
                TripPlannerTestTags.SEARCH_ROW_FROM,
                TripPlannerTestTags.SEARCH_ROW_ORIGIN_DOT,
            )
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
            assertRow(
                "From prompt, editing $field",
                TripPlannerTestTags.SEARCH_ROW_FROM,
                TripPlannerTestTags.SEARCH_ROW_ORIGIN_DOT,
            )
            assertRow(
                "To prompt, editing $field",
                TripPlannerTestTags.SEARCH_ROW_TO,
                TripPlannerTestTags.SEARCH_ROW_DESTINATION_PIN,
            )
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
        val stopText = composeRule.onNodeWithText(
            "(${LONG_NAME.stopName})",
            useUnmergedTree = true,
        ).getUnclippedBoundsInRoot()
        val fromIcon = icons.minBy { it.top }
        assertNear("the label starts at x44", 44.dp, homeText.left - card.left)
        assertNear("the icon is centred on the label's line, the first", homeText.cy(), fromIcon.cy())
        assertTrue(
            stopText.left - card.left >= 44.dp - 0.5.dp,
            "the brackets never go under the icon: ${stopText.left - card.left}",
        )
        assertTrue(stopText.top >= homeText.top - 0.5.dp, "the brackets are never above the label")
        assertTrue(fromIcon.cy() < from.top + from.height / 2f + 0.5.dp)
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesIcon(): List<DpRect> =
        onAllNodes(
            androidx.compose.ui.test.hasTestTag(TripPlannerTestTags.SEARCH_ROW_LABEL_ICON),
            useUnmergedTree = true,
        )
            .fetchSemanticsNodes()
            .map { node ->
                with(density) {
                    node.boundsInRoot.let { DpRect(it.left.toDp(), it.top.toDp(), it.right.toDp(), it.bottom.toDp()) }
                }
            }

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

    // region the room the right column takes

    private fun card() = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)

    /** Where a row's idle text column ends, in the root, in dp. */
    private fun textRight(tag: String): Dp {
        val (node, _) = composeRule.onNodeWithTag(
            tag,
            useUnmergedTree = true,
        ).fetchSemanticsNode().textLayouts().first()
        return with(composeRule.density) { node.boundsInRoot.right.toDp() }
    }

    /** Where the longest line of a row's name ends, in the root, in dp. */
    private fun longestLineRight(tag: String): Dp {
        val (node, layout) = composeRule.onNodeWithTag(
            tag,
            useUnmergedTree = true,
        ).fetchSemanticsNode().textLayouts().first()
        val left = with(composeRule.density) { node.boundsInRoot.left.toDp() }
        return left + px2dp((0 until layout.lineCount).maxOf { layout.getLineRight(it) })
    }

    private fun dividerRoom(): Dp = card().right - boundsOf(TripPlannerTestTags.SEARCH_ROW_DIVIDER).right

    private fun fromLayout(): TextLayoutResult =
        composeRule.onNodeWithTag(
            TripPlannerTestTags.SEARCH_ROW_FROM,
            useUnmergedTree = true,
        ).fetchSemanticsNode().textLayouts().first().second

    /** The list state of the operator's picture: a start that filled itself in, nothing picked, the To row being edited. */
    private fun listState(name: StopItem) {
        fromStop = name
        toStop = null
        editing = ToFromField.To
        screenOffersSwap = false
    }

    @Test
    fun `the room is 64 dp with the column in use and 16 dp, the card's right padding, without`() {
        val grid = cardGrid()
        assertEquals(64.dp, grid.endRoom(columnInUse = true))
        assertEquals(16.dp, grid.endRoom(columnInUse = false))
        assertEquals(grid.startPad, grid.endRoom(columnInUse = false), "The end padding is the start padding, 16.")
        assertTrue(cardColumnInUse(hasSwap = true, showGo = false))
        assertTrue(cardColumnInUse(hasSwap = false, showGo = true))
        assertTrue(cardColumnInUse(hasSwap = true, showGo = true))
        assertTrue(!cardColumnInUse(hasSwap = false, showGo = false))
    }

    @Test
    fun `with nothing in the right column the name and the divider run to the card's right padding, 16 dp from the edge`() {
        listState(LONG_NAME)
        setCard()

        assertNear(
            "the From name's column ends 16 dp from the card's right edge",
            16.dp,
            card().right - textRight(TripPlannerTestTags.SEARCH_ROW_FROM),
        )
        assertNear("the divider ends 16 dp from it", 16.dp, dividerRoom())
        assertNear(
            "the To prompt's field too",
            16.dp,
            card().right - boundsOf(TripPlannerTestTags.SEARCH_STOP_QUERY_FIELD).right,
        )
    }

    @Test
    fun `once a stop is picked the swap's column is kept and the name and the divider stop 64 dp from the edge`() {
        fromStop = LONG_NAME
        toStop = null
        editing = ToFromField.To
        screenOffersSwap = true
        setCard()

        assertNear(
            "the name's column ends 64 dp from the card's right edge",
            64.dp,
            card().right - textRight(TripPlannerTestTags.SEARCH_ROW_FROM),
        )
        assertNear("the divider ends 64 dp from it", 64.dp, dividerRoom())
    }

    @Test
    fun `with both stops and the go circle the room is 64 dp, with or without the swap offered`() {
        trip()
        setCard()
        assertNear("go circle, no swap offered, the divider", 64.dp, dividerRoom())
        assertNear("and the From name", 64.dp, card().right - textRight(TripPlannerTestTags.SEARCH_ROW_FROM))
        assertNear("and the To name", 64.dp, card().right - textRight(TripPlannerTestTags.SEARCH_ROW_TO))

        composeRule.runOnIdle { screenOffersSwap = true }
        composeRule.waitForIdle()
        assertNear("go circle and swap, the divider", 64.dp, dividerRoom())
        assertNear("and the From name", 64.dp, card().right - textRight(TripPlannerTestTags.SEARCH_ROW_FROM))
        assertNear("and the To name", 64.dp, card().right - textRight(TripPlannerTestTags.SEARCH_ROW_TO))
    }

    private fun assertWrappedNameKeepsEndPadding(label: String, fontScale: Float) {
        listState(JASPER)
        setCard(fontScale = fontScale)

        val layout = fromLayout()
        assertTrue(layout.lineCount >= 2, "$label, the name wraps: ${layout.lineCount} lines")
        val limit = card().right - 16.dp
        assertTrue(
            longestLineRight(TripPlannerTestTags.SEARCH_ROW_FROM) <= limit + 0.5.dp,
            "$label, the longest line ends ${longestLineRight(
                TripPlannerTestTags.SEARCH_ROW_FROM,
            )}, no closer than 16 dp to the edge ($limit)",
        )
        assertNear(
            "$label, the text column ends 16 dp from the card's right edge",
            16.dp,
            card().right - textRight(TripPlannerTestTags.SEARCH_ROW_FROM),
        )
        assertNear("$label, the divider ends exactly 16 dp before it", 16.dp, dividerRoom())
        assertRow("$label, From", TripPlannerTestTags.SEARCH_ROW_FROM, TripPlannerTestTags.SEARCH_ROW_ORIGIN_DOT)
        assertCardIsItsRows(label)
    }

    @Test
    fun `at 411 dp and 100 percent a wrapped name ends at least 16 dp before the card's edge and the divider exactly 16 dp`() =
        assertWrappedNameKeepsEndPadding("411 dp, 100 percent", fontScale = 1f)

    @Test
    fun `at 411 dp and 130 percent a wrapped name ends at least 16 dp before the card's edge and the divider exactly 16 dp`() =
        assertWrappedNameKeepsEndPadding("411 dp, 130 percent", fontScale = 1.3f)

    @Test
    @Config(qualifiers = "w320dp-h640dp-notnight-xxhdpi")
    fun `at 320 dp and 200 percent a wrapped name ends at least 16 dp before the card's edge and the divider exactly 16 dp`() =
        assertWrappedNameKeepsEndPadding("320 dp, 200 percent", fontScale = 2f)

    @Test
    fun `the same name wraps at the width the drawn controls leave, so it uses the room the missing swap gives it`() {
        listState(LONG_NAME)
        setCard(fontScale = 1.3f)
        val name = LONG_NAME.stopName
        val natural = composeRule.runOnIdle { nameWidthOnOneLine(name) }
        val withoutSwap = fromLayout().lineCount
        val widthWithout = card().w() - 44.dp - 16.dp

        composeRule.runOnIdle { screenOffersSwap = true }
        composeRule.waitForIdle()
        val withSwap = fromLayout().lineCount
        val widthWith = card().w() - 44.dp - 64.dp

        println(
            "ROOM 130 percent natural=$natural without=$widthWithout lines=$withoutSwap with=$widthWith lines=$withSwap",
        )
        assertEquals(
            natural <= widthWithout,
            withoutSwap == 1,
            "No swap: $natural against a column of $widthWithout wraps to $withoutSwap lines",
        )
        assertEquals(
            natural <= widthWith,
            withSwap == 1,
            "Swap: $natural against a column of $widthWith wraps to $withSwap lines",
        )
        assertTrue(withoutSwap <= withSwap, "The name never wraps more with the room than without it.")
    }

    @Test
    fun `the list state's long name keeps its marker on the first line at every size, with the full width`() {
        listState(LONG_NAME)
        setCard()
        for (scale in listOf(1f, 1.3f, 1.5f)) {
            composeRule.runOnIdle { fontScaleState = scale }
            composeRule.waitForIdle()
            assertRow(
                "From, list state, $scale",
                TripPlannerTestTags.SEARCH_ROW_FROM,
                TripPlannerTestTags.SEARCH_ROW_ORIGIN_DOT,
            )
            assertRow(
                "To, list state, $scale",
                TripPlannerTestTags.SEARCH_ROW_TO,
                TripPlannerTestTags.SEARCH_ROW_DESTINATION_PIN,
            )
            assertCardIsItsRows("list state, $scale")
        }
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-notnight-xxhdpi")
    fun `at 320 dp and 200 percent the list state's name keeps its marker on the first line`() {
        listState(LONG_NAME)
        setCard(fontScale = 2f)

        assertRow("From", TripPlannerTestTags.SEARCH_ROW_FROM, TripPlannerTestTags.SEARCH_ROW_ORIGIN_DOT)
        assertRow("To", TripPlannerTestTags.SEARCH_ROW_TO, TripPlannerTestTags.SEARCH_ROW_DESTINATION_PIN)
        assertCardIsItsRows("320 dp, 200 percent, list state")
    }

    @Test
    fun `typing in the To row moves nothing once a stop is picked, and only the typed row keeps room for the X before that`() {
        // A stop picked: the column is the swap's, so typing, which hides the swap, changes no room.
        fromStop = LONG_NAME
        toStop = null
        editing = ToFromField.To
        screenOffersSwap = true
        setCard()
        val fromBefore = textRight(TripPlannerTestTags.SEARCH_ROW_FROM)
        val dividerBefore = dividerRoom()
        composeRule.runOnIdle { typed.setTextAndPlaceCursorAtEnd("Cen") }
        composeRule.waitForIdle()
        assertNear("the From name's column is where it was", fromBefore, textRight(TripPlannerTestTags.SEARCH_ROW_FROM))
        assertNear("the divider is where it was", dividerBefore, dividerRoom())
        assertNear(
            "and the typed field leaves room for the X",
            64.dp,
            card().right - boundsOf(TripPlannerTestTags.SEARCH_STOP_QUERY_FIELD).right,
        )

        // Nothing picked: the From name and the divider keep the full width and the typed row alone makes room for its X.
        composeRule.runOnIdle {
            typed.setTextAndPlaceCursorAtEnd("")
            screenOffersSwap = false
        }
        composeRule.waitForIdle()
        val fromFull = textRight(TripPlannerTestTags.SEARCH_ROW_FROM)
        val dividerFull = dividerRoom()
        assertNear("nothing picked, the divider runs to 16", 16.dp, dividerFull)
        assertNear(
            "and the empty field to 16 too",
            16.dp,
            card().right - boundsOf(TripPlannerTestTags.SEARCH_STOP_QUERY_FIELD).right,
        )
        composeRule.runOnIdle { typed.setTextAndPlaceCursorAtEnd("Cen") }
        composeRule.waitForIdle()
        assertNear("typing keeps the From name's column", fromFull, textRight(TripPlannerTestTags.SEARCH_ROW_FROM))
        assertNear("typing keeps the divider", dividerFull, dividerRoom())
        assertNear(
            "the typed row stops before its X",
            64.dp,
            card().right - boundsOf(TripPlannerTestTags.SEARCH_STOP_QUERY_FIELD).right,
        )
    }

    @Test
    fun `the room follows the pick in the same frame and never the swap's fade`() {
        listState(LONG_NAME)
        setCard()
        val linesBefore = fromLayout().lineCount
        assertNear("before the pick the divider runs to 16", 16.dp, dividerRoom())

        composeRule.mainClock.autoAdvance = false
        composeRule.runOnUiThread {
            toStop = TOWN_HALL
            screenOffersSwap = true
            editing = null
            Snapshot.sendApplyNotifications()
        }
        composeRule.mainClock.advanceTimeByFrame()
        val firstFrame = dividerRoom()
        val firstName = textRight(TripPlannerTestTags.SEARCH_ROW_FROM)
        composeRule.mainClock.advanceTimeBy(400)

        assertNear("the first frame already holds the final room, 64", 64.dp, firstFrame)
        assertNear("and so does the From name", 64.dp, card().right - firstName)
        assertNear("nothing moves while the swap fades and glides", firstFrame, dividerRoom())
        assertNear(
            "the name's column is where the first frame put it",
            firstName,
            textRight(TripPlannerTestTags.SEARCH_ROW_FROM),
        )
        assertEquals(
            linesBefore,
            fromLayout().lineCount,
            "At 100 percent the name is on one line before and after the pick.",
        )
        assertNear("the card is 113 dp after the pick, the picture's", 113.dp, card().height)
    }

    @Test
    fun `the map state numbers are the same with the real swap offered, rows 56 and 56 and the card 113 at 411 dp and 100 percent`() {
        trip()
        screenOffersSwap = true
        setCard()

        assertNear("From row", 56.dp, boundsOf(TripPlannerTestTags.SEARCH_ROW_FROM).height)
        assertNear("To row", 56.dp, boundsOf(TripPlannerTestTags.SEARCH_ROW_TO).height)
        assertNear("the card", 113.dp, card().height)
        assertEquals(1, rowLines(TripPlannerTestTags.SEARCH_ROW_FROM))
        assertEquals(1, rowLines(TripPlannerTestTags.SEARCH_ROW_TO))
    }

    @Test
    fun `the card is as tall with none, one or two stops chosen, the swap offered as the screen does`() {
        fromStop = null
        toStop = null
        setCard()
        for (scale in listOf(1f, 1.5f)) {
            composeRule.runOnIdle {
                fontScaleState = scale
                fromStop = null
                toStop = null
                screenOffersSwap = false
            }
            composeRule.waitForIdle()
            val none = card().height

            composeRule.runOnIdle {
                fromStop = CENTRAL
                screenOffersSwap = true
            }
            composeRule.waitForIdle()
            val one = card().height

            composeRule.runOnIdle { toStop = TOWN_HALL }
            composeRule.waitForIdle()
            val two = card().height

            assertEquals(none, one, "At $scale one stop chosen does not change the card's height.")
            assertEquals(none, two, "At $scale two stops chosen does not change the card's height.")
        }
    }

    // endregion

    private companion object {
        val LONG_NAME = StopItem(stopName = "Blacktown Rd before Metella Rd", stopId = "200100")
        val TOWN_HALL = StopItem(stopName = "Town Hall Station", stopId = "200070")
        val CENTRAL = StopItem(stopName = "Central Station", stopId = "200060")
        val SEVEN_HILLS = StopItem(stopName = "Seven Hills Station", stopId = "200200")
        val JASPER = StopItem(stopName = "Jasper Road Public School, Seven Hills Rd near Abbott Rd", stopId = "200300")
    }
}
