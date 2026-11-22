package xyz.ksharma.krail.trip.planner.ui.savedtrips

import xyz.ksharma.krail.core.maps.data.model.NearbyStop
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.sandook.utils.GeoUtils
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.LabelChipTap
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.LabelTapOutcome
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The rules behind the Home and Work chips (`docs/MAP_HOME_UX.md` rule 41), each driven at its
 * boundary with real Sydney stops and no layout, location fix or database.
 */
class HomeLabelChipsTest {

    // region the scroll boundary

    @Test
    fun `the chip row wraps up to and including 130 percent text`() {
        listOf(0.85f, 1f, 1.15f, 1.3f).forEach { scale ->
            assertEquals(ChipRowMode.Wrap, chipRowModeFor(scale), "At $scale the chips wrap whole.")
        }
    }

    @Test
    fun `the chip row scrolls sideways over 130 percent text`() {
        listOf(1.31f, 1.5f, 2f, 3f).forEach { scale ->
            assertEquals(ChipRowMode.Scroll, chipRowModeFor(scale), "At $scale the chips scroll.")
        }
    }

    // endregion

    // region the chips

    @Test
    fun `unset labels read Set Home and Set Work, then Park & Ride`() {
        val chips = homeChipsFor(StopLabel.defaults)

        assertEquals(listOf("Set Home", "Set Work"), chips.filterIsInstance<HomeChip.Label>().map { it.text })
        assertEquals(HomeChip.ParkRide, chips.last())
        assertEquals(3, chips.size)
    }

    @Test
    fun `set labels read Home and Work and the chips stay`() {
        val chips = homeChipsFor(listOf(HOME.at(CENTRAL), WORK.at(TOWN_HALL)))

        assertEquals(listOf("Home", "Work"), chips.filterIsInstance<HomeChip.Label>().map { it.text })
        assertEquals(3, chips.size, "A set label keeps its chip.")
    }

    @Test
    fun `one set and one unset reads Home and Set Work`() {
        val chips = homeChipsFor(listOf(HOME.at(CENTRAL), WORK))

        assertEquals(listOf("Home", "Set Work"), chips.filterIsInstance<HomeChip.Label>().map { it.text })
    }

    @Test
    fun `the order is Home, Work, then Park & Ride whatever order the rows come in`() {
        val gym = StopLabel(emoji = "x", label = "Gym", stopId = "1", stopName = "Gym stop")
        val chips = homeChipsFor(listOf(gym, WORK.at(TOWN_HALL), HOME.at(CENTRAL)))

        assertEquals(
            listOf("Home", "Work"),
            chips.filterIsInstance<HomeChip.Label>().map { it.label.label },
        )
        assertIs<HomeChip.ParkRide>(chips.last())
    }

    @Test
    fun `labels beyond Home and Work get no chip here`() {
        val gym = StopLabel(emoji = "x", label = "Gym", stopId = "1", stopName = "Gym stop")

        assertEquals(3, homeChipsFor(listOf(HOME, WORK, gym)).size)
    }

    @Test
    fun `a Work label deleted in Manage still has its Set Work chip`() {
        val chips = homeChipsFor(listOf(HOME.at(CENTRAL)))

        assertEquals(listOf("Home", "Set Work"), chips.filterIsInstance<HomeChip.Label>().map { it.text })
    }

    @Test
    fun `there are no chips until the labels have loaded`() {
        assertEquals(emptyList(), homeChipsFor(emptyList()))
    }

    @Test
    fun `a label row is found whatever its case`() {
        val row = StopLabel(emoji = "x", label = "home", stopId = "1", stopName = "Somewhere")

        assertEquals(row, homeLabelFor("Home", listOf(row)))
        assertNull(homeLabelFor("Gym", listOf(row)), "Only Home and Work have a built in label.")
    }

    // endregion

    // region what a tap starts

    @Test
    fun `a tap on a set chip waits for one location fix`() {
        assertEquals(LabelChipTap.Locating("Home"), startLabelChipTap(HOME.at(CENTRAL)))
    }

    @Test
    fun `a tap on an unset chip opens the stop search to set it`() {
        assertEquals(
            LabelChipTap.Decided(LabelTapOutcome.Assign("Work")),
            startLabelChipTap(WORK),
        )
    }

    // endregion

    // region the nearest stop

    @Test
    fun `the nearest stop wins whatever order the candidates come in`() {
        val candidates = listOf(REDFERN, TOWN_HALL_NEARBY, CENTRAL_NEARBY)

        listOf(candidates, candidates.reversed()).forEach { ordered ->
            assertEquals(
                "200060",
                nearestStopTo(RIDER.latitude, RIDER.longitude, ordered, radiusKm = 5.0)?.stopId,
            )
        }
    }

