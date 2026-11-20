package xyz.ksharma.krail.trip.planner.network.disruption

import com.google.transit.realtime.Alert
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionFeed
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionSeverity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AlertClassifierTest {

    private fun classify(vararg alerts: Alert) = classifyAlerts(DisruptionFeed.METRO, alerts.toList())

    @Test
    fun `Given buses replace metro Then it is a not running disruption that flags the replacement`() {
        val result = classify(AlertFixtures.busesReplaceMetro).single()

        assertEquals(DisruptionSeverity.NOT_RUNNING, result.severity)
        assertTrue(result.replacedByBuses)
        assertEquals("https://transportnsw.info/alerts/details#/ems-8150", result.moreInfoUrl)
        assertEquals(1_795_436_400L, result.startEpochSec)
        assertEquals(1_795_446_005L, result.endEpochSec)
    }

    @Test
    fun `Given metro services do not run Then it is not running but not a bus replacement`() {
        val result = classify(AlertFixtures.metroDoNotRun).single()

        assertEquals(DisruptionSeverity.NOT_RUNNING, result.severity)
        assertEquals(false, result.replacedByBuses)
    }

    @Test
    fun `Given a station facility notice with no effect Then it is dropped`() {
        assertTrue(classify(AlertFixtures.stationUpdate).isEmpty())
    }

    @Test
    fun `Given the live metro feed mix Then only the closures survive`() {
        val result = classify(
            AlertFixtures.busesReplaceMetro,
            AlertFixtures.stationUpdate,
            AlertFixtures.metroDoNotRun,
            AlertFixtures.stationUpdate,
        )

        assertEquals(2, result.size)
    }

    @Test
    fun `Given a lift outage Then it is dropped even with a service effect`() {
        val lift = AlertFixtures.alert(header = "Lift 2 at Central is not available")

        assertTrue(classify(lift).isEmpty())
    }

    @Test
    fun `Given a closure worded with no effect Then the wording still counts`() {
        val faultWithoutEffect = AlertFixtures.alert(
            header = "Metro services suspended due to a signal fault",
            effect = Alert.Effect.UNKNOWN_EFFECT,
        )

        assertEquals(DisruptionSeverity.NOT_RUNNING, classify(faultWithoutEffect).single().severity)
    }

    @Test
    fun `Given significant delays Then it is delays and changes`() {
        val delays = AlertFixtures.alert(
            header = "Metro services are running late",
            effect = Alert.Effect.SIGNIFICANT_DELAYS,
        )

        assertEquals(DisruptionSeverity.DELAYS_AND_CHANGES, classify(delays).single().severity)
    }

    @Test
    fun `Given an alert with no active period Then it is dropped`() {
        assertTrue(classify(AlertFixtures.alert(header = "Buses replace metro services", start = null)).isEmpty())
    }

    @Test
    fun `Given an alert with no end Then it is kept as open ended`() {
        val openEnded = AlertFixtures.alert(header = "Metro services do not run", end = null)

        assertNull(classify(openEnded).single().endEpochSec)
    }

    @Test
    fun `Given an alert with an empty headline Then it is dropped`() {
        assertTrue(classify(AlertFixtures.alert(header = "  ")).isEmpty())
    }
}
