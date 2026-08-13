package xyz.ksharma.krail.trip.planner.ui.alerts.summary

import xyz.ksharma.krail.taj.components.AlertFeedbackVoteChoice

/**
 * On-screen state for the trip's single aggregate AI summary card (one card for the whole
 * alert set shown in `ServiceAlertScreen`, not one per alert). `null` — rather than a
 * `NotShown`/`Idle` case — means render nothing at all: the flag being off, the device
 * failing the availability check, and "not requested yet" are all indistinguishable to the
 * UI, which is the point.
 */
sealed interface AlertSummaryUiState {

    /** Model is running. Card shows the spinning [AiWheelMark][xyz.ksharma.krail.taj.components.AiWheelMark] + skeleton lines. */
    data object Generating : AlertSummaryUiState

    /**
     * A summary arrived. Card shows the settled wheel, the summary text, and the vote row.
     *
     * @param text The generated summary. Never blank — [AlertSummaryViewModel] only
     * transitions here once a non-null, non-blank result comes back.
     * @param vote The rider's vote, or `null` before they vote. At most one vote is recorded.
     */
    data class Resolved(
        val text: String,
        val vote: AlertFeedbackVoteChoice? = null,
    ) : AlertSummaryUiState
}
