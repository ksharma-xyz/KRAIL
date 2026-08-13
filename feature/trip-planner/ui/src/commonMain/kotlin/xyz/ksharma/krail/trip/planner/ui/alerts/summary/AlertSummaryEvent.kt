package xyz.ksharma.krail.trip.planner.ui.alerts.summary

import kotlinx.collections.immutable.ImmutableSet
import xyz.ksharma.krail.taj.components.AlertFeedbackVoteChoice
import xyz.ksharma.krail.trip.planner.ui.state.alerts.ServiceAlert

/** UI-originated intents for [AlertSummaryViewModel]. Composables never call the service. */
sealed interface AlertSummaryEvent {

    /** `ServiceAlertScreen` entered composition with [alerts]. Safe to send repeatedly. */
    data class SummaryRequested(val alerts: ImmutableSet<ServiceAlert>) : AlertSummaryEvent

    /** Rider tapped a vote choice under the summary. */
    data class VoteClicked(val vote: AlertFeedbackVoteChoice) : AlertSummaryEvent
}
