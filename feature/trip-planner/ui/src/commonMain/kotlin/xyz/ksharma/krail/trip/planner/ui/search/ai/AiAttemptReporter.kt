package xyz.ksharma.krail.trip.planner.ui.search.ai

import xyz.ksharma.krail.core.aitext.TripIntentExtraction
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.trip.planner.ui.search.ai.resolve.RiderOriginLocator
import kotlin.random.Random

/**
 * Records what an Ask KRAIL attempt did, and owns the two counters that decide what an attempt
 * *is*.
 *
 * One collaborator rather than an analytics reference and an id generator sitting beside each
 * other on the ViewModel: they only ever answer one question between them, and the session
 * lifetime rules below are the whole definition of the metric they produce.
 *
 * ## One askSessionId is one question
 *
 * Both counters reset together, and when they reset is the metric's definition rather than an
 * implementation detail:
 *
 * | Event | Resets |
 * |---|---|
 * | Dialog opened | **Yes.** A fresh prompt is a fresh question |
 * | Handoff settled, dialog closed itself | **Yes**, via the open that follows |
 * | Rider started over | **Yes.** They said this is a new question |
 * | Identical text resubmitted | No. It increments; it is a second attempt from the rider's side |
 * | App backgrounded with the dialog open | **No** |
 *
 * The last row is the one that matters and it is deliberately the odd one out. A rider who
 * switches out to look up an address and comes back would otherwise restart at 1, so their
 * return attempt books as a first attempt. That inflates first-attempt success, the headline
 * quality number, for exactly the riders who had to go and check something, which is to say the
 * ones who struggled. The bias runs in the flattering direction.
 */
class AiAttemptReporter(
    private val analytics: Analytics?,
    // The rider's own words for their places. Fetched here rather than in the ViewModel because
    // both readers of them are attempt-scoped: the label-word fallback that rescues a sentence
    // whose only place is "work", and hadLabelWord on the row this class sends.
    private val riderLabels: suspend () -> List<String> = { emptyList() },
    private val newAskSessionId: () -> String = ::randomAskSessionId,
) {

    private var askSessionId: String = newAskSessionId()
    private var attemptIndex: Int = 0

    private var labelsSeenDuringThisAttempt: List<String> = emptyList()
    private var originSource: String = ORIGIN_UNKNOWN

    fun startSession() {
        askSessionId = newAskSessionId()
        attemptIndex = 0
        labelsSeenDuringThisAttempt = emptyList()
        originSource = ORIGIN_UNKNOWN
    }

    fun beginAttempt() {
        attemptIndex += 1
    }

    /** Fetched once per attempt, and kept so the row can say whether one was present. */
    suspend fun labelsForThisAttempt(): List<String> =
        riderLabels().also { labelsSeenDuringThisAttempt = it }

    /**
     * How the journey's start was decided.
     *
     * The three empty outcomes are kept apart because they point in opposite directions.
     * `at_destination` and `no_stop_near` are the app declining to fill a field because of
     * something it knows about the rider right now, which is the design working. `unknown` is
     * knowing nothing at all, which is a rider left with an empty field. Collapsed into one
     * value those cancel out and the param answers nothing.
     */
    fun rememberOrigin(saidByRider: Boolean, outcome: RiderOriginLocator.Origin?) {
        originSource = when {
            saidByRider -> "said"
            outcome == RiderOriginLocator.Origin.LOCATED -> "located"
            outcome == RiderOriginLocator.Origin.AT_DESTINATION -> "at_destination"
            outcome == RiderOriginLocator.Origin.NO_STOP_NEAR -> "no_stop_near"
            else -> "unknown"
        }
    }

    fun report(
        state: AiSearchInputUiState,
        reason: String,
        riderText: String,
        extraction: TripIntentExtraction?,
        extractMs: Long?,
    ) {
        val analytics = analytics ?: return
        val resolved = state.resolved
        val redaction = AiSentenceTemplateRedaction.redact(riderText, extraction)

        analytics.track(
            AnalyticsEvent.AskKrailAttemptEvent(
                phase = state.phase.name,
                reason = reason,
                endsResolved = endsResolvedOf(resolved),
                extractedEnds = extractedEndsOf(extraction),
                originSource = originSource,
                inputMode = inputModeOf(state),
                timeShape = timeShapeOf(extraction),
                hadLabelWord = hadLabelWordIn(riderText, labelsSeenDuringThisAttempt),
                spanMatchedVerbatim = redaction.spanMatchedVerbatim,
                attemptIndex = attemptIndex,
                extractMs = extractMs,
                unmatchedKind = unmatchedKindOf(state.unmatchedPlace),
                templateKept = redaction.template != null,
                sentenceTemplate = redaction.template,
                fromStopId = resolved?.fromStopItem?.stopId,
                toStopId = resolved?.toStopItem?.stopId,
                askSessionId = askSessionId,
            ),
        )
    }

    private companion object {
        const val ORIGIN_UNKNOWN = "unknown"
    }
}

private const val HEX_DIGITS_IN_64_BITS = 16

/**
 * Meaningless by design: not stored on the device, not derived from anything, and adding no
 * information about the rider. It only links this attempt's rows to each other and to the
 * timetable they loaded afterwards.
 *
 * Deliberately a different param name from `searchSessionId` rather than a shared namespace with
 * a kind flag. A `load_timetable_click` carrying a `searchSessionId` with no matching
 * `search_stop_query` is already ambiguous with rows from before that join existed; a second
 * meaning on the same name would make it unresolvable.
 */
private fun randomAskSessionId(): String =
    Random.nextLong().toULong().toString(radix = HEX_RADIX).padStart(HEX_DIGITS_IN_64_BITS, '0')

private const val HEX_RADIX = 16
