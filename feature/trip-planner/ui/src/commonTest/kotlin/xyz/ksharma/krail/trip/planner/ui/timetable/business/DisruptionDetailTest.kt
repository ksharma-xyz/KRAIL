package xyz.ksharma.krail.trip.planner.ui.timetable.business

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Descriptions are copied from the live NSW metro alerts feed, 2026-11-20. */
class DisruptionDetailTest {

    private val replaceWithLastServices = """
        Monday 23 November
        From 11:10PM, buses replace metro services between Bankstown and Sydenham.
        Services run between Sydenham and Tallawong, to the normal frequency.
        The last metro services leave:
        - Bankstown at 23:03, arriving Sydenham at 23:24
        - Sydenham at 23:15, arriving Bankstown at 23:37
    """.trimIndent()

    private val doNotRun = """
        Saturday 5 December
        From 4AM to 9AM, metro services do not run between Tallawong and Bankstown.
        Buses replace services between Tallawong and Chatswood, and between Sydenham and Bankstown only.
        Use trains between Epping or Chatswood, the City and Sydenham.
        For travel to and from Waterloo use local buses or trains from Redfern.
    """.trimIndent()

    @Test
    fun `Given buses replace with last services Then sentences and the list are separated`() {
        val detail = parseDisruptionDetail(replaceWithLastServices)

        assertEquals(
            listOf(
                "From 11:10 PM, buses replace metro services between Bankstown and Sydenham.",
                "Services run between Sydenham and Tallawong, to the normal frequency.",
            ),
            detail.paragraphs,
        )
        assertEquals(
            listOf(
                LastService(from = "Bankstown", leaves = "11:03 PM", to = "Sydenham", arrives = "11:24 PM"),
                LastService(from = "Sydenham", leaves = "11:15 PM", to = "Bankstown", arrives = "11:37 PM"),
            ),
            detail.lastServices,
        )
    }

    @Test
    fun `Given the date line and the list heading Then neither is repeated`() {
        val paragraphs = parseDisruptionDetail(replaceWithLastServices).paragraphs

        assertTrue(paragraphs.none { it.startsWith("Monday") || it.startsWith("The last") })
    }

    private fun bolded(text: String) = factRanges(text).map { text.substring(it.first, it.last + 1) }

    @Test
    fun `Given buses replace Then the time and both stations are bold`() {
        assertEquals(
            listOf("11:10PM", "Bankstown", "Sydenham"),
            bolded("From 11:10PM, buses replace metro services between Bankstown and Sydenham."),
        )
    }

    @Test
    fun `Given a still running line Then both ends are bold and the rest is not`() {
        assertEquals(
            listOf("Sydenham", "Tallawong"),
            bolded("Services run between Sydenham and Tallawong, to the normal frequency."),
        )
    }

    @Test
    fun `Given a time range Then both times are bold`() {
        assertEquals(
            listOf("4AM", "9AM", "Tallawong", "Bankstown"),
            bolded("From 4AM to 9AM, metro services do not run between Tallawong and Bankstown."),
        )
    }

    @Test
    fun `Given advice with several places Then each place is bold`() {
        assertEquals(
            listOf("Epping", "Chatswood", "Sydenham"),
            bolded("Use trains between Epping or Chatswood, the City and Sydenham."),
        )
        assertEquals(
            listOf("Waterloo", "Redfern"),
            bolded("For travel to and from Waterloo use local buses or trains from Redfern."),
        )
    }

    @Test
    fun `Given the full December alert Then every time and place is bold and nothing else`() {
        assertEquals(
            listOf(
                "4AM", "9AM", "Tallawong", "Bankstown", "Tallawong", "Chatswood", "Sydenham", "Bankstown",
                "Epping", "Chatswood", "Sydenham", "Waterloo", "Redfern",
            ),
            bolded(doNotRun),
        )
    }

    @Test
    fun `Given words that are not places Then they are not bold`() {
        assertEquals(emptyList(), bolded("Services run to the normal frequency."))
        assertEquals(emptyList(), bolded("Buses replace services between Platforms."))
    }

    @Test
    fun `Given text in an unfamiliar format Then nothing is bold and nothing is lost`() {
        assertEquals(emptyList(), bolded("trains are delayed after a signal fault"))
    }

    @Test
    fun `Given a 24 hour time Then it is bold`() {
        assertEquals(listOf("23:03", "Sydenham", "23:24"), bolded("Bankstown at 23:03, arriving Sydenham at 23:24"))
    }

    @Test
    fun `Given text that matches no known phrase Then it is kept without its list marker`() {
        assertEquals(
            listOf("Check indicator screens.", "Expect crowding at Central"),
            parseDisruptionDetail("Check indicator screens.\n- Expect crowding at Central").paragraphs,
        )
    }

    @Test
    fun `Given 24 hour times Then they read as 12 hour with am or pm`() {
        assertEquals("11:03 PM", to12Hour("23:03"))
        assertEquals("12:10 AM", to12Hour("00:10"))
        assertEquals("12:00 PM", to12Hour("12:00"))
        assertEquals("1:05 AM", to12Hour("01:05"))
        assertEquals("9:15 AM", to12Hour("9:15"))
    }

    @Test
    fun `Given NSW mixed time styles Then all read the way the app writes them`() {
        assertEquals("From 11:10 PM, buses replace metro", normaliseTimes("From 11:10PM, buses replace metro"))
        assertEquals("From 4 AM to 9 AM, no metro", normaliseTimes("From 4AM to 9AM, no metro"))
        assertEquals("leaves 11:03 PM", normaliseTimes("leaves 23:03"))
        assertEquals("From 4 AM to 9 AM", normaliseTimes(normaliseTimes("From 4AM to 9AM")))
    }

    @Test
    fun `Given last services Then the board times are 12 hour`() {
        val detail = parseDisruptionDetail(replaceWithLastServices)

        assertEquals(listOf("11:03 PM", "11:15 PM"), detail.lastServices.map { it.leaves })
        assertEquals(listOf("11:24 PM", "11:37 PM"), detail.lastServices.map { it.arrives })
    }
}
