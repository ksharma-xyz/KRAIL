package xyz.ksharma.krail.trip.planner.ui.themeselection

import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The one place the Settings theme screen and the Intro both write and report a theme through. The Settings
 * screen's own behaviour stays held by `ThemeSelectionViewModelTest`.
 */
class ThemeChoiceTest {

    private val sandook = FakeSandook()
    private val analytics = FakeAnalytics()
    private val choice = ThemeChoice(sandook, analytics)

    @Test
    fun `saving stores the theme as the only one, replacing the one before`() {
        sandook.insertOrReplaceTheme(KrailThemeStyle.Bus.id.toLong())

        choice.save(KrailThemeStyle.Ferry.id)

        assertEquals(KrailThemeStyle.Ferry.id.toLong(), sandook.getProductClass())
    }

    @Test
    fun `saving writes nothing to analytics, and reporting writes nothing to the store`() {
        choice.save(KrailThemeStyle.Metro.id)
        assertEquals(0, analytics.getTrackedEvents("theme_selected").size)

        sandook.clearTheme()
        choice.track(KrailThemeStyle.Metro.id)
        assertNull(sandook.getProductClass())
    }

    @Test
    fun `reporting sends the one theme selected event with the id and nothing else`() {
        choice.track(KrailThemeStyle.Metro.id)

        val event = analytics.getTrackedEvents("theme_selected").single()
        assertEquals(mapOf<String, Any>("themeId" to KrailThemeStyle.Metro.id.toString()), event.properties)
    }
}
