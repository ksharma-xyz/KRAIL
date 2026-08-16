package xyz.ksharma.krail.trip.planner.ui.search.ai

import xyz.ksharma.krail.core.aitext.TripIntentExtraction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LabelWordFallbackTest {

    private val labels = listOf("Home", "Work")

    @Test
    fun `a sentence whose only place is a label word is rescued`() {
        // Reported: "work by 9am tomm" produced no place at all and the rider was told to name
        // a stop. To the model "work" is an ordinary noun.
        val rescued = TripIntentExtraction().withLabelWordAsDestination("work by 9am tomm", labels)

        assertEquals("work", rescued.destinationText)
        assertNull(rescued.originText, "a one-ended sentence has no origin to invent")
    }

    @Test
    fun `a synonym of a label counts`() {
        val rescued = TripIntentExtraction().withLabelWordAsDestination("office by 9am", labels)

        assertEquals("office", rescued.destinationText)
    }

    @Test
    fun `an extraction that already found a place is never second-guessed`() {
        val original = TripIntentExtraction(destinationText = "Central Station")

        val result = original.withLabelWordAsDestination("home to central station", labels)

        assertEquals("Central Station", result.destinationText)
        assertNull(result.originText)
    }

    @Test
    fun `an origin alone is also left alone`() {
        val original = TripIntentExtraction(originText = "Central Station")

        val result = original.withLabelWordAsDestination("from central station", labels)

        assertEquals("Central Station", result.originText)
        assertNull(result.destinationText)
    }

    @Test
    fun `a word that merely contains a label is not a label`() {
        // Same rule as label matching: Homebush is a place, and it is not this rider's home.
        val result = TripIntentExtraction().withLabelWordAsDestination("homebush at 9am", labels)

        assertNull(result.destinationText)
    }

    @Test
    fun `punctuation does not hide a label word`() {
        val result = TripIntentExtraction().withLabelWordAsDestination("work, by 9am.", labels)

        assertEquals("work", result.destinationText)
    }

    @Test
    fun `a sentence with no label word is left as it was`() {
        val result = TripIntentExtraction().withLabelWordAsDestination("hey how are you", labels)

        assertNull(result.destinationText)
    }

    @Test
    fun `a rider with no labels gets no rescue`() {
        val result = TripIntentExtraction().withLabelWordAsDestination("work by 9am", emptyList())

        assertNull(result.destinationText)
    }

    @Test
    fun `only labels the rider actually has are used`() {
        // "gym" is a known synonym group, but this rider never made that label.
        val result = TripIntentExtraction().withLabelWordAsDestination("gym at 6pm", labels)

        assertNull(result.destinationText)
    }
}
