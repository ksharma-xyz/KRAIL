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
        assertEquals(4.dp, gaps.aboveIcon)
        for (gap in listOf(gaps.edge, gaps.cardPadding, gaps.betweenRows, gaps.betweenCards, gaps.aboveIcon)) {
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
    fun `the Options pill is 4 dp above the visible i at every text size, and the control line is centred on it`() {
        val gaps = searchStopGaps()
        assertEquals(8.dp, MAP_ORNAMENT_LIBRARY_MARGIN, "The library puts every ornament 8 dp in even with no padding.")
        assertEquals(21.dp, MAP_ATTRIBUTION_ICON_SIZE)
        assertEquals(1.75.dp, MAP_ATTRIBUTION_GLYPH_INSET)
        assertEquals(27.25.dp, MAP_ATTRIBUTION_GLYPH_TOP, "8 + 21 - 1.75 above the bottom.")
        assertEquals(4.dp, gaps.aboveIcon, "The smallest spacing token, spacingXS.")
        // Pill heights at 100, 150 and 200 percent text: the pill's bottom stays 4 dp over the i, so the line rises.
        for ((pill, centre) in listOf(20.dp to 41.25.dp, 29.dp to 45.75.dp, 34.dp to 48.25.dp)) {
            assertEquals(centre, controlLineCentre(gaps, pill), "pill $pill")
            assertEquals(
                MAP_ATTRIBUTION_GLYPH_TOP + 4.dp,
                controlLineCentre(gaps, pill) - pill / 2,
                "the pill's bottom edge is 4 dp over the i at $pill",
            )
            assertEquals(centre - 24.dp, locationControlBottomPadding(gaps, pill), "the location control, centred on it")
            assertEquals(MAP_ATTRIBUTION_GLYPH_TOP, optionsBottomPadding(gaps, pill), "Options' target ends at the i's top")
        }
    }

    @Test
    fun `the Options target is 48 tall, reaches over the icon's empty edge, and the card is pulled over its top`() {
        val gaps = searchStopGaps()
        for (pill in listOf(20.dp, 29.dp, 32.dp, 36.dp, 39.dp)) {
            assertEquals(48.dp, optionsTargetHeight(gaps, pill))
            // The target's bottom is the i's top, so it overlaps the 21 dp icon's top 1.75 dp and never the i itself.
            assertEquals(
                MAP_ATTRIBUTION_GLYPH_INSET,
                MAP_ORNAMENT_LIBRARY_MARGIN + MAP_ATTRIBUTION_ICON_SIZE - optionsBottomPadding(gaps, pill),
            )
            // The card is 8 over the location control's top (centre + 24), and the target's top is higher than that.
            val locationTop = controlLineCentre(gaps, pill) + 24.dp
            val targetTop = optionsBottomPadding(gaps, pill) + optionsTargetHeight(gaps, pill)
            assertEquals(targetTop - locationTop, optionsCardOverlap(gaps, pill), "pill $pill")
        }
    }

    @Test
    fun `when the pill outgrows the location control the card clears the pill's top, never the control's`() {
        val gaps = searchStopGaps()
        for (pill in listOf(54.dp, 70.dp)) {
            val centre = controlLineCentre(gaps, pill)
            val pillTop = centre + pill / 2
            val targetTop = optionsBottomPadding(gaps, pill) + optionsTargetHeight(gaps, pill)
            assertEquals(targetTop - pillTop, optionsCardOverlap(gaps, pill), "pill $pill")
            assertTrue(pillTop > centre + 24.dp, "The pill is the taller thing on the line at $pill.")
        }
    }

    @Test
    fun `an open stop card lifts the line above itself, the way it lifts the location control`() {
        val gaps = searchStopGaps()
        val pill = 20.dp
        assertEquals(0.dp, controlLineLift(gaps, pill, clearance = 0.dp))
        assertEquals(
            gaps.edge + 120.dp,
            locationControlBottomPadding(gaps, pill, clearance = 120.dp),
            "The location control stands one edge over the card's clearance, as it always did.",
        )
        assertEquals(
            optionsBottomPadding(gaps, pill) + controlLineLift(gaps, pill, 120.dp),
            optionsBottomPadding(gaps, pill, clearance = 120.dp),
            "Options lifts by the same amount, so the line stays one line.",
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
    fun `the top box is hidden only while an open card's board is expanded, in the map state`() {
        assertEquals(true, topBoxHiddenFor(SearchStopMode.Map, boardExpanded = true))
        assertEquals(false, topBoxHiddenFor(SearchStopMode.Map, boardExpanded = false))
        assertEquals(false, topBoxHiddenFor(SearchStopMode.List, boardExpanded = true))
    }

    @Test
    fun `the mode controls are shown only with the location allowed, hidden with it not or with a stop card open`() {
        assertEquals(ModeControls.Shown, modeControlsFor(SearchStopMode.Map, mapReady = true, locationGranted = true))
        assertEquals(ModeControls.Hidden, modeControlsFor(SearchStopMode.Map, mapReady = true, locationGranted = false))
        assertEquals(
            ModeControls.Hidden,
            modeControlsFor(SearchStopMode.Map, mapReady = true, locationGranted = true, stopCardOpen = true),
        )
        // Absent wins: the list state and a map that is not ready compose nothing, whatever the permission says.
        assertEquals(ModeControls.Absent, modeControlsFor(SearchStopMode.List, mapReady = true, locationGranted = true))
        assertEquals(ModeControls.Absent, modeControlsFor(SearchStopMode.Map, mapReady = false, locationGranted = false))
        assertEquals(false, modeControlsShown(SearchStopMode.Map, mapReady = true, locationGranted = false))
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
