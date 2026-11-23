package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.core.adaptiveui.DUAL_PANE_LIST_WIDTH
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The width rule behind `docs/MAP_HOME_UX.md` rule 45: saved trips are one column, at most
 * 560 dp wide and centred. It is a pure function of the room the pane offers, so every boundary
 * is driven directly: 560 is the first width not narrower than the cap, and one more is the
 * first that leaves a gutter.
 */
class SavedTripsColumnTest {

    private class Case(val name: String, val available: Dp, val width: Dp, val gutter: Dp)

    private val cases = listOf(
        Case("zero room", available = 0.dp, width = 0.dp, gutter = 0.dp),
        Case("a sliver", available = 1.dp, width = 1.dp, gutter = 0.dp),
        Case("a negative width is no room", available = (-10).dp, width = 0.dp, gutter = 0.dp),
        Case("the narrowest supported phone", available = 320.dp, width = 320.dp, gutter = 0.dp),
        Case("a typical phone", available = 411.dp, width = 411.dp, gutter = 0.dp),
        Case("one below the cap", available = 559.dp, width = 559.dp, gutter = 0.dp),
        Case("exactly the cap", available = 560.dp, width = 560.dp, gutter = 0.dp),
        Case("one above the cap", available = 561.dp, width = 560.dp, gutter = 0.5.dp),
        Case("a small tablet in portrait", available = 700.dp, width = 560.dp, gutter = 70.dp),
        Case("one below the dual pane gate", available = 839.dp, width = 560.dp, gutter = 139.5.dp),
        Case("a window far wider than any pane", available = 1280.dp, width = 560.dp, gutter = 360.dp),
        Case("the dual pane list pane", available = DUAL_PANE_LIST_WIDTH, width = 480.dp, gutter = 0.dp),
    )

    @Test
    fun `column width for every pane width`() {
        cases.forEach { case ->
            assertEquals(
                case.width,
                columnWidthFor(case.available),
                "Column width for ${case.name} (${case.available}) is capped at 560 dp and never " +
                    "wider than the room it is given.",
            )
        }
    }

    @Test
    fun `gutter is half the room the column leaves`() {
        cases.forEach { case ->
            assertEquals(
                case.gutter,
                columnGutterFor(case.available),
                "Gutter for ${case.name} (${case.available}) is the same on both sides, so the " +
                    "column is centred.",
            )
        }
    }

    @Test
    fun `column plus two gutters is the whole pane at every width`() {
        (0..1600).forEach { whole ->
            val available = whole.dp
            val used = columnWidthFor(available) + columnGutterFor(available) * 2
            assertEquals(available, used, "Column and gutters add up to the pane at $available.")
        }
    }

    @Test
    fun `column never exceeds the cap or the pane`() {
        (0..1600).forEach { whole ->
            val available = whole.dp
            val width = columnWidthFor(available)
            assertTrue(width <= SAVED_TRIPS_COLUMN_MAX_WIDTH, "Column at $available is $width, over the cap.")
            assertTrue(width <= available, "Column at $available is $width, wider than the pane.")
        }
    }

    @Test
    fun `cap is 560 dp`() {
        assertEquals(
            560.dp,
            SAVED_TRIPS_COLUMN_MAX_WIDTH,
            "Rule 45 in docs/MAP_HOME_UX.md names 560 dp. Change the doc and this together.",
        )
    }

    @Test
    fun `the dual pane list pane is never wider than the cap`() {
        assertTrue(
            DUAL_PANE_LIST_WIDTH <= SAVED_TRIPS_COLUMN_MAX_WIDTH,
            "A list pane wider than the cap would leave gutters inside the pane, and the cards " +
                "would no longer fill it as rule 45 expects.",
        )
    }
}
