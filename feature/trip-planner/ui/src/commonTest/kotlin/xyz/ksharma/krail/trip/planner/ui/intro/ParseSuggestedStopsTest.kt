package xyz.ksharma.krail.trip.planner.ui.intro

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The Remote Config value decides what the Intro offers, so every way it can be wrong has to end
 * on the bundled fallback and never on an empty list. A malformed value returns the whole
 * fallback, never a half of it.
 */
class ParseSuggestedStopsTest {

    private val fallback = SuggestedStops(
        start = listOf(SuggestedStop("100", "Fallback Start")),
        end = listOf(SuggestedStop("200", "Fallback End")),
    )

    private val a = stop("1", "A")
    private val b = stop("2", "B")
    private val c = stop("3", "C")

    private fun parse(json: String) = parseSuggestedStops(json, fallback)

    private fun value(start: List<String>, end: List<String>) =
        """{"start":[${start.joinToString(",")}],"end":[${end.joinToString(",")}]}"""

    private fun stop(id: String, name: String) = """{"stopId":"$id","name":"$name"}"""

    @Test
    fun `a valid value is returned in the order given`() {
        val parsed = parse(value(start = listOf(a, b), end = listOf(c)))

        assertEquals(
            SuggestedStops(
                start = listOf(SuggestedStop("1", "A"), SuggestedStop("2", "B")),
                end = listOf(SuggestedStop("3", "C")),
            ),
            parsed,
        )
    }

    @Test
    fun `fields the app does not know are ignored`() {
        val parsed = parse("""{"version":3,"start":[{"stopId":"1","name":"A","emoji":"x"}],"end":[$c]}""")

        assertEquals(listOf(SuggestedStop("1", "A")), parsed.start)
        assertEquals(listOf(SuggestedStop("3", "C")), parsed.end)
    }

    @Test
    fun `text that is not json returns the fallback`() {
        assertEquals(fallback, parse("not json"))
        assertEquals(fallback, parse(""))
    }

    @Test
    fun `a value missing a list or a field returns the fallback`() {
        assertEquals(fallback, parse("""{"start":[$a]}"""))
        assertEquals(fallback, parse("""{"start":[{"stopId":"1"}],"end":[$c]}"""))
    }

    @Test
    fun `an empty list returns the fallback`() {
        assertEquals(fallback, parse(value(start = emptyList(), end = listOf(c))))
        assertEquals(fallback, parse(value(start = listOf(a), end = emptyList())))
    }

    @Test
    fun `a stop id repeated within one list returns the fallback`() {
        assertEquals(fallback, parse(value(start = listOf(a, stop("1", "A again")), end = listOf(c))))
        assertEquals(fallback, parse(value(start = listOf(a), end = listOf(c, stop("3", "C again")))))
    }

    @Test
    fun `a blank stop id or name returns the fallback`() {
        assertEquals(fallback, parse(value(start = listOf(stop(" ", "A")), end = listOf(c))))
        assertEquals(fallback, parse(value(start = listOf(stop("1", "")), end = listOf(c))))
    }

    @Test
    fun `a start that equals the only end returns the fallback`() {
        // Picking that stop as the start would leave the rider with nothing to pick as the end.
        assertEquals(fallback, parse(value(start = listOf(a), end = listOf(a))))
        assertEquals(fallback, parse(value(start = listOf(a, b), end = listOf(a))))
        assertEquals(fallback, parse(value(start = listOf(a), end = listOf(a, b))))
    }

    @Test
    fun `the same stop on both lists is fine while each side keeps another choice`() {
        val parsed = parse(value(start = listOf(a, b), end = listOf(a, b)))

        assertEquals(2, parsed.start.size)
        assertEquals(2, parsed.end.size)
    }

    @Test
    fun `the bundled default passes the same rules it is held to`() {
        val encoded = Json.encodeToString(SuggestedStops.serializer(), DEFAULT_SUGGESTED_STOPS)

        assertEquals(DEFAULT_SUGGESTED_STOPS, parse(encoded))
    }
}
