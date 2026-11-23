package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.theme.KrailDimensions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The layout of Search Stop's map state, rules 9, 33, 35, 36 and 39, with no layout run. */
class SearchStopLayoutRulesTest {

    // region the rows of the group card

    @Test
    fun `a To and From form is the From pill and then the To pill, with no divider`() {
        assertEquals(listOf(GroupRow.From, GroupRow.To), groupCardRows(FormShape.ToAndFrom))
    }

    @Test
    fun `a single bar has only the To row`() {
        assertEquals(listOf(GroupRow.To), groupCardRows(FormShape.Single))
    }

    @Test
    fun `the To pill is always last and From is always first when it is there`() {
        for (shape in FormShape.entries) {
            assertEquals(GroupRow.To, groupCardRows(shape).last())
            if (GroupRow.From in groupCardRows(shape)) assertEquals(GroupRow.From, groupCardRows(shape).first())
        }
    }

    @Test
    fun `every form shape is a card or one bare pill by how many fields it shows, and nothing else`() {
        for (shape in FormShape.entries) {
            val fields = groupCardRows(shape).size
            assertEquals(fields > 1, groupCardShownFor(shape), "$shape shows $fields field(s)")
        }
        assertTrue(groupCardShownFor(FormShape.ToAndFrom), "Two fields, From and To, are on the card.")
        assertEquals(false, groupCardShownFor(FormShape.Single), "One field is one bare pill on the map.")
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
    fun `Options stands one gap above the info icon, which rests on the library's own margin over the inset`() {
        val gaps = searchStopGaps()
        assertEquals(8.dp, MAP_ORNAMENT_LIBRARY_MARGIN, "The library puts every ornament 8 dp in even with no padding.")
        assertEquals(21.dp, MAP_ATTRIBUTION_ICON_SIZE)
        assertEquals(
            MAP_ORNAMENT_LIBRARY_MARGIN + MAP_ATTRIBUTION_ICON_SIZE + gaps.betweenCards,
            modeControlsBottomPadding(gaps),
            "Options is one gap above the icon's top.",
        )
        assertEquals(8.dp, gaps.betweenCards, "The gap is the one 8 dp gap between floating elements.")
        assertEquals(37.dp, modeControlsBottomPadding(gaps))
    }

    @Test
    fun `an open stop card lifts the stack above itself, the way it lifts the location control`() {
        val gaps = searchStopGaps()
        assertEquals(
            gaps.edge + 120.dp,
            modeControlsBottomPadding(gaps, clearance = 120.dp),
            "The stack stands on the same line as the location control, above the card.",
        )
        assertEquals(
            modeControlsBottomPadding(gaps),
            modeControlsBottomPadding(gaps, clearance = 0.dp),
            "With no card the icon sets the height.",
        )
    }

    @Test
    fun `the info icon has no extra bottom padding, only the inset, and its glyph is one edge in`() {
        val gaps = searchStopGaps()
        val padding = mapOrnamentPadding(
            gaps = gaps,
            statusBarTop = 24.dp,
            navigationBarBottom = 24.dp,
            headerHeight = 136.dp,
        )

        assertEquals(24.dp + 16.dp + 136.dp + 8.dp, padding.calculateTopPadding(), "Under the box, one gap clear.")
        assertEquals(16.dp, padding.calculateRightPadding(LayoutDirection.Ltr), "The compass is one edge in.")
        assertEquals(24.dp, padding.calculateBottomPadding(), "Zero above the navigation bar inset, nothing extra.")
        assertEquals(
            gaps.edge,
            padding.calculateLeftPadding(LayoutDirection.Ltr) + MAP_ORNAMENT_LIBRARY_MARGIN + MAP_ATTRIBUTION_GLYPH_INSET,
            "The library adds its margin and the icon its own inset, so the visible i is one edge in.",
        )
        assertEquals(
            0.dp,
            mapOrnamentPadding(gaps, 0.dp, 0.dp, 0.dp).calculateBottomPadding(),
            "With no navigation bar the bottom padding is exactly zero.",
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

    // endregion
}
