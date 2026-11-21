package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The switch behind the search row's action button: beside the bars while they keep room for a
 * typical stop name, under the From bar once they would not. The numbers are the row's real ones,
 * a 48 dp button and 16 dp gap beside the bars and 100 dp of end columns inside each bar, and
 * the widths are the row's content width, its window less 16 dp of page padding either side.
 */
class ActionPlacementTest {

    private class Case(val name: String, val width: Dp, val fontScale: Float, val expected: ActionPlacement)

    private val cases = listOf(
        Case("320 dp at 100 percent", 288.dp, 1f, ActionPlacement.Under),
        Case("320 dp at 130 percent", 288.dp, 1.3f, ActionPlacement.Under),
        Case("320 dp at 200 percent", 288.dp, 2f, ActionPlacement.Under),
        Case("360 dp at 100 percent", 328.dp, 1f, ActionPlacement.Beside),
        Case("411 dp at 100 percent", 379.dp, 1f, ActionPlacement.Beside),
        Case("411 dp at 130 percent", 379.dp, 1.3f, ActionPlacement.Beside),
        Case("411 dp at 150 percent", 379.dp, 1.5f, ActionPlacement.Under),
        Case("411 dp at 200 percent", 379.dp, 2f, ActionPlacement.Under),
        Case("a 560 dp column at 200 percent", 528.dp, 2f, ActionPlacement.Beside),
        Case("the 480 dp list pane at 150 percent", 448.dp, 1.5f, ActionPlacement.Beside),
        Case("the 480 dp list pane at 200 percent", 448.dp, 2f, ActionPlacement.Under),
        Case("exactly room for a typical name", 309.dp, 1f, ActionPlacement.Beside),
        Case("half a dp short of it", 308.5.dp, 1f, ActionPlacement.Under),
        Case("no width at all", 0.dp, 1f, ActionPlacement.Under),
        Case("a negative width a measure pass can hand over", (-10).dp, 1f, ActionPlacement.Under),
    )

    private fun place(width: Dp, fontScale: Float) = actionPlacementFor(
        contentWidth = width,
        fontScale = fontScale,
        besideCost = 64.dp,
        barReserved = 100.dp,
    )

    @Test
    fun `placement for every width and text size`() {
        cases.forEach { case ->
            assertEquals(case.expected, place(case.width, case.fontScale), case.name)
        }
    }

    @Test
    fun `growing the text never moves the button back beside the bars`() {
        val scales = listOf(0.85f, 1f, 1.15f, 1.3f, 1.5f, 1.75f, 2f, 3f)
        listOf(288.dp, 328.dp, 379.dp, 448.dp, 528.dp, 700.dp).forEach { width ->
            val placements = scales.map { place(width, it) }
            val firstUnder = placements.indexOf(ActionPlacement.Under)
            if (firstUnder >= 0) {
                assertEquals(
                    List(placements.size - firstUnder) { ActionPlacement.Under },
                    placements.drop(firstUnder),
                    "At $width the button went back beside the bars as the text grew: $placements",
                )
            }
        }
    }

    @Test
    fun `more width never moves the button under the bars`() {
        listOf(1f, 1.3f, 2f).forEach { scale ->
            val placements = (200..800 step 4).map { place(it.dp, scale) }
            val firstBeside = placements.indexOf(ActionPlacement.Beside)
            if (firstBeside >= 0) {
                assertEquals(
                    List(placements.size - firstBeside) { ActionPlacement.Beside },
                    placements.drop(firstBeside),
                    "At ${scale}x the button went under the bars as the width grew.",
                )
            }
        }
    }

    @Test
    fun `a typical name is the measured width of Town Hall Station`() {
        assertEquals(145.dp, TypicalNameWidth)
    }
}
