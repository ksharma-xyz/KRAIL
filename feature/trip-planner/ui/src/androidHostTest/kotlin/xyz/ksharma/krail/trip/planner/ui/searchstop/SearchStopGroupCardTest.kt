package xyz.ksharma.krail.trip.planner.ui.searchstop

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
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
import xyz.ksharma.krail.taj.theme.md_theme_light_softLabel
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.FROM_PLACEHOLDER
import xyz.ksharma.krail.trip.planner.ui.components.TO_PLACEHOLDER
import xyz.ksharma.krail.trip.planner.ui.components.ToFromField
import xyz.ksharma.krail.trip.planner.ui.components.mapCardColors
import xyz.ksharma.krail.trip.planner.ui.components.softFieldColors
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The group box at the top of Search Stop, read from the real box: the Timetable's own From and To card, one rounded box in
 * the soft theme tint with two plain rows and a 1 dp divider, no back arrow and no glow. The grid numbers are the approved
 * picture's (`docs/MAP_HOME_UX.md`, The box), asserted at 411 dp at 100 and 150 percent text and at 320 dp at 200 percent. The
 * lone pill, for a single field, is the bare pill it was.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-notnight", manifest = Config.NONE)
class SearchStopGroupCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val gaps = searchStopGaps()

    private var surface = Color.Unspecified
    private var pillFill = Color.Unspecified
    private var tint = Color.Unspecified
    private var edge = Color.Unspecified
    private var modeSoftLabel = Color.Unspecified
    private var composeView: View? = null

    private var toStopState by mutableStateOf<StopItem?>(null)
    private var fromStopState by mutableStateOf<StopItem?>(null)
    private var fromLabelState by mutableStateOf<String?>(null)
    private var toLabelState by mutableStateOf<String?>(null)
    private var editingState by mutableStateOf<ToFromField?>(ToFromField.To)
    private lateinit var typed: androidx.compose.foundation.text.input.TextFieldState

    private fun setBox(
        fontScale: Float? = null,
        toStop: StopItem? = null,
        fromStop: StopItem? = null,
        fromLabel: String? = null,
        toLabel: String? = null,
        editing: ToFromField? = ToFromField.To,
        onGo: () -> Unit = {},
    ) {
        toStopState = toStop
        fromStopState = fromStop
        fromLabelState = fromLabel
        toLabelState = toLabel
        editingState = editing
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides if (fontScale != null) Density(density.density, fontScale) else density,
            ) {
                PreviewTheme {
                    composeView = LocalView.current
                    surface = KrailTheme.colors.surface
                    pillFill = softFieldColors().containerColor
                    tint = mapCardColors().tint
                    edge = mapCardColors().chipEdge
                    modeSoftLabel = KrailTheme.colors.softLabel
                    typed = rememberTextFieldState()
                    Box(modifier = Modifier.fillMaxSize().background(surface).padding(gaps.edge)) {
                        SearchStopGroupCard(
                            form = SearchStopFormModel(
                                shape = FormShape.ToAndFrom,
                                editing = editingState,
                                toPlaceholder = toStopState?.stopName ?: TO_PLACEHOLDER,
                                fromPlaceholder = fromStopState?.stopName ?: FROM_PLACEHOLDER,
                                toStop = toStopState,
                                fromStop = fromStopState,
                                fieldState = typed,
                                focusRequester = remember { FocusRequester() },
                                onFlowEvent = {},
                                onGoClick = onGo,
                                onTextChange = {},
                                fromLabel = fromLabelState,
                                toLabel = toLabelState,
                                // The screen offers the swap once the rider has picked a stop (`swapShown`), so the right column
                                // is the card's while it holds one, with or without the go circle.
                                onSwap = if (fromStopState != null || toStopState != null) ({}) else null,
                            ),
                            onBack = {},
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    /** A single field, the shape a label is chosen in: one bar with a fixed [prompt] and no stop held. */
    private fun setSingleBar(fontScale: Float? = null) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides if (fontScale != null) Density(density.density, fontScale) else density,
            ) {
                PreviewTheme {
                    composeView = LocalView.current
                    surface = KrailTheme.colors.surface
                    pillFill = softFieldColors().containerColor
                    tint = mapCardColors().tint
                    Box(modifier = Modifier.fillMaxSize().padding(gaps.edge)) {
                        SearchStopGroupCard(
                            form = SearchStopFormModel(
                                shape = FormShape.Single,
                                editing = singleEditing,
                                toPlaceholder = singlePrompt,
                                fromPlaceholder = FROM_PLACEHOLDER,
                                toStop = null,
                                fromStop = null,
                                fieldState = rememberTextFieldState(),
                                focusRequester = remember { FocusRequester() },
                                onFlowEvent = {},
                                onGoClick = {},
                                onTextChange = {},
                            ),
                            onBack = {},
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private var singleEditing by mutableStateOf<ToFromField?>(null)
    private var singlePrompt by mutableStateOf("")

    private fun showSingle(editing: ToFromField?, prompt: String) {
        composeRule.runOnIdle {
            singleEditing = editing
            singlePrompt = prompt
        }
        composeRule.waitForIdle()
    }

    /** The pixels the window draws, read by drawing the compose view into a bitmap. */
    private fun drawnPixels(): Bitmap {
        var bitmap: Bitmap? = null
        composeRule.runOnUiThread {
            val view = checkNotNull(composeView)
            bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(checkNotNull(bitmap)))
        }
        return checkNotNull(bitmap)
    }

    private fun Bitmap.at(x: Int, y: Int): Color = Color(getPixel(x, y))

    private fun Bitmap.at(x: Dp, y: Dp): Color = at(with(composeRule.density) { x.roundToPx() }, with(composeRule.density) { y.roundToPx() })

    private fun boundsOf(tag: String): DpRect = composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    // region the card

    private fun cardBounds() = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
    private fun DpRect.cy() = top + height / 2f
    private fun DpRect.cx() = left + (right - left) / 2f

    @Test
    fun `the box is one card of two rows, From above To, each as wide as the card, with no pill and no padding of its own`() {
        setBox()

        val card = cardBounds()
        val from = boundsOf(TripPlannerTestTags.SEARCH_ROW_FROM)
        val to = boundsOf(TripPlannerTestTags.SEARCH_ROW_TO)
        assertTrue(from.top < to.top, "From is the top row")
        assertNear("From starts at the card's top", card.top, from.top)
        assertNear("To ends at the card's bottom", card.bottom, to.bottom)
        assertNear("a row is the card's width at the start", card.left, from.left)
        assertNear("a row is the card's width at the end", card.right, to.right)
        assertEquals(16.dp, gaps.cardCorner, "The box keeps the card's 16 dp corner.")
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_BACK).assertDoesNotExist()
    }

    @Test
    fun `the card is the Timetable tint in both rows with no second fill, so the rows are not pills`() {
        setBox(toStop = TOWN_HALL, fromStop = CENTRAL)

        val image = drawnPixels()
        val card = cardBounds()
        val from = boundsOf(TripPlannerTestTags.SEARCH_ROW_FROM)
        val to = boundsOf(TripPlannerTestTags.SEARCH_ROW_TO)
        // Clear of every glyph: the card's top padding, and each row's empty stretch between its text and the circles.
        assertClose(tint, image.at(card.left + 30.dp, card.top + 3.dp), "the card's own fill")
        assertClose(tint, image.at(card.right - 80.dp, from.cy()), "the From row, no pill under it")
        assertClose(tint, image.at(card.right - 80.dp, to.cy()), "the To row, no pill under it")
        assertNotEquals(surface, tint, "The tint is not the page's own surface.")
        assertNotEquals(pillFill, tint)
    }

    @Test
    fun `the markers are the 10 dp dot for From and the pin for To, in a 20 dp box on the row's centre line`() {
        setBox(toStop = TOWN_HALL, fromStop = CENTRAL)

        val from = boundsOf(TripPlannerTestTags.SEARCH_ROW_FROM)
        val to = boundsOf(TripPlannerTestTags.SEARCH_ROW_TO)
        val dot = boundsOf(TripPlannerTestTags.SEARCH_ROW_ORIGIN_DOT)
        val pin = boundsOf(TripPlannerTestTags.SEARCH_ROW_DESTINATION_PIN)
        assertNear("the dot is 10 dp across", 10.dp, dot.right - dot.left)
        assertNear("the pin fills its 20 dp box", 20.dp, pin.right - pin.left)
        assertNear("the dot is centred on From's row", from.cy(), dot.cy())
        assertNear("the pin is centred on To's row", to.cy(), pin.cy())
        assertNear("the dot starts the box 16 dp in, centred in its 20", 16.dp + 5.dp, dot.left - from.left)
        assertNear("the pin is 16 dp in", 16.dp, pin.left - to.left)
    }

    @Test
    fun `a labelled stop shows its label icon in place of the marker and reads label then the stop in brackets`() {
        setBox(toStop = TOWN_HALL, fromStop = CENTRAL, fromLabel = "Home", toLabel = "Work", editing = null)

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_ORIGIN_DOT, useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_DESTINATION_PIN, useUnmergedTree = true).assertDoesNotExist()
        composeRule.onAllNodesWithTag(TripPlannerTestTags.SEARCH_ROW_LABEL_ICON, useUnmergedTree = true).assertCountEquals(2)
        composeRule.onNodeWithText("Home", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("(${CENTRAL.stopName})", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Work", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("(${TOWN_HALL.stopName})", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `a label with no icon, or none at all, keeps the dot and the pin`() {
        setBox(toStop = TOWN_HALL, fromStop = CENTRAL, fromLabel = "Mum", editing = null)

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_ORIGIN_DOT, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_DESTINATION_PIN, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_LABEL_ICON, useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText("Mum", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `the rows read their fixed prompts while empty and their stop as real text once held`() {
        setBox()
        composeRule.onNodeWithText(FROM_PLACEHOLDER).assertExists()
        composeRule.onNodeWithText(TO_PLACEHOLDER).assertExists()

        composeRule.runOnIdle {
            fromStopState = CENTRAL
            toStopState = TOWN_HALL
            editingState = null
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(CENTRAL.stopName).assertExists()
        composeRule.onNodeWithText(TOWN_HALL.stopName).assertExists()
        composeRule.onNodeWithText(FROM_PLACEHOLDER).assertDoesNotExist()
        composeRule.onNodeWithText(TO_PLACEHOLDER).assertDoesNotExist()
    }

    // endregion

    // region the grid

    private fun assertGrid(width: Dp, scale: Float) {
        setBox(fontScale = scale, toStop = TOWN_HALL, fromStop = CENTRAL, editing = null)

        val card = cardBounds()
        val from = boundsOf(TripPlannerTestTags.SEARCH_ROW_FROM)
        val to = boundsOf(TripPlannerTestTags.SEARCH_ROW_TO)
        val go = boundsOf(TripPlannerTestTags.SEARCH_ROW_GO_CIRCLE)
        val target = boundsOf(TripPlannerTestTags.SEARCH_ROW_SEARCH)
        val divider = boundsOf(TripPlannerTestTags.SEARCH_ROW_DIVIDER)
        val label = "$width at ${(scale * 100).toInt()} percent"

        assertNear("$label, the go circle is 40 dp", 40.dp, go.right - go.left)
        assertNear("$label, its right edge is 16 dp from the card's, the left padding", gaps.edge, card.right - go.right)
        assertNear("$label, centred on the To row's centre line", to.cy(), go.cy())
        assertNear("$label, the 48 dp target is centred on the circle", go.cx(), target.cx())
        assertNear("$label, the target is 48 dp", 48.dp, target.right - target.left)
        assertNear("$label, the divider is 1 dp", 1.dp, divider.height)
        assertNear("$label, the divider ends 64 dp from the card's right edge", 64.dp, card.right - divider.right)
        assertNear("$label, 8 dp from the divider's end to the circle", 8.dp, go.left - divider.right)
        assertNear("$label, the divider starts at the text, 44 dp, at every size", 44.dp, divider.left - card.left)
        assertNear("$label, the divider sits between the rows", from.bottom, divider.top)
        val dot = boundsOf(TripPlannerTestTags.SEARCH_ROW_ORIGIN_DOT)
        assertNear("$label, the 10 dp dot is centred in the 20 dp marker column that starts 16 dp in", 21.dp, dot.left - card.left)
        assertTrue(dot.cy() <= from.top + from.height / 2f + 1.5.dp, "$label, the marker is on the first line, never below the row's centre")
        assertTrue(to.height >= 56.dp - 1.dp, "$label, a row is at least 56 dp")
        assertTrue(from.height >= 56.dp - 1.dp, "$label, a row is at least 56 dp")
    }

    @Test
    fun `the grid at 411 dp and 100 percent text is the approved picture's, to the dp`() {
        assertGrid(411.dp, 1f)
        assertNear("a row is 56 dp", 56.dp, boundsOf(TripPlannerTestTags.SEARCH_ROW_FROM).height)
        assertNear("the marker is centred on the row's centre line", boundsOf(TripPlannerTestTags.SEARCH_ROW_FROM).cy(), boundsOf(TripPlannerTestTags.SEARCH_ROW_ORIGIN_DOT).cy())
    }

    @Test
    fun `the grid at 411 dp and 150 percent text is the same, the marker still beside the name`() = assertGrid(411.dp, 1.5f)

    @Test
    @Config(qualifiers = "w320dp-h640dp-notnight")
    fun `at 320 dp and 200 percent text the text still starts at 44 dp and the divider with it`() = assertGrid(320.dp, 2f)

    @Test
    fun `the card is 113 dp at 100 percent text, 56 and 56 and the divider`() {
        setBox(toStop = TOWN_HALL, fromStop = CENTRAL, editing = null)

        assertNear("the card height", 113.dp, cardBounds().height)
    }

    private fun assertSameHeightWithoutGo(scale: Float) {
        setBox(fontScale = scale, toStop = TOWN_HALL, fromStop = CENTRAL, editing = null)
        val with = cardBounds().height
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_GO_CIRCLE, useUnmergedTree = true).assertExists()

        // The same two names with the same stop id: a trip from a stop to itself is no trip, so the go circle is absent and the
        // text, and so the wrapping, is unchanged.
        composeRule.runOnIdle { toStopState = TOWN_HALL.copy(stopId = CENTRAL.stopId) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_GO_CIRCLE, useUnmergedTree = true).assertDoesNotExist()
        assertEquals(with, cardBounds().height, "At $scale the card keeps its height with no go circle.")
    }

    @Test
    fun `the card is exactly as tall without the go circle as with it at 100 percent text`() = assertSameHeightWithoutGo(1f)

    @Test
    fun `the card is exactly as tall without the go circle as with it at 150 percent text`() = assertSameHeightWithoutGo(1.5f)

    @Test
    @Config(qualifiers = "w320dp-h640dp-notnight")
    fun `the card is exactly as tall without the go circle as with it at 320 dp and 200 percent text`() = assertSameHeightWithoutGo(2f)

    @Test
    @Config(qualifiers = "w320dp-h640dp-notnight")
    fun `at 320 dp and 200 percent the names wrap whole, the card grows, and nothing is cut`() {
        setBox(fontScale = 2f, toStop = LONG, fromStop = LONG, editing = null)

        val layouts = composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD, useUnmergedTree = true)
            .fetchSemanticsNode()
            .collectTextLayouts()
        assertTrue(layouts.isNotEmpty(), "The rows hold text that can be measured.")
        for (layout in layouts) assertTrue(!layout.hasVisualOverflow, "A name is never clipped or cut with an ellipsis.")
        assertTrue(cardBounds().height > 200.dp, "The card grew to hold the whole names: ${cardBounds().height}.")
    }

    // endregion

    // region the go circle and the clear X

    @Test
    fun `the go circle starts the trip, and a tap in its 48 dp target reaches it`() {
        var taps = 0
        setBox(toStop = TOWN_HALL, fromStop = CENTRAL, editing = null, onGo = { taps++ })

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_SEARCH, useUnmergedTree = true).performClick()

        assertEquals(1, taps)
        composeRule.onNodeWithContentDescription("Show trips").assertExists()
    }

    @Test
    fun `while the To row holds text an X takes the go circle's slot and clearing it brings the go circle back`() {
        setBox(toStop = TOWN_HALL, fromStop = CENTRAL, editing = ToFromField.To)
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_GO_CIRCLE, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_CLEAR, useUnmergedTree = true).assertDoesNotExist()

        composeRule.runOnIdle { typed.setTextAndPlaceCursorAtEnd("Cen") }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_CLEAR, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_GO_CIRCLE, useUnmergedTree = true).assertDoesNotExist()

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_CLEAR, useUnmergedTree = true).performClick()
        composeRule.waitForIdle()
        assertEquals("", typed.text.toString())
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_GO_CIRCLE, useUnmergedTree = true).assertExists()
    }

    // endregion

    @Test
    fun `a label field reads Search and select with its label, empty or focused, and never a stop name`() {
        setSingleBar()
        for (label in listOf("Home", "Work", "Gym")) {
            for (editing in listOf<ToFromField?>(null, ToFromField.To)) {
                showSingle(editing, labelPromptFor(label))
                composeRule.onNodeWithText("Search and select $label").assertExists()
                composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_FROM).assertDoesNotExist()
            }
        }
    }

    @Test
    fun `a single field is one bare pill with no card, 16 dp in on both sides and filled with the card's fixed light tint`() {
        setSingleBar()
        showSingle(ToFromField.To, labelPromptFor("Home"))

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD).assertDoesNotExist()
        val pill = boundsOf(TripPlannerTestTags.SEARCH_ROW_TO)
        assertNear("the pill starts one edge in", gaps.edge, pill.left)
        assertNear("and ends one edge in", 411.dp - gaps.edge, pill.right)
        assertNear("the pill is 48 tall", 48.dp, pill.height)
        assertNear("with nothing above it", gaps.edge, pill.top)

        val image = drawnPixels()
        val density = composeRule.density
        // A point in the pill clear of its text and icons: its right end, where nothing is drawn.
        val inPill = image.at(with(density) { (pill.right - 24.dp).roundToPx() }, with(density) { (pill.top + 24.dp).roundToPx() })
        assertClose(tint, inPill, "the pill, the box's own fixed light tint and not the soft one")
        assertNotEquals(pillFill, tint)
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-notnight")
    fun `the lone pill is 48 tall and 16 dp in at 100 and at 200 percent text`() {
        setSingleBar(fontScale = 2f)
        showSingle(null, labelPromptFor("Home"))

        val pill = boundsOf(TripPlannerTestTags.SEARCH_ROW_TO)
        assertNear("48 tall at 200 percent", 48.dp, pill.height)
        assertNear("16 in at the start", gaps.edge, pill.left)
        assertNear("16 in at the end", 320.dp - gaps.edge, pill.right)
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-notnight")
    fun `a long label prompt is one line with an ellipsis at 200 percent text, at rest and focused`() {
        setSingleBar(fontScale = 2f)
        for (editing in listOf<ToFromField?>(null, ToFromField.To)) {
            showSingle(editing, labelPromptFor(LONG_LABEL))

            assertNear("the pill stays 48 tall", 48.dp, boundsOf(TripPlannerTestTags.SEARCH_ROW_TO).height)
            val layouts = composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_TO, useUnmergedTree = true)
                .fetchSemanticsNode()
                .collectTextLayouts()
            assertTrue(layouts.isNotEmpty(), "The prompt is text that can be measured.")
            for (layout in layouts) assertEquals(1, layout.lineCount, "The prompt is one line, never two.")
            assertTrue(layouts.any { it.isLineEllipsized(0) }, "A prompt that does not fit ends in an ellipsis.")
        }
    }

    private fun androidx.compose.ui.semantics.SemanticsNode.collectTextLayouts(): List<TextLayoutResult> {
        val own = buildList {
            val results = mutableListOf<TextLayoutResult>()
            config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
            addAll(results)
        }
        return own + children.flatMap { it.collectTextLayouts() }
    }

    // endregion

    // region the edge, no glow

    /**
     * The page just outside the card, and the pixel across the card's left edge nearest to [expectedEdge]. A 1 dp stroke is
     * 2 or 3 pixels wide and its outer pixels are blended with what is under them, so the edge is the best match across it.
     */
    private fun pixelsOutsideAndOnEdge(expectedEdge: Color): Pair<Color, Color> {
        val image = drawnPixels()
        val card = cardBounds()
        val midY = card.cy()
        val density = composeRule.density
        val from = with(density) { (card.left - 2.dp).roundToPx() }
        val to = with(density) { (card.left + 2.dp).roundToPx() }
        val y = with(density) { midY.roundToPx() }
        val across = (from..to).map { Color(image.getPixel(it, y)) }
        fun distance(c: Color) = abs(c.red - expectedEdge.red) + abs(c.green - expectedEdge.green) + abs(c.blue - expectedEdge.blue)
        return image.at(card.left - 3.dp, midY) to across.minBy { distance(it) }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight-xxhdpi")
    fun `in light mode the card wears the 1 dp light softLabel hairline and no glow`() {
        setBox(toStop = TOWN_HALL, fromStop = CENTRAL, editing = null)

        val (outside, onEdge) = pixelsOutsideAndOnEdge(edge)
        assertClose(surface, outside, "just outside the card, where the glow used to spread, is the page's own colour")
        assertClose(edge, onEdge, "across the card's edge there is a pixel of the softLabel hairline")
        assertEquals(md_theme_light_softLabel, edge, "the hairline is the light palette's softLabel")
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun `in dark mode the card wears the same light softLabel hairline, never the mode aware one, and no glow`() {
        setBox(toStop = TOWN_HALL, fromStop = CENTRAL, editing = null)

        val (outside, onEdge) = pixelsOutsideAndOnEdge(edge)
        assertClose(surface, outside, "just outside the card is the page's own colour, no glow")
        assertClose(edge, onEdge, "across the card's edge there is a pixel of the light softLabel hairline in dark mode too")
        assertEquals(md_theme_light_softLabel, edge)
        assertNotEquals(modeSoftLabel, edge, "the dark mode softLabel (B0B0B0, 1.82 to 1 against the map) is not what the edge uses")
    }

    @Test
    fun `the stop card keeps its own edge rule, in light mode only`() {
        assertTrue(searchStopEdgeShown(darkMode = false))
        assertTrue(!searchStopEdgeShown(darkMode = true))
    }

    // endregion

    private fun assertNear(what: String, expected: androidx.compose.ui.unit.Dp, actual: androidx.compose.ui.unit.Dp) {
        assertTrue(abs((expected - actual).value) <= 1f, "$what: expected $expected, got $actual")
    }

    private fun isClose(expected: Color, actual: Color, within: Float = 0.03f): Boolean =
        abs(expected.red - actual.red) <= within && abs(expected.green - actual.green) <= within &&
            abs(expected.blue - actual.blue) <= within

    private fun assertClose(expected: Color, actual: Color, what: String) {
        assertTrue(isClose(expected, actual), "$what: expected $expected, got $actual")
    }

    private fun DpRect.width() = right - left

    private companion object {
        val TOWN_HALL = StopItem(stopName = "Town Hall Station", stopId = "200070")
        val CENTRAL = StopItem(stopName = "Central Station", stopId = "200060")
        const val LONG_LABEL = "Grandmother's house near the Parramatta River ferry wharf"
        val LONG = StopItem(
            stopName = "Wollongong Station Platform Two Via Sydenham And Central",
            stopId = "stop_long",
        )
    }
}
