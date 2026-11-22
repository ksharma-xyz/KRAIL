package xyz.ksharma.krail.trip.planner.ui.parkride.suggestion

import kotlin.test.Test
import kotlin.test.assertEquals

/** When the card's two buttons sit beside each other and when they stack, judged on what each asks for alone. */
class SuggestionActionsLayoutTest {

    private class Case(
        val name: String,
        val notNow: Int,
        val addToHome: Int,
        val gap: Int,
        val available: Int,
        val expected: SuggestionActionsLayout,
    )

    @Test
    fun `two whole buttons sit beside each other only when both fit with the gap`() {
        listOf(
            Case("plenty of room", 90, 130, 8, 300, SuggestionActionsLayout.Beside),
            Case("exactly enough", 90, 130, 8, 228, SuggestionActionsLayout.Beside),
            Case("one pixel short", 90, 130, 8, 227, SuggestionActionsLayout.Stacked),
            Case("the gap is part of the room", 100, 100, 8, 200, SuggestionActionsLayout.Stacked),
            Case("the narrowest window at large text", 180, 260, 8, 256, SuggestionActionsLayout.Stacked),
            Case("no room at all", 90, 130, 8, 0, SuggestionActionsLayout.Stacked),
            Case("buttons with no width fit anywhere", 0, 0, 8, 8, SuggestionActionsLayout.Beside),
        ).forEach { case ->
            assertEquals(
                case.expected,
                suggestionActionsLayout(case.notNow, case.addToHome, case.gap, case.available),
                case.name,
            )
        }
    }

    @Test
    fun `a wider window never turns beside into stacked`() {
        val layouts = (0..400 step 4).map { available ->
            suggestionActionsLayout(notNowWidth = 90, addToHomeWidth = 130, gap = 8, available = available)
        }

        // Once the buttons fit they keep fitting as the window grows: stacked first, then beside, never back.
        val firstBeside = layouts.indexOf(SuggestionActionsLayout.Beside)
        assertEquals(true, firstBeside > 0)
        assertEquals(true, layouts.drop(firstBeside).all { it == SuggestionActionsLayout.Beside })
    }
}
