package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.trip.planner.ui.intro.DEFAULT_SUGGESTED_STOPS
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.ListState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** What the recents card holds (`docs/MAP_HOME_UX.md` rule 9), with real Sydney stops and no layout. */
class SearchStopRecentsTest {

    // A rider standing at Town Hall.
    private val townHallRider = LatLng(-33.8734, 151.2069)
    private val townHall = nearby("200070", "Town Hall Station", -33.8734, 151.2069)
    private val wynyard = nearby("200080", "Wynyard Station", -33.8660, 151.2059)
    private val central = nearby("200060", "Central Station", -33.8832, 151.2063)
    private val parramatta = nearby("215020", "Parramatta Station", -33.8176, 151.0054)

    private val recents = (1..8).map { row("r$it", "Recent $it") }
    private val curated = curatedStopRows(SearchStopFieldType.TO, DEFAULT_SUGGESTED_STOPS)

    private val homeSet = StopLabel(emoji = "H", label = "Home", stopId = "200060", stopName = "Central Station")
    private val workUnset = StopLabel(emoji = "W", label = "Work")

    private fun row(id: String, name: String) = SearchStopState.StopResult(stopName = name, stopId = id)

    private fun nearby(id: String, name: String, lat: Double, lon: Double) = NearbyStopFeature(
        stopId = id,
        stopName = name,
        position = LatLng(lat, lon),
        transportModes = persistentListOf(TransportMode.Train),
    )

    // region the three sources

    @Test
    fun `recents win and stop at five rows`() {
        val card = recentsCardFor(recents, locationKnown = true, nearest = curated, curated = curated)

        assertEquals(RecentsSource.Recents, card?.source)
        assertEquals(listOf("r1", "r2", "r3", "r4", "r5"), card?.rows?.map { it.stopId })
        assertEquals(MAX_RECENT_ROWS, card?.rows?.size)
    }

    @Test
    fun `fewer than five recents show all of them`() {
        val card = recentsCardFor(recents.take(2), locationKnown = false, nearest = emptyList(), curated = curated)

        assertEquals(RecentsSource.Recents, card?.source)
        assertEquals(2, card?.rows?.size)
    }

    @Test
    fun `no recents and a location show the nearest stops`() {
        val nearest = nearestStopRows(townHallRider, listOf(central, townHall, wynyard))
        val card = recentsCardFor(emptyList(), locationKnown = true, nearest = nearest, curated = curated)

        assertEquals(RecentsSource.Nearest, card?.source)
        assertEquals(listOf("200070", "200080", "200060"), card?.rows?.map { it.stopId })
    }

    @Test
    fun `no recents and no location show the curated stops`() {
        val card = recentsCardFor(emptyList(), locationKnown = false, nearest = emptyList(), curated = curated)

        assertEquals(RecentsSource.Curated, card?.source)
        assertEquals(DEFAULT_SUGGESTED_STOPS.end.map { it.stopId }, card?.rows?.map { it.stopId })
    }

    @Test
    fun `a location whose nearby stops are still loading shows nothing, not the curated stops`() {
        assertNull(recentsCardFor(emptyList(), locationKnown = true, nearest = emptyList(), curated = curated))
    }

    @Test
    fun `nothing at all shows no card`() {
        assertNull(recentsCardFor(emptyList(), locationKnown = false, nearest = emptyList(), curated = emptyList()))
    }

    // endregion

    // region nearest

    @Test
    fun `the nearest stops come nearest first and stop at the limit`() {
        val rows = nearestStopRows(townHallRider, listOf(parramatta, central, wynyard, townHall), limit = 3)

        assertEquals(listOf("200070", "200080", "200060"), rows.map { it.stopId })
    }

    @Test
    fun `equal distances go to the smaller stop id`() {
        val a = nearby("200", "A", -33.8700, 151.2000)
        val b = nearby("100", "B", -33.8700, 151.2000)

        assertEquals(listOf("100", "200"), nearestStopRows(townHallRider, listOf(a, b)).map { it.stopId })
        assertEquals(listOf("100", "200"), nearestStopRows(townHallRider, listOf(b, a)).map { it.stopId })
    }

    @Test
    fun `a radius leaves out a stop further away and keeps one on the edge`() {
        val withRadius = nearestStopRows(townHallRider, listOf(townHall, parramatta), radiusKm = 1.0)

        assertEquals(listOf("200070"), withRadius.map { it.stopId })
        assertEquals(2, nearestStopRows(townHallRider, listOf(townHall, parramatta)).size)
    }

    @Test
    fun `a nearby row keeps the stop's own modes`() {
        val rows = nearestStopRows(townHallRider, listOf(townHall))

        assertEquals(listOf<TransportMode>(TransportMode.Train), rows.single().transportModeType)
    }

    @Test
    fun `a stop with an impossible position is skipped`() {
        val broken = nearby("1", "Broken", 123.0, 151.0)

        assertTrue(nearestStopRows(townHallRider, listOf(broken)).isEmpty())
    }

    // endregion

    // region curated

    @Test
    fun `a destination, a label or one leg reads the end list and a start reads the start list`() {
        val end = DEFAULT_SUGGESTED_STOPS.end.map { it.stopId }
        val start = DEFAULT_SUGGESTED_STOPS.start.map { it.stopId }

        assertEquals(end, curatedStopRows(SearchStopFieldType.TO, DEFAULT_SUGGESTED_STOPS).map { it.stopId })
        assertEquals(end, curatedStopRows(SearchStopFieldType.LABEL, DEFAULT_SUGGESTED_STOPS).map { it.stopId })
        assertEquals(start, curatedStopRows(SearchStopFieldType.FROM, DEFAULT_SUGGESTED_STOPS).map { it.stopId })
    }

    @Test
    fun `a curated row carries its own id and name so a tap works before the stops table loads`() {
        val first = curatedStopRows(SearchStopFieldType.TO, DEFAULT_SUGGESTED_STOPS).first()

        assertEquals(DEFAULT_SUGGESTED_STOPS.end.first().stopId, first.stopId)
        assertEquals(DEFAULT_SUGGESTED_STOPS.end.first().name, first.stopName)
        assertTrue(first.transportModeType.isEmpty())
    }

    // endregion

    // region the label row

    @Test
    fun `the label row leads the card whenever a label is set, with or without recents`() {
        assertTrue(shouldShowLabelRow(ListState.Recent, listOf(homeSet, workUnset)))
    }

    @Test
    fun `the label row is hidden while no label has a stop`() {
        assertFalse(shouldShowLabelRow(ListState.Recent, StopLabel.defaults))
        assertFalse(shouldShowLabelRow(ListState.Recent, emptyList()))
    }

    @Test
    fun `the label row follows results only once there is a result, and never on no match or error`() {
        val withResult = ListState.Results(
            results = persistentListOf(SearchStopState.SearchResult.Stop(stopName = "Central", stopId = "1")),
        )
        assertTrue(shouldShowLabelRow(withResult, listOf(homeSet)))
        assertFalse(shouldShowLabelRow(ListState.Results(isLoading = true), listOf(homeSet)))
        assertFalse(shouldShowLabelRow(ListState.NoMatch, listOf(homeSet)))
        assertFalse(shouldShowLabelRow(ListState.Error, listOf(homeSet)))
    }

    // endregion
}
