package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.theme.KrailDimensions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The layout of Search Stop's map state, rules 9, 33, 35, 36 and 39, with no layout run. */
class SearchStopLayoutRulesTest {

    // region the rows of the group card

    @Test
    fun `a To and From form is the To bar, a divider and the From bar`() {
        assertEquals(listOf(GroupRow.To, GroupRow.Divider, GroupRow.From), groupCardRows(FormShape.ToAndFrom))
    }

    @Test
    fun `a single bar has no divider and no From row`() {
        assertEquals(listOf(GroupRow.To), groupCardRows(FormShape.Single))
    }

    @Test
    fun `the To bar is always first and a divider is always between two bars`() {
        for (shape in FormShape.entries) {
            val rows = groupCardRows(shape)
            assertEquals(GroupRow.To, rows.first())
            val divider = rows.indexOf(GroupRow.Divider)
            if (divider >= 0) {
                assertEquals(GroupRow.To, rows[divider - 1])
                assertEquals(GroupRow.From, rows[divider + 1])
            }
        }
    }

    @Test
    fun `the location chip is under the card while the location is not allowed and never in its rows`() {
        assertEquals(false, locationChipShownFor(LocationPill.Hidden))
        assertEquals(true, locationChipShownFor(LocationPill.Ask))
        assertEquals(true, locationChipShownFor(LocationPill.OpenSettings))
    }

    // endregion

    // region the gap table

    @Test
    fun `every gap is a step of the one spacing scale`() {
        val gaps = searchStopGaps()
        val scale = setOf(4.dp, 8.dp, 12.dp, 16.dp)
        assertEquals(16.dp, gaps.edge)
        assertEquals(8.dp, gaps.betweenRows)
        assertEquals(8.dp, gaps.betweenCards)
        assertEquals(16.dp, gaps.cardPadding)
        for (gap in listOf(gaps.edge, gaps.cardPadding, gaps.betweenRows, gaps.betweenCards)) {
            assertTrue(gap in scale, "$gap is on the 4, 8, 12, 16 scale")
        }
    }

    @Test
    fun `the cards take the app's card radius and padding tokens`() {
        val dim = KrailDimensions()
        val gaps = searchStopGaps(dim)
        assertEquals(dim.cardCornerRadius, gaps.cardCorner)
        assertEquals(16.dp, gaps.cardCorner)
        assertEquals(dim.cardHorizontalPadding, gaps.cardPadding)
        assertEquals(dim.cardVerticalPadding, gaps.cardPadding, "One padding on all four sides.")
    }

    @Test
    fun `the mode controls stand one control row and one gap above the map's own bottom row`() {
        val gaps = searchStopGaps()
        assertEquals(16.dp + 48.dp + 8.dp, modeControlsBottomPadding(gaps))
        assertEquals(
            modeControlsBottomPadding(gaps) + 120.dp,
            modeControlsBottomPadding(gaps, clearance = 120.dp),
            "An open stop card lifts the controls by its own height and gap.",
        )
        assertTrue(
            modeControlsBottomPadding(gaps) >= gaps.edge + MAP_CONTROL_ROW_HEIGHT,
            "The controls clear the location button's row, which stands one edge above the inset.",
        )
    }

    // endregion

    // region the mode controls

    @Test
    fun `the mode controls show in the map state of a ready map and nowhere else`() {
        assertTrue(modeControlsShown(SearchStopMode.Map, mapReady = true))
        assertEquals(false, modeControlsShown(SearchStopMode.List, mapReady = true))
        assertEquals(false, modeControlsShown(SearchStopMode.Map, mapReady = false))
        assertEquals(false, modeControlsShown(SearchStopMode.List, mapReady = false))
    }

    @Test
    fun `the button is active while any mode is switched off and not otherwise`() {
        val all = listOf(1, 2, 4, 5, 7, 9)
        assertEquals(false, modeFilterActive(all, all.toSet()))
        assertTrue(modeFilterActive(all, all.toSet() - 5))
        assertTrue(modeFilterActive(all, emptySet()))
    }

    @Test
    fun `the block opens from the button and shuts from the button, the map or leaving the map state`() {
        assertTrue(reduceModeBlock(open = false, ModeBlockEvent.ButtonTapped))
        assertEquals(false, reduceModeBlock(open = true, ModeBlockEvent.ButtonTapped))
        assertEquals(false, reduceModeBlock(open = true, ModeBlockEvent.MapTapped))
        assertEquals(false, reduceModeBlock(open = true, ModeBlockEvent.LeftMapState))
        assertEquals(false, reduceModeBlock(open = false, ModeBlockEvent.MapTapped))
    }

    @Test
    fun `the top stack ends one gap above the mode controls, and is unbounded with none`() {
        val gaps = searchStopGaps()
        assertEquals(Dp.Unspecified, topStackMaxHeight(controlsTop = null, gaps = gaps))
        assertEquals(492.dp, topStackMaxHeight(controlsTop = 500.dp, gaps = gaps))
        assertEquals(0.dp, topStackMaxHeight(controlsTop = 4.dp, gaps = gaps), "Never negative.")
    }

    // endregion
}
