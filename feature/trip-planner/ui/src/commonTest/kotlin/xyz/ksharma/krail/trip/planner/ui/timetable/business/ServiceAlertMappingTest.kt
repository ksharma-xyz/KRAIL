package xyz.ksharma.krail.trip.planner.ui.timetable.business

import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServiceAlertMappingTest {

    private fun info(
        type: String? = "lineInfo",
        url: String? = "https://transportnsw.info/alerts/details#/ems-81168",
        subtitle: String? = "Buses replace trains between Cronulla, Waterfall and Central",
        content: String? = "<div>Details</div>",
        priority: String? = "normal",
    ) = TripResponse.Info(subtitle = subtitle, content = content, priority = priority, type = type, url = url)

    @Test
    fun `Given a lineInfo alert Then it is a line alert carrying its link`() {
        val alert = info().toAlert()

        assertTrue(checkNotNull(alert).isLineAlert)
        assertEquals("https://transportnsw.info/alerts/details#/ems-81168", alert.url)
    }

    @Test
    fun `Given a stopInfo alert Then it is not a line alert`() {
        assertFalse(checkNotNull(info(type = "stopInfo").toAlert()).isLineAlert)
    }

    @Test
    fun `Given an alert with no type Then it is not a line alert`() {
        assertFalse(checkNotNull(info(type = null).toAlert()).isLineAlert)
    }

    @Test
    fun `Given a blank or missing link Then the alert has no url`() {
        assertNull(checkNotNull(info(url = "  ").toAlert()).url)
        assertNull(checkNotNull(info(url = null).toAlert()).url)
    }

    @Test
    fun `Given an alert missing its heading, content or priority Then it is dropped`() {
        assertNull(info(subtitle = null).toAlert())
        assertNull(info(content = null).toAlert())
        assertNull(info(priority = "unknown").toAlert())
    }
}
