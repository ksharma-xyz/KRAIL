package xyz.ksharma.krail.trip.planner.ui.intro

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Intro saves a trip from two stop ids with no network, so a stop that a data refresh renames,
 * demotes to a platform or drops would save a trip the planner cannot use. This reads the real
 * bundled stops file and fails the refresh in CI.
 */
class DefaultSuggestedStopsDataTest {

    private fun problems(suggestions: List<SuggestedStop>): List<String> = suggestions.mapNotNull { stop ->
        val bundled = BundledStops.byId[stop.stopId]
        when {
            bundled == null -> "${stop.stopId} (${stop.name}) is not in NSW_STOPS.pb"
            !bundled.isParent ->
                "${stop.stopId} is a platform or stand (${bundled.name}), not the parent station"
            bundled.name != stop.name -> "${stop.stopId} is now named ${bundled.name}, not ${stop.name}"
            else -> null
        }
    }

    private fun failureMessage(found: List<String>) =
        "DEFAULT_SUGGESTED_STOPS in SuggestedStops.kt no longer matches the bundled stops data. " +
            "Change the constant to the station level stop that exists now:\n" + found.joinToString("\n")

    @Test
    fun `every default suggested stop is a parent station in the bundled stops with the expected name`() {
        val found = problems(DEFAULT_SUGGESTED_STOPS.start + DEFAULT_SUGGESTED_STOPS.end)

        assertTrue(found.isEmpty(), failureMessage(found))
    }

    @Test
    fun `the check reports a missing stop, a platform and a renamed stop`() {
        val found = problems(
            listOf(
                SuggestedStop("999999999", "Nowhere Station"),
                SuggestedStop("200017", "Central Station, Railway Square, Stand J"),
                SuggestedStop("200060", "Central"),
                SuggestedStop("200060", "Central Station"),
            ),
        )

        assertEquals(3, found.size, failureMessage(found))
    }
}
