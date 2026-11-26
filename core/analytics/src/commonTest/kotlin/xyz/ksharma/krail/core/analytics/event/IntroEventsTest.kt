package xyz.ksharma.krail.core.analytics.event

import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.IntroLetsKrailClickEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.IntroLetsKrailClickEvent.FinishedOn
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.IntroLetsKrailClickEvent.InteractionPage
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.IntroLetsKrailClickEvent.LocationResult
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.IntroLetsKrailClickEvent.ParkRideAnswer
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.IntroLetsKrailClickEvent.StopsWait
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.ParkRideUserFacilityEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.SaveTripClickEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.ThemeSelectedEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class IntroEventsTest {

    private val finishedIntro = IntroLetsKrailClickEvent(
        introVersion = 2,
        finishedOn = FinishedOn.FIRST_TRIP,
        parkRideAnswer = ParkRideAnswer.YES,
        locationResult = LocationResult.GRANTED,
        addedParkRide = true,
        savedTrip = true,
        usedSearch = false,
        skipped = false,
        stopsWait = StopsWait.TWO_TO_FIVE_S,
    )

    @Test
    fun `a finished Intro keeps the existing event name`() {
        assertEquals("intro_lets_krail", finishedIntro.name)
    }

    @Test
    fun `a finished Intro sends the nine outcome params with their exact names and values`() {
        assertEquals(
            mapOf(
                "introVersion" to 2,
                "finishedOn" to "first_trip",
                "parkRideAnswer" to "yes",
                "locationResult" to "granted",
                "addedParkRide" to true,
                "savedTrip" to true,
                "usedSearch" to false,
                "skipped" to false,
                "stopsWait" to "2_to_5s",
            ),
            finishedIntro.properties,
        )
    }

    @Test
    fun `a finished Intro sends none of the carousel params`() {
        val keys = finishedIntro.properties.orEmpty().keys

        assertEquals(emptySet(), keys.intersect(setOf("completedOnPage", "completedOnPageNumber")))
    }

    @Test
    fun `the carousel call still sends its two params and nothing else`() {
        val carousel = IntroLetsKrailClickEvent(
            pageType = InteractionPage.PARK_RIDE,
            pageNumber = 6,
        )

        assertEquals("intro_lets_krail", carousel.name)
        assertEquals(
            mapOf("completedOnPage" to "PARK_RIDE", "completedOnPageNumber" to 6),
            carousel.properties,
        )
    }

    @Test
    fun `a param that is null is left out`() {
        assertEquals(emptyMap(), IntroLetsKrailClickEvent().properties)
        assertEquals(
            mapOf("savedTrip" to false),
            IntroLetsKrailClickEvent(savedTrip = false).properties,
        )
    }

    @Test
    fun `every value of each outcome list reaches the event as its own word`() {
        assertEquals(
            setOf("park_ride", "first_trip"),
            FinishedOn.entries.map { event(finishedOn = it).properties?.get("finishedOn") }.toSet(),
        )
        assertEquals(
            setOf("yes", "no", "skipped"),
            ParkRideAnswer.entries
                .map { event(parkRideAnswer = it).properties?.get("parkRideAnswer") }
                .toSet(),
        )
        assertEquals(
            setOf("granted", "granted_no_fix", "denied", "unavailable", "not_asked"),
            LocationResult.entries
                .map { event(locationResult = it).properties?.get("locationResult") }
                .toSet(),
        )
        assertEquals(
            setOf("none", "under_2s", "2_to_5s", "over_5s", "failed"),
            StopsWait.entries.map { event(stopsWait = it).properties?.get("stopsWait") }.toSet(),
        )
    }

    @Test
    fun `the second Intro step has its own screen name and the first keeps its own`() {
        assertEquals("IntroFirstTrip", AnalyticsScreen.IntroFirstTrip.name)
        assertEquals("Intro", AnalyticsScreen.Intro.name)
        assertEquals(
            mapOf("name" to "IntroFirstTrip"),
            AnalyticsEvent.ScreenViewEvent(AnalyticsScreen.IntroFirstTrip).properties,
        )
    }

    @Test
    fun `a chosen colour is sent as themeId, in the form theme_selected sends it`() {
        val event = IntroLetsKrailClickEvent(introVersion = 2, themeId = "4")

        assertEquals(mapOf("introVersion" to 2, "themeId" to "4"), event.properties)
        assertEquals(
            ThemeSelectedEvent(themeId = "4", source = ThemeSelectedEvent.Source.INTRO).properties?.get("themeId"),
            event.properties?.get("themeId"),
        )
    }

    @Test
    fun `themeId is left out when no colour was chosen`() {
        val event = IntroLetsKrailClickEvent(introVersion = 2, finishedOn = FinishedOn.FIRST_TRIP)

        assertEquals(false, event.properties.orEmpty().containsKey("themeId"))
        assertEquals(setOf("introVersion", "finishedOn"), event.properties.orEmpty().keys)
    }

    @Test
    fun `a finished Intro with a colour sends ten params, the nine and themeId`() {
        val event = IntroLetsKrailClickEvent(
            introVersion = 2,
            finishedOn = FinishedOn.FIRST_TRIP,
            parkRideAnswer = ParkRideAnswer.NO,
            locationResult = LocationResult.NOT_ASKED,
            addedParkRide = false,
            savedTrip = false,
            usedSearch = false,
            skipped = true,
            stopsWait = StopsWait.NONE,
            themeId = "7",
        )

        assertEquals(
            mapOf(
                "introVersion" to 2,
                "finishedOn" to "first_trip",
                "parkRideAnswer" to "no",
                "locationResult" to "not_asked",
                "addedParkRide" to false,
                "savedTrip" to false,
                "usedSearch" to false,
                "skipped" to true,
                "stopsWait" to "none",
                "themeId" to "7",
            ),
            event.properties,
        )
    }

    @Test
    fun `the five Intro screens have their own stable names and the two older names stay`() {
        assertEquals(
            listOf("IntroParkRide", "IntroDestination", "IntroOrigin", "IntroConfirm", "IntroTheme"),
            listOf(
                AnalyticsScreen.IntroParkRide,
                AnalyticsScreen.IntroDestination,
                AnalyticsScreen.IntroOrigin,
                AnalyticsScreen.IntroConfirm,
                AnalyticsScreen.IntroTheme,
            ).map { it.name },
        )
        assertEquals("Intro", AnalyticsScreen.Intro.name)
        assertEquals("IntroFirstTrip", AnalyticsScreen.IntroFirstTrip.name)
    }

    @Test
    fun `each Intro screen name is a plain view_screen event with only its name`() {
        listOf(
            AnalyticsScreen.IntroParkRide to "IntroParkRide",
            AnalyticsScreen.IntroDestination to "IntroDestination",
            AnalyticsScreen.IntroOrigin to "IntroOrigin",
            AnalyticsScreen.IntroConfirm to "IntroConfirm",
            AnalyticsScreen.IntroTheme to "IntroTheme",
        ).forEach { (screen, name) ->
            val event = AnalyticsEvent.ScreenViewEvent(screen)

            assertEquals("view_screen", event.name)
            assertEquals(mapOf("name" to name), event.properties)
        }
    }

    @Test
    fun `a Park and Ride change made in the Intro has the intro source`() {
        val event = ParkRideUserFacilityEvent(
            facilityId = "facility-1",
            stopId = "stop-1",
            action = ParkRideUserFacilityEvent.Action.ADD,
            source = ParkRideUserFacilityEvent.Source.INTRO,
        )

        assertEquals("park_ride_user_facility", event.name)
        assertEquals(
            mapOf(
                "facilityId" to "facility-1",
                "stopId" to "stop-1",
                "action" to "add",
                "source" to "intro",
            ),
            event.properties,
        )
    }

    @Test
    fun `the Park and Ride sources are the places a rider adds or removes a station`() {
        // saved_trips_screen counted rows that follow the saved trips by themselves, so it is no longer sent.
        assertEquals(
            setOf("add_park_ride_screen", "intro", "home_suggestion"),
            ParkRideUserFacilityEvent.Source.entries.map { it.value }.toSet(),
        )
    }

    @Test
    fun `a Park and Ride added from the home suggestion has the home suggestion source`() {
        val event = ParkRideUserFacilityEvent(
            facilityId = "facility-1",
            stopId = "stop-1",
            action = ParkRideUserFacilityEvent.Action.ADD,
            source = ParkRideUserFacilityEvent.Source.HOME_SUGGESTION,
        )

        assertEquals("park_ride_user_facility", event.name)
        assertEquals("home_suggestion", event.properties?.get("source"))
    }

    @Test
    fun `a trip saved in the Intro has the intro source`() {
        val event = SaveTripClickEvent(
            fromStopId = "stop-1",
            toStopId = "stop-2",
            source = SaveTripClickEvent.SOURCE_INTRO,
        )

        assertEquals("save_trip_click", event.name)
        assertEquals(
            mapOf("fromStopId" to "stop-1", "toStopId" to "stop-2", "source" to "intro"),
            event.properties,
        )
    }

    @Test
    fun `the existing save trip sources keep their values`() {
        assertEquals("star", SaveTripClickEvent.SOURCE_STAR)
        assertEquals("prompt", SaveTripClickEvent.SOURCE_PROMPT)
        assertEquals("intro", SaveTripClickEvent.SOURCE_INTRO)
        assertEquals(
            "star",
            SaveTripClickEvent(fromStopId = "a", toStopId = "b").properties?.get("source"),
        )
    }

    private fun event(
        finishedOn: FinishedOn? = null,
        parkRideAnswer: ParkRideAnswer? = null,
        locationResult: LocationResult? = null,
        stopsWait: StopsWait? = null,
    ) = IntroLetsKrailClickEvent(
        finishedOn = finishedOn,
        parkRideAnswer = parkRideAnswer,
        locationResult = locationResult,
        stopsWait = stopsWait,
    )
}
