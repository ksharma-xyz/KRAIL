package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Rules 35 and 36 of `docs/MAP_HOME_UX.md` on the real block and the real search row: To is the
 * top bar, From is under it, neither ever moves, both share both edges, and the go circle is
 * 40 dp inside the From bar in the same column as the clear X above it.
 *
 * Every test runs in a window of its own width (density 1, so a dp is a pixel) and reads where the
 * real nodes landed. The two end columns of the To bar are stood in for by 48 dp probes, which is
 * what the back arrow and the X are on the Search Stop screen.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [BLOCK_TEST_SDK], manifest = Config.NONE)
class ToFromBlockLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    // region order, edges and size

    @Test
    @Config(qualifiers = PHONE)
    fun `To is above From with 8 dp between the bars`() {
        setBlock()

        val to = boundsOf(TO)
        val from = boundsOf(FROM)

        assertTrue(to.top < from.top, "To should be above From but To is at ${to.top} and From at ${from.top}.")
        assertNear("gap between the bars", 8.dp, from.top - to.bottom)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `both bars are the same width and share both edges`() {
        setBlock()

        val to = boundsOf(TO)
        val from = boundsOf(FROM)

        assertNear("left edge", to.left, from.left)
        assertNear("right edge", to.right, from.right)
        assertNear("width", to.width, from.width)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `both bars are 48 dp tall`() {
        setBlock()

        assertNear("To height", 48.dp, boundsOf(TO).height)
        assertNear("From height", 48.dp, boundsOf(FROM).height)
    }

    // endregion

    // region the go circle

    @Test
    @Config(qualifiers = PHONE)
    fun `the go circle is 40 dp and inside the From bar with 4 dp around it`() {
        setBlock()

        val from = boundsOf(FROM)
        val circle = boundsOf(GO_CIRCLE)

        assertNear("circle width", 40.dp, circle.width)
        assertNear("circle height", 40.dp, circle.height)
        assertNear("margin above", 4.dp, circle.top - from.top)
        assertNear("margin below", 4.dp, from.bottom - circle.bottom)
        assertNear("margin at the end", 4.dp, from.right - circle.right)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the go circle is not on the To bar`() {
        setBlock()

        val to = boundsOf(TO)
        val circle = boundsOf(GO_CIRCLE)

        assertTrue(
            circle.top >= to.bottom,
            "The go circle is at ${circle.top}, inside the To bar that ends at ${to.bottom}.",
        )
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the go circle is in the same column as the clear X above it`() {
        setBlock()

        val x = boundsOf(TO_TRAILING_PROBE)
        val column = boundsOf(TripPlannerTestTags.SEARCH_ROW_SEARCH)
        val circle = boundsOf(GO_CIRCLE)

        assertNear("column left edge", x.left, column.left)
        assertNear("column right edge", x.right, column.right)
        assertNear("circle centre", centreX(x), centreX(circle))
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the go circle is one tap target of 48 dp that searches and does not edit From`() {
        var searched = 0
        var fromTapped = 0
        setBlock(onGo = { searched++ }, onFrom = { fromTapped++ })

        val column = boundsOf(TripPlannerTestTags.SEARCH_ROW_SEARCH)
        assertNear("tap target width", 48.dp, column.width)
        assertNear("tap target height", 48.dp, column.height)

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_SEARCH).performClick()

        assertEquals(1, searched, "The go circle should search once.")
        assertEquals(0, fromTapped, "The go circle sits inside the From bar and must not also edit it.")
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `each bar opens its own field`() {
        var toTapped = 0
        var fromTapped = 0
        setBlock(onTo = { toTapped++ }, onFrom = { fromTapped++ })

        composeRule.onNodeWithTag(TO).performClick()
        composeRule.onNodeWithTag(FROM).performClick()
        composeRule.onNodeWithTag(FROM).performClick()

        assertEquals(1, toTapped)
        assertEquals(2, fromTapped)
    }

    // endregion

    // region the end columns

    @Test
    @Config(qualifiers = PHONE)
    fun `the origin ring is in the From bar and the back slot in the To bar`() {
        setBlock()

        val from = boundsOf(FROM)
        val to = boundsOf(TO)
        val ring = boundsOf(TripPlannerTestTags.SEARCH_ROW_ORIGIN_RING)
        val back = boundsOf(TO_LEADING_PROBE)

        assertTrue(ring.isWithinRows(from), "The ring $ring is not on the From bar $from.")
        assertTrue(back.isWithinRows(to), "The back slot $back is not on the To bar $to.")
        assertNear("ring and back arrow share a centre line", centreX(back), centreX(ring))
        assertNear("back slot starts at the bar's edge", to.left, back.left)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the To and From values start at one x`() {
        setBlock()

        val to = boundsOf(TO)
        val toText = textBounds(TOWN_HALL.stopName)
        val fromText = textBounds(CENTRAL.stopName)

        assertNear("To and From values", toText.left, fromText.left)
        assertNear("value after the 48 dp column and a 4 dp gap", to.left + 52.dp, toText.left)
    }

    // endregion

    // region nothing moves

    @Test
    @Config(qualifiers = PHONE)
    fun `no part of either bar moves when the dark fill moves from To to From and back`() {
        val editing = mutableStateOf<ToFromField?>(ToFromField.To)
        setBlock(editing = editing)
        val whileEditingTo = everyPart()

        setEditing(editing, ToFromField.From)
        assertEquals(whileEditingTo, everyPart(), "Nothing may move when the rider taps From.")

        setEditing(editing, ToFromField.To)
        assertEquals(whileEditingTo, everyPart(), "Nothing may move when the rider goes back to To.")

        setEditing(editing, null)
        assertEquals(whileEditingTo, everyPart(), "Nothing may move when neither is edited.")
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the field being edited is the one marked selected, and only that one`() {
        val editing = mutableStateOf<ToFromField?>(ToFromField.To)
        setBlock(editing = editing)

        composeRule.onNodeWithTag(TO).assertIsSelected()
        composeRule.onNodeWithTag(FROM).assertIsNotSelected()

        setEditing(editing, ToFromField.From)

        composeRule.onNodeWithTag(TO).assertIsNotSelected()
        composeRule.onNodeWithTag(FROM).assertIsSelected()

        setEditing(editing, null)

        composeRule.onNodeWithTag(TO).assertIsNotSelected()
        composeRule.onNodeWithTag(FROM).assertIsNotSelected()
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `To stays put when the From bar appears under it`() {
        val showFrom = mutableStateOf(false)
        setBlock(showFrom = showFrom)
        val toBefore = boundsOf(TO)
        composeRule.onAllNodesWithTag(FROM).assertCountEquals(0)

        composeRule.runOnIdle { showFrom.value = true }
        composeRule.waitForIdle()

        assertEquals(toBefore, boundsOf(TO), "To must not move when From appears.")
        assertTrue(boundsOf(FROM).top > toBefore.bottom, "From should appear under To.")
    }

    // endregion

    // region one line, an ellipsis, and the full text in the label

    @Test
    @Config(qualifiers = PHONE)
    fun `a long stop name is one line and ends in an ellipsis`() {
        setBlock(to = LONG_STOP, fontScale = 2f)

        val layout = layoutOf(LONG_STOP.stopName)

        assertEquals(1, layout.lineCount, "A field is one line.")
        assertTrue(layout.isLineEllipsized(0), "A name too long for the field should end in an ellipsis.")
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the bar carries the full stop name in its accessibility label`() {
        setBlock(to = LONG_STOP, fontScale = 2f)

        composeRule.onNodeWithTag(TO).assertContentDescriptionEquals("To, ${LONG_STOP.stopName}")
        composeRule.onNodeWithTag(FROM).assertContentDescriptionEquals("From, ${CENTRAL.stopName}")
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `an empty field is labelled with its own words`() {
        setBlock(to = null, from = null)

        composeRule.onNodeWithTag(TO).assertContentDescriptionEquals("Choose destination")
        composeRule.onNodeWithTag(FROM).assertContentDescriptionEquals("Choose starting point")
    }

    // endregion

    // region 320 dp at 200 percent text

    @Test
    @Config(qualifiers = NARROW)
    fun `at 320 dp and 200 percent the rows stay whole`() {
        setBlock(to = LONG_STOP, fontScale = 2f)

        val to = boundsOf(TO)
        val from = boundsOf(FROM)
        val circle = boundsOf(GO_CIRCLE)

        assertTrue(to.top < from.top, "To should be above From.")
        assertNear("To height", 48.dp, to.height)
        assertNear("From height", 48.dp, from.height)
        assertNear("left edge", to.left, from.left)
        assertNear("right edge", to.right, from.right)
        assertNear("circle", 40.dp, circle.width)
        assertTrue(circle.isInside(from), "The go circle $circle should be inside the From bar $from.")
        assertEquals(1, layoutOf(LONG_STOP.stopName).lineCount, "A name is never broken over two lines.")
        assertEquals(1, layoutOf(CENTRAL.stopName).lineCount, "A name is never broken over two lines.")
    }

    @Test
    @Config(qualifiers = NARROW)
    fun `at 320 dp and 200 percent nothing moves when the dark fill moves`() {
        val editing = mutableStateOf<ToFromField?>(ToFromField.To)
        setBlock(to = LONG_STOP, fontScale = 2f, editing = editing)
        val whileEditingTo = everyPart(toName = LONG_STOP.stopName)

        setEditing(editing, ToFromField.From)

        assertEquals(
            whileEditingTo,
            everyPart(toName = LONG_STOP.stopName),
            "Nothing may move when the rider taps From.",
        )
    }

    // endregion

    // region the real search row

    @Test
    @Config(qualifiers = PHONE)
    fun `the real row puts To above From and the go circle inside From`() = assertRealRow(fontScale = 1f, ai = false)

    @Test
    @Config(qualifiers = NARROW)
    fun `the real row at 320 dp and 200 percent keeps the bars and the button whole`() =
        assertRealRow(fontScale = 2f, ai = false)

    @Test
    @Config(qualifiers = NARROW)
    fun `the real row at 320 dp and 200 percent keeps Ask KRAIL whole`() =
        assertRealRow(fontScale = 2f, ai = true)

    @Test
    @Config(qualifiers = PHONE)
    fun `the real row keeps Ask KRAIL beside the block`() = assertRealRow(fontScale = 1f, ai = true)

    @Test
    @Config(qualifiers = PHONE)
    fun `the real row keeps the existing tags and callbacks`() {
        var to = 0
        var from = 0
        var search = 0
        var reverse = 0
        setRow(onTo = { to++ }, onFrom = { from++ }, onSearch = { search++ }, onReverse = { reverse++ })

        listOf(
            TripPlannerTestTags.SEARCH_ROW,
            TripPlannerTestTags.SEARCH_ROW_TO,
            TripPlannerTestTags.SEARCH_ROW_FROM,
            TripPlannerTestTags.SEARCH_ROW_SEARCH,
            TripPlannerTestTags.SEARCH_ROW_REVERSE,
        ).forEach { composeRule.onNodeWithTag(it).assertIsDisplayed().assertIsEnabled() }
        composeRule.onAllNodesWithTag(TripPlannerTestTags.SEARCH_ROW_ASK_KRAIL).assertCountEquals(0)

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_TO).performClick()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_FROM).performClick()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_SEARCH).performClick()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_REVERSE).performClick()

        assertEquals(listOf(1, 1, 1, 1), listOf(to, from, search, reverse))
    }

    private fun assertRealRow(fontScale: Float, ai: Boolean) {
        setRow(fontScale = fontScale, ai = ai)

        val to = boundsOf(TripPlannerTestTags.SEARCH_ROW_TO)
        val from = boundsOf(TripPlannerTestTags.SEARCH_ROW_FROM)
        val circle = boundsOf(GO_CIRCLE)
        val action = boundsOf(
            if (ai) TripPlannerTestTags.SEARCH_ROW_ASK_KRAIL else TripPlannerTestTags.SEARCH_ROW_REVERSE,
        )
        val windowWidth = boundsOf(TripPlannerTestTags.SEARCH_ROW).right

        assertTrue(to.bottom <= from.top, "To $to should be above From $from.")
        assertNear("left edge", to.left, from.left)
        assertNear("right edge", to.right, from.right)
        assertTrue(circle.isInside(from), "The go circle $circle should be inside the From bar $from.")
        assertNear("action width", 48.dp, action.width)
        assertNear("action height", 48.dp, action.height)
        assertTrue(
            action.left >= to.right || action.top >= from.bottom,
            "The action $action should be beside or under the bars, not over them ($to, $from).",
        )
        assertTrue(action.right <= windowWidth, "The action $action runs off the $windowWidth dp row.")
    }

    // endregion

    // region what Search Stop fills in

    @Test
    @Config(qualifiers = PHONE)
    fun `with no go circle the From bar has no go column and no go tap target`() {
        setBlock(showGo = false)

        composeRule.onNodeWithTag(GO_CIRCLE).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_SEARCH).assertDoesNotExist()
        composeRule.onNodeWithTag(FROM).assertExists()
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `an editor stands in for the bar's text and the bar keeps its size`() {
        setBlock(toEditor = { Box(Modifier.size(1.dp).testTag(EDITOR_PROBE)) })

        composeRule.onNodeWithTag(EDITOR_PROBE).assertExists()
        composeRule.onNodeWithText(TOWN_HALL.stopName).assertDoesNotExist()
        val height = boundsOf(TO).height.value
        assertTrue(abs(height - BAR_HEIGHT_DP) < 0.5f, "A bar holding a field is a bar tall, not $height.")
    }

    // endregion

    // region harness

    private fun setBlock(
        fontScale: Float = 1f,
        to: StopItem? = TOWN_HALL,
        from: StopItem? = CENTRAL,
        editing: MutableState<ToFromField?> = mutableStateOf(null),
        showFrom: MutableState<Boolean> = mutableStateOf(true),
        showGo: Boolean = true,
        toEditor: (@Composable () -> Unit)? = null,
        onTo: () -> Unit = {},
        onFrom: () -> Unit = {},
        onGo: () -> Unit = {},
    ) {
        composeRule.setContent {
            WithFontScale(fontScale) {
                PreviewTheme {
                    ToFromBlock(
                        toStopItem = to,
                        fromStopItem = from,
                        onToClick = onTo,
                        onFromClick = onFrom,
                        onGoClick = onGo,
                        editing = editing.value,
                        showFrom = showFrom.value,
                        showGo = showGo,
                        toEditor = toEditor,
                        toLeading = { Box(Modifier.size(48.dp).testTag(TO_LEADING_PROBE)) },
                        toTrailing = { Box(Modifier.size(48.dp).testTag(TO_TRAILING_PROBE)) },
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun setRow(
        fontScale: Float = 1f,
        ai: Boolean = false,
        onTo: () -> Unit = {},
        onFrom: () -> Unit = {},
        onSearch: () -> Unit = {},
        onReverse: () -> Unit = {},
    ) {
        composeRule.setContent {
            WithFontScale(fontScale) {
                PreviewTheme {
                    Box(Modifier.fillMaxSize()) {
                        SearchStopRow(
                            fromButtonClick = onFrom,
                            toButtonClick = onTo,
                            modifier = Modifier.align(Alignment.BottomCenter),
                            fromStopItem = CENTRAL,
                            toStopItem = TOWN_HALL,
                            isExpanded = true,
                            onSearchButtonClick = onSearch,
                            onReverseButtonClick = onReverse,
                            isAiSearchAvailable = ai,
                        )
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

    private fun setEditing(state: MutableState<ToFromField?>, field: ToFromField?) {
        composeRule.runOnIdle { state.value = field }
        composeRule.waitForIdle()
    }

    /** Every part of both bars that must stay where it is, by name, so a failure says which moved. */
    private fun everyPart(toName: String = TOWN_HALL.stopName): Map<String, DpRect> = mapOf(
        "To bar" to boundsOf(TO),
        "From bar" to boundsOf(FROM),
        "back slot" to boundsOf(TO_LEADING_PROBE),
        "clear slot" to boundsOf(TO_TRAILING_PROBE),
        "origin ring" to boundsOf(TripPlannerTestTags.SEARCH_ROW_ORIGIN_RING),
        "go column" to boundsOf(TripPlannerTestTags.SEARCH_ROW_SEARCH),
        "go circle" to boundsOf(GO_CIRCLE),
        "To value" to textBounds(toName),
        "From value" to textBounds(CENTRAL.stopName),
    )

    private fun boundsOf(tag: String): DpRect =
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun textBounds(text: String): DpRect =
        composeRule.onNodeWithText(text, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun layoutOf(text: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.single()
    }

    private fun centreX(r: DpRect): Dp = (r.left + r.right) / 2

    private fun DpRect.isWithinRows(bar: DpRect): Boolean = top >= bar.top && bottom <= bar.bottom

    private fun DpRect.isInside(outer: DpRect): Boolean =
        left >= outer.left && right <= outer.right && top >= outer.top && bottom <= outer.bottom

    private fun assertNear(what: String, expected: Dp, actual: Dp) {
        assertTrue(
            abs(expected.value - actual.value) <= TOLERANCE_DP,
            "$what should be $expected but is $actual.",
        )
    }

    // endregion

    private companion object {
        const val TO = TripPlannerTestTags.SEARCH_ROW_TO
        const val FROM = TripPlannerTestTags.SEARCH_ROW_FROM
        const val GO_CIRCLE = TripPlannerTestTags.SEARCH_ROW_GO_CIRCLE
        const val EDITOR_PROBE = "toFromBlock.editorProbe"
        const val BAR_HEIGHT_DP = 48f
        const val TO_LEADING_PROBE = "probe.to.leading"
        const val TO_TRAILING_PROBE = "probe.to.trailing"
        const val PHONE = "w411dp-h891dp"
        const val NARROW = "w320dp-h800dp"
        const val TOLERANCE_DP = 0.5f

        val TOWN_HALL = StopItem(stopName = "Town Hall Station", stopId = "200070")
        val CENTRAL = StopItem(stopName = "Central Station", stopId = "200060")
        val LONG_STOP = StopItem(stopName = "Sydney Olympic Park Station Platform One", stopId = "2127")
    }
}

private const val BLOCK_TEST_SDK = 34
