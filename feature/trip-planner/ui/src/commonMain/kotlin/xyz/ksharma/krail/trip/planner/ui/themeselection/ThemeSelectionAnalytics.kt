package xyz.ksharma.krail.trip.planner.ui.themeselection

import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent

internal fun Analytics.trackThemeSelectionEvent(themeId: Int) {
    track(AnalyticsEvent.ThemeSelectedEvent(themeId = themeIdParam(themeId)))
}

/**
 * The `themeId` param as analytics sends it for the theme [productClass]: its number as text. The one place that says
 * so, for `theme_selected` and for the Intro's `intro_lets_krail`, so the two cannot differ.
 */
internal fun themeIdParam(productClass: Int): String = productClass.toString()
