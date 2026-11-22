package xyz.ksharma.krail.trip.planner.ui.homemap

import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.core.connectivity.TransportState
import xyz.ksharma.krail.trip.planner.ui.searchstop.LocationPill
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.shouldAutoCenter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The rules for what floats over the home map: its notices, its place, its bottom and its camera. */
class HomeMapChromeRulesTest {

    // region notices

    @Test
    fun `no notice when location is on and the map is online`() {
        assertEquals(emptyList(), homeNoticesFor(LocationPill.Hidden, mapOffline = false))
    }

    @Test
    fun `the location pill shows for a rider who was not asked and for one who refused`() {
        listOf(LocationPill.Ask, LocationPill.OpenSettings).forEach { pill ->
            assertEquals(
                listOf(HomeNotice.LocationPermission),
                homeNoticesFor(pill, mapOffline = false),
                "The $pill pill is the one location pill.",
            )
        }
    }

    @Test
    fun `the Map offline chip shows alone when location is on`() {
        assertEquals(listOf(HomeNotice.MapOffline), homeNoticesFor(LocationPill.Hidden, mapOffline = true))
    }

    @Test
    fun `both notices are two whole items, the location pill first`() {
        assertEquals(
            listOf(HomeNotice.LocationPermission, HomeNotice.MapOffline),
            homeNoticesFor(LocationPill.OpenSettings, mapOffline = true),
        )
    }

    @Test
    fun `the map is offline only when the transport is reported down`() {
        assertTrue(mapOfflineFor(TransportState.Down))
        assertFalse(mapOfflineFor(TransportState.Up))
        assertFalse(mapOfflineFor(TransportState.Unknown), "Unknown is no grounds to claim offline.")
    }

    // endregion

    // region where the map goes

    @Test
    fun `a wide window keeps the one map pane whatever the flag says`() {
        assertEquals(HomeMapPlacement.SidePane, homeMapPlacementFor(mapsAvailable = true, dualPane = true))
        assertEquals(HomeMapPlacement.SidePane, homeMapPlacementFor(mapsAvailable = false, dualPane = true))
    }

    @Test
    fun `a single pane window gets the map behind the cards only when the flag is on`() {
        assertEquals(HomeMapPlacement.Behind, homeMapPlacementFor(mapsAvailable = true, dualPane = false))
        assertEquals(HomeMapPlacement.None, homeMapPlacementFor(mapsAvailable = false, dualPane = false))
    }

    // endregion

    // region the bottom

    private fun bottom(
        loading: Boolean = false,
        reordering: Boolean = false,
        cardOpen: Boolean = false,
    ) = homeBottomFor(loading = loading, reordering = reordering, cardOpen = cardOpen)

    @Test
    fun `the pill is the resting bottom and the only search bar`() {
        assertEquals(HomeBottom.Pill, bottom())
        assertEquals(listOf(HomeBottom.Pill, HomeBottom.Card, HomeBottom.Hidden), HomeBottom.entries)
    }

    @Test
    fun `an open card takes the place of the chips and the pill`() {
        assertEquals(HomeBottom.Card, bottom(cardOpen = true))
    }

    @Test
    fun `nothing shows at the bottom while loading`() {
        assertEquals(HomeBottom.Hidden, bottom(loading = true))
    }

    @Test
    fun `reorder mode takes the whole bottom away, even from an open card`() {
        assertEquals(HomeBottom.Hidden, bottom(reordering = true))
        assertEquals(HomeBottom.Hidden, bottom(reordering = true, cardOpen = true))
    }

    private fun reserve(bottom: HomeBottom) =
        stackBottomReserve(bottom, blockHeight = 120.dp, cardHeight = 300.dp, cardEdge = 16.dp)

    @Test
    fun `the stack stops short of what the bottom holds`() {
        assertEquals(120.dp, reserve(HomeBottom.Pill))
        assertEquals(316.dp, reserve(HomeBottom.Card))
        assertEquals(0.dp, reserve(HomeBottom.Hidden))
    }

    @Test
    fun `the stack takes what is left and never a negative height`() {
        assertEquals(480.dp, stackMaxHeight(available = 600.dp, reserve = 120.dp))
        assertEquals(0.dp, stackMaxHeight(available = 100.dp, reserve = 120.dp))
        assertEquals(0.dp, stackMaxHeight(available = 0.dp, reserve = 0.dp))
    }

    // endregion

    // region the camera

    @Test
    fun `the camera glides to the first fix and never again`() {
        assertFalse(shouldAutoCenter(hasUserLocation = false, alreadyCentered = false), "No fix, nothing to glide to.")
        assertTrue(shouldAutoCenter(hasUserLocation = true, alreadyCentered = false), "The first fix glides.")
        assertFalse(shouldAutoCenter(hasUserLocation = true, alreadyCentered = true), "Later fixes never glide.")
        assertFalse(shouldAutoCenter(hasUserLocation = false, alreadyCentered = true))
    }

    @Test
    fun `a stream of jittering fixes glides exactly once`() {
        // The effect is keyed on whether there is a fix, so it runs once for the false to true edge and
        // the stream of fixes after it changes nothing: this is the whole of the decision it makes.
        val fixes = List(50) { true }
        var centered = false
        var glides = 0
        (listOf(false) + fixes).forEach { hasFix ->
            if (shouldAutoCenter(hasFix, centered)) {
                centered = true
                glides++
            }
        }

        assertEquals(1, glides)
    }

    // endregion
}
