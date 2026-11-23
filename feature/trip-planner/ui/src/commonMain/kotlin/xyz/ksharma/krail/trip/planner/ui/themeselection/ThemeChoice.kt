package xyz.ksharma.krail.trip.planner.ui.themeselection

import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.sandook.Sandook

/**
 * What it means to choose a theme, in one place for the two screens that offer the choice: Settings and
 * the first screen of the Intro. Writing the choice and reporting it are one thing here and not two copies,
 * so the two screens can never store or report a theme differently.
 *
 * Both calls are blocking, so the caller chooses the dispatcher. The app switches colour on its own, from the
 * navigator, and reads this stored value the next time it starts.
 */
internal class ThemeChoice(
    private val sandook: Sandook,
    private val analytics: Analytics,
) {
    /** Stores [productClass] as the rider's theme. Only one entry should exist at a time, so the old goes first. */
    fun save(productClass: Int) {
        sandook.clearTheme()
        sandook.insertOrReplaceTheme(productClass.toLong())
    }

    /**
     * Reports that the rider chose the theme [productClass], through the one `theme_selected` event. [source] is
     * the screen the choice was made on, so the data can tell a first run colour from a rider changing theme.
     */
    fun track(productClass: Int, source: AnalyticsEvent.ThemeSelectedEvent.Source) {
        analytics.trackThemeSelectionEvent(themeId = productClass, source = source)
    }
}
