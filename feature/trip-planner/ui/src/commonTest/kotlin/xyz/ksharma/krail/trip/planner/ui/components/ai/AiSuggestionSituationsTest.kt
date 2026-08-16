package xyz.ksharma.krail.trip.planner.ui.components.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The situations table is meant to be added to. These hold the properties that make adding a row
 * safe, so a seventh situation cannot silently open a gap or shadow an existing one.
 */
class AiSuggestionSituationsTest {

    @Test
    fun `a sunday evening plans tomorrow's commute`() {
        // The one weekend moment when the commute is what somebody is actually thinking about.
        val situation = situationFor(hour = 18, isWeekend = true, isSunday = true)

        assertEquals("sunday-evening", situation.id)
        assertTrue(situation.allowsCommute, "Sunday evening has to be allowed the commute")
        assertTrue(situation.clause.contains("tomorrow"), "or it reads as a trip for tonight")
    }

    @Test
    fun `a saturday at the same hour does not`() {
        val situation = situationFor(hour = 18, isWeekend = true, isSunday = false)

        assertEquals("weekend-evening", situation.id)
        assertFalse(situation.allowsCommute)
    }

    @Test
    fun `no weekend situation outside sunday evening allows the commute`() {
        suggestionSituations()
            .filter { it.dayKind == DayKind.Weekend }
            .forEach { assertFalse(it.allowsCommute, "${it.id} would suggest work at a weekend") }
    }

    @Test
    fun `every hour of every kind of day has a situation`() {
        // The gap this prevents is invisible: `situationFor` falls back to the first row, so a
        // missed hour shows a wrong suggestion rather than crashing, and nobody notices for
        // months. The wrapping band past midnight is where it would happen.
        listOf(true, false).forEach { weekend ->
            (0..23).forEach { hour ->
                val matches = suggestionSituations().filter { situation ->
                    val dayMatches = situation.dayKind == DayKind.Any ||
                        (situation.dayKind == DayKind.Weekend) == weekend
                    val hourMatches = if (situation.fromHour <= situation.toHour) {
                        hour in situation.fromHour until situation.toHour
                    } else {
                        hour >= situation.fromHour || hour < situation.toHour
                    }
                    dayMatches && hourMatches
                }
                assertTrue(
                    matches.isNotEmpty(),
                    "hour $hour weekend=$weekend matches no situation",
                )
            }
        }
    }

    @Test
    fun `every situation has a unique id`() {
        // Ids are how config and tests name a row without quoting its copy. Two rows sharing one
        // makes the second unaddressable.
        val ids = suggestionSituations().map { it.id }

        assertEquals(ids.size, ids.toSet().size, "duplicate situation id in $ids")
    }

    @Test
    fun `every situation asks for a time`() {
        // A situation with no clause would quietly drop the one capability this screen has that
        // the search row does not.
        suggestionSituations().forEach { situation ->
            assertTrue(situation.clause.isNotBlank(), "${situation.id} carries no time")
            assertTrue(situation.clause.startsWith(" "), "${situation.id} would run into the stop name")
        }
    }

    @Test
    fun `an absolute clause never appears in a band that has already passed it`() {
        // "by 9am" at 11am is a trip into the past. Checked against the band rather than against
        // one sampled hour, so a row widened later fails here instead of on a phone.
        suggestionSituations().forEach { situation ->
            // "by 9am tomorrow" names an hour that has not happened yet BECAUSE it says
            // tomorrow, so the band it sits in is irrelevant.
            if (situation.clause.contains("tomorrow")) return@forEach

            ABSOLUTE_CLAUSES.forEach { (clause, deadlineHour) ->
                if (situation.clause.contains(clause)) {
                    assertTrue(
                        situation.toHour <= deadlineHour && situation.fromHour <= situation.toHour,
                        "${situation.id} offers \"$clause\" in a band running to ${situation.toHour}",
                    )
                }
            }
        }
    }

    @Test
    fun `no weekend situation asks to arrive by nine`() {
        // Nobody is being got to work for nine on a Saturday.
        suggestionSituations()
            .filter { it.dayKind == DayKind.Weekend }
            .forEach { situation ->
                assertTrue(
                    !situation.clause.contains("9am"),
                    "${situation.id} suggests a nine o'clock start at a weekend",
                )
            }
    }

    @Test
    fun `the journey turns around in the afternoon on both kinds of day`() {
        assertEquals(Direction.Out, situationFor(hour = 8, isWeekend = false).direction)
        assertEquals(Direction.Back, situationFor(hour = 17, isWeekend = false).direction)
        assertEquals(Direction.Out, situationFor(hour = 10, isWeekend = true).direction)
        assertEquals(Direction.Back, situationFor(hour = 19, isWeekend = true).direction)
    }

    @Test
    fun `the first matching situation wins`() {
        // Order is the priority, so a row added at the top must be able to shadow one below it.
        // That is the mechanism by which a special case (a holiday, an event) gets added later.
        val special = SuggestionSituation(
            id = "special",
            dayKind = DayKind.Any,
            fromHour = 0,
            toHour = 24,
            shape = SuggestionShape.Pair,
            direction = Direction.Back,
            allowsCommute = true,
            clause = CLAUSE_SOON,
        )
        val situations = listOf(special) + suggestionSituations()

        assertEquals("special", situationFor(hour = 8, isWeekend = false, situations = situations).id)
    }

    private companion object {
        /** Clause to the hour by which it stops being in the future. */
        val ABSOLUTE_CLAUSES = mapOf("9am" to 9, "6pm" to 18, "9pm" to 21)
    }
}
