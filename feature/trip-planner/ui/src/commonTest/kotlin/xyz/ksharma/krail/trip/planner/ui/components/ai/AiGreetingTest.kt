package xyz.ksharma.krail.trip.planner.ui.components.ai

import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The suggestion line is the only thing on this screen the rider does not already know, so the
 * rules that build it are worth holding. Every case here is arithmetic on the clock and their
 * saved data, which means none of it needs a device to check.
 */
class AiGreetingTest {

    private val home = StopLabel(emoji = "🏠", label = "Home", stopId = "1", stopName = "Seven Hills Station")
    private val work = StopLabel(emoji = "💼", label = "Work", stopId = "2", stopName = "Wynyard Station")
    private val gym = StopLabel(emoji = "🏋", label = "Gym", stopId = "3", stopName = "Bondi Junction")
    private val unsetWork = StopLabel(emoji = "💼", label = "Work")
    private val savedTrip = Trip(
        fromStopId = "1",
        fromStopName = "Seven Hills Station",
        toStopId = "2",
        toStopName = "Wynyard Station",
    )

    private fun suggestion(
        hour: Int,
        isWeekend: Boolean = false,
        labels: List<StopLabel> = emptyList(),
        savedTrips: List<Trip> = emptyList(),
        isSunday: Boolean = false,
    ) = suggestionFor(
        hour = hour,
        isWeekend = isWeekend,
        labels = labels,
        savedTrips = savedTrips,
        isSunday = isSunday,
    )

    // region Always a complete line

    @Test
    fun `every hour of every kind of day produces a complete line`() {
        // A half-built suggestion is worse than a generic one, and the empty-data case is the
        // state every rider is in exactly once.
        listOf(true, false).forEach { weekend ->
            (0..23).forEach { hour ->
                val line = suggestion(hour, weekend)
                assertTrue(line.isNotBlank(), "hour $hour weekend=$weekend produced nothing")
                assertTrue(line.contains(" to "), "\"$line\" is not a journey")
            }
        }
    }

    @Test
    fun `no line outgrows the fixed height slot`() {
        // The slot cannot grow without moving the field, which is the one thing this layout
        // promises never to do.
        val longNames = listOf(
            StopLabel(emoji = "🏠", label = "Home", stopId = "1", stopName = "Seven Hills Station"),
            StopLabel(emoji = "💼", label = "Work", stopId = "2", stopName = "Wynyard Station"),
        )
        val longTrip = Trip(
            fromStopId = "1",
            fromStopName = "International Airport Station",
            toStopId = "2",
            toStopName = "Sydney Olympic Park Station",
        )
        (0..23).forEach { hour ->
            listOf(true, false).forEach { weekend ->
                listOf(
                    suggestion(hour, weekend, labels = longNames),
                    suggestion(hour, weekend, savedTrips = listOf(longTrip)),
                ).forEach { line ->
                    assertTrue(line.length <= LONGEST_LINE_FOR_TEST, "\"$line\" is ${line.length} chars")
                }
            }
        }
    }

    // endregion

    // region Weekends

    @Test
    fun `a weekend never suggests the commute`() {
        // About a third of trips here are loaded at a weekend. Suggesting work on a Sunday is
        // the app saying out loud that it has not been paying attention.
        listOf("Work", "Uni", "School").forEach { commuteLabel ->
            val labels = listOf(home, StopLabel(emoji = "💼", label = commuteLabel, stopId = "2", stopName = "Wynyard"))
            (0..23).forEach { hour ->
                val line = suggestion(hour, isWeekend = true, labels = labels)
                assertFalse(
                    line.contains(commuteLabel, ignoreCase = true),
                    "\"$line\" suggests $commuteLabel at the weekend",
                )
            }
        }
    }

    @Test
    fun `a weekend does not hand back the commute through a saved trip`() {
        // The bug this rule exists for: the label rung is skipped at weekends, and a rider
        // whose only saved trip IS the trip to work then saw exactly that on a Sunday.
        val commuteTrip = Trip(
            fromStopId = "1",
            fromStopName = "Seven Hills Station",
            toStopId = "2",
            toStopName = "Wynyard Station",
        )
        val line = suggestion(
            hour = 11,
            isWeekend = true,
            labels = listOf(home, work),
            savedTrips = listOf(commuteTrip),
        )

        // Falls to home rather than to strangers' stations. Suggesting the commute here is what
        // the weekend rule exists to stop; suggesting Central to Bondi Junction was what it did
        // instead, and that told the rider nothing about their own trips.
        assertFalse(line.contains("Wynyard"), "\"$line\" is the commute on a weekend")
        assertTrue(line.startsWith("Get me home"), "\"$line\" should offer to get them home")
    }

    @Test
    fun `a weekend keeps a saved trip that is not the commute`() {
        val beachTrip = Trip(
            fromStopId = "9",
            fromStopName = "Central Station",
            toStopId = "8",
            toStopName = "Bondi Junction",
        )
        val line = suggestion(
            hour = 11,
            isWeekend = true,
            labels = listOf(home, work),
            savedTrips = listOf(beachTrip),
        )

        assertTrue(line.startsWith("Central to Bondi Junction"), "\"$line\" dropped a fine trip")
    }

