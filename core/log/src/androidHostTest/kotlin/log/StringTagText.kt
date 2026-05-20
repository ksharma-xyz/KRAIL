package log

import xyz.ksharma.krail.core.log.toStringTag
import kotlin.test.Test
import kotlin.test.assertEquals

class StringTagText {

    @Test
    fun testToStringTagForSavedTripsViewModel() {
        val stackTraceElement = StackTraceElement(
            "xyz.ksharma.krail.trip.planner.ui.savedtrips.SavedTripsViewModel\$loadSavedTrips\$1",
            "invokeSuspend",
            "SavedTripsViewModel.kt",
            52
        )
        val result = stackTraceElement.toStringTag()
        assertEquals("SavedTripsViewModel", result)
    }

    @Test
    fun testToStringTagForRealAnalytics() {
        val stackTraceElement = StackTraceElement(
            "xyz.ksharma.krail.core.analytics.RealAnalytics",
            "track",
            "RealAnalytics.kt",
            18
        )
        val result = stackTraceElement.toStringTag()
        assertEquals("RealAnalytics", result)
    }

    @Test
    fun testToStringTagForLongClassName() {
        val stackTraceElement = StackTraceElement(
            "xyz.ksharma.krail.core.analytics.ThisIsALongClassNameCalledRealAnalytics",
            "track",
            "RealAnalytics.kt",
            18
        )
        val result = stackTraceElement.toStringTag()
        assertEquals("ThisIsALongClassNameCal", result)
    }
}
