package xyz.ksharma.krail.trip.planner.ui.timetable

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItem
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.JourneyTimeOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * The auto-refresh rule, as a table. Every row is a (selection, now) pair and whether the
 * board is live.
 *
 * A fixed UTC zone is used throughout so a row means the same thing wherever the suite runs;
 * the timezone is a parameter of the function precisely so the test does not have to guess.
 */
@OptIn(ExperimentalTime::class)
class TimeTableRefreshPolicyTest {

    private val zone = TimeZone.UTC
    private val today = LocalDate.parse("2026-04-09")
    private val noonToday = Instant.parse("2026-04-09T12:00:00Z")

    @Test
    fun `Given each selection and now Then the live-board rule holds`() {
        val cases = listOf(
            Case("no selection at all — the default live board", null, noonToday, expected = true),
            Case("later the same day", leaveAt(today, hour = 18), noonToday, expected = false),
            Case("one minute from now", leaveAt(today, hour = 12, minute = 1), noonToday, expected = false),
            Case("a future date", leaveAt(LocalDate.parse("2026-04-11"), hour = 9), noonToday, expected = false),
            Case("earlier the same day", leaveAt(today, hour = 6), noonToday, expected = true),
            Case("one minute ago", leaveAt(today, hour = 11, minute = 59), noonToday, expected = true),
            Case("a past date", leaveAt(LocalDate.parse("2026-04-01"), hour = 9), noonToday, expected = true),
            Case("exactly now — the pinned moment has arrived", leaveAt(today, hour = 12), noonToday, expected = true),
            Case(
                "an Arrive-by pin later today",
                leaveAt(today, hour = 18).copy(option = JourneyTimeOptions.ARRIVE),
                noonToday,
                expected = false,
            ),
        )

        cases.forEach { case ->
            assertEquals(
                case.expected,
                shouldAutoRefresh(case.selection, case.now, zone),
                case.description,
            )
        }
    }

    @Test
    fun `Given a pin later today When now crosses it Then the board becomes live`() {
        val pinned = leaveAt(today, hour = 18)

        assertEquals(false, shouldAutoRefresh(pinned, noonToday, zone), "before the pinned time")
        assertEquals(
            true,
            shouldAutoRefresh(pinned, Instant.parse("2026-04-09T18:00:01Z"), zone),
            "a second after the pinned time",
        )
    }

    private fun leaveAt(date: LocalDate, hour: Int, minute: Int = 0) = DateTimeSelectionItem(
        option = JourneyTimeOptions.LEAVE,
        hour = hour,
        minute = minute,
        date = date,
    )

    private data class Case(
        val description: String,
        val selection: DateTimeSelectionItem?,
        val now: Instant,
        val expected: Boolean,
    )
}