    @Test
    fun `equal distances go to the smaller stop id`() {
        val twin = CENTRAL_NEARBY.copy(stopId = "200061", stopName = "Central Station twin")
        val candidates = listOf(twin, CENTRAL_NEARBY)

        listOf(candidates, candidates.reversed()).forEach { ordered ->
            assertEquals(
                "200060",
                nearestStopTo(RIDER.latitude, RIDER.longitude, ordered, radiusKm = 1.0)?.stopId,
                "The same position must always give the same stop.",
            )
        }
    }

    @Test
    fun `no stop within the radius gives none`() {
        assertNull(nearestStopTo(RIDER.latitude, RIDER.longitude, listOf(REDFERN, TOWN_HALL_NEARBY), radiusKm = 1.0))
    }

    @Test
    fun `a stop exactly on the radius counts and one just inside it counts`() {
        val edge = GeoUtils.haversineDistance(RIDER.latitude, RIDER.longitude, REDFERN.latitude, REDFERN.longitude)!!

        assertNotNull(nearestStopTo(RIDER.latitude, RIDER.longitude, listOf(REDFERN), radiusKm = edge))
        assertNull(nearestStopTo(RIDER.latitude, RIDER.longitude, listOf(REDFERN), radiusKm = edge - 0.001))
    }

    @Test
    fun `no candidates gives none`() {
        assertNull(nearestStopTo(RIDER.latitude, RIDER.longitude, emptyList(), radiusKm = 1.0))
    }

    @Test
    fun `a stop whose position is not a position is skipped, not chosen`() {
        val broken = CENTRAL_NEARBY.copy(stopId = "1", latitude = 200.0)

        assertEquals(
            "200060",
            nearestStopTo(RIDER.latitude, RIDER.longitude, listOf(broken, CENTRAL_NEARBY), radiusKm = 1.0)?.stopId,
        )
        assertNull(nearestStopTo(RIDER.latitude, RIDER.longitude, listOf(broken), radiusKm = 1.0))
    }

    // endregion

    // region the tap decision

    @Test
    fun `a fix and a stop near plans from that stop to the labelled stop`() {
        val outcome = decideLabelTap(TOWN_HALL, RIDER, listOf(REDFERN, CENTRAL_NEARBY))

        assertEquals(
            LabelTapOutcome.Plan(
                from = StopItem(stopId = "200060", stopName = "Central Station"),
                to = TOWN_HALL,
            ),
            outcome,
        )
    }

    @Test
    fun `no fix falls back to the search with the labelled stop and no start`() {
        assertEquals(
            LabelTapOutcome.Search(to = TOWN_HALL),
            decideLabelTap(TOWN_HALL, rider = null, candidates = listOf(CENTRAL_NEARBY)),
        )
    }

    @Test
    fun `a fix with no stop within the radius falls back to the search`() {
        assertEquals(
            LabelTapOutcome.Search(to = TOWN_HALL),
            decideLabelTap(TOWN_HALL, RIDER, listOf(REDFERN, TOWN_HALL_NEARBY)),
        )
    }

    @Test
    fun `a fix with no candidates falls back to the search`() {
        assertEquals(LabelTapOutcome.Search(to = TOWN_HALL), decideLabelTap(TOWN_HALL, RIDER, emptyList()))
    }

    @Test
    fun `the nearest stop being the labelled stop itself falls back, it is not a trip`() {
        val central = StopItem(stopId = "200060", stopName = "Central Station")

        assertEquals(
            LabelTapOutcome.Search(to = central),
            decideLabelTap(central, RIDER, listOf(CENTRAL_NEARBY, REDFERN)),
        )
    }

    // endregion

    private fun StopLabel.at(stop: StopItem) = copy(stopId = stop.stopId, stopName = stop.stopName)

    private companion object {
        val HOME = StopLabel(emoji = "H", label = "Home")
        val WORK = StopLabel(emoji = "W", label = "Work")
        val CENTRAL = StopItem(stopId = "200060", stopName = "Central Station")
        val TOWN_HALL = StopItem(stopId = "200070", stopName = "Town Hall Station")

        /** A rider about 70 m from Central, 1.06 km from Redfern and 1.3 km from Town Hall. */
        val RIDER = LatLng(latitude = -33.8845, longitude = 151.2065)

        val CENTRAL_NEARBY = NearbyStop("200060", "Central Station", -33.8840, 151.2060, emptyList())
        val REDFERN = NearbyStop("201510", "Redfern Station", -33.8914, 151.1985, emptyList())
        val TOWN_HALL_NEARBY = NearbyStop("200070", "Town Hall Station", -33.8731, 151.2065, emptyList())
    }
}
