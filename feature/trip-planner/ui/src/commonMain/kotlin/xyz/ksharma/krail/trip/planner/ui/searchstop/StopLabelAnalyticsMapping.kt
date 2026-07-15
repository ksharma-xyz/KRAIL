package xyz.ksharma.krail.trip.planner.ui.searchstop

import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.LabelAssignSurface

/**
 * UI-boundary enum to registered analytics value. Kept as an exhaustive `when` so a
 * new surface fails compilation here instead of silently sending nothing.
 */
internal fun LabelAssignSurface.toAnalyticsSurface(): AnalyticsEvent.StopLabelSurface =
    when (this) {
        LabelAssignSurface.SEARCH_RESULT -> AnalyticsEvent.StopLabelSurface.SEARCH_RESULT
        LabelAssignSurface.RECENT -> AnalyticsEvent.StopLabelSurface.RECENT
        LabelAssignSurface.EMPTY_STATE -> AnalyticsEvent.StopLabelSurface.EMPTY_STATE
        LabelAssignSurface.ADDRESS_RESULT -> AnalyticsEvent.StopLabelSurface.ADDRESS_RESULT
    }

/** Bounded label classification - raw label names never reach analytics. */
internal fun labelKindOf(labelKey: String): AnalyticsEvent.StopLabelKind =
    if (labelKey.equals(StopLabel.PROTECTED_LABEL, ignoreCase = true)) {
        AnalyticsEvent.StopLabelKind.PROTECTED_DEFAULT
    } else {
        AnalyticsEvent.StopLabelKind.CUSTOM
    }
