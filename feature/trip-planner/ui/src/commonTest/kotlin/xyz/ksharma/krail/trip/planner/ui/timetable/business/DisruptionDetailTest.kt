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
                "From 11:10PM, buses replace metro services between Bankstown and Sydenham.",
                "Services run between Sydenham and Tallawong, to the normal frequency.",
            ),
            detail.paragraphs,
        )
        assertEquals(
            listOf(
                LastService(from = "Bankstown", leaves = "23:03", to = "Sydenham", arrives = "23:24"),
                LastService(from = "Sydenham", leaves = "23:15", to = "Bankstown", arrives = "23:37"),
            ),
            detail.lastServices,
        )
    }

    @Test
    fun `Given the date line and the list heading Then neither is repeated`() {
        val paragraphs = parseDisruptionDetail(replaceWithLastServices).paragraphs

        assertTrue(paragraphs.none { it.startsWith("Monday") || it.startsWith("The last") })
    }

    @Test
    fun `Given last services Then the text has no list markers and one line per service`() {
        assertEquals(
            "From 11:10PM, buses replace metro services between Bankstown and Sydenham.\n\n" +
                "Services run between Sydenham and Tallawong, to the normal frequency.\n\n" +
                "Last services\n" +
                "Bankstown 23:03, arrives Sydenham 23:24\n" +
                "Sydenham 23:15, arrives Bankstown 23:37",
            formatDisruptionDescription(replaceWithLastServices),
        )
    }

    @Test
    fun `Given a closure with advice Then every sentence is kept with a blank line between`() {
        assertEquals(
            "From 4AM to 9AM, metro services do not run between Tallawong and Bankstown.\n\n" +
                "Buses replace services between Tallawong and Chatswood, and between Sydenham and Bankstown only.\n\n" +
                "Use trains between Epping or Chatswood, the City and Sydenham.\n\n" +
                "For travel to and from Waterloo use local buses or trains from Redfern.",
            formatDisruptionDescription(doNotRun),
        )
    }

    @Test
    fun `Given text that matches no known phrase Then it is kept without its list marker`() {
        assertEquals(
            listOf("Check indicator screens.", "Expect crowding at Central"),
            parseDisruptionDetail("Check indicator screens.\n- Expect crowding at Central").paragraphs,
        )
    }

    @Test
    fun `Given an empty description Then the text is empty`() {
        assertEquals("", formatDisruptionDescription(""))
    }
}
