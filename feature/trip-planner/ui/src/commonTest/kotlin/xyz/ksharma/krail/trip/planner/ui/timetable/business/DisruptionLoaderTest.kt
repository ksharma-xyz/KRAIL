package xyz.ksharma.krail.trip.planner.ui.timetable.business

import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip.planner.ui.state.disruption.Disruption
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionBannerState
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionFeed
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionReason
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionRepository
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionSeverity
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState.JourneyCardInfo
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import xyz.ksharma.krail.trip.planner.ui.timetable.business.fixtures.ReplacementBusTripFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The loader decides which banner a results screen gets. Trips are real NSW replies from 2026-11-21,
 * the metro alert is the live "buses replace metro" alert for Mon 23 Nov 11:20pm.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DisruptionLoaderTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val dispatcher = StandardTestDispatcher()
    private val sandook = FakeSandook()
    private val trip = Trip(fromStopId = "A", fromStopName = "Bankstown", toStopId = "B", toStopName = "Sydenham")

    private val metroAlert = Disruption(
        id = "metro:night:1",
        feed = DisruptionFeed.METRO,
        severity = DisruptionSeverity.NOT_RUNNING,
        headline = "Buses replace metro services between Bankstown and Sydenham",
        startEpochSec = 1_795_436_400L,
        endEpochSec = 1_795_446_005L,
        moreInfoUrl = "https://transportnsw.info/alerts/details#/ems-8150",
        replacedByBuses = true,
    )

    private class Repo(var answer: Result<List<Disruption>>) : DisruptionRepository {
        var calls = 0
        override suspend fun load(feed: DisruptionFeed): Result<List<Disruption>> {
            calls++
            return answer
        }
    }

    private fun journeys(fixture: String): ImmutableList<JourneyCardInfo> =
        checkNotNull(json.decodeFromString<TripResponse>(fixture).buildJourneyList())

    private fun loader(scope: TestScope, repo: DisruptionRepository?, unavailable: MutableList<DisruptionFeed> = mutableListOf()) =
        DisruptionLoader(
            repository = repo,
            sandook = sandook,
            scope = scope,
            dispatcher = dispatcher,
            nowEpochSec = { 1_795_436_400L },
            onUnavailable = { unavailable += it },
        )

    private fun bothEndsHaveTrainAndMetro() {
        listOf("A", "B").forEach { id ->
            sandook.insertNswStop(stopId = id, stopName = id, stopLat = 0.0, stopLon = 0.0, isParent = null)
            sandook.insertNswStopProductClass(id, 1)
            sandook.insertNswStopProductClass(id, 2)
        }
    }

    @Test
    fun `Given a metro alert and replacement legs Then one replacement banner carries the alert`() = runTest(
        dispatcher,
    ) {
        bothEndsHaveTrainAndMetro()
        val loader = loader(this, Repo(Result.success(listOf(metroAlert))))
        loader.refresh(trip) {}
        advanceUntilIdle()

        // The results after the last metro: every one is a replacement bus, so the metro is missing from them.
        val allBuses = journeys(ReplacementBusTripFixtures.BANKSTOWN_TO_SYDENHAM_NIGHT)
            .filter { journey -> journey.legs.filterIsInstance<JourneyCardInfo.Leg.TransportLeg>().all { it.isReplacementBus } }
        assertTrue(allBuses.isNotEmpty())

        val banner = loader.bannerFor(allBuses, emptySet())

        assertEquals(metroAlert, assertIs<DisruptionBannerState.ReplacementBuses>(banner).disruption)
    }

    @Test
    fun `Given a metro alert and metro missing from the results Then the 1_30 metro banner stands`() = runTest(
        dispatcher,
    ) {
        bothEndsHaveTrainAndMetro()
        // Open ended and already begun, so it covers the Sunday morning results below.
        val openAlert = metroAlert.copy(id = "metro:open", startEpochSec = 0L, endEpochSec = null)
        val loader = loader(this, Repo(Result.success(listOf(openAlert))))
        loader.refresh(trip) {}
        advanceUntilIdle()

        // Parramatta to Central: trains only, so metro is missing and no leg is a replacement bus.
        val banner = loader.bannerFor(journeys(ReplacementBusTripFixtures.PARRAMATTA_TO_CENTRAL), emptySet())

        assertEquals(DisruptionReason.MODE_MISSING, assertIs<DisruptionBannerState.Shown>(banner).reason)
    }

    @Test
    fun `Given replacement legs and no alert loaded Then there is no banner`() = runTest(dispatcher) {
        val loader = loader(this, Repo(Result.success(emptyList())))
        loader.refresh(trip) {}
        advanceUntilIdle()

        val banner = loader.bannerFor(journeys(ReplacementBusTripFixtures.CRONULLA_TO_CENTRAL), emptySet())

        // Nothing to open, so nothing to show: the rows still say "Replacement bus".
        assertEquals(DisruptionBannerState.None, banner)
    }

    @Test
    fun `Given no replacement leg and no alert Then no banner`() = runTest(dispatcher) {
        val loader = loader(this, Repo(Result.success(emptyList())))

        assertEquals(
            DisruptionBannerState.None,
            loader.bannerFor(journeys(ReplacementBusTripFixtures.BONDI_JUNCTION_TO_BONDI_BEACH), emptySet()),
        )
    }

    @Test
    fun `Given the same alerts twice Then the change callback runs once`() = runTest(dispatcher) {
        bothEndsHaveTrainAndMetro()
        val repo = Repo(Result.success(listOf(metroAlert)))
        val loader = loader(this, repo)
        var changes = 0

        loader.refresh(trip) { changes++ }
        advanceUntilIdle()
        loader.refresh(trip) { changes++ }
        advanceUntilIdle()

        assertEquals(1, changes)
        assertEquals(2, repo.calls)
    }

    @Test
    fun `Given the feed fails Then it is reported once per refresh and no alert banner shows`() = runTest(dispatcher) {
        bothEndsHaveTrainAndMetro()
        val unavailable = mutableListOf<DisruptionFeed>()
        val loader = loader(this, Repo(Result.failure(IllegalStateException("down"))), unavailable)

        loader.refresh(trip) {}
        advanceUntilIdle()

        assertTrue(DisruptionFeed.METRO in unavailable)
        assertEquals(
            DisruptionBannerState.None,
            loader.bannerFor(journeys(ReplacementBusTripFixtures.BONDI_JUNCTION_TO_BONDI_BEACH), emptySet()),
        )
    }

    @Test
    fun `Given no repository Then refresh does nothing`() = runTest(dispatcher) {
        val loader = loader(this, repo = null)
        var changes = 0

        loader.refresh(trip) { changes++ }
        advanceUntilIdle()

        assertEquals(0, changes)
    }
}
