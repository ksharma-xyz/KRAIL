package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.navigation.StopSelectedResult
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.LocationKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The stop search's result as the Intro's event, and the field the Intro asks the search to open. */
class IntroSearchResultTest {

    private fun result(fieldType: SearchStopFieldType, kind: LocationKind = LocationKind.TRANSIT_STOP) =
        StopSelectedResult(
            fieldType = fieldType,
            stopId = "10101",
            stopName = "Parramatta Station",
            locationKind = kind,
        )

    @Test
    fun `a pick for the From field is a pick of the start`() {
        assertEquals(
            IntroUiEvent.StopPickedFromSearch(isStart = true, stopId = "10101", stopName = "Parramatta Station"),
            result(SearchStopFieldType.FROM).toIntroEvent(),
        )
    }

    @Test
    fun `a pick for the To field is a pick of the end`() {
        assertEquals(
            IntroUiEvent.StopPickedFromSearch(isStart = false, stopId = "10101", stopName = "Parramatta Station"),
            result(SearchStopFieldType.TO).toIntroEvent(),
        )
    }

    @Test
    fun `a pick for a stop label is not for the Intro`() {
        assertNull(result(SearchStopFieldType.LABEL).toIntroEvent())
    }

    @Test
    fun `an address pick carries its id and name like a stop`() {
        assertEquals(
            IntroUiEvent.StopPickedFromSearch(isStart = true, stopId = "10101", stopName = "Parramatta Station"),
            result(SearchStopFieldType.FROM, LocationKind.ADDRESS).toIntroEvent(),
        )
    }

    @Test
    fun `the start opens the From field and the end opens the To field`() {
        assertEquals(SearchStopFieldType.FROM, introSearchFieldType(isStart = true))
        assertEquals(SearchStopFieldType.TO, introSearchFieldType(isStart = false))
    }
}