    @Test
    fun `a weekend uses another named place when there is one`() {
        val line = suggestion(hour = 10, isWeekend = true, labels = listOf(home, work, gym))

        assertTrue(line.startsWith("Home to Gym"), "\"$line\" should use the non-commute label")
    }

    @Test
    fun `a weekday prefers the commute label over other places`() {
        val line = suggestion(hour = 8, isWeekend = false, labels = listOf(home, gym, work))

        assertTrue(line.startsWith("Home to Work"), "\"$line\" should lead with the commute")
    }

    // endregion

    // region Which source wins

    @Test
    fun `labels are used in the rider's own words`() {
        // The only place the app ever shows that typing "work" resolves to their stop.
        val line = suggestion(hour = 8, labels = listOf(home, work))

        assertTrue(line.startsWith("Home to Work"), "\"$line\" should use the labels, not stop names")
    }

    @Test
    fun `a label with no stop pinned to it is not a label`() {
        // It exists from the moment it is created. Suggesting it would teach a trick the app
        // cannot do, so this falls through to the saved trip.
        val line = suggestion(hour = 8, labels = listOf(home, unsetWork), savedTrips = listOf(savedTrip))

        assertTrue(line.startsWith("Seven Hills to Wynyard"), "\"$line\" should have fallen to the saved trip")
    }

    @Test
    fun `a saved trip is shown as station names with the suffix trimmed`() {
        val line = suggestion(hour = 8, savedTrips = listOf(savedTrip))

        assertTrue(line.startsWith("Seven Hills to Wynyard"), "\"$line\" kept its Station suffix")
    }

    @Test
    fun `a rider with nothing at all still gets a real journey`() {
        val line = suggestion(hour = 11)

        assertTrue(line.startsWith("Central to Parramatta"), "\"$line\" is not a real journey")
    }

    // endregion

    // region Time

    @Test
    fun `every suggestion carries a time`() {
        // The time is the only thing this screen understands that the search row does not. A
        // rider who never sees it demonstrated has no reason to believe they can type it.
        listOf(true, false).forEach { weekend ->
            (0..23).forEach { hour ->
                val line = suggestion(hour, weekend, labels = listOf(home, gym))
                assertTrue(
                    TIME_WORDS.any { line.contains(it) },
                    "\"$line\" at hour $hour carries no time",
                )
            }
        }
    }

    @Test
    fun `no suggestion proposes a time that has already passed`() {
        // "by 9am" at 11am is the app suggesting a trip into the past, which reads as broken
        // rather than as an example.
        (10..23).forEach { hour ->
            val line = suggestion(hour, labels = listOf(home, work))
            assertFalse(line.contains("by 9am"), "\"$line\" at $hour points backwards")
        }
        (18..23).forEach { hour ->
            val line = suggestion(hour, labels = listOf(home, work))
            assertFalse(line.contains("after 6pm"), "\"$line\" at $hour points backwards")
        }
    }

    @Test
    fun `the journey runs back from mid afternoon`() {
        assertTrue(suggestion(hour = 8, labels = listOf(home, work)).startsWith("Home to Work"))
        assertTrue(suggestion(hour = 17, labels = listOf(home, work)).startsWith("Work to Home"))
    }

    // endregion

    @Test
    fun `a rider whose only data is the commute is offered home at the weekend`() {
        // The reported bug: every weekend showed "Central to Bondi Junction". The weekend rules
        // disqualified the labels AND the saved trip, and the ladder fell to two stations the
        // rider has nothing to do with.
        val commuteTrip = Trip(
            fromStopId = "1",
            fromStopName = "Seven Hills Station",
            toStopId = "2",
            toStopName = "Wynyard Station",
        )
        val line = suggestion(
            hour = 11,
            isWeekend = true,
            labels = listOf(home, work),
            savedTrips = listOf(commuteTrip),
        )

        assertTrue(line.startsWith("Get me home"), "\"$line\" fell through to strangers' stops")
        assertFalse(line.contains("Central"), "\"$line\" is not this rider's journey")
    }

    @Test
    fun `a sunday evening offers tomorrow's commute`() {
        val line = suggestion(hour = 18, isWeekend = true, isSunday = true, labels = listOf(home, work))

        assertTrue(line.startsWith("Home to Work"), "\"$line\" should plan the commute")
        assertTrue(line.endsWith("by 9am tomorrow"), "\"$line\" has to say tomorrow")
    }

    @Test
    fun `a weekday evening offers to get them home, with a time`() {
        // The time is the point: it is how a rider learns one can go in at all.
        val line = suggestion(hour = 19, labels = listOf(home, work))

        assertEquals("Get me home by 9pm", line)
    }

    @Test
    fun `no home label means no home-bound line`() {
        // Nothing to get them to, so it falls to the generic pair rather than promising a home
        // that is not set.
        val line = suggestion(hour = 19, labels = emptyList())

        assertFalse(line.contains("Get me home"), "\"$line\" promises a home that is not set")
    }

    private companion object {
        // Mirrors LONGEST_LINE in AiGreeting.kt. Duplicated rather than exposed: the production
        // constant is an implementation detail, and a test that reads it cannot fail when it
        // changes.
        const val LONGEST_LINE_FOR_TEST = 38
        val TIME_WORDS = listOf("by 9am", "in 20 minutes", "after 6pm", "by 9pm")
    }
}
