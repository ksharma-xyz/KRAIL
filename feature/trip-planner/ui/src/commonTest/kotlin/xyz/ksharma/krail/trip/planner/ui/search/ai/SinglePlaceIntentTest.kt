package xyz.ksharma.krail.trip.planner.ui.search.ai

import xyz.ksharma.krail.core.aitext.TripIntentExtraction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SinglePlaceIntentTest {

    @Test
    fun `a lone place in a going-somewhere sentence is the destination`() {
        // The bug this exists for: "let's go home" filled the From field with the rider's
        // home station and left the destination empty.
        val corrected = TripIntentExtraction(originText = "home")
            .withSinglePlaceInTheRightField("let's go home")

        assertNull(corrected.originText)
        assertEquals("home", corrected.destinationText)
    }

    @Test
    fun `take me to X is a destination too`() {
        val corrected = TripIntentExtraction(originText = "Town Hall")
            .withSinglePlaceInTheRightField("take me to Town Hall")

        assertNull(corrected.originText)
        assertEquals("Town Hall", corrected.destinationText)
    }

    @Test
    fun `a lone place the rider is leaving stays the origin`() {
        val corrected = TripIntentExtraction(originText = "Central")
            .withSinglePlaceInTheRightField("leaving Central around nine")

        assertEquals("Central", corrected.originText)
        assertNull(corrected.destinationText)
    }

    @Test
    fun `from X stays the origin`() {
        val corrected = TripIntentExtraction(originText = "Central")
            .withSinglePlaceInTheRightField("from Central")

        assertEquals("Central", corrected.originText)
        assertNull(corrected.destinationText)
    }

    @Test
    fun `both ends given is left alone`() {
        val extraction = TripIntentExtraction(originText = "home", destinationText = "work")
        val corrected = extraction.withSinglePlaceInTheRightField("home to work")

        assertEquals("home", corrected.originText)
        assertEquals("work", corrected.destinationText)
    }

    @Test
    fun `only a destination is left alone`() {
        val corrected = TripIntentExtraction(destinationText = "work")
            .withSinglePlaceInTheRightField("go to work")

        assertNull(corrected.originText)
        assertEquals("work", corrected.destinationText)
    }
}
