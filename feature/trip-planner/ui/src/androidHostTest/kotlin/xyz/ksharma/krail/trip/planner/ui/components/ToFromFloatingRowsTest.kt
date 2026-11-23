package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import xyz.ksharma.krail.taj.components.TextFieldColors
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The To and From block as two soft pills (`ToFromBlockStyle.FloatingRows`): the real pills on a card of the app's
 * own surface, read from real bounds. Both bars are the same taj field in the shared soft colours, the same
 * whichever bar has focus, with a gap between them and no divider.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp", manifest = Config.NONE)
class ToFromFloatingRowsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val rowGap = 8.dp
    private val cardPadding = 16.dp

    private fun setRows(
        editing: MutableState<ToFromField?>,
        to: StopItem? = null,
        from: StopItem? = null,
        onTo: () -> Unit = {},
        onFrom: () -> Unit = {},
        showGo: Boolean = false,
    ) {
        composeRule.setContent {
            PreviewTheme {
                Box(modifier = Modifier.background(KrailTheme.colors.surface).padding(cardPadding)) {
                    ToFromBlock(
                        toStopItem = to,
                        fromStopItem = from,
                        onToClick = onTo,
                        onFromClick = onFrom,
                        onGoClick = {},
                        editing = editing.value,
                        editor = ToFromEditor(
                            state = rememberTextFieldState(),
                            modifier = Modifier.testTag(QUERY_FIELD),
                        ),
                        toLeading = { Box(Modifier.size(48.dp).testTag(BACK_PROBE)) },
                        showGo = showGo,
                        style = ToFromBlockStyle.FloatingRows(rowGap = rowGap),
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun boundsOf(tag: String): DpRect =
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    @Test
    fun `the bars are two pills of one width, 48 tall, one gap apart with no divider, From above To`() {
        setRows(mutableStateOf(ToFromField.To))

        val to = boundsOf(TripPlannerTestTags.SEARCH_ROW_TO)
        val from = boundsOf(TripPlannerTestTags.SEARCH_ROW_FROM)
        assertEquals(to.left, from.left)
        assertEquals(to.width, from.width)
        assertEquals(48.dp, to.height)
        assertEquals(48.dp, from.height)
        assertTrue(from.top < to.top, "From is the top pill: From $from To $to")
        assertTrue(abs((to.top - from.bottom - rowGap).value) <= 0.5f, "From $from To $to")
    }

    @Test
    fun `the back arrow starts the top pill, the pin starts the pill below it, and the go circle ends that one`() {
        setRows(mutableStateOf<ToFromField?>(null), to = TOWN_HALL, from = CENTRAL, showGo = true)

        val from = boundsOf(TripPlannerTestTags.SEARCH_ROW_FROM)
        val to = boundsOf(TripPlannerTestTags.SEARCH_ROW_TO)
        val arrow = boundsOf(BACK_PROBE)
        val pin = boundsOf(TripPlannerTestTags.SEARCH_ROW_DESTINATION_PIN)
        val go = boundsOf(TripPlannerTestTags.SEARCH_ROW_GO_CIRCLE)
        assertTrue(arrow.top >= from.top && arrow.bottom <= from.bottom, "The arrow $arrow is in the top pill $from")
        assertTrue(arrow.left - from.left < 1.dp, "and at its start")
        assertTrue(pin.top >= to.top && pin.bottom <= to.bottom, "The pin $pin is in the pill below $to")
        assertTrue(abs((pin.centerX() - arrow.centerX()).value) <= 1f, "and in the same start column as the arrow")
        assertTrue(go.top >= to.top && go.bottom <= to.bottom, "The go circle $go is in the last pill $to")
        assertTrue(abs((to.right - go.right).value) <= 8.5f, "and at its end")
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_ORIGIN_DOT).assertDoesNotExist()
    }

    @Test
    fun `with one bar the back arrow is its start and there is no pin, no From and no gap above it`() {
        composeRule.setContent {
            PreviewTheme {
                ToFromBlock(
                    toStopItem = null,
                    fromStopItem = null,
                    onToClick = {},
                    onFromClick = {},
                    onGoClick = {},
                    editing = null,
                    toLeading = { Box(Modifier.size(48.dp).testTag(BACK_PROBE)) },
                    showFrom = false,
                    showGo = false,
                    style = ToFromBlockStyle.FloatingRows(rowGap = rowGap),
                )
            }
        }
        composeRule.waitForIdle()

        val to = boundsOf(TripPlannerTestTags.SEARCH_ROW_TO)
        val arrow = boundsOf(BACK_PROBE)
        assertTrue(arrow.top >= to.top && arrow.bottom <= to.bottom && arrow.left - to.left < 1.dp, "$arrow in $to")
        assertEquals(0.dp, to.top, "Nothing is above the one bar.")
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_FROM).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_DESTINATION_PIN).assertDoesNotExist()
    }

    @Test
    fun `a bar at rest shows its stop as text and its prompt only while empty`() {
        val editing = mutableStateOf<ToFromField?>(null)
        setRows(editing, to = TOWN_HALL)

        composeRule.onNodeWithText(TOWN_HALL.stopName).assertExists()
        composeRule.onNodeWithText(TO_PLACEHOLDER).assertDoesNotExist()
        composeRule.onNodeWithText(FROM_PLACEHOLDER).assertExists()
    }

    @Test
    fun `a bar that holds a stop and is edited starts empty with its fixed prompt, never the stop's name`() {
        val editing = mutableStateOf<ToFromField?>(ToFromField.To)
        setRows(editing, to = TOWN_HALL, from = CENTRAL)

        composeRule.onNodeWithText(TOWN_HALL.stopName).assertDoesNotExist()
        composeRule.onNodeWithText(TO_PLACEHOLDER).assertExists()
        composeRule.onNodeWithText(CENTRAL.stopName).assertExists()

        composeRule.runOnIdle { editing.value = ToFromField.From }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(CENTRAL.stopName).assertDoesNotExist()
        composeRule.onNodeWithText(FROM_PLACEHOLDER).assertExists()
        composeRule.onNodeWithText(TOWN_HALL.stopName).assertExists()

        composeRule.runOnIdle { editing.value = null }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(CENTRAL.stopName).assertExists()
        composeRule.onNodeWithText(TOWN_HALL.stopName).assertExists()
    }

    private fun DpRect.centerX() = left + (right - left) / 2f

    @Test
    fun `the idle bar is the same field, a tap on it edits it, and the bar being edited holds the query field`() {
        val taps = mutableListOf<String>()
        val editing = mutableStateOf<ToFromField?>(ToFromField.To)
        setRows(editing, onTo = { taps += "to" }, onFrom = { taps += "from" })

        composeRule.onNodeWithTag(QUERY_FIELD).assertExists()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_FROM).performClick()

        assertEquals(listOf("from"), taps, "A tap on the idle bar is the tap that edits it.")
    }

    @Test
    fun `both bars are the same text field, whichever is edited, so neither swaps to a button`() {
        val editing = mutableStateOf<ToFromField?>(ToFromField.To)
        setRows(editing, to = TOWN_HALL, from = CENTRAL)

        for (field in listOf(ToFromField.To, ToFromField.From, null)) {
            composeRule.runOnIdle { editing.value = field }
            composeRule.waitForIdle()
            composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText))
                .assertCountEquals(2)
        }
        composeRule.onNodeWithText(TOWN_HALL.stopName).assertExists()
        composeRule.onNodeWithText(CENTRAL.stopName).assertExists()
    }

    @Test
    fun `the query field moves with editing and the bars do not move`() {
        val editing = mutableStateOf<ToFromField?>(ToFromField.To)
        setRows(editing)
        val to = boundsOf(TripPlannerTestTags.SEARCH_ROW_TO)
        val from = boundsOf(TripPlannerTestTags.SEARCH_ROW_FROM)

        composeRule.runOnIdle { editing.value = ToFromField.From }
        composeRule.waitForIdle()

        assertEquals(to, boundsOf(TripPlannerTestTags.SEARCH_ROW_TO))
        assertEquals(from, boundsOf(TripPlannerTestTags.SEARCH_ROW_FROM))
        val field = boundsOf(QUERY_FIELD)
        assertTrue(field.top >= from.top - 1.dp && field.bottom <= from.bottom + 1.dp, "The field is in From: $field")
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight")
    fun `in light mode the pills are the shared soft colours, a touch darker than the card`() = assertSoftPills()

    @Test
    @Config(qualifiers = "w411dp-h891dp-night")
    fun `in dark mode the pills are the same soft colours, a touch lighter than the card`() = assertSoftPills()

    /**
     * Pixels of the text are not read here, so the colours are read where the pills get them: the one
     * [softFieldColors] holder, whose fill is a real step from the card and whose text and placeholder read on it.
     */
    private fun assertSoftPills() {
        var soft: TextFieldColors? = null
        var card = Color.Unspecified
        var onSurface = Color.Unspecified
        composeRule.setContent {
            PreviewTheme {
                soft = softFieldColors()
                card = KrailTheme.colors.surface
                onSurface = KrailTheme.colors.onSurface
            }
        }
        composeRule.waitForIdle()

        val colors = checkNotNull(soft)
        assertTrue(colors.containerColor != card, "A pill is not the card's own colour.")
        assertEquals(onSurface, colors.contentColor)
        assertEquals(onSurface, colors.cursorColor)
        assertTrue(colors.containerColor.alpha == 1f, "A pill is a solid fill, never see through.")
    }

    private companion object {
        val TOWN_HALL = StopItem(stopName = "Town Hall Station", stopId = "200070")
        val CENTRAL = StopItem(stopName = "Central Station", stopId = "200060")
        const val QUERY_FIELD = "test.queryfield"
        const val BACK_PROBE = "test.back"
    }
}
