package xyz.ksharma.krail.core.analytics.event

import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.ThemeSelectedEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.ThemeSelectedEvent.Source
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `theme_selected` is sent from the Settings theme screen and from the Intro's finish press, and `source` is what
 * tells the two apart. Builds from before the param send none, which the analytics side reads as unknown.
 */
class ThemeSelectedEventTest {

    @Test
    fun `the event keeps its existing name`() {
        assertEquals("theme_selected", ThemeSelectedEvent(themeId = "4", source = Source.INTRO).name)
    }

    @Test
    fun `from the Intro it sends themeId and source intro, and nothing else`() {
        assertEquals(
            mapOf("themeId" to "4", "source" to "intro"),
            ThemeSelectedEvent(themeId = "4", source = Source.INTRO).properties,
        )
    }

    @Test
    fun `from Settings it sends themeId and source settings, and nothing else`() {
        assertEquals(
            mapOf("themeId" to "4", "source" to "settings"),
            ThemeSelectedEvent(themeId = "4", source = Source.SETTINGS).properties,
        )
    }

    @Test
    fun `source has exactly the two values the ledger names`() {
        assertEquals(listOf("intro", "settings"), Source.entries.map { it.value })
    }

    @Test
    fun `themeId is the same whichever screen sends it`() {
        Source.entries.forEach { source ->
            assertEquals("9", ThemeSelectedEvent(themeId = "9", source = source).properties?.get("themeId"))
        }
    }
}
