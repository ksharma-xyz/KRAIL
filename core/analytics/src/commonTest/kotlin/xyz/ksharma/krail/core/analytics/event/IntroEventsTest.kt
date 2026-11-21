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
            setOf("granted", "denied", "unavailable", "not_asked"),
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
    fun `the existing Park and Ride sources keep their values`() {
        assertEquals(
            setOf("add_park_ride_screen", "saved_trips_screen", "intro"),
            ParkRideUserFacilityEvent.Source.entries.map { it.value }.toSet(),
        )
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
