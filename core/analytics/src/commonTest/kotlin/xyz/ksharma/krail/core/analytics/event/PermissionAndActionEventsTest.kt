package xyz.ksharma.krail.core.analytics.event

import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.LoadTimeTableClickEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.ParkRideActionEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.PermissionResultEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.ReverseStopClickEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The exact names, params and words that are sent. The reporting side reads these strings. */
class PermissionAndActionEventsTest {

    private fun answer(
        outcome: PermissionResultEvent.Outcome,
        canAskAgain: Boolean? = null,
    ) = PermissionResultEvent(
        permission = PermissionResultEvent.Permission.LOCATION,
        outcome = outcome,
        source = PermissionResultEvent.Source.INTRO,
        canAskAgain = canAskAgain,
    )

    @Test
    fun `a refusal says whether the system will ask again, as one or zero`() {
        val event = answer(PermissionResultEvent.Outcome.DENIED, canAskAgain = false)

        assertEquals("permission_result", event.name)
        assertEquals(
            mapOf("permission" to "location", "result" to "denied", "source" to "intro", "canAskAgain" to 0),
            event.properties,
        )
        assertEquals(1, answer(PermissionResultEvent.Outcome.DENIED, true).properties?.get("canAskAgain"))
    }

    @Test
    fun `only a refusal carries can ask again`() {
        val others = PermissionResultEvent.Outcome.entries - PermissionResultEvent.Outcome.DENIED

        others.forEach { outcome ->
            assertFalse(answer(outcome, canAskAgain = true).properties.orEmpty().containsKey("canAskAgain"), "$outcome")
        }
    }

    @Test
    fun `the permission, result and source words`() {
        assertEquals(
            listOf("location", "microphone", "speech_recognition"),
            PermissionResultEvent.Permission.entries.map { it.value },
        )
        assertEquals(
            listOf("granted", "granted_approximate", "denied", "cancelled"),
            PermissionResultEvent.Outcome.entries.map { it.value },
        )
        assertEquals(
            listOf(
                "intro",
                "search_stop_pill",
                "home_pill",
                "map_button",
                "journey_map",
                "home_label_chip",
                "ask_krail",
                "park_ride_map",
            ),
            PermissionResultEvent.Source.entries.map { it.value },
        )
    }

    private fun parkRide(
        action: ParkRideActionEvent.Action,
        facilityId: String? = null,
        mapSessionId: String? = null,
    ) = ParkRideActionEvent(
        action = action,
        source = ParkRideActionEvent.Source.HOME_CARD,
        stopId = " 2155384 ",
        facilityId = facilityId,
        mapSessionId = mapSessionId,
    )

    @Test
    fun `directions carry the stop, one car park and the map open when there is one`() {
        val event = parkRide(ParkRideActionEvent.Action.DIRECTIONS, facilityId = "26", mapSessionId = "abc")

        assertEquals("park_ride_action", event.name)
        assertEquals(
            mapOf(
                "action" to "directions",
                "source" to "home_card",
                "stopId" to "2155384",
                "facilityId" to "26",
                "mapSessionId" to "abc",
            ),
            event.properties,
        )
    }

    @Test
    fun `a card open never carries a car park`() {
        val event = parkRide(ParkRideActionEvent.Action.MAP_CARD_OPENED, facilityId = "26")

        assertEquals(
            mapOf("action" to "map_card_opened", "source" to "home_card", "stopId" to "2155384"),
            event.properties,
        )
    }

    @Test
    fun `the parking action sources`() {
        assertEquals(
            listOf("home_card", "stop_map", "side_map", "add_park_ride"),
            ParkRideActionEvent.Source.entries.map { it.value },
        )
    }

    @Test
    fun `a swap says it was made on stop search`() {
        val event = ReverseStopClickEvent(ReverseStopClickEvent.Source.SEARCH_STOP)

        assertEquals("reverse_stop_click", event.name)
        assertEquals(mapOf("source" to "search_stop"), event.properties)
    }

    @Test
    fun `a timetable load says which way it was opened`() {
        fun load(source: LoadTimeTableClickEvent.Source, session: String?) =
            LoadTimeTableClickEvent("1", "2", source, session).properties.orEmpty()

        val chip = load(LoadTimeTableClickEvent.Source.LABEL_CHIP, null)
        assertEquals("label_chip", chip["source"])
        assertFalse(chip.containsKey("searchSessionId"))

        val search = load(LoadTimeTableClickEvent.Source.SEARCH_STOP, "s1")
        assertEquals("search_stop", search["source"])
        assertTrue(search.containsKey("searchSessionId"))
    }

    @Test
    fun `the To tap says it came from the home search bar`() {
        assertEquals(mapOf("source" to "home_search_bar"), AnalyticsEvent.ToFieldClickEvent.properties)
    }

    @Test
    fun `a label set from a home chip has its own surface`() {
        assertEquals("home_chip", AnalyticsEvent.StopLabelSurface.HOME_CHIP.value)
    }

    @Test
    fun `the Intro's parking search words, and the param is left out when unset`() {
        val sent = AnalyticsEvent.IntroLetsKrailClickEvent.ParkRideLookup.entries.map {
            AnalyticsEvent.IntroLetsKrailClickEvent(parkRideLookup = it).properties?.get("parkRideLookup")
        }

        assertEquals(listOf("not_looked", "found_near", "found_wider", "none_near", "failed"), sent)
        assertFalse(AnalyticsEvent.IntroLetsKrailClickEvent().properties.orEmpty().containsKey("parkRideLookup"))
    }

    @Test
    fun `the Intro can say location was allowed and no fix came`() {
        assertEquals("granted_no_fix", AnalyticsEvent.IntroLetsKrailClickEvent.LocationResult.GRANTED_NO_FIX.value)
    }
}
