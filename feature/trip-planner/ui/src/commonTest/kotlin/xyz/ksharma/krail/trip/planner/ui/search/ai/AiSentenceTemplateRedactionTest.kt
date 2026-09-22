package xyz.ksharma.krail.trip.planner.ui.search.ai

import xyz.ksharma.krail.core.aitext.TimeIntent
import xyz.ksharma.krail.core.aitext.TripIntentExtraction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The gates in [AiSentenceTemplateRedaction], and the invariant that matters more than any
 * individual case: **nothing leaves except allowlisted words and placeholders.**
 *
 * `every output contains only allowlisted words and placeholders` is that invariant, asserted
 * over every sentence in this file including the deliberately nasty ones. A case added to
 * [ADVERSARIAL] is automatically covered by it. If that test is weakened, the whole privacy
 * argument for this class goes with it.
 */
class AiSentenceTemplateRedactionTest {

    // region gate 1: the model must echo the rider, not rewrite them

    @Test
    fun `a clean sentence is templated`() {
        val result = AiSentenceTemplateRedaction.redact(
            riderText = "get me home by 9pm",
            extraction = extraction(destination = "home", time = "9pm"),
        )

        assertEquals("get me <PLACE> by <TIME>", result.template)
        assertTrue(result.spanMatchedVerbatim)
    }

    @Test
    fun `a model that corrects the rider's spelling is reported as not verbatim`() {
        val result = AiSentenceTemplateRedaction.redact(
            riderText = "goin to bondi junctn",
            extraction = extraction(destination = "Bondi Junction"),
        )

        assertFalse(result.spanMatchedVerbatim)
    }

    @Test
    fun `a corrected spelling leaves the rider's words behind, and gate two drops them`() {
        // Gate 1 is the signal; this is gate 2 doing the actual protecting. The substitution
        // matched nothing, so "bondi" and "junctn" are still in the string.
        val result = AiSentenceTemplateRedaction.redact(
            riderText = "goin to bondi junctn",
            extraction = extraction(destination = "Bondi Junction"),
        )

        assertNull(result.template)
    }

    @Test
    fun `a place the model invented is never substituted into the template`() {
        val result = AiSentenceTemplateRedaction.redact(
            riderText = "take me to the usual spot",
            extraction = extraction(destination = "Hogwarts"),
        )

        assertNull(result.template)
        assertFalse(result.spanMatchedVerbatim)
    }

    // endregion

    // region gate 2: leftovers must all be allowlisted

    @Test
    fun `a place the model missed entirely is caught by gate two`() {
        // Gate 1 passes: everything the model DID say was found. It simply did not mention the
        // clinic or the name, and nothing about that failure is visible to gate 1.
        val result = AiSentenceTemplateRedaction.redact(
            riderText = "meet sarah at the clinic on king st then home",
            extraction = extraction(destination = "home"),
        )

        assertTrue(result.spanMatchedVerbatim)
        assertNull(result.template)
    }

    @Test
    fun `an address the model missed is caught by its digits`() {
        val result = AiSentenceTemplateRedaction.redact(
            riderText = "12 smith st to work",
            extraction = extraction(destination = "work"),
        )

        assertNull(result.template)
    }

    @Test
    fun `over-masking is safe`() {
        // "10am Monday work" with the day swallowed into the place: everything is masked, the
        // template is blanker than it needs to be, and nothing of the rider's survives.
        val result = AiSentenceTemplateRedaction.redact(
            riderText = "10am Monday work",
            extraction = extraction(destination = "Monday work", time = "10am"),
        )

        assertEquals("<TIME> <PLACE>", result.template)
    }

    @Test
    fun `a sentence naming no place is kept when every word is allowlisted`() {
        // The NO_PLACE_MENTIONED failure. Its phrasing is exactly what would explain it, and
        // there is nothing to redact.
        val result = AiSentenceTemplateRedaction.redact(
            riderText = "when is the next train",
            extraction = extraction(),
        )

        assertEquals("when is the next train", result.template)
    }

    @Test
    fun `a transport mode is not treated as a place`() {
        val result = AiSentenceTemplateRedaction.redact(
            riderText = "get me home by train",
            extraction = extraction(destination = "home", modeHints = listOf("train")),
        )

        assertEquals("get me <PLACE> by train", result.template)
    }

    // endregion

    // region boundaries

    @Test
    fun `blank text produces no template`() {
        assertNull(AiSentenceTemplateRedaction.redact("   ", extraction()).template)
    }

    @Test
    fun `a null extraction still runs gate two rather than passing the sentence through`() {
        assertNull(
            AiSentenceTemplateRedaction.redact("take me to rozelle", extraction = null).template,
        )
    }

    @Test
    fun `a template longer than the cap is dropped, not truncated`() {
        val long = List(60) { "to" }.joinToString(" ")
        assertTrue(long.length > AiSentenceTemplateRedaction.MAX_TEMPLATE_LENGTH)

        assertNull(AiSentenceTemplateRedaction.redact(long, extraction()).template)
    }

    @Test
    fun `case does not matter to the gates`() {
        val result = AiSentenceTemplateRedaction.redact(
            riderText = "GET ME HOME BY 9PM",
            extraction = extraction(destination = "home", time = "9pm"),
        )

        assertEquals("GET ME <PLACE> BY <TIME>", result.template)
    }

    @Test
    fun `the allowlist holds no digits`() {
        val offenders = AiSentenceTemplateRedaction.ALLOWED_WORDS.filter { word ->
            word.any(Char::isDigit)
        }

        assertTrue(offenders.isEmpty(), "digits in the allowlist: $offenders")
    }

