package xyz.ksharma.krail.trip.planner.ui.timetable.business

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class DisruptionWindowTest {

    // The metro alert for Bankstown to Sydenham: 11:20 pm Monday to 2:00 am Tuesday, Sydney time.
    private val monNight = 1_795_436_400L
    private val tueEarly = 1_795_446_000L

    // The result keeps date and time parts together with non-breaking spaces; compare as plain spaces.
    private fun window(start: Long, end: Long?, zone: TimeZone = TimeZone.of("Australia/Sydney")) =
        formatDisruptionWindow(start, end, zone).replace('\u00A0', ' ')

    @Test
    fun `Given a window across midnight Then both days are named`() {
        assertEquals(
            "Mon 23 Nov, 11:20 PM to Tue 24 Nov, 2:00 AM",
            window(monNight, tueEarly),
        )
    }

    @Test
    fun `Given a window inside one day Then the date is named once`() {
        // Saturday 5 December, 4 am to 9 am.
        assertEquals("Sat 5 Dec, 4:00 AM to 9:00 AM", window(1_796_403_600L, 1_796_421_600L))
    }

    @Test
    fun `Given no end Then it reads from the start`() {
        assertEquals("From Mon 23 Nov, 11:20 PM", window(monNight, null))
    }

    @Test
    fun `Given midnight and noon Then they read 12 AM and 12 PM`() {
        // Mon 23 Nov 00:05 and 12:00 Sydney time.
        assertEquals("Mon 23 Nov, 12:05 AM to 12:00 PM", window(1_795_352_700L, 1_795_395_600L))
        assertEquals("Mon 23 Nov, 12:00 PM to 1:00 PM", window(1_795_395_600L, 1_795_399_200L))
    }

    @Test
    fun `Given another time zone Then the clock follows that zone`() {
        // The same instant is 12:20 pm in UTC, not 11:20 pm in Sydney.
        assertEquals("From Mon 23 Nov, 12:20 PM", window(monNight, null, TimeZone.UTC))
    }

    @Test
    fun `Given a window Then the parts of a time and a date never break apart`() {
        val raw = formatDisruptionWindow(monNight, tueEarly)

        assertEquals(
            "Mon\u00A023\u00A0Nov, 11:20\u00A0PM to Tue\u00A024\u00A0Nov, 2:00\u00A0AM",
            raw,
        )
    }
}
