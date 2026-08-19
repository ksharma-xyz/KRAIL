package xyz.ksharma.krail.core.aitext

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * `genai-prompt` has no structural guarantee on its output (unlike iOS's eventual
 * `@Generable` bridge) — [parseTripIntentJson] is the one part of [AndroidAiTextService]
 * that's deterministic and worth pinning with real tests, since it's the layer defending
 * against every way a model can fail to follow the "respond with ONLY JSON" instruction.
 */
class AndroidAiTextServiceTest {

    @Test
    fun `parses well-formed JSON`() {
        val json = """
            {"originText": "home", "destinationText": "Central Station",
             "timeIntent": {"isArrival": true, "timeText": "6:30pm"}, "modeHints": ["train"]}
        """.trimIndent()

        val result = parseTripIntentJson(json)

        assertEquals(
            TripIntentExtraction(
                originText = "home",
                destinationText = "Central Station",
                timeIntent = TimeIntent(isArrival = true, timeText = "6:30pm"),
                modeHints = listOf("train"),
            ),
            result,
        )
    }

    @Test
    fun `parses JSON wrapped in prose the model added despite instructions`() {
        val raw = """
            Sure, here's the JSON:
            {"originText": "home", "destinationText": null, "timeIntent": null, "modeHints": []}
            Let me know if you need anything else!
        """.trimIndent()

        val result = parseTripIntentJson(raw)

        assertEquals(
            TripIntentExtraction(
                originText = "home",
                destinationText = null,
                timeIntent = null,
                modeHints = emptyList(),
            ),
            result,
        )
    }

    @Test
    fun `parses JSON wrapped in a markdown code fence`() {
        val raw = "```json\n{\"originText\": null, \"destinationText\": \"work\", \"timeIntent\": null, \"modeHints\": []}\n```"

        val result = parseTripIntentJson(raw)

        assertEquals("work", result?.destinationText)
    }

    @Test
    fun `missing optional fields fall back to their declared defaults`() {
        val json = """{"originText": "home"}"""

        val result = parseTripIntentJson(json)

        assertEquals(TripIntentExtraction(originText = "home"), result)
    }

    @Test
    fun `returns null for text with no JSON object at all`() {
        assertNull(parseTripIntentJson("Sorry, I don't understand that request."))
    }

    @Test
    fun `returns null for malformed JSON`() {
        assertNull(parseTripIntentJson("""{"originText": "home", "destinationText":}"""))
    }

    @Test
    fun `returns null for empty string`() {
        assertNull(parseTripIntentJson(""))
    }
}
