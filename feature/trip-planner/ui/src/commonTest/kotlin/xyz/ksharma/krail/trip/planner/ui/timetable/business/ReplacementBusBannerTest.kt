package xyz.ksharma.krail.trip.planner.ui.timetable.business

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.json.Json
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip.planner.ui.state.alerts.ServiceAlert
import xyz.ksharma.krail.trip.planner.ui.state.disruption.Disruption
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionBannerState
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionFeed
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionReason
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionSeverity
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState.JourneyCardInfo
import xyz.ksharma.krail.trip.planner.ui.timetable.business.fixtures.ReplacementBusTripFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A banner for replacement buses must come from NSW labelling a leg a temporary bus, never from a bus
 * leg alone and never from an alert attached to a leg. The trips are real replies from 2026-11-21.
 */
class ReplacementBusBannerTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val metroAlert = Disruption(
        id = "metro:test",
        feed = DisruptionFeed.METRO,
        severity = DisruptionSeverity.NOT_RUNNING,
        headline = "Buses replace metro services between Bankstown and Sydenham",
        startEpochSec = 1_795_436_400L,
        endEpochSec = 1_795_446_005L,
        moreInfoUrl = "https://transportnsw.info/alerts/details#/ems-8150",
        replacedByBuses = true,
    )

    private fun journeys(fixture: String): ImmutableList<JourneyCardInfo> =
        checkNotNull(json.decodeFromString<TripResponse>(fixture).buildJourneyList()) { "fixture did not map" }

    private fun JourneyCardInfo.transportLegs() = legs.filterIsInstance<JourneyCardInfo.Leg.TransportLeg>()

    @Test
    fun `Given Cronulla to Central Then every transport leg is a replacement bus`() {
        val legs = journeys(ReplacementBusTripFixtures.CRONULLA_TO_CENTRAL).flatMap { it.transportLegs() }

        assertTrue(legs.isNotEmpty())
        assertTrue(legs.all { it.isReplacementBus })
    }

    @Test
    fun `Given Cronulla to Central with replacement buses but no alert to open Then no banner`() {
        val cronulla = journeys(ReplacementBusTripFixtures.CRONULLA_TO_CENTRAL)

        // NSW attaches no alert to the replacement legs themselves, so a banner would have no detail and no link.
        assertTrue(cronulla.first().hasReplacementBusLeg())
        assertEquals(DisruptionBannerState.None, replacementBusBanner(cronulla, DisruptionBannerState.None))
    }

    @Test
    fun `Given the first result is a normal trip and a replacement bus comes later Then no banner`() {
        // The shape of a Seven Hills to Redfern search: tonight's trips are normal, tomorrow 6:29 am is replaced.
        // Built from real journeys: a normal bus trip first, then the replacement-bus journeys.
        val laterOnly = listOf(journeys(ReplacementBusTripFixtures.BONDI_JUNCTION_TO_BONDI_BEACH).first()) +
            journeys(ReplacementBusTripFixtures.CRONULLA_TO_CENTRAL)
        val withAlert = DisruptionBannerState.Shown(metroAlert, DisruptionReason.REPLACEMENT_BUS)

        assertFalse(laterOnly.first().hasReplacementBusLeg())
        assertTrue(laterOnly.any { it.hasReplacementBusLeg() })
        assertEquals(DisruptionBannerState.None, replacementBusBanner(laterOnly, DisruptionBannerState.None))
        // Even with a metro alert that says buses replace the line: the trip the rider takes next is unaffected.
        assertEquals(withAlert, replacementBusBanner(laterOnly, withAlert))
    }

    @Test
    fun `Given the first result is a replacement bus and carries the planner line alert Then the banner opens it`() {
        val tallawong = journeys(ReplacementBusTripFixtures.TALLAWONG_TO_BANKSTOWN_NIGHT)

        val banner = replacementBusBanner(tallawong, DisruptionBannerState.None)

        val shown = assertIs<DisruptionBannerState.ReplacementBuses>(banner)
        assertNull(shown.disruption)
        val lineAlert = checkNotNull(shown.lineAlert)
        assertTrue(lineAlert.isLineAlert)
        assertTrue(lineAlert.url?.startsWith("https://transportnsw.info/") == true)
    }

    @Test
    fun `Given Parramatta to Central with a line alert on the legs Then no banner`() {
        val parramatta = journeys(ReplacementBusTripFixtures.PARRAMATTA_TO_CENTRAL)

        // The Richmond to Blacktown closure rides these legs, but it is not this rider's stretch.
        assertTrue(
            parramatta.flatMap { it.transportLegs() }.any { leg -> leg.serviceAlertList.orEmpty().any { it.isLineAlert } },
        )
        assertEquals(DisruptionBannerState.None, replacementBusBanner(parramatta, DisruptionBannerState.None))
    }

    @Test
    fun `Given Central to Bondi Junction carrying the Cronulla closure Then no banner`() {
        val bondi = journeys(ReplacementBusTripFixtures.CENTRAL_TO_BONDI_JUNCTION)

        assertTrue(bondi.flatMap { it.transportLegs() }.none { it.isReplacementBus })
        assertEquals(DisruptionBannerState.None, replacementBusBanner(bondi, DisruptionBannerState.None))
    }

    @Test
    fun `Given Sydenham to Central moved to metro Then no banner`() {
        val metro = journeys(ReplacementBusTripFixtures.SYDENHAM_TO_CENTRAL)

        assertEquals(DisruptionBannerState.None, replacementBusBanner(metro, DisruptionBannerState.None))
    }

    @Test
    fun `Given an ordinary Sydney bus Then it is not a replacement bus`() {
        val buses = journeys(ReplacementBusTripFixtures.BONDI_JUNCTION_TO_BONDI_BEACH)

        assertTrue(buses.flatMap { it.transportLegs() }.isNotEmpty())
        assertFalse(buses.flatMap { it.transportLegs() }.any { it.isReplacementBus })
        assertEquals(DisruptionBannerState.None, replacementBusBanner(buses, DisruptionBannerState.None))
    }

    @Test
    fun `Given no replacement leg Then the metro banner stands`() {
        val metroBanner = DisruptionBannerState.Shown(metroAlert, DisruptionReason.MODE_MISSING)

        assertEquals(
            metroBanner,
            replacementBusBanner(journeys(ReplacementBusTripFixtures.PARRAMATTA_TO_CENTRAL), metroBanner),
        )
    }

    @Test
    fun `Given a metro alert that also says buses replace Then it supplies the detail`() {
        val banner = replacementBusBanner(
            journeys(ReplacementBusTripFixtures.CRONULLA_TO_CENTRAL),
            DisruptionBannerState.Shown(metroAlert, DisruptionReason.REPLACEMENT_BUS),
        )

        assertEquals(metroAlert, assertIs<DisruptionBannerState.ReplacementBuses>(banner).disruption)
    }

    @Test
    fun `Given a metro alert that only says the line is closed Then its own banner stands, not the replacement one`() {
        // "Metro services do not run between ..." never says buses replace it, so it explains nothing here.
        val closedOnly = metroAlert.copy(
            headline = "Metro services do not run between Tallawong and Bankstown",
            replacedByBuses = false,
        )

        val closedBanner = DisruptionBannerState.Shown(closedOnly, DisruptionReason.MODE_MISSING)

        val banner = replacementBusBanner(journeys(ReplacementBusTripFixtures.CRONULLA_TO_CENTRAL), closedBanner)

        assertEquals(closedBanner, banner)
    }

    @Test
    fun `Given every result is a replacement bus so the metro is missing Then the metro alert still supplies the detail`() {
        // Bankstown to Sydenham at 11:40 pm Monday: NSW returned only temporary buses, no metro train,
        // and attached no alert to them. The metro alert is the only text there is.
        val replacementOnly = journeys(ReplacementBusTripFixtures.BANKSTOWN_TO_SYDENHAM_NIGHT)
            .filter { journey -> journey.transportLegs().all { it.isReplacementBus } }
        assertTrue(replacementOnly.isNotEmpty())
        assertTrue(
            replacementOnly.flatMap { it.transportLegs() }.all { it.serviceAlertList.orEmpty().none { a -> a.isLineAlert } },
        )
        val metroBanner = disruptionBanner(
            disruptions = listOf(metroAlert),
            endpointProductClasses = setOf(1, 2),
            resultProductClasses = replacementOnly.flatMap { it.transportModeLines }
                .map { it.transportMode.productClass }
                .toSet(),
            hasReplacementBusLeg = true,
            spanStartEpochSec = metroAlert.startEpochSec + 600,
            spanEndEpochSec = metroAlert.startEpochSec + 4_000,
        )

        val banner = replacementBusBanner(replacementOnly, metroBanner)

        assertEquals(DisruptionReason.MODE_MISSING, assertIs<DisruptionBannerState.Shown>(metroBanner).reason)
        val shown = assertIs<DisruptionBannerState.ReplacementBuses>(banner)
        assertEquals(metroAlert, shown.disruption)
        assertNull(shown.lineAlert)
    }

    @Test
    fun `Given a line alert on a replacement leg Then it supplies the detail and a station alert does not`() {
        val lineAlert = ServiceAlert(
            heading = "Buses replace trains",
            message = "<div>Details</div>",
            isLineAlert = true,
        )
        val stationAlert = ServiceAlert(heading = "Station Update - Central", message = "Lift", isLineAlert = false)
        val withAlerts = journeys(ReplacementBusTripFixtures.CRONULLA_TO_CENTRAL).map { journey ->
            journey.copy(
                legs = journey.legs.map { leg ->
                    if (leg is JourneyCardInfo.Leg.TransportLeg) {
                        leg.copy(serviceAlertList = persistentListOf(stationAlert, lineAlert))
                    } else {
                        leg
                    }
                }.toImmutableList(),
            )
        }

        val banner = replacementBusBanner(withAlerts, DisruptionBannerState.None)

        assertEquals(lineAlert, assertIs<DisruptionBannerState.ReplacementBuses>(banner).lineAlert)
    }

    @Test
    fun `Given the planner marks an alert lineInfo Then it is a line alert and a stop alert is not`() {
        val alerts = journeys(ReplacementBusTripFixtures.CENTRAL_TO_BONDI_JUNCTION)
            .flatMap { it.transportLegs() }
            .flatMap { it.serviceAlertList.orEmpty() }

        assertTrue(alerts.any { it.isLineAlert && it.url != null })
        assertTrue(alerts.any { !it.isLineAlert })
    }

    @Test
    fun `Given the first result is the last metro and buses follow Then the metro alert banner stands`() {
        val night = journeys(ReplacementBusTripFixtures.BANKSTOWN_TO_SYDENHAM_NIGHT)
        val metroClass = 2
        val metroBanner = disruptionBanner(
            disruptions = listOf(metroAlert),
            endpointProductClasses = setOf(1, metroClass),
            resultProductClasses = night.flatMap { it.transportModeLines }.map { it.transportMode.productClass }.toSet(),
            hasReplacementBusLeg = night.any { it.hasReplacementBusLeg() },
            spanStartEpochSec = metroAlert.startEpochSec + 600,
            spanEndEpochSec = metroAlert.startEpochSec + 4_000,
        )

        val banner = replacementBusBanner(night, metroBanner)

        // The 11:30 pm search lists the last metro first. That trip is not replaced, so the replacement banner
        // does not show; the metro alert banner, which names the line, does.
        assertFalse(night.first().hasReplacementBusLeg())
        assertEquals(DisruptionReason.REPLACEMENT_BUS, assertIs<DisruptionBannerState.Shown>(metroBanner).reason)
        assertEquals(metroBanner, banner)
    }

    @Test
    fun `Given replacement legs with no alert list at all Then there is no banner`() {
        val noAlerts = journeys(ReplacementBusTripFixtures.CRONULLA_TO_CENTRAL).map { journey ->
            journey.copy(
                legs = journey.legs.map { leg ->
                    if (leg is JourneyCardInfo.Leg.TransportLeg) leg.copy(serviceAlertList = null) else leg
                }.toImmutableList(),
            )
        }

        val banner = replacementBusBanner(noAlerts, DisruptionBannerState.None)

        assertEquals(DisruptionBannerState.None, banner)
    }
}