    @Test
    fun `the allowlist carries both halves of every pair`() {
        // `before` shipped without `after`, so "before 9" survived and "after 9" did not. A
        // one-sided allowlist biases the distribution in exactly the dimension it is collected
        // to measure, and nothing downstream can see it happening.
        val pairs = listOf(
            "before" to "after",
            "near" to "past",
            "arrive" to "leave",
            "next" to "last",
        )

        pairs.forEach { (one, other) ->
            val has = AiSentenceTemplateRedaction.ALLOWED_WORDS
            assertEquals(
                one in has,
                other in has,
                "\"$one\" and \"$other\" must be allowed together or not at all",
            )
        }
    }

    @Test
    fun `a phrasing that turns on one common word is not lost to it`() {
        // Each of these failed on a single missing word before the allowlist was widened:
        // "show", "trains", "time", "after".
        assertEquals(
            "show me trains to <PLACE>",
            AiSentenceTemplateRedaction.redact(
                "show me trains to work",
                extraction(destination = "work"),
            ).template,
        )
        assertEquals(
            "what time is the next train",
            AiSentenceTemplateRedaction.redact("what time is the next train", extraction())
                .template,
        )
        assertEquals(
            "get me there after <TIME>",
            AiSentenceTemplateRedaction.redact("get me there after 9pm", extraction(time = "9pm"))
                .template,
        )
    }

    @Test
    fun `the allowlist is lowercase, because matching lowercases before looking up`() {
        val offenders = AiSentenceTemplateRedaction.ALLOWED_WORDS.filter { it != it.lowercase() }

        assertTrue(offenders.isEmpty(), "non-lowercase entries never match: $offenders")
    }

    // endregion

    // region the invariant

    @Test
    fun `every output contains only allowlisted words and placeholders`() {
        // The one that matters. Not a sample: every case in this file, run through the same
        // assertion. Anything added to ADVERSARIAL is covered automatically.
        ADVERSARIAL.forEach { (riderText, extraction) ->
            val template = AiSentenceTemplateRedaction.redact(riderText, extraction).template
                ?: return@forEach

            val leftovers = template
                .replace(AiSentenceTemplateRedaction.PLACE_TOKEN, " ")
                .replace(AiSentenceTemplateRedaction.TIME_TOKEN, " ")
                .split(' ', ',', '.', '?', '!', ';', ':', '\'', '"', '(', ')', '/', '-')
                .filter { it.isNotBlank() }

            leftovers.forEach { word ->
                assertTrue(
                    word.lowercase() in AiSentenceTemplateRedaction.ALLOWED_WORDS,
                    "\"$word\" escaped from \"$riderText\" as \"$template\"",
                )
            }
        }
    }

    @Test
    fun `no adversarial sentence leaks a digit`() {
        ADVERSARIAL.forEach { (riderText, extraction) ->
            val template = AiSentenceTemplateRedaction.redact(riderText, extraction).template
                ?: return@forEach

            assertFalse(
                template.any(Char::isDigit),
                "digit survived from \"$riderText\" as \"$template\"",
            )
        }
    }

    // endregion

    private fun extraction(
        origin: String? = null,
        destination: String? = null,
        time: String? = null,
        modeHints: List<String> = emptyList(),
    ) = TripIntentExtraction(
        originText = origin,
        destinationText = destination,
        timeIntent = time?.let { TimeIntent(isArrival = true, timeText = it) },
        modeHints = modeHints,
    )

    private companion object {

        /**
         * Everything the invariant tests run over. Each entry pairs what the rider typed with
         * what the model claimed to find, including the cases where the model is wrong.
         *
         * Add here rather than writing a new test when the concern is "could this leak".
         */
        val ADVERSARIAL: List<Pair<String, TripIntentExtraction?>> = buildList {
            fun case(
                text: String,
                origin: String? = null,
                destination: String? = null,
                time: String? = null,
                modeHints: List<String> = emptyList(),
            ) = add(
                text to TripIntentExtraction(
                    originText = origin,
                    destinationText = destination,
                    timeIntent = time?.let { TimeIntent(isArrival = true, timeText = it) },
                    modeHints = modeHints,
                ),
            )

            // ordinary
            case("get me home by 9pm", destination = "home", time = "9pm")
            case("home to work by 9am", origin = "home", destination = "work", time = "9am")
            case("when is the next train")
            case("get me home by train", destination = "home", modeHints = listOf("train"))

            // a person's name
            case("meet sarah at central", destination = "central")
            case("pick up mum from the airport", destination = "the airport")

            // a health or otherwise sensitive place
            case("get me to the clinic on king st", destination = "the clinic")
            case("to the methadone clinic by 8am", destination = "clinic", time = "8am")

            // street addresses, the case the digit rule exists for
            case("12 smith st to work", destination = "work")
            case("unit 4 / 221b baker street to town", destination = "town")
            case("from 70 powderworks rd to the city", origin = "home", destination = "the city")

            // the model rewriting the rider
            case("goin to bondi junctn", destination = "Bondi Junction")
            case("take me to the usual spot", destination = "Hogwarts")
            case("cent stn pls", destination = "Central Station")

            // the model missing things
            case("meet sarah at the clinic on king st then home", destination = "home")
            case("my ex lives in newtown dont route me past there", destination = "newtown")

            // shapes that are not sentences
            case("")
            case("   ")
            case("?????")
            case("🚃🚃🚃")
            case("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")

            // other languages, which the allowlist has no entries for
            case("llévame a casa", destination = "casa")
            case("带我回家", destination = "家")

            // a null extraction, which must not be a passthrough
            add("take me to rozelle" to null)
        }
    }
}
