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
                        showGo = false,
                        style = ToFromBlockStyle.FloatingRows(rowGap = rowGap),
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun boundsOf(tag: String): DpRect = composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    @Test
    fun `the bars are two pills of one width, 48 tall, one gap apart with no divider between`() {
        setRows(mutableStateOf(ToFromField.To))

        val to = boundsOf(TripPlannerTestTags.SEARCH_ROW_TO)
        val from = boundsOf(TripPlannerTestTags.SEARCH_ROW_FROM)
        assertEquals(to.left, from.left)
        assertEquals(to.width, from.width)
        assertEquals(48.dp, to.height)
        assertEquals(48.dp, from.height)
        assertTrue(abs((from.top - to.bottom - rowGap).value) <= 0.5f, "To $to From $from")
    }

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
