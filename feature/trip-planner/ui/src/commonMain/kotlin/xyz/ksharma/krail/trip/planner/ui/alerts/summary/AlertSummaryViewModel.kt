package xyz.ksharma.krail.trip.planner.ui.alerts.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.aitext.AiAvailability
import xyz.ksharma.krail.core.aitext.AiTextService
import xyz.ksharma.krail.core.aitext.AiUnavailableReasons
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.taj.components.AlertFeedbackVoteChoice
import xyz.ksharma.krail.trip.planner.ui.state.alerts.ServiceAlert

private const val GENERATING_MIN_DURATION_MS = 1000L

// "Not ready yet", not "never available", so a dedupe key set for one of these must not
// permanently block a later retry of the same alert set.
//
// These were the two raw ML Kit spellings, which meant iOS never matched: Foundation Models
// reports its own words for a model that is still downloading, and a rider on an iPhone had
// every retry treated as a permanent failure. Both platforms now report the shared
// AiUnavailableReasons vocabulary, which is the whole reason that object exists.
private val RETRIABLE_REASONS = setOf(AiUnavailableReasons.MODEL_DOWNLOADING)

/**
 * Owns the trip's single aggregate AI summary card: request lifecycle and the vote feedback
 * loop for the whole active alert set, shown atop `ServiceAlertScreen`. Scoped to the alerts
 * sheet only — `TimeTableViewModel` is not touched.
 *
 * Every path collapses to "no entry in [uiState]": the flag being off, the device failing
 * [AiTextService.checkAvailability], and a mid-call failure are all indistinguishable to the
 * UI, which renders nothing in every case beyond the momentary [AlertSummaryUiState.Generating]
 * once a request is actually underway.
 */
class AlertSummaryViewModel(
    private val aiTextService: AiTextService,
    // Lambda, not a captured Boolean: re-read on every call so a debug-settings toggle
    // flipped while this sheet is open (Debug Settings -> back, same nav entry) takes
    // effect immediately rather than only on next ViewModel creation.
    private val isAlertSummaryEnabled: () -> Boolean = { false },
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlertSummaryUiState?>(null)
    val uiState: StateFlow<AlertSummaryUiState?> = _uiState.asStateFlow()

    // The alert-set hash already requested or in flight this session, so a recomposition
    // (rotation, sheet reopened with the same alerts) never re-fires the model. Reset back
    // to null on a *retriable* unavailable reason (model still downloading) so a later
    // recomposition of the exact same set gets a real retry instead of being deduped away
    // forever.
    private var requestedSetKey: String? = null

    // Only one summary request is ever "current" - cancelling the previous job before
    // starting a new one means an old, still-running request for a superseded alert set
    // can never win a race against a newer one and overwrite its result.
    private var activeJob: Job? = null

    fun onEvent(event: AlertSummaryEvent) {
        when (event) {
            is AlertSummaryEvent.SummaryRequested -> requestSummary(event.alerts)
            is AlertSummaryEvent.VoteClicked -> onVoteClicked(event.vote)
        }
    }

    private fun requestSummary(alerts: ImmutableSet<ServiceAlert>) {
        val flagEnabled = isAlertSummaryEnabled()
        // Deliberately unconditional (not debug-gated): `adb logcat | grep AlertSummaryViewModel`
        // is the one line that answers "why is nothing showing" without re-adding ad-hoc
        // logging by hand every time. See AiTextService's own checkAvailability/summarize
        // logs (tag "AiTextService") for the rest of the chain.
        log("AlertSummaryViewModel: requestSummary, flagEnabled=$flagEnabled, alertCount=${alerts.size}")
        if (!flagEnabled || alerts.isEmpty()) return
        val setKey = alerts.summaryCacheKey()
        if (requestedSetKey == setKey) return
        requestedSetKey = setKey

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            val availability = aiTextService.checkAvailability()
            log("AlertSummaryViewModel: availability -> $availability")
            if (availability !is AiAvailability.Available) {
                if (availability is AiAvailability.Unavailable && availability.reason in RETRIABLE_REASONS) {
                    requestedSetKey = null
                }
                // Never leave a summary from a previous, different alert set rendering
                // under the current one's cards.
                _uiState.value = null
                return@launch
            }
            _uiState.value = AlertSummaryUiState.Generating

            val summary = summarizeAlerts(alerts)

            if (summary == null) {
                _uiState.value = null
                return@launch
            }

            // On-device inference can resolve fast enough that the Generating state (and
            // its wheel/border spin) never really registers before the summary pops in.
            // A fixed beat here keeps the loading state visibly intentional regardless of
            // how quick the real call was.
            delay(GENERATING_MIN_DURATION_MS)

            _uiState.value = AlertSummaryUiState.Resolved(text = summary)
        }
    }

    /**
     * One [AiTextService.summarize] call per alert, never one call joining every alert into
     * a single prompt. The earlier joined-prompt approach silently dropped every alert past
     * the first on a 4-alert trip (see `ALERT_SUMMARY_UX.md`'s "Known gap" section) — neither
     * platform's summarizer is instructed to cover N independent items, so it just summarizes
     * the block it's best able to make sense of and drops the rest. Per-alert calls sidestep
     * that entirely: each call only ever has one alert to summarize, the exact shape both
     * platforms' single-alert instructions already handle correctly.
     *
     * Returns `null` only if every alert failed to summarize — a partial result (some alerts
     * summarized, some didn't) still renders rather than being thrown away entirely.
     */
    private suspend fun summarizeAlerts(alerts: ImmutableSet<ServiceAlert>): String? {
        if (alerts.size == 1) {
            return alerts.first().let { aiTextService.summarize("${it.heading}. ${it.message}") }
                .cleanedUpOrNull()
        }

        val perAlertSummaries = alerts.mapNotNull { alert ->
            aiTextService.summarize("${alert.heading}. ${alert.message}").cleanedUpOrNull()
        }
        return perAlertSummaries.takeIf { it.isNotEmpty() }?.joinToString(separator = "\n") { "• $it" }
    }

    /**
     * The vote is recorded in state only: it flips the control to its chosen side and locks
     * it. Nothing is reported anywhere.
     */
    private fun onVoteClicked(vote: AlertFeedbackVoteChoice) {
        val current = _uiState.value as? AlertSummaryUiState.Resolved ?: return
        if (current.vote != null) return // one vote per summary, matches every other feedback control

        _uiState.value = current.copy(vote = vote)
    }
}

/**
 * Stable per-content id for a set of alerts. There is no server-provided alert id in
 * [ServiceAlert] today — content is stable once TfNSW publishes it, so the sorted,
 * concatenated heading+message of every alert in the set is an adequate substitute cache
 * key. Sorted so the same set of alerts hashes identically regardless of iteration order.
 */
internal fun ImmutableSet<ServiceAlert>.summaryCacheKey(): String =
    map { "${it.heading}|${it.message}" }.sorted().joinToString("||").hashCode().toString()

private fun String?.cleanedUpOrNull(): String? =
    this?.trim()?.removePrefix("* ")?.removePrefix("- ").takeUnless { it.isNullOrBlank() }
