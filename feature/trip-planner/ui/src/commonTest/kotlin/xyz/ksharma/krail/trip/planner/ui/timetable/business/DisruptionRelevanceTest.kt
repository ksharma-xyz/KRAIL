package xyz.ksharma.krail.trip.planner.ui.timetable.business

import xyz.ksharma.krail.trip.planner.ui.state.disruption.Disruption
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionBannerState
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionFeed
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionSeverity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DisruptionRelevanceTest {

    private val metro = 2
    private val train = 1
    private val bus = 5

    // Mon 23 Nov 2026, 11:20pm to Tue 2:00am AEDT, the live "buses replace metro" window.
    private val windowStart = 1_795_436_400L
    private val windowEnd = 1_795_446_005L

    private fun disruption(
        severity: DisruptionSeverity = DisruptionSeverity.NOT_RUNNING,
        replacedByBuses: Boolean = true,
        start: Long = windowStart,
        end: Long? = windowEnd,
    ) = Disruption(
        id = "metro:test:$start",
        feed = DisruptionFeed.METRO,
        severity = severity,
        headline = "Buses replace metro services between Bankstown and Sydenham",
        detail = "",
        startEpochSec = start,
        endEpochSec = end,
        moreInfoUrl = null,
        replacedByBuses = replacedByBuses,
    )

    private fun banner(
        disruptions: List<Disruption> = listOf(disruption()),
        endpoints: Set<Int> = setOf(metro, train),
        results: Set<Int> = setOf(bus, train),
        spanStart: Long = windowStart + 600,
        spanEnd: Long = windowStart + 4_000,
    ) = disruptionBanner(disruptions, endpoints, results, spanStart, spanEnd)

    @Test
    fun `Given metro missing from the results Then the banner shows`() {
        assertIs<DisruptionBannerState.Shown>(banner())
    }

    @Test
    fun `Given buses replace metro and the results carry a bus leg beside metro Then the banner shows`() {
        assertIs<DisruptionBannerState.Shown>(banner(results = setOf(bus, metro)))
    }

    @Test
    fun `Given metro running normally with no bus leg Then no banner`() {
        assertEquals(DisruptionBannerState.None, banner(results = setOf(metro)))
    }

    @Test
    fun `Given only one end is served by metro Then no banner`() {
        // Central to Parramatta: Central has a metro, Parramatta does not, so endpoints has no metro.
        assertEquals(DisruptionBannerState.None, banner(endpoints = setOf(train)))
    }

    @Test
    fun `Given the rider filtered metro out Then no banner`() {
        assertEquals(DisruptionBannerState.None, banner(endpoints = emptySet()))
    }

    @Test
    fun `Given the alert window ended before the results Then no banner`() {
        assertEquals(
            DisruptionBannerState.None,
            banner(spanStart = windowEnd + 60, spanEnd = windowEnd + 4_000),
        )
    }

    @Test
    fun `Given the alert window starts after the results Then no banner`() {
        assertEquals(
            DisruptionBannerState.None,
            banner(spanStart = windowStart - 4_000, spanEnd = windowStart - 60),
        )
    }

    @Test
    fun `Given an open ended alert that has begun Then the banner shows`() {
        assertIs<DisruptionBannerState.Shown>(banner(disruptions = listOf(disruption(end = null))))
    }

    @Test
    fun `Given a delays only alert Then no banner yet`() {
        assertEquals(
            DisruptionBannerState.None,
            banner(disruptions = listOf(disruption(severity = DisruptionSeverity.DELAYS_AND_CHANGES))),
        )
    }

    @Test
    fun `Given two closures Then the earliest is shown`() {
        val later = disruption(start = windowStart + 1_000)
        val earlier = disruption(start = windowStart)

        val shown = assertIs<DisruptionBannerState.Shown>(banner(disruptions = listOf(later, earlier)))

        assertEquals(earlier, shown.disruption)
    }

    @Test
    fun `Given a closure that does not mention buses and metro still in the results Then no banner`() {
        assertEquals(
            DisruptionBannerState.None,
            banner(disruptions = listOf(disruption(replacedByBuses = false)), results = setOf(metro, bus)),
        )
    }
}
