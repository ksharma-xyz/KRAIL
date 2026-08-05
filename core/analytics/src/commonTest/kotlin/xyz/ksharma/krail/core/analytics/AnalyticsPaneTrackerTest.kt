package xyz.ksharma.krail.core.analytics

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AnalyticsPaneTrackerTest {

    @AfterTest
    fun reset() {
        AnalyticsPaneTracker.set(AnalyticsPane.SINGLE)
    }

    @Test
    fun `a single pane layout reports SINGLE`() {
        AnalyticsPaneTracker.set(AnalyticsPane.SINGLE)

        assertEquals("SINGLE", AnalyticsPaneTracker.decorate(emptyMap())[AnalyticsPaneTracker.PROP_PANE])
    }

    @Test
    fun `a touch in the detail pane attributes following events to it`() {
        AnalyticsPaneTracker.set(AnalyticsPane.DETAIL)

        assertEquals("DETAIL", AnalyticsPaneTracker.decorate(emptyMap())[AnalyticsPaneTracker.PROP_PANE])
    }

    @Test
    fun `the last touched pane wins`() {
        AnalyticsPaneTracker.set(AnalyticsPane.DETAIL)
        AnalyticsPaneTracker.set(AnalyticsPane.LIST)

        assertEquals(AnalyticsPane.LIST, AnalyticsPaneTracker.currentPane())
    }

    @Test
    fun `existing event params are kept`() {
        AnalyticsPaneTracker.set(AnalyticsPane.LIST)

        val decorated = AnalyticsPaneTracker.decorate(mapOf("stopId" to "200060", "expand" to true))

        assertEquals("200060", decorated["stopId"])
        assertEquals(true, decorated["expand"])
        assertEquals("LIST", decorated[AnalyticsPaneTracker.PROP_PANE])
    }

    @Test
    fun `an event with no params still carries the pane`() {
        AnalyticsPaneTracker.set(AnalyticsPane.DETAIL)

        val decorated = AnalyticsPaneTracker.decorate(null)

        assertEquals(1, decorated.size)
        assertEquals("DETAIL", decorated[AnalyticsPaneTracker.PROP_PANE])
    }
}
