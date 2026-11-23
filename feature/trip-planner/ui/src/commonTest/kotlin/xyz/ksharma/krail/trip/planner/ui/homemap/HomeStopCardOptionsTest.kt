package xyz.ksharma.krail.trip.planner.ui.homemap

import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import xyz.ksharma.krail.trip.planner.ui.stopcard.PickRole
import kotlin.test.Test
import kotlin.test.assertEquals

/** What a stop card on home offers and the event its Save as raises, with no screen. */
class HomeStopCardOptionsTest {

    private val home = StopLabel(emoji = "H", label = "Home")
    private val gym = StopLabel(emoji = "G", label = "Gym")
    private val central = StopItem(stopId = "200060", stopName = "Central Station")

    @Test
    fun `home picks a destination and never a start`() {
        val options = homeStopCardOptions(labels = persistentListOf(home), onEvent = {})

        assertEquals(PickRole.Destination, options.pickRole)
    }

    @Test
    fun `home offers the labels it holds`() {
        val options = homeStopCardOptions(labels = persistentListOf(home, gym), onEvent = {})

        assertEquals(listOf(home, gym), options.labels)
    }

    @Test
    fun `save as raises the event the chips use when a stop is chosen for a label`() {
        val events = mutableListOf<SavedTripUiEvent>()
        val options = homeStopCardOptions(labels = persistentListOf(home, gym), onEvent = { events += it })

        options.onSaveAs(gym, central)

        assertEquals(
            listOf<SavedTripUiEvent>(SavedTripUiEvent.LabelStopAssigned("Gym", "200060", "Central Station")),
            events,
        )
    }
}
